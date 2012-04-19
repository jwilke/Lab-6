import java.util.ArrayList;


public class InternalNode {
	int[] ptrs;
	int height;
	int sector;
	InternalNode[] nodes;
	int freeBytes;
	
	public InternalNode(int s, int h) {
		height = h;
		sector = s;
		ptrs = new int[PTree.POINTERS_PER_INTERNAL_NODE];
		nodes = null;
		freeBytes = 1024;
	}
	
	public InternalNode(int s, int h, InternalNode[] inodes) {
		height = h;
		sector = s;
		ptrs = new int[PTree.POINTERS_PER_INTERNAL_NODE];
		nodes = new InternalNode[PTree.POINTERS_PER_INTERNAL_NODE];
		for(int i = 0; i < inodes.length; i++) {
			nodes[i] = inodes[i];
		}
		freeBytes = 1024;
	}
	
	// Always at bottom
	private InternalNode(int[] ptrs, int s) {
		this.ptrs = ptrs;
		height = 1;
		sector = s;
		freeBytes = 0;
	}
	
	// return added level
	public boolean addBlock(int s, BitMap bitmap, int height) {
		assert (freeBytes >= 8);
		
		int i;
		for(i = 0; i < ptrs.length; i++) {
			if (ptrs[i] == 0) {
				ptrs[i] = s;
				break;
			}
		}
		
		// TODO add another block if needed
		if(i == ptrs.length) {
			
		}
		
		freeBytes -= 8;
		return false;
	}
	
	public void getBlocks(ArrayList<Integer> list, int height) {
		for(int i = 0; i < ptrs.length; i++ ) {
			list.add(ptrs[i]);
		}
		// TODO traverse
	}
	
	public void write_to_buffer(byte[] buffer, int height) {
		for(int i = 0; i < ptrs.length; i++) {
			Common.intToByte(ptrs[i], buffer, i*4);
		}
	}
	
	public void writeBlock(TransID xid, int blockID, byte[] buffer, ADisk disk, int h) {
		if(h == 1) {
			int sector_num = ptrs[blockID];
			byte[][] sec_buffers = TNode.split_buffer(buffer, 2);
			disk.writeSector(xid, sector_num, sec_buffers[0]);
			disk.writeSector(xid, sector_num+1, sec_buffers[1]);
		} else {
			double divisor = Math.pow(256, height-1);
			int the_node = (int) (blockID / divisor);
			nodes[the_node].writeBlock(xid, (int)(blockID - (the_node * divisor)), buffer, disk, h-1);
		}
	}
	
	public boolean hasFree(int height) {
		if(height == 1) {
			return freeBytes > 0;
		}
		for(int i = 0; i < PTree.POINTERS_PER_INTERNAL_NODE; i++) {
			if (nodes[i].hasFree(height-1))
				return true;
		}
		return false;
	}
}
