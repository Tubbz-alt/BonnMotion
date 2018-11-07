/*
 * Created on 18.02.2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package edu.bonn.cs.iv.util;

/**
 * @author peschlow
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Lcm {

	public static int calcLcm(int[] numbers) {
		int lcm = numbers[0];
		for (int i = 1; i < numbers.length; i++) {
			//int last = lcm;
			int next = numbers[i];
			lcm = lcmOfTwo(lcm, next);
			//System.out.println(last + ", " + next + " = " + lcm);
		}
		return lcm;
	}

	private static int lcmOfTwo(int x, int y) {
		return (x * y) / gcdOfTwo(x,y);
	}

	private static int gcdOfTwo(int x, int y) {
		if (y == 0) {
			return x;
		} else {
			return gcdOfTwo(y, x%y);
		} 
	}

}
