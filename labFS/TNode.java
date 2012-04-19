import java.util.ArrayList;

public class TNode {
	int[] ptrs; //sector numbers 32 bytes
	int TNum;
	byte height; // 1 byte
	int total_blocks; //4 bytes
	byte[] metadata; // 64 bytes
	InternalNode[] intNodes;
	// Total			101

	public TNode(int num) {
		TNum = num;
		ptrs = new int[PTree.TNODE_POINTERS];
		height = 1;
		total_blocks = 0;
		metadata = new byte[PTree.METADATA_SIZE];
		intNodes = null;
	}

	public int addBlock(int sector, BitMap bitmap) {
		if(total_blocks < 8) {
			for(int i = 0; i < PTree.TNODE_POINTERS; i++) {
				if(ptrs[i] == 0) {
					ptrs[i] = sector;
					break;
				}
			}
		} else {
			if(intNodes == null) {
				assert(total_blocks == 8);
				intNodes = new InternalNode[PTree.MAX_TREES];
				int isector = bitmap.first_free();
				intNodes[0] = new InternalNode(isector, 1);
				for(int i = 0; i < ptrs.length; i++) {
					intNodes[0].addBlock(ptrs[i], bitmap, 1); //changed
					ptrs[i] = 0;
				}
				ptrs[0] = isector;
				intNodes[0].addBlock(sector, bitmap, 1); //changed
				height++;
			} else {
				// TODO find which internal node to put it in
				boolean found = false;
				for(int i = 0; i < intNodes.length; i++) {
					if(intNodes[i].hasFree(height-1)) {
						//it goes in this int_node
						intNodes[i].addBlock(sector, bitmap, height-1);
						found = true;
						break;
					}
				}
				if(found == false) {
					expandHeight(bitmap);
					intNodes[0].addBlock(sector, bitmap, height-1);
				}
			}
		}

		total_blocks++;

		return total_blocks-1;
	}

	private void expandHeight(BitMap bm) {
		int sect = bm.first_free();
		InternalNode newnode = new InternalNode(sect, height, intNodes);
		for(int i = 0; i < 8; i++) {
			intNodes[i] = null;
			ptrs[i] = 0;
		}
		intNodes[0] = newnode;
		ptrs[0] = sect;
		height++;
	}

	public void writeBlock(TransID xid, int blockID, byte[] buffer, ADisk disk, BitMap bitmap) {
		int sect = -1;
		while(blockID > total_blocks) {
			sect = bitmap.first_free();
			addBlock(sect, bitmap);
		}
		if(total_blocks < 8) {
			int sector_num = ptrs[blockID];
			byte[][] sec_buffers = split_buffer(buffer, 2);
			disk.writeSector(xid, sector_num, sec_buffers[0]);
			disk.writeSector(xid, sector_num+1, sec_buffers[1]);
		} else {
			//figure out which inode the blockID is in
			double divisor = Math.pow(256, height-1);
			int the_node = (int) (blockID / divisor);
			intNodes[the_node].writeBlock(xid, (int)(blockID - (the_node * divisor)), buffer, disk, height-1);
		}
	}

	public static byte[][] split_buffer(byte[] buffer, int num) {
		byte[][] ret = new byte[num][512];

		for(int i = 0; i < num; i++) {
			for(int j = 0; j < 512; j++) {
				ret[i][j] = buffer[(i*512)+j];
			}
		}

		return ret;
	}

	public void write_to_buffer(byte[] buffer) {
		int spot = TNum % 5;
		// write ptrs
		for(int i = 0; i < ptrs.length; i++){
			Common.intToByte(ptrs[i], buffer, 101*spot+(i*4));
		}
		// write height
		buffer[101*spot+32] = height;
		//write total_blocks
		Common.intToByte(total_blocks, buffer, 101*spot + 33);
		// write meta
		for(int i = 0; i < metadata.length; i++) {
			buffer[101*spot+37+i] = metadata[i];
		}
	}

	// TODO create a way to read a buffer and make a TNode from it
	public static TNode build_from_buffer(byte[] buffer, int tnum) {
		int spot = tnum % 5;
		TNode ret = new TNode(tnum);
		for(int i = 0; i < 8; i++) {
			ret.ptrs[i] = Common.byteToInt(buffer,spot*101+i*4);
		}
		ret.height = buffer[101*spot+32];
		ret.total_blocks = Common.byteToInt(buffer, spot*101+33);
		for(int i = 0; i < PTree.METADATA_SIZE; i++) {
			ret.metadata[i] = buffer[101*spot+37+i];
		}
		// TODO traverse tree if height > 1
		return ret;
	}

	public ArrayList<Integer> free_blocks() {
		ArrayList<Integer> freed_secs = new ArrayList<Integer>();
		for(int i = 0; i < ptrs.length - 1; i++) {
			if(ptrs[i] > PTree.TNODE_LOCTION) freed_secs.add(ptrs[i]); // TODO update overhead length
			ptrs[i] = 0;
		}

		return freed_secs;
	}


	public static void unit(Tester t) {
		t.set_object("TNode");
		TNode tn1 = new TNode(1);

		t.set_method("addBlock() - Data Blocks");
		BitMap bm = new BitMap(16384, 1025);
		int sect = bm.first_free();
		tn1.addBlock(sect, bm);
		t.is_equal(tn1.total_blocks, 1);
		t.is_equal(tn1.ptrs[0], sect);

		for(int i = 1; i < 8; i++) {
			sect = bm.first_free();
			tn1.addBlock(sect, bm);
			t.is_equal(tn1.total_blocks, i+1);
			t.is_equal(tn1.ptrs[i], sect);
		}

		sect = bm.first_free();
		tn1.addBlock(sect, bm);
		t.is_equal(tn1.total_blocks, 9);
		t.is_equal(tn1.intNodes[0].ptrs[8], sect);
		t.is_equal(tn1.height, 2);

		t.set_method("splitBuffer()");
		byte data[] = new byte[1024];
		for(int i = 0; i < 1024; i++) {
			data[i] = (byte)(i % 128);
		}

		byte[][] comp = TNode.split_buffer(data,2);
		for(int x = 0; x < 2; x++) {
			for(int y = 0; y < 512; y++) {
				t.is_equal(comp[x][y], (x*512+y)%128);
			}
		}

		TNode tn2 = new TNode(0);
		sect = bm.first_free();
		tn2.addBlock(sect, bm);
		int sect2 = bm.first_free();
		tn2.addBlock(sect2, bm);
		byte[] buff = new byte[512];
		tn2.write_to_buffer(buff);
		TNode test = TNode.build_from_buffer(buff, 0);
		t.is_equal(test.ptrs[0], sect);
		t.is_equal(test.ptrs[1], sect2);
		t.is_equal(test.height, 1);
		t.is_equal(test.total_blocks,2);
	}
}
