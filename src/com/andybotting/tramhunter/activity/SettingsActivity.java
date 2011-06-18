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

package com.andybotting.tramhunter.activity;

import com.andybotting.tramhunter.R;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static final String KEY_DEFAULT_LAUNCH_ACTIVITY = "defaultLaunchActivity";
	
	public static final String KEY_WELCOME_MESSAGE = "displayWelcomeMessage";
	public static final boolean KEY_WELCOME_MESSAGE_DEFAULT_VALUE = true;
	
	public static final String KEY_TRAM_IMAGE = "displayTramImage";
	public static final boolean KEY_TRAM_IMAGE_DEFAULT_VALUE = true;
	
//	public static final String KEY_USE_JSON_API = "useJSONAPI";
//	public static final boolean KEY_USE_JSON_API_DEFAULT_VALUE = false;
	
	public static final String KEY_SEND_STATS = "sendUsageStats";
	public static final boolean KEY_SEND_STATS_DEFAULT_VALUE = false;
	
	private ListPreference mDefaultLaunchActivity;
	private CheckBoxPreference mDisplayWelcomeMessage;
	private CheckBoxPreference mDisplayTramImage;
	private CheckBoxPreference mUseJSONAPI;
	private CheckBoxPreference mSendStats;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.layout.settings);
        
        // Get a reference to the preferences
        mDefaultLaunchActivity = (ListPreference)getPreferenceScreen().findPreference(KEY_DEFAULT_LAUNCH_ACTIVITY);
        mDisplayWelcomeMessage = (CheckBoxPreference)getPreferenceScreen().findPreference(KEY_WELCOME_MESSAGE);
        mDisplayTramImage = (CheckBoxPreference)getPreferenceScreen().findPreference(KEY_TRAM_IMAGE);
//        mUseJSONAPI = (CheckBoxPreference)getPreferenceScreen().findPreference(KEY_USE_JSON_API);
        mSendStats = (CheckBoxPreference)getPreferenceScreen().findPreference(KEY_SEND_STATS);

    }
    
    @Override
    protected void onResume() {
        super.onResume();

        // Setup the initial values
        setPreferenceSummary(getPreferenceScreen().getSharedPreferences(), KEY_DEFAULT_LAUNCH_ACTIVITY);
        setPreferenceSummary(getPreferenceScreen().getSharedPreferences(), KEY_WELCOME_MESSAGE);
        setPreferenceSummary(getPreferenceScreen().getSharedPreferences(), KEY_TRAM_IMAGE);
//        setPreferenceSummary(getPreferenceScreen().getSharedPreferences(), KEY_USE_JSON_API);
        setPreferenceSummary(getPreferenceScreen().getSharedPreferences(), KEY_SEND_STATS);
        
        // Set up a listener whenever a key changes            
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister the listener whenever a key changes            
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);    
    }
      
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		setPreferenceSummary(sharedPreferences, key);
	}
    
    private void setPreferenceSummary(SharedPreferences sharedPreferences, String key){
		if(key.equals(KEY_DEFAULT_LAUNCH_ACTIVITY)){
			mDefaultLaunchActivity.setSummary("Open " + getFreindlyDefaultActivityName(sharedPreferences) + " on launch");
		}
		else if(key.equals(KEY_WELCOME_MESSAGE)){
			mDisplayWelcomeMessage.setSummary(sharedPreferences.getBoolean(key, KEY_WELCOME_MESSAGE_DEFAULT_VALUE) ? 
					"Showing welcome messages" : "Hiding welcome messages");
		}
		else if(key.equals(KEY_TRAM_IMAGE)){
			mDisplayTramImage.setSummary(sharedPreferences.getBoolean(key, KEY_TRAM_IMAGE_DEFAULT_VALUE) ? 
					"Showing tram images" : "Hiding tram images");			
		}
//		else if(key.equals(KEY_USE_JSON_API)){
//			mUseJSONAPI.setSummary(sharedPreferences.getBoolean(key, KEY_USE_JSON_API_DEFAULT_VALUE) ? 
//					"Using experimental JSON API" : "Using stable SOAP API");
//		}
		else if(key.equals(KEY_SEND_STATS)){
			mSendStats.setSummary(sharedPreferences.getBoolean(key, KEY_SEND_STATS_DEFAULT_VALUE) ? 
					"Sending anonymous usage statistics" : "Not send anonymous usage statistics");
		}   
    }
    
    private String getFreindlyDefaultActivityName(SharedPreferences sharedPreferences){
    	// Because the default activity setting is saved as an activity name we need to get the friendly name to show the user
    	// in the summary of the setting
		String[] defaultActivityEntries = getResources().getStringArray(R.array.defaultActivityEntries);
		String[] defaultActivityEntryValues = getResources().getStringArray(R.array.defaultActivityEntryValues);
		String currentDefaultActivityValue = sharedPreferences.getString(KEY_DEFAULT_LAUNCH_ACTIVITY, "HomeActivity");
    	
		return defaultActivityEntries[findStringItemIndex(defaultActivityEntryValues, currentDefaultActivityValue)];
    }
    
    private int findStringItemIndex(String[] array, String item){
    	
    	for(int i = 0; i < array.length; i++){
    		if(array[i].equals(item))
    			return i;
    	}
    	
    	return -1;
    }

	
}