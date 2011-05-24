package com.andybotting.tramhunter.util;

import java.util.Date;

import com.andybotting.tramhunter.TramHunter;
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

	// TODO: Phase this out
	public PreferenceHelper(final Context context) {
		this.context = context;
		this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
	}

	
	public PreferenceHelper() {
		this.context = TramHunter.getContext();
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
	
	public boolean isJSONAPIEnabled()	{
		return preferences.getBoolean(SettingsActivity.KEY_USE_JSON_API, SettingsActivity.KEY_USE_JSON_API_DEFAULT_VALUE);
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
	
}
