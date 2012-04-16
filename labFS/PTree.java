/*
 * PTree -- persistent tree
 *
 * You must follow the coding standards distributed
 * on the class web page.
 *
 * (C) 2007, 2010, 2012 Mike Dahlin
 *
 */

import java.io.IOException;
import java.io.EOFException;

public class PTree{
	/*
	 * Glodals: (2 sectors)
	 * 		Pointer to Tree Pointers?
	 * 		Pointer to Bit Map?
	 * 		Pointer to Data Section?
	 * 		Number of Trees?
	 * 
	 * Tree Pointers: (32 sectors)
	 * 		Tnodes (each 32 bytes)
	 * 
	 * bit Map: (4 sectors)
	 * 		4 sectors of bits, each one represents a sectors
	 * 		(get sector num, x = num/4096 to get bit Map sector
	 * 		x/8 to get byte number in that sector
	 * 		x%8 to get the bit in that byte)   (compare to -1 to see if all the 
	 * 		sectors are taken in that byte)
	 * 
	 * Data: (the rest)
	 * 	
	 */
	
	
	
  public static final int METADATA_SIZE = 64;
  public static final int MAX_TREES = 512;
  public static final int MAX_BLOCK_ID = Integer.MAX_VALUE; 

  //
  // Arguments to getParam
  //
  public static final int ASK_FREE_SPACE = 997;
  public static final int ASK_MAX_TREES = 13425;
  public static final int ASK_FREE_TREES = 23421;

  //
  // TNode structure
  //
  public static final int TNODE_POINTERS = 8;
  public static final int BLOCK_SIZE_BYTES = 1024;
  public static final int POINTERS_PER_INTERNAL_NODE = 256;

  // private ADisk disk;
  // SimpleLock lock;
  // Condition inUse;
  // boolean beingUsed

  /**
   * This function is the constructor. If doFormat == false, 
   * data stored in previous sessions must remain stored. 
   * If doFormat == true, the system should initialize the underlying disk to empty.
   * @param doFormat
   */
  public PTree(boolean doFormat)
  {
	  // disk = new ADisk(doFormat); 
	  // lock = new SimpleLock();
	  // inUse = lock.getCondition();
  }

  public TransID beginTrans()
  {
	  // lock.lock();
	  // if in use wait
	  // set in use
	  // disk.beginTransaction();
	  // unlock
	  return null;
  }

  public void commitTrans(TransID xid) 
    throws IOException, IllegalArgumentException
  {
	  // lock
	  // disk.commitTransaction(xid);
	  // set to not in use
	  // signal
	  // unlock
  }

  public void abortTrans(TransID xid) 
    throws IOException, IllegalArgumentException
  {
	  // lock
	  // disk.abortTransaction()
	  // set to not in use
	  // signal
	  // unlock
  }


  public int createTree(TransID xid) 
    throws IOException, IllegalArgumentException, ResourceException
  {
	  // find a place for a new Tnode
	  // add data to Tnode
	  // return tnum (the location of the tnode)
    return -1;
  }

  public void deleteTree(TransID xid, int tnum) 
    throws IOException, IllegalArgumentException
  {
	  // free in all block uesd by tree in bit map
	  // free tnode in bit map
  }

  /**
   * This function returns the maximum ID of any data block
   * stored in the specified tree. Note that blocks in a tree 
   * are numbered starting from 0. 
   * @param xid
   * @param tnum
   * @throws IOException
   * @throws IllegalArgumentException
   */
  public void getMaxDataBlockId(TransID xid, int tnum)
    throws IOException, IllegalArgumentException
  {
	  // get the data stored in the tnode
	  // keep track of this as you add and remove blocks
  }

  public void readData(TransID xid, int tnum, int blockId, byte buffer[])
    throws IOException, IllegalArgumentException
  {
	  // find the sectors in the Tnode
	  // read from ADisk to two buffers
	  // combine the two buffers to the one buffer, pass two reads
	  // return
  }


  public void writeData(TransID xid, int tnum, int blockId, byte buffer[])
    throws IOException, IllegalArgumentException
  {
	  // see if tree contains the blockId'th block.
	  // if so, determine the sectors in the Tnode for that block (that sector and that sector plus one)
	  // 	not, find a new free 2 continuous sectors in the bitmap, mark them used, add the first to the 
	  //    	tnode's first available open block.  update any metadata (size etc)
	  // 
	  // 
	  // split the buffer,  pass two writes to the ADisk
	  //  
  }

  public void readTreeMetadata(TransID xid, int tnum, byte buffer[])
    throws IOException, IllegalArgumentException
  {
	// find the tnode from the tnum.  follow first pointer to data block and read the
	// first sector and copy the first 64 bytes to the buffer;
  }


  public void writeTreeMetadata(TransID xid, int tnum, byte buffer[])
    throws IOException, IllegalArgumentException
  {
	  // find the tnode from the tnum.  follow first pointer to data block and read the
	  // first sector and copy the first 64 bytes from the buffer to the sector
	  // and write the sector with adisk
  }

  public int getParam(int param)
    throws IOException, IllegalArgumentException
  {
	if(param == PTree.ASK_FREE_SPACE) {
		//cycle through the bitmap and return number of free sectors available
	} else if (param == PTree.ASK_MAX_TREES) {
		//return PTree.maxtrees
	} else if (param == PTree.ASK_FREE_TREES) {
		// return Ptree.maxtrees - number of trees;
	} else {
		//throw appropriate exception
	}
    return -1;
  }

  
}
