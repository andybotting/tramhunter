package com.andybotting.tramhunter.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.andybotting.tramhunter.objects.Stop;
import com.andybotting.tramhunter.objects.Tweet;

public class YarraTramsTwitter {

	private static final String TAG = "YarraTramsTwitter";
    private static final boolean LOGV = Log.isLoggable(TAG, Log.INFO);
    
    private static final String YARRA_TRAMS_TWITTER_URL = "https://api.twitter.com/1/statuses/user_timeline.json?exclude_replies=true&screen_name=yarratrams&count=4";
	
	/**
	 * Fetch JSON data over HTTP
	 * @throws TramTrackerServiceException 
	 */
	public InputStream getJSONData(String url) throws TramTrackerServiceException{
        DefaultHttpClient httpClient = new DefaultHttpClient();

        URI uri;
        InputStream data = null;
        try {
            uri = new URI(url);
            HttpGet method = new HttpGet(uri);
            HttpResponse response = httpClient.execute(method);
            data = response.getEntity().getContent();
        } 
        catch (Exception e) {
        	throw new TramTrackerServiceException(e);
        }

        return data;
    }

	
    /**
     * Parse the given {@link InputStream} into a {@link JSONObject}.
     * @throws TramTrackerServiceException 
     */
    private static JSONArray parseJSONArray(InputStream is) throws IOException, JSONException, TramTrackerServiceException {
    	JSONArray jsonArray = null;
    	try {
    		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    		StringBuilder sb = new StringBuilder();
    		String line = null;
    		while ((line = reader.readLine()) != null) {
    			sb.append(line);
    		}
    		is.close();
    		String jsonData = sb.toString();
            if (LOGV) Log.v(TAG, "JSON Response: " + jsonData);
            jsonArray = new JSONArray(jsonData);
    	} 
    	catch(Exception e){
    		throw new TramTrackerServiceException(e);
    	}
		return jsonArray;
    }	
    
    

    /**
     * Parse the given {@link InputStream} into {@link Stop}
     * assuming a JSON format.
     * @return Stop
     */
    public List<Tweet> parseTweets(JSONArray tweetArray) throws TramTrackerServiceException {
    	
    	try {
    		List<Tweet> tweets = new ArrayList<Tweet>();
	        
	        int tweetCount = tweetArray.length();
	        
	        for (int i = 0; i < tweetCount; i++) {
	        	
	        	JSONObject tweetObject = tweetArray.getJSONObject(i);

	        	// User object
	        	JSONObject userObject = tweetObject.getJSONObject ("user");
	        	
	        	String profileImage = userObject.getString("profile_image_url");
	        	
	            String username = userObject.getString("screen_name");
	            String name = userObject.getString("name");
	            String text = tweetObject.getString("text");
	            String date = tweetObject.getString("created_at");
	            
   	            Tweet tweet = new Tweet();
	            
	            tweet.setUsername(username);
	            tweet.setName(name);
	            tweet.setMessage(text);
	            tweet.setImageUrl(profileImage);
	            tweet.setDate(date);
	        
				tweets.add(tweet);
	        }
	        
			return tweets;
		} 
    	catch (Exception e) {
			throw new TramTrackerServiceException(e);
		}
    	
    }	
	
    /**
     * Get the list of tweets
     */
	public List<Tweet> getTweets() throws TramTrackerServiceException {

		try {
			List<Tweet> tweets = null;
			InputStream jsonData = getJSONData(YARRA_TRAMS_TWITTER_URL);
			JSONArray tweetArray = parseJSONArray(jsonData);
			tweets = parseTweets(tweetArray);
			return tweets;
			
		} 
		catch (Exception e) {
			throw new TramTrackerServiceException(e);
		}
		
	}

}
