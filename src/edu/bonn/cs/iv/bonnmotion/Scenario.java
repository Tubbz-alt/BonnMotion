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

package edu.bonn.cs.iv.bonnmotion;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Base class for creating new scenarios. */

public class Scenario extends App implements Model, ScenarioLink {

	/** Mobile nodes. */
	protected MobileNode[] node;
	/** Area x length [m]. */
	protected double x = 200.0;
	/** Area y length [m]. */
	protected double y = 200.0;
	/** Duration of scenario [s]. */
	protected double duration = 600.0;
	/** Length of initial time span which is to be cut off after scenario generation [s]. */
	protected double ignore = 3600.0;
	/** Random seed to initialise RNG. */
	protected long randomSeed = System.currentTimeMillis(); // this is what the java.util.Random constructor does without parameter, too
   /** Buildings */
   protected Building[] buildings = new Building[0];

	protected Random rand;

        /** Name of the model */
        protected String modelName = null;

	protected boolean circular = false;
	protected double[] aFieldParams = null;
	protected AttractorField aField = null;

	/** if true generate() first must do transition */ 
	protected boolean isTransition = false;
	protected int transitionMode = 0;
	protected Scenario predecessorScenario = null;

	/**
	 * Returns random double form the RandomSeed.
	 * @return double
	 */
	protected double randomNextDouble() {
		return rand.nextDouble(); 
	}

	/**
	 * Returns random Gaussian form the RandomSeed
	 * @return double
	 */
	protected double randomNextGaussian() {
		return rand.nextGaussian();
	}

	public Scenario() {}

	public Scenario(int nodes, double x, double y, double duration, double ignore, long randomSeed) {
		node = new MobileNode[nodes];
		this.x = x;
		this.y = y;
		this.duration = duration;
		this.ignore = ignore;
		rand = new Random(this.randomSeed = randomSeed);
	}



	public Scenario(String basename) throws FileNotFoundException, IOException {
		read(basename);
	}
	
	public Scenario(String args[], Scenario _pre, Integer _transitionMode) {
		// we've got a predecessor, so a transtion is needed
		predecessorScenario = _pre;
		transitionMode = _transitionMode.intValue();
		isTransition = true;
	}	
	
	protected boolean parseArg(char key, String val) {
		switch (key) {
			case 'a':
				aFieldParams = parseDoubleArray(val);
				return true;
			case 'c' : // "circular"
				circular = true;
				return true;
			case 'd' : // "duration"
				duration = Double.parseDouble(val);
				return true;
			case 'i' : // "ignore" (Einschwingphase)
				ignore = Double.parseDouble(val);
				if ( isTransition && predecessorScenario != null && ignore != 0 ) // Falls ich ein Nachfolgeglied in einem ChainScenario bin
					System.out.println( "warning: Ingore is set to " + ignore + ". Are you sure you want this in a chained Scenario?" );	
				return true;
			case 'n' : // "nodes"
				node = new MobileNode[Integer.parseInt(val)];
				return true;
			case 'x' : // "x"
				x = Double.parseDouble(val);
				return true;
			case 'y' : // "y"
				y = Double.parseDouble(val);
				return true;
			case 'R' : // "R"
				randomSeed = Long.parseLong(val);
				return true;
			default :
				return super.parseArg(key, val);
		}
	}

	protected boolean parseArg(String key, String val) {
		if (key.equals("model")) {
			modelName = val;
			return true;
		} else if (key.equals("ignore") ) {
			ignore = Double.parseDouble(val);
			return true;
		} else if (	key.equals("randomSeed") ) {
			randomSeed = Long.parseLong(val);
			return true;
		} else if (	key.equals("x") ) {
			x = Double.parseDouble(val);
			return true;
		} else if (	key.equals("y") ) {
			y = Double.parseDouble(val);
			return true;
		} else if (	key.equals("duration") ) {
			duration = Double.parseDouble(val);
			return true;
		} else if (	key.equals("nn") ) {
			node = new MobileNode[Integer.parseInt(val)];
			return true;
		} else if (	key.equals("nbuildings") ) {
			buildings = new Building[Integer.parseInt(val)];
			return true;
		} else if (	key.equals("circular") ) {
			if (val.equals("true"))
				circular = true;
			return true;
		} else if (key.equals("aFieldParams")) {
			aFieldParams = parseDoubleArray(val);
			return true;
		} else return false;
	}

	/** Called by subclasses before they generate node movements. */
	protected void preGeneration() {
		duration += ignore;
		rand = new Random(randomSeed);

		if (aFieldParams != null) {
			aField = new AttractorField(x, y);
			aField.add(aFieldParams);
		}

		String myClass = this.getClass().getName();
		myClass = myClass.substring(myClass.lastIndexOf('.') + 1);
		
		if (modelName == null)
			modelName = myClass;
		else
			if (! modelName.equals(myClass)) {
				System.out.println("model mismatch: modelName=" + modelName + " myClass=" + myClass);
				System.exit(0);
			}
	}

	/** Called by subclasses after they generate node movements. */
	protected void postGeneration() {
			if (ignore < 600.0 && !isTransition ) // this is a somewhat arbitrary value :)
				System.out.println("warning: setting the initial phase to be cut off to be too short may result in very weird scenarios");
			if (ignore > 0.0)
				cut(ignore, duration);
	}

	/** Extract a certain time span from the scenario. */
	public void cut(double begin, double end) {
		if ((begin >= 0.0) && (end <= duration) && (begin <= end)) {
			for (int i = 0; i < node.length; i++)
				node[i].cut(begin, end);
			duration = end - begin;
		}
	}
		

	/**
	 * @see edu.bonn.cs.iv.bonnmotion.App#go(String[])
	 */
	public void go ( String[] _args ) {
		String paramFile = _args[0];
		String[] args = new String[_args.length - 1];
		System.arraycopy(_args, 1, args, 0, args.length);
		if (paramFile != null) {
			try {
				paramFromFile(paramFile, true);
			} catch (Exception e) {
				App.exceptionHandler( "Could not open parameter file", e );
			}
		}
		parse(args);
		if ( node == null ) {
			System.out.println("Please define the number of nodes.");
//			printHelp();
			System.exit(0);
		}
	}

	public static void printHelp() {
		App.printHelp();
		System.out.println("Scenario:");
		System.out.println("\t-a <attractor parameters (if applicable for model)>");
		System.out.println("\t-c [use circular shape (if applicable for model)]");
		System.out.println("\t-d <scenario duration>");
		System.out.println("\t-i <number of seconds to skip>");
		System.out.println("\t-n <number of nodes>");
		System.out.println("\t-x <width of simulation area>");
		System.out.println("\t-y <height of simulation area>");
		System.out.println("\t-R <random seed>");
	}

	public String getModelName() {
		return modelName;
	}
	
	public void setModelName( String _modelName ) {
		modelName = _modelName;
	}

	public double getDuration() {
		return duration;
	}

   public Building[] getBuilding() {
      Building[] b = new Building[buildings.length];
      System.arraycopy(buildings,0,b,0,buildings.length);
      return b;
   }

	public MobileNode[] getNode() {
		MobileNode[] r = new MobileNode[node.length];
		System.arraycopy(node, 0, r, 0, node.length);
		return r;
	}

	public MobileNode getNode(int n) {
		if (node[n] == null)
			node[n] = new MobileNode();
		return node[n];
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}
	
	public double getIgnore() {
		return ignore;
	}

	public long getRandomSeed() {
		return randomSeed;
	}
	
	public void setNode( MobileNode[] _node ) {
		node = _node;
	}
	public int nodeCount() {
		return node.length;
	}

	/**
	 * Does the same job as paramFronFile but w/o showing warnings.
	 * @see Scenario#paramFromFile(String _fn)
	 * @param _fn Filename
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void paramFromFile(String _fn) throws FileNotFoundException, IOException {
		paramFromFile( _fn, false );
	}
	
	/**
	 * Reads arguments from specific file. Then processes
	 * the command line arguments (overrides arguments from file).<br>
	 * This Method must be implemented in every subclass.
	 * @param _fn Filename
	 * @param _warn if warnings should be shown during parsing
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void paramFromFile(String _fn, boolean _warn) throws FileNotFoundException, IOException {
		String line;
		BufferedReader in = new BufferedReader( new InputStreamReader( new FileInputStream( _fn ) ) );
		while ( (line=in.readLine()) != null ) {
			StringTokenizer st = new StringTokenizer(line, "=");
			String key = st.nextToken();
			String value = st.nextToken();
			if (! parseArg(key, value) && (_warn) )
				System.out.println("warning: unknown key \"" + key + "\" while parsing arguments from \"" + _fn + "\"");
		}
		in.close();
	}

	/**
	 * Reads the base information of a scenario from a
	 * file.
	 * It is typically invoked by application to re-read the processing
	 * scenario from a generated file.
	 * @param basename Basename of the scenario
	 */
	public void read(String basename) throws FileNotFoundException, IOException {
		String line;

		paramFromFile(basename+".params");

      int i = 0;
      // read buildings
      if (buildings.length > 0) {
         BufferedReader bin =
            new BufferedReader(
               new InputStreamReader(
               new FileInputStream(basename + ".buildings")));
         // XXX: do sanity check that number of lines matches number of buildings
         while ((line = bin.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line);
            //System.out.println(i+" "+buildings.length+" "+buildings[0].x1);
            buildings[i] = new Building(Double.parseDouble(st.nextToken()),
                                        Double.parseDouble(st.nextToken()),
                                        Double.parseDouble(st.nextToken()),
                                        Double.parseDouble(st.nextToken()),
                                        Double.parseDouble(st.nextToken()),
                                        Double.parseDouble(st.nextToken()));
            //buildings[i].x1 = Double.parseDouble(st.nextToken()); 
            //buildings[i].x2 = Double.parseDouble(st.nextToken()); 
            //buildings[i].y1 = Double.parseDouble(st.nextToken()); 
            //buildings[i].y2 = Double.parseDouble(st.nextToken()); 
            //buildings[i].doorx = Double.parseDouble(st.nextToken()); 
            //buildings[i].doory = Double.parseDouble(st.nextToken()); 
            i++;
         }
         bin.close();
      }

      // read movements
		BufferedReader in =
			new BufferedReader(
				new InputStreamReader(
					new GZIPInputStream(new FileInputStream(basename + ".movements.gz"))));
		i = 0;
		while ((line = in.readLine()) != null) {
			node[i] = new MobileNode();
			StringTokenizer st = new StringTokenizer(line);
			while (st.hasMoreTokens()) {
				double time = Double.parseDouble(st.nextToken());
				Position pos =
					new Position(
						Double.parseDouble(st.nextToken()),
						Double.parseDouble(st.nextToken()));
				if (!node[i].add(time, pos)) {
					System.out.println( time + ": " + pos.x + "/" + pos.y );				
					throw new RuntimeException("Error while adding waypoint.");
				}
			}
			i++;
		}
		in.close();
	}

	public void setDuration(double _duration) {
		duration = _duration;
	}

	public void write( String _name ) throws FileNotFoundException, IOException {
		write(_name, null);
	}

	/**
	 * Writes the generated scenario and the scenario
	 * parameters to files.
	 * @param basename Basename of the output files
	 */
	public void write(String basename, String[] params)
		throws FileNotFoundException, IOException {

		PrintWriter info = new PrintWriter(new FileOutputStream(basename + ".params"));
		info.println( "model=" + getModelName() );
		info.println( "ignore=" + ignore );
		info.println( "randomSeed=" + randomSeed );
		info.println( "x=" + x );
		info.println( "y=" + y );
		info.println( "duration=" + duration );
		info.println( "nn=" + node.length );
		info.println( "circular=" + circular );
		if ( params != null )
			for (int i = 0; i < params.length; i++)
				info.println(params[i]);
		
		if (aFieldParams != null) {
			info.print("aFieldParams=" + aFieldParams[0]);
			for (int i = 1; i < aFieldParams.length; i++)
				info.print("," + aFieldParams[i]);
			info.println("");
		}
		
		info.close();
		PrintWriter movements =
			new PrintWriter(
				new GZIPOutputStream(new FileOutputStream(basename + ".movements.gz")));
		for (int i = 0; i < node.length; i++) {
			movements.println(node[i].movementString());
		}
		movements.close();
	}

	/** Helper function for creating scenarios. */
	public Position randomNextPosition() {
		return randomNextPosition(-1., -1.);
	}

	/** Helper function for creating scenarios. */
	public Position randomNextPosition(double fx, double fy) {
		double x2 = 0., y2 = 0., r = 0., rx = 0., ry = 0.;
		if (circular) {
			x2 = x/2.0;
			y2 = y/2.0;
			r = (x2 < y2) ? x2 : y2;
		}
		Position pos = null;
		do {
			if (aField == null) {
				rx = (fx < 0.) ? x * randomNextDouble() : fx;
				ry = (fy < 0.) ? y * randomNextDouble() : fy;
			} else {
				pos = aField.getPos(randomNextDouble(), randomNextGaussian(), randomNextGaussian());
				if (pos != null) {
					rx = pos.x;
					ry = pos.y;
				}
			}
		} while (((aField != null) && (pos == null)) || (circular && (Math.sqrt((rx - x2) * (rx - x2) + (ry - y2) * (ry - y2)) > r)));
		if (pos == null)
			return new Position(rx, ry);
		else
			return pos;
	}
	
	public Waypoint transition(Scenario _pre, int _mode, int _nn) throws ScenarioLinkException {
		if (_pre == null) // No predecessor => We start an 0/0 @ 0.0
//			return new Waypoint(0, new Position(0, 0));
		return new Waypoint( 0, randomNextPosition() );

		if (_pre.node.length != node.length)
			throw new ScenarioLinkException("#Node does not match");

		MobileNode[] preNodes = null;
		Waypoint w = null, nw = null;

		preNodes = _pre.getNode();
		w = preNodes[_nn].lastElement();
		switch (_mode) {
			case LINKMODE_FAST :
				nw = transitionWaypointFast( w );
				break;
			case LINKMODE_MOVE :
				nw = transitionWaypointMove( w, _nn );
				break;
			default :
				throw new ScenarioLinkException("Unknown Mode");
		}
		node[_nn].add(nw.time, nw.pos);

/*
				System.out.println( "PreNode(t:x,y) -> Node(t:x,y) Mode:"  + _mode );
				System.out.print( w.time + ":" + w.pos.x + "," + w.pos.y );
				System.out.println( " - 0:" + nw.pos.x + "," + nw.pos.y );
*/
		//		System.out.println(" done.");
		return nw;
	}
	
	public Waypoint transitionWaypointFast( Waypoint _w) {
		Waypoint w = null;
		
		//		The predecessor Scenario is greater: if the node is outside the new field: realocate the node 
		if ( (_w.pos.x > x) || (_w.pos.y > y) ) {
			System.out.println( "\t\tOut!!!!  X: "+ _w.pos.x +" / Y: "+ _w.pos.y );
			double xRe =  _w.pos.x - (int)(_w.pos.x / x) * (_w.pos.x%x); 
			double yRe =  _w.pos.y - (int)(_w.pos.y / y) * (_w.pos.y%y);
			System.out.println( "\t\tNew Pos: X: " + xRe + " / Y: " + yRe);
			w = new Waypoint( 0.0, new Position( xRe, yRe) );
		} else {
			w = new Waypoint( 0.0, _w.pos );
		}
		
		return w;
	}
	
	public Waypoint transitionWaypointMove( Waypoint _w, int _nn) {
		Waypoint w = transitionWaypointFast( _w );

		if ( (w.pos.x != _w.pos.x) || (w.pos.y != _w.pos.y) ) {
			node[_nn].add( 0.0, _w.pos );
			return new Waypoint(2.0, w.pos);
		} else {
			return new Waypoint( 0.0, _w.pos );
		}
	}

}
