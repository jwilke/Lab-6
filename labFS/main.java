import java.io.IOException;


public class main {
	public static void main(String args[]) throws IllegalArgumentException, IOException {
		System.out.println("Testing TranID");
		TransID id = new TransID();
		id.unit();
		
		System.out.println("\nTesting Write");
		Write w = new Write(0, new byte[0]);
		w.unit();
		
		System.out.println("\nTesting Transaction");
		Transaction t = new Transaction();
		t.unit();
		
	}
}
