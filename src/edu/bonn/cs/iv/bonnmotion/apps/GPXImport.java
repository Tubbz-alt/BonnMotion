package edu.bonn.cs.iv.bonnmotion.apps;

import java.awt.geom.Point2D;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.GZIPOutputStream;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.ProjectionFactory;

import edu.bonn.cs.iv.bonnmotion.App;
import edu.bonn.cs.iv.bonnmotion.ModuleInfo;

/**
 * Application that converts GPX files to Bonnmotion output.
 * javaproj-1.0.6.jar must be in classpath.
 *
 */
public class GPXImport extends App implements ContentHandler {
    private static ModuleInfo info;
    
    static {
        info = new ModuleInfo("GPXImport");
        info.description = "Application that converts GPX files to Bonnmotion format";
        
        info.major = 1;
        info.minor = 0;
        info.revision = ModuleInfo.getSVNRevisionStringValue("$LastChangedRevision: 269 $");
        
        info.contacts.add(ModuleInfo.BM_MAILINGLIST);
        info.authors.add("University of Bonn");
		info.affiliation = ModuleInfo.UNIVERSITY_OF_BONN;
    }
    
    public static ModuleInfo getInfo() {
        return info;
    }
    
	private class TrkPoint {
		public final double x;
		public final double y;
		public double z;
		public Date time;
		public double convertedTime;
		
		public TrkPoint(double x, double y) {
			this.x = x;
			this.y = y;
		}
	}
	
	private String fileName = null;

	private final Projection projection =
		ProjectionFactory.fromPROJ4Specification(
				new String[] {
						"+proj=latlong",
						"+ellps=WGS84",
						"+proj=utm",
						"+zone=32",
						"+ellps=WGS84"
				});
	
	private HashMap<String, ArrayList<Double>> bounds = new HashMap<String, ArrayList<Double>>();
	
	private boolean inTrkptContent = false;
	private boolean inTimeContent = false;
    private boolean inEleContent = false;
	private String timestr = "";
	private boolean compress = false;
    private boolean importHeight = false;
    private double defaultHeight = 0;
    private String invalidHeight = "-99999.000000";
	
	private TrkPoint currentTrkPoint = null;
	private ArrayList<TrkPoint> coordlist = new ArrayList<TrkPoint>();

	public GPXImport(String[] args) {
		this.bounds.put("min_x", new ArrayList<Double>());
		this.bounds.put("min_y", new ArrayList<Double>());
		this.bounds.put("max_x", new ArrayList<Double>());
		this.bounds.put("max_y", new ArrayList<Double>());
	    this.bounds.put("max_z", new ArrayList<Double>());
	    bounds.get("max_z").add(.0);
	    
		this.go(args);
	}

	@Override
	public void go(String[] args) {
		parse(args);

		if (this.fileName == null) {
			GPXImport.printHelp();
			System.exit(0);
		}

		XMLReader parser = null;
		try {
			parser = XMLReaderFactory.createXMLReader();
		} catch (SAXException e) {
			App.exceptionHandler("Could not create XML reader ", e);
		}

		parser.setContentHandler(this);

		try {
			parser.parse(fileName);
		} catch (Exception e) {
			App.exceptionHandler("Could not parse" , e);
		}
		
		this.createParams();
		this.createMovements();
	}
	
	private long calculateDateDiff(Date starttime, Date endtime) {
		GregorianCalendar cal1 = new GregorianCalendar();
		GregorianCalendar cal2 = new GregorianCalendar();
		
		cal1.setTime(endtime);
		cal2.setTime(starttime);
		
		long delta = cal1.getTime().getTime() - cal2.getTime().getTime();
		
		return delta;
	}
	
	private void createMovements() {
		double min_x = this.minFromDoubleList(this.bounds.get("min_x"));
		double min_y = this.minFromDoubleList(this.bounds.get("min_y"));
		
		Date starttime = this.coordlist.get(0).time;
		Date endtime = this.coordlist.get(this.coordlist.size() - 1).time;
		long delta = this.calculateDateDiff(starttime, endtime);
		
		PrintWriter movements = null;
		
		if (this.compress) {
			try {
				movements = 
					new PrintWriter(
							new GZIPOutputStream(
									new FileOutputStream(this.fileName + ".movements.gz")));
			} catch (Exception e) {
				App.exceptionHandler("Error opening ", e);
			}
		} else {
			movements =
				App.openPrintWriter(this.fileName + ".movements");
		}
		
		if (importHeight) {
		    movements.println("#3D");
		}
		
		for (TrkPoint p : this.coordlist) {
	        // check if this entry is outside duration range
            if (p.time.after(endtime)) {
                break;
            }
            // check if this entry is after starttime
            if (p.time.before(starttime)) {
                continue;
            }
            
            delta = this.calculateDateDiff(starttime, p.time);
            long days = Math.round(delta / (60.0 * 60.0 * 24.0 * 1000.0));
            long seconds = (long) (delta / 1000.0);
            long time = days * 24 * 3600 + seconds;
            
            p.convertedTime = time;
		}
		
		// This checks if multiple waypoints occupy the same timestamp and if so
		// diversifies the positions upon the second.
        for (int entry = 0; entry < this.coordlist.size(); entry++) {
            TrkPoint p = this.coordlist.get(entry);
            
            int occurrences = 0;
            for (TrkPoint x : this.coordlist) {
                if (x.convertedTime == p.convertedTime) {
                    occurrences++;
                }
            }
            
            if (occurrences > 1) {
                for (int i = 1; i < occurrences; i++) {
                    coordlist.get(entry+i).convertedTime += i*(1./occurrences);
                }
            }
        }
        
        // Probably due to double inaccurateness it can happen that the min_x/min_y shifts 
        // too little, resulting in negative coordinates. 
        // This is checked here and the shift is corrected if necessary
        for (TrkPoint p : this.coordlist) {
            double x = p.x - min_x;
            double y = p.y - min_y;
            if (x < 0) {
                min_x -= x + 0.001;
            }
            if (y < 0) {
                min_y -= y + 0.001;
            }
        }
		
		for (TrkPoint p : this.coordlist) {
			double x = p.x - min_x;
			double y = p.y - min_y;
			if (!importHeight) {
			    movements.print(p.convertedTime + " " + x + " " + y + " ");
			} else {
			    movements.print(p.convertedTime + " " + x + " " + y + " " + p.z + " ");
			}
		}
		
		movements.println();
		movements.close();
		if (this.compress) {
			System.out.println("File [" + this.fileName + ".movements.gz] created.");
		} else {
			System.out.println("File [" + this.fileName + ".movements] created.");
		}
	}
	
	private double maxFromDoubleList(ArrayList<Double> arr) {
		double result = arr.get(0);
		Iterator<Double> it = arr.iterator();
		while (it.hasNext()) {
			double n = it.next();
			if (n > result) {
				result = n;
			}
		}
		
		return result;
	}
	
	private double minFromDoubleList(ArrayList<Double> arr) {
		double result = arr.get(0);
		Iterator<Double> it = arr.iterator();
		while (it.hasNext()) {
			double n = it.next();
			if (n < result) {
				result = n;
			}
		}
		
		return result;
	}
	
	private void createParams() {
		double max_x = 
			this.maxFromDoubleList(this.bounds.get("max_x")) 
			- this.minFromDoubleList(this.bounds.get("min_x"));
		double max_y = 
			this.maxFromDoubleList(this.bounds.get("max_y"))
			- this.minFromDoubleList(this.bounds.get("min_y"));

		Date starttime = this.coordlist.get(0).time;
		Date endtime = this.coordlist.get(this.coordlist.size() - 1).time;
		
		long delta = this.calculateDateDiff(starttime, endtime);
		long seconds = delta / 1000l;
		long days = Math.round(delta / (60.0 * 60.0 * 24.0 * 1000.0));
		if (days < 0) {
			System.out.println("Timestamp of the end of simulation is earlier than of its start!");
			System.exit(0);
		}
		long duration = days * 24 * 3600 + seconds - 1;
		
		PrintWriter params = App.openPrintWriter(this.fileName + ".params");
		params.println("model=" + "GPXImport "+ getInfo().getFormattedVersion()); 
		params.println("x=" + max_x);
		params.println("y=" + max_y);
	    if (importHeight) {
	        params.println("z=" + bounds.get("max_z").get(0)); 
	    }
		params.println("duration=" + duration);
		params.println("nn=" + "1");
		params.close();
		System.out.println("File [" + this.fileName + ".params] created");
	}

	public boolean parseArg(char key, String val) {
		switch (key) {
		case 'f':
			this.fileName = val;
			return true;
		case 'c':
			this.compress = true;
			return true;
	    case 'h':
            this.importHeight = true;
            return true;
        case 'H':
            this.defaultHeight = Double.parseDouble(val);
            return true;
		}

		return super.parseArg(key, val);
	}

	public static void printHelp() {
        System.out.println(getInfo().toDetailString());
		System.out.println(getInfo().name);
		System.out.println("\t-f <filename>");
		System.out.println("\t-c <compress> (true|false)");
	    System.out.println("\t-h <import height> (true|false)");
	    System.out.println("\t-H <default height> (double)");
	}

	@Override
	public void characters(char[] ch, int start, int length)
	throws SAXException {
		if (this.inTimeContent) {
			for (int i = start; i < start + length; i++) {
				this.timestr += ch[i];
			}
        }
        else if (this.inEleContent) {
            String tmp = "";
            for (int i = start; i < start + length; i++) {
                tmp += ch[i];
            }
            
            double z;
            if (tmp.equals(invalidHeight)) {
                z = this.defaultHeight;
            } else {
                try {
                    z = Double.parseDouble(tmp);
                }
                catch (NumberFormatException e) {
                    z = this.defaultHeight;
                }
            }
            
            double maxz = bounds.get("max_z").get(0);
            if (z > maxz) bounds.get("max_z").set(0, z);
            
            this.currentTrkPoint.z = z;
        }
	}

	@Override
	public void endDocument() throws SAXException {
	}

	@Override
	public void endElement(String uri, String localName, String name)
	throws SAXException {
		if (name.equals("trkpt")) {
			this.inTrkptContent = false;
		} else if (name.equals("time")) {
			if (this.inTrkptContent) {
				this.inTimeContent = false;
				SimpleDateFormat f = new SimpleDateFormat("y-M-d'T'H:m:s'Z'");
				try {
					this.currentTrkPoint.time = f.parse(this.timestr);
				} catch (ParseException e) {
					App.exceptionHandler("Error parsing Date ", e);
				}
			}
		} else if (name.equals("ele")) {
            if (importHeight) {
                if (this.inTrkptContent) {
                    this.inEleContent = false;
                }
            }
		}
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length)
	throws SAXException {
	}

	@Override
	public void processingInstruction(String target, String data)
	throws SAXException {
	}

	@Override
	public void setDocumentLocator(Locator locator) {
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
	}

	@Override
	public void startDocument() throws SAXException {
	}

	@Override
	public void startElement(String uri, String localName, String name,
			Attributes atts) throws SAXException {
		double minlat = 0f;
		double minlon = 0f;
		double maxlat = 0f;
		double maxlon = 0f;

		if (name.equals("bounds")) {
			minlat = Double.parseDouble(atts.getValue("minlat"));
			minlon = Double.parseDouble(atts.getValue("minlon"));
			maxlat = Double.parseDouble(atts.getValue("maxlat"));
			maxlon = Double.parseDouble(atts.getValue("maxlon"));
			
			Point2D.Double dst = new Point2D.Double();
			
			Point2D.Double src = new Point2D.Double(minlon, minlat);
			this.projection.transform(src, dst);
			
			bounds.get("min_x").add(dst.x);
			bounds.get("min_y").add(dst.y);
			
			src = new Point2D.Double(maxlon, maxlat);
			this.projection.transform(src, dst);
			
			bounds.get("max_x").add(dst.x);
			bounds.get("max_y").add(dst.y);

		} else if (name.equals("trkpt")) {
			this.inTrkptContent = true;
			
			double lat = Double.parseDouble(atts.getValue("lat"));
			double lon = Float.parseFloat(atts.getValue("lon"));
			
			Point2D.Double src = new Point2D.Double(lon, lat);
			Point2D.Double dst = new Point2D.Double();
			
			this.projection.transform(src, dst);
			
			this.currentTrkPoint = new TrkPoint(dst.x, dst.y);
			
			this.coordlist.add(this.currentTrkPoint);
		} else if (name.equals("time")) {
			if (this.inTrkptContent) {
				this.inTimeContent = true;
				this.timestr = "";
			}
		} else if (name.equals("ele")) {
            if (importHeight) {
                if (this.inTrkptContent) {
                    this.inEleContent = true;
                }
            }
        }
	}

	@Override
	public void startPrefixMapping(String prefix, String uri)
	throws SAXException {
	}
}
