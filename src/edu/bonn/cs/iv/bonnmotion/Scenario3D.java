package edu.bonn.cs.iv.bonnmotion;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

//import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import edu.bonn.cs.iv.bonnmotion.models.DisasterArea;
import edu.bonn.cs.iv.util.IntegerHashMap;
import edu.bonn.cs.iv.util.IntegerHashSet;

public class Scenario3D extends Scenario {
    protected double z = 200.0; /** Area z length [m]. */

    public Scenario3D() {
    }

    public Scenario3D(int nodes, double x, double y, double z, double duration, double ignore, long randomSeed) {
        node = new MobileNode3D[nodes];
        this.x = x;
        this.y = y;
        this.z = z;
        this.duration = duration;
        this.ignore = ignore;
        setRand(new Random(this.randomSeed = randomSeed));
        count_rands = 0;
    }

    protected Scenario3D(String basename) throws FileNotFoundException, IOException {
        read(basename);
        count_rands = 0;
    }

    protected Scenario3D(String basename, boolean haveLD) throws FileNotFoundException, IOException {
        read(basename, haveLD);
    }

    protected Scenario3D(String args[], Scenario3D _pre, Integer _transitionMode) {
        // we've got a predecessor, so a transition is needed
        predecessorScenario = _pre;
        transitionMode = _transitionMode.intValue();
        isTransition = true;
        count_rands = 0;
    }
    
    public double getZ() {
        return z;
    }

    protected boolean parseArg(char key, String val) {
        switch (key) {
            case 'z': // "z"
                z = Double.parseDouble(val);
                return true;
            case 'n': // "nodes"
                node = new MobileNode3D[Integer.parseInt(val)];
                return true;
            default:
                return super.parseArg(key, val);
        }
    }

    @Override
    protected boolean parseArg(String key, String val) {
        if (key.equals("z")) {
            z = Double.parseDouble(val);
            return true;
        }
        else if (key.equals("nn")) {
            node = new MobileNode3D[Integer.parseInt(val)];
            return true;
        }
        else {
            return super.parseArg(key, val);
        }
    }

    @Override
    public void writeCoordinates(PrintWriter info) {
        info.println("x=" + x);
        info.println("y=" + y);
        info.println("z=" + z);
    }

    /** Helper function for creating scenarios. */
    @Override
    public Position3D randomNextPosition() {
        return randomNextPosition(-1., -1., -1.);
    }

    public Position3D randomNextPosition(double fx, double fy, double fz) {
        double x2 = 0., y2 = 0., z2 = 0, r = 0., rx = 0., ry = 0., rz = 0.;
        if (circular) {
            x2 = x / 2.0;
            y2 = y / 2.0;
            z2 = z / 2.0;
            r = (x2 < y2) ? ((x2 < z2) ? x2 : z2) : ((y2 < z2) ? y2 : z2);
        }
        Position3D pos = null;
        do {
            if (aField == null) {
                rx = (fx < 0.) ? x * randomNextDouble() : fx;
                ry = (fy < 0.) ? y * randomNextDouble() : fy;
                rz = (fz < 0.) ? z * randomNextDouble() : fz;
            }
            else {
                /*
                 * pos = aField.getPos(randomNextDouble(), randomNextGaussian(),
                 * randomNextGaussian()); if (pos != null) { rx = pos.x; ry = pos.y;
                 */
                throw new RuntimeException("Not Implemented");
            }
        }
        while (((aField != null) && (pos == null))
                || (circular && (Math.sqrt((rx - x2) * (rx - x2) + (ry - y2) * (ry - y2) + (rz - z2) * (rz - z2)) > r)));
        if (pos == null) return new Position3D(rx, ry, rz);
        else return pos;
    }

    public static void printHelp() {
        Scenario.printHelp();
        System.out.println("Scenario3D:");
        System.out.println("\t-z <depth of simulation area>");
    }

    /**
     * Reads the base information of a scenario from a file. It is typically invoked by application
     * to re-read the processing scenario from a generated file.
     * 
     * @param basename
     *            Basename of the scenario
     */
    @Override
    protected String read(String basename) throws FileNotFoundException, IOException {
        String help = read(basename, false);
        return help;
    }

    private static boolean negativeHeightWarningShowed = false;
    
    /**
     * Reads the base information of a scenario from a file. It is typically invoked by application
     * to re-read the processing scenario from a generated file.
     * 
     * @param basename
     *            Basename of the scenario
     * @param haveLD
     *            have pre-computed link dump or read movements.gz
     */
    @Override
    protected String read(String basename, boolean hasPrecomputedLinkDump) throws FileNotFoundException, IOException {
        String line;

        paramFromFile(basename + ".params");

        int i = 0;
        // read buildings
        if (buildings.length > 0) {
            BufferedReader bin = new BufferedReader(new InputStreamReader(new FileInputStream(basename + ".buildings")));
            // XXX: do sanity check that number of lines matches number of buildings
            while ((line = bin.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line);
                buildings[i] = new Building(Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()), Double.parseDouble(st
                        .nextToken()), Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()), Double.parseDouble(st
                        .nextToken()));
                i++;
            }
            bin.close();
        }

        // read movements

        // String movements = new String();
        StringBuilder movements = new StringBuilder();

        if (!hasPrecomputedLinkDump) {
            double extendedtime = 0.0;
            double xpos = 0.0;
            double ypos = 0.0;
            double zpos = 0.0;
            double status = 0.0;
            int j = 0;
            boolean nodestart = false;
            boolean nodestop = false;
            BufferedReader in = new BufferedReader(new InputStreamReader(new GZIPInputStream(
                    new FileInputStream(basename + ".movements.gz"))));
            i = 0;
            j = 0;
            while ((line = in.readLine()) != null) {
                //comment prefix
                if (line.startsWith("#")) {
                    continue;
                }
                
                if (!(getModelName().equals(DisasterArea.getInfo().name))) {
                    node[i] = new MobileNode3D();
                }
                StringTokenizer st = new StringTokenizer(line);
                while (st.hasMoreTokens()) {
                    if (getModelName().equals(DisasterArea.getInfo().name)) {
                        switch (i % 5) {
                            case 0:
                                extendedtime = Double.parseDouble(st.nextToken());
                                if (extendedtime == 0.0) {
                                    nodestart = true;
                                }
                                else {
                                    nodestart = false;
                                }
                                if (extendedtime == duration) {
                                    nodestop = true;
                                }
                                else {
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
                                zpos = Double.parseDouble(st.nextToken());
                                break;
                            case 4:
                                status = Double.parseDouble(st.nextToken());
                                if (nodestart) {
                                    node[j] = new MobileNode3D();
                                }
                                Position3D extendedpos = new Position3D(xpos, ypos, zpos, status);
                                if (!node[j].add(extendedtime, extendedpos)) {
                                    System.out.println(extendedtime + ": " + extendedpos.x + "/" + extendedpos.y + "/" + extendedpos.z);
                                    throw new RuntimeException("Error while adding waypoint.");
                                }
                                if (nodestop) {
                                    j++;
                                }

                                movements.append(" ");
                                movements.append(extendedtime);
                                movements.append(" ");
                                movements.append(xpos);
                                movements.append(" ");
                                movements.append(ypos);
                                movements.append(" ");
                                movements.append(zpos);
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
                    else {
                        double time = Double.parseDouble(st.nextToken());
                        Position3D pos = new Position3D(Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()), Double
                                .parseDouble(st.nextToken()));
                        
                        if (!negativeHeightWarningShowed && pos.z < 0) {
                            System.err.printf("NOTE: your input contains a node with a negative z-value (%f;%f;%f).\n"+
                                            "The following behaviour is not tested enough. Especially be careful with the resulting statistics!\n", pos.x, pos.y, pos.z);
                            negativeHeightWarningShowed = true;
                        }
                        
                        if (!node[i].add(time, pos)) {
                            System.out.println(time + ": " + pos.x + "/" + pos.y + "/" + pos.z);
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

    @Override
    public Building3D[] getBuilding() {
        Building3D[] b = new Building3D[buildings.length];
        System.arraycopy(buildings, 0, b, 0, buildings.length);
        return b;
    }

    @Override
    public MobileNode3D[] getNode() {
        MobileNode3D[] r = new MobileNode3D[this.node.length];
        System.arraycopy(this.node, 0, r, 0, this.node.length);
        return r;
    }

    // vanishes ambulace parking point nodes
    @Override
    public MobileNode3D[] getNode(String Modelname, String basename) {
        if (Modelname.equals(DisasterArea.getInfo().name)) {
            IntegerHashSet VanishingNodes = searchVanishing(basename);

            int writtenNodes = 0;
            MobileNode3D[] r = new MobileNode3D[node.length - VanishingNodes.size()];
            for (int i = 0; i < node.length; i++) {
                boolean vanish = false;
                Integer nodeaddress = new Integer(i);
                if (VanishingNodes.contains(nodeaddress)) {
                    vanish = true;
                }
                if (!vanish) {
                    System.arraycopy(node, i, r, writtenNodes, 1);
                    writtenNodes++;
                }
            }
            return r;
        }
        return null;
    }

    @Override
    public MobileNode3D getNode(int n) {
        try {
            if (node[n] == null) {
                node[n] = new MobileNode3D();
            }
            return (MobileNode3D)node[n];
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Fatal error: Requesting non-existing node" + e.getLocalizedMessage());
            System.exit(-1);
            return null;
        }

    }
    
    /**
     * Creates a (2D-)Scenario from 3DScenario.
     * 
     */
    public static Scenario3D convertFrom2DScenario(Scenario source, double z) {
        Scenario3D s = new Scenario3D();

        s.aField = source.aField;
        s.aFieldParams = source.aFieldParams;
        s.buildings = source.buildings;
        s.circular = source.circular;
        s.count_rands = source.count_rands;
        s.duration = source.duration;
        s.ignore = source.ignore;
        s.isTransition = source.isTransition;
        s.modelName = source.modelName + "from2D";
        s.predecessorScenario = source.predecessorScenario;
        s.setRand(source.getRand());
        s.randomSeed = source.randomSeed;
        s.transitionMode = source.transitionMode;
        s.x = source.x;
        s.y = source.y;
        s.z = z;
        
        MobileNode3D[] nodes = new MobileNode3D[source.nodeCount()];
        int nodeindex = 0;
        for (MobileNode n : source.node) {
            nodes[nodeindex] = new MobileNode3D();

            for (Waypoint w : n.waypoints) {
                nodes[nodeindex].waypoints.add(new Waypoint(w.time, new Position3D(w.pos.x, w.pos.y, z, w.pos.status)));
            }
            nodeindex++;
        }
        s.setNode(nodes);
        
        return s;
    }
    
    /**
     * Creates a (2D-)Scenario from 3DScenario.
     * 
     */
    public static Scenario convertTo2DScenario(Scenario source) {
        Scenario s = new Scenario();

        s.aField = source.aField;
        s.aFieldParams = source.aFieldParams;
        s.buildings = source.buildings;
        s.circular = source.circular;
        s.count_rands = source.count_rands;
        s.duration = source.duration;
        s.ignore = source.ignore;
        s.isTransition = source.isTransition;
        s.modelName = source.modelName + "to2D";
        s.predecessorScenario = source.predecessorScenario;
        s.setRand(source.getRand());
        s.randomSeed = source.randomSeed;
        s.transitionMode = source.transitionMode;
        s.x = source.x;
        s.y = source.y;
        
        MobileNode[] nodes = new MobileNode[source.nodeCount()];
        int nodeindex = 0;
        for (MobileNode n : source.node) {
            nodes[nodeindex] = new MobileNode();

            for (Waypoint w : n.waypoints) {
                nodes[nodeindex].waypoints.add(new Waypoint(w.time, new Position(w.pos.x, w.pos.y, w.pos.status)));
            }
            nodeindex++;
        }
        s.setNode(nodes);
        
        return s;
    }
}
