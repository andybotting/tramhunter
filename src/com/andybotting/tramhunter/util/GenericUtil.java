package com.andybotting.tramhunter.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
	
    
    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while(two_halfs++ < 1);
        }
        return buf.toString();
    }
 
    public static String MD5(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException  {
        MessageDigest md;
        md = MessageDigest.getInstance("MD5");
        byte[] md5hash = new byte[32];
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        md5hash = md.digest();
        return convertToHex(md5hash);
    }
    
    
    
}
