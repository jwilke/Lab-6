import java.io.IOException;

/**
 * This class will run in the background and remove items from the wbl that have
 * been written to the disk and to the log
 * 
 * Issues opperations to disk
 * 
 * It will use callbacktracker to monitor
 * 
 * Then remove from wbl
 * 
 * Garbage collect from log
 * 
 * Update Log Status
 *
 */
public class WriteBackWorker extends Thread{

	CallbackTracker cbt;
	Disk disk;
	LogStatus log;
	WriteBackList wbl;

	public WriteBackWorker(CallbackTracker cbt, Disk d, LogStatus ls, WriteBackList wbl) {
		this.cbt = cbt;
		this.disk = d;
		this.log = ls;
		this.wbl = wbl;
	}

	public void run() {
		while(true) {
			Transaction t = wbl.getNextWriteback();
			for(int i = 0; i < t.getNUpdatedSectors(); i++) {
				byte[] buffer = new byte[Disk.SECTOR_SIZE];
				int sec_num = t.getUpdateI(i, buffer);
				int tag = log.CURRENT_TAG++;
				if(sec_num != -1) {
					try {
						disk.startRequest(Disk.WRITE, tag, sec_num, buffer);
					} catch (Exception e) {}
				}
				cbt.waitForTag(tag);
			}
			wbl.removeNextWriteback();
			//log.recoverySectorsInUse(t.recallLogSectorStart(), t.recallLogSectorNSectors());
			log.writeBackDone(t.recallLogSectorStart(), t.recallLogSectorNSectors()+1);
		}
	}
}

