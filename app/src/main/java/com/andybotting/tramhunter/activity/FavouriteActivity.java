/*  
 * Copyright 2013 Andy Botting <andy@andybotting.com>
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

package com.andybotting.tramhunter.activity;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.objects.Favourite;
import com.andybotting.tramhunter.objects.FavouriteList;
import com.andybotting.tramhunter.objects.Route;
import com.andybotting.tramhunter.objects.Stop;
import com.andybotting.tramhunter.ui.TouchListView;

public class FavouriteActivity extends AppCompatActivity {

	private final static int CONTEXT_MENU_SET_NAME = 0;
	private final static int CONTEXT_MENU_VIEW_STOP = 1;
	private final static int CONTEXT_MENU_UNFAVOURITE = 2;
	private final static int CONTEXT_MENU_HOMESCREEN_SHORTCUT = 3;

	private FavouritesListAdapter mListAdapter;
	private FavouriteList mFavourites;
	private TouchListView mListView;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		setContentView(R.layout.favourite_list);

		// Set up the Action Bar
		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.title_favourite_stops);

		mListView = (TouchListView) getListView();
		mListView.setDropListener(onDrop);

		mFavourites = new FavouriteList();

		displayFavStops(true);
	}

	protected ListView getListView() {
		if (mListView == null) {
			mListView = (TouchListView) findViewById(android.R.id.list);
		}
		return mListView;
	}

	protected void setListAdapter(ListAdapter adapter) {
		getListView().setAdapter(adapter);
	}

	protected ListAdapter getListAdapter() {
		ListAdapter adapter = getListView().getAdapter();
		if (adapter instanceof HeaderViewListAdapter) {
			return ((HeaderViewListAdapter)adapter).getWrappedAdapter();
		} else {
			return adapter;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		displayFavStops(false);
	}

	/**
	 * 
	 * @param alertIfNoFavourites
	 */
	public void displayFavStops(boolean alertIfNoFavourites) {
		if (alertIfNoFavourites && !mFavourites.hasFavourites())
			alertNoFavourites();

		displayStops();
	}

	/**
	 * 
	 */
	private void alertNoFavourites() {
		final Intent routeListIntent = new Intent(this,
				RoutesListActivity.class);

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder
				.setMessage(R.string.no_favourite_stops)
				.setCancelable(false)
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								startActivity(routeListIntent);
								finish();
							}
						})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
						finish();
					}
				});

		AlertDialog alert = dialogBuilder.create();
		alert.setTitle("No Favourite Stops");
		alert.setIcon(R.drawable.icon);
		alert.show();
	}

	/**
	 * 
	 */
	public void displayStops() {
		mFavourites = new FavouriteList();
		mListAdapter = new FavouritesListAdapter();

		mListView.setOnItemClickListener(mListView_OnItemClickListener);
		mListView.setOnCreateContextMenuListener(mListView_OnCreateContextMenuListener);
		setListAdapter(mListAdapter);
	}

	/**
	 * 
	 * @param stop
	 * @param route
	 */
	private void viewStop(Stop stop, Route route) {
		int tramTrackerId = stop.getTramTrackerID();
		Bundle bundle = new Bundle();
		bundle.putInt("tramTrackerId", tramTrackerId);

		if (route != null)
			bundle.putInt("routeId", route.getId());

		Intent intent = new Intent(FavouriteActivity.this, StopDetailsActivity.class);
		intent.putExtras(bundle);
		startActivityForResult(intent, 1);
	}

	/**
	 * 
	 * @param favourite
	 */
	private void nameStop(final Favourite favourite) {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		final EditText input = new EditText(this);
		alert.setView(input);
		alert.setTitle(R.string.dialog_set_name);
		alert.setIcon(R.drawable.icon);

		// Set the initial stop name in the text field
		if (favourite.hasName())
			input.setText(favourite.getName());
		else
			input.setText(favourite.getStop().getPrimaryName());

		final CharSequence message = getString(R.string.dialog_set_name_message, favourite.getName());
		alert.setMessage(message);

		// Save the new stop name
		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String customName = input.getText().toString().trim();

				if (customName.matches(favourite.getStop().getPrimaryName()))
					favourite.setName(null);
				else
					favourite.setName(customName);

				mFavourites.writeFavourites();
				displayStops();
			}
		});

		alert.setNeutralButton("Reset", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				favourite.setName(null);
				mFavourites.writeFavourites();
				displayStops();
			}
		});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.cancel();
					}
				});
		alert.show();

	}

	/**
	 * Menu actions
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case android.R.id.home:
			finish();
			return true;
		}

		return false;
	}

	/**
	 * List item click listener
	 */
	private OnItemClickListener mListView_OnItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> adapterView, View row, int position, long id) {
			Favourite favourite = mFavourites.getFavourite(position);
			viewStop(favourite.getStop(), favourite.getRoute());
		}
	};

	/**
	 * Context menu items
	 */
	private OnCreateContextMenuListener mListView_OnCreateContextMenuListener = new OnCreateContextMenuListener() {
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
			Favourite favourite = mFavourites.getFavourite(info.position);

			menu.setHeaderIcon(R.drawable.icon);
			menu.setHeaderTitle(favourite.getName());
			// TODO: These should be set in strings.xml / menu xml
			menu.add(0, CONTEXT_MENU_SET_NAME, CONTEXT_MENU_SET_NAME, "Set Stop Name");
			menu.add(0, CONTEXT_MENU_VIEW_STOP, CONTEXT_MENU_VIEW_STOP, "View Stop");
			menu.add(0, CONTEXT_MENU_UNFAVOURITE, CONTEXT_MENU_UNFAVOURITE, "Unfavourite Stop");
			menu.add(0, CONTEXT_MENU_HOMESCREEN_SHORTCUT, CONTEXT_MENU_HOMESCREEN_SHORTCUT, "Create Shortcut");
		}
	};

	/**
	 * Action a context menu item
	 */
	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Favourite favourite = mFavourites.getFavourite(info.position);

		switch (item.getItemId()) {

			case CONTEXT_MENU_VIEW_STOP:
				viewStop(favourite.getStop(), favourite.getRoute());
				return true;

			case CONTEXT_MENU_SET_NAME:
				// Toggle favourite
				nameStop(favourite);
				return true;

			case CONTEXT_MENU_UNFAVOURITE:
				// Toggle favourite
				mFavourites.removeFavourite(favourite);
				mFavourites.writeFavourites();
				displayStops();
				return true;
			case CONTEXT_MENU_HOMESCREEN_SHORTCUT:
				createHomescreenShortcut(favourite);
				return true;
		}

		return super.onContextItemSelected(item);
	}

	private void createHomescreenShortcut(Favourite favourite){
		Intent shortcutIntent = new Intent(this, StopDetailsActivity.class);
		shortcutIntent.putExtra("tramTrackerId", favourite.getStop().getTramTrackerID());
		Route route = favourite.getRoute();
		if (route != null)
			shortcutIntent.putExtra("routeId", route.getId());

		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		final Intent putShortcutIntent = new Intent();
		putShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		putShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, favourite.getName());
		//TODO: create a good icon for this
		putShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this, R.drawable.star_on));
		putShortcutIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
		sendBroadcast(putShortcutIntent);
		Toast.makeText(this, R.string.toast_creating_homescreen_shortcut, Toast.LENGTH_SHORT).show();
	}

	/**
     * 
     */
	private TouchListView.DropListener onDrop = new TouchListView.DropListener() {
		@Override
		public void drop(int from, int to) {
			Favourite favourite = mListAdapter.getItem(from);
			mListAdapter.remove(favourite);
			mListAdapter.insert(favourite, to);
			mListAdapter.notifyDataSetChanged();
			// mPreferenceHelper.setStarredStopsString(mFavourites.toString());
		}
	};

	/**
	 * 
	 * @author andy
	 * 
	 */
	private class FavouritesListAdapter extends BaseAdapter {

		public int getCount() {
			return mFavourites.getCount();
		}

		public void insert(Favourite fav, int to) {
			mFavourites.addFavourite(fav, to);
		}

		public void remove(Favourite fav) {
			mFavourites.removeFavourite(fav);
		}

		public Favourite getItem(int position) {
			return mFavourites.getFavourite(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View pv;
			if (convertView == null) {
				LayoutInflater inflater = getLayoutInflater();
				pv = inflater.inflate(R.layout.favourite_list_row, parent, false);
			} else {
				pv = convertView;
			}

			Favourite favourite = mFavourites.getFavourite(position);
			Stop stop = favourite.getStop();

			String stopName;
			String stopDetails;

			if (favourite.hasName()) {
				stopName = favourite.getName();
				stopDetails = stop.getPrimaryName();
				stopDetails += ", " + stop.getStopDetailsLine();
			} else {
				stopName = stop.getPrimaryName();
				stopDetails = stop.getStopDetailsLine();
			}

			((TextView) pv.findViewById(R.id.stopNameTextView)).setText(stopName);
			((TextView) pv.findViewById(R.id.stopDetailsTextView)).setText(stopDetails);
			((TextView) pv.findViewById(R.id.stopRoutesTextView)).setText(favourite.getRouteName());

			return pv;
		}

	}

}