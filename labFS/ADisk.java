/*
 * ADisk.java
 *
 * You must follow the coding standards distributed
 * on the class web page.
 *
 * (C) 2007, 2010 Mike Dahlin
 *
 */
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

public class ADisk{
	/*
	 * Internal Data
	 * 
	 * Disk disk;
	 * ActiveTransactionList atl;
	 * WriteBackList wbl
	 * LogStatus log;
	 * CallbackTracker cbt;
	 */

	Disk disk;
	ActiveTransactionList atl;
	WriteBackList wbl;
	LogStatus log;
	CallbackTracker cbt;

	//-------------------------------------------------------
	// The size of the redo log in sectors
	//-------------------------------------------------------
	public static final int REDO_LOG_SECTORS = 1024;

	//-------------------------------------------------------
	//
	// Allocate an ADisk that stores its data using
	// a Disk.
	//
	// If format is true, wipe the current disk
	// and initialize data structures for an empty 
	// disk.
	//
	// Otherwise, initialize internal state, read the log, 
	// and redo any committed transactions. 
	//
	//-------------------------------------------------------
	public ADisk(boolean format)
	{
		atl = new ActiveTransactionList();
		wbl = new WriteBackList();
		cbt = new CallbackTracker();
		try {
			disk = new Disk(cbt);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		byte[] formatbuffer = new byte[Disk.SECTOR_SIZE];

		if(format) {
			Vector<Integer> tags = new Vector<Integer>();
			for(int i = 0; i < disk.NUM_OF_SECTORS; i++) {
				tags.add(i);
				try {
					disk.startRequest(Disk.WRITE, i, i, formatbuffer);
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			cbt.waitForTags(tags);
		}
		log = new LogStatus(disk, cbt, wbl);
		// else
		// create new disk from log passing cbt

		// create writeback worker
		//WriteBackWorker wbw = new WriteBackWorker(cbt, disk, log, wbl);
		//wbw.run();
	}

	//-------------------------------------------------------
	//
	// Return the total number of data sectors that
	// can be used *not including space reserved for
	// the log or other data structures*. This
	// number will be smaller than Disk.NUM_OF_SECTORS.
	//
	//-------------------------------------------------------
	public int getNSectors()
	{
		// return Disk.NUM_OF_SECTORS
		return Disk.NUM_OF_SECTORS - Disk.ADISK_REDO_LOG_SECTORS - 1; 
	} 

	//-------------------------------------------------------
	//
	// Begin a new transaction and return a transaction ID
	//
	//-------------------------------------------------------
	public TransID beginTransaction()
	{
		Transaction tran = new Transaction();
		atl.put(tran);
		return tran.getTransID();
	}

	//-------------------------------------------------------
	//
	// First issue writes to put all of the transaction's
	// writes in the log.
	//
	// Then issue a barrier to the Disk's write queue.
	//
	// Then, mark the log to indicate that the specified
	// transaction has been committed. 
	//
	// Then wait until the "commit" is safely on disk
	// (in the log).
	//
	// Then take some action to make sure that eventually
	// the updates in the log make it to their final
	// location on disk. Do not wait for these writes
	// to occur. These writes should be asynchronous.
	//
	// Note: You must ensure that (a) all writes in
	// the transaction are in the log *before* the
	// commit record is in the log and (b) the commit
	// record is in the log before this method returns.
	//
	// Throws 
	// IOException if the disk fails to complete
	// the commit or the log is full.
	//
	// IllegalArgumentException if tid does not refer
	// to an active transaction.
	// 
	//-------------------------------------------------------
	public void commitTransaction(TransID tid) 
	throws IOException, IllegalArgumentException
	{
		Transaction temp = atl.get(tid);
		if(temp == null) {
			throw new IllegalArgumentException();
		}
		// change status
		temp.commit();
		// ask log status where to put the log and find place in log
		int logstart = log.reserveLogSectors(temp.getNUpdatedSectors());
		// issue writes to log
		byte[] dataToWrite = temp.getSectorsForLog();
		byte[] sector = new byte[Disk.SECTOR_SIZE];
		Vector<Integer> tags = new Vector<Integer>();
		int temp_tag;
		for(int i = 0; i < temp.getNUpdatedSectors()+1; i++) {
			sector = Arrays.copyOfRange(dataToWrite, i*Disk.SECTOR_SIZE, (i+1)*Disk.SECTOR_SIZE);
			temp_tag = log.CURRENT_TAG++;
			disk.startRequest(disk.WRITE, temp_tag, (logstart+i)%disk.ADISK_REDO_LOG_SECTORS, sector);
			tags.add(temp_tag);
		}
		// issue barrier to log
		disk.addBarrier();
		cbt.waitForTags(tags);
		// issue commit to log
		log.writeCommit();
		// move it from atl.remove(tid) to wbl.addCommitted()
		atl.remove(tid);
		wbl.addCommitted(temp);
	}



	//-------------------------------------------------------
	//
	// Free up the resources for this transaction without
	// committing any of the writes.
	//
	// Throws 
	// IllegalArgumentException if tid does not refer
	// to an active transaction.
	// 
	//-------------------------------------------------------
	public void abortTransaction(TransID tid) 
	throws IllegalArgumentException
	{
		Transaction t = atl.remove(tid);
		if(t == null)
			throw new IllegalArgumentException("TransID does not refer to active transaction");
		
		try {
			t.abort();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	//-------------------------------------------------------
	//
	// Read the disk sector numbered sectorNum and place
	// the result in buffer. Note: the result of a read of a
	// sector must reflect the results of all previously
	// committed writes as well as any uncommitted writes
	// from the transaction tid. The read must not
	// reflect any writes from other active transactions
	// or writes from aborted transactions.
	//
	// Throws 
	// IOException if the disk fails to complete
	// the read.
	//
	// IllegalArgumentException if tid does not refer
	// to an active transaction or buffer is too small
	// to hold a sector.
	// 
	// IndexOutOfBoundsException if sectorNum is not
	// a valid sector number
	//
	//-------------------------------------------------------
	public void readSector(TransID tid, int sectorNum, byte buffer[])
	throws IOException, IllegalArgumentException, 
	IndexOutOfBoundsException
	{
		
		if (buffer.length < Disk.SECTOR_SIZE) {
			throw new IllegalArgumentException("buffer size less then sector size");
		}
		if (sectorNum < Disk.ADISK_REDO_LOG_SECTORS+1 || sectorNum > Disk.NUM_OF_SECTORS)
			throw new IndexOutOfBoundsException("writing to incorrect sector");
		
		
		boolean found;
		// get transaction in atl, if found update buffer
		Transaction tempTran = atl.get(tid);
		found = tempTran.checkRead(sectorNum, buffer);
		
		// get transaction in wbl, if found update buffer
		if (!found) found = wbl.checkRead(sectorNum, buffer);
		
		// check the disk, update buffer
		int tag = log.CURRENT_TAG++;
		if(!found) disk.startRequest(Disk.READ, tag, sectorNum, buffer);
		// use callback tracker to wait
		DiskResult res = cbt.waitForTag(tag);
		if(res.getStatus() != DiskResult.OK) throw new IOException("disk fails to complete the read");
	}

	//-------------------------------------------------------
	//
	// Buffer the specified update as part of the in-memory
	// state of the specified transaction. Don't write
	// anything to disk yet.
	//  
	// Concurrency: The final value of a sector
	// must be the value written by the transaction that
	// commits the latest.
	//
	// Throws 
	// IllegalArgumentException if tid does not refer
	// to an active transaction or buffer is too small
	// to hold a sector.
	// 
	// IndexOutOfBoundsException if sectorNum is not
	// a valid sector number
	//
	//-------------------------------------------------------
	public void writeSector(TransID tid, int sectorNum, byte buffer[])
	throws IllegalArgumentException, 
	IndexOutOfBoundsException
	{
		if (buffer.length < Disk.SECTOR_SIZE) {
			throw new IllegalArgumentException("buffer size less then sector size");
		}
		if (sectorNum < Disk.ADISK_REDO_LOG_SECTORS+1 || sectorNum > Disk.NUM_OF_SECTORS)
			throw new IndexOutOfBoundsException("writing to incorrect sector");
		
		// look up Transaction by TransID in atl
		Transaction temp = atl.get(tid);
		
		// add write to transaction (addWrite)
		if (temp != null) {
			temp.addWrite(sectorNum, buffer);
		} else {
			throw new IllegalArgumentException("TransID not an active transaction");
		}
	}


}
