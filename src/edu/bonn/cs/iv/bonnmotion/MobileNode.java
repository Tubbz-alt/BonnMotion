package edu.bonn.cs.iv.bonnmotion;

import java.util.Vector;

/** Mobile node. */

public class MobileNode {
	protected static final int debug = 0;
	protected static final boolean printAngleStuff = false;

	/** Times when mobile changes speed or direction. */
	protected double[] ct = null;

	protected Vector<Waypoint> waypoints = new Vector<Waypoint>();

	/** Optimised for waypoints coming in with increasing time.
	@return Success of insertion (will return false iff there is already another waypoint in the list with same time but different position). */
	public boolean add(double time, Position pos) {
		ct = null;
		int i = waypoints.size() - 1;
		while (i >= 0) {
			Waypoint w = waypoints.elementAt(i);
			if (time > w.time) {
				waypoints.insertElementAt(new Waypoint(time, pos), i + 1);
				return true;
			} else if (time == w.time) {
				if (pos.equals(w.pos))
					return true;
				else 
					return false;
			} else {
				i--;
				System.err.println( "warning: MobileNode: trying to insert waypoint in the past <1>.");
				System.err.println( "w.time: " +w.time + " time: " + time );
			}

		}
		waypoints.insertElementAt(new Waypoint(time, pos), 0);
		return true;
	}

	/**
	 * Add Waypoint from an other node.
	 * @param _node Waypoints of node to add
	 */
	public void add( MobileNode _node ) {
		double timeOffset = 0;

		if ( waypoints.size() > 0 )
			timeOffset = ((Waypoint) waypoints.lastElement()).time + 0.0001; // Sonst ein Zeipunkt mit 2 Positionen 

		for ( int i=0; i<_node.waypoints.size(); i++ ) {
			Waypoint next = (Waypoint) _node.waypoints.get(i);
			waypoints.add( new Waypoint(next.time + timeOffset, next.pos) );
		}
	}

	/** Remove the latest waypoint (last in the internal list). */
	public void removeLastElement() {
		waypoints.remove(waypoints.lastElement());
	}

	/** @return the latest waypoint (last in the internal list). */
	public Waypoint lastElement() {
		return waypoints.lastElement();
	}

	public int numWaypoints() {
		return waypoints.size();
	}

	public Waypoint getWaypoint(int idx) {
		return waypoints.elementAt(idx);
	}

	/** @return the vector of waypoints. */
//	this is just so dirty!!
	/*	public Vector waypoints() {
		return waypoints;
	} */

	/** Move all waypoints by a certain offset. */
	public void shiftPos(double _x, double _y) {
		for (int i = 0; i < waypoints.size(); i++) {
			Waypoint oldWP = waypoints.get(i);
			Waypoint newWP =
				new Waypoint(oldWP.time, new Position(oldWP.pos.x + _x, oldWP.pos.y + _y) );
			waypoints.setElementAt(newWP, i);
		}
	}

	/** @return Array with times when this mobile changes speed or direction. */
	public double[] changeTimes() {
		if (ct == null) {
			ct = new double[waypoints.size()];
			for (int i = 0; i < ct.length; i++)
				ct[i] = waypoints.elementAt(i).time;
		}
		return ct;
	}

	public void cut(double begin, double end) {
		if (waypoints.size() == 0)
			return;
		ct = null;
		Vector<Waypoint> nwp = new Vector<Waypoint>();
		Waypoint w = null;
		double oldstatus = 0.0;
		for (int i = 0; i < waypoints.size(); i++) {
			Waypoint w2 = waypoints.elementAt(i);
			if ((w2.time >= begin) && (w2.time <= end)) {
				//System.out.println(w2.time+","+w2.pos.status+" old:"+oldstatus);
				w = w2;
				if (nwp.size() == 0) {
					// create new waypoint for 0.0 with status from Waypoint before
					Position hpos = positionAt(begin);
					Position bpos = new Position(hpos.x, hpos.y, oldstatus);
					//if (!bpos.equals(w.pos))
					nwp.addElement(new Waypoint(0.0, bpos));
					if (w.time > begin) {
						nwp.addElement(new Waypoint(w.time - begin, w.pos));
					}				
				} else 
					nwp.addElement(new Waypoint(w.time - begin, w.pos));
			}
			// trace status
			if (w2.pos.status == 2.0) {
				oldstatus = 2.0;
			} else if (w2.pos.status == 1.0) {
				oldstatus = 0.0;
			}
		}
		if (w == null) { // no waypoints with the given time span    
			Waypoint start = new Waypoint(0.0, positionAt(begin));
			Waypoint stop = new Waypoint(end - begin, positionAt(end));
			nwp.addElement(start);
			if (!start.pos.equals(stop.pos))
				nwp.addElement(stop);
		} else if (w.time < end) {
			Position epos = positionAt(end);
			if (!epos.equals(w.pos))
				nwp.addElement(new Waypoint(end - begin, epos));
		}
		waypoints = nwp;		
	}

	public String movementString() {
		String r = null;
		StringBuffer sb = new StringBuffer(100*waypoints.size());
		for (int i = 0; i < waypoints.size(); i++) {
			Waypoint w = waypoints.elementAt(i);
			sb.append(" ");
			sb.append(w.time);
			sb.append(" ");
			sb.append(w.pos.x);
			sb.append(" ");
			sb.append(w.pos.y);
		}
		sb.deleteCharAt(0);
		r = sb.toString();
		return r;
	}

	/** @param border The border we add around the scenario to prevent ns-2 from crashing; this value is added to all x- and y-values. */
	public String[] movementStringNS(String id, double border) {
		System.out.println("waypoints " + waypoints.size());
		String[] r = new String[waypoints.size() + 1];
		Waypoint w = waypoints.elementAt(0);
		r[0] = id + " set X_ " + (w.pos.x + border);
		r[1] = id + " set Y_ " + (w.pos.y + border);
		for (int i = 1; i < waypoints.size(); i++) {
			Waypoint w2 = waypoints.elementAt(i);
			double dist = w.pos.distance(w2.pos);
			r[i + 1] =
				"$ns_ at "
				+ w.time
				+ " \""
				+ id
				+ " setdest "
				+ (w2.pos.x + border)
				+ " "
				+ (w2.pos.y + border)
				+ " "
				+ (dist / (w2.time - w.time))
				+ "\"";
			if (dist == 0.0)
				r[i + 1] = "# " + r[i + 1];
			// hack alert... but why should we schedule these in ns-2?
			w = w2;
		}
		return r;
	}

	public String placementStringGlomo(String id) {
		Waypoint w = waypoints.elementAt(0);
		String r = id + " 0S (" + w.pos.x + ", " + w.pos.y + ", 0.0)";
		return r;
	}

	public String[] movementStringGlomo(String id) {
		String[] r = new String[waypoints.size() - 1];
		//Waypoint w = (Waypoint) waypoints.elementAt(0);
		for (int i = 1; i < waypoints.size(); i++) {
			Waypoint w2 = waypoints.elementAt(i);
			r[i - 1] = id + " " + w2.time + "S (" + w2.pos.x + ", " + w2.pos.y + ", 0.0)";
		}
		return r;
	}

	/** @return Position of this mobile at a given time. */
	public Position positionAt(double time) {
		Position p1 = null;
		double t1 = 0.0;
		for (int i = 0;
		i < waypoints.size();
		i++) { // oh no, we should make this efficient (esp. for gauss-markov) -> binary search
			Waypoint w = waypoints.elementAt(i);
			if (w.time == time)
				return w.pos;
			else if (w.time > time) {
				if ((p1 == null) || p1.equals(w.pos))
					return w.pos;
				else {
					double weight = (time - t1) / (w.time - t1);
					return new Position(
							p1.x * (1 - weight) + w.pos.x * weight,
							p1.y * (1 - weight) + w.pos.y * weight,
							p1.status);
				}
			}
			p1 = w.pos;
			t1 = w.time;
		}
		return p1;
	}

	public static boolean sameBuilding(Building[] buildings, Position pos1, Position pos2) {
		// XXX: take care that two nodes do not communicate via opposite wall such that the following
		//      will not be allowed:
		//     __________
		//     |        |
		//      x       | x
		//     |________|
		//    
		for (int i = 0; i < buildings.length; i++) {
			Building b = buildings[i];
			if ((pos1.x > b.x1) && (pos1.x < b.x2) && (pos1.y > b.y1) && (pos1.y < b.y2)) {
				if ((pos2.x > b.x1) && (pos2.x < b.x2) && (pos2.y > b.y1) && (pos2.y < b.y2)) {
					return true;
				}
				else if ((b.x1 == b.doorx) && (pos2.x > b.doorx - 10) && (pos2.y == b.doory) && (pos1.x < b.doorx + 10) && (pos1.y == b.doory)) {
					return true; 
				}
				else if ((b.x2 == b.doorx) && (pos2.x < b.doorx + 10) && (pos2.y == b.doory) && (pos1.x > b.doorx - 10) && (pos1.y == b.doory)) {
					return true; 
				}
				else if ((b.y1 == b.doory) && (pos2.y > b.doory - 10) && (pos2.x == b.doorx) && (pos1.x == b.doorx) && (pos1.y < b.doory + 10)) {
					return true; 
				}
				else if ((b.y1 == b.doory) && (pos2.y < b.doory + 10) && (pos2.x == b.doorx) && (pos1.x == b.doorx) && (pos1.y > b.doory - 10)) {
					return true; 
				}
				else {
					return false;
				}
			}
			if ((pos2.x > b.x1) && (pos2.x < b.x2) && (pos2.y > b.y1) && (pos2.y < b.y2)) {
				if ((b.x1 == b.doorx) && (pos1.x > b.doorx - 10) && (pos1.y == b.doory) && (pos2.x < b.doorx + 10) && (pos2.y == b.doory)) {
					return true; 
				}
				else if ((b.x2 == b.doorx) && (pos1.x < b.doorx + 10) && (pos1.y == b.doory) && (pos2.x > b.doorx - 10) && (pos2.y == b.doory)) {
					return true; 
				}
				else if ((b.y1 == b.doory) && (pos1.y > b.doory - 10) && (pos1.x == b.doorx) && (pos2.x == b.doorx) && (pos2.y < b.doory + 10)) {
					return true; 
				}
				else if ((b.y1 == b.doory) && (pos1.y < b.doory + 10) && (pos1.x == b.doorx) && (pos2.x == b.doorx) && (pos2.y > b.doory - 10)) {
					return true; 
				}
				else {
					return false;
				}
			}
		}
		return true;
	}

	public static double[] pairStatistics(
			MobileNode node1,
			MobileNode node2,
			double start,
			double duration,
			double range,
			boolean calculateMobility
	) {
		return pairStatistics(node1,node2,start,duration,range,calculateMobility,new Building[0]);
	} 

	public static double[] pairStatistics(
			MobileNode node1,
			MobileNode node2,
			double start,
			double duration,
			double range,
			boolean calculateMobility,
			Building[] buildings) {

		double[] ch1 = node1.changeTimes();
		double[] ch2 = node2.changeTimes();

		//### wenn einer der beiden Knoten ausgeschaltet ist, ist der Link down

		Vector<Double> changes = new Vector<Double>();
		int i1 = 0;
		int i2 = 0;
		double t0 = start;
		Position o1 = node1.positionAt(start);
		Position o2 = node2.positionAt(start);
		double mobility = 0.0;
		double on_time = 0.0;
		boolean connected = false;
		boolean nodes_on_before = false;
		while (t0 < duration) {
			double t1;
			if (i1 < ch1.length)
				if (i2 < ch2.length)
					t1 = (ch1[i1] < ch2[i2]) ? ch1[i1++] : ch2[i2++];
					else
						t1 = ch1[i1++];
			else if (i2 < ch2.length)
				t1 = ch2[i2++];
			else
				t1 = duration;
			if (t1 > duration)
				t1 = duration;
			if (t1 > t0) {
				Position n1 = node1.positionAt(t1);
				Position n2 = node2.positionAt(t1);
				boolean conn_t0 = ((o1.distance(o2) <= range) && (sameBuilding(buildings,o1,o2)));
				boolean conn_t1 = ((n1.distance(n2) <= range) && (sameBuilding(buildings,n1,n2)));
				boolean nodes_on = ((o1.status != 2) && (o2.status != 2));
				if ((!connected) && conn_t0 && nodes_on) {
					// either we just started, or some floating point op went wrong in the last epoch.
					changes.addElement(new Double(t0));
					connected = true;
					if ((t0 != start)&&nodes_on_before)
						System.out.println("MobileNode.pairStatistics: fp correction 1: connect at " + t0);
				}
				if (connected && conn_t0 && (!nodes_on)) {
					changes.addElement(new Double(t0));
					connected = false;
				}
				if (debug > 1) {
					System.out.println("o1=" + o1);
					System.out.println("n1=" + n1);
					System.out.println("o2=" + o2);
					System.out.println("n2=" + n2);
					System.out.println("do=" + o1.distance(o2));
					System.out.println("dn=" + n1.distance(n2));
				}
				double dt = t1 - t0;          //time
				double dxo = o1.x - o2.x;     //distance x at t0
				double dxn = n1.x - n2.x;     //distance x at t1
				double dyo = o1.y - o2.y;     //distance y at t0
				double dyn = n1.y - n2.y;     //distance y at t1
				double c1 = (dxn - dxo) / dt;
				double c0 = (dxo * t1 - dxn * t0) / dt;
				double d1 = (dyn - dyo) / dt;
				double d0 = (dyo * t1 - dyn * t0) / dt;

				if (nodes_on) {
					on_time = on_time + dt;
				}

				if ((c1 != 0.0) || (d1 != 0.0)) { // we have relative movement
					double m = -1.0 * (c0 * c1 + d0 * d1) / (c1 * c1 + d1 * d1);
					// calculate relative mobility
					double relmob = 0.0;
					if ((calculateMobility || printAngleStuff)&&nodes_on) {
						double dOld = Math.sqrt(dxo * dxo + dyo * dyo);
						double dNew = Math.sqrt(dxn * dxn + dyn * dyn);
						if ((m > t0) && (m < t1)) {
							// at t0, nodes were losing distance to each other, but at t1, they are gaining distance again
							Position in1 = node1.positionAt(m);
							Position in2 = node2.positionAt(m);
							double dxi = in1.x - in2.x;
							double dyi = in1.y - in2.y;
							double dInt = Math.sqrt(dxi * dxi + dyi * dyi);
							relmob = (Math.abs(dInt - dOld) + Math.abs(dNew - dInt))/dt;
						} else
							relmob = Math.abs(dNew - dOld)/dt;
						mobility += relmob;
					}


					double m2 = m * m;
					double q = (c0 * c0 + d0 * d0 - range * range) / (c1 * c1 + d1 * d1);
					if (m2 - q > 0.0) {
						double d = Math.sqrt(m2 - q);
						double min = m - d;
						double max = m + d;
						if (debug > 1) {
							System.out.println("[" + t0 + ";" + t1 + "]:");
							System.out.println("\t[" + m + "-" + d + "=" + min + ";" + m + "+" + d + "=" + max + "]");
						}
						if ((min >= t0) && (min <= t1) && sameBuilding(buildings,o1,o2)) {
							if (d < 0.01) {
								System.out.println("---------------");
								System.out.println("MobileNode.pairStatistics: The time span these 2 nodes are in range seems very");
								System.out.println("  short. Might this be an error or a bad choice of parameters?");
								System.out.println("o1=" + o1);
								System.out.println("n1=" + n1);
								System.out.println("o2=" + o2);
								System.out.println("n2=" + n2);
								System.out.println( "[" + t0 + ";" + t1 + "]:[" + m + "-" + d + "=" + min + ";" + m + "+" + d + "=" + max + "]");
								System.out.println("---------------");
							}
							if (nodes_on) {
								if (!connected) {
									changes.addElement(new Double(min));
									connected = true;
									if (debug > 0)
										System.out.println("connect at " + min);
								} else if (min - t0 > 0.001) {
									System.out.println("MobileNode.pairStatistics: sanity check failed (1)");
									System.exit(0);
								} else
									System.out.println("MobileNode.pairStatistics: connect too late: t=" + min + " t0=" + t0);
							}
							if (printAngleStuff) {
								Position meet1 = node1.positionAt(min);
								Position meet2 = node2.positionAt(min);
								Position axis = Position.diff(meet1, meet2);
								Position mov1 = Position.diff(o1, n1);
								Position mov2 = Position.diff(o2, n2);
								Position movd = Position.diff(mov2, mov1);

								double v_delta = movd.norm() / dt;
								double phi_a = Position.angle2(axis, mov1);
								double phi_b = Position.angle2(axis, mov2);
								double phi_delta = Position.angle2(axis, movd);

								System.out.println( "phi_a=" + phi_a + " phi_b=" + phi_b + " phi_delta=" + phi_delta + " v_delta=" + v_delta);
							}
						}
						if ((max >= t0) && (max <= t1) && (sameBuilding(buildings, o1, o2))) {
							if(nodes_on) //if not on, it was done before
								if (connected) {
									changes.addElement(new Double(max));
									connected = false;
									if (debug > 0)
										System.out.println("disconnect at " + max);
								} else if (max - t0 > 0.001) {
									System.out.println("MobileNode.pairStatistics: sanity check failed (2)");
									System.exit(0);
								} else
									System.out.println("MobileNode.pairStatistics: disconnect too late: t=" + max + " t0=" + t0);
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
				} else { // !connected
					if (conn_t1 && nodes_on) {
						changes.addElement(new Double(t1));
						connected = true;
						System.out.println("MobileNode.pairStatistics: fp correction 3: connect at " + t1);
					}
				}
			}
		}
		/* add disconnect at the end of time - for correct stats link is counted at link-break */
		/* NA: I do not know, why this wasn't needed before. 
		   However, due to our changes we seem to need it and it shouldn't change anything */
		if (connected) 
			changes.addElement(new Double(duration));
		//System.out.println("Changes-Size: "+changes.size());

		double[] result = new double[changes.size() + 2];
		for (int i = 0; i < result.length; i++)
			if (i == 0) {
				result[i] = mobility;
				result[i+1] = on_time;
				++i;
			} else {
				result[i] = ((Double) changes.elementAt(i - 2)).doubleValue();
			}   
		return result;
	}



	public static double[] get_connection_time(
			MobileNode node1,
			MobileNode node2,
			double start,
			double duration,
			double range
	) {
		return get_connection_time(node1,node2,start,duration,range,new Building[0]);
	} 


	public static double[] get_connection_time(
			MobileNode node1,
			MobileNode node2,
			double start,
			double duration,
			double range,
			Building[] buildings) {

		double[] ch1 = node1.changeTimes();
		double[] ch2 = node2.changeTimes();

		double on_time = 0.0;
		double con_time = 0.0;
		double link_up_at = 0.0;

		int i1 = 0;
		int i2 = 0;
		double t0 = start;
		Position o1 = node1.positionAt(start);
		Position o2 = node2.positionAt(start);
		//double mobility = 0.0;
		boolean connected = false;
		//boolean nodes_on_before = false;
		while (t0 < duration) {
			double t1;
			if (i1 < ch1.length)
				if (i2 < ch2.length)
					t1 = (ch1[i1] < ch2[i2]) ? ch1[i1++] : ch2[i2++];
					else
						t1 = ch1[i1++];
			else if (i2 < ch2.length)
				t1 = ch2[i2++];
			else
				t1 = duration;
			if (t1 > duration)
				t1 = duration;
			if (t1 > t0) {
				Position n1 = node1.positionAt(t1);
				Position n2 = node2.positionAt(t1);
				boolean conn_t0 = ((o1.distance(o2) <= range) && (sameBuilding(buildings,o1,o2)));
				boolean conn_t1 = ((n1.distance(n2) <= range) && (sameBuilding(buildings,n1,n2)));
				boolean nodes_on = ((o1.status != 2) && (o2.status != 2));
				if ((!connected) && conn_t0 && nodes_on) {
					// either we just started, or some floating point op went wrong in the last epoch.
					link_up_at = t0;
					connected = true;
				}
				if (connected && conn_t0 && (!nodes_on)) {
					con_time = con_time + (t0 - link_up_at);
					connected = false;
				}

				double dt = t1 - t0;          //time
				double dxo = o1.x - o2.x;     //distance x at t0
				double dxn = n1.x - n2.x;     //distance x at t1
				double dyo = o1.y - o2.y;     //distance y at t0
				double dyn = n1.y - n2.y;     //distance y at t1
				double c1 = (dxn - dxo) / dt;
				double c0 = (dxo * t1 - dxn * t0) / dt;
				double d1 = (dyn - dyo) / dt;
				double d0 = (dyo * t1 - dyn * t0) / dt;

				if (nodes_on) {
					on_time = on_time + dt;
				}

				if ((c1 != 0.0) || (d1 != 0.0)) { // we have relative movement
					double m = -1.0 * (c0 * c1 + d0 * d1) / (c1 * c1 + d1 * d1);
					double m2 = m * m;
					double q = (c0 * c0 + d0 * d0 - range * range) / (c1 * c1 + d1 * d1);
					if (m2 - q > 0.0) {
						double d = Math.sqrt(m2 - q);
						double min = m - d;
						double max = m + d;
						if (debug > 1) {
							System.out.println("[" + t0 + ";" + t1 + "]:");
							System.out.println("\t[" + m + "-" + d + "=" + min + ";" + m + "+" + d + "=" + max + "]");
						}
						if ((min >= t0) && (min <= t1) && sameBuilding(buildings,o1,o2)) {
							if (d < 0.01) {
								System.out.println("---------------");
								System.out.println("MobileNode.pairStatistics: The time span these 2 nodes are in range seems very");
								System.out.println("  short. Might this be an error or a bad choice of parameters?");
								System.out.println("o1=" + o1);
								System.out.println("n1=" + n1);
								System.out.println("o2=" + o2);
								System.out.println("n2=" + n2);
								System.out.println( "[" + t0 + ";" + t1 + "]:[" + m + "-" + d + "=" + min + ";" + m + "+" + d + "=" + max + "]");
								System.out.println("---------------");
							}
							if (nodes_on) {
								if (!connected) {
									link_up_at = min;
									connected = true;
									if (debug > 0)
										System.out.println("connect at " + min);
								} else if (min - t0 > 0.001) {
									System.out.println("MobileNode.pairStatistics: sanity check failed (1)");
									System.exit(0);
								} else
									System.out.println("MobileNode.pairStatistics: connect too late: t=" + min + " t0=" + t0);
							}
						}
						if ((max >= t0) && (max <= t1) && (sameBuilding(buildings, o1, o2))) {
							if(nodes_on) //if not on, it was done before
								if (connected) {
									con_time = con_time + (max - link_up_at);
									connected = false;
									if (debug > 0)
										System.out.println("disconnect at " + max);
								} else if (max - t0 > 0.001) {
									System.out.println("MobileNode.pairStatistics: sanity check failed (2)");
									System.exit(0);
								} else
									System.out.println("MobileNode.pairStatistics: disconnect too late: t=" + max + " t0=" + t0);
						}
					}
				}
				t0 = t1;
				o1 = n1;
				o2 = n2;
				//nodes_on_before = nodes_on;

				// floating point inaccuracy detection:

				if (connected) {
					if (!conn_t1) {
						con_time = con_time + (t1 - link_up_at);
						connected = false;
						System.out.println("MobileNode.pairStatistics: fp correction 2: disconnect at " + t1);
					}
				} else { // !connected
					if (conn_t1 && nodes_on) {
						link_up_at = t1;
						connected = true;
						System.out.println("MobileNode.pairStatistics: fp correction 3: connect at " + t1);
					}
				}
			}
		}
		/* add disconnect at the end of time - for correct stats link is counted at link-break */
		/* NA: I do not know, why this wasn't needed before. 
		   However, due to our changes we seem to need it and it shouldn't change anything */
		if (connected) {
			con_time = con_time + (duration - link_up_at);
		}
		//System.out.println("Changes-Size: "+changes.size());

		double[] result = new double[2];
		result[0] = on_time;
		result[1] = con_time;
		return result;
	}


	public static double[] getSpeedoverTime(MobileNode node, double start, double end, double interval) {

		int size = (int) (((end - start) / interval) + 1);
		double[] speed = new double[2*size];  // it's not the speed - only distances and times

		double[] cht = node.changeTimes();

		double[] dist_cht = new double[cht.length];
		boolean[] on_cht = new boolean[cht.length];
		double[] time_cht = new double[cht.length];
		double[] time_start = new double[cht.length];	

		double t0 = start;
		Position p0 = node.positionAt(start);

		for (int i = 0; i < cht.length; i++) { 

			double t1 = cht[i];
			double dt = t1 - t0;

			Position p1 = node.positionAt(t1);

			double dx = p1.x - p0.x;
			double dy = p1.y - p0.y;

			double dp = Math.sqrt(dx * dx + dy * dy);

			dist_cht[i] = dp;
			on_cht[i] = (p0.status != 2.0); // if status of t0 == 2.0 node is off/away
			time_cht[i] = dt;
			time_start[i] = t0;

			t0 = t1;
			p0 = p1;
		}

		int i=0;
		for (int j=0; j<size; j++) {

			//interval of [start+j*interval;start+(j+1)*interval)

			int k = 0;
			//System.out.println("i:"+i+" k:"+k+" j:"+j+" array_length:"+time_start.length);
			while ((time_start.length > i+k)&&(time_start[i+k] < (start+(j+1)*interval))) {
				k++;
				//System.out.println(". i:"+i+" k:"+k+" j:"+j+" array_length:"+time_start.length);
			}

			// calc on_time in interval and distance for interval
			double on_time = 0.0;
			double dist_on = 0.0;
			for (int i_run = i; i_run < i+k; i_run++) {
				//System.out.println("i_run:"+i_run);
				if (on_cht[i_run]) {
					// falls nicht komplett drin, m�ssen wir rechnen
					double help = time_cht[i_run];
					double help_dist = dist_cht[i_run];
					if (time_start[i_run] < (start+j*interval)) {
						help = help - (start+j*interval - time_start[i_run]);
					}
					if ((time_start[i_run]+time_cht[i_run]) > (start+(j+1)*interval)) {
						help = help - ((time_start[i_run]+time_cht[i_run])-(start+(j+1)*interval));
					}
					help_dist = help_dist - ((1 - help/time_cht[i_run]) * help_dist);

					on_time = on_time + help;
					dist_on = dist_on + help_dist;
				}
			}

			speed[2*j] = dist_on; //speed
			speed[(2*j)+1] = on_time; //on_time

			i = i+k-1;

		}

		return speed;
	}


	public static double getNodesOnTime(MobileNode node, double duration) {

		double[] cht = node.changeTimes();
		double  on_time = 0.0;
		double t0 = 0.0;
		Position p0 = node.positionAt(t0);

		for (int i=0; i<cht.length; i++) { 

			double t1 = cht[i];
			double dt = t1 - t0;

			Position p1 = node.positionAt(t1);

			if (p0.status != 2.0) { // if status of t0 == 2.0 node is off/away
				on_time = on_time + dt;
			}

			t0 = t1;
			p0 = p1;
		}

		on_time = on_time + (duration - t0);

		return on_time;
	}

	public static boolean isNodeOffAtTime(MobileNode node, double time) {

		Position pos = node.positionAt(time);
		return (pos.status == 2);

	}

	public static double[] getOnOffChanges(MobileNode node) {

		double[] ch = node.changeTimes();
		boolean on = true;
		int events = 0;

		for (int t = 0; t < ch.length; t++) {
			if (node.positionAt(ch[t]).status > 0) {
				++events;
			}
		}

		double[] result = new double[events];
		int j = 0;
		for (int t = 0; t < ch.length; t++) {
			if (node.positionAt(ch[t]).status == 2) {
				result[j] = ch[t];
				++j;
				//if (!on) {
					//    System.err.println("Error: Node switches off, but is already switched off! (t="+ch[t]+")");
				//} 
				on = false;
			}
			if (node.positionAt(ch[t]).status == 1) {
				result[j] = ch[t];
				++j;
				if (on) {			    
					System.err.println("Error: Node switches on, but is already switched on! (t="+ch[t]+")");
				} 
				on = true;
			}		   
		}
		return result;
	}
}