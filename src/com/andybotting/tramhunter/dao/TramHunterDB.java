package com.andybotting.tramhunter.dao;

import java.io.File;
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
import android.os.Environment;
import android.provider.BaseColumns;
import android.util.Log;

import com.andybotting.tramhunter.objects.Destination;
import com.andybotting.tramhunter.objects.Route;
import com.andybotting.tramhunter.objects.Stop;

import com.andybotting.tramhunter.util.PreferenceHelper;
import com.andybotting.tramhunter.util.StringUtil;

public class TramHunterDB extends SQLiteOpenHelper {
	
    private static final String TAG = "TramHunterDB";
    private static final boolean LOGV = Log.isLoggable(TAG, Log.INFO);
	 
	private static final String AUTHORITY = "com.andybotting.tramhunter";
	private static final String DATABASE_NAME = "tramhunter.db";
	
	private static final String DATABASE_INTERNAL_PATH = "/data/data/"+ AUTHORITY + "/databases/";
    
	// Update this with the App Version Code (App Version x 100)
	// E.g. 
	// 	App Version v0.1.00 = DB Version 100
	//  App Version v0.2.92 = DB Version 292
	// 	App Version v1.2.0 = DB Version 1200
	private static final int DATABASE_VERSION = 800;
	
	
	private SQLiteDatabase mDB = null;
	private Context mContext;
	private boolean mIsInitializing = false;

	// Are we using the internal database?
	private boolean mIsDBInternal = true;
	
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
	
	
    public TramHunterDB(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		mContext = context;
		if (LOGV) Log.v(TAG, "Instantiating TramHunter database");
	}	
	
	public SQLiteDatabase getDatabase() {
		SQLiteDatabase db;  
		
		if (LOGV) Log.v(TAG, "Getting DB");
		db = getExternalStorageDB();
		if (db == null) {
			if (LOGV) Log.v(TAG, "DB from SD Card failed, using internal");
			db = getInternalStorageDB();
		}

		return db;
	}
	
	/**
	 * Return the SQL Database from the internal device storage
	 * @return
	 */
	private SQLiteDatabase getInternalStorageDB() {
		if (LOGV) Log.v(TAG, "Getting DB from device internal storage");

		SQLiteDatabase db = null;
		String dbFile = DATABASE_INTERNAL_PATH + DATABASE_NAME;

		try {		 	
			this.createDB(dbFile);
		} 
		catch (IOException ioe) {
			throw new Error("Unable to create database:" + ioe);
		}

		try {
			this.openDB(dbFile);
		}
		catch(SQLException sqle){
			throw sqle;
		}

		mIsDBInternal = true;
		db = getWritableDatabase(dbFile);
		return db;
	}


	/**
	 * Return the SQL Database from external storage
	 */
	private SQLiteDatabase getExternalStorageDB() {
		if (LOGV) Log.v(TAG, "Getting DB from external storage (SD Card)");

		SQLiteDatabase db = null;

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            return null;

        // Build the directory on the SD Card, if it doesn't exist
        File appDbDir = new File(Environment.getExternalStorageDirectory(), "Android/data/" + AUTHORITY + "/files");
        if (!appDbDir.exists()) {
        	if (LOGV) Log.v(TAG, "Making dirs");
        	appDbDir.mkdirs();
        }

        // Our dbFile at /mnt/sdcard/Android/data/com.andybotting.tubechaser/files/tubechaser.db
        File dbFileObj = new File(appDbDir, DATABASE_NAME);
        String dbFile = dbFileObj.getAbsolutePath();
        
		try {	 	
			this.createDB(dbFile);
		} 
		catch (IOException ioe) {
			throw new Error("Unable to create database:" + ioe);
		}

		try {
			this.openDB(dbFile);
		}
		catch(SQLException sqle){
			throw sqle;
		}

		mIsDBInternal = false;
		db = getWritableDatabase(dbFile);
		return db;
	}
	
	/**
	 * Create the initial database at a given file path
	 * @param dbFile - a String representing the absolute file name
	 * @throws IOException
	 */
	public void createDB(String dbFile) throws IOException{

		boolean dbExist = checkDB(dbFile);
 
		if (dbExist) {
			if (LOGV) Log.v(TAG, "Found existing DB at " + dbFile);

			mDB = SQLiteDatabase.openDatabase(dbFile, null, SQLiteDatabase.OPEN_READONLY);
			int thisDBVersion = mDB.getVersion();
			mDB.close();
			
			if (LOGV) Log.v(TAG, "Current DB Version: v" + thisDBVersion + " - Shipped DB Version is v" + DATABASE_VERSION);
			if (thisDBVersion < DATABASE_VERSION) {							
				try {
					copyDB(dbFile);
				} catch (IOException e) {
					throw new Error("Error copying database");
				}
			}	
		}
		else {
			if (LOGV) Log.v(TAG, "Creating a new DB at " + dbFile);
			// By calling this method and empty database will be created into the default system path
			// of your application so we are gonna be able to overwrite that database with our database.
			mDB = getReadableDatabase(dbFile);
			
			try {
				copyDB(dbFile);
			} catch (IOException e) {
				throw new Error("Error copying database: " + e);
			}
		}
	}
 
	
	/**
	 * Check if the database already exist to avoid re-copying the file each time you open the application.
	 * @return true if it exists, false if it doesn't
	 */
	private boolean checkDB(String dbFile){
 		SQLiteDatabase checkDB = null;
 		if (LOGV) Log.v(TAG, "Checking for an existing DB at " + dbFile);
 		
		try {
			checkDB = SQLiteDatabase.openDatabase(dbFile, null, SQLiteDatabase.OPEN_READONLY);
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
	private void copyDB(String dbFile) throws IOException {
		
		if (LOGV) Log.v(TAG, "Copying packaged DB to " + dbFile);
		
		// Open your local db as the input stream
		InputStream is = mContext.getAssets().open(DATABASE_NAME);

		// Open the empty db as the output stream
		OutputStream os = new FileOutputStream(dbFile);
		
		// Transfer bytes from the inputfile to the outputfile
		byte[] buffer = new byte[1024];
		int length;
		while ( (length = is.read(buffer) ) > 0) {
			os.write(buffer, 0, length);
		}

		// Close the streams
		os.flush();
		os.close();
		is.close();
		
		if (LOGV) Log.v(TAG, "DB copying completed");
	}

	public void openDB(String dbFile) throws SQLException {
	 	// Open the database
		mDB = SQLiteDatabase.openDatabase(dbFile, null, SQLiteDatabase.OPEN_READONLY);
		// Close the DB to prevent a leak
		mDB.close();
	}

	@Override
	public synchronized void close() {
		if(mDB != null)
			mDB.close();
		super.close();
	}
 
	@Override
	public void onCreate(SQLiteDatabase db) {
		if (LOGV) Log.v(TAG, "DB onCreate() called");
		// Do nothing here
	}
 
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (LOGV) Log.v(TAG, "DB onUpgrade() called");
		// Do nothing here
	}
	
	public synchronized SQLiteDatabase getReadableDatabase(String dbFile) {
		if(mIsDBInternal) {
			return super.getReadableDatabase();
		}
		
		
		if (mDB != null && mDB.isOpen()) {
			return mDB; // The database is already open for business
		}

		if (mIsInitializing) {
			throw new IllegalStateException("getReadableDatabase called recursively");
		}

		try {
			return getWritableDatabase();
		} catch (SQLiteException e) {
			Log.e(TAG, "Couldn't open " + DATABASE_NAME + " for writing (will try read-only):", e);
		}

		SQLiteDatabase db = null;
		try {
			mIsInitializing = true;
			db = SQLiteDatabase.openDatabase(dbFile, null, SQLiteDatabase.OPEN_READONLY);
			if (db.getVersion() != DATABASE_VERSION) {
				throw new SQLiteException("Can't upgrade read-only database from version " + db.getVersion() + " to " + DATABASE_VERSION + ": " + dbFile);
			}

			onOpen(db);
			if (LOGV) Log.v(TAG, "Opened " + DATABASE_NAME + " in read-only mode");
			mDB = db;
			return mDB;
		} finally {
			mIsInitializing = false;
			if (db != null && db != mDB)
				db.close();
		}
	}


	public synchronized SQLiteDatabase getWritableDatabase(String dbFile) {
		if(mIsDBInternal) {
			return super.getWritableDatabase();
		}
		if (mDB != null && mDB.isOpen() && !mDB.isReadOnly()) {
			return mDB; // The database is already open for business
		}

		if (mIsInitializing) {
			throw new IllegalStateException("getWritableDatabase called recursively");
		}

		// If we have a read-only database open, someone could be using it
		// (though they shouldn't), which would cause a lock to be held on
		// the file, and our attempts to open the database read-write would
		// fail waiting for the file lock. To prevent that, we acquire the
		// lock on the read-only database, which shuts out other users.

		boolean success = false;
		SQLiteDatabase db = null;
		// if (mDatabase != null) mDatabase.lock(); //can't call the locks for
		// some reason. beginTransaction does lock it though
		try {
			mIsInitializing = true;
			db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
			int version = db.getVersion();
			if (version != DATABASE_VERSION) {
				db.beginTransaction();
				try {
					if (version == 0) {
						onCreate(db);
					} else {
						onUpgrade(db, version, DATABASE_VERSION);
					}
					db.setVersion(DATABASE_VERSION);
					db.setTransactionSuccessful();
				} finally {
					db.endTransaction();
				}
			}

			onOpen(db);
			success = true;
			return db;
		} finally {
			mIsInitializing = false;
			if (success) {
				if (mDB != null) {
					try {
						mDB.close();
					} catch (Exception e) {
					}
					// mDatabase.unlock();
				}
				mDB = db;
			} else {
				// if (mDatabase != null) mDatabase.unlock();
				if (db != null)
					db.close();
			}
		}
	}
	
	
	// Get a list of destinations
	public List<Route> getRoutes() {
		SQLiteDatabase db = getDatabase();
	
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
		SQLiteDatabase db = getDatabase();

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
		SQLiteDatabase db = getDatabase();

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
		SQLiteDatabase db = getDatabase();

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
		SQLiteDatabase db = getDatabase();
		
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
	public ArrayList<Stop> getFavouriteStops(Context context) {
		ArrayList<Stop> stops = new ArrayList<Stop>();
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
		SQLiteDatabase db = getDatabase();
		
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
		SQLiteDatabase db = getDatabase();
		
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
		SQLiteDatabase db = getDatabase();

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
		SQLiteDatabase db = getDatabase();		
		
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
	

	// Get the tram class based on the number
	public String getTramClass(int vehicleNo) {
		SQLiteDatabase db = getDatabase();
		
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

