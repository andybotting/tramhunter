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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.dao.TramHunterDB;
import com.andybotting.tramhunter.ui.UIUtils;

public class EnterTTIDActivity extends Activity {

	int tramTrackerId;
	Context context;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);	  

		context = this.getApplicationContext();
		setContentView(R.layout.enter_ttid);
		 
		String title = "Enter an ID";
		((TextView) findViewById(R.id.title_text)).setText(title);
		
		// Home title button
		findViewById(R.id.title_btn_home).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	UIUtils.goHome(EnterTTIDActivity.this);
		    }
		});	

		// Search title button
		findViewById(R.id.title_btn_search).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	UIUtils.goSearch(EnterTTIDActivity.this);
		    }
		});	
		
		
		final Button button = (Button) findViewById(R.id.buttonGo);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				 
				EditText textTramTrackerId = (EditText)findViewById(R.id.textTramTrackerId);
				
				try {
					tramTrackerId = Integer.parseInt(textTramTrackerId.getText().toString());
				}
				catch (NumberFormatException nfe) {
					CharSequence text = "TramTracker ID " + tramTrackerId + " not valid!";
					int duration = Toast.LENGTH_SHORT;
					Toast toast = Toast.makeText(context, text, duration);
					toast.show();
				}
				
				TramHunterDB db = new TramHunterDB();
				
				// Check to make sure we get 1 result for our TramTrackerID search
				if (tramTrackerId >= 8000) {
					CharSequence text = "TramTracker ID " + tramTrackerId + " not found!";
					int duration = Toast.LENGTH_SHORT;
					Toast toast = Toast.makeText(context, text, duration);
					toast.show();
				}
				else if (db.checkStop(tramTrackerId)) {
					// Hide our soft keyboard
					Bundle bundle = new Bundle();
					bundle.putInt("tramTrackerId", tramTrackerId);
					Intent stopsListIntent = new Intent(context, StopDetailsActivity.class);
					stopsListIntent.putExtras(bundle);
					startActivityForResult(stopsListIntent, 1);
					finish();
				}
				else {				  	
					CharSequence text = "TramTracker ID " + tramTrackerId + " not found!";
					int duration = Toast.LENGTH_SHORT;
					Toast toast = Toast.makeText(context, text, duration);
					toast.show();
				}
				
				// Clean up
				db.close();
				
			 }
		 });
	 }
}
