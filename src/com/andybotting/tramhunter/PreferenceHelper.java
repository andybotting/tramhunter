package com.andybotting.tramhunter;

import android.content.SharedPreferences;

public class PreferenceHelper {
	private static final String FAV_ON_LAUNCH = "goToFavouriteOnLaunch";
	private static final String WELCOME_QUOTE = "displayWelcomeMessage";
	
	private final SharedPreferences preferences;

	public PreferenceHelper(SharedPreferences preferences) {
		this.preferences = preferences;
	}
	
	public boolean isWelcomeQuoteEnabled()
	{
		return preferences.getBoolean(WELCOME_QUOTE, false);
	}
	
	public boolean isFavouriteOnLaunchEnabled()
	{
		return preferences.getBoolean(FAV_ON_LAUNCH, false);
	}
}
