package com.andybotting.tramhunter.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class GenericUtil {

    public static void popToast(Context context, CharSequence text){   	
    	int duration = Toast.LENGTH_LONG;
    	Toast toast = Toast.makeText(context, text, duration);
    	toast.show();	
    }
    
    public static void logDebug(String message){
    	// Log some text thats EASY to see in logcat
    	if(message!=null)
    		Log.v("Debug", "########## " +  message);
    }
	
}
