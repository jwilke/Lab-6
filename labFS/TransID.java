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
  
  public void unit() {
  	Tester t = new Tester();
  	t.set_object("TransID");
  	
  	// test constructor
  	TransID id1 = new TransID();
  	TransID id2 = new TransID();
  	TransID id3 = new TransID();
  	t.is_equal(id1.tranNum, 1, "tranNum");
  	t.is_equal(id2.tranNum, 2, "tranNum");
  	t.is_equal(id3.tranNum, 3, "tranNum");
  	t.is_equal(tranCurrent, 4);
  	
  	// getTranNum()
  	t.is_equal(id1.getTranNum(), 1, "tranNum");
  	t.is_equal(id2.getTranNum(), 2, "tranNum");
  	t.is_equal(id3.getTranNum(), 3, "tranNum");
  	
  	// equals(TransID)
  	t.is_true(!id1.equals(id2));
  	t.is_true(!id1.equals(id3));
  	t.is_true(!id2.equals(id3));
  	t.is_true(!id2.equals(id1));
  	t.is_true(!id3.equals(id1));
  	t.is_true(!id3.equals(id2));
  	t.is_true(id1.equals(id1));
  	t.is_true(id2.equals(id2));
  	t.is_true(id3.equals(id3));
  	
  	// equals(Object)
  	Object o = (Object) id1;
  	t.is_true(id1.equals(o));
  	o = (Object) id2;
  	t.is_true(id2.equals(o));
  	o = (Object) id3;
  	t.is_true(id3.equals(o));
  	
  	
  	t.close();
  }
}
