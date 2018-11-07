package edu.bonn.cs.iv.bonnmotion;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import edu.bonn.cs.iv.bonnmotion.models.DisasterArea;
import edu.bonn.cs.iv.util.IntegerHashMap;
import edu.bonn.cs.iv.util.IntegerHashSet;

/** Base class for creating new scenarios. */
public class Scenario extends App implements Model, ScenarioLink {
    protected MobileNode[] node; /* Mobile nodes. */
    protected double x = 200.0; /** Area x length [m]. */
    protected double y = 200.0; /** Area y length [m]. */
    protected double duration = 600.0; /** Duration of scenario [s]. */
    protected double ignore = 3600.0; /** Length of initial time span which is to be cut off after scenario generation [s]. */
    protected long randomSeed = System.currentTimeMillis(); /** Random seed to initialise RNG. */ // this is what the java.util.Random constructor does without parameter, too
    protected Building[] buildings = new Building[0]; /** Buildings */
    protected Random rand;
    public long count_rands;

    /** Name of the model */
    protected String modelName = null;
    protected boolean circular = false;
    protected double[] aFieldParams = null;
    protected AttractorField aField = null;

    /** if true generate() first must do transition */
    protected boolean isTransition = false;
    protected int transitionMode = 0;
    protected Scenario predecessorScenario = null;

    /** caches movements from last read(basename). null if read(basename) was not executed yet */
    public String movements = null;

    /**
     * Returns random double from the RandomSeed.
     * @return double
     */
    protected double randomNextDouble() {
            count_rands++;
            return getRand().nextDouble();
    }

	protected double randomNextDouble(final double value) {
		count_rands++;
		return (getRand().nextDouble()*value);
	}

    /**
     * Returns 1.0 or -1.0
     * @return 1.0 or -1.0
     */
    protected double randomNextPlusOrMinusOne() {
        count_rands++;
        if(getRand().nextBoolean()) { return 1.0; }
        else { return -1.0; }
    }

    /**
     * Returns a pseudorandom, uniformly distributed int value between 0 (inclusive) and the specified value (exclusive), drawn from this random number generator's sequence. (Docu taken from: http://download.oracle.com/javase/1.4.2/docs/api/java/util/Random.html#nextInt%28int%29)
     * @param n the bound on the random number to be returned. Must be positive.
     * @return a pseudorandom, uniformly distributed int value between 0 (inclusive) and n (exclusive).
     */
    protected int randomNextInt(int n) {
        count_rands++;
        return getRand().nextInt(n);
    }
	
	protected int randomNextInt() {
		count_rands++;
		return getRand().nextInt();
	}

    /**
     * Returns random Gaussian from the RandomSeed
     * @return double
     */
    protected double randomNextGaussian() {
            count_rands++;
            return getRand().nextGaussian();
    }

    /**
     * Returns random Weibull from the RandomSeed
     * @return double
     */
    protected double randomNextWeibull(double shape, double scale) {
        count_rands++;
        return scale * java.lang.Math.pow(-java.lang.Math.log(getRand().nextDouble()), 1.0 / shape);
    }

	public Scenario() {}

	public Scenario(int nodes, double x, double y, double duration, double ignore, long randomSeed) {
		node = new MobileNode[nodes];
		this.x = x;
		this.y = y;
		this.duration = duration;
		this.ignore = ignore;
		setRand(new Random(this.randomSeed = randomSeed));
		count_rands = 0;
	}

    public static Scenario getScenario(String basename) throws FileNotFoundException, IOException {
        if (Scenario.is3DScenario(basename)) {
            return new Scenario3D(basename);
        }
        else {
            return new Scenario(basename);
        }
    }


	protected Scenario(String basename) throws FileNotFoundException, IOException {
		read(basename);
		count_rands = 0;
	}

	protected Scenario(String basename, boolean haveLD) throws FileNotFoundException, IOException {
		read(basename, haveLD);
	}

	protected Scenario(String args[], Scenario _pre, Integer _transitionMode) {
		// we've got a predecessor, so a transtion is needed
		predecessorScenario = _pre;
		transitionMode = _transitionMode.intValue();
		isTransition = true;	
		count_rands = 0;
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
			if ( isTransition && predecessorScenario != null && ignore != 0 ) // Req. for chainscenarios
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
		case 'R' :
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
			System.out.println("randomSeed (String):"+val);
			randomSeed = Long.parseLong(val);
			System.out.println("randomSeed (Long):"+randomSeed);
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
		} else {
			return false;
		}
	}
	
    /**
     * Checks the .params file if the scenario is a 3D scenario
     * @param basename The name of the scenario to check
     * @return true if the scenario is a 3D scenario, false otherwise
     * @throws IOException if the scenario file was not found
     */
    protected static boolean is3DScenario(String basename) throws IOException {
        String line;
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(basename + ".params")));
        while ((line = in.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line, "=");
            String key = st.nextToken();
            st.nextToken();

            if (key.equals("z")) {
                return true;
            }
        }
        in.close();
        return false;
    }

	/** Called by subclasses before they generate node movements. */
	protected void preGeneration() {
		duration += ignore;
		setRand(new Random(randomSeed));

		if (aFieldParams != null) {
			aField = new AttractorField(x, y);
			aField.add(aFieldParams);
		}

		String myClass = this.getClass().getName();
		myClass = myClass.substring(myClass.lastIndexOf('.') + 1);

		if (modelName == null) {
			modelName = myClass;
		}
		else if (! modelName.equals(myClass)) {
			System.out.println("model mismatch: modelName=" + modelName + " myClass=" + myClass);
			System.exit(0);
		}
	}

	/** Called by subclasses after they generate node movements. */
	protected void postGeneration() {
		if (ignore < 600.0 && !isTransition ) {// this is a somewhat arbitrary value :)
			System.out.println("warning: setting the initial phase to be cut off to be too short may result in very weird scenarios");
		}
		if (ignore > 0.0) {
			cut(ignore, duration);
		}
		long next_seed = getRand().nextLong();
		while (Long.signum(next_seed) < 0) {
		    next_seed = getRand().nextLong();
		}
		System.out.println("Next RNG-Seed =" + next_seed+ " | #Randoms = "+count_rands);
	}

	/** Extract a certain time span from the scenario. */
	public void cut(double begin, double end) {
		if ((begin >= 0.0) && (end <= duration) && (begin <= end)) {
			for (int i = 0; i < node.length; i++) {
			    node[i].cut(begin, end);
			}
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

	public int getNumberOfNodes() {
		return node.length;
	}

	//vanishes ambulace parking point nodes 
	public MobileNode[] getNode(String Modelname, String basename){
		if(Modelname.equals(DisasterArea.getInfo().name)){
			IntegerHashSet VanishingNodes = searchVanishing(basename);
			/*Iterator<?> it = VanishingNodes.iterator();
			while(it.hasNext()){
				System.out.println("drin " + it.next());
			}*/
			int writtenNodes = 0;
			MobileNode[] r = new MobileNode[node.length - VanishingNodes.size()];
			for(int i = 0; i < node.length; i++){
				boolean vanish = false;
				Integer nodeaddress = new Integer(i);
				if(VanishingNodes.contains(nodeaddress)){
					vanish = true;
				}
				if(!vanish){
					System.arraycopy(node, i, r, writtenNodes, 1);
					writtenNodes++;
				}
			}
			return r;
		}
		/*else{
			System.out.println("doch nicht");
		}*/
		return null;
	}

	public IntegerHashSet searchVanishing(String basename){

		IntegerHashSet VanishingNodes = new IntegerHashSet();
		/*
		String line;
		String fromFile;
		try{
		BufferedReader in = new BufferedReader( new InputStreamReader( new FileInputStream( basename+".changes" ) ) );
		while ( (line=in.readLine()) != null ) {
			StringTokenizer st = new StringTokenizer(line);
			while(st.hasMoreTokens()){
				fromFile = st.nextToken();
				//System.out.println(fromFile);
				Integer node = new Integer(fromFile);
				VanishingNodes.add(node);
			}
		}
		in.close();
		}
		catch(Exception e){
			System.out.println("Error in searchVanishing of Scenario.java");
			System.exit(0);
		}
		 */
		return VanishingNodes;
	}

	public MobileNode getNode(int n) {
		try {
		    if (node[n] == null) {
				node[n] = new MobileNode();
		    }
		    return node[n];
		} catch(ArrayIndexOutOfBoundsException e) {
		    System.err.println("Fatal error: Requesting non-existing node" + e.getLocalizedMessage());
		    System.exit(-1);
		    return null;
		}
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

	public int nodeCount(String Modelname, String basename){
		if(Modelname.equals(DisasterArea.getInfo().name)){
			return node.length - searchVanishing(basename).size();
		}
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
			if (! parseArg(key, value) && (_warn) ) {
				System.out.println("warning: unknown key \"" + key + "\" while parsing arguments from \"" + _fn + "\"");
			}
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
	protected String read(String basename) throws FileNotFoundException, IOException {
		String help = read(basename, false);
		return help;
	}

	/**
	 * Reads the base information of a scenario from a
	 * file.
	 * It is typically invoked by application to re-read the processing
	 * scenario from a generated file.
	 * @param basename Basename of the scenario
	 * @param haveLD have pre-computed link dump or read movements.gz
	 */
	protected String read(String basename, boolean hasPrecomputedLinkDump) throws FileNotFoundException, IOException {
		String line;

		paramFromFile(basename+".params");

		int i = 0; 
		// read buildings
		if (buildings.length > 0) {
			BufferedReader bin =
				new BufferedReader(new InputStreamReader(new FileInputStream(basename + ".buildings")));
			// XXX: do sanity check that number of lines matches number of buildings
			while ((line = bin.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line);
				buildings[i] = new Building(Double.parseDouble(st.nextToken()),
						Double.parseDouble(st.nextToken()),
						Double.parseDouble(st.nextToken()),
						Double.parseDouble(st.nextToken()),
						Double.parseDouble(st.nextToken()),
						Double.parseDouble(st.nextToken()));
				i++;
			}
			bin.close();
		}

		// read movements
		StringBuilder movements = new StringBuilder();

		if (!hasPrecomputedLinkDump) {
			double extendedtime = 0.0;
			double xpos = 0.0;
			double ypos = 0.0;
			double status = 0.0;
			int j = 0;
			boolean nodestart = false;
			boolean nodestop = false;
			BufferedReader in = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(basename + ".movements.gz"))));
			i = 0;
			j = 0;
			while ((line = in.readLine()) != null) {
			    assert !(line.equals("#3D")) : "trying to read 3D scenario file using 2D code";
			    
			    //comment prefix
                if (line.startsWith("#")) {
                    continue;
                }
                
				if(!(getModelName().equals(DisasterArea.getInfo().name))){
					node[i] = new MobileNode();
				}
				StringTokenizer st = new StringTokenizer(line);
				while (st.hasMoreTokens()) {
					if(getModelName().equals(DisasterArea.getInfo().name)){
						switch(i%4) {
						case 0:
							extendedtime = Double.parseDouble(st.nextToken());
							if(extendedtime == 0.0){
								nodestart = true;
							}
							else{
								nodestart = false;
							}
							if(extendedtime == duration){
								nodestop = true;
							}
							else{
								nodestop = false;
							}
							break;
						case 1:
							xpos = Double.parseDouble(st.nextToken());
							break;
						case 2:
							ypos = Double.parseDouble(st.nextToken());
							break;
						case 3:
							status = Double.parseDouble(st.nextToken());
							if(nodestart){
								node[j] = new MobileNode();
							}
							Position extendedpos = new Position(xpos, ypos, status);
							if (!node[j].add(extendedtime, extendedpos)) {
								System.out.println( extendedtime + ": " + extendedpos.x + "/" + extendedpos.y );				
								throw new RuntimeException("Error while adding waypoint.");
							}
							if(nodestop){
								j++;
							}

							movements.append(" ");
							movements.append(extendedtime);
							movements.append(" ");
							movements.append(xpos);
							movements.append(" ");
							movements.append(ypos);
							movements.append(" ");
							movements.append(status);
							if(duration == extendedtime){
							    movements.append("\n");
							}
		
							break;
						default:
							break;
						}
					}
					else{
						double time = Double.parseDouble(st.nextToken());
						Position pos = new Position(Double.parseDouble(st.nextToken()),Double.parseDouble(st.nextToken()));
						if (!node[i].add(time, pos)) {
							System.out.println( time + ": " + pos.x + "/" + pos.y );				
							throw new RuntimeException("Error while adding waypoint.");
						}
					}
				}
				i++;
			}
			in.close();
		}
		this.movements = movements.toString();
		return this.movements;
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
	public void write(String basename, String[] params) throws FileNotFoundException, IOException {

		PrintWriter info = new PrintWriter(new FileOutputStream(basename + ".params"));
		info.println( "model=" + getModelName() );
		info.println( "ignore=" + ignore );
		info.println( "randomSeed=" + randomSeed );
		this.writeCoordinates(info);
		info.println( "duration=" + duration );
		info.println( "nn=" + node.length );
		info.println( "circular=" + circular );
		if ( params != null ) {
			for (int i = 0; i < params.length; i++) {
				info.println(params[i]);
			}
		}

		if (aFieldParams != null) {
			info.print("aFieldParams=" + aFieldParams[0]);
			for (int i = 1; i < aFieldParams.length; i++)
				info.print("," + aFieldParams[i]);
			info.println("");
		}

		info.close();
		PrintWriter movements =	new PrintWriter(new GZIPOutputStream(new FileOutputStream(basename + ".movements.gz")));

		if (this instanceof Scenario3D) {
		    movements.println("#3D");
		}
		
		for (int i = 0; i < node.length; i++) {
			movements.println(node[i].movementString());
		}
		movements.close();
	}

	public void writeCoordinates(PrintWriter info) {
		info.println("x=" + x);
		info.println("y=" + y);
	}

	/**
	 * Writes the generated scenario and the scenario
	 * parameters to files. Pays attention to status changes.
	 * @param basename Basename of the output files
	 */
	@SuppressWarnings("unchecked")
	public void write(String basename, String[] params, IntegerHashMap statuschanges) throws FileNotFoundException, IOException {
		PrintWriter info = new PrintWriter(new FileOutputStream(basename + ".params"));
		info.println( "model=" + getModelName() );
		info.println( "ignore=" + ignore );
		info.println( "randomSeed=" + randomSeed );
		this.writeCoordinates(info);
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
		PrintWriter movements = new PrintWriter(new GZIPOutputStream(new FileOutputStream(basename + ".movements.gz")));
		for (int i = 0; i < node.length; i++) {
			Integer nodeaddress = new Integer(i);
			LinkedList<Double> statuschangetimes = new LinkedList<Double>();
			String[] splitted = node[i].movementString().split(" ");
			int waypoints = splitted.length / 3;
			String[] towrite = new String[waypoints * 3 + waypoints];
			for(int j = 0; j < towrite.length; j++){
				towrite[j] = "0";
			}
			for(int j = 0; j < waypoints; j++){
				towrite[4*j] = splitted[3*j];
				towrite[4*j+1] = splitted[3*j+1];
				towrite[4*j+2] = splitted[3*j+2];
				if(statuschanges.get(nodeaddress) == null){
					towrite[4*j+3] = "0";
				}
				else{
					statuschangetimes = ((LinkedList<Double>)statuschanges.get(nodeaddress));
					Double time = new Double(towrite[4*j]);
					double cutignore = time.doubleValue() + ignore;
					Double realtime = new Double(cutignore);
					for(int k = 0; k < statuschangetimes.size(); k++){
						if(((Double)statuschangetimes.get(k)).compareTo(realtime) == 0){
							if((k%2) == 0){
								//borderentry
								towrite[4*j+3] = "2";
								break;
							}
							else{
								//borderexit
								towrite[4*j+3] = "1";
								break;
							}
						}
						else{
							//not on border, not status change
							towrite[4*j+3] = "0";
						}
					}
				}
			}
			for(int j = 0; j < towrite.length; j++){
				movements.println(towrite[j]);
			}
		}
		movements.close();
		read(basename);
	}

	/** Helper function for creating scenarios. */
	public Position randomNextPosition() {
	    assert !(this instanceof Scenario3D) : "using 2D method with 3D object";
	    
		return randomNextPosition(-1., -1.);
	}

	/** Helper function for creating scenarios. */
	public Position randomNextPosition(double fx, double fy) {
	    assert !(this instanceof Scenario3D) : "using 2D method with 3D object";
	       
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
			return new Waypoint( 0, randomNextPosition() );

		if (_pre.node.length != node.length)
			throw new ScenarioLinkException("#Node does not match");

		MobileNode[] preNodes = null;
		Waypoint w = null, nw = null;

		preNodes = _pre.getNode();
		w = preNodes[_nn].getLastWaypoint();
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

	public Random getRand() {
		return rand;
	}

	public void setRand(Random rand) {
		this.rand = rand;
	}
}
