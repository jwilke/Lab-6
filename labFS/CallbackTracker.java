/*
 * CallbackTracker.java
 *
 * Wait for a particular tag to finish...
 *
 * You must follow the coding standards distributed
 * on the class web page.
 *
 * (C) 2011 Mike Dahlin
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.locks.Condition;

public class CallbackTracker implements DiskCallback{
	
	
	
	SimpleLock lock;
	HashMap<Integer, DiskResult> list;
	ArrayList<Integer> dontWaits;
	Condition tagBroadcaster;
	
	public CallbackTracker() {
		lock = new SimpleLock();
		list = new HashMap<Integer, DiskResult>();
		dontWaits = new ArrayList<Integer>();
		tagBroadcaster = lock.newCondition();
	}
	
	
    public void requestDone(DiskResult result){
    	lock.lock();
    	
    	if(dontWaits.contains(result.getTag())) {
    		dontWaits.remove(result.getTag());
    		lock.unlock();
    		return;
    	}
    	
    	
        // save result
    	list.put(result.getTag(), result);
    	// broadcast
    	tagBroadcaster.notifyAll();
    	
    	
    	lock.unlock();
    }

    //
    // Wait for one tag to be done
    //
    public DiskResult waitForTag(int tag) {
    	lock.lock();
        // check for tag in all list
    	while(!list.containsKey(tag)) {
    		// if not there wait
    		try {
				tagBroadcaster.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
    	
    	DiskResult ret = list.get(tag);
    	// if there: remove for list, return result
    	lock.unlock();
        return ret;
    }

    //
    // Wait for a set of tags to be done
    //
    public Vector<DiskResult> waitForTags(Vector<Integer> tags){
    	lock.lock();
    	Iterator<Integer> iter = tags.iterator();
    	Vector<DiskResult> ret = new Vector<DiskResult>();
    	int ctag;
    	while(iter.hasNext()) {
    		ctag = iter.next();
    		while(!list.containsKey(ctag)) {
        		// if not there wait
        		try {
    				tagBroadcaster.await();
    			} catch (InterruptedException e) {
    				e.printStackTrace();
    			}
        	}
    		ret.add(list.get(ctag));
    	}
    	lock.unlock();
        return ret;
    }

    //
    // To avoid memory leaks, need to tell CallbackTracker
    // if there are tags that we don't plan to wait for.
    // When these results arrive, drop them on the
    // floor.
    //
    public void dontWaitForTag(int tag){
    	lock.lock();
    	dontWaits.add(tag);
    	lock.unlock();
    }

    public void dontWaitForTags(Vector<Integer> tags){
    	lock.lock();
    	Iterator<Integer> iter = tags.iterator();
    	while(iter.hasNext()) {
    		dontWaits.add(iter.next());
    	}
    	lock.unlock();
    }
}
