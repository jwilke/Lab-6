import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
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
	LinkedList<Transaction> list;
	SimpleLock lock;
	Condition is_empty;
	
	public WriteBackList() {
		// create an empty list of Transactions
		list = new LinkedList<Transaction>();
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
    public Transaction getNextWriteback() {
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

    public static void unit(Tester t) throws IllegalArgumentException, IOException {
    	t.set_object("WriteBackList");
    	
    	// constructor
    	t.set_method("Constructor()");
    	WriteBackList wbl1 = new WriteBackList();
    	LinkedList<Transaction> ll1 = new LinkedList<Transaction>();
    	t.is_equal(ll1, wbl1.list, "list");
    	t.is_true(wbl1.lock != null, "lock");
    	t.is_true(wbl1.is_empty != null, "is_empty");
    	
    	
    	
    	// addCommitted
    	t.set_method("addCommitted");
    	Transaction tran1 = new Transaction();
    	tran1.rememberLogSectors(1, 1);
    	byte[] b1 = new byte[5];
    	int off = 100;
    	for (int i = 0; i < b1.length; i++) {
    		b1[i] = (byte) (off + i);
    	}
    	tran1.addWrite(1, b1);
    	tran1.commit();
    	
    	Transaction tran2 = new Transaction();
    	tran2.rememberLogSectors(2, 1);
    	byte[] b2 = new byte[5];
    	off += 100;
    	for (int i = 0; i < b2.length; i++) {
    		b2[i] = (byte) (off + i);
    	}
    	tran2.addWrite(2, b2);
    	tran2.commit();
    	
    	Transaction tran3 = new Transaction();
    	tran3.rememberLogSectors(3, 1);
    	byte[] b3 = new byte[5];
    	off += 100;
    	for (int i = 0; i < b3.length; i++) {
    		b3[i] = (byte) (off + i);
    	}
    	tran3.addWrite(3, b3);
    	tran3.commit();
    	
    	wbl1.addCommitted(tran1);
    	wbl1.addCommitted(tran2);
    	wbl1.addCommitted(tran3);
    	
    	t.is_equal(tran1, wbl1.list.get(0));
    	t.is_equal(tran2, wbl1.list.get(1));
    	t.is_equal(tran3, wbl1.list.get(2));
    	
    	
    	
    	// getNextWriteBack
    	t.set_method("getNextWriteBack()");
    	WriteBackList wbl2 = new WriteBackList();
    	wbl2.addCommitted(tran1);
    	t.is_equal(tran1, wbl1.getNextWriteback());
    	t.is_equal(tran1, wbl2.getNextWriteback());
    	wbl2.addCommitted(tran2);
    	t.is_equal(tran1, wbl2.getNextWriteback());
    	
    	
    	
    	// check read
    	t.set_method("checkRead()");
    	byte[] buffer = new byte[5];
    	wbl1.checkRead(1, buffer);
    	t.is_equal(b1, buffer);
    	wbl1.checkRead(2, buffer);
    	t.is_equal(b2, buffer);
    	wbl1.checkRead(3, buffer);
    	t.is_equal(b3, buffer);
    	
    	
    	
    	
    	
    	
    	// remove NextWriteBack
    	t.set_method("removeNextWriteback()");
    	t.is_equal(tran1, wbl1.removeNextWriteback());
    	t.is_equal(2, wbl1.list.size());
    	t.is_equal(tran2, wbl1.removeNextWriteback());
    	t.is_equal(1, wbl1.list.size());
    	t.is_equal(tran3, wbl1.removeNextWriteback());
    	t.is_equal(0, wbl1.list.size());
    	
    }
    
    public static void printArray(byte[] b) {
    	System.out.print("[");
    	for (int i = 0; i < b.length-1; i++)
    		System.out.print(b[i] + ", ");
    	System.out.println(b[b.length-1] + "]");
    }
    
}
