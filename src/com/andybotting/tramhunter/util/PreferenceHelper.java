package com.andybotting.tramhunter.util;

import java.util.ArrayList;
import java.util.Date;

import com.andybotting.tramhunter.activity.SettingsActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;

public class PreferenceHelper {
	
	private static final String KEY_GUID = "guid";
	private static final String KEY_STARRED_STOPS_STRING = "starred_stops_string";
	private static final String KEY_FIRST_LAUNCH_VERSION = "first_launch_version";
	private static final String KEY_LAST_UPDATE = "last_update";
	private static final String KEY_STATS_TIMESTAMP = "stats_timestamp";
	
	private final SharedPreferences preferences;
	private final Context context;

	public PreferenceHelper(final Context context) {
		this.context = context;
		this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	public boolean isWelcomeQuoteEnabled() {
		return preferences.getBoolean(SettingsActivity.KEY_WELCOME_MESSAGE, SettingsActivity.KEY_WELCOME_MESSAGE_DEFAULT_VALUE);
	}
	
	public String defaultLaunchActivity() {
		return preferences.getString(SettingsActivity.KEY_DEFAULT_LAUNCH_ACTIVITY, "HomeActivity");
	}
	
	public boolean isTramImageEnabled() {
		return preferences.getBoolean(SettingsActivity.KEY_TRAM_IMAGE, SettingsActivity.KEY_TRAM_IMAGE_DEFAULT_VALUE);
	}	
	
	public boolean isSendStatsEnabled()	{
		return preferences.getBoolean(SettingsActivity.KEY_SEND_STATS, SettingsActivity.KEY_SEND_STATS_DEFAULT_VALUE);
	}
	
    /**
     * Return a string representing the GUID
     */
	public String getGUID() {
		return preferences.getString(KEY_GUID, "");
	}
	
    /**
     * Set a string representing the GUID
     */	
	public void setGUID(String guid) {
		SharedPreferences.Editor editor = preferences.edit();	
		editor.putString(KEY_GUID, guid);
		editor.commit();
	}	
	
	
	
	
    /**
     * Return a boolean representing that this is the first launch for this version
     */
	public boolean isFirstLaunchThisVersion() {
		long lastVersion = preferences.getLong(KEY_FIRST_LAUNCH_VERSION, 0);
		
		try {
			PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			if (lastVersion < pi.versionCode) {
				return true;
			}
		} catch (NameNotFoundException e) {
			// Nothing
		}
		
		return false;
	}
	
    /**
     * Set a long signalling the lastest version of the application launched
     */	
	public void setFirstLaunchThisVersion() {
		
		try {
			PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			SharedPreferences.Editor editor = preferences.edit();
			editor.putLong(KEY_FIRST_LAUNCH_VERSION, pi.versionCode);
			editor.commit();
		} catch (NameNotFoundException e) {
			// Nothing
		}
	}	

    /**
     * Return a long representing the last update
     */
	public long getLastUpdateTimestamp() {
		return preferences.getLong(KEY_LAST_UPDATE, 0);
	}
	
    /**
     * Set a long representing the last stats send date
     */	
	public void setLastUpdateTimestamp() {
		Date now = new Date();
		SharedPreferences.Editor editor = preferences.edit();	
		editor.putLong(KEY_LAST_UPDATE, now.getTime());
		editor.commit();
	}	
	
	/**
     * Return a long representing the last stats send date
     */
	public long getStatsTimestamp() {
		return preferences.getLong(KEY_STATS_TIMESTAMP, 0);
	}
	
    /**
     * Set a long representing the last update
     */	
	public void setStatsTimestamp() {
		Date now = new Date();
		SharedPreferences.Editor editor = preferences.edit();
		editor.putLong(KEY_STATS_TIMESTAMP, now.getTime());
		editor.commit();
	}	

	/**
     * Return a string representing the starred station/lines
     */
	public String getStarredStopsString() {
		return preferences.getString(KEY_STARRED_STOPS_STRING, "");
	}	
	
    /**
     * Set a string representing the starred station/lines
     */	
	public void setStarredStopsString(String stopsString) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(KEY_STARRED_STOPS_STRING, stopsString);
		editor.commit();
	}
	
    /**
     * Set a long representing the last update
     */		
	public boolean isStarred(int tramTrackerID) {
		String starredStopsString = getStarredStopsString();
		
		if (!starredStopsString.equalsIgnoreCase("")) {
			ArrayList<Integer> list = StringUtil.parseString(starredStopsString);
			for (int item : list) {
				if (item == tramTrackerID)
					return true;
			}
		}
		return false;
	}
	
    /**
     * Append the new station to the end of the station string
     * and set it in the preferences.
     */	
	public void starStop(int tramTrackerID) {
		if (!isStarred(tramTrackerID)) {
			//if (LOGV) Log.v(TAG, "Starring Stop: " + String.valueOf(stationId) + ":" + String.valueOf(lineId));
			//String givenItem = String.format("%s:%s", stationId, lineId);
			ArrayList<Integer> list = StringUtil.parseString(getStarredStopsString());
			list.add(tramTrackerID);
			setStarredStopsString(StringUtil.makeString(list));
		}
	}
	
    /**
     * Generate a new station string, removing the given station/line
     * and set it in the preferences.
     */	
	public void unstarStop(int tramTrackerID) {
		if (isStarred(tramTrackerID)) {
			//String givenItem = String.format("%s:%s", stationId, lineId);
			//if (LOGV) Log.v(TAG, "Unstarring Stop: " + givenItem);
			String starredStopsString = getStarredStopsString();
			ArrayList<Integer> list = StringUtil.parseString(starredStopsString);
			
			for (int i=0; i < list.size(); i++) {
				if (list.get(i) == tramTrackerID)
					list.remove(i);
			}
			setStarredStopsString(StringUtil.makeString(list));
		}
	
	}
	
	public void setStopStar(int tramTrackerID, boolean star) {
		if(star)
			starStop(tramTrackerID);
		else
			unstarStop(tramTrackerID);
	}
	
}
