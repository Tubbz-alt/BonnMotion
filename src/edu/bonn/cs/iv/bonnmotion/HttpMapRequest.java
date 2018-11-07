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

package edu.bonn.cs.iv.bonnmotion;

//import java.awt.geom.Point2D;
import java.io.*;
import java.net.*;
import java.util.Vector;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class HttpMapRequest {
	private static final String OSM_XAPI_URL = "http://www.informationfreeway.org/api/0.6/";
	private static final String ORS_URL = "http://openls.geog.uni-heidelberg.de/cs-uni-bonn/route";

	/** counter for OSM queries */
	public static long countOSMQueries = 0;
	/** counter for ORS requests */
	public static long countORSRequests = 0;
	/** counter for failed ORS requests */
	public static long countFailedORSRequests = 0;

	public static String sendOSMMapQuery(BoundingBox bb) {
		BoundingBox newbb = bb.transform();
		checkSize(newbb);
		String request = "map?bbox=" + newbb.left + "," + newbb.bottom + "," + newbb.right + "," + newbb.top;
		return getOSMQueryResponseString(request);
	}

	public static InputStream getOSMQueryResponseStream(String request) {
		InputStream response = null;
		try {
			URL queryURL = new URL(OSM_XAPI_URL + request);
			System.out.println("\nOSM Request:\n" + OSM_XAPI_URL + request + "\n");
			URLConnection conn = queryURL.openConnection();
			conn.setReadTimeout(30000);
			response = conn.getInputStream();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		countOSMQueries++;

		return response;
	}

	public static String getOSMQueryResponseString(String request) {
		String response = "";
		try {
			URL queryURL = new URL(OSM_XAPI_URL + request);
			System.out.println("\nOSM Request:\n" + OSM_XAPI_URL + request + "\n");
			URLConnection conn = queryURL.openConnection();
			conn.setReadTimeout(30000);
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = br.readLine()) != null)
				response += line + "\n";

			br.close();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		countOSMQueries++;

		return response;
	}

	public static void printORSResponse(Position src, Position dst, int epsgCode, String pref) {
		printORSResponse(src, dst, epsgCode, pref, ORS_URL);
	}

	public static void printORSResponse(Position src, Position dst, int epsgCode, String pref, String ors_url) {
		if (ors_url == null)
			ors_url = ORS_URL;

		try {
			// send route request
			String request = createXMLRouteRequest(src, dst, epsgCode, pref);
			System.out.println("\nORS Request:\n" + request + "\n");
			URL url = new URL(ors_url);
			URLConnection conn = url.openConnection();
			conn.setDoOutput(true);
			OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());
			osw.write("REQUEST=" + request);
			osw.flush();
			osw.close();

			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			System.out.println("\nORS Response:");
			String line = "";
			while ((line = rd.readLine()) != null)
				System.out.println(line);

			rd.close();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	public static Position[] getORSRouteWaypoints(Position src, Position dst, int epsgCode, String pref) {
		return getORSRouteWaypoints(src, dst, epsgCode, pref, ORS_URL);
	}

	public static Position[] getORSRouteWaypoints(Position src, Position dst, int epsgCode, String pref, String ors_url) {
		if (ors_url == null)
			ors_url = ORS_URL;
		Position[] result = null;

		try {
			// send route request
			String request = createXMLRouteRequest(src, dst, epsgCode, pref);
			URL url = new URL(ors_url);
			URLConnection conn = url.openConnection();
			conn.setDoOutput(true);
			OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());
			osw.write("REQUEST=" + request);
			osw.flush();
			osw.close();

			// receive response
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(conn.getInputStream());
			NodeList nl = doc.getElementsByTagName("gml:pos");
			Vector<Position> wp = new Vector<Position>();
			for (int i = 2; i < nl.getLength(); i++) {
				String[] pos = nl.item(i).getTextContent().split(" ");
				wp.add(new Position(Double.parseDouble(pos[0]), Double.parseDouble(pos[1])));
			}

			if (nl.getLength() == 0) {
				nl = doc.getElementsByTagName("xls:Error");
				for (int i = 0; i < nl.getLength(); i++) {
					String errorMessage = nl.item(i).getAttributes().getNamedItem("message").getNodeValue();
					System.out.println("ORSError: " + errorMessage);
				}

				countFailedORSRequests++;
			}
			else {
				// ORS does _not_ necessarily include the start and end
				// positions from the request!
				if (!nl.item(0).getTextContent().equals(src.x + " " + src.y))
					wp.add(0, src);
				if (!nl.item(nl.getLength() - 1).getTextContent().equals(dst.x + " " + dst.y))
					wp.add(dst);

				result = (Position[])wp.toArray(new Position[wp.size()]);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		countORSRequests++;

		return result;
	}

	public static double getORSRouteDistance(Position src, Position dst, int epsgCode, String pref) {
		return getORSRouteDistance(src, dst, epsgCode, pref, ORS_URL);
	}

	public static double getORSRouteDistance(Position src, Position dst, int epsgCode, String pref, String ors_url) {
		if (ors_url == null)
			ors_url = ORS_URL;
		double result = Double.MAX_VALUE;

		try {
			// send route request
			String request = createXMLRouteRequest(src, dst, epsgCode, pref);
			URL url = new URL(ors_url);
			URLConnection conn = url.openConnection();
			conn.setDoOutput(true);
			OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());
			osw.write("REQUEST=" + request);
			osw.flush();
			osw.close();

			// receive response
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(conn.getInputStream());
			NodeList nl = doc.getElementsByTagName("xls:Error");
			if (nl.getLength() > 0) {
				for (int i = 0; i < nl.getLength(); i++) {
					String errorMessage = nl.item(i).getAttributes().getNamedItem("message").getNodeValue();
					System.out.println("ORSError: " + errorMessage);
				}

				countFailedORSRequests++;
				result = -1.0;
			}
			else if (pref.equals("Fastest")) {
				nl = doc.getElementsByTagName("xls:TotalTime");
				String timeString = nl.item(0).getTextContent();
				// example time string: "P0DT0H42M26S"
				int days = 0;
				int hours = 0;
				int minutes = 0;
				int seconds = 0;
				boolean containsD = timeString.contains("D");
				boolean containsH = timeString.contains("H");
				boolean containsM = timeString.contains("M");
				boolean containsS = timeString.contains("S");
				if (containsS) {
					int i = containsM ? timeString.indexOf("M") : timeString.indexOf("T");
					int j = timeString.indexOf("S");
					seconds = Integer.parseInt(timeString.substring(i + 1, j));
				}
				if (containsM) {
					int i = containsH ? timeString.indexOf("H") : timeString.indexOf("T");
					int j = timeString.indexOf("M");
					minutes = Integer.parseInt(timeString.substring(i + 1, j));
				}
				if (containsH) {
					int i = timeString.indexOf("T");
					int j = timeString.indexOf("H");
					hours = Integer.parseInt(timeString.substring(i + 1, j));
				}
				if (containsD) {
					int i = timeString.indexOf("P");
					int j = timeString.indexOf("D");
					days = Integer.parseInt(timeString.substring(i + 1, j));
				}

				result = 86400 * days + 3600 * hours + 60 * minutes + seconds;
			}
			else if (pref.equals("Shortest")) {
				nl = doc.getElementsByTagName("xls:TotalDistance");
				NamedNodeMap nnm = nl.item(0).getAttributes();
				result = Double.parseDouble(nnm.getNamedItem("value").getNodeValue());
			}
			else {
				System.out.println("Error: unknown DistanceMetric String!");
				System.exit(0);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		countORSRequests++;

		return result;
	}

	public static boolean isValidForORS(Position pos, int epsgCode, String pref) {
		return isValidForORS(pos, epsgCode, pref, ORS_URL);
	}

	public static boolean isValidForORS(Position pos, int epsgCode, String pref, String ors_url) {
		if (ors_url == null)
			ors_url = ORS_URL;
		boolean result = true;

		try {
			// send route request
			String request = createXMLRouteRequest(pos, pos, epsgCode, pref);
			URL url = new URL(ors_url);
			URLConnection conn = url.openConnection();
			conn.setDoOutput(true);
			OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());
			osw.write("REQUEST=" + request);
			osw.flush();
			osw.close();

			// receive response
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(conn.getInputStream());
			NodeList nl = doc.getElementsByTagName("xls:Error");
			if (nl.getLength() > 0) {
				for (int i = 0; i < nl.getLength(); i++) {
					String errorMessage = nl.item(i).getAttributes().getNamedItem("message").getNodeValue();
					System.out.println("ORSError: " + errorMessage);
					// String locationPath =
					// nl.item(i).getAttributes().getNamedItem("locationPath").getNodeValue();
					// if (locationPath.equals("Position - \'Start\'") ||
					// errorMessage.startsWith("Position \'Start\' not possible"))
					// result = false;
				}

				result = false;
				countFailedORSRequests++;
			}

		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		countORSRequests++;

		return result;
	}

	public static String createXMLRouteRequest(Position src, Position dst, int epsgCode, String pref) {
		String result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		result += "<xls:XLS xmlns:xls=\"http://www.opengis.net/xls\"";
		result += " xmlns:sch=\"http://www.ascc.net/xml/schematron\"";
		result += " xmlns:gml=\"http://www.opengis.net/gml\"";
		result += " xmlns:xlink=\"http://www.w3.org/1999/xlink\"";
		result += " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"";
		result += " xsi:schemaLocation=\"http://www.opengis.net/xls";
		result += " http://schemas.opengis.net/ols/1.1.0/RouteService.xsd\" version=\"1.1\"";
		result += " xls:lang=\"en\">\n";
		result += "<xls:RequestHeader srsName=\"EPSG:" + epsgCode + "\"/>\n";
		result += "<xls:Request methodName=\"RouteRequest\" requestID=\"123456789\" version=\"1.1\">\n";
		result += "<xls:DetermineRouteRequest distanceUnit=\"M\">\n";
		result += "<xls:RoutePlan>\n";
		result += "<xls:RoutePreference>" + pref + "</xls:RoutePreference>\n";
		result += "<xls:WayPointList>\n";
		result += getORSXMLPointString("Start", src, epsgCode);
		result += getORSXMLPointString("End", dst, epsgCode);
		result += "</xls:WayPointList>\n";
		result += "</xls:RoutePlan>\n";
		result += "<xls:RouteGeometryRequest/>\n";
		result += "</xls:DetermineRouteRequest>\n";
		result += "</xls:Request>\n";
		result += "</xls:XLS>\n";

		return result;
	}

	private static String getORSXMLPointString(String type, Position pos, int epsgCode) {
		String result = "<xls:" + type + "Point>\n";
		result += "<xls:Position>\n";
		result += "<gml:Point srsName=\"EPSG:" + epsgCode + "\">\n";
		result += "<gml:pos>" + pos.x + " " + pos.y + "</gml:pos>\n";
		result += "</gml:Point>\n";
		result += "</xls:Position>\n";
		result += "</xls:" + type + "Point>\n";

		return result;
	}

	private static void checkSize(BoundingBox bb) {
		// check size of bounding box
		double bbSize = (bb.right - bb.left) * (bb.top - bb.bottom);
		if (!(bbSize < 100)) {
			System.out.println("Error: The size of the bounding box must be less than 100 square degrees!");
			System.exit(0);
		}
	}
}
