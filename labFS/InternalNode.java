import java.io.IOException;
import java.util.ArrayList;


public class InternalNode {
	int[] ptrs;
	int height;
	int sector;
	InternalNode[] nodes;
	//int freeBytes;

	public InternalNode(int s, int h) {
		height = h;
		sector = s;
		ptrs = new int[PTree.POINTERS_PER_INTERNAL_NODE];
		nodes = new InternalNode[PTree.POINTERS_PER_INTERNAL_NODE];
	}

	public InternalNode(int s, int h, int[] inodes) {
		height = h;
		sector = s;
		ptrs = new int[PTree.POINTERS_PER_INTERNAL_NODE];
		nodes = new InternalNode[PTree.POINTERS_PER_INTERNAL_NODE];
		for(int i = 0; i < inodes.length; i++) {
			ptrs[i] = inodes[i];
			System.out.println("i: " + ptrs[i]);
			nodes[i] = null;
		}
	}

	// Always at bottom
	private InternalNode(int[] ptrs, int s) {
		this.ptrs = ptrs;
		height = 1;
		sector = s;
	}

	// return added level
	public void addBlock(TransID xid, int s, BitMap bitmap, int blockID, int h, ADisk disk) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
		if(h == 1) {
			ptrs[blockID] = s;
			return;
		}
		// math
		double divisor = Math.pow(256, h-1);
		int the_node = (int) (blockID / divisor);
		// build from b
		if(nodes[the_node] == null) {
			int sect = bitmap.first_free_block();
			nodes[the_node] = new InternalNode(sect, height-1);
			ptrs[the_node] = sect;
			//nodes[the_node] = InternalNode.build_from_buffer(TNode.getInternalBuffer(xid, ptrs[the_node], disk), ptrs[the_node], h-1);
		}
		nodes[the_node].addBlock(xid, s, bitmap, (int)(blockID - (the_node * divisor)), h-1, disk);
		
		// write to disk
		byte[] b1024 = new byte[1024];
		nodes[the_node].write_to_buffer(b1024, h-1);
		byte[][] bOut = TNode.split_buffer(b1024, 2);
		disk.writeSector(xid, ptrs[the_node], bOut[0]);
		disk.writeSector(xid, ptrs[the_node]+1, bOut[1]);
	}
	
	

	public void write_to_buffer(byte[] buffer, int height) {
		for(int i = 0; i < ptrs.length; i++) {
			Common.intToByte(ptrs[i], buffer, i*4);
		}
	}

	public static InternalNode build_from_buffer(byte[] buffer, int sector, int height) {
		if(height  >= 1) {
			InternalNode ret = new InternalNode(sector, height);
			for(int i = 0; i < buffer.length; i += 4) {
				ret.ptrs[i/4] = Common.byteToInt(buffer, i);
			}
			return ret;
		}

		return null;
	}

	public void writeBlock(TransID xid, int blockID, byte[] buffer, ADisk disk, int h) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
		if(h == 1) {
			int sector_num = ptrs[blockID];
			byte[][] sec_buffers = TNode.split_buffer(buffer, 2);
			disk.writeSector(xid, sector_num, sec_buffers[0]);
			disk.writeSector(xid, sector_num+1, sec_buffers[1]);
		} else {
			double divisor = Math.pow(256, h-1);
			int the_node = (int) (blockID / divisor);
			
			if(nodes[the_node] == null) {
				nodes[the_node] = InternalNode.build_from_buffer(TNode.getInternalBuffer(xid, ptrs[the_node], disk), ptrs[the_node], h-1); 
			}
			
			nodes[the_node].writeBlock(xid, (int)(blockID - (the_node * divisor)), buffer, disk, h-1);
		}
	}

	public void free_blocks(TransID xid, BitMap bm, int h, ADisk disk) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
		if(h > 1) {
			// free other internal nodes
			for(int i = 0; i < ptrs.length; i++) {
				if(ptrs[i] != 0) {
					if(nodes[i] == null) {
						byte[] buffer1 = new byte[Disk.SECTOR_SIZE];
						byte[] buffer2 = new byte[Disk.SECTOR_SIZE];
						disk.readSector(xid, ptrs[i], buffer1);
						disk.readSector(xid, ptrs[i]+1,buffer2);
						byte[] buffer = TNode.combine_buffer(buffer1, buffer2);
						nodes[i] = InternalNode.build_from_buffer(buffer, ptrs[i], h-1);
					}
					nodes[i].free_blocks(xid, bm, h-1, disk);
					nodes[i] = null;
				}
			}  
		}

		// free these pointers 
		for(int i = 0; i < ptrs.length; i++) {
			if(ptrs[i] != 0) {
				bm.free_sector(ptrs[i]);
				bm.free_sector(ptrs[i]+1);
			}
		}
	}

	public int get_sector(TransID xid, int blockID, ADisk disk, int h) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
		if(h == 1) {
			return ptrs[blockID];
		}
		
		// math
		double divisor = Math.pow(256, h-1);
		int the_node = (int) (blockID / divisor);
		
		// build from b
		if(nodes[the_node] == null) {
			nodes[the_node] = InternalNode.build_from_buffer(TNode.getInternalBuffer(xid, ptrs[the_node], disk), ptrs[the_node], h-1);
		}
		
		return nodes[the_node].get_sector(xid, (int)(blockID - (the_node * divisor)), disk, h-1);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	public static void unit(Tester t) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
		t.set_object("InternalNode");
		
		
		// Constructor(int, int)
		t.set_method("Construtor(int, int)");
		InternalNode in1 = new InternalNode(2000, 1);
		t.is_equal(256, in1.ptrs.length);
		t.is_equal(1, in1.height);
		t.is_equal(2000, in1.sector);
		t.is_true(in1.nodes == null);
		
		
		
		
		
		
		// Constructor(int[], int)
		t.set_method("Constructor(int[], int");
		int[] ptrs2 = new int[256];
		for(int i = 0; i < ptrs2.length; i++) {
			ptrs2[i] = i+3000;
		}
		InternalNode in2 = new InternalNode(ptrs2, 2001);
		t.is_equal(ptrs2, in2.ptrs);
		t.is_equal(1, in2.height);
		t.is_equal(2001, in2.sector);
		t.is_true(in1.nodes == null);
		
		
		
		
		
		
		// Constructor(int, int, int[])
		ptrs2 = new int[256];
		for(int i = 0; i < ptrs2.length; i++) {
			ptrs2[i] = i+3000;
		}
		in2 = new InternalNode(2001, 1, ptrs2);
		t.is_equal(ptrs2, in2.ptrs);
		t.is_equal(1, in2.height);
		t.is_equal(2001, in2.sector);
		t.is_true(in1.nodes == null);
		
		
		
		
		
		
		// addBlock
		ADisk disk = new ADisk(false);
		BitMap bm = new BitMap(Disk.NUM_OF_SECTORS, PTree.DATA_LOCATION);

		TransID xid = disk.beginTransaction();
		in1.addBlock(xid, 2002, bm, 0, 1, disk);
		t.is_equal(1, in1.height);
		t.is_equal(2002, in1.ptrs[0]);
		
		in1.addBlock(xid, 2004, bm, 1, 1, disk);
		t.is_equal(1, in1.height);
		t.is_equal(2004, in1.ptrs[1]);
		
		in1.addBlock(xid, 2006, bm, 2, 1, disk);
		t.is_equal(1, in1.height);
		t.is_equal(2006, in1.ptrs[2]);
		
		
		
		
		
		
		
		// write to buffer
		t.set_method("write_to_buffer()");
		byte[] buffer = new byte[1024];
		in1.write_to_buffer(buffer, 1);
		t.is_equal(2002, Common.byteToInt(buffer, 0));
		t.is_equal(2004, Common.byteToInt(buffer, 4));
		t.is_equal(2006, Common.byteToInt(buffer, 8));
		for(int i = 12; i < buffer.length; i += 4) {
			t.is_equal(0, Common.byteToInt(buffer, i));
		}
		
		
		
		
		
		
		// build from buffer
		t.set_method("build_from_buffer()");
		InternalNode in3 = InternalNode.build_from_buffer(buffer, 2000, 1);
		t.is_equal(2000, in3.sector);
		t.is_equal(1, in3.height);
		t.is_equal(2002, in3.ptrs[0]);
		t.is_equal(2004, in3.ptrs[1]);
		t.is_equal(2006, in3.ptrs[2]);
		
		
		
		
		
		
		// writeBlock
		t.set_method("writeBlock()");
		byte[] bufferOut = new byte[1024];
		for(int i = 0; i < bufferOut.length; i++) {
			bufferOut[i] = (byte) (i % 128);
		}
		in1.writeBlock(xid, 0, bufferOut, disk, 1);
		disk.commitTransaction(xid);
		TransID xid2 = disk.beginTransaction();
		byte[] bufferWritten = new byte[512];
		disk.readSector(xid, 2002, bufferWritten);
		byte[][] bOut = TNode.split_buffer(bufferOut, 2);
		t.is_equal(bOut[0], bufferWritten);
		
		
		
		
		
		
		// free blocks
		// get sector
		t.set_method("get_sector()");
		t.is_equal(2002, in1.get_sector(xid2, 0, disk, 1));
		t.is_equal(2004, in1.get_sector(xid2, 1, disk, 1));
		t.is_equal(2006, in1.get_sector(xid2, 2, disk, 1));
		
	}
}
