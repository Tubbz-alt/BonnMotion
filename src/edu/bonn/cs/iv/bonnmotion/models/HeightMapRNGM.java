/*******************************************************************************
 ** BonnMotion - a mobility scenario generation and analysis tool             **
 ** Copyright (C) 2002-2012 University of Bonn                                **
 ** Copyright (C) 2012-2016 University of Osnabrueck                          **
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

/**
 * Height Map Reference Node Group Mobility
 *
 * Based on RPGM.  Modified to:
 *
 * <ul>
 * <li>add support for using a height map to set the height of nodes</li>
 * <li>to set the reference point of a group to be a node rather than an abstract point</li>
 * <li>to define groups statically</li>
 * </ul>
 *
 * Modifications from RPGM are marked between
 * // ACS begin
 * and
 * // ACS end
 *
 *  @author Yitzchak M. Gottlieb <ygottlieb@perspectalabs.com>
 */

package edu.bonn.cs.iv.bonnmotion.models;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import com.perspectalabs.bonnmotion.util.HeightMap;
import com.perspectalabs.bonnmotion.util.PositionGeoParser;

import java.io.FileReader;
import java.io.LineNumberReader;

import edu.bonn.cs.iv.bonnmotion.*;
import edu.bonn.cs.iv.bonnmotion.printer.Dimension;
import edu.bonn.cs.iv.bonnmotion.StationaryNode;
import edu.bonn.cs.iv.util.maps.PositionGeo;

/**
 * Application to create movement scenarios according to the Reference Point
 * Group Mobility model.
 */

public class HeightMapRNGM extends RandomSpeedBase {
    private static ModuleInfo info;

    static {
        info = new ModuleInfo("HeightMapRNGM");
        info.description = "Application to create movement scenarios according to the Reference Point Group Mobility model with support for static groups and 3D motion";

        info.major = 1;
        info.minor = 0;
        info.revision = 0;

        info.contacts.add("Alex Poylisher <apoylisher@perspectalabs.com>");
        info.contacts.add("Yitzchak M. Gottlieb <ygottlieb@perspectalabs.com>");
        info.authors.add("Perspecta Labs Inc.");
        info.affiliation = "Perspecta Labs Inc. <https://www.perspectalabs.com>";
    }

    public static ModuleInfo getInfo() {
        return info;
    }

    protected HashMap<Integer, List<Integer>> groupMembershipTable = new HashMap<Integer, List<Integer>>();
    protected List<StationaryNode> stationaryNodes = new ArrayList<>();
    protected int numSubseg = 4;
    protected double speedScale = 1.5;
    protected boolean referencePointIsNode = false;
    protected HeightMap heightMap = null;
    protected String heightMapPath = null;
    protected PositionGeo referencePositionGeo = null;
    protected String groupMembershipPath = null;

    private static final String MOBILE_HEADER = "[MOBILE]";
    private static final String STATIONARY_HEADER = "[STATIONARY]";
    private static final char CONFIG_COMMENT = '#';


    /** Maximum deviation from group center [m]. */
    protected double maxdist = 2.5;
    /** Average nodes per group. */
    protected double avgMobileNodesPerGroup = 3.0;
    /** Standard deviation of nodes per group. */
    protected double groupSizeDeviation = 2.0;
    /**
     * The probability for a node to change to a new group when moving into it's
     * range.
     */
    protected double pGroupChange = 0.01;

    public HeightMapRNGM(int nodes, double x, double y, double duration,
            double ignore, long randomSeed, double minspeed, double maxspeed,
            double maxpause, double maxdist, double avgMobileNodesPerGroup,
            double groupSizeDeviation, double pGroupChange) {
        super(0, x, y, duration, ignore, randomSeed, minspeed, maxspeed,
                maxpause);

        this.maxdist = maxdist;
        this.avgMobileNodesPerGroup = avgMobileNodesPerGroup;
        this.groupSizeDeviation = groupSizeDeviation;
        this.pGroupChange = pGroupChange;
        generate();
    }

    public HeightMapRNGM(String[] args) {
        go(args);
    }

    public void go(String args[]) {
        super.go(args);
        generate();
    }

    // ACS begin

    /**
     * Generate the mobile and stationary nodes for the given parameters
     */
    public void generate() {

        if (groupMembershipPath != null) {
            if (!readNodeGroups(groupMembershipPath)) {
                System.exit(-1);
            }
        }
        
        if (heightMapPath != null) {
            heightMap = new HeightMap(heightMapPath, referencePositionGeo);
        }

        if (groupMembershipTable.isEmpty()) {
            generateForRandomlyDefinedGroups();
        } else {
            generateForExplicitlyDefinedNodes();
        }
    }

    /**
     * Update the height of the position from the heightMap.
     * 
     * @param position
     *            The position to update
     * @return The position with updated height (not a new position)
     */
    private Position updateHeight(Position position) {
        Position retval = position;

        if (heightMap != null) {
            retval.z = heightMap.getHeight(retval);
        }

        return retval;
    }

    /**
     * Create a new position with the correct height
     * 
     * @param x
     *            The X-coordinate of the position
     * @param y
     *            The Y-coordinate of the position
     * @return A new position at (x, y) with updated height
     */
    private Position newPosition(double x, double y) {
        return updateHeight(new Position(x, y));
    }

    /**
     * Call {@link Position#rndprox} on the position and update the height on
     * the returned value.
     */
    private Position rndprox(Position position, double maxdist, double dist,
            double dir, Dimension dim) {
        return updateHeight(position.rndprox(maxdist, dist, dir, dim));
    }

    /**
     * Generate motion for a reference node using the random waypoint model
     *
     * @return The reference node.
     */
    private GroupNode generateForReferenceNode() {
        GroupNode retval = new GroupNode(null);
        retval.setgroup(retval);

        double t = 0.0;

        // pick position inside the interval
        // [maxdist; x - maxdist], [maxdist; y - maxdist]
        // (to ensure that the group area doesn't overflow the borders)
        Position src = newPosition(
                (parameterData.x - 2 * maxdist) * randomNextDouble() + maxdist,
                (parameterData.y - 2 * maxdist) * randomNextDouble() + maxdist);

        if (!retval.add(0.0, src)) {
            System.err.println(getInfo().name
                    + ".generate: error while adding reference node "
                    + "movement (1)");
            System.exit(-1);
        }

        while (t < parameterData.duration) {
            Position dst = newPosition(
                    (parameterData.x - 2 * maxdist) * randomNextDouble()
                            + maxdist,
                    (parameterData.y - 2 * maxdist) * randomNextDouble()
                            + maxdist);

            double speed = (maxspeed - minspeed) * randomNextDouble()
                    + minspeed;

            t += src.distance(dst) / speed;

            if (!retval.add(t, dst)) {
                System.err.println(getInfo().name
                        + ".generate: error while adding reference node "
                        + "movement (2)");
                System.exit(-1);
            }

            if ((t < parameterData.duration) && (maxpause > 0.0)) {
                double pause = maxpause * randomNextDouble();
                if (pause > 0.0) {
                    t += pause;

                    if (!retval.add(t, dst)) {
                        System.err.println(getInfo().name
                                + ".generate: error while adding reference "
                                + "node movement (3)");
                        System.exit(-1);
                    }
                }
            }
            src = dst;
        }
        return retval;
    }

    /**
     * Allocate the array of MobileNodes that will be generated in the end.
     * Since the node groups file may contain node IDs that are not sequential
     * from 0 to the number of nodes, this function may create an array that
     * contains nodes for which group motion is not defined. Those nodes should
     * be given a position of the referencePoint
     *
     * @return An array of GroupNodes in which to fill with group nodes.
     */
    private GroupNode[] allocateNodes() {

        Set<Integer> allNodeIds = new HashSet<Integer>();

        for (List<Integer> nodeIds : groupMembershipTable.values()) {
            allNodeIds.addAll(nodeIds);
        }

        int maxNodeId = Collections.max(allNodeIds);

        GroupNode[] retval = new GroupNode[maxNodeId + 1];

        for (int i = 0; i < retval.length; ++i) {
            if (!allNodeIds.contains(i)) {
                retval[i] = new GroupNode(null);

                retval[i].add(0.0, newPosition(0.0, 0.0));
            }
        }

        return retval;
    }

    /**
     * Determine whether the group member should pause based on the movement of
     * the reference point.
     *
     * @param node
     *            The GroupNode for which to ask the question
     * @param timeindex
     *            The index into GropuNode#changeTimes for which to determine
     *            whether to pause
     * @return true if the node should pause at this time, false otherwise
     */
    private boolean groupMemberShouldPause(GroupNode node, int timeindex) {
        boolean pause = (timeindex == 0);

        MobileNode group = node.group();
        final double[] groupChangeTimes = group.changeTimes();

        if (!pause) {
            final Position pos1 = group
                    .positionAt(groupChangeTimes[timeindex - 1]);
            final Position pos2 = group.positionAt(groupChangeTimes[timeindex]);
            pause = pos1.equals(pos2);
        }

        return pause;
    }

    /**
     * Generate interim movement for a member of a group. Generate numSubseg
     * motions for the node between the motions of the group reference point at
     * the given timeindex.
     *
     * @param node
     *            The node for which to generate movement
     * @param timeindex
     *            The index into the node's group's table of changeTimes
     * @param msrc
     *            The starting position of the interim movement
     * @return The final position of the node.
     */
    private Position generateInterimForGroupMember(GroupNode node,
            int timeindex, Position msrc) {
        Position mdst = null;
        Position prevLoc = msrc;

        MobileNode group = node.group();
        final double[] groupChangeTimes = group.changeTimes();

        Position grpSegStart = group
                .positionAt(groupChangeTimes[timeindex - 1]);

        Position grpSegEnd = group.positionAt(groupChangeTimes[timeindex]);

        double segTimeDelta = (groupChangeTimes[timeindex]
                - groupChangeTimes[timeindex - 1]) / numSubseg;

        for (int segm = 1; segm <= numSubseg; ++segm) {

            double interimX = grpSegStart.x
                    + segm * (grpSegEnd.x - grpSegStart.x) / numSubseg;
            double interimY = grpSegStart.y
                    + segm * (grpSegEnd.y - grpSegStart.y) / numSubseg;

            Position grpSegInterim = newPosition(interimX, interimY);

            double interimTime = groupChangeTimes[timeindex - 1]
                    + segm * segTimeDelta;

            double speed = 0;

            do {
                mdst = rndprox(grpSegInterim, maxdist, randomNextDouble(),
                        randomNextDouble(), parameterData.calculationDim);
                speed = prevLoc.distance(mdst) / segTimeDelta;
            } while (speed > maxspeed * speedScale);

            prevLoc = mdst;

            if (!node.add(interimTime, mdst)) {
                System.err.println(getInfo().name
                        + ".generate: error while adding node movement for "
                        + "intermediate dest");
                System.exit(-1);
            }
        }

        return mdst;
    }

    /**
     * Generate the motion for the GroupNode. The node will have interim motion
     * between the waypoints of the reference point.
     *
     * @param node
     *            The node for which to generate movements.
     */
    private void generateForGroupMember(GroupNode node) {
        double mt = 0.0;

        MobileNode group = node.group();

        Position msrc = rndprox(group.positionAt(mt), maxdist,
                randomNextDouble(), randomNextDouble(),
                parameterData.calculationDim);

        // System.out.println("src: " + msrc.toString());

        if (!node.add(0.0, msrc)) {
            System.err.println(getInfo().name
                    + ".generate: error while adding node movement (1)");
            System.exit(-1);
        }

        while (mt < parameterData.duration) {
            Position mdst = newPosition(0.0, 0.0);
            final double[] groupChangeTimes = group.changeTimes();
            int currentGroupChangeTimeIndex = 0;

            // Determine the current time index for which the member
            // node should move
            while ((currentGroupChangeTimeIndex < groupChangeTimes.length)
                    && (groupChangeTimes[currentGroupChangeTimeIndex] <= mt))
                currentGroupChangeTimeIndex++;

            double next = (currentGroupChangeTimeIndex < groupChangeTimes.length)
                    ? groupChangeTimes[currentGroupChangeTimeIndex]
                    : parameterData.duration;

            boolean pause = groupMemberShouldPause(node,
                    currentGroupChangeTimeIndex);

            // don't do any movement within a pause
            if (pause) {
                mdst = msrc;
            } else {
                mdst = generateInterimForGroupMember(node,
                        currentGroupChangeTimeIndex, msrc);
            }

            msrc = mdst;
            mt = next;
        } // end of while mt < parameterData.duration
    }

    /**
     * For each node, add waypoints between the node's waypoints to account for
     * correct changes of height above datum.
     * 
     * @param nodes
     *            The nodes whose way points to process
     * @return The array of nodes with intercalated points.
     */
    private MobileNode[] intercalateHeightWayPoints(MobileNode[] nodes) {
        MobileNode[] retval = nodes;

        if (heightMap != null) {
            retval = new MobileNode[nodes.length];

            for (int i = 0; i < nodes.length; ++i) {
                // Create a new node
                MobileNode newnode = new MobileNode();
                retval[i] = newnode;

                // Process each way point
                Waypoint prevWaypoint = null;
                for (Waypoint waypoint : nodes[i].getWaypoints()) {
                    if (prevWaypoint == null
                            || prevWaypoint.pos.equals(waypoint.pos)) {
                        // If there was no movement, copy the point
                        newnode.add(waypoint.time, waypoint.pos);
                    } else {
                        // If there way movement, get the path
                        List<Position> path = heightMap
                                .getPath(prevWaypoint.pos, waypoint.pos);
                        double distance = heightMap.getLength(path);
                        path.remove(0); // The first point on the path is the
                                        // previous way point, so remove it.

                        // Use the average speed
                        double speed = distance
                                / (waypoint.time - prevWaypoint.time);

                        double t = prevWaypoint.time;
                        Position prevPosition = prevWaypoint.pos;

                        for (Position position : path) {
                            t += prevPosition.distance(position) / speed;
                            newnode.add(t, position);
                            prevPosition = position;
                        }
                    }
                    prevWaypoint = waypoint;
                }
            }
        }

        return retval;
    }

    /**
     * Generate for mobile and stationary nodes that
     * are defined
     */
    private void generateForExplicitlyDefinedNodes() {
        preGeneration();
        generateGroupNodes();
        generateStationaryNodes();
        postGeneration();
    } // end of generateForExplicitlyDefinedGroups method

    /**
     * Generates for the group nodes that are defined.
     * For each group make the first node a leader,
     * and all the others a member of the group they
     * belong to.
     */
    private void generateGroupNodes() {
        final GroupNode[] node = allocateNodes();

        for (Map.Entry<Integer, List<Integer>> groupentry : groupMembershipTable
                .entrySet()) {
            int groupId = groupentry.getKey();
            List<Integer> members = groupentry.getValue();

            GroupNode ref = generateForReferenceNode();

            if (referencePointIsNode) {
                node[groupId] = ref;
            }

            for (int memberId : members) {
                GroupNode memberNode = new GroupNode(ref);

                generateForGroupMember(memberNode);

                node[memberId] = memberNode;

            } // end of iteration through nodes of a group

        } // end of iteration through group leaders

        this.parameterData.nodes = intercalateHeightWayPoints(node);

    }

    /**
     * Generates the stationary nodes that are defined.
     * For each stationary node convert it to a mobile node.
     * Convert the Latitude and Longitude to offset points.
     * The height of the node is taken from the Height Map,
     * and the optional altitude is added on if it defined.
     * Remove any nodes that previously had the same id, and
     * insert the new node into its correct spot in the node list
     */
    private void generateStationaryNodes() {
        ArrayList<MobileNode> allNodes = new ArrayList<>(Arrays.asList(this.parameterData.nodes));

        for(StationaryNode node : this.stationaryNodes) {
            MobileNode newNode = new MobileNode();
            PositionGeo geoPosition = node.getPosition();
            Position position = heightMap.transformFromWgs84ToPosition(geoPosition);
            Position rasterPosition = heightMap.transformFromWgs84ToRaster(geoPosition);

            position.z = this.heightMap.getHeight(rasterPosition);
            if(node.getAltitude() != null) {
                position.z += node.getAltitude();
            }
            newNode.add(0, position);
            if(node.getId() < allNodes.size()) {
                allNodes.remove(node.getId());
            }
            allNodes.add(node.getId(), newNode);
        }
        this.parameterData.nodes = Arrays.copyOf(allNodes.toArray(), allNodes.size(), MobileNode[].class);
    }

    // ACS end

    private void generateForRandomlyDefinedGroups() {
        preGeneration();

        final GroupNode[] node = new GroupNode[this.parameterData.nodes.length];
        final Vector<MobileNode> rpoints = new Vector<MobileNode>();

        // groups move in a random waypoint manner:
        int nodesRemaining = node.length;
        int offset = 0;

        while (nodesRemaining > 0) {
            MobileNode ref = new MobileNode();
            rpoints.addElement(ref);
            double t = 0.0;

            // pick position inside the interval [maxdist; x - maxdist],
            // [maxdist; y - maxdist]
            // (to ensure that the group area doesn't overflow the borders)
            Position src = newPosition(
                    (parameterData.x - 2 * maxdist) * randomNextDouble()
                            + maxdist,
                    (parameterData.y - 2 * maxdist) * randomNextDouble()
                            + maxdist);

            if (!ref.add(0.0, src)) {
                System.err.println(getInfo().name
                        + ".generate: error while adding group movement (1)");
                System.exit(-1);
            }

            while (t < parameterData.duration) {
                Position dst = newPosition(
                        (parameterData.x - 2 * maxdist) * randomNextDouble()
                                + maxdist,
                        (parameterData.y - 2 * maxdist) * randomNextDouble()
                                + maxdist);

                double speed = (maxspeed - minspeed) * randomNextDouble()
                        + minspeed;
                t += src.distance(dst) / speed;

                if (!ref.add(t, dst)) {
                    System.err.println(getInfo().name
                            + ".generate: error while adding group movement (2)");
                    System.exit(-1);
                }

                if ((t < parameterData.duration) && (maxpause > 0.0)) {
                    double pause = maxpause * randomNextDouble();
                    if (pause > 0.0) {
                        t += pause;

                        if (!ref.add(t, dst)) {
                            System.err.println(getInfo().name
                                    + ".generate: error while adding group movement (3)");
                            System.exit(-1);
                        }
                    }
                }
                src = dst;
            }

            int size; // define group size
            while ((size = (int) Math
                    .round(randomNextGaussian() * groupSizeDeviation
                            + avgMobileNodesPerGroup)) < 1)
                ;

            if (size > nodesRemaining) {
                size = nodesRemaining;
            }

            nodesRemaining -= size;
            offset += size;

            for (int i = offset - size; i < offset; i++) {
                node[i] = new GroupNode(ref);
            }
        }

        // nodes follow their reference points:
        for (int i = 0; i < node.length; i++) {
            double t = 0.0;
            MobileNode group = node[i].group();
            Position src = rndprox(group.positionAt(t), maxdist,
                    randomNextDouble(), randomNextDouble(),
                    parameterData.calculationDim);

            if (!node[i].add(0.0, src)) {
                System.err.println(getInfo().name
                        + ".generate: error while adding node movement (1)");
                System.exit(-1);
            }

            while (t < parameterData.duration) {
                Position dst;
                double speed;
                final double[] groupChangeTimes = group.changeTimes();
                int currentGroupChangeTimeIndex = 0;

                while ((currentGroupChangeTimeIndex < groupChangeTimes.length)
                        && (groupChangeTimes[currentGroupChangeTimeIndex] <= t))
                    currentGroupChangeTimeIndex++;

                double next = (currentGroupChangeTimeIndex < groupChangeTimes.length)
                        ? groupChangeTimes[currentGroupChangeTimeIndex]
                        : parameterData.duration;
                boolean pause = (currentGroupChangeTimeIndex == 0);

                if (!pause) {
                    final Position pos1 = group.positionAt(
                            groupChangeTimes[currentGroupChangeTimeIndex - 1]);
                    final Position pos2 = group.positionAt(
                            groupChangeTimes[currentGroupChangeTimeIndex]);
                    pause = pos1.equals(pos2);
                }

                if (!pause) {
                    do {
                        dst = rndprox(group.positionAt(next), maxdist,
                                randomNextDouble(), randomNextDouble(),
                                parameterData.calculationDim);
                        speed = src.distance(dst) / (next - t);
                    } while (speed > maxspeed || speed < minspeed);
                } else {
                    dst = src;
                }

                if (pGroupChange > 0.0) {
                    // create dummy with current src and dst for easier
                    // parameter passing
                    final MobileNode dummy = new MobileNode();

                    if (!dummy.add(t, src)) {
                        System.err.println(getInfo().name
                                + ".generate: error while adding node movement (2)");
                        System.exit(-1);
                    }

                    if (!dummy.add(next, dst)) {
                        System.err.println(getInfo().name
                                + ".generate: error while adding node movement (3)");
                        System.exit(-1);
                    }

                    // group to change to, null if group is not changed
                    MobileNode nRef = null;
                    // time when the link between ref and dummy gets up
                    double linkUp = parameterData.duration;
                    // time when the link between ref and dummy gets down
                    double linkDown = 0.0;
                    // time when the group is changed
                    double nNext = 0;

                    // check all reference points if currently a groupchange
                    // should happen
                    for (MobileNode ref : rpoints) {
                        if (ref != group) {
                            final double[] ct = MobileNode.pairStatistics(dummy,
                                    ref, t, next, maxdist, false,
                                    parameterData.calculationDim);
                            // check if the link comes up before any other link
                            // to a ref by now
                            if (ct.length > 6 && ct[6] < linkUp) {
                                if (randomNextDouble() < pGroupChange) {
                                    linkUp = ct[6];
                                    linkDown = (ct.length > 7) ? ct[7] : next;

                                    // change group at time tmpnext
                                    final double tmpnext = linkUp
                                            + randomNextDouble()
                                                    * (linkDown - linkUp);

                                    // check if group change is possible at this
                                    // time
                                    if (this.groupChangePossible(tmpnext, ref,
                                            dummy)) {
                                        nNext = tmpnext;
                                        nRef = ref;
                                    }
                                }
                            }
                        }
                    }

                    if (nRef != null) {
                        // change group to nRef at time nNext
                        group = nRef;
                        next = nNext;
                        dst = dummy.positionAt(next);
                        node[i].setgroup(nRef);
                    }
                }

                if (!node[i].add(next, dst)) {
                    System.err.println(getInfo().name
                            + ".generate: error while adding node movement (4)");
                    System.exit(-1);
                }

                src = dst;
                t = next;
            }
        }

        // write the nodes into our base
        this.parameterData.nodes = node;

        postGeneration();
    }

    /**
     * Checks if the groupchange into the given group is currently possible for
     * the given point depending on the calculation of speed and next position
     * of the group.
     *
     * @param time
     *            current time
     * @param group
     *            group node
     * @param node
     *            the node that should change its group
     * @return true if groupchange is currently possible, false otherwise
     */
    private boolean groupChangePossible(final Double time,
            final MobileNode group, final MobileNode node) {
        /*
         * idea: build a line through the given points, walk maxdist - threshold
         * in the other direction and check if this position can be reached by
         * maxspeed
         */
        final double threshold = 0.1;

        final Position refPos = group.positionAt(time);
        final Position nodePos = node.positionAt(time);
        final double scaledDistanceToWalk = (maxdist - threshold)
                / refPos.distance(nodePos);

        // get position of the point with max distance
        final Position src = newPosition(
                refPos.x - scaledDistanceToWalk * nodePos.x,
                refPos.y - scaledDistanceToWalk * nodePos.y);

        // get time of next position of group
        final double[] groupChangeTimes = group.changeTimes();
        int currentGroupChangeTimeIndex = 0;

        while ((currentGroupChangeTimeIndex < groupChangeTimes.length)
                && (groupChangeTimes[currentGroupChangeTimeIndex] <= time)) {
            currentGroupChangeTimeIndex++;
        }

        if (currentGroupChangeTimeIndex >= groupChangeTimes.length) {
            return false;
        }

        // check for pause, there speed is calculated differently and may be >
        // maxspeed
        boolean pause = (currentGroupChangeTimeIndex == 0);

        if (!pause) {
            Position pos1 = group.positionAt(
                    groupChangeTimes[currentGroupChangeTimeIndex - 1]);
            Position pos2 = group
                    .positionAt(groupChangeTimes[currentGroupChangeTimeIndex]);
            pause = pos1.equals(pos2);
        } else {
            return true;
        }

        final double next = (currentGroupChangeTimeIndex < groupChangeTimes.length)
                ? groupChangeTimes[currentGroupChangeTimeIndex]
                : parameterData.duration;
        final double speed = src.distance(nodePos) / (next - time);

        // check if the calculated needed speed is <= maxspeed
        return (speed <= maxspeed);
    }

    // ACS begin

    /**
     * Clears the groupMembershipTable and adds all the groups
     * from the configuration file
     *
     * @param groups
     *              Group membership lines that were read from the configuration file
     * @return true if reading was successful
     */
    private boolean readGroupMembership(List<String> groups) {
        boolean retval = true;

        groupMembershipTable.clear();
        try {
            for (int lineNumber = 0; lineNumber < groups.size(); lineNumber++) {
                String line = groups.get(lineNumber);
                String[] splitLine = line.split("\\s+");

                List<Integer> members = new ArrayList<Integer>(
                        splitLine.length);

                for (int i = 0; i < splitLine.length; i++) {
                    String member = splitLine[i];

                    if (!member.isEmpty()) {
                        members.add(Integer.parseInt(member));
                    }
                }

                int groupId = lineNumber + 1;

                if (!members.isEmpty()) {
                    if (referencePointIsNode) {
                        groupId = members.remove(0);
                    }

                    groupMembershipTable.put(groupId, members);
                }
            }
        } catch(Exception e) {
            retval = false;
            System.err.println(
                    "Unable to read group membership: " + e.getMessage());
        }

        return retval;
    }

    /**
     * Reads the config file and adds the mobile and stationary
     * nodes to separate groups
     * @param nodeGroupsPath
     *                          File path to the configuration
     * @return true if successful configuration of mobile and stationary nodes
     */
    private boolean readNodeGroups(String nodeGroupsPath) {
        ArrayList<String> mobile = new ArrayList<>();
        ArrayList<String> stationary = new ArrayList<>();
        List<String> pointer = mobile;
        boolean retval = true;

        try {
            LineNumberReader reader = new LineNumberReader(
                    new FileReader(nodeGroupsPath));

            for (String line = reader.readLine(); line != null; line = reader
                    .readLine()) {

                if(line.contains(MOBILE_HEADER)) {
                    pointer = mobile;
                    continue;
                } else if(line.contains(STATIONARY_HEADER)) {
                    pointer = stationary;
                    continue;
                } else if((line.isEmpty())) {
                    continue;
                } else if(line.charAt(0) == CONFIG_COMMENT) {
                    continue;
                }

                pointer.add(line);
            }

            retval &= readGroupMembership(mobile);
            retval &= readStationaryNodes(stationary);
        } catch (java.io.FileNotFoundException fnfe) {
            retval = false;
            System.err.println(
                    "Unable to read group membership: " + fnfe.getMessage());
        } catch (java.io.IOException ioe) {
            retval = false;
            System.err.println(
                    "Unable to read group membership: " + ioe.getMessage());
        }

        return retval;
    }

    /**
     * Clears stationaryNodes and adds all the stationary nodes
     * from the configuration file
     *
     * @param stationary
     *              Stationary node lines that were read from the configuration file
     * @return true if reading was successful
     */
    private boolean readStationaryNodes(List<String> stationary) {

        boolean retval = false;

        this.stationaryNodes.clear();
        try {
            for(String line : stationary) {
                String[] splitLine = line.split("\\s+");
                int id = Integer.parseInt(splitLine[0]);
                double latitude = Double.parseDouble(splitLine[1]);
                double longitude = Double.parseDouble(splitLine[2]);
                PositionGeo position = new PositionGeo(longitude, latitude);

                Double altitude = null;
                if(splitLine.length == 4) {
                    altitude = Double.parseDouble(splitLine[3]);
                }
                this.stationaryNodes.add(StationaryNode.createNode(id, position, altitude));
            }

            this.stationaryNodes.sort(StationaryNode::compareById);
            retval = true;
        } catch(Exception e) {
            System.err.println(
                    "Unable to read stationary nodes: " + e.getMessage());
        }

        return retval;
    }

    // ACS end

    protected boolean parseArg(String key, String value) {
        if (key.equals("groupsize_E")) {
            avgMobileNodesPerGroup = Double.parseDouble(value);
            return true;
        } else if (key.equals("groupsize_S")) {
            groupSizeDeviation = Double.parseDouble(value);
            return true;
        } else if (key.equals("pGroupChange")) {
            pGroupChange = Double.parseDouble(value);
            return true;
        } else if (key.equals("maxdist")) {
            maxdist = Double.parseDouble(value);
            return true;
        }
        // ACS begin
        else if (key.equals("numSubseg")) {
            numSubseg = Integer.parseInt(value);
            return true;
        } else if (key.equals("speedScale")) {
            speedScale = Double.parseDouble(value);
            return true;
        } else if (key.equals("referencePointIsNode")) {
            referencePointIsNode = Boolean.parseBoolean(value);
            return true;
        }
        // ACS end
        else
            return super.parseArg(key, value);
    }

    public void write(String _name) throws FileNotFoundException, IOException {
        // ACS begin
        String[] p = new String[] { "groupsize_E=" + avgMobileNodesPerGroup,
                "groupsize_S=" + groupSizeDeviation,
                "pGroupChange=" + pGroupChange, "maxdist=" + maxdist,
                "numSubseg=" + numSubseg, "speedScale=" + speedScale,
                "referencePointIsNode=" + referencePointIsNode
                // ACS end
        };

        super.write(_name, p);
    }

    protected boolean parseArg(char key, String val) {
        switch (key) {
        case 'a': //
            avgMobileNodesPerGroup = Double.parseDouble(val);
            return true;
        case 'c':
            pGroupChange = Double.parseDouble(val);
            return true;
        // ACS begin
        case 'e':
            numSubseg = Integer.parseInt(val);
            return true;
        case 'g':
            if (parameterData.nodes != null) {
                System.err.println("Cannot specify the number of nodes and "
                        + "their groups simultaneously");
                return false;
            } else {
                groupMembershipPath = val;
                // Will be overwritten in generate()
                parameterData.nodes = new MobileNode[0];
                return true;
            }
        case 'm':
            speedScale = Double.parseDouble(val);
            return true;
        case 'N':
            referencePointIsNode = true;
            return true;
        case 't':
            heightMapPath = val;
            return true;
        case 'o':
            referencePositionGeo = PositionGeoParser.parsePositionGeo(val);
            return true;
        // ACS end
        case 'r':
            maxdist = Double.parseDouble(val);
            return true;
        case 's':
            groupSizeDeviation = Double.parseDouble(val);
            return true;
        case 'n':
            if (groupMembershipPath != null) {
                System.err.println("Cannot specify the number of nodes and "
                        + "their groups simultaneously");
                return false;
            } // fall through
        default:
            return super.parseArg(key, val);
        }
    }

    public static void printHelp() {
        System.out.println(getInfo().toDetailString());
        RandomSpeedBase.printHelp();
        System.out.println(getInfo().name + ":");
        System.out.println("\t-a <average no. of nodes per group>");
        System.out.println("\t-c <group change probability>");
        // ACS begin
        System.out.println("\t-e <number of subsegments in each RP segment>");
        System.out.println("\t-g <group membership file>");
        System.out.println(
                "\t-m <max speed scale for member speed relative to RP speed>");
        System.out
                .println("\t-N Reference point is itself a node.  Requires -g");
        System.out.println("\t-t <terrain model file>");
        // ACS end
        System.out.println("\t-r <max. distance to group center>");
        System.out.println("\t-s <group size standard deviation>");
    }
}
