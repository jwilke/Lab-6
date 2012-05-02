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
	}
	
	public DirEnt(int inumber, FlatFS disk, TransID id) throws IllegalArgumentException, IOException {
		// read meta data and build from that
		byte[] meta = new byte[PTree.METADATA_SIZE];
		disk.readFileMetadata(id, inumber, meta);
		for(int i = 0; i < 32; i+=2) {
			char c = (char) meta[i];
			c = (char) (c & (meta[i+1] << 8)); // TODO maybe flip
			name[i/2] = c;
		}
		System.out.println(name);
		
		inum = inumber;
		num_files = Common.byteToInt(meta, 34);
		valid = true;
		
		
		// read blocks and get included files
		byte[] temp = new byte[FILES_ON_DISK];
		for(int i = 0; i < num_files; i++) {
			disk.read(id, inum, i * FILES_ON_DISK, FILES_ON_DISK, temp);
			read_file_data(temp);
		}
	}
	
	private void read_file_data(byte[] file) {
		// read name
		String n = "";
		for(int i = 0; i < 32; i+=2) {
			char c = (char) file[i];
			c = (char) (c & (file[i+1] << 8)); // TODO maybe flip
			n += c;
		}
		
		// read inum
		int inum = file[32];
		inum = inum & (file[33] << 8);
		
		// read isDir
		boolean dir = false;
		if(file[34] == 1) dir = true;
		
		files.add(n);
		fileInums.add(inum);
		isDir.add(dir);
	}
	
	private byte[] write_file_data() {
		byte[] file = new byte[num_files*FILES_ON_DISK];
		
		byte[] tempname;
		for(int x = 0; x < num_files; x++) {
			tempname = files.get(x).getBytes();
			for(int i = 0; i < tempname.length; i++) {
				file[x*FILES_ON_DISK+i] = tempname[i];
			}
			
			byte first = (byte) (fileInums.get(x) & 0xFF);
			byte second = (byte) ((fileInums.get(x)>>8) & 0x1);
			
			file[x*FILES_ON_DISK+32] = first;
			file[x*FILES_ON_DISK+33] = second;
			
			if(isDir.get(x)) {
				file[x*FILES_ON_DISK+34] = 1;
			}
		}
		
		return file;
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
		files.add(n);
		fileInums.add(i);
		isDir.add(dir);
		num_files++;
	}

	public int getInum() {
		return inum;
	}

	public int getNumFiles() {
		return num_files;
	}
	
	public String[] get_list_files() {
		return (String[]) files.toArray();
	}
	
	public void print_to_disk(FlatFS disk, TransID id) throws IllegalArgumentException, IOException {
		disk.write(id, inum, 0, num_files*FILES_ON_DISK, write_file_data());
	}
}
