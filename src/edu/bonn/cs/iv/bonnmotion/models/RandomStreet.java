/*******************************************************************************
 ** BonnMotion - a mobility scenario generation and analysis tool             **
 ** Copyright (C) 2002-2010 University of Bonn                                **
 ** Code: Matthias Schwamborn                                                 **
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
import java.util.StringTokenizer;

import edu.bonn.cs.iv.bonnmotion.BoundingBox;
import edu.bonn.cs.iv.bonnmotion.HttpMapRequest;
import edu.bonn.cs.iv.bonnmotion.MobileNode;
import edu.bonn.cs.iv.bonnmotion.Position;
import edu.bonn.cs.iv.bonnmotion.Scenario;
import edu.bonn.cs.iv.bonnmotion.Waypoint;

import com.jhlabs.map.proj.*;

/** Application to construct RandomStreet mobility scenarios. */

public class RandomStreet extends Scenario
{
    private static final String MODEL_NAME = "RandomStreet";

    private static final boolean DEBUG = true;

    private String parameterFile = null;
    /** bounding box for the underlying map */
    private BoundingBox mapBBox = null;
    /** bounding box for routes (map bbox with margin) */
    private BoundingBox routeBBox = null;
    /** ORS distance metric ("Fastest" or "Shortest") */
    private String orsDistMetric = "Fastest";
    /** projection of the input positions */
    private Projection proj = null;
    /** EPSG code of the projection */
    private int epsgCode = 0;
    /** speed parameters */
    private double minSpeed = -1;
    private double maxSpeed = -1;
    private double maxPause = -1;
    /** distance between neighbouring mesh nodes */
    private double meshNodeDistance = -1;
    /** max iterations for ORS request loop */
    private int maxORSRequestIterations = 30;
    /** ORS URL */
    private String orsUrl = null;
    /** log filename */
    private String logFile = null;

    public RandomStreet(int nodes,
                        double x,
                        double y,
                        double duration,
                        double ignore,
                        long randomSeed)
    {
        super(nodes, x, y, duration, ignore, randomSeed);
        generate();
    }

    public RandomStreet(String[] args)
    {
        go(args);
    }

    public void go(String[] args)
    {
        super.go(args);
        generate();
    }

    public void generate()
    {
        preGeneration();

        if (isTransition)
            System.out.println("Warning: Ignoring transition...");

        for (int i = 0; i < node.length; i++)
        {
            double t = 0.0;
            node[i] = new MobileNode();

            // set initial position of mobile node
            Position start = randomNextMapPosition();
            addWaypoint(node[i], t, start);

            while (t < duration)
            {
                if (maxPause > 0.0)
                {
                    double pause = maxPause * randomNextDouble();
                    t += pause;
                }

                Position[] route = null;
                int numIterations = 0;
                while (route == null) // loop to account for ORS errors
                {
                    // prevent infinite while loop
                    if (numIterations > maxORSRequestIterations)
                    {
                        System.out.println("Error: no valid ORS RouteResponse after " + maxORSRequestIterations + " tries!");
                        System.exit(-1);
                    }
                    
                    Position dst = randomNextMapPosition();

                    // get route waypoints by querying OpenRouteService
                    route = HttpMapRequest.getORSRouteWaypoints(scenarioToMapPosition(node[i].getLastWaypoint().pos), dst, epsgCode, orsDistMetric, orsUrl);
                    
                    numIterations++;
                }

                // add route to waypoint list (drive to destination)
                t = addRoute(node[i], t, route);
            }
        }

        postGeneration();
    }

    protected boolean parseArg(String key, String value)
    {
        if (key.equals("paramFile"))
        {
            parameterFile = value;
            return true;
        }
        else
            return super.parseArg(key, value);
    }

    protected boolean parseArg(char key, String value)
    {
        switch (key)
        {
            case 'p': // "Parameter file"
                parameterFile = value;
                return true;
            case 'u': // ORS URL
                orsUrl = value;
                return true;
            case 'w': // write node movement to file
                logFile = value;
                return true;
            default:
                return super.parseArg(key, value);
        }
    }

    public void write(String _name) throws FileNotFoundException, IOException
    {
        String[] p = new String[1];
        p[0] = "paramFile=" + parameterFile;
        super.write(_name, p);
    }

    public static void printHelp()
    {
        Scenario.printHelp();
        System.out.println( MODEL_NAME + ":");
        System.out.println("\t-p <parameter file>");
    }

    protected void preGeneration()
    {
        super.preGeneration();

        parseParameterFile(parameterFile);

        if (minSpeed > maxSpeed || minSpeed <= 0 || maxSpeed <= 0)
        {
            System.out.println("Error: There is something wrong with the Speed values!");
            System.exit(0);
        }

        x = routeBBox.width();
        y = routeBBox.height();
        mapBBox.setProjection(proj);
        routeBBox.setProjection(proj);
    }

    protected void postGeneration()
    {
        for (int i = 0; i < node.length; i++)
        {
            // remove waypoints exceeding duration
            Waypoint l = node[i].getLastWaypoint();
            if (l.time < duration)
                node[i].add(duration, l.pos);
            else
            {
                while (l.time > duration)
                {
                    node[i].removeLastElement();
                    l = node[i].getLastWaypoint();
                    if (l.time < duration)
                        node[i].add(duration, node[i].positionAt(duration));
                }
            }
        }

        double failedORSRequests = HttpMapRequest.countFailedORSRequests / (double)HttpMapRequest.countORSRequests;
        DecimalFormat df = new DecimalFormat("###.##%");
        System.out.println("\n#OSM queries = " + HttpMapRequest.countOSMQueries + " | #ORS requests = " + HttpMapRequest.countORSRequests + " (" + df.format(failedORSRequests) + " failed)");

        if (meshNodeDistance > 0)
            addMeshNodes();
        if (logFile != null)
            mywrite();

        super.postGeneration();
    }

    private void parseParameterFile(String fileName)
    {
        try
        {
            FileReader fr = new FileReader(fileName);
            BufferedReader fileIn = new BufferedReader(fr);

            // parse parameters
            String line = null;
            while ((line = fileIn.readLine()) != null)
            {
                if (line.startsWith("#"))
                    continue;

                StringTokenizer st = new StringTokenizer(line, "=");
                String key = st.nextToken();
                String value = st.nextToken();

                if (DEBUG) System.out.println("DEBUG: key = \"" + key + "\", value = \"" + value + "\"");

                if (key.equals("MapBBox"))
                {
                    String[] v = value.split(" ");
                    double left = Double.parseDouble(v[0]);
                    double bottom = Double.parseDouble(v[1]);
                    double right = Double.parseDouble(v[2]);
                    double top = Double.parseDouble(v[3]);
                    double margin = Double.parseDouble(v[4]);

                    if (DEBUG) System.out.println("DEBUG: grid = (" + left + ", " + bottom + ", " + right + ", " + top + "), margin = " + margin);

                    if (margin < 0)
                    {
                        System.out.println("Error: margin must be >= 0!");
                        System.exit(0);
                    }
                    if (margin > 1)
                        System.out.println("Warning: margin is > 1, are you sure you want to add a margin with size > 100% of the original bounding box?");

                    mapBBox = new BoundingBox(left, bottom, right, top);

                    double marginX = mapBBox.width()*margin*0.5;
                    double marginY = mapBBox.height()*margin*0.5;
                    routeBBox = new BoundingBox(left - marginX, bottom - marginY, right + marginX, top + marginY);

                    if (DEBUG) System.out.println("DEBUG: routeBBox = (" + routeBBox.origin().x + ", " + routeBBox.origin().y + ", " + (routeBBox.origin().x + routeBBox.width()) + ", " + (routeBBox.origin().y + routeBBox.height()) + ")");
                }
                else if (key.equals("ORSDistanceMetric"))
                {
                    orsDistMetric = value;
                    if (!orsDistMetric.equals("Fastest") && !orsDistMetric.equals("Shortest"))
                    {
                        System.out.println("Warning: ORSDistanceMetric must be either \"Fastest\" or \"Shortest\"... setting to default: \"Fastest\"");
                        orsDistMetric = "Fastest";
                    }
                }
                else if (key.equals("EPSGCode"))
                {
                    epsgCode = Integer.parseInt(value);
                    proj = ProjectionFactory.getNamedPROJ4CoordinateSystem("epsg:"+value);
                    if (DEBUG) System.out.println("DEBUG: Proj4 string = \"" + proj.getPROJ4Description() + "\"");
                }
                else if (key.equals("Speed"))
                {
                    String[] v = value.split(" ");
                    minSpeed = Double.parseDouble(v[0]);
                    maxSpeed = Double.parseDouble(v[1]);

                    if (DEBUG) System.out.println("DEBUG: minSpeed = " + minSpeed + ", maxSpeed = " + maxSpeed);
                }
                else if (key.equals("MaxPause"))
                {
                    maxPause = Double.parseDouble(value);

                    if (DEBUG) System.out.println("DEBUG: maxPause = " + maxPause);
                }
                else if (key.equals("MeshNodeDistance"))
                {
                    String[] v = value.split(" ");
                    meshNodeDistance = Double.parseDouble(v[0]);

                    if (DEBUG) System.out.println("DEBUG: meshNodeDistance = " + meshNodeDistance);
                }
                else if (key.equals("MaxORSRequestIterations"))
                {
                    String[] v = value.split(" ");
                    maxORSRequestIterations = Integer.parseInt(v[0]);

                    if (DEBUG) System.out.println("DEBUG: maxORSRequestIterations = " + maxORSRequestIterations);
                }
                else
                    continue;
            }

            fileIn.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    // write nodes' movement to file
    private void mywrite()
    {
        try
        {
            FileWriter fileOut = new FileWriter(logFile);
            for (int i = 0; i < node.length; i++)
            {
                fileOut.write("# node " + i + " movement\n");
                for (int j = 0; j < node[i].getNumWaypoints(); j++)
                    fileOut.write(node[i].getWaypoint(j).time + " " + node[i].getWaypoint(j).pos.x + " " + node[i].getWaypoint(j).pos.y + "\n");
                fileOut.write("\n");
            }
            fileOut.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private double addRoute(MobileNode node,
                            double t,
                            Position[] route)
    {
        Position src = scenarioToMapPosition(node.getLastWaypoint().pos);
        Position dst = null;
        for (int i = 0; i < route.length; i++)
        {
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

    private void addWaypoint(MobileNode node,
                             double t,
                             Position mapPosition)
    {
        if (!routeBBox.contains(mapPosition))
        {
            System.out.println("Warning: Position "+ mapPosition.toString() + " lies outside the route bounding box!");
            System.exit(42);
        }

        Position scenarioPosition = mapToScenarioPosition(mapPosition);

        Position old = null;
        double dist = -1.0;
        if (node.getNumWaypoints() > 0)
        {
            old = scenarioToMapPosition(node.getLastWaypoint().pos);
            dist = mapPosition.distance(old);
            // dirty hack: even if the map positions are equal, the corresponding scenario positions might differ
            if (node.getLastWaypoint().time == t && (dist == 0.0 || old.toString().equals(mapPosition.toString())))
                return;
        }
        if (!node.add(t, scenarioPosition))
        {
            System.out.println("Error: Adding waypoint "+ mapPosition.toString() + " failed (there was already a waypoint " + old.toString() + " at time " + t +"; dist = " + dist + ")!");
            System.exit(0);
        }
    }

    private Position mapToScenarioPosition(Position mapPosition)
    {
        Position origin = routeBBox.origin();
        return new Position(mapPosition.x - origin.x, mapPosition.y - origin.y);
    }

    private Position scenarioToMapPosition(Position scenarioPosition)
    {
        Position origin = routeBBox.origin();
        return new Position(origin.x + scenarioPosition.x, origin.y + scenarioPosition.y);
    }

    private Position randomNextMapPosition()
    {
        Position result = null;
        double rx = mapBBox.origin().x + randomNextDouble() * mapBBox.width();
        double ry = mapBBox.origin().y + randomNextDouble() * mapBBox.height();

        int numIterations = 0;
        while (result == null)
        {
            // prevent infinite while loop
            if (numIterations > maxORSRequestIterations)
            {
                System.out.println("Error: no valid random position could be generated after " + maxORSRequestIterations + " tries!");
                System.exit(-1);
            }
            
            result = new Position(rx, ry);
            if (!HttpMapRequest.isValidForORS(result, epsgCode, orsDistMetric, orsUrl))
            {
                result = null;

                rx = mapBBox.origin().x + randomNextDouble() * mapBBox.width();
                ry = mapBBox.origin().y + randomNextDouble() * mapBBox.height();
            }
            numIterations++;
        }

        return result;
    }

    /**
     *
     * @pre meshNodeDistance > 0
     */
    private void addMeshNodes()
    {
        int numMeshX = (int)Math.floor(x / meshNodeDistance);
        int numMeshY = (int)Math.floor(y / meshNodeDistance);
        int numMeshNodes = numMeshX * numMeshY;

        System.out.println("Adding a grid of " + numMeshNodes + " static mesh nodes...");

        MobileNode[] nodeNew = new MobileNode[node.length + numMeshNodes];
        for (int i = 0; i < node.length; i++)
            nodeNew[i] = node[i];
        for (int i = 0; i < numMeshNodes; i++)
            nodeNew[node.length + i] = new MobileNode();

        for (int j = 0; j < numMeshY; j++)
        {
            for (int i = 0; i < numMeshX; i++)
                nodeNew[node.length + j*numMeshX + i].add(0.0, new Position((i+1) * meshNodeDistance, (j+1) * meshNodeDistance));
        }

        node = nodeNew;
    }
}
