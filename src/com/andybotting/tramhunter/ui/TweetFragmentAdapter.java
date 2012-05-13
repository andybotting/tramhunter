package com.andybotting.tramhunter.ui;

import java.util.ArrayList;

import com.andybotting.tramhunter.objects.Tweet;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class TweetFragmentAdapter extends FragmentPagerAdapter {

	private ArrayList<Tweet> mTweets;
	private Tweet tweet;

	public TweetFragmentAdapter(FragmentManager fm, ArrayList<Tweet> tweets) {
		super(fm);
		mTweets = tweets;
	}

	@Override
	public Fragment getItem(int position) {
		tweet = mTweets.get(position);
		return TweetFragment.newInstance(tweet);
	}

	@Override
	public int getCount() {
		return mTweets.size();
	}
	
}