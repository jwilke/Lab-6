/*
 * WriteBackList.java
 *
 * List of commited transactions with pending writebacks.
 *
 * You must follow the coding standards distributed
 * on the class web page.
 *
 * (C) 2011 Mike Dahlin
 *
 */
public class WriteBackList{
	
	/*
	 * List of Transactions
	 * SimpleLock
	 */
	
	public WriteBackList() {
		// create an empty list of Transactions
		// create a SimpleLock
	}

    // 
    // You can modify and add to the interfaces
    //

    // Once a transaction is committed in the log,
    // move it from the ActiveTransactionList to 
    // the WriteBackList
    public void addCommitted(Transaction t){
    	// lock
    	// add Transaction to the end of the list
    }

    //
    // A write-back thread should process
    // writebacks in FIFO order.
    //
    // NOTE: Don't remove the Transaction from
    // the list until the writeback is done
    // (reads need to see them)!
    //
    // NOTE: Service transactions in FIFO order
    // so that if there are multiple writes
    // to the same sector, the write that is
    // part of the last-committed transaction "wins".
    //
    // NOTE: you need to use log order for commit
    // order -- the transaction IDs are assigned
    // when transactions are created, so commit
    // order may not match transaction ID order.
    //    
    public Transaction getNextWriteback(){
    	// lock
    	// get first item in list
        return null;
    }

    //
    // Remove a transaction -- its writebacks
    // are now safely on disk.
    //
    public Transaction removeNextWriteback(){
    	// lock
    	// get and delete first item in list
        return null;
    }

    //
    // Check to see if a sector has been written
    // by a committed transaction. If there
    // are multiple writes to the same sector,
    // be sure to return the last-committed write.
    //
    public boolean checkRead(int secNum, byte buffer[])
    throws IllegalArgumentException, 
           IndexOutOfBoundsException
    {
    	// lock
    	// search list for last committed to secNum
    	// if found update buffer and return true
        return false;
    }

    
    
}
