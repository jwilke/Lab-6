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

	Disk disk;
	ActiveTransactionList atl;
	WriteBackList wbl;
	LogStatus log;
	CallbackTracker cbt;

	private static boolean TESTING = false;

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
		
		
		log = new LogStatus(disk, cbt);
		if(!format) log.recover(wbl);
		// else
		// create new disk from log passing cbt

		startWorker();
	}

	private void startWorker() {
		// create writeback worker
		if(!TESTING) {
			WriteBackWorker wbw = new WriteBackWorker(cbt, disk, log, wbl);
			wbw.start();
		}
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
	public void commitTransaction(TransID tid, int thread) 
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
			sector = Transaction.copyOfRange(dataToWrite, i*Disk.SECTOR_SIZE, (i+1)*Disk.SECTOR_SIZE);
			temp_tag = log.getNextTag();
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

	public void commitTransaction(TransID tid) throws IllegalArgumentException, IOException {
		commitTransaction(tid, -1);
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


		boolean found = false;
		// get transaction in atl, if found update buffer
		Transaction tempTran = atl.get(tid);
		if (tempTran != null) found = tempTran.checkRead(sectorNum, buffer);

		// get transaction in wbl, if found update buffer
		if (!found) found = wbl.checkRead(sectorNum, buffer);

		// check the disk, update buffer
		int tag = log.getNextTag();
		if(!found) { 
			disk.startRequest(Disk.READ, tag, sectorNum, buffer);
			DiskResult res = cbt.waitForTag(tag);
			if(res.getStatus() != DiskResult.OK) throw new IOException("disk fails to complete the read");
		}
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
		if (sectorNum < Disk.ADISK_REDO_LOG_SECTORS+1 || sectorNum > Disk.NUM_OF_SECTORS) {
			throw new IndexOutOfBoundsException("writing to incorrect sector");
		}

		// look up Transaction by TransID in atl
		Transaction temp = atl.get(tid);

		// add write to transaction (addWrite)
		if (temp != null) {
			temp.addWrite(sectorNum, buffer);
		} else {
			throw new IllegalArgumentException("TransID not an active transaction");
		}
	}




	public static void unit(Tester t, boolean first) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
		t.set_object("ADisk");
		t.is_equal(14, TransID.tranCurrent);
		TESTING = true;
		ADisk ad1 = new ADisk(true);

		if(first) {
			// getNSectors
			t.set_method("getNSectors()");
			t.is_equal(15359, ad1.getNSectors());
		}

	
		// beginTransaction
		t.set_method("beginTransaction");
		TransID tid1 = ad1.beginTransaction();
		t.is_equal(14, tid1.getTranNum());
		t.is_true(ad1.atl.get(tid1) != null);

		TransID tid2 = ad1.beginTransaction();
		t.is_equal(15, tid2.getTranNum());
		t.is_true(ad1.atl.get(tid2) != null);

		TransID tid3 = ad1.beginTransaction();
		t.is_equal(16, tid3.getTranNum());
		t.is_true(ad1.atl.get(tid3) != null);

		
		
		// abortTransaction
		t.set_method("abortTransaction");
		ad1.abortTransaction(tid1);
		t.is_true(ad1.atl.get(tid1) == null);
		t.is_true(ad1.atl.get(tid2) != null);
		t.is_true(ad1.atl.get(tid3) != null);

		ad1.abortTransaction(tid2);
		t.is_true(ad1.atl.get(tid1) == null);
		t.is_true(ad1.atl.get(tid2) == null);
		t.is_true(ad1.atl.get(tid3) != null);

		ad1.abortTransaction(tid3);
		t.is_true(ad1.atl.get(tid1) == null);
		t.is_true(ad1.atl.get(tid2) == null);
		t.is_true(ad1.atl.get(tid3) == null);





		tid1 = ad1.beginTransaction();
		tid2 = ad1.beginTransaction();
		tid3 = ad1.beginTransaction();


		// writeSector add to old transaction
		t.set_method("writeSector() - to old Transaction");
		byte[] buffer1 = new byte[Disk.SECTOR_SIZE];
		byte[] buffer0 = new byte[Disk.SECTOR_SIZE];

		for(int i = 0; i < buffer1.length; i++) {
			buffer1[i] = (byte) i;
		}
		Transaction tran1 = ad1.atl.get(tid1);
		ad1.writeSector(tid1, 2000, buffer1); // ************
		t.is_true(tran1.hasTranID(tid1));
		tran1.addWrite(2000, buffer1);
		tran1.getUpdateS(2000, buffer0);
		t.is_equal(buffer1, buffer0);


		byte[] buffer2 = new byte[Disk.SECTOR_SIZE];
		for(int i = 0; i < buffer2.length; i++) {
			buffer2[i] = (byte) (i+1);
		}
		Transaction tran2 = ad1.atl.get(tid2);
		ad1.writeSector(tid2, 2001, buffer2);
		t.is_true(tran2.hasTranID(tid2), "Tran");
		tran2.getUpdateS(2001, buffer0);
		t.is_equal(buffer2, buffer0, "buffer");

		byte[] buffer3 = new byte[Disk.SECTOR_SIZE];
		for(int i = 0; i < buffer1.length; i++) {
			buffer3[i] = (byte) (i+2);
		}
		Transaction tran3 = ad1.atl.get(tid3);
		ad1.writeSector(tid3, 2002, buffer3);
		t.is_true(tran3.hasTranID(tid3), "Tran");
		tran3.getUpdateS(2002, buffer0);
		t.is_equal(buffer3, buffer0, "buffer");





		// readSector from atl
		t.set_method("readSector() - from atl");
		ad1.readSector(tid1, 2000, buffer0);
		t.is_equal(buffer1, buffer0);
		ad1.readSector(tid2, 2001, buffer0);
		t.is_equal(buffer2, buffer0);
		ad1.readSector(tid3, 2002, buffer0);
		t.is_equal(buffer3, buffer0);


		

		// commitTransaction
		t.set_method("commitTransaction");
		byte[] buffer9 = new byte[Disk.SECTOR_SIZE];
		Common.intToByte(0, buffer9, 0);
		Common.intToByte(3, buffer9, 4);

		
		tran1 = ad1.atl.get(tid1);
		//Transaction tran1b  = tran1;
		t.is_true(tran1 != null);
		ad1.commitTransaction(tid1);
		tran1 = ad1.atl.get(tid1);
		t.is_true(tran1 == null);
		t.is_equal(tid1, ad1.wbl.getNextWriteback().getTransID());
		t.is_equal(0, ad1.log.logStartPoint());
		t.is_equal(3, ad1.log.logCurrent());
		ad1.disk.startRequest(Disk.READ, 2000, Disk.ADISK_REDO_LOG_SECTORS, buffer0);
		ad1.cbt.waitForTag(2000);
		t.is_equal(buffer9, buffer0);

		Common.intToByte(0, buffer9, 0);
		Common.intToByte(6, buffer9, 4);
		tran2 = ad1.atl.get(tid2);
		t.is_true(tran2 != null);
		ad1.commitTransaction(tid2);
		tran2 = ad1.atl.get(tid2);
		t.is_true(tran2 == null);
		ad1.wbl.removeNextWriteback();
		t.is_equal(tid2, ad1.wbl.getNextWriteback().getTransID());
		t.is_equal(0, ad1.log.logStartPoint());
		t.is_equal(6, ad1.log.logCurrent());
		ad1.disk.startRequest(Disk.READ, 2000, Disk.ADISK_REDO_LOG_SECTORS, buffer0);
		ad1.cbt.waitForTag(2000);
		t.is_equal(buffer9, buffer0);

		Common.intToByte(0, buffer9, 0);
		Common.intToByte(9, buffer9, 4);
		tran3 = ad1.atl.get(tid3);
		t.is_true(tran3 != null);
		ad1.commitTransaction(tid3);
		tran3 = ad1.atl.get(tid3);
		t.is_true(tran3 == null);
		ad1.wbl.removeNextWriteback();
		t.is_equal(tid3, ad1.wbl.getNextWriteback().getTransID());
		t.is_equal(0, ad1.log.logStartPoint());
		t.is_equal(9, ad1.log.logCurrent());
		ad1.disk.startRequest(Disk.READ, 2000, Disk.ADISK_REDO_LOG_SECTORS, buffer0);
		ad1.cbt.waitForTag(2000);
		t.is_equal(buffer9, buffer0);

		ad1.log.recoverySectorsInUse(0, 0);
		
		tid1 = ad1.beginTransaction();
		ad1.writeSector(tid1, 2001, buffer1);
		ad1.commitTransaction(tid1);
		tid2 = ad1.beginTransaction();
		ad1.writeSector(tid2, 2002, buffer2);
		ad1.commitTransaction(tid2);
		tid3 = ad1.beginTransaction();
		ad1.writeSector(tid3, 2003, buffer3);
		ad1.commitTransaction(tid3);


		// readSector from wbl
		t.set_method("readSectors() - from wbl");

		ad1.readSector(tid1, 2001, buffer0);
		t.is_equal(buffer1, buffer0);
		ad1.readSector(tid2, 2002, buffer0);
		t.is_equal(buffer2, buffer0);
		ad1.readSector(tid3, 2003, buffer0);
		t.is_equal(buffer3, buffer0);

		
		// test Recovery
		t.set_method("Test recovery");
		System.out.println("New Stuff");
		ad1 = new ADisk(false);
		
		t.is_equal(3, ad1.wbl.list.size());
		
		ad1.readSector(tid1, 2001, buffer0);
		t.is_equal(buffer1, buffer0);
		ad1.readSector(tid2, 2002, buffer0);
		t.is_equal(buffer2, buffer0);
		ad1.readSector(tid3, 2003, buffer0);
		t.is_equal(buffer3, buffer0);
		
		
		
		System.out.println("Testing ADisk - Test Set 2");
		TESTING = false;
		ad1 = new ADisk(true);

		tid1 = ad1.beginTransaction();
		ad1.writeSector(tid1, 2001, buffer1);
		ad1.commitTransaction(tid1);
		tid2 = ad1.beginTransaction();
		ad1.writeSector(tid2, 2002, buffer2);
		ad1.commitTransaction(tid2);
		tid3 = ad1.beginTransaction();
		ad1.writeSector(tid3, 2003, buffer3);
		ad1.commitTransaction(tid3);
		ad1.disk.addBarrier();
		ad1.disk.startRequest(Disk.READ, 5, 9, buffer0);
		ad1.cbt.waitForTag(5);

		// readSector from disk
		t.set_method("readSectors() - from disk");

		ad1.disk.startRequest(Disk.WRITE, 11, 3, buffer1);
		ad1.cbt.waitForTag(11);
		ad1.disk.startRequest(Disk.READ, 11, 3, buffer0);
		ad1.cbt.waitForTag(11);
		t.is_equal(buffer1, buffer0);

		ad1.readSector(tid1, 2001, buffer0);
		t.is_equal(buffer1, buffer0);

		ad1.readSector(tid1, 2002, buffer0);
		t.is_equal(buffer2, buffer0);

		ad1.readSector(tid1, 2003, buffer0);
		t.is_equal(buffer3, buffer0);

		/*ad1 = new ADisk(true);
		for (int i = 0; i < 80; i++) {
			ADiskTestThread test1 = new ADiskTestThread(ad1, Disk.ADISK_REDO_LOG_SECTORS+1, 8, (byte) ('a'));
			ADiskTestThread test2 = new ADiskTestThread(ad1, Disk.ADISK_REDO_LOG_SECTORS+2, 8, (byte) ('a'));
			ADiskTestThread test3 = new ADiskTestThread(ad1, Disk.ADISK_REDO_LOG_SECTORS+3, 8, (byte) ('a'));
			ADiskTestThread test4 = new ADiskTestThread(ad1, Disk.ADISK_REDO_LOG_SECTORS+4, 8, (byte) ('a')); 
			ADiskTestThread test5 = new ADiskTestThread(ad1, Disk.ADISK_REDO_LOG_SECTORS+5, 8, (byte) ('a'));
			ADiskTestThread test6 = new ADiskTestThread(ad1, Disk.ADISK_REDO_LOG_SECTORS+6, 8, (byte) ('a')); 
			ADiskTestThread test7 = new ADiskTestThread(ad1, Disk.ADISK_REDO_LOG_SECTORS+7, 8, (byte) ('a')); 
			ADiskTestThread test8 = new ADiskTestThread(ad1, Disk.ADISK_REDO_LOG_SECTORS+8, 8, (byte) ('a'));
			test1.start();
			test2.start();
			test3.start();
			test4.start();
			test5.start();
			test6.start();
			test7.start();
			test8.start();

			try {
				test1.join();
				test2.join();
				test3.join();
				test4.join();
				test5.join();
				test6.join();
				test7.join();
				test8.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("finished test: " + i + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		}*/

		

	}
}
