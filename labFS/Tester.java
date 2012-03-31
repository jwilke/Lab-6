/*
	 * Tester.java
	 *
	 *  Created on: Dec 23, 2011
	 *      Author: grahambenevelli
	 *
	 *  Last Updaded: March 31, 2011
	 */

public class Tester {
	
	
	private int test;
	private int failed;
	private String method;
	private String object;

	/**
	 * constructor
	 * pre-none
	 * post-method == "", test == 0
	 */
	public Tester() {
		method = null;
		object = null;
		test = 0;
		failed = 0;
	}
	
	/**
	 * acts like destructor, prints final results
	 * and then resets everything
	 */
	public void close() {
		System.out.println("Passed: " + (test - failed));
		System.out.println("Failed: " + failed);
		if (failed == 0) {
			System.out.println("All Tests Passed!");
		} else {
			System.out.println("Tests Failed");
		}
		
		method = null;
		object = null;
		test = 0;
		failed = 0;
	}

	/**
	 * print out failing situation, including the test number, method,
	 * and the expected and recieved values
	 * pre: var, exp, got != null
	 * post: none
	 * @param - string var: the variable that is being compared
	 * @param - string exp: the expected value of this test
	 * @param - string got: the actual value of this test
	 */
	public void print_fail(String var, String exp, String got) {
		
		System.out.println("Test Failed");
		System.out.println("Test: " + test);
		System.out.println("Method: " + method);
		System.out.println("Comparing: " + var);
		System.out.println("Expected: \t" + exp);
		System.out.println("Actual: \t" + got);
		System.out.println("************************************************");
	}

	/**
	 * print out failing situation, including the test number, method,
	 * and the expected and recieved values
	 * pre: var, exp, got != null
	 * post: none
	 * @param - string var: the variable that is being compared
	 * @param - string exp: the expected value of this test
	 * @param - string got: the actual value of this test
	 * @param - string n: the name of the variable being compared, for
	 * 						future use
	 */
	public void print_fail(String var, String exp, String got, String n) {
		System.out.println("For Variable: " + n);
		print_fail(var, exp, got);
	}

	/**
	 * print out failing situation, including the test number, method,
	 * and the expected and recieved values
	 * pre: var, exp, got != null
	 * post: none
	 * @param - string var: the variable that is being compared
	 * @param - int exp: the expected value of this test
	 * @param - int got: the actual value of this test
	 */
	void print_fail(String var, int exp, int got) {
		String exps = "" + exp;
		String gots = "" + got;
		print_fail(var, exps, gots);
	}

	/**
	 * print out failing situation, including the test number, method,
	 * and the expected and recieved values
	 * pre: var, exp, got != null
	 * post: none
	 * @param - string var: the variable that is being compared
	 * @param - string exp: the expected value of this test
	 * @param - string got: the actual value of this test
	 * @param - string n: the name of the variable being compared, for
	 * 						future use
	 */
	void print_fail(String var, int exp, int got, String n) {
		System.out.println("For Variable: " + n);
		print_fail(var, exp, got);
	}

	/**
	 * print out failing situation, including the test number, method,
	 * and the expected and recieved values
	 * pre: var, exp, got != null
	 * post: none
	 * @param - string var: the variable that is being compared
	 * @param - char exp: the expected value of this test
	 * @param - char got: the actual value of this test
	 */
	public void print_fail(String var, char exp, char got) {
		String exps = "" + exp;
		String gots = "" + got;
		print_fail(var, exps, gots);
	}

	/**
	 * print out failing situation, including the test number, method,
	 * and the expected and recieved values
	 * pre: var, exp, got != null
	 * post: none
	 * @param - string var: the variable that is being compared
	 * @param - char exp: the expected value of this test
	 * @param - char got: the actual value of this test
	 */
	public void print_fail(String var, char exp, char got, String n) {
		String exps = "" + exp;
		String gots = "" + got;
		print_fail(var, exps, gots, n);
	}

	/**
	 * print out failing situation, including the test number, method,
	 * and the expected and recieved values
	 * pre: var, exp, got != null
	 * post: none
	 * @param - string var: the variable that is being compared
	 * @param - double exp: the expected value of this test
	 * @param - double got: the actual value of this test
	 */
	public void print_fail(String var, double exp, double got) {
		String exps = "" + exp;
		String gots = "" + got;
		print_fail(var, exps, gots);
	}

	/**
	 * print out failing situation, including the test number, method,
	 * and the expected and recieved values
	 * pre: var, exp, got != null
	 * post: none
	 * @param - string var: the variable that is being compared
	 * @param - double exp: the expected value of this test
	 * @param - double got: the actual value of this test
	 */
	public void print_fail(String var, double exp, double got, String n) {
		String exps = "" + exp;
		String gots = "" + got;
		print_fail(var, exps, gots, n);
	}




	/**
	 * get the number of tests run already
	 * pre - test >= 0
	 * post - same
	 */
	public int get_test() {
		return test;
	}

	/**
	 * get the number of tests failed
	 */
	public int get_failed() {
		return failed;
	}

	public double get_percent() {
		return 1.0*(test - failed)/test;
	}

	/**
	 * get the method that is currently being tested
	 * pre - method != null
	 * post - same
	 */
	public String get_method() {
		return method;
	}

	/**
	 * set the name of the method to be tested
	 * pre - method != null
	 * post - method == m
	 */
	public void set_method(String m) {
		method = m;
	}

	/**
	 * get the name of the object to be tested
	 * Pre: object != null
	 * Post: same
	 */
	public String get_object() {
		return object;
	}

	/**
	 * set the name of the object to be tested
	 * Pre: object != null
	 * Post: same
	 */
	public void set_object(String o) {
		object = o;
	}

	/**
	 * tests whether two ints are the same
	 * pre - test >= 0
	 * post - return i1 == i2
	 */
	public boolean is_equal(int i1, int i2) {
		test++;
		if (i1 != i2) {
			print_fail("int", i1, i2);
			failed++;
			return false;
		} else {
			return true;
		}
	}

	/**
	 * tests whether two ints are the same, giving the name of the variable
	 * pre - test >= 0
	 * post - return i1 == i2
	 */
	public boolean is_equal(int i1, int i2, String n) {
		test++;
		if (i1 != i2) {
			print_fail("int", i1, i2, n);
			failed++;
			return false;
		} else {
			return true;
		}
	}

	/**
	 * test if the two chars are equal
	 * pre - test >= 0
	 * post - same
	 */
	public boolean is_equal(char c1, char c2) {
		test++;
		if (c1 != c2 ) {
			print_fail("char", c1, c2);
			failed++;
			return false;
		} else {
			return true;
		}
	}

	/**
	 * test if the two chars are equal
	 * pre - test >= 0
	 * post - same
	 */
	public boolean is_equal(char c1, char c2, String n) {
		test++;
		if (c1 != c2 ) {
			print_fail("char", c1, c2, n);
			failed++;
			return false;
		} else {
			return true;
		}
	}


	/**
	 * test whether two floating point numbers are equal
	 * pre - test > 0
	 * post - same
	 * @param: double d1 - the expected value
	 * @param: double d2 - the resulting value
	 */
	public boolean is_equal(double d1, double d2) {
		test++;
		if (d1 != d2 ) {
			print_fail("double", d1, d2);
			failed++;
			return false;
		} else {
			return true;
		}
	}

	/**
	 * test whether two floating point numbers are equal
	 * pre - test > 0
	 * post - same
	 * @param: double d1 - the expected value
	 * @param: double d2 - the resulting value
	 * @param - string n: the name of the variable being tested
	 */
	public boolean is_equal(double d1, double d2, String n) {
		test++;
		if (d1 != d2 ) {
			print_fail("double", d1, d2, n);
			failed++;
			return false;
		} else {
			return true;
		}
	}


	public boolean is_equal(String s1, String s2) {
		test++;
		if (!((s1 == null && s2 == null) || (s1 != null && s1.equals(s2)))) {
			print_fail("String", s1, s2);
			failed++;
			return false;
		} else {
			return true;
		}
	}

	public boolean is_equal(String s1, String s2, String n) {
		test++;
		if (s1 != s2) {
			print_fail("String", s1, s2, n);
			failed++;
			return false;
		} else {
			return true;
		}
	}

	/**
	 * checks if the passed bool is true
	 * pre: test >= 0
	 * post: same
	 * @param - bool b1: the expected value, return true if true
	 */
	public boolean is_true(boolean b1) {
		test++;
		if (!b1) {
			print_fail("bool", "true", "false");
			failed++;
			return false;
		} else {
			return true;
		}
	}

	/**
	 * checks if the passed bool is true
	 * pre: test >= 0
	 * post: same
	 * @param - bool b1: the expected value, return true if true
	 * @param - string n: the name of the tested variable
	 */
	public boolean is_true(boolean b1, String n) {
		test++;
		if (!b1) {
			print_fail("bool", "true", "false", n);
			failed++;
			return false;
		} else {
			return true;
		}
	}
}
