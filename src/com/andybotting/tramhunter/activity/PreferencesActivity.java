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
        displayWelcomeMessage.setTitle("Welcome Quote");
        displayWelcomeMessage.setSummary("Display a tram quote in the Tram Hunter menu");
        //displayWelcomeMessage.setDefaultValue(True); // String cast exception
        inlinePrefCat.addPreference(displayWelcomeMessage);
                
        CheckBoxPreference goToFavouriteOnLaunch = new CheckBoxPreference(this);
        goToFavouriteOnLaunch.setKey("goToFavouriteOnLaunch");
        goToFavouriteOnLaunch.setTitle("Favourites On Launch");
        goToFavouriteOnLaunch.setSummary("Go directly to your favourite stops on launch");
        //goToFavouriteOnLaunch.setDefaultValue(False); // String cast exception
        inlinePrefCat.addPreference(goToFavouriteOnLaunch);
        
        return root;
    }
}
