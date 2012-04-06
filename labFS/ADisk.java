/*
 * ADisk.java
 *
 * You must follow the coding standards distributed
 * on the class web page.
 *
 * (C) 2007, 2010 Mike Dahlin
 *
 */
import java.io.IOException;

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
	  // if format == true 
	  // wipe disk, write zeros to all
	  // else
	  // create new disk from log passing cbt
	  
	  /*
	   * allocate:
	   * atl
	   * wbl
	   * log
	   */
	  
	  // create writeback worker
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
	  return -1; // Fixme
  } 

  //-------------------------------------------------------
  //
  // Begin a new transaction and return a transaction ID
  //
  //-------------------------------------------------------
  public TransID beginTransaction()
  {
	  // Transaction tran = new Transaction
	  // ATL.put(tran)
	  return null; // Fixme
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
	  // change status
	  // ask log status where to put the log
	  // find place in log
	  // issue writes to log
	  // issue barrier to log
	  // issue commit to log
	  // wait for log to finish getting the commit (maybe CallBackTracker)
	  // move it from atl.remove(tid) to wbl.addCommitted()
	  
	  // wait for tag
	  // remove from wbl
	  // free in log
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
	  //atl.remove(tid)
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
	  // get transaction in atl, if found update buffer
	  // get transaction in wbl, if found update buffer
	  // check the disk, update buffer
	  // use callback tracker to wait
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
	  // look up Transaction by TransID in atl
	  // add write to transaction (addWrite)
	  // otherwise create a new one
  }


}
