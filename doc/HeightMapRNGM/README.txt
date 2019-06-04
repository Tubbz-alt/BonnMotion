# Copyright (c) 2018--2019 Perspecta Labs Inc.
#
# This software was developed with government funds, and the government retains
# rights to use this software.
#
# LICENSED MATERIAL - PROPERTY OF PERSPECTA LABS INC.
# Possession and/or use of this material is subject to the provisions of a
# written license agreement with Perspecta Labs Inc.
#

This file describes the use and implementation of the Height Map Reference Node
Group Mobility (HM-RNGM) model for the BonnMotion generator.  The HM-RNGM
model is based on the Reference Point Group Mobility Model (RPGM).  HM-RNGM
enhances RPGM by adding support for limited three-dimensional mobility by
calculating the height of the node at its new location and calculating node
speed based on the 3D Euclidean distance to that point.  In addition, HM-RNGM
allows node groups to be statically defined in a configuration file.  Finally,
HM-RNGM allows the statically defined group to follow a node as its reference
point rather than creating an abstract point that the nodes follow.

Installation
------------
The HM-RNGM is installed with the other BonnMotion classes.  However, it
requires that the gdal library be available in the standard library path or in
/usr/gdal2/lib.

Invoking HM-RNGM
----------------

HM-RNGM is invoked through the standard BonnMotion BM class with the model
name HeightMapRNGM.  Note that the model must be invoked with the '-J 3D'
command-line option.  The model takes the following parameters in addition to
those defined by the RandomSpeedBase model:

-e  The number of subsegments each following node in a group takes for each
    segment of the reference node's motion.

-g  The path to the membership group file.

-m  The multiple of the maximum speed at which the following nodes can move
    during subsegment motion.

-o  Geographic position (latitude, longitude) to use as the origin rather than
    the natural origin of the terrain map (position 0,0 in the terrain file's
    raster).  The format of the position is ISO6709 annex H.  (E.g.
    +010203.45-0060708.9 for 1* 2' 3.45" N 6* 7' 8.9" W)

-r  The maximum distance from group leader to each member

-t  Path to the terrain file that defines the height of the terrain at each
    location.  The file must be readable by the GDAL library and must define a
    spatial reference system that is convertable to WGS84.  The file must
    report height (values) in meters.  HM-RNGM will create mobility files
    whose x and y coordinates are meter offsets from the origin of the terrain
    file in the direction of the terrain file's projection.


Configuring Membership Group File
---------------------------------------

The default membership group file is "node_groups.txt". This default file
contains two sections labeled [MOBILE] and [STATIONARY]. Each line in the
[MOBILE] section is a group of nodes that move together. Each line in the
[STATIONARY] section is a node and it's latitude, longitude, and altitude. For
more information on this file, review the comments (lines prepended with #)
inside it.
