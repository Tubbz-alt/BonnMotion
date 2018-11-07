/*******************************************************************************
 ** BonnMotion - a mobility scenario generation and analysis tool             **
 ** Copyright (C) 2002-2010 University of Bonn                                **
 ** Code: Zia-Ul-Huda (based on RandomStreet)                                 **
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

package edu.bonn.cs.iv.bonnmotion.models;

import java.io.*;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import edu.bonn.cs.iv.bonnmotion.BoundingBox;
import edu.bonn.cs.iv.bonnmotion.HttpMapRequest;
import edu.bonn.cs.iv.bonnmotion.MobileNode;
import edu.bonn.cs.iv.bonnmotion.ModuleInfo;
import edu.bonn.cs.iv.bonnmotion.Position;
import edu.bonn.cs.iv.bonnmotion.Scenario;
import edu.bonn.cs.iv.bonnmotion.Waypoint;
import edu.bonn.cs.iv.bonnmotion.HttpMapRequest.ORSStartPositionFailedException;
import edu.bonn.cs.iv.bonnmotion.models.slaw.Cluster;
import edu.bonn.cs.iv.bonnmotion.models.slaw.ClusterMember;
import edu.bonn.cs.iv.bonnmotion.models.slaw.SLAWBase;
import edu.bonn.cs.iv.util.maps.CoordinateTransformation;
import edu.bonn.cs.iv.util.maps.CoordinateTransformation.proj4lib;

/** Application to construct MSLAW mobility scenarios. */

public class MSLAW extends SLAWBase {
    private static ModuleInfo info;
    
    static {
        info = new ModuleInfo("MSLAW");
        info.description = "Application to construct MSLAW mobility scenarios";
        
        info.major = 1;
        info.minor = 0;
        info.revision = ModuleInfo.getSVNRevisionStringValue("$LastChangedRevision: 428 $");
        
        info.contacts.add(ModuleInfo.BM_MAILINGLIST);
        info.authors.add("Zia-Ul-Huda");
        info.authors.add("Matthias Schwamborn");
    }
    
    public static ModuleInfo getInfo() {
        return info;
    }

    private static final boolean DEBUG = true;
    private String parameterFile = null;
    /** bounding box for the underlying map */
    private BoundingBox mapBBox = null;
    /** bounding box for routes (map bbox with margin) */
    private BoundingBox routeBBox = null;
    /** ORS distance metric ("Fastest"/"Shortest"/"Pedestrian") */
    private String orsDistMetric = "Pedestrian";
    /** projection of the input positions */
    private CoordinateTransformation transformation = null;
    /** EPSG code of the projection */
    private int epsgCode = 0;
    /** speed parameters */
    private double minSpeed = -1;
    private double maxSpeed = -1;
    /** max iterations for ORS request loop */
    private int maxORSRequestIterations = 30;
    /** ORS URL */
    private String orsUrl = null;
    /** log filename */
    private String logFile = null;
    /** distance method */
    private String distanceMethod = "Real";

    public MSLAW(int nodes, double x, double y, double duration, double ignore, long randomSeed) {
        super(nodes, x, y, duration, ignore, randomSeed);
        generate();
    }

    public MSLAW(String[] args) {
        go(args);
    }

    public void go(String[] args) {
        super.go(args);
        generate();
    }

    /**
     * generates waypoints for SLAW model
     * 
     * @return array of waypoint Position
     */
    private Position[] generate_waypoints() {
        // Number of levels as mentioned in original paper "SLAW: A Mobility Model for Human Walks"
        // in: Proc. of the IEEE Infocom (2009).
        int levels = 8;
        // convert hurst to alpha as done in matlab implementation by Seongik Hong, NCSU, US
        // (3/10/2009)
        double converted_hurst = 2 - 2 * hurst;
        // initial variance at first level as used in matlab implementation by Seongik Hong, NCSU,
        // US (3/10/2009)
        double initial_variance = 0.8;

        // variances for all levels
        double[] xvalues = new double[levels];
        double[] level_variances = new double[levels];

        for (int i = 0; i < levels; i++) {
            xvalues[i] = Math.pow(4, i + 1);
            level_variances[i] = initial_variance * Math.pow(xvalues[i] / xvalues[0], converted_hurst);
        }

        Hashtable<String, Integer> wpoints = new Hashtable<String, Integer>();
        Hashtable<String, Double> Xoffset = new Hashtable<String, Double>();
        Hashtable<String, Double> Yoffset = new Hashtable<String, Double>();
        wpoints.put("0,0", this.noOfWaypoints);
        Xoffset.put("0,0", 0.0);
        Yoffset.put("0,0", 0.0);
        double Xwind, Ywind;

        for (int level = 0; level < levels; level++) {
            System.out.println("Level " + (level + 1) + " of " + levels + " started.");
            // Number of squares at current level
            double n_squares = Math.pow(4, level);
            Xwind = mapBBox.width() / Math.pow(2, level);
            Ywind = mapBBox.height() / Math.pow(2, level);

            for (int square = 0; square < n_squares; square++) {
                if (square % 2000 == 0 && square != 0) {
                    System.out.println(square + " of " + n_squares + " processed.");
                }
                // generate the ofsets of x and y for children squares
                double val;
                double xval = Xoffset.get(level + "," + square);
                double yval = Yoffset.get(level + "," + square);

                for (int i = 0; i < 4; i++) {
                    val = xval;
                    // add window size to the Xoff set of second and third child square
                    if (i == 1 || i == 3) {
                        val += Xwind / 2;
                    }
                    Xoffset.put((level + 1) + "," + (4 * square + i), val);
                    
                    val = yval;
                    // add window size to the Yoff set of third and fourth child square
                    if (i == 2 || i == 3) {
                        val += Ywind / 2;
                    }
                    Yoffset.put((level + 1) + "," + (4 * square + i), val);
                }

                // get waypoints assigned to this node
                int wp = wpoints.get(level + "," + square);
                if (wp == 0) {
                    // assign 0 to all child nodes as waypoints
                    for (int i = 0; i < 4; i++) {
                        wpoints.put((level + 1) + "," + (4 * square + i), 0);
                    }
                } else if (level == 0) {
                    // first level
                    int[] num = devide_waypoints(wp, level_variances[level]);
                    for (int i = 0; i < 4; i++) {
                        wpoints.put((level + 1) + "," + (4 * square + i), num[i]);
                    }
                } else {
                    // inner levels
                    double[] cur_wp = new double[(int)Math.pow(4, level)];
                    for (int i = 0; i < cur_wp.length; i++) {
                        cur_wp[i] = wpoints.get(level + "," + i);
                    }

                    double avg = calculate_average(cur_wp);

                    for (int i = 0; i < Math.pow(4, level); i++) {
                        cur_wp[i] /= avg;
                    }

                    double var = calculate_var(cur_wp) + 1;
                    int[] num = devide_waypoints(wp, ((level_variances[level] + 1) / var) - 1);
                    for (int i = 0; i < 4; i++) {
                        wpoints.put((level + 1) + "," + (4 * square + i), num[i]);
                    }
                }
            }// for squares
        }// for level

        // create waypoints
        Xwind = mapBBox.width() / Math.sqrt(Math.pow(4, levels));
        Ywind = mapBBox.height() / Math.sqrt(Math.pow(4, levels));
        Position routable_point = find_routable_position();
        if (routable_point == null) {
            throw new RuntimeException(getInfo().name + ".generate_waypoints(): Error: Could not find a random starting point.");
        }

        System.out.println("Routable Position found at " + routable_point.toString());

        int total_squares = (int)Math.pow(4, levels);
        Vector<Position> waypoints = new Vector<Position>();
        int w;
        Position temp;
        int count = 0;
        for (int i = 0; i < total_squares; i++) {
            // get waypoints of current square
            w = wpoints.get(levels + "," + i);
            if (w != 0) {
                for (int j = 0; j < w; j++) {
                    temp = randomNextMapPosition(Xoffset.get(levels + "," + i), Yoffset.get(levels + "," + i), Xwind / 2, Ywind / 2, true,
                            routable_point, waypoints);
                    if (temp != null) {
                        // store the new calculated waypoint 
                        waypoints.add(temp);
                    }
                    count++;
                    
                    if (count % 100 == 0) {
                        System.out.println(count + " waypoints of " + this.noOfWaypoints + " tested. " + waypoints.size() + " generated.");
                    }
                }
            }
        }

        return waypoints.toArray(new Position[0]);
    }

    private Position find_routable_position() {
        double xx, yy;
        Position src = null, dst = null;

        for (int i = 0; i < this.maxORSRequestIterations; i++) {
            xx = mapBBox.origin().x + (randomNextDouble() * mapBBox.width());
            yy = mapBBox.origin().y + (randomNextDouble() * mapBBox.height());
            src = new Position(xx, yy);

            xx = mapBBox.origin().x + (randomNextDouble() * mapBBox.width());
            yy = mapBBox.origin().y + (randomNextDouble() * mapBBox.height());
            dst = new Position(xx, yy);

            Position[] route = null;
            try {
                route = HttpMapRequest.getORSRouteWaypoints(src, dst, epsgCode, orsDistMetric, orsUrl);
                if (route != null) {
                    break;
                }
            } catch (ORSStartPositionFailedException e) {
                route = null;
            }
        }

        return src;
    }

    public void generate() {
        preGeneration();

        if (isTransition) {
            System.out.println("Warning: Ignoring transition...");
        }

        if (this.waypoints == null) {
            System.out.println("Generating Waypoints.\n\n");
            
            this.waypoints = generate_waypoints();
            this.noOfWaypoints = waypoints.length;
        }

        System.out.println("Generating Clusters.\n\n");
        Cluster[] clusters = generate_clusters(this.waypoints);
        System.out.println(clusters.length + " Clusters found.");

        // These variables have values same as in the matlab implementation of SLAW model by Seongik
        // Hong, NCSU, US (3/10/2009)
        int powerlaw_step = 1;
        int levy_scale_factor = 1;
        int powerlaw_mode = 1;

        Hashtable<String, Double> distances = new Hashtable<String, Double>();

        System.out.println("Trace generation started.\n");
        
out:    for (int user = 0; user < node.length; user++) {
            double t = 0.0;
            node[user] = new MobileNode();

            // get random clusters and waypoints
            Cluster[] clts = make_selection(clusters, null, false);
            // total list of waypoints assigned
            ClusterMember[] wlist = get_waypoint_list(clts);
            // random source node
            int src = (int)Math.floor(randomNextDouble() * wlist.length);
            int dst = -1;
            int count = 0;

            while (t < duration) {
                if (DEBUG) {
                    //System.out.println("Current Node: " + user + "\tCurrent time: " + t);
                }
                count = 0;
                Position source = wlist[src].pos;
                addWaypoint(node[user], t, source);

                wlist[src].is_visited = true;
                // get list of not visited locations
                for (int i = 0; i < wlist.length; i++) {
                    if (!wlist[i].is_visited) {
                        count++;
                    }
                }
                // if all waypoints are visited then change one of clusters randomly. Destructive
                // mode of original SLAW matlab implementation by Seongik Hong, NCSU, US (3/10/2009)
                while (count == 0) {
                    clts = make_selection(clusters, clts, true);
                    wlist = get_waypoint_list(clts);
                    for (int i = 0; i < wlist.length; i++) {
                        if (!wlist[i].is_visited) {
                            if (source.distance(wlist[i].pos) != 0.0) {
                                count++;
                            }
                            else {
                                wlist[i].is_visited = true;
                            }
                        }
                    }
                }

                ClusterMember[] not_visited = new ClusterMember[count];
                count = 0;
                for (int i = 0; i < wlist.length; i++) {
                    if (!wlist[i].is_visited) {
                        not_visited[count++] = wlist[i];
                    }
                }
                // get distance from source to all remaining waypoints
                double[] dist = new double[not_visited.length];
                for (int i = 0; i < not_visited.length; i++) {
                    if (distanceMethod.equals("Real")) {
                        if (distances.containsKey(source.toString() + "," + not_visited[i].pos.toString())) {
                            dist[i] = distances.get(source.toString() + "," + not_visited[i].pos.toString());
                        }
                        else if (distances.containsKey(not_visited[i].pos.toString() + "," + source.toString())) {
                            dist[i] = distances.get(not_visited[i].pos.toString() + "," + source.toString());
                        }
                        else {
                            int num = 0;
                            while (num < maxORSRequestIterations) {

                                dist[i] = HttpMapRequest.getORSRouteDistance(source, not_visited[i].pos, epsgCode, orsDistMetric, orsUrl);
                                if (dist[i] != -1) {
                                    break;
                                }
                                num++;
                            }
                            if (num >= maxORSRequestIterations) {
                                System.out.println("Error: no valid distance after " + maxORSRequestIterations + " tries!");
                                System.exit(-1);
                            }
                            distances.put(source.toString() + "," + not_visited[i].pos.toString(), dist[i]);
                        }
                    }
                    else {
                        dist[i] = source.distance(not_visited[i].pos);
                    }
                }

                double[] weights = new double[not_visited.length];
                // cumulative sum of distance weights
                for (int i = 0; i < weights.length; i++) {
                    weights[i] = 0;
                    for (int j = 0; j <= i; j++) {
                        weights[i] += 1 / Math.pow(dist[j], this.dist_weight);
                    }
                }

                for (int i = 0; i < weights.length; i++) {
                    weights[i] /= weights[weights.length - 1];
                }

                double r = randomNextDouble();
                int index;
                for (index = 0; index < weights.length; index++) {
                    if (r < weights[index]) {
                        break;
                    }
                }
                
                if (index == weights.length) {
                    index--;
                }

                // select the next destination
                for (int i = 0; i < wlist.length; i++) {
                    if (wlist[i].pos.x == not_visited[index].pos.x && wlist[i].pos.y == not_visited[index].pos.y) {
                        dst = i;
                        break;
                    }
                }

                Position destination = wlist[dst].pos;

                Position[] route = null;
                int numIterations = 0;
                while (route == null) // loop to account for ORS errors
                {
                    // prevent infinite while loop
                    if (numIterations >= maxORSRequestIterations) {
                        System.err.println("Error: no valid RouteResponse after " + maxORSRequestIterations + " tries!");
                        break;
                    }
                    
                    try { 
                        // get route waypoints by querying OpenRouteService
                        route = HttpMapRequest.getORSRouteWaypoints(source, destination, epsgCode, orsDistMetric, orsUrl);
                    } catch (ORSStartPositionFailedException e) {
                        if (DEBUG) {
                            System.err.println(e.getMessage());
                            System.err.println("ORS failed to generate route. discarding this node.");
                        }
                        
                        user--;
                        continue out;
                    }
                    numIterations++;
                }

                // if no route could be received discard destination
                if (route == null) {
                    wlist[dst].is_visited = true;
                    continue;
                }
                
                // add route to waypoint list (drive to destination)
                t = addRoute(node[user], t, route);

                // select pause time by power law formula
                if ((t < duration) && (this.maxpause > 0.0)) {
                    t += random_powerlaw(powerlaw_step, levy_scale_factor, powerlaw_mode)[0];
                }
                // change destination to next source
                src = dst;

            }
            System.out.println("Node " + (user + 1) + " of " + node.length + " done.");
        }

        postGeneration();
    }

    protected boolean parseArg(String key, String value) {
        if (key.equals("paramFile")) {
            parameterFile = value;
            return true;
        }
        else
            return super.parseArg(key, value);
    }

    protected boolean parseArg(char key, String value) {
        switch (key) {
            case 'p': // "Parameter file"
                parameterFile = value;
                return true;
            case 'u': // ORS URL
                orsUrl = value;
                return true;
            case 'w': // write node movement to file
                logFile = value;
                return true;
            case 'F': // provide waypoint csv
                waypoints_filename = value;
                return true;
            default:
                return super.parseArg(key, value);
        }
    }

    public void write(String _name) throws FileNotFoundException, IOException {
        String[] p = new String[1];
        p[0] = "paramFile=" + parameterFile;
        super.write(_name, p);

        try {
            PrintWriter csv = new PrintWriter(new FileOutputStream(_name + "_waypoints.csv"));

            for (Position pos : this.waypoints) {
                csv.println(pos.getMovementStringPart());
            }

            csv.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void printHelp() {
        System.out.println(getInfo().toDetailString());
        Scenario.printHelp();
        System.out.println(getInfo().name + ":");
        System.out.println("\t-p <parameter file>");
    }

    protected void preGeneration() {
        super.preGeneration();

        parseParameterFile(parameterFile);

        if (minSpeed > maxSpeed || minSpeed <= 0 || maxSpeed <= 0) {
            throw new RuntimeException(getInfo().name + ".preGeneration(): There is something wrong with the speed values!");
        }

        x = routeBBox.width();
        y = routeBBox.height();
        mapBBox.setTransformation(transformation);
        routeBBox.setTransformation(transformation);
        
        if (this.waypoints_filename != null) {
            System.out.println("Loading waypoints from file: " + this.waypoints_filename + "\n");
            this.waypoints = readWaypointsFromFile(this.waypoints_filename);
            this.noOfWaypoints = this.waypoints.length;
        }
    }

    protected void postGeneration() {
        for (int i = 0; i < node.length; i++) {
            // remove waypoints exceeding duration
            Waypoint l = node[i].getLastWaypoint();
            if (l.time < duration)
                node[i].add(duration, l.pos);
            else {
                while (l.time > duration) {
                    node[i].removeLastElement();
                    l = node[i].getLastWaypoint();
                    if (l.time < duration)
                        node[i].add(duration, node[i].positionAt(duration));
                }
            }
        }

        double failedORSRequests = HttpMapRequest.countFailedORSRequests / (double)HttpMapRequest.countORSRequests;
        DecimalFormat df = new DecimalFormat("###.##%");
        System.out.println("\n#OSM queries = " + HttpMapRequest.countOSMQueries + " | #ORS requests = " + HttpMapRequest.countORSRequests
                + " (" + df.format(failedORSRequests) + " failed)");

        if (logFile != null)
            mywrite();

        super.postGeneration();
    }

    private void parseParameterFile(String fileName) {
        try {
            FileReader fr = new FileReader(fileName);
            BufferedReader fileIn = new BufferedReader(fr);

            // parse parameters
            String line = null;
            while ((line = fileIn.readLine()) != null) {
                if (line.startsWith("#"))
                    continue;

                StringTokenizer st = new StringTokenizer(line, "=");
                String key = st.nextToken();
                String value = st.nextToken();

                if (DEBUG)
                    System.out.println("DEBUG: key = \"" + key + "\", value = \"" + value + "\"");

                if (key.equals("MapBBox")) {
                    String[] v = value.split(" ");
                    double left = Double.parseDouble(v[0]);
                    double bottom = Double.parseDouble(v[1]);
                    double right = Double.parseDouble(v[2]);
                    double top = Double.parseDouble(v[3]);
                    double margin = Double.parseDouble(v[4]);

                    if (DEBUG)
                        System.out.println("DEBUG: grid = (" + left + ", " + bottom + ", " + right + ", " + top + "), margin = " + margin);

                    if (margin < 0) {
                        System.out.println("Error: margin must be >= 0!");
                        System.exit(0);
                    }
                    if (margin > 1)
                        System.out.println("Warning: margin is > 1, are you sure you want to add a margin with size > 100% of the original bounding box?");

                    mapBBox = new BoundingBox(left, bottom, right, top);

                    double marginX = mapBBox.width() * margin * 0.5;
                    double marginY = mapBBox.height() * margin * 0.5;
                    routeBBox = new BoundingBox(left - marginX, bottom - marginY, right + marginX, top + marginY);

                    if (DEBUG)
                        System.out.println("DEBUG: routeBBox = (" + routeBBox.origin().x + ", " + routeBBox.origin().y + ", "
                                + (routeBBox.origin().x + routeBBox.width()) + ", " + (routeBBox.origin().y + routeBBox.height()) + ")");
                }
                else if (key.equals("ORSDistanceMetric")) {
                    orsDistMetric = value;
                    String[] allowedMetrics = { "Fastest", "Shortest", "Pedestrian" };

                    if (!Arrays.asList(allowedMetrics).contains(orsDistMetric)) {
                        System.out.println("Warning: ORSDistanceMetric must be one of: " + 
                                Arrays.asList(allowedMetrics).toString() + "... setting to default: \"Fastest\"");
                        orsDistMetric = "Fastest";
                    }
                }
                else if (key.equals("EPSGCode")) {
                    epsgCode = Integer.parseInt(value);
                    transformation = new CoordinateTransformation("epsg:"+value, proj4lib.PROJ4J);
                    if (DEBUG)
                    	System.out.println("DEBUG: Proj4 string = \"" + transformation.getProj4Description() + "\"");
                }
                else if (key.equals("Speed")) {
                    String[] v = value.split(" ");
                    minSpeed = Double.parseDouble(v[0]);
                    maxSpeed = Double.parseDouble(v[1]);

                    if (DEBUG)
                        System.out.println("DEBUG: minSpeed = " + minSpeed + ", maxSpeed = " + maxSpeed);
                }
                else if (key.equals("Pause")) {
                    String[] v = value.split(" ");
                    minpause = Double.parseDouble(v[0]);
                    maxpause = Double.parseDouble(v[1]);

                    if (DEBUG)
                        System.out.println("DEBUG: minPause = " + minpause + ", maxPause = " + maxpause);
                }
                else if (key.equals("MaxORSRequestIterations")) {
                    String[] v = value.split(" ");
                    maxORSRequestIterations = Integer.parseInt(v[0]);

                    if (DEBUG)
                        System.out.println("DEBUG: maxORSRequestIterations = " + maxORSRequestIterations);
                }
                else if (key.equals("Waypoints")) {
                    noOfWaypoints = Integer.parseInt(value);

                    if (DEBUG)
                        System.out.println("DEBUG: Waypoints = " + maxpause);
                }
                else if (key.equals("Beta")) {
                    beta = Double.parseDouble(value);

                    if (DEBUG)
                        System.out.println("DEBUG: Beta = " + maxpause);
                }
                else if (key.equals("Hurst")) {
                    hurst = Double.parseDouble(value);

                    if (DEBUG)
                        System.out.println("DEBUG: Hurst = " + hurst);
                }
                else if (key.equals("DistWeight")) {
                    dist_weight = Double.parseDouble(value);

                    if (DEBUG)
                        System.out.println("DEBUG: DistWeight = " + dist_weight);
                }
                else if (key.equals("ClusterRange")) {
                    cluster_range = Double.parseDouble(value);

                    if (DEBUG)
                        System.out.println("DEBUG: ClusterRange = " + cluster_range);
                }
                else if (key.equals("ClusterRatio")) {
                    cluster_ratio = Integer.parseInt(value);

                    if (DEBUG)
                        System.out.println("DEBUG: ClusterRatio = " + cluster_ratio);
                }
                else if (key.equals("WaypointRatio")) {
                    waypoint_ratio = Integer.parseInt(value);

                    if (DEBUG)
                        System.out.println("DEBUG: WaypointRatio = " + waypoint_ratio);
                }
                else if (key.equals("DistanceMethod")) {
                    distanceMethod = value;

                    if (DEBUG)
                        System.out.println("DEBUG: DistanceMethod = " + distanceMethod);
                }
                else
                    continue;
            }

            fileIn.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    // write nodes' movement to file
    private void mywrite() {
        try {
            FileWriter fileOut = new FileWriter(logFile);
            for (int i = 0; i < node.length; i++) {
                fileOut.write("# node " + i + " movement\n");
                for (int j = 0; j < node[i].getNumWaypoints(); j++)
                    fileOut.write(node[i].getWaypoint(j).time + " " + node[i].getWaypoint(j).pos.x + " " + node[i].getWaypoint(j).pos.y
                            + "\n");
                fileOut.write("\n");
            }
            fileOut.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private double addRoute(MobileNode node, double t, Position[] route) {
        Position src = scenarioToMapPosition(node.getLastWaypoint().pos);
        Position dst = null;
        for (int i = 0; i < route.length; i++) {
            dst = route[i];
            double speed = (maxSpeed - minSpeed) * randomNextDouble() + minSpeed;
            double dist = src.distance(dst);
            double time = dist / speed;
            t += time;
            addWaypoint(node, t, dst);
            if (t >= duration)
                return t;

            src = dst;
        }

        return t;
    }

    private void addWaypoint(MobileNode node, double t, Position mapPosition) {
        if (!routeBBox.contains(mapPosition)) {
            System.out.println("Warning: Position " + mapPosition.toString() + " lies outside the route bounding box!");
            System.exit(42);
        }

        Position scenarioPosition = mapToScenarioPosition(mapPosition);

        Position old = null;
        double dist = -1.0;
        if (node.getNumWaypoints() > 0) {
            old = scenarioToMapPosition(node.getLastWaypoint().pos);
            dist = mapPosition.distance(old);
            // dirty hack: even if the map positions are equal, the corresponding scenario positions
            // might differ
            if (node.getLastWaypoint().time == t && (dist == 0.0 || old.toString().equals(mapPosition.toString())))
                return;
        }
        if (!node.add(t, scenarioPosition)) {
            System.out.println("Error: Adding waypoint " + mapPosition.toString() + " failed (there was already a waypoint "
                    + old.toString() + " at time " + t + "; dist = " + dist + ")!");
            System.exit(0);
        }
    }

    private Position mapToScenarioPosition(Position mapPosition) {
        Position origin = routeBBox.origin();
        return new Position(mapPosition.x - origin.x, mapPosition.y - origin.y);
    }

    private Position scenarioToMapPosition(Position scenarioPosition) {
        Position origin = routeBBox.origin();
        return new Position(origin.x + scenarioPosition.x, origin.y + scenarioPosition.y);
    }

    private Position randomNextMapPosition(double Xoffset, double Yoffset, double Xwind, double Ywind, boolean is_ignore,
            Position routable_point, Vector<Position> waypoints) {

        double theta = 2 * Math.PI * randomNextDouble();
        double xx = mapBBox.origin().x + Xoffset + Xwind + (randomNextDouble() * Xwind) * Math.cos(theta);
        double yy = mapBBox.origin().y + Yoffset + Ywind + (randomNextDouble() * Ywind) * Math.sin(theta);

        Position result = null;
        Position[] route = null;

        int numIterations = 0;
        while (result == null) {
            // prevent infinite while loop
            if (numIterations == maxORSRequestIterations) {
                if (!is_ignore) {
                    System.out.println("Error: no valid random position could be generated after " + maxORSRequestIterations + " tries!");
                    System.exit(-1);
                }
                else {
                    System.out.println("Warning: Waypoint dropped after " + maxORSRequestIterations + " relocation tries.");
                    return null;
                }
            }
            result = new Position(xx, yy);

            if (!waypoints.contains(result)) {
                try {
                    route = HttpMapRequest.getORSRouteWaypoints(result, routable_point, epsgCode, orsDistMetric, orsUrl);
                } catch (ORSStartPositionFailedException e) {
                    route = null;
                }
            }

            if (route == null) {
                System.out.println("Could not find a routable position in try number " + (++numIterations) + ".");
                result = null;

                theta = 2 * Math.PI * randomNextDouble();
                xx = mapBBox.origin().x + Xoffset + Xwind + (randomNextDouble() * Xwind) * Math.cos(theta);
                yy = mapBBox.origin().y + Yoffset + Ywind + (randomNextDouble() * Ywind) * Math.sin(theta);
            }
        }

        return result;
    }
}
