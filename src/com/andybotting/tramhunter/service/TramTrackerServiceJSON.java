package com.andybotting.tramhunter.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
     * Parse the given {@link InputStream} into a {@link JSONArray}.
     */
    private static JSONArray parseJsonStream(InputStream is) throws IOException, JSONException {
        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        return new JSONArray(new String(buffer));
    }

    /**
     * Parse the given {@link InputStream} into {@link Stop}
     * assuming a JSON format.
     * @return Stop
     */
    public static Stop parseStopInformation(InputStream is) throws IOException, JSONException {

        Stop stop = new Stop();
    	
        // Parse incoming JSON stream
        JSONArray stopData = parseJsonStream(is);
        
        int stopCount = stopData.length();
        for (int i = 0; i < stopCount; i++) {
            JSONObject stopItem = stopData.getJSONObject(i);
            
            JSONObject responseObject = stopItem.getJSONObject("responseObject");
            
            String flagStopNo = responseObject.getString("FlagStopNo");
            String stopName = responseObject.getString("StopName");
            String cityDirection = responseObject.getString("CityDirection");
            String suburbName = responseObject.getString("SuburbName");

			stop.setFlagStopNumber(flagStopNo);
			stop.setStopName(stopName);
			stop.setCityDirection(cityDirection);
			stop.setSuburb(suburbName);

        }
        
    	return stop;
    }

	/**
	 * Convert the timestamp string to a java date object
	 * @param timestamp
	 * @return date
	 */
	private static Date parseTimestamp(String timestamp) {
		//"PredictedArrivalDateTime": "\/Date(1305384840000+1000)\/",

		String fixedDate = timestamp.substring(7, 19);
		Date date = new Date(fixedDate);
		
		return date;
	}
	
	
	/**
	 * Get tram stop information for a given TramTracker ID
	 */
	public Stop getStopInformation(int tramTrackerID) {
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

    	List<NextTram> nextTrams = new ArrayList<NextTram>();
    	
        // Parse incoming JSON stream
        JSONArray nextTramsData = parseJsonStream(is);	
    	
        int nextTramsCount = nextTramsData.length();
        for (int i = 0; i < nextTramsCount; i++) {
        	
            JSONObject nextTramObject = nextTramsData.getJSONObject(i);
            JSONObject responseObject = nextTramObject.getJSONObject("responseObject");
            
    		//	{
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
    		//	}            
            
            NextTram tram = new NextTram();
		
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
            
            String predictedArrivalDateTimeString = responseObject.getString("PredictedArrivalDateTime");
            String requestDateTimeString = responseObject.getString("RequestDateTime");
            
            Date predictedArrivalDateTime = parseTimestamp(predictedArrivalDateTimeString);
            Date requestDateTime = parseTimestamp(requestDateTimeString);
            
//			tram.setInternalRouteNo(internalRouteNo);
//			tram.setRouteNo(routeNo);
//			tram.setHeadboardRouteNo(headboardRouteNo);
//			tram.setVehicleNo(Integer.parseInt(vehicleNo));
//			tram.setDestination(destination);
//			tram.setHasDisruption(hasDisruption);
//			tram.setIsTTAvailable(isTTAvailable);
//			tram.setIsLowFloorTram(isLowFloorTram);
//			tram.setAirConditioned(airConditioned);
//			tram.setDisplayAC(displayAC);
//			tram.setHasSpecialEvent(hasSpecialEvent);
//			tram.setSpecialEventMessage(specialEventMessage);
//			tram.setPredictedArrivalDateTime(predictedArrivalDateTime);
//			tram.setRequestDateTime(requestDateTime);
			
			nextTrams.add(tram);
			
        }
        
		return nextTrams;
    	
    }
	
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

	public String getGUID() {
		return guid;
	}


}
