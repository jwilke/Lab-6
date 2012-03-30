/*
 * Transaction.java
 *
 * List of active transactions.
 *
 * You must follow the coding standards distributed
 * on the class web page.
 *
 * (C) 2011 Mike Dahlin
 *
 */
import java.io.IOException;

public class Transaction{
	
	/*
	 * TransID id;
	 * List Writes
	 * Status (COMMITTED, IN_PROGRESS, ABORTED)
	 * int start_in_log;
	 * int num_sects
	 */
	
	public Transaction() {
		// create variables
		// status = IN_PROGRESS
	}

    // 
    // You can modify and add to the interfaces
    //

    public void addWrite(int sectorNum, byte buffer[])
    throws IllegalArgumentException, 
           IndexOutOfBoundsException
    {
    	// search for write with sectorNum
    	// if found update write
    	// else
    	// add to list of Writes
    }

    //
    // Return true if this transaction has written the specified
    // sector; in that case update buffer[] with the written value.
    // Return false if this transaction has not written this sector.
    //
    public boolean checkRead(int sectorNum, byte buffer[])
    throws IllegalArgumentException, 
           IndexOutOfBoundsException
    {
    	// lock
    	// search list for last committed to secNum
    	// if found update buffer and return true
        return false;
    }


    public void commit()
    throws IOException, IllegalArgumentException
    {
    	// change status
    }

    public void abort()
    throws IOException, IllegalArgumentException
    {
    	// change status
    }



    // 
    // These methods help get a transaction from memory to
    // the log (on commit), get a committed transaction's writes
    // (for writeback), and get a transaction from the log
    // to memory (for recovery).
    //

    //
    // For a committed transaction, return a byte
    // array that can be written to some number
    // of sectors in the log in order to place
    // this transaction on disk. Note that the
    // first sector is the header, which lists
    // which sectors the transaction updates
    // and the last sector is the commit. 
    // The k sectors between should contain
    // the k writes by this transaction.
    //
    public byte[] getSectorsForLog(){
    	//decide on format for the header. -- include size/length/number of writes, pointer to first sector, etc.
    	//create a byte array for the header, the writes, and the commit.
    	//byte ret[] = new byte[(list.length+ 2)*sector_size);
    	//return ret
        return null;
    }

    //
    // You'll want to remember where a Transactions
    // log record ended up in the log so that
    // you can free this part of the log when
    // writeback is done.
    //
    public void rememberLogSectors(int start, int nSectors){
    	//start_in_log = start;
    	//int num_sects = nSectors;
    }
    public int recallLogSectorStart(){
    	//return start_in_log;
        return -1;
    }
    public int recallLogSectorNSectors(){
    	//return num_sects
        return -1;
    }



    //
    // For a committed transaction, return
    // the number of sectors that this
    // transaction updates. Used for writeback.
    //
    public int getNUpdatedSectors(){
    	//return list.length/size
        return -1;
    }

    //
    // For a committed transaction, return
    // the sector number and body of the
    // ith sector to be updated. Return
    // a secNum and put the body of the
    // write in byte array. Used for writeback.
    //
    public int getUpdateI(int i, byte buffer[]){
    	//if status == committed -> go to the ith (by time) write, update buffer, return sec num
        return -1;
    }
    
    /*
     * Header - 
     * TranID
     * length of buffer
     * secNum first update
     * secNum second update
     * ...
     * secNum ith update 
     */

    
    //
    // Parse a sector from the log that *may*
    // be a transaction header. If so, return
    // the total number of sectors in the
    // log that should be read (including the
    // header and commit) to get the full transaction.
    //
    public static int parseHeader(byte buffer[]){
    	// based on the decided format of the header given.
    	// j = retrieve size from buffer; should be sector size?
    	// 
        return -1;
    }

    //
    // Parse k+2 sectors from disk (header + k update sectors + commit).
    // If this is a committed transaction, construct a
    // committed transaction and return it. Otherwise
    // throw an exception or return null.
    public static Transaction parseLogBytes(byte buffer[]){
        return null;
    }
    
    
}


