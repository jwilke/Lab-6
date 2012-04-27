
public class BitMap {
	byte bits[][];  //only two-dimensional for ease of writing to disk
	int total_sectors;
	int sec_div;
	int free_sectors;

	public BitMap(int ts) {
		total_sectors = ts;
		free_sectors = ts;
		sec_div = ts/(Disk.SECTOR_SIZE * 8);
		bits = new byte[sec_div][Disk.SECTOR_SIZE];
	}
	
	public void passBits(byte[][] b) {
		for(int i = 0; i < sec_div; i++) {
			for(int j = 0; j < Disk.SECTOR_SIZE; j++) {
				bits[i][j] = b[i][j];
			}
		}
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
		free_sectors--;
	}
	
	public void free_sector(int sector) {
		int curPart = sector / (Disk.SECTOR_SIZE * 8);
		int curByte = (sector % (Disk.SECTOR_SIZE * 8)) / 8;
		int curBit = sector % 8;
		bits[curPart][curByte] = (byte) (bits[curPart][curByte] & (-1 ^ (1 << curBit)));
		free_sectors++;
	}
	
	public int first_free_sector() {
		for(int i = 0; i < sec_div; i++) {
			for(int j = 0; j < Disk.SECTOR_SIZE; j++) {
				if ( bits[i][j] != -1) {
					int bit = 0;
					byte comp = bits[i][j];
					while( bit < 8) {
						if( (((byte)(1<<bit)) & comp) == 0 ) {
							int ret = (i*(Disk.SECTOR_SIZE * 8)) + (j*8) + bit;
							bits[i][j] = (byte) (bits[i][j] | (1 << bit));
							free_sectors--;
							return ret;
						}
						bit++;
					}
				}
			}
		}
		
		
		return -1;
	}
	
	public int first_free_block() {
		boolean found_first = false;
		for(int i = 0; i < sec_div; i++) {
			for(int j = 0; j < Disk.SECTOR_SIZE; j++) {
				if ( bits[i][j] != -1) {
					int bit = 0;
					byte comp = bits[i][j];
					while( bit < 8 ) {
						if( (((byte)(1<<bit)) & comp) == 0 ) {
							int ret = (i*(Disk.SECTOR_SIZE * 8)) + (j*8) + bit;
							if(found_first) {
								set_sector(ret);
								set_sector(ret-1);
								return ret-1;
							} else {
								found_first = true;
							}
						} else {
							found_first = false;
						}
						bit++;
					}
					found_first = false;
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

		t.set_method("set_sector");
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
		
		int j = bm2.first_free_sector();
		t.is_equal(1024, j);
		
		j = bm2.first_free_block();
		t.is_equal(1025, j);
		bm2.set_sector(1027);
		j = bm2.first_free_block();
		t.is_equal(1028, j);
		j = bm2.first_free_block();
		t.is_equal(1030, j);
	}
}
