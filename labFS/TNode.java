import java.util.ArrayList;

public class TNode {
	int[] ptrs; //sector numbers 32 bytes
	int TNum;
	int height; // 4 bytes
	int total_blocks;
	byte[] metadata; // 64 bytes
	// Total			100


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
	
	public void write_to_buffer(byte[] buffer) {
		int spot = TNum % 5;
		// write ptrs
		for(int i = 0; i < ptrs.length; i++){
			Common.intToByte(ptrs[i], buffer, 100*spot+(i*4));
		}
		// write height
		Common.intToByte(height, buffer, 100*spot + 32);
		// write meta
		for(int i = 0; i < metadata.length; i++) {
			buffer[100*spot+36+i] = metadata[i];
		}
	}
	
	public ArrayList<Integer> free_blocks() {
		ArrayList<Integer> freed_secs = new ArrayList<Integer>();
		for(int i = 0; i < ptrs.length - 1; i++) {
			if(ptrs[i] < PTree.TNODE_LOCTION) freed_secs.add(ptrs[i]); // TODO update overhead length
			ptrs[i] = 0;
		}
		
		if(ptrs[7] != 0) {
			// TODO go through indirect
		}
		
		return freed_secs;
	}
}
