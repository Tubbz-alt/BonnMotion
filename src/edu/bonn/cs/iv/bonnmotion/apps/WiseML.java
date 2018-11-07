/*******************************************************************************
 ** WiseML-Exporter for BonnMotion mobility generator                         **
 ** Copyright (C) 2009 Raphael Ernst					      				  **
 ** University of Bonn							                              **
 ** Institute of Computer Science 4					                          **
 ** Communication and Distributed Systems				                      **
 ** Code: Raphael Ernst                                                       **
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

public class WiseML extends App {
    static final int COMPRESSION_NONE = 0;
    static final int COMPRESSION_NOTABS = 1;
    static final int COMPRESSION_BEST = 2;

    static final int HEADER = 0;
    static final int NODE_LEVEL = HEADER+1;
    static final int DATA_LEVEL = NODE_LEVEL+1;
    static final int OTHER_LEVEL = DATA_LEVEL+1;

    protected static final String filesuffix = ".wml";

    protected String name = null;
    protected PrintWriter out = null;
    protected int compression = COMPRESSION_NONE;
    protected double intervalLength = 1.0;
    protected double defaultAltitude = 0;
    protected String path_header = null;
    protected String path_footer = null;
    protected String path_nodeId = null;
    protected boolean useIntegerTimes = false;

    public WiseML(String[] args) {
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
            s = Scenario.getScenario(name);
        } catch (Exception e) {
            App.exceptionHandler( "Error reading file", e);
        }

        MobileNode[] node = s.getNode();

        printWiseMLHead();

        final double duration = Math.ceil(s.getDuration());
        printWiseMLNodeMovement(node, duration);

        printWiseMLTail();

        closeWriter();
    }

    protected void printWiseMLHead() {
        catFile(path_header);
    }

    protected void printWiseMLTail() {
        catFile(path_footer);
    }

    protected void catFile(String _pathToFile) {
        if(_pathToFile != null) {
            BufferedReader input;
            try {
                input = new BufferedReader(new FileReader(_pathToFile));
                String line;
                while((line = input.readLine()) != null) {
                    print(line, HEADER);
                }
                input.close();
            }
            catch (IOException e) {
                System.err.println("IOError - Skipping this step. Errormessage: " + e.getLocalizedMessage());
            }
        }
    }

    protected void printWiseMLNodeMovement(MobileNode[] _nodes, final double _duration) {
        double t = 0;

		printWiseMLTimestamp(t);
		for(int currentNode=0;currentNode<_nodes.length;currentNode++) {
		    Position p = _nodes[currentNode].positionAt(t);
		    printWiseMLNodePosition(getNodeId(currentNode),p.x,p.y,this.defaultAltitude);
		}
		t += intervalLength;

        while(t<_duration+1) {
		    printWiseMLTimestamp(t);
		    for(int currentNode=0;currentNode<_nodes.length;currentNode++) {
				Position p = _nodes[currentNode].positionAt(t);
				Position oldPosition = _nodes[currentNode].positionAt(t-intervalLength);
				if(oldPosition.equals(p)) {
				    System.out.println("Omitting output of node " + getNodeId(currentNode) + ". It has not moved since last output at time " + (t-intervalLength) + ".");
				}
				else {
				    printWiseMLNodePosition(getNodeId(currentNode),p.x,p.y,this.defaultAltitude);
				}
		    }
		    t += intervalLength;
        }
    }

    protected void printWiseMLTimestamp(final double _value) {
		print("<timestamp>",OTHER_LEVEL);
		String timeToPrint;
		if(this.useIntegerTimes) {
		    final int value = new Double(_value).intValue();
		    timeToPrint = Integer.toString(value);
		}
		else {
		    timeToPrint = Double.toString(_value);
		}
		print(timeToPrint,OTHER_LEVEL+1);
		print("</timestamp>",OTHER_LEVEL);
    }

    protected void printWiseMLNodePosition(final String _nodeId, final double _posX, final double _posY, final double _posZ) {
		final String nodeId = "<node id=\"" + _nodeId + "\">";
		final String posX = "<x>" + _posX + "</x>";
		final String posY = "<y>" + _posY + "</y>";
		final String posZ = "<z>" + _posZ + "</z>";
	
		print(nodeId,OTHER_LEVEL);
		print("<position>",OTHER_LEVEL+1);
		print(posX,OTHER_LEVEL+2);
		print(posY,OTHER_LEVEL+2);
		print(posZ,OTHER_LEVEL+2);
		print("</position>",OTHER_LEVEL+1);
		print("</node>",OTHER_LEVEL);
    }

    protected String getNodeId(final int _nodeNumber) {
        String ret = null;

        if(path_nodeId != null) {
            BufferedReader nodeIds;
            try {
                nodeIds = new BufferedReader(new FileReader(path_nodeId));
                for(int i=0;i<=_nodeNumber;i++) {
                    ret = nodeIds.readLine();
                    if(ret == null) {
                        break;
                    }
                }
            }
            catch (IOException e) {
                System.err.println("Cannot assign node id using default value. Error message: " + e.getLocalizedMessage());
                ret = Integer.toString(_nodeNumber);
            }   
        }

        if(ret == null) {
            ret = Integer.toString(_nodeNumber);
        }

        return ret;
    }

    protected void print(String _writeOut) {
        print(_writeOut,0);
    }

    protected void print(String _writeOut, final int _level) {
        if(out == null) {
            out = openPrintWriter(name + filesuffix);
        }

        if(compression == COMPRESSION_NONE) {
            for(int level=0; level<_level; level++) {
                out.print("\t");
            }
        }

        out.print(_writeOut);

        if(compression != COMPRESSION_BEST) {
            out.println("");
        }
    }

    protected void closeWriter() {
        out.close();
        out = null;

    }
    
    protected boolean parseArg(char key, String val) {
        switch (key) {
        	case 'f':
                this.name = val;
                return true;
            case 'c':
                this.compression = Integer.parseInt(val);
                if(this.compression > COMPRESSION_BEST) {
                    System.err.println("Invalid compression defined! Using no compression.");
                    this.compression = COMPRESSION_NONE;
                }
                return true;
            case 'a':
                this.defaultAltitude = Double.parseDouble(val);
                return true;
        	case 'F':
                this.path_footer = val;
                return true;
        	case 'H':
                this.path_header = val;
                return true;
        	case 'I':
				this.useIntegerTimes = true;
				System.out.println("Warning: This (-I) will convert all printed times into integers!");
				return true;
        	case 'L':
				this.intervalLength = Double.parseDouble(val);
				return true;
            case 'N':
                this.path_nodeId = val;
                return true;
            default:
                return super.parseArg(key, val);
        }
    }

    public static void printHelp() {
        System.out.println();
        App.printHelp();
        System.out.println("WiseML:");
        System.out.println("Outputs node movement in WiseML");
        System.out.println("\t[-a <altitude>]\t(default 0)");
        System.out.println("\t[-c <compressionlevel>]\t(default 0) 0 = NONE, 1 = No tabs, 2 = No tabs, no newlines");
        System.out.println("\t-f <filename>\tScenario");
        System.out.println("\t[-F <path to file>]\tWiseML footer");
        System.out.println("\t[-H <path to file>]\tWiseML header");
        System.out.println("\t[-I]\t\t\tConvert times to integer values");
        System.out.println("\t[-L <double>]\t\tTime between two outputs (default 1.0)");
        System.out.println("\t[-N <path to file>]\tWiseML node ids");
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        new WiseML(args);
    }
}
