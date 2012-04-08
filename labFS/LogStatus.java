import java.io.IOException;
import java.util.concurrent.locks.Condition;

/*
 * LogStatus.java
 *
 * Keep track of where head of log is (where should next
 * committed transactio go); where tail is (where
 * has write-back gotten); and where recovery
 * should start.
 *
 * You must follow the coding standards distributed
 * on the class web page.
 *
 * (C) 2011 Mike Dahlin
 *
 */
public class LogStatus{

	/*
	 * int start
	 * int current
	 * int firstSecHeader = 2
	 * 
	 * bit map
	 */

	private int start;
	private int current;
	private byte[] bitmap;
	private SimpleLock lock;
	private Condition is_full;

	public int CURRENT_TAG = 0;
	private final int START_LOG = 0;
	private final int HEADER_LOC = Common.ADISK_REDO_LOG_SECTORS;
	private final int START_OFFSET = 10;

	/**
	 * Start off cold. Use other method for starting from log
	 */
	public LogStatus() {
		lock = new SimpleLock();
		is_full = lock.newCondition();

		start = START_LOG;
		current = START_LOG;
		bitmap = new byte[Common.ADISK_REDO_LOG_SECTORS/8];
		for(int i = 0; i < bitmap.length; i++) {
			bitmap[i] = 0;
		}
	}

	// 
	// Return the index of the log sector where
	// the next transaction should go.
	//
	public int reserveLogSectors(int nSectors)
	{
		lock.lock();
		// TODO find a way to wait if log is full
		// int oldCurent = current
		int begin = current;
		// current += nSectors
		current = ((current + START_LOG + nSectors) % Common.ADISK_REDO_LOG_SECTORS) - START_LOG;
		// set bitmap
		setBits(begin, nSectors);
		// return oldCurrrent
		lock.unlock();
		return begin;
	}

	/**
	 * Set the bits from the starting location to the end of length
	 * @param begin - the beginning location in terms of bits
	 * @param length - the length from start to end
	 */
	private void setBits(int begin, int length) {
		while(length > 0) {
			int curByte = begin / 8;
			for(int j = begin%8; j < 8 && length > 0; j++, begin++, length--) {
				bitmap[curByte] = (byte) (bitmap[curByte] | (1 << (7 - j)));
			}
			begin = begin % Common.ADISK_REDO_LOG_SECTORS;
		}

	}

	/**
	 * Set the bits in the bitmap to 0
	 * @param begin - the start location in terms of bits
	 * @param length - the length from start to end
	 */
	private void freeBits(int begin, int length) {
		while(length > 0) {
			int curByte = begin / 8;
			for(int j = begin%8; j < 8 && length > 0; j++, begin++, length--) {
				bitmap[curByte] = (byte) (bitmap[curByte] & (-1 ^ (1 << (7 - j))));
			}
			begin = begin % Common.ADISK_REDO_LOG_SECTORS;
		}
	}

	//
	// The write back for the specified range of
	// sectors is done. These sectors may be safely 
	// reused for future transactions. (Circular log)
	//
	public int writeBackDone(int startSector, int nSectors)
	{
		lock.lock();
		// update start if startSector = start
		if(startSector == start) 
			start += nSectors;
		// clear bits
		freeBits(startSector, nSectors);
		lock.unlock();
		return start; // ??? What do we want to return
	}

	//
	// During recovery, we need to initialize the
	// LogStatus information with the sectors 
	// in the log that are in-use by committed
	// transactions with pending write-backs
	//
	public void recoverySectorsInUse(int startSector, int nSectors)
	{
		// set start to startSector
		start = startSector;
		// set current to start+nSectors
		current = (start + nSectors) % Common.ADISK_REDO_LOG_SECTORS;
	}

	//
	// On recovery, find out where to start reading
	// log from. LogStatus should reserve a sector
	// in a well-known location. (Like the log, this sector
	// should be "invisible" to everything above the
	// ADisk interface.) You should update this
	// on-disk information at appropriate times.
	// Then, on recovery, you can read this information 
	// to find out where to start processing the log from.
	//
	// NOTE: You can update this on-disk info
	// when you finish write-back for a transaction. 
	// But, you don't need to keep this on-disk
	// sector exactly in sync with the tail
	// of the log. It can point to a transaction
	// whose write-back is complete (there will
	// be a bit of repeated work on recovery, but
	// not a big deal.) On the other hand, you must
	// make sure of three things: (1) it should always 
	// point to a valid header record; (2) if a 
	// transaction T's write back is not complete,
	// it should point to a point no later than T's
	// header; (3) reserveLogSectors must block
	// until the on-disk log-start-point points past
	// the sectors about to be reserved/reused.
	//
	public int logStartPoint(){ // added Disk d
		return start;
	}
	
	public int logCurrent() {
		// return current sector header
		return current;
	}

	public void writeCommit(Disk d, CallbackTracker cbt) throws IllegalArgumentException, IOException {
		lock.lock();
		// add commit to end
		int location = reserveLogSectors(1);
		
		// send header to disk
		int tag = CURRENT_TAG++;
		byte[] header = new byte[Disk.SECTOR_SIZE];
		Common.intToByte(start, header, 0);
		Common.intToByte(current, header, 4);
		d.startRequest(Disk.WRITE, tag, HEADER_LOC, header);
		lock.unlock();
		cbt.waitForTag(tag);
		lock.lock();
		// send commit to disk
		byte[] commit = new byte[Disk.SECTOR_SIZE];
		commit[0] = (byte) 'c';
		commit[1] = (byte) 'o';
		commit[2] = (byte) 'm';
		commit[3] = (byte) 'm';
		commit[4] = (byte) 'i';
		commit[5] = (byte) 't';
		tag = CURRENT_TAG++;
		d.startRequest(Disk.WRITE, tag, location, commit);
		lock.unlock();
		cbt.waitForTag(tag);
	}

	public static void unit(Tester t) {
		t.set_object("LogStatus");


		// Constructor
		t.set_method("Constructor()");
		LogStatus ls1= new LogStatus();
		t.is_equal(0, ls1.start, "start");
		t.is_equal(0, ls1.current, "current");
		t.is_true(ls1.lock instanceof SimpleLock, "lock");
		t.is_true(ls1.is_full instanceof Condition, "Condition");
		byte[] bit1 = new byte[1024/8];
		t.is_equal(bit1, ls1.bitmap);



		// setBits
		t.set_method("setBits()");
		ls1.setBits(11*8, 32);
		t.is_equal(0, ls1.bitmap[10]);
		t.is_equal(-1, ls1.bitmap[11]);
		t.is_equal(-1, ls1.bitmap[12]);
		t.is_equal(-1, ls1.bitmap[13]);
		t.is_equal(-1, ls1.bitmap[14]);
		t.is_equal(0, ls1.bitmap[15]);
		
		ls1.setBits(20*8, 4);
		t.is_equal(0, ls1.bitmap[19]);
		t.is_equal(-16, ls1.bitmap[20]);
		t.is_equal(0, ls1.bitmap[21]);
		
		ls1.setBits(22*8 + 6, 16);
		t.is_equal(0, ls1.bitmap[21]);
		t.is_equal(3, ls1.bitmap[22]);
		t.is_equal(-1, ls1.bitmap[23]);
		t.is_equal(-4, ls1.bitmap[24]);
		t.is_equal(0, ls1.bitmap[25]);
		
		ls1.setBits(126*8, 32);
		t.is_equal(0, ls1.bitmap[125]);
		t.is_equal(-1, ls1.bitmap[126]);
		t.is_equal(-1, ls1.bitmap[127]);
		t.is_equal(-1, ls1.bitmap[0]);
		t.is_equal(-1, ls1.bitmap[1]);
		t.is_equal(0, ls1.bitmap[2]);



		// freeBits
		t.set_method("setBits()");
		ls1.freeBits(11*8, 32);
		t.is_equal(0, ls1.bitmap[10]);
		t.is_equal(0, ls1.bitmap[11]);
		t.is_equal(0, ls1.bitmap[12]);
		t.is_equal(0, ls1.bitmap[13]);
		t.is_equal(0, ls1.bitmap[14]);
		t.is_equal(0, ls1.bitmap[15]);
		
		ls1.freeBits(20*8, 3);
		t.is_equal(0, ls1.bitmap[19]);
		t.is_equal(16, ls1.bitmap[20]);
		t.is_equal(0, ls1.bitmap[21]);
		
		ls1.freeBits(22*8 + 6, 3);
		t.is_equal(0, ls1.bitmap[21]);
		t.is_equal(0, ls1.bitmap[22]);
		t.is_equal(127, ls1.bitmap[23]);
		t.is_equal(-4, ls1.bitmap[24]);
		t.is_equal(0, ls1.bitmap[25]);
		
		ls1.freeBits(126*8, 24);
		t.is_equal(0, ls1.bitmap[125]);
		t.is_equal(0, ls1.bitmap[126]);
		t.is_equal(0, ls1.bitmap[127]);
		t.is_equal(0, ls1.bitmap[0]);
		t.is_equal(-1, ls1.bitmap[1]);
		t.is_equal(0, ls1.bitmap[2]);
		
		
		
		// reserveLogSectors
		t.set_method("reserveLogSectors(int)");
		ls1.bitmap = new byte[Common.ADISK_REDO_LOG_SECTORS/8];
		t.is_equal(0, ls1.reserveLogSectors(1));
		t.is_equal(0, ls1.start, "start");
		t.is_equal(1, ls1.current, "current");
		t.is_equal(-128, ls1.bitmap[0], "bitmap");
		
		t.is_equal(1, ls1.reserveLogSectors(100));
		t.is_equal(0, ls1.start);
		t.is_equal(101, ls1.current);
		t.is_equal(-1, ls1.bitmap[0], "bitmap");
		t.is_equal(-1, ls1.bitmap[1], "bitmap");
		t.is_equal(-1, ls1.bitmap[2], "bitmap");
		t.is_equal(-1, ls1.bitmap[3], "bitmap");
		t.is_equal(-1, ls1.bitmap[4], "bitmap");
		t.is_equal(-1, ls1.bitmap[5], "bitmap");
		t.is_equal(-1, ls1.bitmap[6], "bitmap");
		t.is_equal(-1, ls1.bitmap[7], "bitmap");
		t.is_equal(-1, ls1.bitmap[8], "bitmap");
		t.is_equal(-1, ls1.bitmap[9], "bitmap");
		t.is_equal(-1, ls1.bitmap[10], "bitmap");
		t.is_equal(-1, ls1.bitmap[11], "bitmap");
		t.is_equal(-8, ls1.bitmap[12], "bitmap");
		t.is_equal(0, ls1.bitmap[13], "bitmap");
		
		t.is_equal(101, ls1.reserveLogSectors(30));
		t.is_equal(0, ls1.start, "Start");
		t.is_equal(131, ls1.current, "current");
		t.is_equal(-1, ls1.bitmap[12], "bitmap");
		t.is_equal(-1, ls1.bitmap[13], "bitmap");
		t.is_equal(-1, ls1.bitmap[14], "bitmap");
		t.is_equal(-1, ls1.bitmap[15], "bitmap");
		t.is_equal(-32, ls1.bitmap[16], "bitmap");
		
		
		
		
		
		// writeBackDone
		t.set_method("writeBackDone");
		t.is_equal(1, ls1.writeBackDone(0, 1));
		t.is_equal(1, ls1.start, "start");
		t.is_equal(127, ls1.bitmap[0], "bitmap");
		
		t.is_equal(101, ls1.writeBackDone(1, 100));
		t.is_equal(101, ls1.start);
		t.is_equal(0, ls1.bitmap[0], "bitmap");
		t.is_equal(0, ls1.bitmap[1], "bitmap");
		t.is_equal(0, ls1.bitmap[2], "bitmap");
		t.is_equal(0, ls1.bitmap[3], "bitmap");
		t.is_equal(0, ls1.bitmap[4], "bitmap");
		t.is_equal(0, ls1.bitmap[5], "bitmap");
		t.is_equal(0, ls1.bitmap[6], "bitmap");
		t.is_equal(0, ls1.bitmap[7], "bitmap");
		t.is_equal(0, ls1.bitmap[8], "bitmap");
		t.is_equal(0, ls1.bitmap[9], "bitmap");
		t.is_equal(0, ls1.bitmap[10], "bitmap");
		t.is_equal(0, ls1.bitmap[11], "bitmap");
		t.is_equal(7, ls1.bitmap[12], "bitmap");
		t.is_equal(-1, ls1.bitmap[13], "bitmap");
		
		t.is_equal(131, ls1.writeBackDone(101,30));
		t.is_equal(131, ls1.start, "Start");
		t.is_equal(0, ls1.bitmap[12], "bitmap");
		t.is_equal(0, ls1.bitmap[13], "bitmap");
		t.is_equal(0, ls1.bitmap[14], "bitmap");
		t.is_equal(0, ls1.bitmap[15], "bitmap");
		t.is_equal(0, ls1.bitmap[16], "bitmap");
		
		
		// recoverySectorsInUse TODO
		t.set_method("recoverySectorsInUse()");
		ls1.recoverySectorsInUse(234, 700);
		t.is_equal(234, ls1.start, "start");
		t.is_equal(934, ls1.current, "current");
		ls1.recoverySectorsInUse(1, 599);
		t.is_equal(1, ls1.start, "start");
		t.is_equal(600, ls1.current, "current");
		ls1.recoverySectorsInUse(923, 100);
		t.is_equal(923, ls1.start, "start");
		t.is_equal(1023, ls1.current, "current");
		ls1.recoverySectorsInUse(950, 100);
		t.is_equal(950, ls1.start, "start");
		t.is_equal(26, ls1.current, "current");
		
		
		
		
		// logStartPoint
		t.set_method("logStartPoint()");
		ls1.recoverySectorsInUse(234, 700);
		t.is_equal(234, ls1.logStartPoint());
		ls1.recoverySectorsInUse(1, 599);
		t.is_equal(1, ls1.logStartPoint(), "start");
		ls1.recoverySectorsInUse(923, 100);
		t.is_equal(923, ls1.logStartPoint(), "start");
		ls1.recoverySectorsInUse(950, 100);
		t.is_equal(950, ls1.logStartPoint(), "start");
		
		// logCurrent
		t.set_method("logStartPoint()");
		ls1.recoverySectorsInUse(234, 700);
		t.is_equal(934, ls1.logCurrent(), "current");
		ls1.recoverySectorsInUse(1, 599);
		t.is_equal(600, ls1.logCurrent(), "current");
		ls1.recoverySectorsInUse(923, 100);
		t.is_equal(1023, ls1.logCurrent(), "current");
		ls1.recoverySectorsInUse(950, 100);
		t.is_equal(26, ls1.logCurrent(), "current");
		
		
		
		// writeCommit TODO



	}

}