package com.andybotting.tramhunter.dao;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.andybotting.tramhunter.objects.Destination;
import com.andybotting.tramhunter.objects.Route;
import com.andybotting.tramhunter.objects.Stop;

import com.andybotting.tramhunter.util.PreferenceHelper;
import com.andybotting.tramhunter.util.StringUtil;

public class TramHunterDB extends SQLiteOpenHelper {
	
    private static final String TAG = "TramHunterDB";
    private static final boolean LOGV = Log.isLoggable(TAG, Log.DEBUG);
	 
	private static final String AUTHORITY = "com.andybotting.tramhunter";
	private static final String DB_NAME = "tramhunter.db";
	private static final String DB_PATH = "/data/data/"+ AUTHORITY + "/databases/";
	private static final String DB_FILE = DB_PATH + DB_NAME;

	// Update this is we modify the DB in any way
	private static final int DB_VERSION = 7;
	
	// Existing
	private static final String TABLE_ROUTES = "routes";
	private static final String TABLE_DESTINATIONS = "destinations";
	private static final String TABLE_STOPS = "stops";
	private static final String TABLE_TRAMS = "trams";
	
	private static final String TABLE_STOPS_JOIN_DESTINATIONS = "stops "
		+ "JOIN destination_stops ON destination_stops.stop_id = stops._id "
		+ "JOIN destinations ON destination_stops.destination_id = destinations._id";

	private static final String TABLE_STOPS_JOIN_ROUTES = "stops "
		+ "JOIN destination_stops ON destination_stops.stop_id = stops._id "
		+ "JOIN destinations ON destination_stops.destination_id = destinations._id "
		+ "JOIN routes ON destinations.route_id = routes._id";
	
	private static final String TABLE_DESTINATIONS_JOIN_ROUTES = "destinations "
		+ "JOIN destination_stops ON destination_stops.destination_id = destinations._id "
		+ "JOIN routes ON destinations.route_id = routes._id";
	

	private SQLiteDatabase db; 
	private Context context;
	private PreferenceHelper mPreferenceHelper;
	
	public TramHunterDB(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		this.context = context;
		this.mPreferenceHelper = new PreferenceHelper(context);
	}	
	
	
	public SQLiteDatabase getDatabase() {
		SQLiteDatabase db;  
		
		try {		 	
			this.createDataBase();
		} 
		catch (IOException ioe) {
			throw new Error("Unable to create database");
		}
	
		try {
			this.openDataBase();
		}
		catch(SQLException sqle){
			throw sqle;
		}

		db = this.getWritableDatabase();
			
		return db;
	}
	
	public void createDataBase() throws IOException{
 
		boolean dbExist = checkDataBase();
 
		if (dbExist) {
			List<Integer> oldFavouriteStops = new ArrayList<Integer>();

			db = SQLiteDatabase.openDatabase(DB_FILE, null, SQLiteDatabase.OPEN_READONLY);
			int thisDBVersion = db.getVersion();
			
			// Get the favourite stops if the version is changing
			if (thisDBVersion < 7) {
				if (LOGV) Log.v(TAG, "Existing DB version < 7, upgrading any existing favourites...");
				oldFavouriteStops = getOldFavouriteStops(db);
			}
			
			db.close();
			
			if (thisDBVersion < DB_VERSION) {							
				try {
					// Upgrade the DB
					if (LOGV) Log.v(TAG, "Copying database...");
					copyDataBase();
					
					// If we're upgrading with existing favourites
					if (oldFavouriteStops.size() > 0) {
						if (LOGV) Log.v(TAG, "Found " + oldFavouriteStops.size() + " old favourite stop(s)...");
						for (int tramTrackerID : oldFavouriteStops)
							mPreferenceHelper.starStop(tramTrackerID);
					}
				} 
				catch (IOException e) {
					throw new Error("Error copying database");
				}
			}	
		}
		else{
			// By calling this method and empty database will be created into the default system path
			// of your application so we are gonna be able to overwrite that database with our database.
			this.getReadableDatabase();
 
			try {
				copyDataBase();
			} catch (IOException e) {
				throw new Error("Error copying database");
			}
		}
	}
 
	
	/**
	 * Check if the database already exist to avoid re-copying the file each time you open the application.
	 * @return true if it exists, false if it doesn't
	 */
	private boolean checkDataBase() {
 
		SQLiteDatabase checkDB = null;
 
		try {
			checkDB = SQLiteDatabase.openDatabase(DB_FILE, null, SQLiteDatabase.OPEN_READONLY);
		}
		catch(SQLiteException e){
			//database does't exist yet.
		}
 
		if(checkDB != null)
			checkDB.close();
 
		return checkDB != null ? true : false;
	}
 
	/**
	 * Copies your database from your local assets-folder to the just created empty database in the
	 * system folder, from where it can be accessed and handled.
	 * This is done by transfering bytestream.
	 * */
	private void copyDataBase() throws IOException {
		
		// Open your local db as the input stream
		InputStream myInput = context.getAssets().open(DB_NAME);

		// Open the empty db as the output stream
		OutputStream myOutput = new FileOutputStream(DB_FILE);
 
		// Transfer bytes from the inputfile to the outputfile
		byte[] buffer = new byte[1024];
		int length;
		while ( (length = myInput.read(buffer) ) > 0) {
			myOutput.write(buffer, 0, length);
		}
 
		// Close the streams
		myOutput.flush();
		myOutput.close();
		myInput.close();
	}

	
	public void openDataBase() throws SQLException {
	 	// Open the database
		db = SQLiteDatabase.openDatabase(DB_FILE, null, SQLiteDatabase.OPEN_READONLY);
		// Close the DB to prevent a leak
		db.close();
	}
 
	@Override
	public synchronized void close() {
		if(db != null)
			db.close();
		super.close();
	}
 
	@Override
	public void onCreate(SQLiteDatabase db) {
		// Do nothing here
	}
 
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Do nothing here
	}
	
	// Get a list of destinations
	public List<Route> getRoutes() {
		db = getDatabase();
	
		List<Route> routes = new ArrayList<Route>();
		
		Cursor c = db.query(TABLE_ROUTES, 
				new String[] { "_id", "number" }, 
				null, 
				null, 
				null, 
				null, 
				null);
		
		if (c.moveToFirst()) {		
			do {	
				Route route = new Route();

				int col_id = c.getColumnIndexOrThrow(Routes.ID);
				int col_number = c.getColumnIndexOrThrow(Routes.NUMBER);
				
				int routeId = c.getInt(col_id);
				route.setId(routeId);
				route.setNumber(c.getString(col_number));
				
				// TODO: We should call this as another method, but doing it here
				// until we get out db create/close stuff sorted
				Cursor d = db.query(TABLE_DESTINATIONS, 
						new String[] { "destinations._id", "destination", "direction"},
						DestinationsColumns.ROUTE_ID + " = '" + routeId + "'",
						null, 
						null, 
						null, 
						null);
				
				if (d.moveToFirst()) {
					do {	
						Destination dest = new Destination();

						int d_col_id = d.getColumnIndexOrThrow(DestinationsColumns.ID);
						int d_col_destination = d.getColumnIndexOrThrow(DestinationsColumns.DESTINATION);
						int d_col_direction = d.getColumnIndexOrThrow(DestinationsColumns.DIRECTION);

						dest.setId(d.getLong(d_col_id));
						dest.setRouteNumber(route.getNumber());
						dest.setDestination(d.getString(d_col_destination));
						dest.setUp(d.getInt(d_col_direction));
						
						if (dest.getUp()) {
							route.setDestinationDown(dest);
						}
						else {
							route.setDestinationUp(dest);
						}
					} 
					while(d.moveToNext());
				}
				
				d.close();				

				routes.add(route);
			} 
			while(c.moveToNext());
		}
		
		c.close();
		db.close();
		
		return routes;
	}
	
	


	// Get a list of destinations
	public List<Destination> getDestinations() {
		db = getDatabase();

		List<Destination> destinations = new ArrayList<Destination>();
		
		Cursor c = db.query(TABLE_DESTINATIONS_JOIN_ROUTES, 
							new String[] { "destinations._id", "number", "destinations.destination", "direction"},
							null, 
							null, 
							"destinations._id", 
							null, 
							null);
		
		if (c.moveToFirst()) {		
			do {	
				Destination dest = new Destination();

				int col_id = c.getColumnIndexOrThrow(RoutesColumns.ID);
				int col_number = c.getColumnIndexOrThrow(RoutesColumns.NUMBER);
				int col_destination = c.getColumnIndexOrThrow(DestinationsColumns.DESTINATION);
				int col_direction = c.getColumnIndexOrThrow(DestinationsColumns.DIRECTION);
				
				dest.setId(c.getInt(col_id));
				dest.setRouteNumber(c.getString(col_number));
				dest.setDestination(c.getString(col_destination));
				dest.setUp(c.getInt(col_direction));
			
				destinations.add(dest);
			} 
			while(c.moveToNext());
		}
		
		c.close();
		db.close();
		return destinations;
	}
	
	
	// Get a stop from a given TramTracker ID
	public Destination getDestination(long destinationId) {
		db = getDatabase();

		Cursor c = db.query(TABLE_DESTINATIONS_JOIN_ROUTES, 
				new String[] { "destinations._id", "number", "destination", "direction"}, 
				"destinations._id = '"  + destinationId + "'", 
				null, 
				null, 
				null, 
				null);
	
		Destination dest = new Destination();
		
		if (c.moveToFirst()) {		
			int col_id = c.getColumnIndexOrThrow(RoutesColumns.ID);
			int col_number = c.getColumnIndexOrThrow(RoutesColumns.NUMBER);
			int col_destination = c.getColumnIndexOrThrow(DestinationsColumns.DESTINATION);
			int col_direction = c.getColumnIndexOrThrow(DestinationsColumns.DIRECTION);

			dest.setId(c.getInt(col_id));
			dest.setRouteNumber(c.getString(col_number));
			dest.setDestination(c.getString(col_destination));
			dest.setUp(c.getInt(col_direction));

		}
		c.close();
		db.close();
		
		return dest;
	}
	
	
	
	
	// Get a list of destinations for a given TramTracker ID
	public List<Route> getRoutesForStop(int tramTrackerId) {
		db = getDatabase();

		List<Route> routes = new ArrayList<Route>();
		
		Cursor c = db.query(TABLE_STOPS_JOIN_ROUTES, 
							new String[] { "routes._id AS route_id", "number"}, 
							StopsColumns.TRAMTRACKER_ID + " = '"  + tramTrackerId + "'", 
							null, 
							"routes._id", 
							null, 
							"routes._id", 
							null);
		
		if (c.moveToFirst()) {		
			do {	
				Route route = new Route();
				
				int col_id = c.getColumnIndexOrThrow("route_id");
				int col_number = c.getColumnIndexOrThrow(Routes.NUMBER);
				route.setId(c.getInt(col_id));
				route.setNumber(c.getString(col_number));

				routes.add(route);
			} 
			while(c.moveToNext());
		}
		
		c.close();
		db.close();
		return routes;
	}	
	
	
//	// Get a list of destinations for a given route
//	public List<Stop> getFavouriteStopsOnRoute(Stop origin, Route route) {
//		db = getDatabase();
//
//		final List<Stop> stops = new ArrayList<Stop>();
//		
//		Cursor c = db.query(TABLE_STOPS_JOIN_ROUTES, 
//							new String[] { 
//								"stops._id AS _id", 
//								"stops.tramtracker_id AS tramtracker_id", 
//								"stops.flag_number AS flag_number", 
//								"stops.primary_name AS primary_name", 
//								"stops.secondary_name AS secondary_name", 
//								"stops.routes AS routes", 
//								"stops.city_direction AS city_direction", 
//								"stops.latitude AS latitude", 
//								"stops.longitude AS longitude", 
//								"stops.suburb AS suburb", 
//								"stops.starred AS starred"
//							}, 
//							String.format("stops.%s = 1 AND routes.%s = %s AND stops.%s <> %s", 
//									StopsColumns.STARRED,
//									RoutesColumns.ID, route.getId(), 
//									StopsColumns.TRAMTRACKER_ID, origin.getTramTrackerID()),
//							null, 
//							null, 
//							null, 
//							"destination_stops." + DestinationsStopsColumns.STOP_ORDER, 
//							null);
//		
//		if (c.moveToFirst()) {		
//			do {	
//				stops.add(getStopFromCursor(c));
//			} 
//			while(c.moveToNext());
//		}
//		
//		c.close();
//		db.close();
//		return stops;
//	}
	
	
	
	
	
	
	
	
	
	
	
	// Get a list of our 'starred' stops
	public List<Stop> getAllStops() {
		db = getDatabase();
		
		List<Stop> stops = new ArrayList<Stop>();
		
		Cursor c = db.query(TABLE_STOPS, 
							null, 
							null, 
							null, 
							null, 
							null, 
							null, 
							null);
	
		if (c.moveToFirst()) {		
			do {
				Stop stop = getStopFromCursor(c);
				
				stops.add(stop);
				
			} while(c.moveToNext());
		}
		
		c.close();
		db.close();
		return stops;
	}
	
	
	/**
	 * Get a list of favourite stops
	 */
	public List<Stop> getFavouriteStops(Context context) {
		List<Stop> stops = new ArrayList<Stop>();
		PreferenceHelper preferenceHelper = new PreferenceHelper(context);	

		String starredStopsString = preferenceHelper.getStarredStopsString();
		if (!starredStopsString.matches("")) {
			ArrayList<Integer> list = StringUtil.parseString(starredStopsString);
		
			for (int item : list) {
				Stop stop = getStop(item);
				stops.add(stop);
			}
			
		}
		
		return stops;
	}	
	
	
	
//	// Get a list of our 'starred' stops
//	public List<Stop> getFavouriteStops() {
//		db = getDatabase();
//		List<Stop> stops = getFavouriteStops(db);
//		db.close();
//		
//		return stops;
//	}	
//	
	
	
	
	
	private List<Integer> getOldFavouriteStops(SQLiteDatabase db) {
		List<Integer> favouriteStops = new ArrayList<Integer>();
		
		Cursor c = db.query(TABLE_STOPS, 
							new String[] { StopsColumns.TRAMTRACKER_ID }, 
							"starred = 1",
							null, 
							null, 
							null, 
							null, 
							null);
	
		if (c.moveToFirst()) {		
			do {
				int col_tramTrackerID = c.getColumnIndexOrThrow(StopsColumns.TRAMTRACKER_ID);
				int tramTrackerID = c.getInt(col_tramTrackerID);
				favouriteStops.add(tramTrackerID);
				
			} while(c.moveToNext());
		}
		
		c.close();
		return favouriteStops;
	}	
	

	// Get a list of Stops for a particular destination
	public List<Stop> getStopsForDestination(long destinationId) {
		db = getDatabase();
		
		List<Stop> stops = new ArrayList<Stop>();
		
		Cursor c = db.query(TABLE_STOPS_JOIN_DESTINATIONS, 
							null, 
							"destinations._id = '"  + destinationId + "'", 
							null, 
							"stops._id", 
							null, 
							DestinationsStopsColumns.STOP_ORDER, 
							null);

		if (c.moveToFirst()) {		
			do {
				Stop stop = getStopFromCursor(c);
				
				stops.add(stop);
				
			} while(c.moveToNext());
		}
		
		c.close();
		db.close();
		return stops;
	}
	
	
	// Get a list of Stops for a particular destination
	public List<Stop> getStopsForSearch(String searchString) {
		db = getDatabase();
		
		String searchQuery = StopsColumns.FLAG_NUMBER + " LIKE '%" + searchString 
								+ "%' OR " + StopsColumns.PRIMARY_NAME + " LIKE '%" + searchString + "%'"
								+ "OR " + StopsColumns.SECONDARY_NAME + " LIKE '%" + searchString + "%'"
								+ "OR " + StopsColumns.TRAMTRACKER_ID + " LIKE '%" + searchString + "%'";
		
		String searchLimit = "100";
		
		List<Stop> stops = new ArrayList<Stop>();
		
		Cursor c = db.query(TABLE_STOPS, 
							null, 
							searchQuery, 
							null, 
							null, 
							null, 
							null, 
							searchLimit);

		if (c.moveToFirst()) {		
			do {
				Stop stop = getStopFromCursor(c);
				
				stops.add(stop);
				
			} while(c.moveToNext());
		}
		
		c.close();
		db.close();
		return stops;
	}
	
	
	
	
	
	
	
	
	// Get a stop object from a given TramTracker ID
	public Stop getStop(int tramTrackerId) {
		db = getDatabase();

		Cursor c = db.query(TABLE_STOPS, 
							null, 
							StopsColumns.TRAMTRACKER_ID + " = '"  + tramTrackerId + "'", 
							null, 
							null, null, null);	
	
		Stop stop = new Stop();
		
		if (c.moveToFirst()) {		
			int col_id = c.getColumnIndexOrThrow(StopsColumns.ID);
			int col_tramTrackerID = c.getColumnIndexOrThrow(StopsColumns.TRAMTRACKER_ID);
			int col_flagStopNumber = c.getColumnIndexOrThrow(StopsColumns.FLAG_NUMBER);
			int col_primaryName = c.getColumnIndexOrThrow(StopsColumns.PRIMARY_NAME);
			int col_secondaryName = c.getColumnIndexOrThrow(StopsColumns.SECONDARY_NAME);
			int col_routesString = c.getColumnIndexOrThrow(StopsColumns.ROUTES);
			int col_cityDirection = c.getColumnIndexOrThrow(StopsColumns.CITY_DIRECTION);
			int col_latitude = c.getColumnIndexOrThrow(StopsColumns.LATITUDE);
			int col_longitude = c.getColumnIndexOrThrow(StopsColumns.LONGITUDE);
			int col_suburb = c.getColumnIndexOrThrow(StopsColumns.SUBURB);

			stop.setId(c.getInt(col_id));
			stop.setTramTrackerID(c.getInt(col_tramTrackerID));
			stop.setFlagStopNumber(c.getString(col_flagStopNumber));
			stop.setPrimaryName(c.getString(col_primaryName));
			stop.setSecondaryName(c.getString(col_secondaryName));
			stop.setRoutesString(c.getString(col_routesString));
			stop.setCityDirection(c.getString(col_cityDirection));
			stop.setLatitude(c.getDouble(col_latitude));
			stop.setLongitude(c.getDouble(col_longitude));
			stop.setSuburb(c.getString(col_suburb));

		}
		c.close();
		db.close();
		return stop;
	}
	
	// Check to see if a stop exists
	public boolean checkStop(int tramTrackerId){
		db = getDatabase();		
		
		Cursor c = db.query(TABLE_STOPS, 
							new String[] { StopsColumns.TRAMTRACKER_ID }, 
							StopsColumns.TRAMTRACKER_ID + " = '" + tramTrackerId + "'", 
							null, 
							null, 
							null, 
							null);
		
		int numRows = c.getCount();
		c.moveToFirst();
		boolean returnValue = false;
		if (numRows == 1){
			returnValue = (c.getInt(0) != 0);
		}
		c.close();
		db.close();
			
		return returnValue;
	}
	
	// 
//	public boolean getStopStar(int tramTrackerId) {
//		db = getDatabase();
//
//		Cursor c = db.query(TABLE_STOPS, 
//							new String[] { "starred" }, 
//							StopsColumns.TRAMTRACKER_ID + " = '"  + tramTrackerId + "'", 
//							null, 
//							null, 
//							null, 
//							null);
//		
//		int numRows = c.getCount();
//		c.moveToFirst();
//		
//		boolean returnValue = false;
//		if (numRows == 1){
//			returnValue = (c.getInt(0) != 0);
//		}
//	
//		return returnValue;
//		
//	}
//	
//	
//	// Allow a DB query based on the TramTrackerID
//	public void setStopStar(int tramTrackerId, boolean star) {	
//		db = getDatabase();
//		
//		int starred = 0;
//		if (star) { 
//			starred = 1;
//		}
//		
//		ContentValues values = new ContentValues();
//		values.put("starred", starred);
//		
//		db.update(TABLE_STOPS, 
//				values, 
//				StopsColumns.TRAMTRACKER_ID + " = '" + tramTrackerId + "'", 
//				null);
//		
//		db.close();
//	}
//	
//	// Set the 'read' flag in the database
//	public void setFirstLaunch() {
//		db = getDatabase();
//		ContentValues values = new ContentValues();
//		values.put("id", 0);
//		values.put("read", 1);
//		db.insert(TABLE_FIRST_LAUNCH, null, values);
//		db.close();
//	}
//	
//	
//	// Return a bool for whether the opening message dialog has been read
//	public boolean checkFirstLaunch(){
//		db = getDatabase();	
//		
//		Cursor c = db.query(TABLE_FIRST_LAUNCH, 
//							new String[] { "read" }, 
//							"id=0", 
//							null, 
//							null, 
//							null, 
//							null);
//		
//		int numRows = c.getCount();
//		c.moveToFirst();
//		
//		boolean returnValue = false;
//		if (numRows == 1){
//			returnValue = (c.getInt(0) != 0);
//		}
//		c.close();
//		db.close();
//			
//		return returnValue;
//	}
	
	private Stop getStopFromCursor(Cursor c) {
		Stop stop = new Stop();

		int col_tramTrackerID = c.getColumnIndexOrThrow(StopsColumns.TRAMTRACKER_ID);
		int col_flagStopNumber = c.getColumnIndexOrThrow(StopsColumns.FLAG_NUMBER);
		int col_primaryName = c.getColumnIndexOrThrow(StopsColumns.PRIMARY_NAME);
		int col_secondaryName = c.getColumnIndexOrThrow(StopsColumns.SECONDARY_NAME);
		int col_routesString = c.getColumnIndexOrThrow(StopsColumns.ROUTES);
		int col_cityDirection = c.getColumnIndexOrThrow(StopsColumns.CITY_DIRECTION);
		int col_latitude = c.getColumnIndexOrThrow(StopsColumns.LATITUDE);
		int col_longitude = c.getColumnIndexOrThrow(StopsColumns.LONGITUDE);
		int col_suburb = c.getColumnIndexOrThrow(StopsColumns.SUBURB);

		stop.setTramTrackerID(c.getInt(col_tramTrackerID));
		stop.setFlagStopNumber(c.getString(col_flagStopNumber));
		stop.setPrimaryName(c.getString(col_primaryName));
		stop.setSecondaryName(c.getString(col_secondaryName));
		stop.setRoutesString(c.getString(col_routesString));
		stop.setCityDirection(c.getString(col_cityDirection));
		stop.setLatitude(c.getDouble(col_latitude));
		stop.setLongitude(c.getDouble(col_longitude));
		stop.setSuburb(c.getString(col_suburb));
		return stop;
	}	
	
//	// Set the GUID in our database for next time
//	public void setGUID(String guid) {
//		db = getDatabase();
//		ContentValues values = new ContentValues();
//		values.put("id", 0);
//		values.put("guid", guid);
//		db.insert(TABLE_GUID, null, values);
//		db.close();
//	}
//	
//	
//	// Return a String of the GUID value generated from TramTracker
//	public String getGUID(){
//		db = getDatabase();	
//		
//		Cursor c = db.query(TABLE_GUID, 
//							new String[] { "guid" }, 
//							"id=0", 
//							null, 
//							null, 
//							null, 
//							null);
//		
//		int numRows = c.getCount();
//		c.moveToFirst();
//		String guid = "";
//
//		if (numRows == 1){
//			guid = c.getString(0);
//		}
//
//		c.close();
//		db.close();
//		return guid;
//	}
	
	// Get the tram class based on the number
	public String getTramClass(int vehicleNo) {
		db = getDatabase();
		
		Cursor c = db.query(TABLE_TRAMS, 
				new String[] { TramsColumns.CLASS }, 
				TramsColumns.NUMBER + " = '" + vehicleNo + "'", 
				null, 
				null, 
				null, 
				null);
		
		String tramClass = "";
		
		if (c.moveToFirst()) {		
			int col_class = c.getColumnIndexOrThrow(TramsColumns.CLASS);
			tramClass = c.getString(col_class);
		}
		else {
			tramClass = null;
		}
		
		c.close();
		db.close();
		return tramClass;
	}
	
	
//	// Set the statsdate in our database
//	public void setStatsDate() {
//		db = getDatabase();
//		ContentValues values = new ContentValues();
//		values.put("id", 0);
//		values.put("statsdate", System.currentTimeMillis());
//		db.insert(TABLE_STATS, null, values);
//		db.close();
//	}
//
//	
//	public long getStatsDate() {
//		db = getDatabase();
//		Cursor c = db.query(TABLE_STATS, 
//				new String[] { "statsdate" }, 
//				"id=0", 
//				null, 
//				null, 
//				null, 
//				null);
//
//		int numRows = c.getCount();
//		c.moveToFirst();
//		long returnValue = 0;
//		if (numRows == 1){
//			returnValue = c.getLong(0);
//		}
//		c.close();
//		db.close();
//		return returnValue;
//	}
	
	
	

	// Database column definitions
	 public static interface StopsColumns {
		public static final String ID = "_id";
		public static final String TRAMTRACKER_ID = "tramtracker_id";
		public static final String FLAG_NUMBER = "flag_number";
		public static final String PRIMARY_NAME = "primary_name";
		public static final String SECONDARY_NAME = "secondary_name";
		public static final String ROUTES = "routes";
		public static final String CITY_DIRECTION = "city_direction";
		public static final String LATITUDE = "latitude";
		public static final String LONGITUDE = "longitude";
		public static final String SUBURB = "suburb";
	 }

	 public static interface DestinationsColumns {
		public static final String ID = "_id";
		public static final String DESTINATION = "destination";
		public static final String ROUTE_ID = "route_id";
		public static final String DIRECTION = "direction";
	 }
		
	 public static interface RoutesColumns {
		public static final String ID = "_id";
		public static final String NUMBER = "number";
	 }	 
	 
	 public static interface DestinationsStopsColumns {
		 public static final String ID = "_id";
		 public static final String DESTINATION_ID = "route_id";
		 public static final String STOP_ID = "stop_id";
		 public static final String STOP_ORDER = "stop_order";
	 }

	 public static interface TramsColumns {
			public static final String ID = "_id";
			public static final String NUMBER = "number";
			public static final String CLASS = "class";
	 }	 	 
		
	public static class Stops implements BaseColumns, StopsColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/stops/");
		public static final Uri CONTENT_ROUTES_URI = Uri.parse("content://" + AUTHORITY + "/stops/route/");
			
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/com.andybotting.dbcopytest.stop";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/com.andybotting.dbcopytest.stop";

		public static final String CONTENT_ROUTES_TYPE = "vnd.android.cursor.item/com.andybotting.dbcopytest.routestop";
	}

	public static class Routes implements BaseColumns, RoutesColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/routes/");

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/com.andybotting.dbcopytest.route";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/com.andybotting.dbcopytest.route";
			
	}

	
}

