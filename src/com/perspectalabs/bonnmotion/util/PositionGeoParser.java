/*******************************************************************************
 ** ISO6709 Parser for BonnMotion                                             **
 ** Copyright (C) 2018 Perspecta Labs Inc.                                    **
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

package com.perspectalabs.bonnmotion.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.bonn.cs.iv.util.maps.PositionGeo;

/**
 * Class to parse ISO6709 Annex H strings to PositionGeo objects
 *
 * @author ygottlieb
 *
 */
public class PositionGeoParser {

    private static Pattern ISO6709_H_LATITUDE = Pattern
            .compile("(?<sign>[+-])" + "(?<deg>\\d{2})" + "((?<min>\\d{2})"
                    + "(?<sec>\\d{2})?)?" + "(\\.(?<fraction>\\d+))?");

    private static Pattern ISO6709_H_LONGITUDE = Pattern
            .compile("(?<sign>[+-])" + "(?<deg>\\d{3})" + "((?<min>\\d{2})"
                    + "(?<sec>\\d{2})?)?" + "(\\.(?<fraction>\\d+))?");

    private static Pattern ISO6709_APPENDIX_H = Pattern
            .compile("(?<latitude>[+-]\\d+(\\.\\d+)?)"
                    + "(?<longitude>[+-]\\d+(\\.\\d+)?)");

    /**
     * Return the named group of the matcher parsed as an Integer
     *
     * @param m
     *            the matcher
     * @param groupname
     *            The name of the group to return from the matcher
     * @return the parsed integer, may be null
     */
    private static Integer getPositionPart(Matcher m, String groupname) {
        Integer retval = null;

        String group = m.group(groupname);

        if (group != null) {
            retval = Integer.parseInt(group, 10);
        }

        return retval;
    }

    /**
     * @see #getPositionPart(Matcher, String)
     * @param m
     * @param groupname
     * @param max
     * @return the numeric value of the named group. Double.NaN if the value is
     *         greater than max.
     */
    private static Double getPositionPart(Matcher m, String groupname,
            int max) {
        Double retval;

        Integer intValue = getPositionPart(m, groupname);

        if (intValue == null) {
            retval = null;
        } else if (intValue <= max) {
            retval = intValue.doubleValue();
        } else {
            retval = Double.NaN;
        }

        return retval;
    }

    /**
     * Parse the part (latitude or longitude) of the position
     *
     * @param pattern
     *            The pattern with which to parse the part
     * @param part
     *            The part to parse
     * @return The value of the part in the interval [-maxdegree, maxdegree].
     *         Return NaN if cannot be parsed correctly
     */
    private static double parsePositionPart(Pattern pattern, String part,
            int maxdegree) {
        double retval = Double.NaN;

        Matcher m = pattern.matcher(part);

        if (m.matches()) {
            double scale = 1.0;
            retval = getPositionPart(m, "deg", maxdegree);

            Double min = getPositionPart(m, "min", 60);
            if (min != null) {
                retval *= 60.0;
                scale *= 60.0;
                retval += min;
            }

            Double sec = getPositionPart(m, "sec", 60);
            if (sec != null) {
                retval *= 60.0;
                scale *= 60.0;
                retval += sec;
            }

            Integer fraction = getPositionPart(m, "fraction");
            if (fraction != null) {
                retval += Double.parseDouble("0." + fraction);
            }

            boolean isnegative = m.group("sign").equals("-");
            retval = retval / scale * (isnegative ? -1.0 : 1.0);
        }

        return retval;
    }

    /**
     * Parse a ISO6709 Annex H position string into a PositionGeo instance
     * 
     * @param position
     *            The string to parse
     * @return A PositionGeo corresponding to the given string
     * @throws IllegalArgumentException
     *             if the string cannot be parsed as an ISO6709 Annex H string.
     */
    public static PositionGeo parsePositionGeo(String position) {
        PositionGeo retval = null;
        Matcher m = ISO6709_APPENDIX_H.matcher(position);

        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "Postion '" + position + "' is no parsable as ISO6709");
        } else {
            double latitude = parsePositionPart(ISO6709_H_LATITUDE,
                    m.group("latitude"), 90);

            double longitude = parsePositionPart(ISO6709_H_LONGITUDE,
                    m.group("longitude"), 180);

            if (Double.isNaN(latitude)) {
                throw new IllegalArgumentException(
                        "Postion '" + m.group("latitude")
                                + "' is not a parsable ISO6709 latitude");
            } else if (Double.isNaN(longitude)) {
                throw new IllegalArgumentException(
                        "Postion '" + m.group("longitude")
                                + "' is not a parsable ISO6709 longitude");
            } else {

                retval = new PositionGeo(longitude, latitude);
            }

            return retval;
        }
    }
}
