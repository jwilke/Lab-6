import java.util.ArrayList;
import java.util.Iterator;

/*
 * ActiveTransaction.java
 *
 * List of active transactions.
 *
 * You must follow the coding standards distributed
 * on the class web page.
 *
 * (C) 2011 Mike Dahlin
 *
 */
public class ActiveTransactionList{
	
	/*
	 * List of Transactions 
	 * LinkedList trans
	 * SimpleLock
	 */
	private ArrayList<Transaction> list;
	SimpleLock lock;
	
	public ActiveTransactionList() {
		list = new ArrayList<Transaction>();
		lock = new SimpleLock();
	}

    /*
     * You can alter or add to these suggested methods.
     */

    public void put(Transaction trans){
    	// lock
    	lock.lock();
    	
    	// add transaction to list
    	list.add(trans);
    	
    	lock.unlock();
    }

    public Transaction get(TransID tid){
    	// lock.lock();
    	lock.lock();
    	
    	// search list
    	Iterator<Transaction> it = list.iterator();
    	while(it.hasNext()) {
    		Transaction temp = it.next();
    		if(temp.hasTranID(tid)) {
    			// if found return transaction
    			lock.unlock();
    			return temp;
    		}
    	}
    	
    	// else return null
    	lock.unlock();
        return null;
    }

    public Transaction remove(TransID tid){
    	// lock.lock();
    	lock.lock();
    	
    	// search list
    	Iterator<Transaction> it = list.iterator();
    	while(it.hasNext()) {
    		Transaction temp = it.next();
    		if(temp.hasTranID(tid)) {
    			// if found delete and return transaction
    			it.remove();
    			lock.unlock();
    			return temp;
    		}
    	}

    	// else return null
        lock.unlock();
        return null;
    }

    
}