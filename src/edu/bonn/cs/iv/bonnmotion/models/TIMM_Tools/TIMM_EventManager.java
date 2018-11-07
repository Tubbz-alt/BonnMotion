package edu.bonn.cs.iv.bonnmotion.models.TIMM_Tools;

import java.util.Vector;

public class TIMM_EventManager {
    private final Vector<Double> eventTimes;
    private double lastTime;

    public TIMM_EventManager() {
        this.eventTimes = new Vector<Double>(0, 1);
        this.lastTime = -1;
    }

    public boolean isFinished() {
        return (eventTimes.size() > 0) ? false : true;
    }

    /**
     * Add new event
     * 
     * @param _time
     *            Add this time a new event time
     */
    public void addEvent(final double _time) {
        if (_time != Double.MAX_VALUE && !eventTimes.contains(_time) && this.lastTime < _time) {
            this.eventTimes.add(_time);
        }
    }

    /**
     * Adds new event times. The extra two events +-0.001 were implemented because of potential
     * rounding errors. Shouldn't have any effect since java's rounding errors are small enough.
     * Test this...
     * 
     * @param _times
     *            Vector with event times to add
     */
    public void addEvents(final Vector<Double> _times) {
        for (double time : _times) {
            this.addEvent(time);
        }
    }

    /**
     * Get the time of the next event
     * 
     * @return Time of next event
     */
    public double getNextEvent() {
        double ret = Double.MAX_VALUE;

        for (double time : eventTimes) {
            if (ret > time) {
                ret = time;
            }
        }

        eventTimes.remove(ret);
        this.lastTime = ret;
        
        if (ret == Double.MAX_VALUE) {
            throw new RuntimeException("Error: TIMM_EventManager, getNextEvent, ret should not be Double.MAX_VALUE.");
        }
        
        return ret;
    }

    /**
     * Nice representation of the EventManager for debugging purpose
     * 
     * @return A string containing all scheduled event times
     */
    public String toString() {
        StringBuilder sb = new StringBuilder("#EventManager: ");

        for (double time : eventTimes) {
            sb.append(time + " ");
        }

        return sb.toString();
    }
}
