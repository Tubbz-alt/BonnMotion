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

package edu.bonn.cs.iv.bonnmotion.models;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

import com.perspectalabs.bonnmotion.util.HeightMap;

// ACS begin
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
// ACS end

import edu.bonn.cs.iv.bonnmotion.GroupNode;
import edu.bonn.cs.iv.bonnmotion.MobileNode;
import edu.bonn.cs.iv.bonnmotion.ModuleInfo;
import edu.bonn.cs.iv.bonnmotion.Position;
import edu.bonn.cs.iv.bonnmotion.RandomSpeedBase;
import edu.bonn.cs.iv.bonnmotion.printer.Dimension;

/**
 * Application to create movement scenarios according to the Reference Point
 * Group Mobility model.
 */

public class RPGM extends RandomSpeedBase {
    private static ModuleInfo info;

    static {
        info = new ModuleInfo("RPGM");
        info.description = "Application to create movement scenarios according to the Reference Point Group Mobility model";

        info.major = 1;
        info.minor = 1;
        info.revision = ModuleInfo
                .getSVNRevisionStringValue("$LastChangedRevision: 650 $");

        info.contacts.add(ModuleInfo.BM_MAILINGLIST);
        info.authors.add("University of Bonn");
        info.affiliation = ModuleInfo.UNIVERSITY_OF_BONN;
    }

    public static ModuleInfo getInfo() {
        return info;
    }

    // ACS begin
    protected HashMap<Integer, List<Integer>> groupMembershipTable = new HashMap<Integer, List<Integer>>();
    protected int numSubseg = 4;
    protected double speedScale = 1.5;
    protected boolean referencePointIsNode = false;
    protected HeightMap heightMap = null;
    // ACS end

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

    public RPGM(int nodes, double x, double y, double duration, double ignore,
            long randomSeed, double minspeed, double maxspeed, double maxpause,
            double maxdist, double avgMobileNodesPerGroup,
            double groupSizeDeviation, double pGroupChange) {
        super(nodes, x, y, duration, ignore, randomSeed, minspeed, maxspeed,
                maxpause);
        this.maxdist = maxdist;
        this.avgMobileNodesPerGroup = avgMobileNodesPerGroup;
        this.groupSizeDeviation = groupSizeDeviation;
        this.pGroupChange = pGroupChange;
        generate();
    }

    public RPGM(String[] args) {
        go(args);
    }

    public void go(String args[]) {
        super.go(args);
        generate();
    }

    // ACS begin
    public void generate() {
        if (groupMembershipTable.isEmpty()) {
            generateForRandomlyDefinedGroups();
        } else {
            generateForExplicitlyDefinedGroups();
        }
    }

    private Position updateHeight(Position position) {
        Position retval = position;
        
        if (heightMap != null) {
            retval.z = heightMap.getHeight(retval);
        }
        
        return retval;
    }
    
    private Position newPosition(double x, double y) {
        return updateHeight(new Position(x, y));
    }
    
    private Position rndprox(Position position, double maxdist, double dist, double dir, Dimension dim)
    {
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
                    + ".generate: error while adding reference node movement (1)");
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
                        + ".generate: error while adding reference node movement (2)");
                System.exit(-1);
            }

            if ((t < parameterData.duration) && (maxpause > 0.0)) {
                double pause = maxpause * randomNextDouble();
                if (pause > 0.0) {
                    t += pause;

                    if (!retval.add(t, dst)) {
                        System.err.println(getInfo().name
                                + ".generate: error while adding reference node movement (3)");
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
     * be given a position of 0.0, 0.0
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

                retval[i].add(0.0, new Position(0.0, 0.0, 0.0));
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

            double prevX = grpSegStart.x
                    + (segm - 1) * (grpSegEnd.x - grpSegStart.x) / numSubseg;
            double prevY = grpSegStart.y
                    + (segm - 1) * (grpSegEnd.y - grpSegStart.y) / numSubseg;

            Position grpSegInterim = newPosition(interimX, interimY);
            Position grpSegPrev = newPosition(prevX, prevY);

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
     * Generate motion for when the groups are explicitly defined.
     * 
     * TODO: Allow first node of each group to be the reference node
     */
    public void generateForExplicitlyDefinedGroups() {
        preGeneration();

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

        this.parameterData.nodes = node;

        postGeneration();
    } // end of generateForExplicitlyDefinedGroups method

    // ACS end

    public void generateForRandomlyDefinedGroups() {
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
    private boolean readGroupMembership(String groupMembershipFileName) {

        boolean retval = false;

        groupMembershipTable.clear();

        try {
            LineNumberReader reader = new LineNumberReader(
                    new FileReader(groupMembershipFileName));

            for (String line = reader.readLine(); line != null; line = reader
                    .readLine()) {

                String[] splitLine = line.split(" ");

                List<Integer> members = new ArrayList<Integer>(splitLine.length);

                for (int i = 0; i < splitLine.length; i++) {
                    members.add(Integer.parseInt(splitLine[i]));
                }
                
                int groupId = reader.getLineNumber();
                
                if (referencePointIsNode) {
                    groupId = members.remove(0);
                }
                
                groupMembershipTable.put(groupId, members);
            }

            retval = true;

            reader.close();

        } catch (java.io.FileNotFoundException fnfe) {
            System.err.println(
                    "Unable to read group membership: " + fnfe.getMessage());
        } catch (java.io.IOException ioe) {
            System.err.println(
                    "Unable to read group membership: " + ioe.getMessage());
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
        String[] p = new String[] {
                "groupsize_E=" + avgMobileNodesPerGroup,
                "groupsize_S=" + groupSizeDeviation,
                "pGroupChange=" + pGroupChange,
                "maxdist=" + maxdist,
                "numSubseg=" + numSubseg,
                "speedScale=" + speedScale,
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
            return readGroupMembership(val);
        case 'm':
            speedScale = Double.parseDouble(val);
            return true;
        case 'N':
            referencePointIsNode = true;
            return true;
        case 't':
            heightMap = new HeightMap(val);
            parameterData.x = heightMap.getX();
            parameterData.y = heightMap.getY();
            return true;
        case 'x':
        case 'y':
            if (heightMap == null) {
                return super.parseArg(key, val);
            } else {
                return true;
            }
// ACS end
        case 'r':
            maxdist = Double.parseDouble(val);
            return true;
        case 's':
            groupSizeDeviation = Double.parseDouble(val);
            return true;
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
        System.out.println(
                "\t-N Reference point is itself a node.  Requires -g");
        System.out.println("\t-t <terrain model file>");
        // ACS end
        System.out.println("\t-r <max. distance to group center>");
        System.out.println("\t-s <group size standard deviation>");
    }
}
