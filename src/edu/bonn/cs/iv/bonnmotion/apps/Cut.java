/*******************************************************************************
 ** BonnMotion - a mobility scenario generation and analysis tool             **
 ** Copyright (C) 2002-2005 University of Bonn                                **
 **                                                                           **
 ** This program is free software; you can redistribute it and/or modify      **
 ** it under the terms of the GNU General Public License as published by      **
 ** the Free Software Foundation; either version 2 of the License, or         **
 ** (at your option) any later version.                                       **
 **                                                                           **
 ** This program is distributed in the hope that it will be useful,           **
 ** but WITHOUT ANY WARRANTY; without even the implied warranty of            **
 ** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             **
 ** GNU General Public License for more details.                              **
 **                                                                           **
 ** You should have received a copy of the GNU General Public License         **
 ** along with this program; if not, write to the Free Software               **
 ** Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA **
 *******************************************************************************/

package edu.bonn.cs.iv.bonnmotion.apps;

import java.io.*;

import edu.bonn.cs.iv.bonnmotion.App;
import edu.bonn.cs.iv.bonnmotion.Scenario;

/** Application to save a certain timeframe from one scenario into a new file. */

public class Cut extends App {
	protected double begin = 0.0;
	protected double end = 0.0;
	protected String source = null;
	protected String destination = null;

	public Cut(String[] args) throws FileNotFoundException, IOException {
		go( args );
	}

	public void go( String[] args ) throws FileNotFoundException, IOException {
		parse(args);
		if ((source == null) || (destination == null) || (end == 0.0)) {
			printHelp();
			System.exit(0);
		}
	
		Scenario s = new Scenario( source ); 

		s.cut(begin, end);
		s.write(destination, new String[0]);
	}

	protected boolean parseArg(char key, String val) {
		switch (key) {
			case 'b': // "begin"
				begin = Double.parseDouble(val);
				return true;
			case 'd':
				destination = val;
				return true;
			case 'e': // "end"
				end = Double.parseDouble(val);
				return true;
			case 's': // "source"
				source = val;
				return true;
			default:
				return super.parseArg(key, val);
		}
	}

	public static void printHelp() {
		App.printHelp();
		System.out.println("Cut:");
		System.out.println("\t-b <beginning of timeframe>");
		System.out.println("\t-d <destination file name>");
		System.out.println("\t-e <end of timeframe>");
		System.out.println("\t-s <source file name>");
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		new Cut(args);
	}
}
