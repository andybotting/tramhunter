package com.andybotting.tramhunter.util;

import java.util.Date;

public class StringUtil {

	/**
	 * @param dateStr
	 * @return
	 */
	public static String humanFriendlyDate(long time) {
		
		// today
		Date today = new Date();
	
		// how much time since (ms)
		Long duration = today.getTime() - time;
	
		int second = 1000;
		int minute = second * 60;
		int hour = minute * 60;
		int day = hour * 24;
	
		if (duration < minute) {
			int n = (int) Math.floor(duration / second);
			return n + "s";
		}

		if (duration < hour) {
			int n = (int) Math.floor(duration / minute);
			return n + "m";
		}
	
		if (duration < day) {
			int n = (int) Math.floor(duration / hour);
			return n + "h";
		}

		if (duration < day * 365) {
			int n = (int) Math.floor(duration / day);
			return n + "d";
		} else {
			return ">1y";
		}
	}
	
	/**
	 * @param dateStr
	 * @return
	 */
	public static String humanFriendlyDate(Date date) {
		return humanFriendlyDate(date.getTime());
	}
	
}
