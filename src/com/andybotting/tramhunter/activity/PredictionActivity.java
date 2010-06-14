package com.andybotting.tramhunter.activity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.andybotting.tramhunter.NextTram;
import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.Route;
import com.andybotting.tramhunter.Stop;
import com.andybotting.tramhunter.dao.TramHunterDB;
import com.andybotting.tramhunter.service.TramTrackerService;
import com.andybotting.tramhunter.service.TramTrackerServiceSOAP;
import com.andybotting.tramhunter.util.FavouriteStopUtil;

public class PredictionActivity extends ListActivity {
	private static final int MAX_FAVOURITES_RANGE_METRES = 1000;
	private static final int NEXT_TRAM_CUTOFF_TIME_MINS = 300;
	
	private TramHunterDB db;
	private FavouriteStopUtil favouriteStopUtil;

	private List<NextTram> nextTrams = new ArrayList<NextTram>();

	CompoundButton starButton;
	TramTrackerService ttService;

	protected boolean running = false;

	private volatile Thread refreshThread;

	boolean loadingError = false;
	boolean showDialog = true;

	Date melbourneTime;

	// Handle the timer
	Handler UpdateHandler = new Handler() {
		public void handleMessage(Message msg) {
			new GetNextTramTimes().execute();
		}
	};

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		// Set the window to have a spinner in the title bar
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.prediction_list);

		// Create out DB instance
		initialiseDatabase();
		final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		favouriteStopUtil = new FavouriteStopUtil(db, locationManager);

		String title = "Hunted Trips";
		setTitle(title);

		// Our thread for updating the stops every 60 secs
		refreshThread = new Thread(new CountDown());
		refreshThread.setDaemon(true);
		refreshThread.start();

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (db != null) {
			db.close();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		refreshThread.interrupt();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (refreshThread.isInterrupted()) {
			refreshThread.start();
		}
	}

	// Add settings to menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, 0, 0, "Refresh");
		MenuItem menuItem1 = menu.findItem(0);
		menuItem1.setIcon(R.drawable.ic_menu_refresh);

		return true;
	}

	// Menu actions
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			// Refresh
			showDialog = true;
			new GetNextTramTimes().execute();
			return true;
		}
		
		return false;

	}

	private void initialiseDatabase() {
		db = new TramHunterDB(this);
	}

	private class CountDown implements Runnable {
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				Message m = new Message();
				UpdateHandler.sendMessage(m);
				try {
					// 60 Seconds
					Thread.sleep(60 * 1000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	private class GetNextTramTimes extends
			AsyncTask<NextTram, Void, List<NextTram>> {
		private final ProgressDialog dialog = new ProgressDialog(
				PredictionActivity.this);
		TramTrackerService ttService = new TramTrackerServiceSOAP(getBaseContext());
		TextView dateStringText = (TextView) findViewById(R.id.bottomLine);

		// Can use UI thread here
		@Override
		protected void onPreExecute() {

			if (showDialog) {
				// Show the dialog window
				this.dialog.setMessage("Fetching Tram Times...");
				this.dialog.show();
				showDialog = false;
			}
			// Show the spinner in the title bar
			setProgressBarIndeterminateVisibility(true);

		}

		// Automatically done on worker thread (separate from UI thread)
		@Override
		protected List<NextTram> doInBackground(final NextTram... params) {

			nextTrams.clear();
			// Lets finds routes from our favourite stop that take us to other
			// favourite stops
			for (Stop favStop : favouriteStopUtil.getClosestFavouriteStops(5, MAX_FAVOURITES_RANGE_METRES)) {				getNextTramsForFavouriteStop(favStop);
			}
			
			return nextTrams;
		}

		private void getNextTramsForFavouriteStop(Stop favStop) {
			final Map<String, List<Stop>> routesToOtherFavourites = getRoutesToOtherFavourites(favStop);
			
			final List<NextTram> nextTramsForStop = ttService.getNextPredictedRoutesCollection(favStop);
			
			for (NextTram nextTram : nextTramsForStop) {
				if (isNextTramWithinMaximumTime(nextTram) && isRouteNumberMatchingFilter(routesToOtherFavourites, nextTram)) {
					nextTram.setOriginStop(favStop);
					nextTram.setFavouritesOnRoute(routesToOtherFavourites.get(nextTram.getRouteNo()));
					nextTrams.add(nextTram);
				}
			}
		}

		private boolean isRouteNumberMatchingFilter(
				final Map<String, List<Stop>> routesToOtherFavourites,
				NextTram nextTram) {
			return routesToOtherFavourites.keySet().contains(nextTram.getRouteNo());
		}

		private boolean isNextTramWithinMaximumTime(NextTram nextTram) {
			return nextTram.minutesAway() <= NEXT_TRAM_CUTOFF_TIME_MINS;
		}

		private Map<String, List<Stop>> getRoutesToOtherFavourites(final Stop closestFavStop) {
			final Map<String, List<Stop>> routeNumbersWithStops = new HashMap<String, List<Stop>>();
			
			for (Route routeFromFavStop : closestFavStop.getRoutes()) {

				List<Stop> otherFavStopsForRoute = db
						.getFavouriteStopsOnRoute(closestFavStop,
								routeFromFavStop);

				// If we found other fav stops on the route
				if (!otherFavStopsForRoute.isEmpty()) {
					routeNumbersWithStops.put(routeFromFavStop.getNumber(), otherFavStopsForRoute);
				}
			}
			return routeNumbersWithStops;
		}

		// Can use UI thread here
		@Override
		protected void onPostExecute(List<NextTram> nextTrams) {

			if (nextTrams.size() > 0) {
				// Sort trams by minutesAway
				Collections.sort(nextTrams);
				loadingError = false;

				SimpleDateFormat sdf = new SimpleDateFormat(
						"EEE, d MMM yyyy h:mm a");
				Calendar today = Calendar.getInstance(); // today

				dateStringText.setText("Last updated: "
						+ sdf.format(today.getTime()));

				setListAdapter(new NextTramsListAdapter());
			} else {
				// If we've not had a loading error already
				if (!loadingError) {
					Context context = getApplicationContext();
					CharSequence text = "Failed to fetch tram times";
					int duration = Toast.LENGTH_SHORT;
					Toast toast = Toast.makeText(context, text, duration);
					toast.show();
				}
				loadingError = true;
			}

			// Hide our dialog window
			if (this.dialog.isShowing()) {
				this.dialog.dismiss();
			}
			setProgressBarIndeterminateVisibility(false);

		}
	}

	private class NextTramsListAdapter extends BaseAdapter {

		public int getCount() {
			return nextTrams.size();
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {

			View pv = convertView;
			ViewWrapper wrapper = null;

			if (pv == null) {
				LayoutInflater inflater = getLayoutInflater();
				pv = inflater.inflate(R.layout.prediction_list_row, parent,
						false);

				wrapper = new ViewWrapper(pv);

				pv.setTag(wrapper);

				wrapper = new ViewWrapper(pv);
				pv.setTag(wrapper);
			} else {
				wrapper = (ViewWrapper) pv.getTag();
			}

			pv.setId(0);

			NextTram thisTram = (NextTram) nextTrams.get(position);

			wrapper.getNextTramRouteNumber().setText(thisTram.getRouteNo());
			
			//TODO: Temporarily going to put the favourite stops name in this column
			final StringBuilder sb = new StringBuilder();
			for (Stop stop : thisTram.getFavouritesOnRoute()) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append(stop.getPrimaryName());
			}
			
			// wrapper.getNextTramDestination().setText(thisTram.getDestination());
			final String label = String.format("From: %s%nTo: %s", thisTram.getOriginStop().getPrimaryName(), sb.toString());
			wrapper.getNextTramDestination().setText(label);

			wrapper.getNextTramTime().setText(thisTram.humanMinutesAway());

			return pv;
		}

	}

	private class ViewWrapper {
		View base;

		TextView nextTramRouteNumber = null;
		TextView nextTramDestination = null;
		TextView nextTramTime = null;

		ViewWrapper(View base) {
			this.base = base;
		}

		TextView getNextTramRouteNumber() {
			if (nextTramRouteNumber == null) {
				nextTramRouteNumber = (TextView) base
						.findViewById(R.id.routeNumber);
			}
			return (nextTramRouteNumber);
		}

		TextView getNextTramDestination() {
			if (nextTramDestination == null) {
				nextTramDestination = (TextView) base
						.findViewById(R.id.routeDestination);
			}
			return (nextTramDestination);
		}

		TextView getNextTramTime() {
			if (nextTramTime == null) {
				nextTramTime = (TextView) base.findViewById(R.id.nextTime);
			}
			return (nextTramTime);
		}

	}
}