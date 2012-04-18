public class TNode {
	int[] ptrs; //sector numbers 32 bytes
	int TNum;
	int height; // 4 bytes
	int total_blocks;
	byte[] metadata; // 64 bytes


	public TNode(int num) {
		TNum = num;
		ptrs = new int[PTree.TNODE_POINTERS];
		height = 0;
		total_blocks = 0;
		metadata = new byte[PTree.METADATA_SIZE];
	}

	public void addBlock(int sector) {
		if(height == 0) {
			for(int i = 0; i < PTree.TNODE_POINTERS-1; i++) {
				if(ptrs[i] == 0) {
					ptrs[i] = sector;
					break;
				}
			}
			//TODO : make an indirect block for the 8th ptr and add it to indirect block
		}
		
		total_blocks++;
	}

	public void writeBlock(Transaction trans, int blockID, byte[] buffer) {
		if(blockID < 7) {
			int sector_num = ptrs[blockID];
			if(sector_num == 0) {
				return;  //TODO GET FREE BLOCK AND ADD IT TO THE TREE
			}
			byte[][] sec_buffers = split_buffer(buffer, 2);
			trans.addWrite(sector_num, sec_buffers[0]);
			trans.addWrite(sector_num+1, sec_buffers[1]);
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
}
