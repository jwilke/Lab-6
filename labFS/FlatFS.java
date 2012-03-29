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

  public FlatFS(boolean doFormat)
    throws IOException
  {
  }

  public TransID beginTrans()
  {
    return null;
  }

  public void commitTrans(TransID xid)
    throws IOException, IllegalArgumentException
  {
  }

  public void abortTrans(TransID xid)
    throws IOException, IllegalArgumentException
  {
  }

  public int createFile(TransID xid)
    throws IOException, IllegalArgumentException
  {
    return -1;
  }

  public void deleteFile(TransID xid, int inumber)
    throws IOException, IllegalArgumentException
  {
  }

  public int read(TransID xid, int inumber, int offset, int count, byte buffer[])
    throws IOException, IllegalArgumentException, EOFException
  {
    return -1;
  }
    

  public void write(TransID xid, int inumber, int offset, int count, byte buffer[])
    throws IOException, IllegalArgumentException
  {
  }

  public void readFileMetadata(TransID xid, int inumber, byte buffer[])
    throws IOException, IllegalArgumentException
  {
  }


  public void writeFileMetadata(TransID xid, int inumber, byte buffer[])
    throws IOException, IllegalArgumentException
  {
  }

  public int getParam(int param)
    throws IOException, IllegalArgumentException
  {
    return -1;
  }
    

  
  

}
