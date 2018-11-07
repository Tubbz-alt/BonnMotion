#!/usr/bin/env python

###############################################################################
## BonnMotion - a mobility scenario generation and analysis tool             ##
## Copyright (C) 2002, 2003 University of Bonn                               ##
##                                                                           ##
## This program is free software; you can redistribute it and/or modify      ##
## it under the terms of the GNU General Public License as published by      ##
## the Free Software Foundation; either version 2 of the License, or         ##
## (at your option) any later version.                                       ##
##                                                                           ##
## This program is distributed in the hope that it will be useful,           ##
## but WITHOUT ANY WARRANTY; without even the implied warranty of            ##
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             ##
## GNU General Public License for more details.                              ##
##                                                                           ##
## You should have received a copy of the GNU General Public License         ##
## along with this program; if not, write to the Free Software               ##
## Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA ##
###############################################################################

from xml.sax import saxutils
from xml.sax import ContentHandler
from xml.sax import make_parser
from xml.sax.handler import feature_namespaces
from optparse import OptionParser
from gzip import GzipFile
import sys
import string

def normalize_whitespace(text):
    "Remove redundant whitespace from a string"
    return ' '.join(text.split())

#
# The class that merges all information and writes out the BonnMotion files
#

class GatherInfo:
    DefaultMobilityModel = "RandomWaypoint"

    def __init__(self, verbose):
        self.verbose = verbose
        self.currentNodePos = {}
        self.motion = {}          # For each node, a list of (startTime,
                                  # startPos, endTime, endPos) tuples
        self.parameters = {}      # Dictionary of parameters to write into the params file

    def setIgnore(self, ignore):
        self.parameters["ignore"] = ignore

    # Initial position of a node
    def nodeInitialPos(self, nodeId, posTuple):
        posTuple = tuple(map(float, posTuple))
        nodeId = int(nodeId)
        self.currentNodePos[nodeId] = posTuple
        self.motion[nodeId] = []

    # New movement of a node
    def nextPos(self, nodeId, posTuple, startTime, endTime):
        posTuple = tuple(map(float, posTuple))
        startTime = float(startTime)
        endTime = float(endTime)
        nodeId = int(nodeId)
        self.motion[nodeId].append((startTime, self.currentNodePos[nodeId], endTime, posTuple))
        self.currentNodePos[int(nodeId)] = posTuple

    def stopTime(self, duration):
        self.parameters["duration"] = duration

    def setParameters(self, model, minPos, maxPos, noNodes):
        self.parameters["model"] = model
        if minPos:
            self.parameters["minX"] = float(minPos[0])
            self.parameters["minY"] = float(minPos[1])
            self.parameters["x"] = float(maxPos[0])
            self.parameters["y"] = float(maxPos[1])
        if noNodes:
            self.parameters["nn"] = noNodes
        
    # Write out the files for BonnMotion
    def write(self, fdMovements, fdParams):
        nodes = self.motion.keys()
        nodes.sort()
        if not self.parameters.has_key("nn"):
            nn = len(nodes)
            self.parameters["nn"] = nn
            if self.verbose: print >> sys.stderr, "Warning: Number of nodes not given, using %d!" % (nn,)
        nn = self.parameters["nn"]
        maxTime = 0.0
        # Check whether we have enough nodes
        if nn != len(nodes):
            sys.exit("Too few nodes: Number of nodes set to %d, but only %d nodes were given!" % (nn, len(nodes)))
        # Check whether the node ids are consecutive and begin with 0
        for i in xrange(nn):
            if i != nodes[i]:
                sys.exit("Consecutive Node IDs needed. (first node is 0)")
        minX = 0.0
        maxX = 0.0
        minY = 0.0
        maxY = 0.0
        if self.parameters.has_key("minX"):
            minX = self.parameters["minX"]
            minY = self.parameters["minY"]
            maxX = self.parameters["x"]
            maxY = self.parameters["y"]
        else:
            # We need to find out the coordinate range for ourselves
            for i in xrange(nn):
                pList = self.motion[i]
                lastEndTime = -1.0
                for startTime, startPos, endTime, endPos in pList:
                    minX = min(minX, startPos[0])
                    maxX = max(maxX, startPos[0])
                    minY = min(minY, startPos[1]) 
                    maxY = max(maxY, startPos[1]) 
                    minX = min(minX, endPos[0])
                    maxX = max(maxX, endPos[0])
                    minY = min(minY, endPos[1]) 
                    maxY = max(maxY, endPos[1]) 
            if self.verbose: print >> sys.stderr, "Warning: max and min coordinates not found, will use coordinates from waypoints: X:%f-%f, Y:%f-%f!" % (minX, maxX, minY, maxY)
        if self.verbose:
            if minX < 0.0:
                print >> sys.stderr, "Warning: min x coordinate < 0, will shift origin to 0!"
            if minY < 0.0:
                print >> sys.stderr, "Warning: min y coordinate < 0, will shift origin to 0!"
        self.parameters["minX"] = 0.0
        self.parameters["minY"] = 0.0
        self.parameters["x"] = maxX-minX
        self.parameters["y"] = maxY-minY
        # Write movements
        for i in xrange(nn):
            pList = self.motion[i]
            lastEndTime = -1.0
            for startTime, startPos, endTime, endPos in pList:
                if startTime > lastEndTime:
                    # we need a pause
                    fdMovements.write("%f %f %f " % (startTime, startPos[0]-minX, startPos[1]-minY))
                # write motion
                fdMovements.write("%f %f %f " % (endTime, endPos[0]-minX, endPos[1]-minY))
                maxTime = max(maxTime, endTime)
                
            fdMovements.write("\n")
        # If no duration was given, we take the computed one
        if not self.parameters.has_key("duration"):
            if self.verbose: print >> sys.stderr, "Warning: 'stop_time' element not found, will use max end time!"
            self.parameters["duration"] = maxTime
        # If no model was given, simply take the default
        if  not self.parameters.has_key("model") or not self.parameters["model"]:
            self.parameters["model"] = self.DefaultMobilityModel
            if self.verbose: print >> sys.stderr, "Warning: 'mobility_model' element not found, will use '" + self.DefaultMobilityModel + "'"
        # And write the parameter file
        self.__writeParameters(fdParams)

    def __writeParameters(self, fdParams):
        for key in self.parameters.keys():
            fdParams.write("%s=%s\n" % (key, str(self.parameters[key])))

#
# This class stores the contents of all elements below a given hierarchy of elements
#

class ElementAndSub:

    def __init__(self, hierarchy):
        self.elements = {}
        self.__hierarchy = hierarchy

    def getHierarchy(self):
        return self.__hierarchy
    
    def addElement(self, name, value):
        self.elements[name] = value

    def beginElement(self):
        self.elements = {}

    def endElement(self):
        pass

# These are the classes that transform the information from the XML
# file into a form digestible to GatherInfo

class PositionChange(ElementAndSub):
    PositionChangeHierarchy = (u'simulation', u'mobility', u'position_change')

    def __init__(self, gatherInfo):
        ElementAndSub.__init__(self, self.PositionChangeHierarchy)
        self.__gatherInfo = gatherInfo

    def endElement(self):
        ElementAndSub.endElement(self)
        self.__gatherInfo.nextPos(self.elements[(u'node_id',)],
                                (self.elements[(u'destination', u'xpos')],self.elements[(u'destination', u'ypos')]),
                                self.elements[(u'start_time',)],
                                self.elements[(u'end_time',)])


class Node(ElementAndSub):
    NodeHierarchy = (u'simulation', u'node_settings', u'node')

    def __init__(self, gatherInfo):
        ElementAndSub.__init__(self, self.NodeHierarchy)
        self.__gatherInfo = gatherInfo

    def endElement(self):
        ElementAndSub.endElement(self)
        self.__gatherInfo.nodeInitialPos(int(self.elements[(u'node_id',)]),
                                (float(self.elements[(u'position', u'xpos')]), float(self.elements[(u'position', u'ypos')])))


class Statistics(ElementAndSub):
    NodeHierarchy = (u'simulation', u'statistics')

    def __init__(self, gatherInfo):
        ElementAndSub.__init__(self, self.NodeHierarchy)
        self.__gatherInfo = gatherInfo

    def endElement(self):
        ElementAndSub.endElement(self)
        if self.elements.has_key((u'stop_time',)):
            self.__gatherInfo.stopTime(float(self.elements[(u'stop_time',)]))
            
class Parameters(ElementAndSub):
    NodeHierarchy = (u'simulation', u'parameter')

    def __init__(self, gatherInfo):
        ElementAndSub.__init__(self, self.NodeHierarchy)
        self.__gatherInfo = gatherInfo

    def endElement(self):
        ElementAndSub.endElement(self)
        if self.elements.has_key((u'xmin',)):
            theMinPos = (float(self.elements[(u'xmin',)]), float(self.elements[(u'ymin',)]))
            theMaxPos = (float(self.elements[(u'xmax',)]), float(self.elements[(u'ymax',)]))
        else:
            theMaxPos = theMinPos = None
        if self.elements.has_key((u'mobility_model',)):
            theModel = self.elements[(u'mobility_model',)]
        else:
            theModel = None
        if self.elements.has_key((u'numberOfNodes',)):
            theNoNodes = int(self.elements[(u'numberOfNodes',)])
        else:
            theNoNodes = None
        self.__gatherInfo.setParameters(model = theModel, minPos = theMinPos, maxPos = theMaxPos, noNodes = theNoNodes)

# The XML parsing class

class ElementHandler(ContentHandler):

    def __init__(self):
        self.__elementTypes = {}
        self.elementNesting = []
        self.currentlyHandlingKnown = 0

    def addHandler(self, handler):
        self.__elementTypes[handler.getHierarchy()] = handler

    def startElement(self, name, attrs):
        self.elementNesting.append(name)
        self.currentContent = ""
        ten = tuple(self.elementNesting)
        if self.__elementTypes.has_key(ten):
            assert not self.currentlyHandlingKnown
            self.currentlyHandlingKnown = 1
            self.currentHandler = self.__elementTypes[ten]
            self.currentHandler.beginElement()

    def characters(self, ch):
        self.currentContent =  self.currentContent + ch

    def endElement(self, name):
        assert self.elementNesting[-1] == name
        ten = tuple(self.elementNesting)
        if self.currentlyHandlingKnown and self.__elementTypes.has_key(ten):
            self.currentlyHandlingKnown = 0
            self.currentHandler.endElement()
            self.currentHandler = None
        if self.currentlyHandlingKnown:
            self.currentHandler.addElement(tuple(self.elementNesting[len(self.currentHandler.getHierarchy()):]), self.currentContent)
        del self.elementNesting[-1]
        self.currentContent = ""
   

if __name__ == '__main__':
    oParser = OptionParser(usage="%prog [options] infile", version="%prog 0.1")
    oParser.add_option("-m", "--movements-file", help="write movements to FILE (gzipped)", metavar="FILE")
    oParser.add_option("-p", "--params-file", help="write params to FILE", metavar="FILE")
    oParser.add_option("-i", "--ignore", type="float", default=0.0, help="Ignore the first T seconds", metavar="T")
    oParser.add_option("-v", "--verbose", action="store_true", dest="verbose", default=False)   
    
    (options, args) = oParser.parse_args()

    if len(args) != 1:
        sys.exit("Programs takes exactly one argument, the XML file in SPP mobility format")

    # Strip off last . suffix
    baseName = string.join(string.split(args[0],".")[:-1], ".")

    if not options.movements_file:
        options.movements_file = baseName + ".movements.gz"
    if not options.params_file:
        options.params_file = baseName + ".params"

    # Create an XML parser
    parser = make_parser()

    # Create the Gatheter
    gather = GatherInfo(verbose = options.verbose)

    # Set command line parameters
    gather.setIgnore(options.ignore)

    # Create the handler
    dh = ElementHandler()

    # Add the elements we want to collect
    dh.addHandler(PositionChange(gather))
    dh.addHandler(Node(gather))
    dh.addHandler(Statistics(gather))
    dh.addHandler(Parameters(gather))

    # Tell the parser to use our handler
    parser.setContentHandler(dh)

    # Parse the input
    parser.parse(args[0])

    # Write the files
    fdMovements = GzipFile(options.movements_file, "w+")
    fdParams = open(options.params_file, "w+")
    gather.write(fdMovements, fdParams)
    fdMovements.close()
    fdParams.close()

