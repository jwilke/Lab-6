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
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

public class Transaction{
	
	/*
	 * TransID id;
	 * List Writes
	 * Status (COMMITTED, IN_PROGRESS, ABORTED)
	 * int start_in_log;
	 * int num_sects
	 */

	
	TransID id;
	LinkedList<Write> write_list;
	byte Status;
	int start_in_log;
	int num_sects;
	
	public Transaction() {
		id = new TransID();
		write_list = new LinkedList<Write>();
		Status = Common.IN_PROGRESS;
		start_in_log = -1; 
		num_sects = 0;
	}

    // 
    // You can modify and add to the interfaces
    //

    public void addWrite(int sectorNum, byte buffer[])
    throws IllegalArgumentException, 
           IndexOutOfBoundsException
    {
    	// search for write with sectorNum
    	Iterator<Write> it = write_list.iterator();
    	Write temp = null;
    	boolean found = false;
    	while(it.hasNext()) {
    		temp = it.next();
    		if(temp.secNum == sectorNum) {
    			found = true;
    			break;
    		}
    	}
    	
    	if(found) { // if it was found, update the buffer
    		temp.updateBuffer(sectorNum, buffer);
    	} else {  //otherwise, add it to our write_list
    		Write add = new Write(sectorNum, buffer);
    		write_list.add(add);
    		num_sects++;
    	}
    	return;
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
    	// search list for write to secNum
    	// if found update buffer and return true
    	Iterator<Write> it = write_list.iterator();
    	Write temp = null;
    	boolean found = false;
    	while(it.hasNext()) {
    		temp = it.next();
    		if(temp.secNum == sectorNum) {
    			found = true;
    			break;
    		}
    	}
    	
    	if(found) {
    		for (int i = 0; i < temp.cData.length; i++) {
    			buffer[i] = temp.cData[i];
    		}
    		return true;
    	}
    	
        return false;
    }


    public void commit()
    throws IOException, IllegalArgumentException
    {
    	// change status
    	Status = Common.COMMITED;
    }

    public void abort()
    throws IOException, IllegalArgumentException
    {
    	// change status
    	Status = Common.ABORTED;    	
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
    	/*int ah = write_list.size()*4 - 472;
    	if(ah > 0) {
    		ah = (write_list.size()*4 - 472)/512;
    	} else {
    		ah = 0;
    	}*/
    	
    	byte ret[] = new byte[(write_list.size() +2)*512];
    	
    	longToByte(id.getTranNum(),ret,0);
    	
    	int s = write_list.size();
    	intToByte(s,ret,8);
    	
    	ret[12] = Status;

    	Iterator<Write> it = write_list.iterator();
    	Write temp = null;
    	int j = 0;
    	while(it.hasNext()) {
    		temp = it.next();
    		intToByte(temp.secNum,ret,40+j*4);
    		for(int k = 0; k < 512; k++) {
    			ret[(j+1) * 512 + k] = temp.cData[k]; 
    		}
    		j++;
    	}
    	
    	
        return ret;
    }
    
    public void intToByte(int src, byte dest[], int offset) {
    	for(int i = 0; i < 4; i++) {
    		dest[offset+3-i] = (byte) (src >>> (i*8));
    	}
    }
    
    public void longToByte(long src, byte dest[], int offset) {
    	for(int i = 0; i < 8; i++) {
    		dest[offset+7-i] = (byte) (src >>> (i*8));
    	}
    }

    //
    // You'll want to remember where a Transactions
    // log record ended up in the log so that
    // you can free this part of the log when
    // writeback is done.
    //
    public void rememberLogSectors(int start, int nSectors){
    	start_in_log = start;
    	num_sects = nSectors;
    }
    public int recallLogSectorStart(){
    	return start_in_log;
    }
    public int recallLogSectorNSectors(){
    	return num_sects;
    }



    //
    // For a committed transaction, return
    // the number of sectors that this
    // transaction updates. Used for writeback.
    //
    public int getNUpdatedSectors(){
    	return write_list.size();
    }

    //
    // For a committed transaction, return
    // the sector number and body of the
    // ith sector to be updated. Return
    // a secNum and put the body of the
    // write in byte array. Used for writeback.
    //
    public int getUpdateI(int i, byte buffer[]){
    	if (Status == Common.COMMITED) {
    		//go to the ith write, update buffer, return sec num
        	Write temp = write_list.get(i);
        	for(int j = 0; j < 512; j++) {
        		buffer[j] = temp.cData[j];
        	}
        	return temp.secNum;
    	}
        return -1;
    }
    

    /*
     * Header
     * TranID number
     * number of updates
     * Status - written to disk or not
     *   
     * 40th byte - secNum first update
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
    	// j = retrieve size from buffer; return j;
    	int j = (((int)buffer[8]) << 24) | (((int)buffer[9]) << 16) | (((int)buffer[10]) << 8) | ((int)buffer[11]);
        return j;
    }

    //
    // Parse k+2 sectors from disk (header + k update sectors + commit).
    // If this is a committed transaction, construct a
    // committed transaction and return it. Otherwise
    // throw an exception or return null.
    public static Transaction parseLogBytes(byte buffer[]){
    	Transaction t = new Transaction();
    	t.Status = Common.COMMITED;
    	
    	// create array of secNums
    	int num = parseHeader(buffer);
    	int j = 0;
    	for(int i = 0; i < num; i++) {
    		j = (((int)buffer[40+i]) << 24) | (((int)buffer[41+i]) << 16) | (((int)buffer[42+i]) << 8) | ((int)buffer[43+i]);
    		t.addWrite(j, Arrays.copyOfRange(buffer, (i+1)*512, (i+2)*512));
    		j = 0;
    	}
    	
        return t;
    }
    
    
}


