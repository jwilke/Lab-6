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

			DirEnt rootEnt = getRootEntry(id);
			rootEnt.addFile(".", root, true);
			rootEnt.addFile("..", root, true);
			rootEnt.print_to_disk(disk, id);

			byte[] buff = format_metadata("root", root, rootEnt.getNumFiles(), true);

			disk.writeFileMetadata(id, root, buff);

			disk.commitTrans(id);
		}

		open_xid = null;
		open_in = -1;
		avail_fd = true;
	}

	public int createFile(String filename, boolean openIt)
	throws IOException, IllegalArgumentException
	{
		if(!avail_fd) {
			throw new IllegalArgumentException();
		}
		
		String[] filePath = filename.split("/");

		TransID id = disk.beginTrans();
		
		DirEnt current = getCurDir(id, filename);
		if(current == null) return -1;

		// create file and get inum
		int inum = disk.createFile(id);

		byte[] meta = format_metadata(filePath[filePath.length - 1], inum, 0, false); 
		disk.writeFileMetadata(id, inum, meta);

		// add file to directory
		current.addFile(filePath[filePath.length -1], inum, false);
		meta = format_metadata(current.getName(), current.getInum(), current.getNumFiles(), true); 
		disk.writeFileMetadata(id, current.getInum(), meta);
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
		if(!avail_fd) {
			throw new IllegalArgumentException();
		}
		String[] filePath = dirname.split("/");

		TransID id = disk.beginTrans();
		DirEnt current = getCurDir(id, dirname);
		if(current == null) return;

		// create file and get inum
		int inum = disk.createFile(id);

		DirEnt dir = new DirEnt(filePath[filePath.length-1], inum);
		dir.addFile(".", current.getInum(), true);
		dir.addFile("..", inum, true);

		// write files to disk
		byte[] meta = format_metadata(filePath[filePath.length - 1], inum, 2, true); 
		disk.writeFileMetadata(id, inum, meta);
		dir.print_to_disk(disk, id);

		//  add file to directory
		current.addFile(filePath[filePath.length -1], inum, true);
		meta = format_metadata(current.getName(), current.getInum(), current.getNumFiles(), true);

		// write current to disk && metadata
		current.print_to_disk(disk, id);
		disk.writeFileMetadata(id, current.getInum(), meta);

		//commit to disk
		disk.commitTrans(id);
	}



	public void unlink(String filename)
	throws IOException, IllegalArgumentException
	{
		if(!avail_fd) {
			throw new IllegalArgumentException();
		}
		String[] filePath = filename.split("/");
		String fname = filePath[filePath.length-1];
		if(fname.equals(".") || fname.equals("..")) {
			throw new IllegalArgumentException();
		}
		
		TransID id = disk.beginTrans();
		DirEnt current = getCurDir(id, filename);
		if(current == null) return;
		int inumber = -1;
		if(!current.isFile(fname)) {
			inumber = current.get_next_Dir(fname);
			if(inumber == -1) {
				disk.abortTrans(id);
				throw new IllegalArgumentException();
			}
			DirEnt test = new DirEnt(inumber, disk,id);
			if(test.getNumFiles() > 2) {
				disk.abortTrans(id);
				throw new IllegalArgumentException();
			}
		} else {
			inumber = current.get_next_File(fname);
			if(inumber == -1) {
				disk.abortTrans(id);
				throw new IllegalArgumentException();
			}
		}
		
		
		current.deleteFile(fname);
		
		byte[] meta = format_metadata(current.getName(), current.getInum(), current.getNumFiles(), true); 
		disk.writeFileMetadata(id, current.getInum(), meta);
		current.print_to_disk(disk, id);
		
		disk.deleteFile(id, inumber);
		disk.commitTrans(id);
	}

	public void rename(String oldName, String newName)
	throws IOException, IllegalArgumentException
	{
		if(!avail_fd) {
			throw new IllegalArgumentException();
		}
		
		TransID id = disk.beginTrans();
		
		// get current directory this file lives in
		DirEnt current = getCurDir(id, oldName);
		
		// get new current
		DirEnt currentNew = getCurDir(id, oldName);
		
		// If either doesn't exist, throw bad name
		if(current == null || currentNew == null)  {
			disk.abortTrans(id);
			throw new IllegalArgumentException();
		}
		
		
		String[] filePathOld = oldName.split("/");
		String foldname = filePathOld[filePathOld.length-1];

		// get the file information
		boolean isDir = true;
		int inumber = current.get_next_Dir(foldname);
		if(inumber == -1) {
			isDir = false;
			inumber = current.get_next_File(foldname);
		}
		if(inumber == -1) {
			disk.abortTrans(id);
			throw new IllegalArgumentException();
		}

		// delete the old file
		current.deleteFile(foldname);
		byte[] meta = format_metadata(current.getName(), current.getInum(), current.getNumFiles(), true); 
		disk.writeFileMetadata(id, current.getInum(), meta);
		current.print_to_disk(disk, id);
		
		// create the new file
		String[] filePathNew = newName.split("/");
		String fnewname = filePathNew[filePathNew.length-1];
		
		//add file to new directory
		currentNew = getCurDir(id, oldName);
		try {
			currentNew.addFile(fnewname, inumber, isDir);
		} catch (IllegalArgumentException e) {
			disk.abortTrans(id);
			throw new IllegalArgumentException();
		}
		
		meta = format_metadata(currentNew.getName(), currentNew.getInum(), currentNew.getNumFiles(), true); 
		disk.writeFileMetadata(id, currentNew.getInum(), meta);
		currentNew.print_to_disk(disk, id);
		
		//change parent if isdir
		if(isDir) {
			DirEnt test = new DirEnt(inumber, disk, id);
			test.change_parent(currentNew.getInum());
			meta = format_metadata(fnewname, test.getInum(), test.getNumFiles(), true); 
			disk.writeFileMetadata(id, test.getInum(), meta);
			test.print_to_disk(disk, id);
		}

		
		disk.commitTrans(id);
	}

	private DirEnt getCurDir(TransID id, String filename) throws IllegalArgumentException, IOException {
		// parse filename
		String[] filePath = filename.split("/");
		/*for(int i = 0; i < filePath.length; i++) {
			System.out.println(filePath[i]);
		}*/
		// use directories to find file
		DirEnt current = getRootEntry(id);
		for( int i = 1; i < filePath.length - 1; i++) {
			int next = current.get_next_Dir(filePath[i]);
			//System.out.println(filePath[i]);
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
		if(!avail_fd) {
			throw new IllegalArgumentException();
		}
		
		// get directory this file lives in
		TransID id = disk.beginTrans();
		if(dirname.equals("/")) {
			String[] ret = getRootEntry(id).get_list_files();
			disk.commitTrans(id);
			return ret;
		}
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

		return disk.getTotalBlocks(open_xid, open_in) * PTree.BLOCK_SIZE_BYTES;
	}

	public int space(int fd)
	throws IOException, IllegalArgumentException
	{
		if(avail_fd == true && fd != 1) {
			throw new IllegalArgumentException();
		}

		int blocks = disk.getTotalBlocks(open_xid, open_in);
		if (blocks <= 8) return blocks+1;
		
		int b = blocks;
		while(b > 0) {
			blocks += (b-1)/256 + 1;
			b /= 256;
		}
		
		return blocks + 1;
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
	
	
	
	
	
	
	
	
	
	
	
	
	

	public static void unit(Tester t) throws IOException {
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
		t.set_method("constructor");
		RFS rfs1 = new RFS(false);

		t.is_true(rfs1.disk != null);
		t.is_true(rfs1.avail_fd);
		t.is_equal(-1, rfs1.open_in);
		t.is_true(rfs1.open_xid == null);

		TransID xid1 = rfs1.disk.beginTrans();
		byte[] buffer = new byte[1024];
		rfs1.disk.read(xid1, 0, 0, 1024, buffer);

		DirEnt root1 = new DirEnt(rfs1.root, rfs1.disk, xid1);
		/*t.is_equal(0,root1.getInum());
		t.is_equal(0, root1.get_next_Dir("."));
		t.is_equal(0, root1.get_next_Dir(".."));
		t.is_equal(2, root1.getNumFiles());




		// getRootEntry(TransID)
		t.set_method("getRootEntry()");
		root1 = rfs1.getRootEntry(xid1);
		t.is_equal(0,root1.getInum());
		t.is_equal(0, root1.get_next_Dir("."));
		t.is_equal(0, root1.get_next_Dir(".."));
		t.is_equal(2, root1.getNumFiles());






		// getCurDirDir(TransID, String)
		t.set_method("getCurDir");
		DirEnt curEnt = rfs1.getCurDir(xid1, "/");
		t.is_equal(0,curEnt.getInum());
		t.is_equal(0, curEnt.get_next_Dir("."));
		t.is_equal(0, curEnt.get_next_Dir(".."));
		t.is_equal(2, curEnt.getNumFiles());

		curEnt = rfs1.getCurDir(xid1, "/../../..");
		t.is_equal(0,curEnt.getInum());
		t.is_equal(0, curEnt.get_next_Dir("."));
		t.is_equal(0, curEnt.get_next_Dir(".."));
		t.is_equal(2, curEnt.getNumFiles());

		for(int i = 1; i <= 13; i++) {
			rfs1.disk.createFile(xid1);
		}
		rfs1.disk.commitTrans(xid1);
		xid1 = rfs1.disk.beginTrans();
		for(int i = 0; i < 13; i++) {
			
			DirEnt entA = new DirEnt("" + (char) (97 + i), i+1);
			meta1 = RFS.format_metadata("" + (char) (97 + i), i+1, 0, true);
			rfs1.disk.writeFileMetadata(xid1, i+1, meta1);
			entA.print_to_disk(rfs1.disk, xid1);
			rfs1.disk.commitTrans(xid1);
			xid1 = rfs1.disk.beginTrans();
		}

		curEnt.addFile("a", 1, true);
		curEnt.addFile("d", 4, true);
		curEnt.addFile("e", 5, true);
		curEnt.addFile("m", 13, true);
		curEnt.print_to_disk(rfs1.disk, xid1);
		meta1 = RFS.format_metadata("root", curEnt.getInum(), curEnt.getNumFiles(), true);
		rfs1.disk.writeFileMetadata(xid1,  curEnt.getInum(), meta1);
		curEnt.print_to_disk(rfs1.disk, xid1);
		rfs1.disk.commitTrans(xid1);
		xid1 = rfs1.disk.beginTrans();
		
		curEnt = rfs1.getCurDir(xid1, "/a/q");
		t.is_equal(1,curEnt.getInum());
		
		curEnt.addFile("b", 2, true);
		curEnt.addFile("c", 3, true);
		curEnt.print_to_disk(rfs1.disk, xid1);
		meta1 = RFS.format_metadata(curEnt.getName(), curEnt.getInum(), curEnt.getNumFiles(), true);
		rfs1.disk.writeFileMetadata(xid1,  curEnt.getInum(), meta1);
		curEnt.print_to_disk(rfs1.disk, xid1);
		rfs1.disk.commitTrans(xid1);
		xid1 = rfs1.disk.beginTrans();
		
		curEnt = rfs1.getCurDir(xid1, "/a/b/q");
		t.is_equal(2, curEnt.getInum());
		
		curEnt = rfs1.getCurDir(xid1, "/a/c/q");
		t.is_equal(3, curEnt.getInum());
		
		
		curEnt = rfs1.getCurDir(xid1, "/d/q");
		t.is_equal(4,curEnt.getInum());
		
		curEnt = rfs1.getCurDir(xid1, "/e/q");
		t.is_equal(5,curEnt.getInum());
		
		curEnt.addFile("f", 6, true);
		curEnt.addFile("g", 7, true);
		curEnt.addFile("h", 8, true);
		curEnt.print_to_disk(rfs1.disk, xid1);
		meta1 = RFS.format_metadata(curEnt.getName(), curEnt.getInum(), curEnt.getNumFiles(), true);
		rfs1.disk.writeFileMetadata(xid1,  curEnt.getInum(), meta1);
		curEnt.print_to_disk(rfs1.disk, xid1);
		rfs1.disk.commitTrans(xid1);
		xid1 = rfs1.disk.beginTrans();
		
		curEnt = rfs1.getCurDir(xid1, "/e/f/q");
		t.is_equal(6,curEnt.getInum());
		
		curEnt = rfs1.getCurDir(xid1, "/e/g/q");
		t.is_equal(7,curEnt.getInum());
		
		curEnt = rfs1.getCurDir(xid1, "/e/h/q");
		t.is_equal(8,curEnt.getInum());
		
		curEnt.addFile("i", 9, true);
		curEnt.addFile("j", 10, true);
		curEnt.print_to_disk(rfs1.disk, xid1);
		meta1 = RFS.format_metadata(curEnt.getName(), curEnt.getInum(), curEnt.getNumFiles(), true);
		rfs1.disk.writeFileMetadata(xid1,  curEnt.getInum(), meta1);
		curEnt.print_to_disk(rfs1.disk, xid1);
		rfs1.disk.commitTrans(xid1);
		xid1 = rfs1.disk.beginTrans();
		
		curEnt = rfs1.getCurDir(xid1, "/e/h/i/q");
		t.is_equal(9,curEnt.getInum());
		
		curEnt = rfs1.getCurDir(xid1, "/e/h/j/q");
		t.is_equal(10,curEnt.getInum());
		
		curEnt.addFile("k", 11, true);
		curEnt.addFile("l", 12, true);
		curEnt.print_to_disk(rfs1.disk, xid1);
		meta1 = RFS.format_metadata(curEnt.getName(), curEnt.getInum(), curEnt.getNumFiles(), true);
		rfs1.disk.writeFileMetadata(xid1,  curEnt.getInum(), meta1);
		curEnt.print_to_disk(rfs1.disk, xid1);
		rfs1.disk.commitTrans(xid1);
		xid1 = rfs1.disk.beginTrans();
		
		curEnt = rfs1.getCurDir(xid1, "/e/h/j/k/q");
		t.is_equal(11,curEnt.getInum());
		
		curEnt = rfs1.getCurDir(xid1, "/e/h/j/l/q");
		t.is_equal(12,curEnt.getInum());
		
		
		curEnt = rfs1.getCurDir(xid1, "/m/q");
		t.is_equal(13,curEnt.getInum());
		rfs1.disk.commitTrans(xid1);
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		System.out.println("Testing RFS part 2");
		// createFile(String, boolean)
		t.set_method("createFile()");
		rfs1 = new RFS(true);
		
		int fd1 = rfs1.createFile("/a", false);
		t.is_equal(-1,fd1);
		
		
		xid1 = rfs1.disk.beginTrans();
		root1 = rfs1.getRootEntry(xid1);
		rfs1.disk.commitTrans(xid1);
		
		t.is_equal(3, root1.getNumFiles());
		t.is_equal(1, root1.get_next_File("a"));
		
		fd1 = rfs1.createFile("/b", true);
		t.is_equal(1, fd1);
		rfs1.close(fd1);
		t.is_equal(true, rfs1.avail_fd);
		xid1 = rfs1.disk.beginTrans();
		root1 = rfs1.getRootEntry(xid1);
		
		t.is_equal(4, root1.getNumFiles());
		t.is_equal(2, root1.get_next_File("b"));
		
		rfs1.disk.commitTrans(xid1);
		
		
		
		
		
		
		// createDir(String)
		t.set_method("createDir");
		rfs1.createDir("/c");
		xid1 = rfs1.disk.beginTrans();
		root1 = rfs1.getRootEntry(xid1);
		rfs1.disk.commitTrans(xid1);
		t.is_equal(5, root1.getNumFiles());
		t.is_equal(5, root1.get_list_files().length);
		
		rfs1.createDir("/c/d");
		xid1 = rfs1.disk.beginTrans();
		root1 = rfs1.getRootEntry(xid1);
		curEnt = rfs1.getCurDir(xid1, "/c/d");
		rfs1.disk.commitTrans(xid1);
		t.is_equal(3, curEnt.getNumFiles());
		
		fd1 = rfs1.createFile("/c/d/e", true);
		t.is_equal(1, fd1);
		t.is_equal(false, rfs1.avail_fd);
		
		
		root1 = rfs1.getRootEntry(rfs1.open_xid);
		curEnt = rfs1.getCurDir(rfs1.open_xid, "/c/d");
		DirEnt curEnt2 = rfs1.getCurDir(rfs1.open_xid, "/c/d/e");
		t.is_equal(5, root1.getNumFiles());
		t.is_equal(3, curEnt.getNumFiles());
		t.is_equal(3, curEnt2.getNumFiles());
		rfs1.close(fd1);
		
		xid1 = rfs1.disk.beginTrans();
		root1 = rfs1.getRootEntry(xid1);
		curEnt = rfs1.getCurDir(xid1, "/c/d");
		curEnt2 = rfs1.getCurDir(xid1, "/c/d/e");
		rfs1.disk.commitTrans(xid1);
		
		t.is_equal(5, root1.getNumFiles());
		t.is_equal(3, curEnt.getNumFiles());
		t.is_equal(3, curEnt2.getNumFiles());
		
		
		
		
		
		
		// unlink(String)
		System.out.println("unlink");
		
		t.set_method("unlink()");
		rfs1.unlink("/c/d/e");
		xid1 = rfs1.disk.beginTrans();
		root1 = rfs1.getRootEntry(xid1);
		curEnt = rfs1.getCurDir(xid1, "/c/d");
		curEnt2 = rfs1.getCurDir(xid1, "/c/d/e");
		rfs1.disk.commitTrans(xid1);
		t.is_equal(5, root1.getNumFiles());
		t.is_equal(3, curEnt.getNumFiles());
		t.is_equal(2, curEnt2.getNumFiles());
		
		rfs1.unlink("/c/d");
		xid1 = rfs1.disk.beginTrans();
		root1 = rfs1.getRootEntry(xid1);
		curEnt = rfs1.getCurDir(xid1, "/c/d");
		rfs1.disk.commitTrans(xid1);
		t.is_equal(5, root1.getNumFiles());
		t.is_equal(2, curEnt.getNumFiles());
		
		
		
		
		
		System.out.println("rename");
		// rename(string, String)
		t.set_method("rename");
		
		rfs1.rename("/c", "/d");
		xid1 = rfs1.disk.beginTrans();
		root1 = rfs1.getRootEntry(xid1);
		rfs1.disk.commitTrans(xid1);
		
		int inum = root1.get_next_Dir("d");
		t.is_equal(3, inum);
		
		
		
		
		
		
		System.out.println("readDir");
		// readDir
		t.set_method("readDir");
		xid1 = rfs1.disk.beginTrans();
		root1 = rfs1.getRootEntry(xid1);
		rfs1.disk.commitTrans(xid1);
		String[] files = rfs1.readDir("/");
		t.is_equal(5, files.length);
		t.is_equal(".", files[0]);
		t.is_equal("..", files[1]);
		t.is_equal("a", files[2]);
		t.is_equal("b", files[3]);
		t.is_equal("d", files[4]);
		
		xid1 = rfs1.disk.beginTrans();
		root1 = rfs1.getRootEntry(xid1);
		rfs1.disk.commitTrans(xid1);
		files = rfs1.readDir("/d");
		t.is_equal(2, files.length);
		t.is_equal(".", files[0]);
		t.is_equal("..", files[1]);

		
		
		System.out.println("open");
		// open(string)
		t.set_method("open");
		fd1 = rfs1.open("/a");
		t.is_equal(1, rfs1.open_in);
		t.is_equal(false, rfs1.avail_fd);
		t.is_true(rfs1.open_xid != null);
		
		try{
			rfs1.open("/b");
			t.is_true(false);
		} catch (Exception e) {
			t.is_true(true);
		}
		
		
		
		
		System.out.println("close");
		// close(int)
		t.set_method("close");
		rfs1.close(fd1);
		t.is_equal(-1, rfs1.open_in);
		t.is_equal(true, rfs1.avail_fd);
		t.is_true(rfs1.open_xid == null);
		
		try{
			rfs1.close(fd1);
			t.is_true(false);
		} catch (Exception e) {
			t.is_true(true);
		}
		
		
		
		System.out.println("read");
		// read
		t.set_method("read");
		fd1 = rfs1.createFile("/d/z", true);
		t.is_equal(1, fd1);
		t.is_true(rfs1.open_xid != null);
		t.is_true(!rfs1.avail_fd);
		t.is_equal(4, rfs1.open_in);
		
		buffer = new byte[100];
		rfs1.read(fd1, 0, 100, buffer);
		t.is_equal(new byte[100], buffer);
		
		
		
		
		
		
		// write
		t.set_method("write");
		byte[] bTest = new byte[100];
		for(int i = 0; i < buffer.length; i++) {
			//bTest[i] = (byte) (i%128);
			buffer[i] = (byte) (i%128);
		}
		rfs1.write(fd1, 0, 100, buffer);
		rfs1.read(fd1, 0, 100, bTest);
		t.is_equal(buffer, bTest);
		
		//rfs1.close(fd1);
		
		
		
		
		System.out.println("size");
		// size
		t.set_method("size");
		t.is_equal(1024, rfs1.size(fd1));
		
		// space
		t.set_method("space");
		t.is_equal(2, rfs1.space(fd1));
		
		
		rfs1.write(fd1, 2048, 100, buffer);
		rfs1.read(fd1, 2048, 100, bTest);
		t.is_equal(bTest, buffer);
		t.is_equal(1024*3, rfs1.size(fd1));
		t.is_equal(4, rfs1.space(fd1));
		
		
		buffer = new byte[1500];
		for(int i = 0; i < buffer.length; i++) {
			buffer[i] = (byte) (i%128);
		}
		bTest = new byte[1500];
		rfs1.write(fd1, 1024*10, 1500, buffer);
		rfs1.read(fd1, 1024*10, 1500, bTest);
		t.is_equal(bTest, buffer);
		t.is_equal(1024*12, rfs1.size(fd1));
		t.is_equal(14, rfs1.space(fd1));
		
		rfs1.close(fd1);
		t.is_equal(-1, rfs1.open_in);
		t.is_equal(true, rfs1.avail_fd);
		t.is_true(rfs1.open_xid == null);*/
		
		
		DirEnt curEnt = null;
		DirEnt curEnt2 = null;
		//test rename
		t.set_method("rename pt 2");
		rfs1 = new RFS(true);
		rfs1.createDir("/a");
		rfs1.createDir("/b");
		rfs1.createDir("/a/c");
		
		xid1 = rfs1.disk.beginTrans();
		root1 = rfs1.getRootEntry(xid1);
		curEnt = rfs1.getCurDir(xid1, "/a/c");
		curEnt2 = rfs1.getCurDir(xid1, "/b/z");
		rfs1.disk.commitTrans(xid1);
		t.is_equal(4, root1.getNumFiles());
		t.is_equal(3, curEnt.getNumFiles());
		
		t.is_equal(2, curEnt2.getNumFiles());
		
		rfs1.rename("/a/c", "/b/d");
		xid1 = rfs1.disk.beginTrans();
		root1 = rfs1.getRootEntry(xid1);
		curEnt = rfs1.getCurDir(xid1, "/a/c");
		curEnt2 = rfs1.getCurDir(xid1, "/b/z");
		rfs1.disk.commitTrans(xid1);
		
		String s[] = curEnt.get_list_files();
		System.out.println("a files:");
		for(String a : s) System.out.print(a + "   " );
		
		s = curEnt2.get_list_files();
		System.out.println("b files:");
		for(String a : s) System.out.print(a + "   " );
		
		t.is_equal(4, root1.getNumFiles());
		t.is_equal(2, curEnt.getNumFiles());
		t.is_equal(3, curEnt2.getNumFiles());
		t.is_true(curEnt2.get_next_Dir("d") != -1);
		t.is_true(curEnt.get_next_Dir("c") == -1);
		
		/*rfs1.rename("/b/d", "/e");
		xid1 = rfs1.disk.beginTrans();
		root1 = rfs1.getRootEntry(xid1);
		curEnt = rfs1.getCurDir(xid1, "/a/c");
		curEnt2 = rfs1.getCurDir(xid1, "/b/z");
		rfs1.disk.commitTrans(xid1);
		t.is_equal(5, root1.getNumFiles());
		t.is_equal(2, curEnt.getNumFiles());
		t.is_equal(2, curEnt2.getNumFiles());
		t.is_true(curEnt2.get_next_Dir("d") == -1);
		t.is_true(curEnt.get_next_Dir("c") == -1);
		t.is_true(root1.get_next_Dir("e") != -1);*/
	}
}
