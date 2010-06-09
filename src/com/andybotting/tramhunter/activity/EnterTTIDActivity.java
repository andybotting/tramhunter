package com.andybotting.tramhunter.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.dao.TramHunterDB;

public class EnterTTIDActivity extends Activity {

	int tramTrackerId;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);	  

		setContentView(R.layout.enter_ttid);
		 
		String title = getResources().getText(R.string.app_name) + ": Enter an ID";
 		setTitle(title);

		// Show soft keyboard right away
		final InputMethodManager imm = (InputMethodManager) EnterTTIDActivity.this.getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

		final Button button = (Button) findViewById(R.id.buttonGo);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				 
				EditText textTramTrackerId = (EditText)findViewById(R.id.textTramTrackerId);
				
				try {
					tramTrackerId = Integer.parseInt(textTramTrackerId.getText().toString());
				}
				catch (NumberFormatException nfe) {
					Context context = getApplicationContext();
					CharSequence text = "TramTracker ID " + tramTrackerId + " not valid!";
					int duration = Toast.LENGTH_SHORT;
					Toast toast = Toast.makeText(context, text, duration);
					toast.show();
				}
				
				TramHunterDB db = new TramHunterDB(getBaseContext());
				
				// Check to make sure we get 1 result for our TramTrackerID search
				if (db.checkStop(tramTrackerId)) {
					// Hide our soft keyboard
					imm.hideSoftInputFromWindow(textTramTrackerId.getWindowToken(), 0);
					Bundle bundle = new Bundle();
					bundle.putInt("tramTrackerId", tramTrackerId);
					Intent stopsListIntent = new Intent(EnterTTIDActivity.this, StopDetailsActivity.class);
					stopsListIntent.putExtras(bundle);
					startActivityForResult(stopsListIntent, 1);				
				}
				else {				  	
					Context context = getApplicationContext();
					CharSequence text = "TramTracker ID " + tramTrackerId + " not found!";
					int duration = Toast.LENGTH_SHORT;

					Toast toast = Toast.makeText(context, text, duration);
					toast.show();
				}
				
			 }
		 });
	 }
}
