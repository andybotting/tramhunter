/*  
 * Copyright 2012 Andy Botting <andy@andybotting.com>  
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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.objects.Tweet;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class InfoWindow {

	private final Activity mActivity;

	public InfoWindow(final Activity mActivity) {
		this.mActivity = mActivity;
	}
	
	public List<View> getInfoWindows(List<Tweet> tweets) {
		
		List<View> tweetViews = new ArrayList<View>();
		
		for (int i=1; i <= tweets.size(); i++) {
			String number = i + "/" + tweets.size();
			tweetViews.add(buildTweetView(tweets.get(i-1), number));
		}
		
		return tweetViews;
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

	
	private View buildTweetView(Tweet tweet, String num) {
		
		View tweetView = mActivity.getLayoutInflater().inflate(R.layout.tweet, null);
		
		TextView name = (TextView) tweetView.findViewById(R.id.tweet_name);
		TextView username = (TextView) tweetView.findViewById(R.id.tweet_username);
		TextView message = (TextView) tweetView.findViewById(R.id.tweet_message);
		TextView time = (TextView) tweetView.findViewById(R.id.tweet_time);
		ImageView image = (ImageView) tweetView.findViewById(R.id.tweet_image);
		TextView number = (TextView) tweetView.findViewById(R.id.tweet_number);

		name.setText(tweet.getName());
		username.setText(tweet.getUsername());
		message.setText(tweet.getMessage());
		time.setText(tweet.twitterHumanFriendlyDate());
		image.setImageBitmap(getBitmap(tweet.getImageUrl()));
		number.setText(num);

		return tweetView;
	}
	
}
