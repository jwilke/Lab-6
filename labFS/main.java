import java.io.IOException;


public class main {
	public static void main(String args[]) throws IllegalArgumentException, IOException {
		Tester t = new Tester();
		System.out.println("Testing TranID");
		TransID id = new TransID();
		id.unit(t);
		
		System.out.println("Testing Write");
		Write w = new Write(0, new byte[0]);
		w.unit(t);
		
		System.out.println("Testing Transaction");
		Transaction tran = new Transaction();
		tran.unit(t);
		
		System.out.println("Testing ActiveTransactionList");
		ActiveTransactionList atl = new ActiveTransactionList();
		atl.unit(t);
		
		System.out.println("Testing WrtieBackList");
		WriteBackList.unit(t);
		
		System.out.println("Testing LogStatus");
		LogStatus.unit(t);
		
		System.out.println();
		t.close();
	}
}
