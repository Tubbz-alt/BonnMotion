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

import edu.bonn.cs.iv.util.Heap;
import edu.bonn.cs.iv.bonnmotion.*;
import edu.bonn.cs.iv.bonnmotion.*;

import java.io.*;
import java.util.StringTokenizer;
import java.util.Vector;
import edu.bonn.cs.iv.bonnmotion.*;

/** Application that generates a motion file according to Horst Hellbrücks XML schema. */

public class SPPXml extends App {
	protected String name = null;
	protected double node_range = 250;
	protected int idx = 0;

	public SPPXml(String[] args) throws FileNotFoundException, IOException {
		go( args );
	}

	public void go( String[] args ) throws FileNotFoundException, IOException  {
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

		MobileNode[] nodes = s.getNode();
		
		PrintWriter m = openPrintWriter(name + ".xml");

		print_header(m);
		print_parameter(m, s);
		print_node_settings(m, nodes);
		print_mobility(m, nodes);
		print_statistics(m);
		print_footer(m);
		m.close();
	}


    private void print_header(PrintWriter m) {
	m.println("<?xml version=\"1.0\" ?>");
	m.print("<simulation xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        m.println(" xsi:noNamespaceSchemaLocation=\"http://www.i-u.de/schools/hellbrueck/ansim/xml/sppmobtrace.xsd\">");
    }

    private void print_footer(PrintWriter m) {
	m.println("</simulation>");
    }

    private void print_statistics(PrintWriter m) {
	m.println("\t<statistics>");
	m.println("\t</statistics>");
    }
    
    private void print_parameter(PrintWriter m, Scenario s) {
	m.println("\t<parameter>");
	m.println("\t\t<field_shape>rectangular</field_shape>");
	m.println("\t\t<xmin>0.0</xmin>");
	m.println("\t\t<xmax>" + s.getX() + "</xmax>");
	m.println("\t\t<ymin>0.0</ymin>");
	m.println("\t\t<ymax>" + s.getY()  + "</ymax>");
	m.println("\t\t<numberOfNodes>" + s.nodeCount()  + "</numberOfNodes>");
	m.println("\t\t<mobility_model>" + s.getModelName() + "</mobility_model>");
	m.println("\t</parameter>");
    }

    private void print_node_settings(PrintWriter m, MobileNode[] nodes) {
	m.println("\t<node_settings>");
	for (int i = 0; i < nodes.length; i++) {

	    Position pos = nodes[i].positionAt(0);

	    m.println("\t\t<node>");
	    m.println("\t\t\t<node_id>" + i + "</node_id>");
	    m.println("\t\t\t<range>" + node_range  + "</range>");
	    m.println("\t\t\t<position>");
	    m.println("\t\t\t\t<xpos>" + pos.x + "</xpos>");
	    m.println("\t\t\t\t<ypos>" + pos.y  + "</ypos>");
	    m.println("\t\t\t</position>");
	    m.println("\t\t</node>");
	}
	m.println("\t</node_settings>");
    }

    private void print_mobility(PrintWriter m, MobileNode[] nodes) {
	int [] indexes;
	indexes = new int[nodes.length];

	for (int i = 0; i < nodes.length; i++) {
	    indexes[i] = 0;
	}

	m.println("\t<mobility>");

	// Write movements in chronological order
	while(true) {
	    double min = Double.POSITIVE_INFINITY;
	    int i = -1;
	    for (int j = 0; j < nodes.length; j++) {
		if (indexes[j] < nodes[j].numWaypoints() - 1) {
		    Waypoint wp = nodes[j].getWaypoint(indexes[j]);
		    if (wp.time < min) {
			min = wp.time;
			i = j;
		    }
		}
	    }
	    if (i == -1) {
		break;
	    }
	    print_movement(m, i, nodes[i].getWaypoint(indexes[i]), nodes[i].getWaypoint(indexes[i]+1));
	    indexes[i]++;
	}

	m.println("\t</mobility>");
    }

    private void print_movement(PrintWriter m, int nodeid, Waypoint wa, Waypoint wb) {
	if ((wa.pos.x != wb.pos.x) || (wa.pos.y != wb.pos.y)) { // Pauses are not exported
	    m.println("\t\t<position_change>");
	    m.println("\t\t\t<node_id>" + nodeid  + "</node_id>");
	    m.println("\t\t\t<start_time>" + wa.time  + "</start_time>");
	    m.println("\t\t\t<end_time>" + wb.time  + "</end_time>");
	    m.println("\t\t\t<destination>");
	    m.println("\t\t\t\t<xpos>" + wb.pos.x  + "</xpos>");
	    m.println("\t\t\t\t<ypos>" + wb.pos.y  + "</ypos>");
	    m.println("\t\t\t</destination>");
	    m.println("\t\t</position_change>");
	}
    }

	protected boolean parseArg(char key, String val) {
		switch (key) {
			case 'f':
				name = val;
				return true;
			case 'r':
				node_range = Double.parseDouble(val);
				return true;
			default:
				return super.parseArg(key, val);
		}
	}

	public static void printHelp() {
		App.printHelp();
		System.out.println("SPPXml:");
		System.out.println("\t-f <scenario name>");
		System.out.println("\t-r <node range>");
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		new SPPXml(args);
	}
}
