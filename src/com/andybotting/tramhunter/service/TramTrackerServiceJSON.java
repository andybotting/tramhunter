package com.andybotting.tramhunter.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.andybotting.tramhunter.objects.NextTram;
import com.andybotting.tramhunter.objects.Route;
import com.andybotting.tramhunter.objects.Stop;

public class TramTrackerServiceJSON implements TramTrackerService {

    private static final String TAG = "TramTrackerServiceJSON";
    private static final boolean LOGV = Log.isLoggable(TAG, Log.INFO);
	
	private static final String BASE_URL = "http://extranetdev.yarratrams.com.au/pidsservicejson/Controller";

	// Not until we use this interface properly 
	//private static final String CLIENTTYPE = "TRAMHUNTER";
	//private static final String CLIENTVERSION = "0.9.0";
	//private static final String CLIENTWEBSERVICESVERSION = "6.4.0.0";
	
	private Context mContext;

	// Not needed until we start using the GUID
	//private PreferenceHelper mPreferenceHelper;
	
	String guid = "";

	public TramTrackerServiceJSON(Context context) {
		this.mContext = context;
		//this.mPreferenceHelper = new PreferenceHelper(mContext);
	}
	
	/**
	 * Fetch JSON data over HTTP
	 */
	public InputStream getJSONData(String url){
        DefaultHttpClient httpClient = new DefaultHttpClient();
        
        // Set the user agent
        String packageName = "Unknown";
        String packageVersion = "Unknown";
        
		try {
			packageName = mContext.getPackageName();
			PackageInfo pi = mContext.getPackageManager().getPackageInfo(packageName, 0);
			packageVersion = pi.versionName;
		} 
		catch (NameNotFoundException e) {
			// Nope
		}

        httpClient.getParams().setParameter("http.useragent", packageName + " " + packageVersion);
        
        URI uri;
        InputStream data = null;
        try {
            uri = new URI(url);
            HttpGet method = new HttpGet(uri);
            HttpResponse response = httpClient.execute(method);
            data = response.getEntity().getContent();
        } catch (Exception e) {
            e.printStackTrace();
        }
       

        return data;
    }
	
	
    /**
     * Parse the given {@link InputStream} into a {@link JSONObject}.
     */
    private static JSONObject parseJsonStream(InputStream is) throws IOException, JSONException {
    	JSONObject jsonObject = null;
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
            jsonObject = new JSONObject(jsonData);
    	} 
    	catch(Exception e){
    		e.printStackTrace();
    	}
		return jsonObject;
    }

    
    /**
     * Parse the given {@link InputStream} into {@link Stop}
     * assuming a JSON format.
     * @return Stop
     */
    public static Stop parseStopInformation(InputStream is) throws IOException, JSONException {
		//	{
		//	   "responseObject":[
		//	      {
		//	         "FlagStopNo":"14",
		//	         "StopName":"Royal Melbourne Hospital & Flemington Rd",
		//	         "CityDirection":"towards City",
		//	         "Latitude":-37.799511541211,
		//	         "Longitude":144.95492172036,
		//	         "SuburbName":"Parkville",
		//	         "IsCityStop":false,
		//	         "HasConnectingBuses":true,
		//	         "HasConnectingTrains":false,
		//	         "HasConnectingTrams":false,
		//	         "StopLength":31,
		//	         "IsPlatformStop":true,
		//	         "Zones":"0,1"
		//	      }
		//	   ],
		//	   "isError":false
		//	}
        Stop stop = new Stop();
    	
        // Parse incoming JSON stream
        JSONObject stopData = parseJsonStream(is);
        JSONObject responseObject = stopData.getJSONObject("responseObject");
            
        String flagStopNo = responseObject.getString("FlagStopNo");
        String stopName = responseObject.getString("StopName");
        String cityDirection = responseObject.getString("CityDirection");
        String suburbName = responseObject.getString("SuburbName");

		stop.setFlagStopNumber(flagStopNo);
		stop.setStopName(stopName);
		stop.setCityDirection(cityDirection);
		stop.setSuburb(suburbName);
        
    	return stop;
    }
	
    
	/**
	 * Get tram stop information for a given TramTracker ID
	 */
	public Stop getStopInformation(int tramTrackerID) {
		Stop stop = null;
		String url = BASE_URL + "/GetStopInformation.aspx?s=" + tramTrackerID;
		InputStream httpData = getJSONData(url);
		
		try {
			stop = parseStopInformation(httpData);
			stop.setTramTrackerID(tramTrackerID);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return stop;
	}
	
	

    /**
     * Parse the given {@link InputStream} into {@link Stop}
     * assuming a JSON format.
     * @return Stop
     */
    public static List<NextTram> parseNextPredictedRoutesCollection(InputStream is) throws IOException, JSONException {

		//	[{
		//	    "responseObject": [
		//	        {
		//	            "TripID": null,
		//	            "InternalRouteNo": 55,
		//	            "RouteNo": "55",
		//	            "HeadboardRouteNo": "55",
		//	            "VehicleNo": 2003,
		//	            "Destination": "Domain Interchange",
		//	            "HasDisruption": false,
		//	            "IsTTAvailable": true,
		//	            "IsLowFloorTram": false,
		//	            "AirConditioned": true,
		//	            "DisplayAC": false,
		//	            "HasSpecialEvent": false,
		//	            "SpecialEventMessage": "Bus replacement Rte 59 btw Stop 57 Hawker St & Stop 59 Airport West til approx 7am Mon.",
		//	            "PredictedArrivalDateTime": "\/Date(1305382369000+1000)\/",
		//	            "RequestDateTime": "\/Date(1305381485739+1000)\/"
		//	        },
		//	        {
		//	            "TripID": null,
		//	            "InternalRouteNo": 59,
		//	            "RouteNo": "59",
		//	            "HeadboardRouteNo": "59",
		//	            "VehicleNo": 2025,
		//	            "Destination": "Flinders St City",
		//	            "HasDisruption": false,
		//	            "IsTTAvailable": true,
		//	            "IsLowFloorTram": false,
		//	            "AirConditioned": true,
		//	            "DisplayAC": false,
		//	            "HasSpecialEvent": true,
		//	            "SpecialEventMessage": "Bus replacement Rte 59 btw Stop 57 Hawker St & Stop 59 Airport West til approx 7am Mon.",
		//	            "PredictedArrivalDateTime": "\/Date(1305384840000+1000)\/",
		//	            "RequestDateTime": "\/Date(1305381485739+1000)\/"
		//	        }
		//	    ],
		//	    "isError": false
		//	}]      	
    	
    	List<NextTram> nextTrams = new ArrayList<NextTram>();
    	
        // Parse incoming JSON stream
        JSONObject nextTramsData = parseJsonStream(is);	
        JSONArray nextTramsArray = nextTramsData.getJSONArray("responseObject");
        int nextTramsCount = nextTramsArray.length();
        for (int i = 0; i < nextTramsCount; i++) {
        	
        	JSONObject responseObject = nextTramsArray.getJSONObject(i);
        	

            int internalRouteNo = responseObject.getInt("InternalRouteNo");
            String routeNo = responseObject.getString("RouteNo");
            String headboardRouteNo = responseObject.getString("HeadboardRouteNo");
            int vehicleNo = responseObject.getInt("VehicleNo");
            String destination = responseObject.getString("Destination");
            boolean hasDisruption = responseObject.getBoolean("HasDisruption");
            boolean isTTAvailable = responseObject.getBoolean("IsTTAvailable");
            boolean isLowFloorTram = responseObject.getBoolean("IsLowFloorTram");
            boolean airConditioned = responseObject.getBoolean("AirConditioned");
            boolean displayAC = responseObject.getBoolean("DisplayAC");
            boolean hasSpecialEvent = responseObject.getBoolean("HasSpecialEvent");
            String specialEventMessage = responseObject.getString("SpecialEventMessage");
            // Parse dates
        	String predictedArrivalDateTimeString = responseObject.getString("PredictedArrivalDateTime");
            Date predictedArrivalDateTime = parseTimestamp(predictedArrivalDateTimeString);
            String requestDateTimeString = responseObject.getString("RequestDateTime");
            Date requestDateTime = parseTimestamp(requestDateTimeString);

            NextTram tram = new NextTram();
            
			tram.setInternalRouteNo(internalRouteNo);
			tram.setRouteNo(routeNo);
			tram.setHeadboardRouteNo(headboardRouteNo);
			tram.setVehicleNo(vehicleNo);
			tram.setDestination(destination);
			tram.setHasDisruption(hasDisruption);
			tram.setIsTTAvailable(isTTAvailable);
			tram.setIsLowFloorTram(isLowFloorTram);
			tram.setAirConditioned(airConditioned);
			tram.setDisplayAC(displayAC);
			tram.setHasSpecialEvent(hasSpecialEvent);
			tram.setSpecialEventMessage(specialEventMessage);
			tram.setPredictedArrivalDateTime(predictedArrivalDateTime);
			tram.setRequestDateTime(requestDateTime);
			
			nextTrams.add(tram);
			
        }
        
		return nextTrams;
    	
    }
	
    /**
     * Get the list of next trams for a given stop and route
     */
	public List<NextTram> getNextPredictedRoutesCollection (Stop stop, Route route) {

		List<NextTram> nextTrams = null;
	
		//  http://extranetdev.yarratrams.com.au/pidsservicejson/Controller/GetNextPredictedRoutesCollection.aspx?s=3173&r=0
		StringBuffer url = new StringBuffer();
		url.append(BASE_URL);
		url.append("/GetNextPredictedRoutesCollection.aspx");
		url.append("?s=" + stop.getTramTrackerID());
		
		// Filter the results by route
		if (route == null)
			url.append("&r=0");
		else
			url.append("&r=" + route.getNumber());
		
		InputStream httpData = getJSONData(url.toString());
		
		try {
			nextTrams = parseNextPredictedRoutesCollection(httpData);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return nextTrams;
				
	}
	
	
	/**
	 * Convert the JSON timestamp string to a java date object
	 * @param timestamp
	 * @return date
	 */
	private static Date parseTimestamp(String timestamp) {
		//"PredictedArrivalDateTime": "\/Date(1305384840000+1000)\/",
		Long fixedDate = Long.parseLong(timestamp.substring(6, 19));
		if (LOGV) Log.v(TAG, "Date string: " + fixedDate);
		Date date = new Date();
		date.setTime(fixedDate);
		return date;
	}
	

	public String getGUID() {
		return guid;
	}


}
