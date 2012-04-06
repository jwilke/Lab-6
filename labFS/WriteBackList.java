import java.util.ArrayList;
import java.util.concurrent.locks.Condition;

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
	ArrayList<Transaction> list;
	SimpleLock lock;
	Condition is_empty;
	
	public WriteBackList() {
		// create an empty list of Transactions
		list = new ArrayList<Transaction>();
		// create a SimpleLock
		lock = new SimpleLock();
		is_empty = lock.newCondition();
	}
	
	/*
	public void activateWorker(CallbackTracker cbt) {
		// create worker
	}
	 */

    // 
    // You can modify and add to the interfaces
    //

    // Once a transaction is committed in the log,
    // move it from the ActiveTransactionList to 
    // the WriteBackList
    public void addCommitted(Transaction t){
    	// lock
    	lock.lock();
    	
    	// add transaction to list
    	list.add(t);
    	is_empty.signalAll();
    	
    	lock.unlock();
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
    	lock.lock();
    	// get first item in list
    	while(list.size() == 0) {
			try {
				is_empty.await();
			} catch (InterruptedException e) {}
    	}
    	
    	Transaction t = list.get(0);
    	
    	lock.unlock();
    	return t;
    }

    //
    // Remove a transaction -- its writebacks
    // are now safely on disk.
    //
    public Transaction removeNextWriteback(){
    	// lock
    	lock.lock();
    	// get and delete first item in list
    	Transaction t = list.remove(0);
    	
    	lock.unlock();
        return t;
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
    	lock.lock();
    	Transaction t;
    	boolean found = false;
    	
    	// search list for last committed to secNum
    	for(int i = list.size()-1; i >= 0; i--) {
    		t = list.get(i);
    		if(t.getUpdateS(secNum, buffer)) {
    			found = true;
    			break;
    		}
    	}
    	
    	// if found update buffer and return true
    	lock.unlock();
        return found;
    }

    
    
}
