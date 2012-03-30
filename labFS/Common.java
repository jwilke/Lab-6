import java.util.Arrays;

/*
 * NOTE: 
 * This file represents the constants and basic data struct to your 
 * file system.
 */
public class Common{

  public static final int ADISK_REDO_LOG_SECTORS = 1024;
  public static final int TREE_METADATA_SIZE = 32;
  
  public static final int MAX_TREES = 512;
  
  public static final int MAX_CONCURRENT_TRANSACTIONS = 8;
  public static final int MAX_WRITES_PER_TRANSACTION = 32;
  
  public static final int FS_MAX_NAME = 32;
  public static final int MAX_PATH = 128;
  
  public static final int MAX_FD = 32;
  
  public static final int READ = 0;
  public static final int WRITE = 1;
  
  public static final int COMMITED = 1;
  public static final int IN_PROGRESS = 0;
  public static final int ABORTED = -1;
  
  
  
}

class Write{
	

	 int secNum;
	 //byte oData[];
	 byte cData[];
	 int time;
	 
	
	public Write(int secNum, byte cb[]) {
		this.secNum = secNum;
		cData = new byte[cb.length];
		for (int i = 0; i < cb.length; i++) {
			cData[i] = cb[i];
		}
	}
	
	public boolean isSecNum(int sn) {
		return sn == secNum;
	}
	
	public boolean updateBuffer(int secNum, byte b[]) {
		if (!isSecNum(secNum)) return false;
		for (int i = 0; i < cData.length && i < b.length; i++) {
			b[i] = cData[i];
		}
		return true;
	}
}
