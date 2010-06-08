package com.andybotting.tramhunter.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

import com.andybotting.tramhunter.Preferences;
import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.dao.TramHunterDB;

public class PreferencesActivity extends Activity {

	private TramHunterDB tramHunterDB;
	private Preferences preferences;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		tramHunterDB = new TramHunterDB(this);
		preferences = tramHunterDB.getPreferences();

		setContentView(R.layout.preferences);

		final CheckBox displayWelcomeMessage = (CheckBox) findViewById(R.id.displayWelcomeMessage);
		displayWelcomeMessage.setChecked(preferences.isDisplayWelcomeMessage());
		displayWelcomeMessage.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				preferences.setDisplayWelcomeMessage(((CheckBox) v).isChecked());
			}
		});
		
		final CheckBox goToFavouriteOnLaunch = (CheckBox) findViewById(R.id.goToFavouriteOnLaunch);
		goToFavouriteOnLaunch.setChecked(preferences.isGoToFavouriteOnLaunch());
		goToFavouriteOnLaunch.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				preferences.setGoToFavouriteOnLaunch(((CheckBox) v).isChecked());
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		tramHunterDB.updatePreferences(preferences);
	}
	
	
	
	
}
