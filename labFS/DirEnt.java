import java.io.IOException;
import java.util.ArrayList;

/*
 * DirEnt -- fields of a directory entry. Feel free
 * to modify this class as desired.
 *
 * You must follow the coding standards distributed
 * on the class web page.
 *
 * (C) 2007 Mike Dahlin
 *
 */
public class DirEnt{
	public final static int MAX_NAME_LEN_CHAR = 16;
	private boolean valid;
	private int inum;
	private int num_files;
	private char name[] = new char[MAX_NAME_LEN_CHAR];
	private ArrayList<String> files;
	private ArrayList<Integer> fileInums;
	private ArrayList<Boolean> isDir;

	/*
	 * Meta Data
	 * 
	 * 0-31  - name
	 * 32-33 - inum
	 * 34-37 - num_files
	 * 38    - valid?
	 */

	public final static int FILES_ON_DISK = 35;
	/*
	 * Files on Disk
	 * 
	 * 0-31  - name
	 * 32-33 - inum
	 * 34	 - directory?
	 */

	public DirEnt(String n, int inumber) {
		char[] temp = n.toCharArray();
		for(int i = 0; i < temp.length && i < MAX_NAME_LEN_CHAR; i++) {
			name[i] = temp[i];
		}
		inum = inumber;
		valid = true;
		num_files = 0;
		files = new ArrayList<String>();
		fileInums = new ArrayList<Integer>();
		isDir = new ArrayList<Boolean>();
	}

	public DirEnt(int inumber, FlatFS disk, TransID id) throws IllegalArgumentException, IOException {
		// read meta data and build from that
		byte[] meta = new byte[PTree.METADATA_SIZE];
		disk.readFileMetadata(id, inumber, meta);
		for(int i = 0; i < 32; i+=2) {
			char c = (char) meta[i];
			c = (char) (c | (meta[i+1] << 8));
			name[i/2] = c;
		}
		
		files = new ArrayList<String>();
		fileInums = new ArrayList<Integer>();
		isDir = new ArrayList<Boolean>();

		inum = inumber;
		num_files = Common.byteToInt(meta, 34);

		valid = true;


		byte[] temp = new byte[FILES_ON_DISK * num_files];
		disk.read(id, inumber, 0, FILES_ON_DISK * num_files, temp);

		for(int i = 0; i < num_files; i++) {
			getFileFromByte(i, temp);
		}
	}
	
	public boolean change_parent(int inum) {
		int i = files.indexOf("..");
		if(i == -1) return false;
		
		fileInums.set(i, inum);
		
		return true;
	}
	
	public boolean isFile(String name) { //if this name is a file, return true
		int i = files.indexOf(name);
		if(i == -1)
			return false;
		
		return !isDir.get(i);
	}

	private void getFileFromByte(int index, byte[] file) {
		// read name
		StringBuilder n = new StringBuilder();
		for(int i = 0; i < 32; i+=2) {
			int c = file[index*FILES_ON_DISK + i];
			c = (c | (file[index*FILES_ON_DISK + i+1] << 8));
			if(c == 0) break;
			n.append((char) c);
		}

		// read inum
		int inum = file[index*FILES_ON_DISK + 32] & 0xFF;
		inum = inum | ((file[index*FILES_ON_DISK + 33] << 8) & 0xFFFF);

		// read isDir
		boolean dir = false;
		if(file[index*FILES_ON_DISK + 34] == 1) dir = true;

		addFile(n.toString(), inum, dir);
		num_files--;
	}

	private void addFileToByte(int index, byte[] file) {
		byte[] tempname;
		tempname = files.get(index).getBytes();
		for(int i = 0; i < tempname.length; i++) {
			file[index*FILES_ON_DISK+i*2] = tempname[i];
		}

		byte first = (byte) (fileInums.get(index) & 0xFF);
		byte second = (byte) ((fileInums.get(index)>>8) & 0x1);

		file[index*FILES_ON_DISK+32] = first;
		file[index*FILES_ON_DISK+33] = second;

		if(isDir.get(index)) {
			file[index*FILES_ON_DISK+34] = 1;
		}
	}

	public int get_next_Dir(String dname) {
		for(int i = 0; i < num_files; i++) {
			if(isDir.get(i)) {
				if(files.get(i).equals(dname)) {
					return fileInums.get(i);
				}
			}
		}
		return -1;
	}

	public int get_next_File(String fname) {
		for(int i = 0; i < num_files; i++) {
			if(!isDir.get(i)) {
				if(files.get(i).equals(fname)) {
					return fileInums.get(i);
				}
			}
		}
		return -1;
	}

	public void addFile(String n, int i, boolean dir) {
		if(files.contains(n)) {
			System.out.println("File " + n + " already exists in directory " + new String(name));
			throw new IllegalArgumentException();
		}
		files.add(n);
		fileInums.add(i);
		isDir.add(dir);
		num_files++;
	}

	public void deleteFile(String n) {
		int i = files.indexOf(n);
		if(i == -1) return;
		files.remove(i);
		fileInums.remove(i);
		isDir.remove(i);
		num_files--;
	}

	public int getInum() {
		return inum;
	}

	public int getNumFiles() {
		return num_files;
	}
	
	public String getName() {
		String s = "";
		for(char c : name)
			s+=c;
		return s;
	}

	public String[] get_list_files() {
		String[] ret = new String[files.size()];
		for(int i = 0; i < ret.length; i++) {
			ret[i] = files.get(i);
		}
		return ret;
	}

	public void print_to_disk(FlatFS disk, TransID id) throws IllegalArgumentException, IOException {
		byte[] file = new byte[num_files*FILES_ON_DISK];

		for(int i = 0; i < num_files; i++) {
			addFileToByte(i, file);
		}

		//System.out.println((new Tester()).arrayToString(file));
		disk.write(id, inum, 0, num_files*FILES_ON_DISK, file);
	}


	public static void unit(Tester t) throws IOException {
		t.set_object("DirEnt");

		// constructor(String, int)
		t.set_method("Constructor(String, int)");
		DirEnt dr1 = new DirEnt("directory", 1);
		t.is_equal(1, dr1.inum);
		t.is_equal(0, dr1.num_files);
		char[] charArr = "directory".toCharArray();
		int index;
		for(index = 0; index < charArr.length; index++) {
			t.is_equal(charArr[index], dr1.name[index]);
		}
		for( ; index < dr1.name.length; index++) {
			t.is_equal((char) 0, dr1.name[index]);
		}
		t.is_true(dr1.files != null);
		t.is_equal(0, dr1.files.size());
		t.is_true(dr1.fileInums != null);
		t.is_equal(0, dr1.fileInums.size());
		t.is_true(dr1.isDir != null);
		t.is_equal(0, dr1.isDir.size());



		

		// getInum
		t.set_method("getInum()");
		dr1 = new DirEnt("directory", 1);
		t.is_equal(1, dr1.getInum());
		dr1 = new DirEnt("directory", 511);
		t.is_equal(511, dr1.getInum());
		dr1 = new DirEnt("directory", 100);
		t.is_equal(100, dr1.getInum());




		// addFile
		dr1 = new DirEnt("directory", 1);
		dr1.addFile("a", 2, true);
		t.is_equal("a", dr1.files.get(0));
		t.is_equal(2, dr1.fileInums.get(0).intValue());
		t.is_equal(true, dr1.isDir.get(0));
		t.is_equal(1, dr1.num_files);

		dr1.addFile("b", 3, false);
		t.is_equal("b", dr1.files.get(1));
		t.is_equal(3, dr1.fileInums.get(1).intValue());
		t.is_equal(false, dr1.isDir.get(1));
		t.is_equal(2, dr1.num_files);

		dr1.addFile("c", 4, true);
		t.is_equal("c", dr1.files.get(2));
		t.is_equal(4, dr1.fileInums.get(2).intValue());
		t.is_equal(true, dr1.isDir.get(2));
		t.is_equal(3, dr1.num_files);

		dr1.addFile("d", 5, false);
		t.is_equal("d", dr1.files.get(3));
		t.is_equal(5, dr1.fileInums.get(3).intValue());
		t.is_equal(false, dr1.isDir.get(3));
		t.is_equal(4, dr1.num_files);

		dr1.addFile("e", 6, true);
		t.is_equal("e", dr1.files.get(4));
		t.is_equal(6, dr1.fileInums.get(4).intValue());
		t.is_equal(true, dr1.isDir.get(4));
		t.is_equal(5, dr1.num_files);

		//dr1.addFile("e", 6, true);
		//t.is_equal(5, dr1.num_files);





		// getNumFiles
		t.set_method("getNumFiles()");
		dr1 = new DirEnt("directory", 1);
		t.is_equal(0, dr1.getNumFiles());
		dr1.addFile("a", 2, true);
		t.is_equal(1, dr1.getNumFiles());
		dr1.addFile("b", 3, false);
		t.is_equal(2, dr1.getNumFiles());
		dr1.addFile("c", 4, true);
		t.is_equal(3, dr1.getNumFiles());
		dr1.addFile("d", 5, true);
		t.is_equal(4, dr1.getNumFiles());
		dr1.addFile("e", 6, true);
		t.is_equal(5, dr1.getNumFiles());
		dr1.addFile("f", 7, true);
		t.is_equal(6, dr1.getNumFiles());





		// deleteFile
		t.set_method("deleteFile(String)");
		dr1.deleteFile("f");
		t.is_equal(5, dr1.getNumFiles());
		t.is_equal(5, dr1.files.size());
		t.is_equal(5, dr1.fileInums.size());
		t.is_equal(5, dr1.isDir.size());
		dr1.deleteFile("f");
		t.is_equal(5, dr1.getNumFiles());
		t.is_equal(5, dr1.files.size());
		t.is_equal(5, dr1.fileInums.size());
		t.is_equal(5, dr1.isDir.size());
		dr1.deleteFile("a");
		t.is_equal(4, dr1.getNumFiles());
		t.is_equal(4, dr1.files.size());
		t.is_equal(4, dr1.fileInums.size());
		t.is_equal(4, dr1.isDir.size());
		t.is_equal("b", dr1.files.get(0));
		t.is_equal(3, dr1.fileInums.get(0).intValue());
		t.is_equal(false, dr1.isDir.get(0));



		// get_list_files
		t.set_method("get_list_files()");
		String[] fileNames = dr1.get_list_files();
		t.is_equal("b", fileNames[0]);
		t.is_equal("c", fileNames[1]);
		t.is_equal("d", fileNames[2]);
		t.is_equal("e", fileNames[3]);






		// get_next_Dir(String)
		t.set_method("get_next_dir(String)");
		dr1 = new DirEnt("directory", 1);
		dr1.addFile("aa", 2, true);
		dr1.addFile("bb", 3, false);
		dr1.addFile("cc", 4, true);
		dr1.addFile("dd", 5, false);
		dr1.addFile("ee", 6, true);
		dr1.addFile("ff", 7, false);
		dr1.addFile("g", 8, true);
		dr1.addFile("h", 9, false);
		dr1.addFile("i", 10, true);
		dr1.addFile("j", 11, false);
		dr1.addFile("k", 12, true);
		dr1.addFile("lucky", 13, false);

		t.is_equal(2, dr1.get_next_Dir("aa"));
		t.is_equal(-1, dr1.get_next_Dir("bb"));
		t.is_equal(4, dr1.get_next_Dir("cc"));
		t.is_equal(-1, dr1.get_next_Dir("dd"));
		t.is_equal(6, dr1.get_next_Dir("ee"));
		t.is_equal(-1, dr1.get_next_Dir("ff"));
		t.is_equal(8, dr1.get_next_Dir("g"));
		t.is_equal(-1, dr1.get_next_Dir("h"));
		t.is_equal(10, dr1.get_next_Dir("i"));
		t.is_equal(-1, dr1.get_next_Dir("j"));
		t.is_equal(12, dr1.get_next_Dir("k"));
		t.is_equal(-1, dr1.get_next_Dir("lucky"));
		t.is_equal(-1, dr1.get_next_Dir("monkey"));







		// get_next_file(String)
		t.set_method("get_next_Files(String)");
		t.is_equal(-1, dr1.get_next_File("aa"));
		t.is_equal(3, dr1.get_next_File("bb"));
		t.is_equal(-1, dr1.get_next_File("cc"));
		t.is_equal(5, dr1.get_next_File("dd"));
		t.is_equal(-1, dr1.get_next_File("ee"));
		t.is_equal(7, dr1.get_next_File("ff"));
		t.is_equal(-1, dr1.get_next_File("g"));
		t.is_equal(9, dr1.get_next_File("h"));
		t.is_equal(-1, dr1.get_next_File("i"));
		t.is_equal(11, dr1.get_next_File("j"));
		t.is_equal(-1, dr1.get_next_File("k"));
		t.is_equal(13, dr1.get_next_File("lucky"));
		t.is_equal(-1, dr1.get_next_File("monkey"));






		// addFileToByte(int byte[])
		t.set_method("addFileToByte()");
		byte[] b1 = new byte[35*dr1.num_files];
		for(index = 0; index < dr1.num_files; index++) {
			dr1.addFileToByte(index, b1);
			t.is_equal(((byte) 'a') + index, b1[+index*FILES_ON_DISK]);
			//for(int i = 1; i < 32; i++) {
			//	t.is_equal(0, b1[i+index*FILES_ON_DISK]);
			//}
			t.is_equal(2+index, b1[32+index*FILES_ON_DISK]);
			t.is_equal(0, b1[33+index*FILES_ON_DISK]);
			t.is_equal((2+index+1)%2, b1[34+index*FILES_ON_DISK]);
		}


		
		


		// getFileFromByte(int, byte[])
		t.set_method("getFileFromBytes");
		DirEnt dr2 = new DirEnt("directory", 1);
		dr2.getFileFromByte(0, b1);
		t.is_equal(0, dr2.num_files);
		t.is_equal(1, dr2.files.size());
		t.is_equal(1, dr2.fileInums.size());
		t.is_equal(1, dr2.isDir.size());
		t.is_equal("aa", dr2.files.get(0));
		t.is_equal(2, dr2.fileInums.get(0).intValue());
		t.is_equal(true, dr2.isDir.get(0));
		
		dr2.getFileFromByte(1, b1);
		t.is_equal(0, dr2.num_files);
		t.is_equal(2, dr2.files.size());
		t.is_equal(2, dr2.fileInums.size());
		t.is_equal(2, dr2.isDir.size());
		t.is_equal("bb", dr2.files.get(1));
		t.is_equal(3, dr2.fileInums.get(1).intValue());
		t.is_equal(false, dr2.isDir.get(1));
		
		dr2.getFileFromByte(2, b1);
		t.is_equal(0, dr2.num_files);
		t.is_equal(3, dr2.files.size());
		t.is_equal(3, dr2.fileInums.size());
		t.is_equal(3, dr2.isDir.size());
		t.is_equal("cc", dr2.files.get(2));
		t.is_equal(4, dr2.fileInums.get(2).intValue());
		t.is_equal(true, dr2.isDir.get(2));
		
		
		
		
		
		
		// print_to_disk
		t.set_method("print_to_disk");
		FlatFS ffs1 = new FlatFS(false);
		TransID id1 = ffs1.beginTrans();
		int inum1 = ffs1.createFile(id1);
		dr1.inum = inum1;
		dr1.print_to_disk(ffs1, id1);
		byte[] meta1 = RFS.format_metadata("directory", dr1.inum, dr1.num_files, true);
		ffs1.writeFileMetadata(id1, dr1.inum, meta1);
		ffs1.commitTrans(id1);
		id1 = ffs1.beginTrans();
		
		byte[] b2 = new byte[dr1.num_files*FILES_ON_DISK];
		ffs1.read(id1, dr1.inum, 0, dr1.num_files*FILES_ON_DISK, b2);
		t.is_equal(b1, b2);
		
		
		
		// constructor(int, FlatFS, TransID)
		t.set_method("constructor(int, FlatFS, TransID");
		dr2 = new DirEnt(dr1.inum, ffs1, id1);
		t.is_equal(dr1.inum, dr2.inum);
		t.is_equal(dr1.num_files, dr2.num_files);
		t.is_equal(dr1.name, dr2.name);
		for(int i = 0; i < dr1.num_files; i++) {
			//System.out.println(dr2.files.get(i));
			t.is_equal(dr1.files.get(i), dr2.files.get(i));
			t.is_equal(dr1.fileInums.get(i), dr2.fileInums.get(i));
			t.is_equal(dr1.isDir.get(i), dr2.isDir.get(i));
		}
		
		
		
		for(int i = 0; i < 10000; i++) {
			System.out.print("");
		}
	}
}
