package com.andybotting.tramhunter.activity;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

public class PreferencesActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setPreferenceScreen(createPreferenceHierarchy());
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
        
        PreferenceCategory inlinePrefCat = new PreferenceCategory(this);
        inlinePrefCat.setTitle("Preferences");
        root.addPreference(inlinePrefCat);
        
        CheckBoxPreference displayWelcomeMessage = new CheckBoxPreference(this);
        displayWelcomeMessage.setKey("displayWelcomeMessage");
        displayWelcomeMessage.setTitle("Welcome Message");
        displayWelcomeMessage.setSummary("Display the welcome message on the home page");
        inlinePrefCat.addPreference(displayWelcomeMessage);
                
        CheckBoxPreference goToFavouriteOnLaunch = new CheckBoxPreference(this);
        goToFavouriteOnLaunch.setKey("goToFavouriteOnLaunch");
        goToFavouriteOnLaunch.setTitle("Favourite On Launch");
        goToFavouriteOnLaunch.setSummary("Go straight to your favourite stop upon launching the application");
        inlinePrefCat.addPreference(goToFavouriteOnLaunch);
        
        return root;
    }
}
