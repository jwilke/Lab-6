/*
 * RFS -- reliable file system
 *
 * You must follow the coding standards distributed
 * on the class web page.
 *
 * (C) 2007 Mike Dahlin
 *
 */
import java.io.IOException;
import java.io.EOFException;
public class RFS{

	private FlatFS disk;
	private final int root = 0;
	
	/*
	 * Meta Data
	 * 
	 * 0-31  - name
	 * 32-33 - inum
	 * 34-37 - num_files
	 * 38    - valid?
	 */

	public RFS(boolean doFormat)
	throws IOException
	{
		disk = new FlatFS(doFormat);
		if(doFormat) {
			TransID id = disk.beginTrans();
			int i = disk.createFile(id);
			if(i != root) { throw new IllegalStateException("Root was not set up properly"); }
			
			byte[] buff = format_metadata("root", root, 0, true);
			
			disk.writeFileMetadata(id, root, buff);
			
			disk.commitTrans(id);
		}
	}

	public int createFile(String filename, boolean openIt)
	throws IOException, IllegalArgumentException
	{
		String[] filePath = filename.split("//"); // TODO error checking
		for(String a: filePath) 
			System.out.println(a);
		
		TransID id = disk.beginTrans();
		DirEnt current = getCurDir(id, filename);
		if(current == null) return -1;
		
		// create file and get inum
		int inum = disk.createFile(id);
		
		byte[] meta = format_metadata(filePath[filePath.length - 1], inum, 0, false); 
		disk.writeFileMetadata(id, inum, meta);
		
		// TODO add file to directory
		current.addFile(filePath[filePath.length -1], inum, false);
		current.print_to_disk(disk, id);
		
		//commit to disk
		disk.commitTrans(id);
		
		// if openIt then return file description
		if(openIt) return inum;
		
		return -1;
	}

	

	public void createDir(String dirname)
	throws IOException, IllegalArgumentException
	{
		String[] filePath = dirname.split("//"); // TODO error checking
		for(String a: filePath) 
			System.out.println(a);
		
		TransID id = disk.beginTrans();
		DirEnt current = getCurDir(id, dirname);
		if(current == null) return;
		
		// create file and get inum
		int inum = disk.createFile(id);
		
		DirEnt dir = new DirEnt(filePath[filePath.length-1], inum);
		dir.addFile("..", current.getInum(), true);
		dir.addFile(".", inum, true);
		
		// write files to disk
		byte[] meta = format_metadata(filePath[filePath.length - 1], inum, 2, true); 
		disk.writeFileMetadata(id, inum, meta);
		dir.print_to_disk(disk, id);
		
		//  add file to directory
		current.addFile(filePath[filePath.length -1], inum, true);
		meta = format_metadata(filePath[filePath.length - 1], current.getInum(), current.getNumFiles(), true);
		
		// write current to disk && metadata
		current.print_to_disk(disk, id);
		disk.writeFileMetadata(id, current.getInum(), meta);
		
		//commit to disk
		disk.commitTrans(id);
	}



	public void unlink(String filename)
	throws IOException, IllegalArgumentException
	{
		TransID id = disk.beginTrans();
		DirEnt current = getCurDir(id, filename);
		if(current == null) return;
		
		int inumber = current.get_next_File(filename);
		current.deleteFile(filename);
		disk.deleteFile(id, inumber);
		disk.commitTrans(id);
	}

	public void rename(String oldName, String newName)
	throws IOException, IllegalArgumentException
	{
		// get current directory this file lives in
		TransID id = disk.beginTrans();
		DirEnt current = getCurDir(id, oldName);
		if(current == null) return;
		// get new current
		DirEnt currentNew = getCurDir(id, oldName);
		if(current == null || currentNew == null) return;
		String[] filePathOld = oldName.split("//"); // TODO error checking
		
		// get the file information
		boolean isDir = true;
		int inumber = current.get_next_Dir(filePathOld[filePathOld.length]);
		if(inumber == -1) {
			isDir = false;
			inumber = current.get_next_File(filePathOld[filePathOld.length]);
		}
		if(inumber == -1) return;
		
		// delete the old file
		current.deleteFile(filePathOld[filePathOld.length]);
		
		// create the new file
		String[] filePathNew = newName.split("//"); // TODO error checking
		currentNew.addFile(filePathNew[filePathNew.length - 1], inumber, isDir);
		
		disk.commitTrans(id);
	}
	
	private DirEnt getCurDir(TransID id, String filename) throws IllegalArgumentException, IOException {
		// parse filename
		String[] filePath = filename.split("//"); // TODO error checking
		for(String a: filePath) 
			System.out.println(a);
		
		// use directories to find file
		DirEnt current = getRootEntry(id);
		for( int i = 0; i < filePath.length - 1; i++) {
			int next = current.get_next_Dir(filePath[i]);
			if(next == -1) {
				System.out.println("Directory does not exist.");
				disk.abortTrans(id);
				return null;
			}
			current = new DirEnt(next, disk, id);
		}
		return current;
	}


	public int open(String filename)
	throws IOException, IllegalArgumentException
	{
		return -1;
	}


	public void close(int fd)
	throws IOException, IllegalArgumentException
	{
	}


	public int read(int fd, int offset, int count, byte buffer[])
	throws IOException, IllegalArgumentException
	{
		return -1;
	}


	public void write(int fd, int offset, int count, byte buffer[])
	throws IOException, IllegalArgumentException
	{
	}

	public String[] readDir(String dirname)
	throws IOException, IllegalArgumentException
	{
		// get directory this file lives in
		TransID id = disk.beginTrans();
		DirEnt current = getCurDir(id, dirname);
		if(current == null) return null;
		
		// get the directory itself
		String[] filePath = dirname.split("//"); // TODO error checking
		int inumber = current.get_next_Dir(filePath[filePath.length - 1]);
		if(inumber == -1) return null;
		current = new DirEnt(inumber, disk, id);
		
		// get string array
		String[] ret = current.get_list_files();
		
		disk.commitTrans(id);
		
		return ret;
	}

	public int size(int fd)
	throws IOException, IllegalArgumentException
	{
		return -1;
	}

	public int space(int fd)
	throws IOException, IllegalArgumentException
	{
		return -1;
	}

	private DirEnt getRootEntry(TransID id) throws IllegalArgumentException, IOException {
		return new DirEnt(root, disk, id);
	}

	public static byte[] format_metadata(String name, int inumber, int num_files, boolean val) {
		byte[] data = new byte[PTree.METADATA_SIZE];
		byte[] ndata = name.getBytes();
		for(int i =0 ; i < 32 && i/2 < ndata.length; i+=2) {
			data[i] = ndata[i/2];
		}
		
		byte first = (byte) (inumber & 0xFF);
		byte second = (byte) ((inumber>>8) & 0x1);
		
		data[32] = first;
		data[33] = second;
		
		Common.intToByte(num_files, data, 34);
		
		if(val) {
			data[38] = 1;
		}
		
		return data;
	}
	
	public static void unit(Tester t) {
		t.set_object("RFS");
		
		// format_metadata(String, int, int, boolean)
		t.set_method("format_metadata(String, int, int, boolean)");
		byte[] meta1 = RFS.format_metadata("root", 0, 2, true);
		for(int i = 0; i < "root".length(); i++) {
			t.is_equal((byte) "root".charAt(i), meta1[i*2]);
		}
		t.is_equal(0, meta1[32]);
		t.is_equal(0, meta1[33]);
		t.is_equal(2, Common.byteToInt(meta1, 34));
		t.is_equal(1, meta1[38]);
		
		meta1 = RFS.format_metadata("directory", 127, 23, true);
		for(int i = 0; i < "directory".length(); i++) {
			t.is_equal((byte) "directory".charAt(i), meta1[i*2]);
		}
		t.is_equal(127, meta1[32]);
		t.is_equal(0, meta1[33]);
		t.is_equal(23, Common.byteToInt(meta1, 34));
		t.is_equal(1, meta1[38]);
		
		meta1 = RFS.format_metadata("file", 27, 0, false);
		for(int i = 0; i < "file".length(); i++) {
			t.is_equal((byte) "file".charAt(i), meta1[i*2]);
		}
		t.is_equal(27, meta1[32]);
		t.is_equal(0, meta1[33]);
		t.is_equal(0, Common.byteToInt(meta1, 34));
		t.is_equal(0, meta1[38]);
		
		
		
		
		
		// constructor(boolean)
		// getRootEntry(TransID)
		// getCurDirDir(TransID, String)
		// createFile(String, boolean)
		// createDir(String)
		// unlink(String)
		// rename(string, String)
		// size
		// space
		// readDir
		// open(string)
		// close(int)
		// read
		// write
		
	}
}
