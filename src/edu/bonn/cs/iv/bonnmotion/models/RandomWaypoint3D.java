package edu.bonn.cs.iv.bonnmotion.models;

import java.io.FileNotFoundException;
import java.io.IOException;

import edu.bonn.cs.iv.bonnmotion.MobileNode3D;
import edu.bonn.cs.iv.bonnmotion.ModuleInfo;
import edu.bonn.cs.iv.bonnmotion.Position3D;
import edu.bonn.cs.iv.bonnmotion.RandomSpeedBase3D;
import edu.bonn.cs.iv.bonnmotion.Scenario3D;
import edu.bonn.cs.iv.bonnmotion.ScenarioLinkException;
import edu.bonn.cs.iv.bonnmotion.Waypoint;

public class RandomWaypoint3D extends RandomSpeedBase3D {
    private static ModuleInfo info;
    
    static {
        info = new ModuleInfo("RandomWaypoint3D");
        info.description = "Application to construct RandomWaypoint (3D) mobility scenarios";
        
        info.major = 1;
        info.minor = 0;
        info.revision = ModuleInfo.getSVNRevisionStringValue("$LastChangedRevision: 269 $");
        
        info.contacts.add(ModuleInfo.BM_MAILINGLIST);
        info.authors.add("Florian Schmitt");
		info.affiliation = ModuleInfo.UNIVERSITY_OF_BONN;
    }
    
    public static ModuleInfo getInfo() {
        return info;
    }

	protected int dim = 4;
	
	private double meshNodeDistance = -1.;
	
	public RandomWaypoint3D(int nodes, double x, double y, double z, double duration, double ignore, long randomSeed, double minspeed, double maxspeed, double maxpause, int dim) {
		super(nodes, x, y, z, duration, ignore, randomSeed, minspeed, maxspeed, maxpause);
		this.dim = dim;
		generate();
	}

	public RandomWaypoint3D(String[] args) {
		go(args);
	}
	
	public void go(String[] args) {
		super.go(args);
		generate();
	}
	
	public RandomWaypoint3D(String args[], Scenario3D _pre, Integer _transitionMode){
		predecessorScenario = _pre;
		transitionMode = _transitionMode.intValue();
		isTransition = true;
		go(args);
	}

	
	public void generate(){
		preGeneration();	// Sets Random Seed & Duration += ignore-Time & Attractor Field
		
		for(int i = 0; i < node.length; i++){
			node[i] = new MobileNode3D();
			double t = 0.;
			Position3D src = null;
			if(isTransition){
				try{
					Waypoint lastW = transition(predecessorScenario, transitionMode, i);
					src = (Position3D)lastW.pos;
					t = lastW.time;
				} catch(ScenarioLinkException e){
					e.printStackTrace();
				}
			} 
			else{
				src = randomNextPosition();
			}
			while(t < duration){
				Position3D dst;
				if(!node[i].add(t, src))
					throw new RuntimeException(getInfo().name + ".go: error while adding waypoint (1)");
			
				switch(dim){
					case 1 :		// movement only on x-axis
						dst = randomNextPosition(-1., src.y, -1.);
						break;
					case 2 : 		// movement on x or y-axis
						switch((int)(randomNextDouble() * 2.0)){
							case 0 :
								dst = randomNextPosition(-1., src.y, src.z);
								break;
							case 1 :
								dst = randomNextPosition(src.x, -1., src.z);
								break;
							default :
								throw new RuntimeException(getInfo().name + ".go: This is impossible - how can (int)(randomNextDouble() * 2.0) be something other than 0 or 1?!");
						}
						break;
					case 3 : 		// movement on x, y or z-axis
						switch((int)(randomNextDouble() * 3.0)){
							case 0 :
								dst = randomNextPosition(-1., src.y, src.z);
								break;
							case 1 :
								dst = randomNextPosition(src.x, -1., src.z);
								break;
							case 2 :
								dst = randomNextPosition(src.x, src.y, -1.);
								break;
							default :
								throw new RuntimeException(getInfo().name + ".go: This is impossible - how can (int)(randomNextDouble() * 3.0) be something other than 0, 1 or 2?!");
						}
						break;
					case 4 : 		// classical Random Waypoint
						dst = randomNextPosition();
						break;
					default :
						throw new RuntimeException(getInfo().name + ".go: dimension may only be of value 1, 2, 3 or 4.");
				}
				double speed = (maxspeed - minspeed) * randomNextDouble() + minspeed;
				double dist = src.distance(dst);
				double time = dist / speed;
				t += time;
				if(!node[i].add(t, dst))
					throw new RuntimeException(getInfo().name + ".go: error while adding waypoint (2)");
				if((t < duration) && (maxpause > 0.0)){
					double pause = maxpause * randomNextDouble();
					t += pause;
				}
				src = dst;
			}
		}
		postGeneration();
	}

    protected boolean parseArg(String key, String value) {
        if (key.equals("dim")) {
            dim = Integer.parseInt(value);
            return true;
        } else return super.parseArg(key, value);
    }
	
	protected boolean parseArg(char key, String val) {
		switch (key) {
			case 'o': // "dimensiOn"
				dim = Integer.parseInt(val);
				if ((dim < 1) || (dim > 4)) {
					System.out.println("dimension must be between 1 and 4");
					System.exit(0);
				}
				if ((aFieldParams != null) && (dim != 4))
					System.out.println("warning: attractor field not used if dim != 4");
				return true;
			case 'm': // set mesh node distance
			    meshNodeDistance = Double.parseDouble(val);
			    return true;
			default:
				return super.parseArg(key, val);
		}
	}
	
	public static void printHelp() {
	    System.out.println(getInfo().toDetailString());
		RandomSpeedBase3D.printHelp();
		System.out.println( getInfo().name + ":");
		System.out.println("\t-o <dimension: 1: x only, 2: x or y, 3: x or y or z, 4: x and y and z>");
	}
	
	protected void postGeneration() {
		for ( int i = 0; i < node.length; i++ ) {

			Waypoint l = node[i].getLastWaypoint();
			if (l.time > duration) {
				Position3D p = (Position3D)node[i].positionAt(duration);
				node[i].removeLastElement();
				node[i].add(duration, p);
			}
		}
		
		if (meshNodeDistance > 0)
		    addMeshNodes();
    
		super.postGeneration();
	}
	
	private void addMeshNodes()
    {
        int numMeshX = (int)Math.floor(x / meshNodeDistance);
        int numMeshY = (int)Math.floor(y / meshNodeDistance);
        int numMeshZ = (int)Math.floor(z / meshNodeDistance);
        int numMeshNodes = numMeshX * numMeshY * numMeshZ;

        System.out.println("Adding a grid of " + numMeshNodes + " static mesh nodes...");

        MobileNode3D[] nodeNew = new MobileNode3D[node.length + numMeshNodes];
        for (int i = 0; i < node.length; i++)
            nodeNew[i] = (MobileNode3D)node[i];
        for (int i = 0; i < numMeshNodes; i++)
            nodeNew[node.length + i] = new MobileNode3D();

        for (int j = 0; j < numMeshY; j++)
        {
            for (int i = 0; i < numMeshX; i++)
                nodeNew[node.length + j*numMeshX + i].add(0.0, new Position3D((i+1) * meshNodeDistance, (j+1) * meshNodeDistance, (i+1)*(j+1)*meshNodeDistance));
        }

        node = nodeNew;
    }
	
	public void write( String _name ) throws FileNotFoundException, IOException {
		String[] p = new String[1];
		p[0] = "dim=" + dim;
		super.write(_name, p);
	}
}
