package com.andybotting.tramhunter;

import java.util.UUID;

import com.andybotting.tramhunter.activity.HomeActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;


/**
 * @author necros
 * Allows the default activity to be launched via the home screen without issues when the user backtracks to the home screen or
 * the screen is rotated on the home screen. By using a seperat starting activity we can run this activity and the home in a seperate task
 * therefore ALWAYS starting a NEW home activity when the app icon is clicked.
 */
public class TramHunter extends Activity {

	public static final String KEY_PERFORM_DEFAULT_ACTIVITY_LAUNCH = "PDAL"; 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
		Intent intent = new Intent(TramHunter.this, HomeActivity.class);
		Bundle bundle = new Bundle();
		// Use a unique UUID each time we load the home activity from the launcher
		bundle.putString(KEY_PERFORM_DEFAULT_ACTIVITY_LAUNCH, UUID.randomUUID().toString());
		intent.putExtras(bundle);
		startActivityForResult(intent, 1);        
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		finish();
	}
	
}
