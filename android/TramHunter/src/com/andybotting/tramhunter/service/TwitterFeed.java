package com.andybotting.tramhunter.service;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.os.Environment;
import android.text.Html;
import android.util.Log;

import com.andybotting.tramhunter.TramHunterApplication;
import com.andybotting.tramhunter.objects.Stop;
import com.andybotting.tramhunter.objects.Tweet;
import com.andybotting.tramhunter.ui.UIUtils;
import com.andybotting.tramhunter.util.PreferenceHelper;
import com.andybotting.tramhunter.util.UserAgent;

public class TwitterFeed {

	private static final String TAG = "YarraTramsTwitter";
	private static final boolean LOGV = Log.isLoggable(TAG, Log.INFO);

	// private static final String TRAM_HUNTER_TWITTER_URL =
	// "http://api.twitter.com/1/statuses/user_timeline.json?exclude_replies=true&screen_name=tram_hunter&count=2";
	private static final String YARRA_TRAMS_TWITTER_URL = "http://tramhunter2.appspot.com/twitter_feed/";
	private static final int TWITTER_UPDATE_MINS = 5;

	private Context mContext;

	public TwitterFeed() {
		mContext = TramHunterApplication.getContext();
	}

	/**
	 * Fetch JSON data over HTTP
	 */
	public InputStream getJSONData(String url) throws URISyntaxException, IllegalStateException, IOException {
		Log.d(TAG, "Fetching URL: " + url);
		DefaultHttpClient httpClient = new DefaultHttpClient();
		String userAgent = UserAgent.getUserAgent();
		httpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, userAgent);

		URI uri = new URI(url);
		HttpGet method = new HttpGet(uri);
		method.addHeader("Accept-Encoding", "gzip");
		HttpResponse response = httpClient.execute(method);

		// Handle GZIP'd response from Twitter
		// (which is about half the time for some reason)
		InputStream is = response.getEntity().getContent();
		Header contentEncoding = response.getFirstHeader("Content-Encoding");
		if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
			Log.d(TAG, "Twitter feed is GZIP'd");
			is = new GZIPInputStream(is);
		}

		return is;

	}

	/**
	 * Parse the JSON array
	 */
	private static JSONArray parseJSONArray(InputStream is) throws IOException, JSONException {

		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}
		is.close();
		String jsonData = sb.toString();
		Log.v(TAG, "JSON Response: " + jsonData);
		return new JSONArray(jsonData);
	}

	/**
	 * Parse the tweet
	 */
	public ArrayList<Tweet> parseTweets(JSONArray tweetArray) throws JSONException {

		ArrayList<Tweet> tweets = new ArrayList<Tweet>();

		int tweetCount = tweetArray.length();

		for (int i = 0; i < tweetCount; i++) {

			JSONObject tweetObject = tweetArray.getJSONObject(i);

			// User object
			JSONObject userObject = tweetObject.getJSONObject("user");

			String profileImage = userObject.getString("profile_image_url");
			String username = userObject.getString("screen_name");
			String name = userObject.getString("name");

			// Decode HTML characters
			// http://stackoverflow.com/questions/2918920/decode-html-entities-in-android
			String encodedMessage = tweetObject.getString("text");
			String message = Html.fromHtml(encodedMessage).toString();

			String date = tweetObject.getString("created_at");

			Tweet tweet = new Tweet();

			tweet.setUsername(username);
			tweet.setName(name);
			tweet.setMessage(message);
			tweet.setDate(date);

			File filePath;
			try {
				// Android API v8+
				filePath = new File(TramHunterApplication.getContext().getExternalFilesDir(null), username + ".PNG");
			} catch (NoSuchMethodError e) {
				// Older API
				filePath = new File(Environment.getExternalStorageDirectory(), "Android/data/com.andybotting.tramhunter/files/" + username + ".PNG");
			}

			try {
				// Test to see if our bitmap exists and is loadable
				Bitmap bitmap = BitmapFactory.decodeFile(filePath.getAbsolutePath());
				bitmap.getHeight();
				tweet.setImagePath(filePath.getAbsolutePath());
			} catch (Exception e) {
				if (LOGV) Log.v(TAG, "Image doesn't exist, downloading...");
				// Let's download the profile image
				Bitmap bitmap;
				try {
					bitmap = downloadImage(profileImage);
					saveImage(bitmap, username);
					tweet.setImagePath(filePath.getAbsolutePath());
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

			tweets.add(tweet);
		}

		return tweets;
	}

	/**
	 * Get the list of tweets by either saved preferences or fetch from twitter
	 */
	public ArrayList<Tweet> getTweets() throws IOException, JSONException, IllegalStateException, URISyntaxException {
		ArrayList<Tweet> tweets = null;

		PreferenceHelper preferenceHelper = new PreferenceHelper();
		long lastUpdate = preferenceHelper.getLastTwitterUpdateTimestamp();
		long timeDiff = UIUtils.dateDiff(lastUpdate);

		String savedTwitterData = preferenceHelper.getLastTwitterData();

		JSONArray tweetArray;

		// Kick off an update
		if ((timeDiff > TWITTER_UPDATE_MINS * 60000) || (savedTwitterData == null)) {
			if (LOGV) Log.v(TAG, "Fetching fresh twitter feed...");

			InputStream jsonData = getJSONData(YARRA_TRAMS_TWITTER_URL);
			tweetArray = parseJSONArray(jsonData);

			// Save this data for next time
			preferenceHelper.setLastTwitterData(tweetArray.toString());
		} else {
			if (LOGV) Log.v(TAG, "Using saved twitter feed...");
			tweetArray = new JSONArray(savedTwitterData);
		}

		tweets = parseTweets(tweetArray);

		return tweets;
	}

	/**
	 * Download an image from a URL
	 */
	public Bitmap downloadImage(String url) throws IOException {
		if (LOGV) Log.v(TAG, "Downloading image: " + url);

		int IO_BUFFER_SIZE = 4 * 1024;

		final AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
		final HttpGet getRequest = new HttpGet(url);

		try {
			HttpResponse response = client.execute(getRequest);
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				Log.w(TAG, "Error " + statusCode + " while retrieving bitmap from " + url);
				return null;
			}

			final HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream inputStream = null;
				OutputStream outputStream = null;
				try {
					inputStream = entity.getContent();
					final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
					outputStream = new BufferedOutputStream(dataStream, IO_BUFFER_SIZE);

					byte[] b = new byte[IO_BUFFER_SIZE];
					int read;
					while ((read = inputStream.read(b)) != -1) {
						outputStream.write(b, 0, read);
					}
					outputStream.flush();

					final byte[] data = dataStream.toByteArray();
					final Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

					// FIXME : Should use BitmapFactory.decodeStream(inputStream) instead.
					// final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

					return bitmap;

				} finally {
					if (inputStream != null) {
						inputStream.close();
					}
					if (outputStream != null) {
						outputStream.close();
					}
					entity.consumeContent();
				}
			}
		} catch (IOException e) {
			Log.w(TAG, "I/O error while retrieving bitmap from " + url, e);
		} catch (IllegalStateException e) {
			Log.w(TAG, "Incorrect URL: " + url);
		} catch (Exception e) {
			Log.w(TAG, "Error while retrieving bitmap from " + url, e);
		} finally {
			if (client != null) {
				client.close();
			}
		}
		return null;
	}

	/**
	 * Save our downloaded image to the SD card
	 */
	public void saveImage(Bitmap bitmap, String username) {

		File filePath;
		try {
			// Android API v8+
			filePath = new File(TramHunterApplication.getContext().getExternalFilesDir(null), username + ".PNG");
		} catch (NoSuchMethodError e) {
			// Older API
			filePath = new File(Environment.getExternalStorageDirectory(), "Android/data/com.andybotting.tramhunter/files/" + username + ".PNG");
		}

		if (LOGV) Log.v(TAG, "Saving file: " + filePath.getAbsolutePath());

		FileOutputStream os = null;
		try {
			os = new FileOutputStream(filePath);
			bitmap.compress(Bitmap.CompressFormat.PNG, 95, os);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
