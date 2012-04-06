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

import java.util.Vector;

public class CallbackTracker implements DiskCallback{
	
	/*
	 * List of DiskResults
	 * SimpleLock lock
	 */
	
    public void requestDone(DiskResult result){
        // save result
    	// broadcast
    }

    //
    // Wait for one tag to be done
    //
    public DiskResult waitForTag(int tag){
        // check for tag in all list
    	// if not there wait
    	// if there: remove for list, return result
        return null;
    }

    //
    // Wait for a set of tags to be done
    //
    public Vector waitForTags(Vector tags){
        // TBD
        return null;
    }

    //
    // To avoid memory leaks, need to tell CallbackTracker
    // if there are tags that we don't plan to wait for.
    // When these results arrive, drop them on the
    // floor.
    //
    public void dontWaitForTag(int tag){
        // TBD
    }

    public void dontWaitForTags(Vector tags){
        // TBD
    }



}