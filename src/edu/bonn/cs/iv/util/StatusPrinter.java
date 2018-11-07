package edu.bonn.cs.iv.util;

import java.io.*;

public class StatusPrinter {
	protected final PrintStream prn;
	protected int last = 0;

	public StatusPrinter(PrintStream prn) {
		this.prn = prn;
	}

	public void print(String str) {
		prn.print(str);
		int spaces = last - str.length();
		for (int i = 0; i < spaces; i++)
			prn.print(" ");
		prn.print("\r");
		last = str.length();
	}
}
