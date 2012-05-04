/*
 * FlatFS -- flat file system
 *
 * You must follow the coding standards distributed
 * on the class web page.
 *
 * (C) 2007 Mike Dahlin
 *
 */

import java.io.IOException;
import java.io.EOFException;
public class FlatFS{

	public static final int ASK_MAX_FILE = 2423;
	public static final int ASK_FREE_SPACE_BLOCKS = 29542;
	public static final int ASK_FREE_FILES = 29545;
	public static final int ASK_FILE_METADATA_SIZE = 3502;

	private PTree disk;

	public FlatFS(boolean doFormat)
	throws IOException
	{
		disk = new PTree(doFormat);
	}

	public TransID beginTrans()
	{
		return disk.beginTrans();
	}

	public void commitTrans(TransID xid)
	throws IOException, IllegalArgumentException
	{
		disk.commitTrans(xid);
	}

	public void abortTrans(TransID xid)
	throws IOException, IllegalArgumentException
	{
		disk.abortTrans(xid);
	}

	public int createFile(TransID xid)
	throws IOException, IllegalArgumentException
	{
		return disk.createTree(xid);
	}

	public void deleteFile(TransID xid, int inumber)
	throws IOException, IllegalArgumentException
	{
		disk.deleteTree(xid, inumber);
	}

	public int read(TransID xid, int inumber, int offset, int count, byte buffer[])
	throws IOException, IllegalArgumentException, EOFException
	{
		int beginID = offset / PTree.BLOCK_SIZE_BYTES;
		int in_first_block = 1024-(offset%1024);
		int in_last_block = (count - in_first_block) % 1024; 
		int endID = (offset + count - 1) /PTree.BLOCK_SIZE_BYTES;
		int file_end = disk.getMaxDataBlockId(xid, inumber);
		
		if(file_end < endID) {
			count -= (endID - file_end - 1)*PTree.BLOCK_SIZE_BYTES + in_last_block;
			endID = file_end;
		}
		
		byte[][] allBuff = new byte[endID - beginID + 1][PTree.BLOCK_SIZE_BYTES];
		for(int i = beginID; i <= endID; i++ ) {
			disk.readData(xid, inumber, i, allBuff[i - beginID]);
		}
		
		int new_offset = offset % 1024;
		int index = 0;
		
		for(int i = 0; i < allBuff.length; i++) {
			int temp = 0;
			if(i == 0) temp = new_offset;
			 for(int j = temp; j < allBuff[i].length && index < buffer.length; j++) {
				 buffer[index] = allBuff[i][j];
				 index++;
			 }
		}
		return count;
	}


	public void write(TransID xid, int inumber, int offset, int count, byte buffer[])
	throws IOException, IllegalArgumentException
	{
		
		int max_blocks = disk.getMaxDataBlockId(xid, inumber);
		int beginID = offset / PTree.BLOCK_SIZE_BYTES;
		int endID = (offset + count - 1) / PTree.BLOCK_SIZE_BYTES;

		// read first block if needed
		byte[] bufferOut = new byte[PTree.BLOCK_SIZE_BYTES];
		if(max_blocks > beginID)
			disk.readData(xid, inumber, beginID, bufferOut);
		
		// copy first block
		int j = 0;
		for(int i = offset%1024; i < bufferOut.length && i < buffer.length; i++, j++) {
			bufferOut[i] = buffer[j];
		}
		
		disk.writeData(xid, inumber, beginID, bufferOut);
		
		if(beginID == endID) return;
		
		// copy middle blocks
		for(int i = beginID + 1; i < endID; i++) {
			for(int x = 0; x < PTree.BLOCK_SIZE_BYTES; x++, j++) {
				bufferOut[x] = buffer[j];
			}
			disk.writeData(xid, inumber, i, bufferOut);
		}
		
		// read last block if needed
		if(max_blocks > endID)
			disk.readData(xid, inumber, endID, bufferOut);
		
		// copy last block
		for(int i = 0; j < count; i++, j++) {
			bufferOut[i] = buffer[j];
		}
		disk.writeData(xid, inumber, endID, bufferOut);
	}
	
	

	public void readFileMetadata(TransID xid, int inumber, byte buffer[])
	throws IOException, IllegalArgumentException
	{
		disk.readTreeMetadata(xid, inumber, buffer);
	}


	public void writeFileMetadata(TransID xid, int inumber, byte buffer[])
	throws IOException, IllegalArgumentException
	{
		disk.writeTreeMetadata(xid, inumber, buffer);
	}

	public int getParam(int param)
	throws IOException, IllegalArgumentException
	{
		if(param == FlatFS.ASK_MAX_FILE) {
			return PTree.MAX_TREES;
		} else if (param == FlatFS.ASK_FREE_SPACE_BLOCKS) {
			return disk.getParam(PTree.ASK_FREE_SPACE)/2;
		} else if (param == FlatFS.ASK_FILE_METADATA_SIZE) {
			return PTree.METADATA_SIZE;
		} else if(param == FlatFS.ASK_FREE_FILES){
			return disk.getParam(PTree.ASK_FREE_TREES);
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	public byte[] quick_buf(int size) {
		byte[] ret = new byte[size];
		for(int i = 0; i < size; i++) {
			ret[i] = (byte)( Math.random() * 128);
		}
		return ret;
	}
	
	public int getTotalBlocks(TransID id, int tnum) throws IllegalArgumentException, IOException {
		int ret =  disk.getMaxDataBlockId(id, tnum) + 1;
		return ret;
	}

	public static void unit(Tester t) throws IllegalArgumentException, IOException {
		t.set_object("FlatFS");


		byte data[] = new byte[1024];
		for(int i = 0; i < 1024; i++) {
			data[i] = (byte)(i % 128);
		}
		byte test_data[] = new byte[1024];
		for(int i = 0; i < 1024; i++) {
			data[i] = (byte)(i % 128);
		}
		FlatFS f = new FlatFS(true);
		
		// createFile()
		t.set_method("read and write()");
		TransID id = f.beginTrans();
		int inumber1 = f.createFile(id);
		System.out.println(inumber1);
		int c = 0;
		for(int i = 0; i < 9; i++) {
			f.write(id, inumber1, i*1024, 1024, data);
			c = f.read(id, inumber1, i*1024, 1024, test_data);
			t.is_equal(data, test_data);
			t.is_equal(c, 1024);
		}
		
		f.write(id, inumber1, 500, 1023, data);
		c = f.read(id, inumber1, 500, 1023, test_data);
		test_data[1023] = data[1023];
		t.is_equal(data, test_data);
		t.is_equal(1023, c);
		
		int size = 4500;
		data = f.quick_buf(size);
		test_data = new byte[size];
		f.write(id, inumber1, 5000, size, data);
		c = f.read(id, inumber1, 5000, size, test_data);
		t.is_equal(data, test_data);
		t.is_equal(c, size);
		
		
		
		f.commitTrans(id);
		while(!f.disk.disk.wbl.is_empty()) {
			System.out.print("");
		}
	}



}
