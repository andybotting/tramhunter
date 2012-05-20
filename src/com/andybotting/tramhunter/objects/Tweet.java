package com.andybotting.tramhunter.objects;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Tweet {
	
	private String username;
	private String name;
	private String message;
	private String imageUrl;
	private String imagePath;
	private Date date;
	
	
	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}
	
	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}
	
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}
	
	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}
	
	/**
	 * @param imageUrl the imageUrl to set
	 */
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
	
	/**
	 * @return the imageUrl
	 */
	public String getImageUrl() {
		return imageUrl;
	}	

	/**
	 * @return the imagePath
	 */
	public String getImagePath() {
		return imagePath;
	}

	/**
	 * @param imagePath the imagePath to set
	 */
	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}
	
	/**
	 * @param date the date to set
	 */
	public void setDate(String dateString) {
		
		// parse Twitter date
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZZ yyyy", Locale.ENGLISH);
		dateFormat.setLenient(false);
		
		try {
			this.date = dateFormat.parse(dateString);
		} catch (Exception e) {
			// Nothing
		}
	}
	
	
	/**
	 * @param date the date to set
	 */
	public void setDate(long time) {
		this.date = new Date(time);
	}
	
	
	/**
	 * @param date the date to set
	 */
	public void setDate(Date date) {
		this.date = date;
	}
	

	/**
	 * @return the date
	 */
	public long getDateLong() {
		return date.getTime();
	}	

	
	/**
	 * @param dateStr
	 * @return
	 */
	public String twitterHumanFriendlyDate() {
		
		// today
		Date today = new Date();
	
		// how much time since (ms)
		Long duration = today.getTime() - date.getTime();
	
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

}
