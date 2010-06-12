package com.andybotting.tramhunter.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferenceHelper {
	private static final String FAV_ON_LAUNCH = "goToFavouriteOnLaunch";
	private static final String WELCOME_QUOTE = "displayWelcomeMessage";
	
	private final SharedPreferences preferences;

	public PreferenceHelper(final Context context) {
		this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
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
