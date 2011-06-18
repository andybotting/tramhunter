/*  
 * Copyright 2010 Andy Botting <andy@andybotting.com>  
 *  
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * This file is distributed in the hope that it will be useful, but  
 * WITHOUT ANY WARRANTY; without even the implied warranty of  
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU  
 * General Public License for more details.  
 *  
 * You should have received a copy of the GNU General Public License  
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  
 *  
 * This file incorporates work covered by the following copyright and  
 * permission notice:
 * 
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andybotting.tramhunter.util;

import java.util.Date;

import com.andybotting.tramhunter.TramHunterApplication;
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
	
	private final SharedPreferences mPreferences;
	private final Context mContext;


	/**
	 * Constructor
	 */
	public PreferenceHelper() {
		mContext = TramHunterApplication.getContext();
		mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
	}
	
	
	public boolean isWelcomeQuoteEnabled() {
		return mPreferences.getBoolean(SettingsActivity.KEY_WELCOME_MESSAGE, SettingsActivity.KEY_WELCOME_MESSAGE_DEFAULT_VALUE);
	}
	
	public String defaultLaunchActivity() {
		return mPreferences.getString(SettingsActivity.KEY_DEFAULT_LAUNCH_ACTIVITY, "HomeActivity");
	}
	
	public boolean isTramImageEnabled() {
		return mPreferences.getBoolean(SettingsActivity.KEY_TRAM_IMAGE, SettingsActivity.KEY_TRAM_IMAGE_DEFAULT_VALUE);
	}	
	
//	public boolean isJSONAPIEnabled()	{
//		return mPreferences.getBoolean(SettingsActivity.KEY_USE_JSON_API, SettingsActivity.KEY_USE_JSON_API_DEFAULT_VALUE);
//	}
	
	public boolean isSendStatsEnabled()	{
		return mPreferences.getBoolean(SettingsActivity.KEY_SEND_STATS, SettingsActivity.KEY_SEND_STATS_DEFAULT_VALUE);
	}
	
    /**
     * Return a string representing the GUID
     */
	public String getGUID() {
		return mPreferences.getString(KEY_GUID, "");
	}
	
    /**
     * Set a string representing the GUID
     */	
	public void setGUID(String guid) {
		SharedPreferences.Editor editor = mPreferences.edit();	
		editor.putString(KEY_GUID, guid);
		editor.commit();
	}	

	
    /**
     * Return a boolean representing that this is the first launch for this version
     */
	public boolean isFirstLaunchThisVersion() {
		long lastVersion = mPreferences.getLong(KEY_FIRST_LAUNCH_VERSION, 0);
		
		try {
			PackageInfo pi = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
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
			PackageInfo pi = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
			SharedPreferences.Editor editor = mPreferences.edit();
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
		return mPreferences.getLong(KEY_LAST_UPDATE, 0);
	}
	
    /**
     * Set a long representing the last stats send date
     */	
	public void setLastUpdateTimestamp() {
		Date now = new Date();
		SharedPreferences.Editor editor = mPreferences.edit();	
		editor.putLong(KEY_LAST_UPDATE, now.getTime());
		editor.commit();
	}	
	
	/**
     * Return a long representing the last stats send date
     */
	public long getStatsTimestamp() {
		return mPreferences.getLong(KEY_STATS_TIMESTAMP, 0);
	}
	
    /**
     * Set a long representing the last update
     */	
	public void setStatsTimestamp() {
		Date now = new Date();
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putLong(KEY_STATS_TIMESTAMP, now.getTime());
		editor.commit();
	}	
	
	/**
     * Return a string representing the starred station/lines
     */
	public String getStarredStopsString() {
		return mPreferences.getString(KEY_STARRED_STOPS_STRING, "");
	}	
	
    /**
     * Set a string representing the starred station/lines
     */	
	public void setStarredStopsString(String stopsString) {
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putString(KEY_STARRED_STOPS_STRING, stopsString);
		editor.commit();
	}
	
}
