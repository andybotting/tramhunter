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

package com.andybotting.tramhunter;

import java.util.UUID;
import com.andybotting.tramhunter.activity.HomeActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * @author necros
 * Allows the default activity to be launched via the home screen without issues when the user backtracks to the home screen or
 * the screen is rotated on the home screen. By using a seperat starting activity we can run this activity and the home in a seperate task
 * therefore ALWAYS starting a NEW home activity when the app icon is clicked.
 * 
 * DO NOT MOVE/RENAME THIS FILE! EVER!!! It will break the app upgrades (i.e. Icon will not work and ALL USERS will need to completely reinstall).
 */
public class TramHunter extends Activity {

    private static TramHunter instance;
	public static final String KEY_PERFORM_DEFAULT_ACTIVITY_LAUNCH = "PDAL"; 


	/**
	 * Store the application context
	 */
    public TramHunter() {
        instance = this;
    }

    /**
     * This allows us to get the context anywhere within the application by importing
     * TramHunter, and calling TramHunter.getContext() 
     * @return Context
     */
    public static Context getContext() {
        return instance;
    }
	
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent next = new Intent(this, HomeActivity.class);
		// Use a unique UUID each time we load the home activity from the launcher
		next.putExtra(KEY_PERFORM_DEFAULT_ACTIVITY_LAUNCH, UUID.randomUUID().toString());
		startActivity(next);
		finish();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		finish();
	}
	
}
