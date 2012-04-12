//import java.util.Arrays;

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

	/**
	 * Convert an int into 4 bytes
	 * @param src
	 * @param dest
	 * @param offset
	 */
	public static void intToByte(int src, byte dest[], int offset) {
		for(int i = 0; i < 4; i++) {
			dest[offset+3-i] = (byte) (src >>> (i*8));
		}
	}

	/**
	 * Convert a long into 8 bytes
	 * @param src
	 * @param dest
	 * @param offset
	 */
	public static void longToByte(long src, byte dest[], int offset) {
		for(int i = 0; i < 8; i++) {
			dest[offset+7-i] = (byte) (src >>> (i*8));
		}
	}

	/**
	 * convert a place in an array to an int
	 * @param buffer
	 * @param offset
	 * @return
	 */
	public static int byteToInt(byte buffer[], int offset) {
		int x = (int) buffer[offset];
		x &= 0xff;
		int out = x << 24;
		x = (int) buffer[offset+1];
		x &= 0xff;
		out = out | (x << 16);
		x = (int) buffer[offset+2];
		x &= 0xff;
		out = out | (x << 8);
		x = (int) buffer[offset+3];
		x &= 0xff;
		out = out | (x);
		return out;
	}

	// used for testing
	public static void printArray(byte[] b) {
		System.out.print("[");
		for (int i = 0; i < b.length-1; i++) {
			System.out.print(b[i] + ", ");
		}
		System.out.println(b[b.length-1] + "]");
	}

	/**
	 * Tests to see if two array are equal
	 * @param b1
	 * @param b2
	 * @return
	 */
	public static boolean arrayEquals(byte[] b1, byte[] b2) {
		if(b1 == null && b2 == null) return true;
		if(b1 == null || b2 == null) return false;

		boolean out = b1.length == b2.length;

		for (int i = 0; i < b1.length && out; i++) {
			out = b1[i] == b2[i];
		}

		return out;
	}

}

class Write{


	int secNum;
	byte cData[];


	/** 
	 * Constructor
	 * @param secNum
	 * @param cb
	 */
	public Write(int secNum, byte cb[]) {
		this.secNum = secNum;
		cData = new byte[cb.length];
		for (int i = 0; i < cb.length; i++) {
			cData[i] = cb[i];
		}
	}

	/**
	 * checks to see if the sector is the one refering to this write
	 * @param sn
	 * @return
	 */
	public boolean isSecNum(int sn) {
		return sn == secNum;
	}

	/**
	 * update the buffer with a new write
	 * @param secNum
	 * @param b
	 * @return
	 */
	public boolean updateBuffer(int secNum, byte b[]) {
		if (!isSecNum(secNum)) return false;

		for (int i = 0; i < cData.length && i < b.length; i++) {
			cData[i] = b[i];
		}
		return true;
	}

	/**
	 * read from the buffer
	 * @param secNum
	 * @param b
	 * @return
	 */
	public boolean copyFromBuffer(int secNum, byte b[]) {
		for (int i = 0; i < cData.length && i < b.length; i++) {
			b[i] = cData[i];
		}
		return true;
	}

	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (!(o instanceof Write)) return false;
		Write w = (Write) o;
		boolean result = secNum == w.secNum && cData.length == w.cData.length;
		if (result){
			for (int i = 0; i < cData.length; i++ ) {
				if(cData[i] != w.cData[i]) return false;
			}
		}
		return result;
	}

	public String toString() {
		return "Write[" + secNum + ", " + byteString(cData) + "]";
	}



	private String byteString(byte[] d) {
		String output = "{";

		for (int i = 0; i < d.length-1; i++) {
			output += d[i] + ", ";
		}
		output += d[d.length-1] + "}";

		return output;
	}

	public void unit(Tester t) {
		t.set_object("Write");
		Write w1;
		int sn1;
		byte[] b1;

		// Test Constructor
		sn1 = 0;
		b1 = new byte[5];
		for (byte i = 0; i < 5; i++ ) 
			b1[i] = i;
		w1 = new Write(sn1, b1);
		t.is_equal(sn1, w1.secNum, "secNum");
		for (int i = 0; i < b1.length ; i++ ) 
			t.is_equal(i, (int) w1.cData[i]);

		sn1 = 98762;
		b1 = new byte[100];
		for (byte i = 0; i < 100; i++ ) 
			b1[i] = (byte) (Math.random()*100);
		w1 = new Write(sn1, b1);
		t.is_equal(sn1, w1.secNum, "secNum");
		for (int i = 0; i < b1.length ; i++ ) 
			t.is_equal((int) b1[i], (int) w1.cData[i], "cData[" + i + "]");



		// Test isSecNum(int)
		t.set_method("isSecNum");
		t.is_true(w1.isSecNum(sn1));
		sn1 = 0;
		w1 = new Write(sn1, b1);
		t.is_true(w1.isSecNum(0));
		sn1 = 782;
		w1 = new Write(sn1, b1);
		t.is_true(w1.isSecNum(782));



		// Test update buffer
		t.set_method("updateBuffer()");
		sn1 = 1;
		for (byte i = 0; i < 100; i++ ) 
			b1[i] = i;
		w1 = new Write(sn1, b1);
		for (int i = 0; i < b1.length ; i++ ) 
			t.is_equal(i, (int) w1.cData[i], "cData[" + i + "]");

		b1 = new byte[50];
		for (byte i = 49; i >= 0; i-- ) 
			b1[49-i] = i;
		w1.updateBuffer(sn1, b1);
		for (int k = 49; k >= 0; k-- )  
			t.is_equal(k, (int) w1.cData[49-k], "cData[" + (49-k) + "]");
		for(int i = 50; i < w1.cData.length; i++ )
			t.is_equal(i, w1.cData[i], "cData[" + i + "]");
		//System.out.println(w1);
		t.is_true(!w1.updateBuffer(50, b1));

		// test equals()
		t.set_method("equals()");
		sn1 = 1;
		b1 = new byte[5];
		for (byte i = 0; i < 5; i++ ) 
			b1[i] = i;
		w1 = new Write(sn1, b1);
		t.is_true(w1.equals(w1));

		Write w2 = new Write(sn1, b1);
		t.is_true(w1.equals(w2));

		t.is_true(!w1.equals(null));

		b1 = new byte[5];
		for (byte i = 0; i < 5; i++ ) 
			b1[i] = (byte) (i+9);
		w2 = new Write(sn1, b1);
		t.is_true(!w1.equals(w2));



	}
}
