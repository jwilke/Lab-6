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
	
	public ActiveTransactionList() {
		// create empty list of Transactions
	}

    /*
     * You can alter or add to these suggested methods.
     */

    public void put(Transaction trans){
    	// lock.lock();
    	// add to list
        System.exit(-1); // TBD
    }

    public Transaction get(TransID tid){
    	// lock.lock();
    	// search list
    	// if found return transaction
    	// else return null
        System.exit(-1); // TBD
        return null;
    }

    public Transaction remove(TransID tid){
    	// lock.lock();
    	// search list
    	// if found delete and return transaction
    	// else return null
        System.exit(-1); // TBD
        return null;
    }

    
}