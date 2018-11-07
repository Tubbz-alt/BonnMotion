package edu.bonn.cs.iv.bonnmotion;

import java.util.Vector;

/** Mobile node. */

public class MobileNode3D extends MobileNode {
    /** Move all waypoints by a certain offset. */
    public void shiftPos(double _x, double _y, double _z) {
        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint oldWP = waypoints.get(i);
            Waypoint newWP = new Waypoint(oldWP.time, ((Position3D)oldWP.pos).newShiftedPosition(_x, _y, _z));
            waypoints.setElementAt(newWP, i);
        }
    }

    @Override
    public void cut(double begin, double end) {
        if (waypoints.size() == 0) {
            return;
        }
        changeSpeedOrDirectionTimes = null;
        Vector<Waypoint> nwp = new Vector<Waypoint>();
        Waypoint w = null;
        double oldstatus = 0.0;
        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint w2 = waypoints.elementAt(i);
            if ((w2.time >= begin) && (w2.time <= end)) {
                w = w2;
                if (nwp.size() == 0) {
                    Position3D hpos = (Position3D)positionAt(begin);
                    Position3D bpos = hpos.clone(oldstatus);
                    nwp.addElement(new Waypoint(0.0, bpos));
                    if (w.time > begin) {
                        nwp.addElement(new Waypoint(w.time - begin, w.pos));
                    }
                }
                else
                    nwp.addElement(new Waypoint(w.time - begin, w.pos));
            }
            if (w2.pos.status == 2.0) {
                oldstatus = 2.0;
            }
            else if (w2.pos.status == 1.0) {
                oldstatus = 0.0;
            }
        }
        if (w == null) { // no waypoints with the given time span
            Waypoint start = new Waypoint(0.0, (Position3D)positionAt(begin));
            Waypoint stop = new Waypoint(end - begin, (Position3D)positionAt(end));
            nwp.addElement(start);
            if (!start.pos.equals(stop.pos))
                nwp.addElement(stop);
        }
        else if (w.time < end) {
            Position3D epos = (Position3D)positionAt(end);
            if (!epos.equals(w.pos))
                nwp.addElement(new Waypoint(end - begin, epos));
        }
        waypoints = nwp;
    }

    /**
     * @param border
     *            The border we add around the scenario to prevent ns-2 from crashing; this value is
     *            added to all x-, y- and z-values.
     */
    @Override
    public String[] movementStringNS(String id, double border) {
        System.out.println("waypoints " + waypoints.size());
        String[] r = new String[waypoints.size() + 2];
        Waypoint w = waypoints.elementAt(0);
        Position3D p = (Position3D)w.pos;
        r[0] = id + " set X_ " + (p.x + border);
        r[1] = id + " set Y_ " + (p.y + border);
        r[2] = id + " set Z_ " + (p.z + border);
        for (int i = 1; i < waypoints.size(); i++) {
            Waypoint w2 = waypoints.elementAt(i);
            Position3D p2 = (Position3D)w2.pos;
            double dist = p.distance(w2.pos);
            r[i + 2] = "$ns_ at " + w.time + " \"" + id + " setdest " + (p2.x + border) + " " + (p2.y + border) + " " + (p2.z + border)
                    + " " + (dist / (w2.time - w.time)) + "\"";
            if (dist == 0.0) {
                r[i + 2] = "# " + r[i + 1];
            }
            // hack alert... but why should we schedule these in ns-2?
            w = w2;
        }
        return r;
    }

    @Override
    public String placementStringGlomo(String id) {
        Waypoint w = waypoints.elementAt(0);
        Position3D p = (Position3D)w.pos;
        return id + " 0S (" + p.x + ", " + p.y + ", " + p.z + ")";
    }

    @Override
    public String[] movementStringGlomo(String id) {
        String[] r = new String[waypoints.size() - 1];
        for (int i = 1; i < waypoints.size(); i++) {
            Waypoint w = waypoints.elementAt(i);
            Position3D p = (Position3D)w.pos;
            r[i - 1] = id + " " + w.time + "S (" + p.x + ", " + p.y + ", " + p.z + ")";
        }
        return r;
    }
    
    
    @Override
    protected Position positionAt_old(double time) {
        Position3D p1 = null;
        double t1 = 0.0;
        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint w = waypoints.elementAt(i);
            if (w.time == time) {
                return w.pos;
            }
            else if (w.time > time) {
                if ((p1 == null) || p1.equals(w.pos))
                    return w.pos;
                else {
                    double weight = (time - t1) / (w.time - t1);
                    
                    return p1.getWeightenedPosition((Position3D)w.pos, weight);
                }
            }
            p1 = (Position3D)w.pos;
            t1 = w.time;
        }
        return p1;
    }

    @Override
    protected Position binarySearch(int i, int j, double time) {
        int median = (i + j) / 2;
        Waypoint w = waypoints.elementAt(median);
        if (i + 1 == j) { // waypoint not found in waypoint list
            Waypoint w_i = waypoints.elementAt(i);
            Waypoint w_j = waypoints.elementAt(j);

            double weight = (time - w_i.time) / (w_j.time - w_i.time);
            
            // if positions of surrounding waypoints are equal => no movement at time
            // just return position
            if (((Position3D)w_i.pos).equals((Position3D)w_j.pos)) {
                return new Position3D(w_i.pos.x, w_i.pos.y, ((Position3D)w_i.pos).z);
            }
            
            return ((Position3D)w_i.pos).getWeightenedPosition((Position3D)w_j.pos, weight);
        }
        else {
            if (time == w.time) { // waypoint found
                return w.pos;
            }
            else if (time < w.time) { // left recursion
                return binarySearch(i, median, time);
            }
            else { // right recursion
                return binarySearch(median, j, time);
            }
        }
    }

    @SuppressWarnings("unused")
    public static boolean sameBuilding(Building3D[] buildings, Position pos1, Position pos2) {
        // XXX: take care that two nodes do not communicate via opposite wall such that the
        // following
        // will not be allowed:
        // __________
        // | |
        // x | x
        // |________|
        //
        for (int i = 0; i < buildings.length; i++) {
            Building3D building = buildings[i];

            if (building.isInside(pos1)) {
                // pos1 is in the building
                if (building.isInside(pos2)) {
                    // pos2 is in the building, both are in the same building
                    return true;
                }

                // pos2 is not in the building
                // check if pos1 can communicate with pos2 through the door
                return building.canCommunicateThroughDoor(pos1, pos2);
            }
            else {
                // pos1 is not in the building
                if (building.isInside(pos2)) {
                    // pos2 is in the building
                    if (building.canCommunicateThroughDoor(pos2, pos1)) {
                        return true;
                    }
                }

                return false;
            }
        }
        return true;
    }

    public static double[] pairStatistics(MobileNode node1, MobileNode node2, double start, double duration, double range,
            boolean calculateMobility) {
        return pairStatistics(node1, node2, start, duration, range, calculateMobility, new Building3D[0]);
    }

    public static double[] pairStatistics(MobileNode _node1, MobileNode _node2, double start, double duration, double range,
            boolean calculateMobility, Building3D[] buildings) {
        MobileNode3D node1 = (MobileNode3D)_node1;
        MobileNode3D node2 = (MobileNode3D)_node2;
        double[] ch1 = node1.changeTimes();
        double[] ch2 = node2.changeTimes();

        // ### wenn einer der beiden Knoten ausgeschaltet ist, ist der Link down

        Vector<Double> changes = new Vector<Double>();
        int i1 = 0;
        int i2 = 0;
        double t0 = start;
        Position3D o1 = (Position3D)node1.positionAt(start);
        Position3D o2 = (Position3D)node2.positionAt(start);

        double mobility = 0.0;
        double on_time = 0.0;
        double D_spatial = 0.0;
        int D_spatial_count = 0;
        boolean connected = false;
        boolean nodes_on_before = false;

        // For average relative speed metric
        double Relative_speed = 0.0;
        int Relative_speed_count = 0;

        while (t0 < duration) {
            double t1;
            if (i1 < ch1.length) {
                if (i2 < ch2.length) {
                    t1 = (ch1[i1] < ch2[i2]) ? ch1[i1++] : ch2[i2++];
                } else {
                    t1 = ch1[i1++];
                }
            } else if (i2 < ch2.length) {
                t1 = ch2[i2++];
            } else {
                t1 = duration;
            }
            
            if (t1 > duration) {
                t1 = duration;
            }
            
            if (t1 > t0) {
                Position3D n1 = (Position3D)node1.positionAt(t1);
                Position3D n2 = (Position3D)node2.positionAt(t1);
                boolean conn_t0 = ((o1.distance(o2) <= range) && (sameBuilding(buildings, o1, o2)));
                boolean conn_t1 = ((n1.distance(n2) <= range) && (sameBuilding(buildings, n1, n2)));
                boolean nodes_on = ((o1.status != 2) && (o2.status != 2));
                if ((!connected) && conn_t0 && nodes_on) {
                    // either we just started, or some floating point op went wrong in the last
                    // epoch.
                    changes.addElement(new Double(t0));
                    connected = true;
                    if ((t0 != start) && nodes_on_before)
                        System.out.println("MobileNode.pairStatistics: fp correction 1: connect at " + t0);
                }
                if (connected && conn_t0 && (!nodes_on)) {
                    changes.addElement(new Double(t0));
                    connected = false;
                }

                double dt = t1 - t0; // time
                double dxo = o1.x - o2.x; // distance x at t0
                double dxn = n1.x - n2.x; // distance x at t1
                double dyo = o1.y - o2.y; // distance y at t0
                double dyn = n1.y - n2.y; // distance y at t1
                double dzo = o1.z - o2.z; // distance z at t0
                double dzn = n1.z - n2.z; // distance z at t1
                double c1 = (dxn - dxo) / dt;
                double c0 = (dxo * t1 - dxn * t0) / dt;
                double d1 = (dyn - dyo) / dt;
                double d0 = (dyo * t1 - dyn * t0) / dt;
                double e1 = (dzn - dzo) / dt;
                double e0 = (dzo * t1 - dzn * t0) / dt;

                // calculate degree of spatial dependence
                if (o1.distance(o2) < 2 * range) {
                    Position3D v_i = Position3D.diff(o1, n1);
                    Position3D v_j = Position3D.diff(o2, n2);
                    double s_i = Math.sqrt(Position3D.scalarProduct(v_i, v_i));
                    double s_j = Math.sqrt(Position3D.scalarProduct(v_j, v_j));
                    if (s_i > 0.0 && s_j > 0.0) {
                        double RD = Position3D.scalarProduct(v_i, v_j) / (s_i * s_j);
                        double SR = s_i > s_j ? (s_j / s_i) : (s_i / s_j);
                        D_spatial += RD * SR;
                        if (RD * SR != 0.0) {
                            D_spatial_count++;
                        }
                    }

                    // calculate the relative speed
                    Position3D relative_vector = Position3D.diff(v_i, v_j);
                    double RS = relative_vector.norm();

                    if (Math.abs(RS) > 0.0000001) {
                        Relative_speed += RS;
                        Relative_speed_count++;
                    }

                }

                if (nodes_on) {
                    on_time = on_time + dt;
                }

                if ((c1 != 0.0) || (d1 != 0.0) || e1 != 0.0) { // we have relative movement
                    double m = -1.0 * (c0 * c1 + d0 * d1 + e0 * e1) / (c1 * c1 + d1 * d1 + e1 * e1);
                    // calculate relative mobility
                    double relmob = 0.0;
                    if ((calculateMobility || printAngleStuff) && nodes_on) {
                        double dOld = Math.sqrt(dxo * dxo + dyo * dyo + dzo * dzo);
                        double dNew = Math.sqrt(dxn * dxn + dyn * dyn + dzo * dzo);
                        if ((m > t0) && (m < t1)) {
                            // at t0, nodes were losing distance to each other, but at t1, they are
                            // gaining distance again
                            Position3D in1 = (Position3D)node1.positionAt(m);
                            Position3D in2 = (Position3D)node2.positionAt(m);
                            double dInt = in2.distance(in1);
                            relmob = (Math.abs(dInt - dOld) + Math.abs(dNew - dInt)) / dt;
                        }
                        else {
                            relmob = Math.abs(dNew - dOld) / dt;
                        }
                        mobility += relmob;
                    }

                    double m2 = m * m;
                    double q = (c0 * c0 + d0 * d0 + e0 * e0 - range * range) / (c1 * c1 + d1 * d1 + e1 * e1);
                    if (m2 - q > 0.0) {
                        double d = Math.sqrt(m2 - q);
                        double min = m - d;
                        double max = m + d;

                        if ((min >= t0) && (min <= t1) && sameBuilding(buildings, o1, o2)) {
                            if (d < 0.01) {
                                System.out.println("---------------");
                                System.out.println("MobileNode.pairStatistics: The time span these 2 nodes are in range seems very");
                                System.out.println("  short. Might this be an error or a bad choice of parameters?");
                                System.out.println("o1=" + o1);
                                System.out.println("n1=" + n1);
                                System.out.println("o2=" + o2);
                                System.out.println("n2=" + n2);
                                System.out.println("[" + t0 + ";" + t1 + "]:[" + m + "-" + d + "=" + min + ";" + m + "+" + d + "=" + max
                                        + "]");
                                System.out.println("---------------");
                            }
                            if (nodes_on) {
                                if (!connected) {
                                    changes.addElement(new Double(min));
                                    connected = true;
                                }
                                else if (min - t0 > 0.001) {
                                    System.out.println("MobileNode.pairStatistics: sanity check failed (1)");
                                    System.exit(0);
                                }
                                else
                                    System.out.println("MobileNode.pairStatistics: connect too late: t=" + min + " t0=" + t0);
                            }
                            if (printAngleStuff) {
                                Position3D meet1 = (Position3D)node1.positionAt(min);
                                Position3D meet2 = (Position3D)node2.positionAt(min);
                                Position3D axis = Position3D.diff(meet1, meet2);
                                Position3D mov1 = Position3D.diff(o1, n1);
                                Position3D mov2 = Position3D.diff(o2, n2);
                                Position3D movd = Position3D.diff(mov2, mov1);

                                double v_delta = movd.norm() / dt;
                                double phi_a = Position3D.angle2(axis, mov1);
                                double phi_b = Position3D.angle2(axis, mov2);
                                double phi_delta = Position3D.angle2(axis, movd);

                                System.out
                                        .println("phi_a=" + phi_a + " phi_b=" + phi_b + " phi_delta=" + phi_delta + " v_delta=" + v_delta);
                            }
                        }
                        if ((max >= t0) && (max <= t1) && (sameBuilding(buildings, o1, o2))) {
                            if (nodes_on) // if not on, it was done before
                                if (connected) {
                                    changes.addElement(new Double(max));
                                    connected = false;
                                }
                                else if (max - t0 > 0.001) {
                                    System.out.println("MobileNode.pairStatistics: sanity check failed (2)");
                                    System.exit(0);
                                }
                                else {
                                    System.out.println("MobileNode.pairStatistics: disconnect too late: t=" + max + " t0=" + t0);
                                }
                        }
                    }
                }
                t0 = t1;
                o1 = n1;
                o2 = n2;
                nodes_on_before = nodes_on;

                // floating point inaccuracy detection:

                if (connected) {
                    if (!conn_t1) {
                        changes.addElement(new Double(t1));
                        connected = false;
                        System.out.println("MobileNode.pairStatistics: fp correction 2: disconnect at " + t1);
                    }
                }
                else { // !connected
                    if (conn_t1 && nodes_on) {
                        changes.addElement(new Double(t1));
                        connected = true;
                        System.out.println("MobileNode.pairStatistics: fp correction 3: connect at " + t1);
                    }
                }
            }
        }
        /* add disconnect at the end of time - for correct stats link is counted at link-break */
        /*
         * NA: I do not know, why this wasn't needed before. However, due to our changes we seem to
         * need it and it shouldn't change anything
         */
        if (connected) {
            changes.addElement(new Double(duration));
        }

        double[] result = new double[changes.size() + 6];
        for (int i = 0; i < result.length; i++) {
            if (i == 0) {
                result[i] = mobility;
                result[i + 1] = on_time;
                result[i + 2] = D_spatial;
                result[i + 3] = D_spatial_count;
                result[i + 4] = Relative_speed;
                result[i + 5] = Relative_speed_count;
                i += 5;
            }
            else {
                result[i] = ((Double)changes.elementAt(i - 6)).doubleValue();
            }
        }

        return result;
    }

    /**
     * Finds the degree of temporal dependence for a node.
     * 
     * @param node
     *            Node being calculated
     * @param start
     *            Start time of the simulation
     * @param end
     *            End time of the simulation
     * @param c
     *            Value used to determine if times are too far apart to use
     * @return A double array containing the dependence and the number of calculations
     */
    public static double[] getDegreeOfTemporalDependence(MobileNode _node, double start, double end, double c) {
        MobileNode3D node = (MobileNode3D)_node;
        double temporal_dependence[] = new double[2];
        double[] change_times = node.changeTimes();

        double temp_dependence = 0.0;
        int temporal_dependence_count = 0;

        for (int i = 0; i < change_times.length - 1; i++) {
            for (int j = i + 1; j < change_times.length - 1; j++) {
                if (Math.abs(change_times[i] - change_times[j]) > c) {
                    Position3D vector_t = Position3D.diff((Position3D)node.positionAt(change_times[i]), 
                            (Position3D)node.positionAt(change_times[i + 1]));
                    Position3D vector_t_prime = Position3D.diff((Position3D)node.positionAt(change_times[j]), 
                            (Position3D)node.positionAt(change_times[j + 1]));
                    double time_t = change_times[i + 1] - change_times[i];
                    double time_t_prime = change_times[j + 1] - change_times[j];

                    double dependence = getDegreeOfDependence(vector_t, vector_t_prime, time_t, time_t_prime);

                    if (Math.abs(dependence) > 0.0000001) {
                        temp_dependence += dependence;
                        temporal_dependence_count++;
                    }
                }
            }
        }

        temporal_dependence[0] = temp_dependence;
        temporal_dependence[1] = temporal_dependence_count;

        return temporal_dependence;
    }

    /**
     * Get the degree of dependence between two vectors
     * 
     * @param vector_i
     *            First vector
     * @param vector_j
     *            Second vector
     * @param time_i
     *            First time interval
     * @param time_j
     *            Second time interval
     * @return Double of the dependence
     */
    public static double getDegreeOfDependence(Position _vector_i, Position _vector_j, double time_i, double time_j) {
        Position3D vector_i = (Position3D)_vector_i;
        Position3D vector_j = (Position3D)_vector_j;
        double dependence = 0.0;
        double speed_i = vector_i.norm() / time_i;
        double speed_j = vector_j.norm() / time_j;

        if (speed_i > 0.0 && speed_j > 0.0) {
            double RD = Position3D.scalarProduct(vector_i, vector_j) / (vector_i.norm() * vector_j.norm());
            double SR = speed_i > speed_j ? (speed_j / speed_i) : (speed_i / speed_j);
            dependence = RD * SR;
        }
        return dependence;
    }

    public static double[] getConnectionTime(MobileNode node1, MobileNode node2, double start, double duration, double range) {
        return getConnectionTime(node1, node2, start, duration, range, new Building3D[0]);
    }

    public static double[] getConnectionTime(MobileNode _node1, MobileNode _node2, double start, double duration, double range,
            Building3D[] buildings) {

        MobileNode3D node1 = (MobileNode3D)_node1;
        MobileNode3D node2 = (MobileNode3D)_node2;
        double[] ch1 = node1.changeTimes();
        double[] ch2 = node2.changeTimes();

        double on_time = 0.0;
        double con_time = 0.0;
        double link_up_at = 0.0;

        int i1 = 0;
        int i2 = 0;
        double t0 = start;
        Position3D o1 = (Position3D)node1.positionAt(start);
        Position3D o2 = (Position3D)node2.positionAt(start);

        boolean connected = false;

        while (t0 < duration) {
            double t1;
            if (i1 < ch1.length) {
                if (i2 < ch2.length) {
                    t1 = (ch1[i1] < ch2[i2]) ? ch1[i1++] : ch2[i2++];
                } else {
                    t1 = ch1[i1++];
                }
            } else if (i2 < ch2.length) {
                t1 = ch2[i2++];
            } else {
                t1 = duration;
            }
            
            if (t1 > duration)
                t1 = duration;
            
            if (t1 > t0) {
                Position3D n1 = (Position3D)node1.positionAt(t1);
                Position3D n2 = (Position3D)node2.positionAt(t1);
                boolean conn_t0 = ((o1.distance(o2) <= range) && (sameBuilding(buildings, o1, o2)));
                boolean conn_t1 = ((n1.distance(n2) <= range) && (sameBuilding(buildings, n1, n2)));
                boolean nodes_on = ((o1.status != 2) && (o2.status != 2));
                if ((!connected) && conn_t0 && nodes_on) {
                    // either we just started, or some floating point op went wrong in the last
                    // epoch.
                    link_up_at = t0;
                    connected = true;
                }
                if (connected && conn_t0 && (!nodes_on)) {
                    con_time = con_time + (t0 - link_up_at);
                    connected = false;
                }

                double dt = t1 - t0; // time
                double dxo = o1.x - o2.x; // distance x at t0
                double dxn = n1.x - n2.x; // distance x at t1
                double dyo = o1.y - o2.y; // distance y at t0
                double dyn = n1.y - n2.y; // distance y at t1
                double dzo = o1.z - o2.z; // distance z at t0
                double dzn = n1.z - n2.z; // distance z at t1
                double c1 = (dxn - dxo) / dt;
                double c0 = (dxo * t1 - dxn * t0) / dt;
                double d1 = (dyn - dyo) / dt;
                double d0 = (dyo * t1 - dyn * t0) / dt;
                double e1 = (dzn - dzo) / dt;
                double e0 = (dzo * t1 - dzn * t0) / dt;

                if (nodes_on) {
                    on_time = on_time + dt;
                }

                if ((c1 != 0.0) || (d1 != 0.0) || (e1 != 0.0)) { // we have relative movement
                    // TODO: check if calculations are correct for 3D coordinates
                    double m = -1.0 * (c0 * c1 + d0 * d1 + e0 * e1) / (c1 * c1 + d1 * d1 + e1 * e1);
                    double m2 = m * m;
                    double q = (c0 * c0 + d0 * d0 + e0 * e0 - range * range) / (c1 * c1 + d1 * d1 + e1 * e1);
                    if (m2 - q > 0.0) {
                        double d = Math.sqrt(m2 - q);
                        double min = m - d;
                        double max = m + d;
                        if ((min >= t0) && (min <= t1) && sameBuilding(buildings, o1, o2)) {
                            if (d < 0.01) {
                                System.out.println("---------------");
                                System.out.println("MobileNode.pairStatistics: The time span these 2 nodes are in range seems very");
                                System.out.println("  short. Might this be an error or a bad choice of parameters?");
                                System.out.println("o1=" + o1);
                                System.out.println("n1=" + n1);
                                System.out.println("o2=" + o2);
                                System.out.println("n2=" + n2);
                                System.out.println("[" + t0 + ";" + t1 + "]:[" + m + "-" + d + "=" + min + ";" + m + "+" + d + "=" + max
                                        + "]");
                                System.out.println("---------------");
                            }
                            if (nodes_on) {
                                if (!connected) {
                                    link_up_at = min;
                                    connected = true;
                                }
                                else if (min - t0 > 0.001) {
                                    System.out.println("MobileNode.pairStatistics: sanity check failed (1)");
                                    System.exit(0);
                                }
                                else
                                    System.out.println("MobileNode.pairStatistics: connect too late: t=" + min + " t0=" + t0);
                            }
                        }
                        if ((max >= t0) && (max <= t1) && (sameBuilding(buildings, o1, o2))) {
                            if (nodes_on) // if not on, it was done before
                                if (connected) {
                                    con_time = con_time + (max - link_up_at);
                                    connected = false;
                                }
                                else if (max - t0 > 0.001) {
                                    System.out.println("MobileNode.pairStatistics: sanity check failed (2)");
                                    System.exit(0);
                                }
                                else
                                    System.out.println("MobileNode.pairStatistics: disconnect too late: t=" + max + " t0=" + t0);
                        }
                    }
                }
                t0 = t1;
                o1 = n1;
                o2 = n2;

                // floating point inaccuracy detection:

                if (connected) {
                    if (!conn_t1) {
                        con_time = con_time + (t1 - link_up_at);
                        connected = false;
                        System.out.println("MobileNode.pairStatistics: fp correction 2: disconnect at " + t1);
                    }
                }
                else { // !connected
                    if (conn_t1 && nodes_on) {
                        link_up_at = t1;
                        connected = true;
                        System.out.println("MobileNode.pairStatistics: fp correction 3: connect at " + t1);
                    }
                }
            }
        }
        /* add disconnect at the end of time - for correct stats link is counted at link-break */
        /*
         * NA: I do not know, why this wasn't needed before. However, due to our changes we seem to
         * need it and it shouldn't change anything
         */
        if (connected) {
            con_time = con_time + (duration - link_up_at);
        }

        double[] result = new double[2];
        result[0] = on_time;
        result[1] = con_time;
        return result;
    }

    public static double[] getSpeedoverTime(MobileNode _node, double start, double end, double interval) {
        MobileNode3D node = (MobileNode3D)_node;
        int size = (int)(((end - start) / interval) + 1);
        double[] speed = new double[2 * size]; // it's not the speed - only distances and times

        double[] cht = node.changeTimes();

        double[] dist_cht = new double[cht.length];
        boolean[] on_cht = new boolean[cht.length];
        double[] time_cht = new double[cht.length];
        double[] time_start = new double[cht.length];

        double t0 = start;
        Position3D p0 = (Position3D)node.positionAt(start);

        for (int i = 0; i < cht.length; i++) {

            double t1 = cht[i];
            double dt = t1 - t0;

            Position3D p1 = (Position3D)node.positionAt(t1);

            double dp = p0.distance(p1);

            dist_cht[i] = dp;
            on_cht[i] = (p0.status != 2.0); // if status of t0 == 2.0 node is off/away
            time_cht[i] = dt;
            time_start[i] = t0;

            t0 = t1;
            p0 = p1;
        }

        int i = 0;
        for (int j = 0; j < size; j++) {
            int k = 0;
            while ((time_start.length > i + k) && (time_start[i + k] < (start + (j + 1) * interval))) {
                k++;
            }

            // calc on_time in interval and distance for interval
            double on_time = 0.0;
            double dist_on = 0.0;
            for (int i_run = i; i_run < i + k; i_run++) {
                if (on_cht[i_run]) {
                    double help = time_cht[i_run];
                    double help_dist = dist_cht[i_run];
                    if (time_start[i_run] < (start + j * interval)) {
                        help = help - (start + j * interval - time_start[i_run]);
                    }
                    if ((time_start[i_run] + time_cht[i_run]) > (start + (j + 1) * interval)) {
                        help = help - ((time_start[i_run] + time_cht[i_run]) - (start + (j + 1) * interval));
                    }
                    help_dist = help_dist - ((1 - help / time_cht[i_run]) * help_dist);

                    on_time = on_time + help;
                    dist_on = dist_on + help_dist;
                }
            }

            speed[2 * j] = dist_on; // speed
            speed[(2 * j) + 1] = on_time; // on_time

            i = i + k - 1;
        }

        return speed;
    }
}