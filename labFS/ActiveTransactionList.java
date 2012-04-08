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
    
    public void unit(Tester t) {
    	t.set_object("ActiveTransactionList");
    	
    	
    	// test constructor
    	t.set_method("Constructor");
    	ActiveTransactionList atl = new ActiveTransactionList();
    	t.is_equal(0, atl.list.size());
    	ArrayList<Transaction> arrList1 = new ArrayList<Transaction>();
    	t.is_equal(arrList1, atl.list);
    	
    	
    	//test put
    	t.set_method("put()");
    	
    	Transaction tran1 = new Transaction();
    	TransID id1 = tran1.getTransID();
    	int sn1 = 1;
    	byte[] b1 = new byte[5];
    	for (byte i = 0; i < 5; i++)
    		b1[i] = i;
    	tran1.addWrite(sn1, b1);
    	atl.put(tran1);
    	
    	t.is_equal(tran1, atl.list.get(0));
    	
    	Transaction tran2 = new Transaction();
    	TransID id2 = tran2.getTransID();
    	int sn2 = 1;
    	byte[] b2 = new byte[5];
    	for (byte i = 0; i < 5; i++)
    		b1[i] = (byte) (i+100);
    	tran2.addWrite(sn2, b2);
    	atl.put(tran2);
    	
    	t.is_equal(tran2, atl.list.get(1));
    	
    	Transaction tran3 = new Transaction();
    	TransID id3 = tran3.getTransID();
    	int sn3 = 1;
    	byte[] b3 = new byte[5];
    	for (byte i = 0; i < 5; i++)
    		b1[i] = (byte) (i+200);
    	tran2.addWrite(sn3, b3);
    	atl.put(tran3);
    	
    	t.is_equal(tran3, atl.list.get(2));
    	
    	
    	
    	
    	//test get
    	t.set_method("get()");
    	t.is_equal(tran1, atl.get(id1));
    	t.is_equal(tran2, atl.get(id2));
    	t.is_equal(tran3, atl.get(id3));
    	
    	
    	
    	//test remove
    	t.set_method("remove()");
    	t.is_equal(tran1, atl.remove(id1));
    	t.is_equal(2, atl.list.size());
    	
    	t.is_equal(null, atl.remove(id1));
    	t.is_equal(2, atl.list.size());
    	t.is_equal(tran2, atl.remove(id2));
    	t.is_equal(1, atl.list.size());
    	t.is_equal(tran3, atl.remove(id3));
    	t.is_equal(0, atl.list.size());
    	
    	
    	
    	
    	
    	
    }

    
}