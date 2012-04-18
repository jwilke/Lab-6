
public class BitMap {
	private byte bits[][];
	int total_sectors;
	int sec_div;

	public BitMap(int ts) {
		total_sectors = ts;
		
		sec_div = ts/(Disk.SECTOR_SIZE * 8);
		bits = new byte[sec_div][Disk.SECTOR_SIZE];
	}

	public boolean get_sector(int n) {
		int bmsector = n / (Disk.SECTOR_SIZE * 8);
		int bmbyte = (n % (Disk.SECTOR_SIZE * 8)) / 8;
		int bmbit = n % 8;

		return !((bits[bmsector][bmbyte] & (1 << bmbit)) == 0);
	}

	public void set_sector(int sector) {
		int curPart = sector / (Disk.SECTOR_SIZE * 8);
		int curByte = (sector % (Disk.SECTOR_SIZE * 8)) / 8;
		int curBit = sector % 8;
		bits[curPart][curByte] = (byte) (bits[curPart][curByte] | (1 << curBit));
	}
	
	public void free_sector(int sector) {
		int curPart = sector / (Disk.SECTOR_SIZE * 8);
		int curByte = (sector % (Disk.SECTOR_SIZE * 8)) / 8;
		int curBit = sector % 8;
		bits[curPart][curByte] = (byte) (bits[curPart][curByte] & (-1 ^ (1 << curBit)));
	}
	
	public int first_free() {
		for(int i = 0; i < sec_div; i++) {
			for(int j = 0; j < Disk.SECTOR_SIZE; j++) {
				if ( bits[i][j] != -1) {
					int bit = 0;
					byte comp = bits[i][j];
					while( bit < 8) {
						if( (((byte)(1<<bit)) & comp) == 0 ) {
							int ret = (i*(Disk.SECTOR_SIZE * 8)) + (j*8) + bit;
							bits[i][j] = (byte) (bits[i][j] | (1 << bit));
							return ret;
						}
						bit++;
					}
				}
			}
		}
		
		
		return -1;
	}
	
	public byte[][] get_bits() {
		return bits;
	}

	public static void unit(Tester t) {
		t.set_object("BitMap");
		BitMap bm1 = new BitMap(16384);

		t.set_method("get_sector");
		bm1.set_sector(1);
		t.is_true(bm1.get_sector(1));
		bm1.set_sector(4987);
		t.is_true(bm1.get_sector(4987));
		bm1.set_sector(16383);
		t.is_true(bm1.get_sector(16383));
		bm1.set_sector(89);
		t.is_true(bm1.get_sector(89));
		bm1.set_sector(4096);
		t.is_true(bm1.get_sector(4096));
		
		
		bm1.free_sector(1);
		t.is_true(!bm1.get_sector(1));
		bm1.free_sector(4987);
		t.is_true(!bm1.get_sector(4987));
		bm1.free_sector(16383);
		t.is_true(!bm1.get_sector(16383));
		bm1.free_sector(89);
		t.is_true(!bm1.get_sector(89));
		bm1.free_sector(4096);
		t.is_true(!bm1.get_sector(4096));

		BitMap bm2 = new BitMap(16384);
		for(int i = 0; i < 1024; i++) {
			bm2.set_sector(i);
		}
		
		int j = bm2.first_free();
		t.is_equal(1024, j);
	}
}
