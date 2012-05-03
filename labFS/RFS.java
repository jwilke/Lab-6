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
	
	//These will be our table of file descriptors. Perhaps a separate class for it?
	private TransID open_xid;	//stores TransID for a given fd
	private int open_in;
	private boolean avail_fd;
	
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
			
			DirEnt rootEnt = getRootEntry(id);
			rootEnt.addFile("..", root, true);
			rootEnt.addFile(".", root, true);
			
			disk.commitTrans(id);
		}
		
		open_xid = null;
		open_in = -1;
		avail_fd = true;
	}

	public int createFile(String filename, boolean openIt)
	throws IOException, IllegalArgumentException
	{
		String[] filePath = filename.split("/");
		
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
		if(openIt) {
			int fd = -1;
			try {
				fd = open(filename);
			} catch (IllegalArgumentException e) {
				return -1;
			}
			return fd;
		}
		
		return -1;
	}

	

	public void createDir(String dirname)
	throws IOException, IllegalArgumentException
	{
		String[] filePath = dirname.split("/");
		
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
		if(current == null) {
			throw new IllegalArgumentException();
		}
		// get new current
		DirEnt currentNew = getCurDir(id, oldName);
		if(current == null || currentNew == null) return;
		String[] filePathOld = oldName.split("/");
		
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
		String[] filePathNew = newName.split("/");
		currentNew.addFile(filePathNew[filePathNew.length - 1], inumber, isDir);
		
		disk.commitTrans(id);
	}
	
	private DirEnt getCurDir(TransID id, String filename) throws IllegalArgumentException, IOException {
		// parse filename
		String[] filePath = filename.split("/");
		
		// use directories to find file
		DirEnt current = getRootEntry(id);
		for( int i = 1; i < filePath.length - 1; i++) {
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
		if(!avail_fd) {
			throw new IllegalArgumentException();
		}
		TransID id = disk.beginTrans();
		String[] file = filename.split("/");
		
		DirEnt current = getCurDir(id, filename);
		if(current == null) {
			disk.abortTrans(id);
			throw new IllegalArgumentException();
		}
		
		int inum = current.get_next_File(file[file.length-1]);
		if(inum == -1) {
			disk.abortTrans(id);
			throw new IllegalArgumentException();
		}
		
		open_xid = id;
		open_in = inum;
		avail_fd = false;
		
		return 1;
	}


	public void close(int fd)
	throws IOException, IllegalArgumentException
	{
		if(avail_fd) {
			throw new IllegalArgumentException();
		}
		disk.commitTrans(open_xid);
		open_xid = null;
		open_in = -1;
		avail_fd = true;
	}


	public int read(int fd, int offset, int count, byte buffer[])
	throws IOException, IllegalArgumentException
	{
		//lookup transid and xid from the fd and pass to the ptree
		if(avail_fd == true && fd != 1) {
			throw new IllegalArgumentException();
		}
		
		return disk.read(open_xid, open_in, offset, count, buffer);
	}


	public void write(int fd, int offset, int count, byte buffer[])
	throws IOException, IllegalArgumentException
	{
		if(avail_fd == true && fd != 1) {
			throw new IllegalArgumentException();
		}
		
		disk.write(open_xid, open_in, offset, count, buffer);
	}

	public String[] readDir(String dirname)
	throws IOException, IllegalArgumentException
	{
		// get directory this file lives in
		TransID id = disk.beginTrans();
		DirEnt current = getCurDir(id, dirname);
		if(current == null) return null;
		
		// get the directory itself
		String[] filePath = dirname.split("/");
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
		if(avail_fd == true && fd != 1) {
			throw new IllegalArgumentException();
		}
		
		return disk.getTotalBlocks(open_in) * PTree.BLOCK_SIZE_BYTES;
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
