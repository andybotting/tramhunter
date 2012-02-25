package com.andybotting.tramhunter.activity;

import com.andybotting.tramhunter.R;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class NetworkMapActivity extends Activity {  
  
    @Override  
    protected void onCreate(Bundle savedInstanceState) {  
    	super.onCreate(savedInstanceState);  

    	setContentView(R.layout.network_map);

    	WebView webView = (WebView) findViewById(R.id.webview);
    	
    	// Hide but not remove the functionality of the scrollbar. 
    	// The layout margin adjustment is a nasty work-around.
    	webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
    	
    	webView.setVerticalScrollBarEnabled(false);
    	webView.setHorizontalScrollBarEnabled(false);
        	
    	WebSettings settings = webView.getSettings();
    	settings.setBuiltInZoomControls(true);
    	settings.setLoadWithOverviewMode(true);
    	settings.setUseWideViewPort(true);

        try {
            webView.loadUrl("file:///android_asset/networkmap/networkmap.html");
        } catch (Exception e) {
            e.printStackTrace();
        }    	
        
	}
}