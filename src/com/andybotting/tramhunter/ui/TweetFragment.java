package com.andybotting.tramhunter.ui;

import java.net.URL;

import com.andybotting.tramhunter.objects.Tweet;

import com.andybotting.tramhunter.R;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;  
import android.view.LayoutInflater;  
import android.view.View;  
import android.view.ViewGroup;  
import android.widget.ImageView;
import android.widget.TextView;
   
public class TweetFragment extends Fragment {
	 
	private static Tweet mTweet;
        
	public static TweetFragment newInstance(Tweet tweet) {
		TweetFragment tweetFragment = new TweetFragment();
		mTweet = tweet;
		return tweetFragment;
	}
        
	@Override  
	public void onCreate(Bundle savedInstanceState) {  
		super.onCreate(savedInstanceState);  
	}  
	
	// TODO: Should cache this image for next time
	public Bitmap getBitmap(String bitmapUrl) {
		try {
			URL url = new URL(bitmapUrl);
			return BitmapFactory.decodeStream(url.openConnection().getInputStream()); 
		}
		catch(Exception ex) {
			return null;
		}
	}
	    
	@Override  
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {  

		View tweetView = inflater.inflate(R.layout.tweet, container, false);  
		
		TextView name = (TextView) tweetView.findViewById(R.id.tweet_name);
		TextView username = (TextView) tweetView.findViewById(R.id.tweet_username);
		TextView message = (TextView) tweetView.findViewById(R.id.tweet_message);
		TextView time = (TextView) tweetView.findViewById(R.id.tweet_time);
		ImageView image = (ImageView) tweetView.findViewById(R.id.tweet_image);

		name.setText(mTweet.getName());
		username.setText(mTweet.getUsername());
		message.setText(mTweet.getMessage());
		time.setText(mTweet.twitterHumanFriendlyDate());
		image.setImageBitmap(getBitmap(mTweet.getImageUrl()));
		
		return tweetView;
	}  
}  