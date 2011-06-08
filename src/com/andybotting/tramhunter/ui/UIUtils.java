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

package com.andybotting.tramhunter.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import java.util.Date;

import com.andybotting.tramhunter.activity.HomeActivity;


public class UIUtils {

    /**
     * Invoke "home" action, returning to {@link Home}.
     */
    public static void goHome(Context context) {
        final Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    /**
     * Invoke "search" action, triggering a default search.
     */
    public static void goSearch(Activity activity) {
        activity.startSearch(null, false, Bundle.EMPTY, false);
    }

    /**
     * Return the difference between two dates
     */
	public static long dateDiff(long timestamp) {
		Date now = new Date();
		long diff = now.getTime() - timestamp;
		return diff;
	}

    /**
     * Generate a human friendly time string based on milliseconds
     */
	public static String timeString(long duration) {
		
		long ONE_SECOND = 1000;
		long ONE_MINUTE = ONE_SECOND * 60;
		long ONE_HOUR = ONE_MINUTE * 60;
		long ONE_DAY = ONE_HOUR * 24;

		StringBuffer res = new StringBuffer();
	    long temp = 0;
	    
	    if (duration >= ONE_DAY * 99) {
	    	return "Never updated";
	    }

    	if (duration >= ONE_MINUTE) {
    		temp = duration / ONE_DAY;
    		if (temp > 0) {
    			duration -= temp * ONE_DAY;
    			res.append(temp).append(" day");
    		}
    		else {
    			temp = duration / ONE_HOUR;
    			if (temp > 0) {
    				duration -= temp * ONE_HOUR;
    				res.append(temp).append(" hr");
    			}
    			else {
    				temp = duration / ONE_MINUTE;
    				if (temp > 0) {
    					duration -= temp * ONE_MINUTE;
    					res.append(temp).append(" min");
    				}
    			}
    		}

	    	res.insert(0, "Updated ");
	    	res.append(" ago");
	    	return res.toString();
	    } 
	    else {
	    	return "Just updated";
	    }
	}

    /**
     * Helper for creating toasts. You could call it a toaster.
     */
    public static void popToast(Context context, CharSequence text) {
    	int duration = Toast.LENGTH_LONG;
    	Toast toast = Toast.makeText(context, text, duration);
    	toast.show();	
    }
    
    /**
     * Get the tram image name from a given class
     * @param tramClass
     * @return string representing the tram image resource name
     */
    public static String getTramImage(String tramClass) {
    	
    	// If tramClass is null, 
    	if (tramClass == null)
    		return null;
    	
		// Match the name
		if (tramClass.matches("A"))
			return "class_a";
		else if (tramClass.matches("A1"))
			return "class_a";	
		else if (tramClass.matches("A2"))
			return "class_a";		
		else if (tramClass.matches("B1"))
			return "class_b";
		else if (tramClass.matches("B2"))
			return "class_b";	
		else if (tramClass.matches("C"))
			return "class_c";	
		else if (tramClass.matches("C2"))
			return "class_c";		
		else if (tramClass.matches("D1"))
			return "class_d1";	
		else if (tramClass.matches("D2"))
			return "class_d2";	
		else if (tramClass.matches("SW5"))
			return "class_w";
		else if (tramClass.matches("SW6"))
			return "class_w";
		else if (tramClass.matches("W6"))
			return "class_w";		
		else if (tramClass.matches("W7"))
			return "class_w";				
		else if (tramClass.matches("Z1"))
			return "class_z1";		
		else if (tramClass.matches("Z2"))
			return "class_z1";			
		else if (tramClass.matches("Z3"))
			return "class_z3";			
		else
			return null;
    }
    
    
}
