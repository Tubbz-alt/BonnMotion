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

import edu.bonn.cs.iv.bonnmotion.*;

/** Application that creates a movement file for ns-2. */

public class NSFile extends App {
	/** Add border around the scenario to prevent ns-2 from crashing. */
	public static final double border = 10.0;

	protected String name = null;

	public NSFile(String[] args) {
		go( args );
	}

	public void go( String[] args ) {
		parse(args);

		Scenario s = null;
		if ( name == null ) {
			printHelp();
			System.exit(0);
		}
		
		try {
			s = new Scenario(name);
		} catch (Exception e) {
			App.exceptionHandler( "Error reading file", e);
		}

		MobileNode[] node = s.getNode();
		
		PrintWriter settings = openPrintWriter(name + ".ns_params");
		settings.println("set val(x) " + (s.getX() + 2 * border));
		settings.println("set val(y) " + (s.getY() + 2 * border));
		settings.println("set val(nn) " + node.length);
		settings.println("set val(duration) " + s.getDuration());
		settings.close();

		PrintWriter movements_ns = openPrintWriter(name + ".ns_movements");
		for (int i = 0; i < node.length; i++) {
			String[] m = node[i].movementStringNS("$node_(" + i + ")", border);
			for (int j = 0; j < m.length; j++)
				movements_ns.println(m[j]);
		}
		movements_ns.close();
	}

	protected boolean parseArg(char key, String val) {
		switch (key) {
			case 'f':
				name = val;
				return true;
			default:
				return super.parseArg(key, val);
		}
	}

	public static void printHelp() {
		System.out.println();
		App.printHelp();
		System.out.println("NSFile:");
		System.out.println("\t-f <filename>");
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		new NSFile(args);
	}
}
