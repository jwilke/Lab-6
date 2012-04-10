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
import java.io.Serializable;
//import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;

public class Transaction implements Serializable{

	public static final int OFFSET = 40;
	
	private TransID id;
	private LinkedList<Write> write_list;
	private byte Status;
	private int start_in_log;
	private int num_sects;
	SimpleLock lock;
	private int readers;
	private boolean is_writing;
	private Condition CV_reading;
	private Condition CV_writing;
	
	public Transaction() {
		id = new TransID();
		write_list = new LinkedList<Write>();
		Status = Common.IN_PROGRESS;
		start_in_log = -1; 
		num_sects = 0;
		lock = new SimpleLock();
		readers = 0;
		is_writing = false;
		CV_reading = lock.newCondition();
		CV_writing = lock.newCondition();
	}

    // 
    // You can modify and add to the interfaces
    //

    public void addWrite(int sectorNum, byte buffer[])
    throws IllegalArgumentException, 
           IndexOutOfBoundsException
    {
    	lock.lock();
    	is_writing = true;
    	
    	while(readers > 0) {
    		try{
    			CV_writing.await();
    		} catch (Exception e) {}
    	}
    	
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
    	
    	is_writing = false;
    	CV_reading.signalAll();
    	CV_writing.signalAll();
    	
    	lock.unlock();
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
    	lock.lock();
    	while(is_writing) {
    		try{
    			CV_writing.await();
    		} catch (Exception e) {}
    	}
    	readers++;
    	
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
    		readers--;
    		CV_writing.signalAll();
    		CV_reading.signalAll();
        	lock.unlock();
    		return true;
    	}
    	
    	readers--;
		CV_writing.signalAll();
		CV_reading.signalAll();
    	lock.unlock();
        return false;
    }


    public void commit()
    throws IOException, IllegalArgumentException
    {
    	lock.lock();
    	if(Status == Common.ABORTED) {
    		lock.unlock();
    		return;
    	}
    	
    	while(is_writing) {
    		try{
    			CV_writing.await();
    		} catch (Exception e) {}
    	}
    	// change status
    	Status = Common.COMMITED;
    	lock.unlock();
    }

    public void abort()
    throws IOException, IllegalArgumentException
    {
    	lock.lock();
    	// change status
    	Status = Common.ABORTED;
    	lock.unlock();
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
    	lock.lock();
    	
    	while(is_writing) {
    		try {
    			CV_writing.await();
    		} catch (Exception e) {}
    	}
    	
    	byte ret[] = new byte[(write_list.size() +2)*Disk.SECTOR_SIZE];
    	
    	longToByte(id.getTranNum(),ret,0);
    	
    	int s = write_list.size();
    	intToByte(s,ret,8);
    	
    	ret[12] = Status;

    	Iterator<Write> it = write_list.iterator();
    	Write temp = null;
    	int j = 0;
    	while(it.hasNext()) {
    		temp = it.next();
    		intToByte(temp.secNum,ret,OFFSET+j*4);
    		for(int k = 0; k < Disk.SECTOR_SIZE && k < temp.cData.length; k++) {
    			ret[(j+1) * Disk.SECTOR_SIZE + k] = temp.cData[k]; 
    		}
    		j++;
    	}
    	
    	lock.unlock();
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
    	lock.lock();
    	start_in_log = start;
    	num_sects = nSectors+1;
    	lock.unlock();
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
    	if(Status != Common.COMMITED)
    		return 0;
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
    	//lock.lock();
    	if (Status == Common.COMMITED) {
    		//go to the ith write, update buffer, return sec num
        	Write temp = write_list.get(i);
        	for(int j = 0; 	j < Disk.SECTOR_SIZE && 
        					j < buffer.length &&
        					j < temp.cData.length; j++) {
        		buffer[j] = temp.cData[j];
        	}
        	//lock.unlock();
        	return temp.secNum;
    	}
    	//lock.unlock();
        return -1;
    }
    
    
    
    public boolean getUpdateS(int sec_num, byte buffer[]) {
    	lock.lock();
    	if (Status == Common.COMMITED) {
	    	Iterator<Write> iter = write_list.iterator();
	    	Write temp;
	    	while(iter.hasNext()) {
	    		temp = iter.next();
	    		if(temp.isSecNum(sec_num)) {
	    			lock.unlock();
	    			return temp.copyFromBuffer(sec_num, buffer);
	    		}
	    	}
    	}
    	lock.unlock();
    	return false;
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
    	assert(buffer[12] == Common.COMMITED);
    		
    	Transaction t = new Transaction();
    	t.Status = Common.COMMITED;
    	
    	// create array of secNums
    	int num = parseHeader(buffer);
    	int j = 0;
    	for(int i = 0; i < num; i++) {
    		j = (((int)buffer[OFFSET+i]) << 24) | (((int)buffer[OFFSET+1+i]) << 16) | (((int)buffer[OFFSET+2+i]) << 8) | ((int)buffer[OFFSET+3+i]);
    		t.addWrite(j, copyOfRange(buffer, (i+1)*Disk.SECTOR_SIZE, (i+2)*Disk.SECTOR_SIZE));
    		j = 0;
    	}
    	
        return t;
    }
    
    /**
     * Copy the range of a the input at a range from 
     * start to end
     */
    public static byte[] copyOfRange(byte[] input, int start, int end) {
    	byte[] output = new byte[end-start];
    	for (int i = start; i < end && i < input.length; i++) {
    		output[i-start] = input[i];
    	}
    	return output;
    }
    
    /**
     * Get the hashCode of the transaction
     */
    public int hashCode() {
    	return (int) id.getTranNum();
    }
    
    /**
     * returns whether this transaction has the given TransID
     * @param tid - the TransID in questions
     * @return - the equivalence of both id and tid
     */
    public boolean hasTranID(TransID tid) {
    	lock.lock();
    	boolean res = id.equals(tid);
    	lock.unlock();
    	return res;
    }
    
    /**
     * Get the TransID assigned to this Transaction
     * @return - return TransID
     */
    public TransID getTransID() {
		return id;
	}
    
    /**
     * Unit tests for Transaction
     * @throws IllegalArgumentException
     * @throws IOException
     */
    public void unit(Tester t) throws IllegalArgumentException, IOException {
    	t.set_object("Transaction");
    	Transaction tran = new Transaction();
    	
    	//test constructor
    	t.set_method("Transaction()");
    	t.is_equal(5, tran.id.getTranNum(), "TranID");
    	t.is_equal(tran.write_list.size(), 0, "write_list");
    	t.is_equal(tran.Status, Common.IN_PROGRESS, "Status");
    	t.is_equal(tran.start_in_log, -1, "start_in_log");
    	t.is_equal(tran.num_sects, 0, "num_sects");
    	
    	
    	
    	
    	// test addWrite()
    	t.set_method("addWrite()");
    	int sn1 = 1;
    	byte[] b1 = new byte[5];
    	for (byte i = 0; i < 5; i++)
    		b1[i] = i;
    	tran.addWrite(sn1, b1);
    	t.is_equal(tran.write_list.get(0), new Write(sn1, b1));
    	t.is_equal(1, tran.write_list.size(), "num writes");
    	
    	
    	sn1 = 1;
    	b1 = new byte[5];
    	for (byte i = 0; i < 5; i++) {
    		b1[i] = (byte) (i+1);
    		//System.out.println(b1[i]);
    	}
    	tran.addWrite(sn1, b1);
    	t.is_equal(tran.write_list.get(0), new Write(sn1, b1));
    	t.is_equal(1, tran.write_list.size(), "num writes");
    	
    	
    	int sn2 = 2;
    	byte[] b2 = new byte[5];
    	for (byte i = 0; i < 5; i++)
    		b2[i] = (byte) (i+2);
    	tran.addWrite(sn2, b2);
    	t.is_equal(tran.write_list.get(0), new Write(sn1, b1));
    	t.is_equal(tran.write_list.get(1), new Write(sn2, b2));
    	
    	
    	
    	
    	// checkRead(secNum, byte[])
    	t.set_method("checkRead(int, byte[])");
    	byte[] b3 = new byte[5];
    	t.is_true(tran.checkRead(sn1,b3));
    	for (int i = 0; i<b3.length; i++)
    		t.is_equal(i+1, b3[i]);
    	
    	b3 = new byte[5];
    	t.is_true(tran.checkRead(sn2,b3));
    	for (int i = 0; i<b3.length; i++)
    		t.is_equal(i+2, b3[i]);
    	
    	t.is_true(!tran.checkRead(45, b3));
    	
    	
    	//test commit
    	t.set_method("commit()");
    	tran.commit();
    	t.is_equal(Common.COMMITED, tran.Status, "Status");
    	
    	//test abort
    	t.set_method("abort()");
    	tran.abort();
    	t.is_equal(Common.ABORTED, tran.Status, "Status");
    	
    	// int to byte
    	t.set_method("intToByte(int, byte[], int");
    	intToByte(1, b3, 0);
    	t.is_equal(0, b3[0]);
    	t.is_equal(0, b3[1]);
    	t.is_equal(0, b3[2]);
    	t.is_equal(1, b3[3]);
    	
    	intToByte(-1, b3, 0);
    	t.is_equal(-1, b3[0]);
    	t.is_equal(-1, b3[1]);
    	t.is_equal(-1, b3[2]);
    	t.is_equal(-1, b3[3]);
    	
    	intToByte(0x1f233a11, b3, 0);
    	t.is_equal(0x1f, b3[0]);
    	t.is_equal(0x23, b3[1]);
    	t.is_equal(0x3a, b3[2]);
    	t.is_equal(0x11, b3[3]);
    	
    	
    	// long to byte
    	t.set_method("longToByte(long, byte[], int");
    	b3 = new byte[8];
    	longToByte(1, b3, 0);
    	t.is_equal(0, b3[0]);
    	t.is_equal(0, b3[1]);
    	t.is_equal(0, b3[2]);
    	t.is_equal(0, b3[3]);
    	t.is_equal(0, b3[4]);
    	t.is_equal(0, b3[5]);
    	t.is_equal(0, b3[6]);
    	t.is_equal(1, b3[7]);
    	
    	longToByte(-1, b3, 0);
    	t.is_equal(-1, b3[0]);
    	t.is_equal(-1, b3[1]);
    	t.is_equal(-1, b3[2]);
    	t.is_equal(-1, b3[3]);
    	t.is_equal(-1, b3[4]);
    	t.is_equal(-1, b3[5]);
    	t.is_equal(-1, b3[6]);
    	t.is_equal(-1, b3[7]);
    	
    	longToByte(305405742, b3, 0);
    	t.is_equal(0, b3[0]);
    	t.is_equal(0, b3[1]);
    	t.is_equal(0, b3[2]);
    	t.is_equal(0, b3[3]);
    	t.is_equal(0x12, b3[4]);
    	t.is_equal(0x34, b3[5]);
    	t.is_equal(0x1f, b3[6]);
    	t.is_equal(0x2e, b3[7]);
    	
    	
    	// rememberLogSectors
    	t.set_method("rememberLogSectors");
    	tran.rememberLogSectors(2, 100);
    	t.is_equal(2, tran.start_in_log);
    	t.is_equal(102, tran.num_sects);
    	
    	tran.rememberLogSectors(9836, 512);
    	t.is_equal(9836, tran.start_in_log);
    	t.is_equal(514, tran.num_sects);
    	
    	tran.rememberLogSectors(1, 1);
    	t.is_equal(1, tran.start_in_log);
    	t.is_equal(3, tran.num_sects);
    	
    	
    	
    	// recallLogSectorStart()
    	t.set_method("recallLogSectorStart()");
    	tran.rememberLogSectors(2, 100);
    	t.is_equal(2, tran.recallLogSectorStart());
    	
    	tran.rememberLogSectors(980986, 100);
    	t.is_equal(980986, tran.recallLogSectorStart());
    	
    	tran.rememberLogSectors(34, 100);
    	t.is_equal(34, tran.recallLogSectorStart());
    	
    	
    	// recallLogSectorsNSectors()
    	t.set_method("recallLogSectorsNSectors()");
    	tran.rememberLogSectors(2, 100);
    	t.is_equal(102, tran.recallLogSectorNSectors());
    	
    	tran.rememberLogSectors(2, 512);
    	t.is_equal(514, tran.recallLogSectorNSectors());
    	
    	tran.rememberLogSectors(2, 10123);
    	t.is_equal(10125, tran.recallLogSectorNSectors());
    	
    	
    	
    	// copyOfRange()
    	t.set_method("copyOfRange");
    	b3 = new byte[10];
    	for(int i = 0; i < b3.length; i++)
    		b3[i] = (byte) i;
    	byte b4[] = copyOfRange(b3, 2, 5);
    	for(int i = 0; i < b4.length; i++)
    		t.is_equal(b4[i], i+2);
    	
    	
    	
    	// getNUpdatedSectors()
    	tran.Status = Common.IN_PROGRESS;
    	t.set_method("getNUpdatedSectors");
    	t.is_equal(tran.getNUpdatedSectors(), 0);
    	tran.commit();
    	t.is_equal(2, tran.getNUpdatedSectors());
    	tran.addWrite(123, b4);
    	t.is_equal(3, tran.getNUpdatedSectors());
    	

    	
    	// getUpdate()
    	t.set_method("getUpdateI()");
    	
    	byte b5[] = new byte[b4.length];
    	tran.getUpdateI(2, b5);
    	for(int i = 0; i < b5.length; i++)
    		t.is_equal(i+2, b5[i]);
    	b4 = new byte[100];
    	for(int i = 0; i< b4.length; i++)
    		b4[i] = (byte) (100-i);
    	tran.addWrite(321, b4);
    	b5 = new byte[b4.length];
    	tran.getUpdateI(3, b5);
    	for (int i = 0; i < b5.length; i++) {
    		t.is_equal(100-i, b5[i]);
    	}
    	
    	// getTransID
    	// TODO
    	
    	              
    	
    	
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
    	
    	// getSectorsForLog 
    	t.set_method("getSectorsForLog()");
    	tran = new Transaction();
    	for(int i = 0; i < 100; i++) {
    		b5[i] = (byte) i;
    	}
    	tran.addWrite(100, b5);
    	tran.addWrite(101, b5);
    	tran.addWrite(102, b5);
    	tran.addWrite(103, b5);
    	tran.commit();
    	// test TransID
    	b5 = tran.getSectorsForLog();
    	b1 = new byte[8];
    	tran.longToByte(tran.id.getTranNum(), b1, 0);
    	for(int i = 0; i < b1.length; i++) {
    		t.is_equal(b1[i], b5[i], "TransID");
    	}
    	// test number of updates
    	b1 = new byte[4];
    	tran.intToByte(4, b1, 0);
    	for(int i = 0; i < b1.length; i++) {
    		t.is_equal(b1[i], b5[i+8], "number of updates");
    	}
    	// test Status
    	t.is_equal(Common.COMMITED, b5[12], "Status");
    	int size = 4;
    	for(int i = 0; i < size; i++) {
    		for (int j = 0; j < 100; j++) {
    			t.is_equal(j, b5[(i+1)*512 + j]);
    		}
        	tran.intToByte(100+i, b1, 0);
        	for(int j = 0; j < 4; j++) {
        		t.is_equal(b1[j], b5[OFFSET + (i*4) +j]);
        	}
    	}
    	
    	
    	
    	
    	// parseHeader()
    	t.set_method("parseHeader()");
    	t.is_equal(4, Transaction.parseHeader(b5));
    	
    	
    	
    	
    	
    	// parseLogBytes()
    	t.set_method("ParseLogBytes()");
    	Transaction tran2 = Transaction.parseLogBytes(b5);
    	t.is_equal(4, tran2.num_sects);
    	t.is_equal(-1, tran2.start_in_log);
    	t.is_equal(Common.COMMITED, tran2.Status);
    	t.is_equal(4, tran2.write_list.size());
    	
    	
    	
    }

	
    
}


