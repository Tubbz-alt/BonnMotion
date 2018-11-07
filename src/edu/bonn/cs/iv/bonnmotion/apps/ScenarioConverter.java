/*******************************************************************************
 ** BonnMotion - a mobility scenario generation and analysis tool             **
 ** ScenarioConverter                                                         **
 **                                                                           **
 ** Copyright (C) 2011 University of Bonn                                     **
 ** Institute of Computer Science 4                                           **
 ** Communication and Distributed Systems                                     **
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

package edu.bonn.cs.iv.bonnmotion.apps;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import edu.bonn.cs.iv.bonnmotion.*;

public class ScenarioConverter extends App {
    private static ModuleInfo info;
    
    static {
        info = new ModuleInfo("ScenarioConverter");
        info.description = "Application that converts scenario types";
        
        info.major = 1;
        info.minor = 0;
        info.revision = ModuleInfo.getSVNRevisionStringValue("$LastChangedRevision: 248 $");
        
        info.contacts.add(ModuleInfo.BM_MAILINGLIST);
        info.authors.add("Florian Schmitt");
		info.affiliation = ModuleInfo.UNIVERSITY_OF_BONN;
    }
    
    public static ModuleInfo getInfo() {
        return info;
    }
    
    enum Mode { convert3Dto2D, convert2Dto3D };
    
    private String sourcename = "";
    private String targetname = "";
    private Mode mode = Mode.convert3Dto2D;
    private double z = 0;

    public ScenarioConverter(String[] args) {
        go(args);
    }

    public void go(String[] args) {
        parse(args);

        if(sourcename.equals("")) {
            printHelp();
            System.err.println("\nSource scenario missing");
            System.exit(0);
        }
        
        switch (mode) {
            case convert3Dto2D:
                Convert3Dto2D();
                break;
            case convert2Dto3D:
                Convert2Dto3D();
                break;
        }
    }
    
    private void Convert2Dto3D() {
        Scenario source = null;
        
        try {
            source = Scenario.getScenario(sourcename);
        } catch (FileNotFoundException e) {
            App.exceptionHandler("source scenario was not found", e);
        } catch (IOException e) {
            App.exceptionHandler("error reading source scenario", e);
        }
        
        if (source instanceof Scenario3D) {
            System.err.println("source scenario is already a 3D scenario");
            System.exit(-1);
        }
        
        Scenario3D target = Scenario3D.convertFrom2DScenario(source, z);
        if (targetname.isEmpty()) {
            targetname = sourcename + "_from2D";
        }
        
        try {
            target.write(targetname, parseParams(sourcename));
        } catch (FileNotFoundException e) {
            App.exceptionHandler("source scenario was not found", e);
        } catch (IOException e) {
            App.exceptionHandler("error writing scenario", e);
        }
    }

    private void Convert3Dto2D() {
        Scenario source = null;
        
        try {
            source = Scenario.getScenario(sourcename);
        } catch (FileNotFoundException e) {
            App.exceptionHandler("source scenario was not found", e);
        } catch (IOException e) {
            App.exceptionHandler("error reading source scenario", e);
        }
        
        if (!(source instanceof Scenario3D)) {
            System.err.println("source scenario is no 3D scenario");
            System.exit(-1);
        }
        
        Scenario target = Scenario3D.convertTo2DScenario(source);
        if (targetname.isEmpty()) {
            targetname = sourcename + "_to2D";
        }
        
        try {
            target.write(targetname, parseParams(sourcename));
        } catch (FileNotFoundException e) {
            App.exceptionHandler("source scenario was not found", e);
        } catch (IOException e) {
            App.exceptionHandler("error writing scenario", e);
        }
    }
    
    private String[] parseParams(String basename) throws IOException {
        List<String> result = new ArrayList<String>();
        String line;
        BufferedReader in = new BufferedReader( new InputStreamReader( new FileInputStream( basename + ".params" ) ) );
        while ( (line=in.readLine()) != null ) {
            StringTokenizer st = new StringTokenizer(line, "=");
            String key = st.nextToken();
            String value = st.nextToken();
            String[] standardparams = { "model", "ignore", "randomSeed", "x", "y", "z", "duration", "nn", "nBuildings", "circular", "aFieldParams" };
            
            if (!Arrays.asList(standardparams).contains(key)) {
                result.add(key+"="+value);
            }
        }
        in.close();
        
        return result.toArray(new String[]{});
    }

    protected boolean parseArg(char key, String val) {
        switch (key) {
            case 'f':
                sourcename = val;
                return true;
            case 'g':
                targetname = val;
                return true;
            case 'm':
                if (val.equals("a")) {
                    mode = Mode.convert3Dto2D;
                } else if (val.equals("b")) {
                    mode = Mode.convert2Dto3D;
                } else {
                    System.err.println("Mode '" + val + "' does not exist.");
                    System.exit(-1);
                }
                return true;
            case 'z':
                z = Double.parseDouble(val);
                return true;
            default:
                return super.parseArg(key, val);
        }
    }

    public static void printHelp() {
        System.out.println(getInfo().toDetailString());
        App.printHelp();
        System.out.println(getInfo().name);
        System.out.println("\t-f <scenario> source scenario name");
        System.out.println("\t-g <scenario> target scenario name (default: 'sourcename_to2D' / 'sourcename_from2D')");
        System.out.println("\t-m <mode> a or b");
        System.out.println("\t\ta: convert 3D scenario to 2D scenario");
        System.out.println("\t\tb: convert 2D scenario to 3D scenario");
        System.out.println("\t-z <double> z value which will be used when converting 2D to 3D (default: 0)");
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        new ScenarioConverter(args);
    }
}