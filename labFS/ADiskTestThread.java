import java.io.IOException;

/**
 * used to test ADisk with multiple threads
 *
 */
public class ADiskTestThread extends Thread{
	int base_sector;
	int mult_sector;
	ADisk disk;
	byte c;
	ADiskTestThread(ADisk ad, int base, int mult, byte ch) {
		disk = ad;
		base_sector = base;
		mult_sector = mult;
		c = ch;
	}
	
	void set_base(int b) {
		base_sector = b;
	}
	
	void set_mult(int m) {
		mult_sector = m;
	}
	
	public void run() {
		int index = base_sector;
		TransID tid;
		byte[] buffer = new byte[Disk.SECTOR_SIZE];
		for(int i = 0; i < Disk.SECTOR_SIZE; i++) {
			buffer[i] = c;
		}
		while(index >= Disk.ADISK_REDO_LOG_SECTORS+1 && index < Disk.NUM_OF_SECTORS) {
			System.out.println(c + " " + index);
			tid = disk.beginTransaction();
			disk.writeSector(tid, index, buffer);
			//System.out.println("Here");
			try {
				disk.commitTransaction(tid, c);
			} catch (IllegalArgumentException e) {
				
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			index += mult_sector;
		}
	}
}
