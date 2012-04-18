import java.util.ArrayList;


public class InternalNode {
	int[] ptrs;
	int height;
	int sector;
	
	public InternalNode(int h, int s) {
		height = h-1;
		sector = s;
		ptrs = new int[PTree.POINTERS_PER_INTERNAL_NODE];
	}
	
	public void addBlock(int s) {
		for(int i = 0; i < ptrs.length; i++) {
			if (ptrs[i] == 0) {
				ptrs[i] = s;
				break;
			}
		}
		// TODO add another block if needed
	}
	
	public void getBlocks(ArrayList<Integer> list) {
		for(int i = 0; i < ptrs.length; i++ ) {
			list.add(ptrs[i]);
		}
		
	}
}
