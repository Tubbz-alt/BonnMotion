/*
 * Created on Dec 9, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package edu.bonn.cs.iv.util;

import java.util.Date;

/**
 * @author peschlow
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class InfoTimer {
	Date startTime_;
	Date stopTime_;
	Date lastStopTime_;
	double elapsedTime_;
	double elapsedTimeTotal_;

	public InfoTimer() {
		startTime_ = new Date();
		stopTime_ = null;
		lastStopTime_ = startTime_;
		elapsedTime_ = 0;
		elapsedTimeTotal_ = 0;
	}

	public void showInterrupt(String text) {
		stopTime_ = new Date();
		elapsedTimeTotal_ = (stopTime_.getTime() - startTime_.getTime()) / 1000.;
		elapsedTime_ = (stopTime_.getTime() - lastStopTime_.getTime()) / 1000.;
		lastStopTime_ = stopTime_;
		System.out.println(text + ": " + elapsedTimeTotal_ + " (+" + elapsedTime_ + ")");
	}

}
