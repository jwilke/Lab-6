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

    // 
    // Return the index of the log sector where
    // the next transaction should go.
    //
    public int reserveLogSectors(int nSectors)
    {
        return -1;
    }

    //
    // The write back for the specified range of
    // sectors is done. These sectors may be safely 
    // reused for future transactions. (Circular log)
    //
    public int writeBackDone(int startSector, int nSectors)
    {
        return -1;
    }

    //
    // During recovery, we need to initialize the
    // LogStatus information with the sectors 
    // in the log that are in-use by committed
    // transactions with pending write-backs
    //
    public void recoverySectorsInUse(int startSector, int nSectors)
    {
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
    // when you fininish write-back for a transaction. 
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
    public int logStartPoint(){
        return -1;
    }
    
}