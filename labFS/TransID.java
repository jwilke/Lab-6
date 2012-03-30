/*
 * TransId.java
 *
 * Interface to ADisk
 *
 * You must follow the coding standards distributed
 * on the class web page.
 *
 * (C) 2007 Mike Dahlin
 *
 */



public class TransID{
	
	private static long tranCurrent = 0;
	private long tranNum;
	
  //
  // Implement this class
  //

  public TransID()
  {
	  tranNum = tranCurrent;
	  tranCurrent++;
  }
  
  public boolean equals(Object o) {
	  if (o instanceof TransID)
		  return equals((TransID) o);
	  else
		  return false;
  }
  
  public boolean equals(TransID other) {
	  return this.tranNum == other.tranNum;
  }

  public long getTranNum() {
	  return tranNum;
  }
}
