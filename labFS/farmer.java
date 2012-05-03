
public class farmer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int[] res = solveFarmer(5, 1, 94);
		System.out.println("Cows: " + res[0]);
		System.out.println("Pigs: " + res[1]);
		System.out.println("Chickens: " + res[2]);
	}

	
	public static int[] solveFarmer(int c, int p, int k) {
		double cost = 10*c + 3*p + 0.5*k;
		//System.out.println("Here: " + (c+p+k) + " " + cost);
		if(c + p + k >= 100) {
			if(cost == 100.0) {
				int[] ret = new int[3];
				ret[0] = c;
				ret[1] = p;
				ret[2] = k;
				return ret;
			} else {
				return null;
			}
		} else {
			if(cost >= 100) return null;
			int[] ret = solveFarmer(c+1, p, k);
			if(ret == null) ret = solveFarmer(c, p+1, k);
			if(ret == null) ret = solveFarmer(c, p, k+1);
			return ret;
		}
	}
}
