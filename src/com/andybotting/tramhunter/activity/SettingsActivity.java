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
	
	public static final String KEY_SEND_STATS = "sendUsageStats";
	public static final boolean KEY_SEND_STATS_DEFAULT_VALUE = false;
	
	private ListPreference mDefaultLaunchActivity;
	private CheckBoxPreference mDisplayWelcomeMessage;
	private CheckBoxPreference mDisplayTramImage;
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
        mSendStats = (CheckBoxPreference)getPreferenceScreen().findPreference(KEY_SEND_STATS);

    }
    
    @Override
    protected void onResume() {
        super.onResume();

        // Setup the initial values
        setPreferenceSummary(getPreferenceScreen().getSharedPreferences(), KEY_DEFAULT_LAUNCH_ACTIVITY);
        setPreferenceSummary(getPreferenceScreen().getSharedPreferences(), KEY_WELCOME_MESSAGE);
        setPreferenceSummary(getPreferenceScreen().getSharedPreferences(), KEY_TRAM_IMAGE);
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
			
		}else if(key.equals(KEY_WELCOME_MESSAGE)){
			mDisplayWelcomeMessage.setSummary(sharedPreferences.getBoolean(key, KEY_WELCOME_MESSAGE_DEFAULT_VALUE) ? 
					"Show welcome messages" : "Hide welcome messages");
			
		}else if(key.equals(KEY_TRAM_IMAGE)){
			mDisplayTramImage.setSummary(sharedPreferences.getBoolean(key, KEY_TRAM_IMAGE_DEFAULT_VALUE) ? 
					"Show tram images" : "Hide tram images");			
		    	
		}else if(key.equals(KEY_SEND_STATS)){
			mSendStats.setSummary(sharedPreferences.getBoolean(key, KEY_SEND_STATS_DEFAULT_VALUE) ? 
					"Send anonymous usage statistics" : "Don't send anonymous usage statistics");
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