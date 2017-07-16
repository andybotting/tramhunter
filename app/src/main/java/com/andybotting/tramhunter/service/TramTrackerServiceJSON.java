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

package com.andybotting.tramhunter.service;

import android.util.Log;

import com.andybotting.tramhunter.dao.TramHunterDB;
import com.andybotting.tramhunter.objects.NextTram;
import com.andybotting.tramhunter.objects.Route;
import com.andybotting.tramhunter.objects.Stop;
import com.andybotting.tramhunter.objects.TramRun;
import com.andybotting.tramhunter.objects.TramRunTime;
import com.andybotting.tramhunter.util.PreferenceHelper;
import com.andybotting.tramhunter.util.UserAgent;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class TramTrackerServiceJSON implements TramTrackerService {

	private static final String TAG = "TTServiceJSON";
	private static final boolean LOGV = Log.isLoggable(TAG, Log.INFO);

	// Production
	private static final String BASE_URL = "http://ws2.tramtracker.com.au/TramTracker/RestService";
	// Development
	//private static final String BASE_URL = "http://extranetdev.yarratrams.com.au/PIDSServiceWCF/RestService";

	private static final String CLIENT_TYPE = "TRAMHUNTER";

	private PreferenceHelper mPreferenceHelper;
	private TramHunterDB mDB;

	public TramTrackerServiceJSON() {
		this.mPreferenceHelper = new PreferenceHelper();
	}


	/**
	 * Fetch JSON data over HTTP
	 * 
	 * @throws TramTrackerServiceException
	 */
	public InputStream getJSONData(String url, List<NameValuePair> params) throws TramTrackerServiceException {

		DefaultHttpClient httpClient = new DefaultHttpClient();
		String userAgent = UserAgent.getUserAgent();
		httpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, userAgent);

		// TramTracker required parameters
		if (params == null) params = new LinkedList<NameValuePair>();

		params.add(new BasicNameValuePair("aid", CLIENT_TYPE));

		String guid = getGUID();
		params.add(new BasicNameValuePair("tkn", guid));

		String paramString = URLEncodedUtils.format(params, "utf-8");

		try {
			URI uri = new URI(url + "?" + paramString);
			if (LOGV) Log.d(TAG, "Fetching URL: " + uri.toString());
			HttpGet method = new HttpGet(uri);
			HttpResponse response = httpClient.execute(method);
			InputStream jsonData = response.getEntity().getContent();
			return jsonData;
		} catch (Exception e) {
			throw new TramTrackerServiceException(e);
		}
	}

	/**
	 * Parse the given {@link InputStream} into a {@link JSONObject}.
	 * 
	 * @throws TramTrackerServiceException
	 */
	private JSONObject parseJSONStream(InputStream is) throws IOException, JSONException, TramTrackerServiceException {
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
			if (LOGV) Log.d(TAG, "JSON Response: " + jsonData);
			jsonObject = new JSONObject(jsonData);
		} catch (Exception e) {
			throw new TramTrackerServiceException(e);
		}
		return jsonObject;
	}

	/**
	 * Helper method to parse the response object from JSON returned from the
	 */
	private JSONObject getResponseObject(JSONObject serviceData) throws IOException, JSONException, TramTrackerServiceException {

		JSONObject responseObject = null;

		String errorMessage = "TramTracker Service Error";

		// If we have an error message returned from the API, pass it through
		if (serviceData.getBoolean("hasError")) {
			if (serviceData.has("errorMessage")) errorMessage = serviceData.getString("errorMessage");
			// If we have an error, then don't pass any data
			responseObject = null;
			throw new TramTrackerServiceException(errorMessage);
		} else {

			if (serviceData.isNull("responseObject")) {
				throw new TramTrackerServiceException("Tram Tracker returned no results.");
			}

			try {
				responseObject = serviceData.getJSONObject("responseObject");
			} catch (Exception e) {
				throw new TramTrackerServiceException("Tram Tracker returned a bad response.");
			}
		}

		return responseObject;
	}

	/**
	 * Helper method to parse the response array from JSON returned from the
	 */
	private JSONArray getResponseArray(JSONObject serviceData) throws IOException, JSONException, TramTrackerServiceException {

		JSONArray responseArray = null;

		String errorMessage = "TramTracker Service Error";

		// If we have an error message returned from the API, pass it through
		if (serviceData.getBoolean("hasError")) {
			if (serviceData.has("errorMessage")) errorMessage = serviceData.getString("errorMessage");
			// If we have an error, then don't pass any data
			responseArray = null;
			if (errorMessage.contains("No or invalid device token provided.")) {
				getNewClientGuid();
			}
			throw new TramTrackerServiceException(errorMessage);
		} else {

			if (serviceData.isNull("responseObject")) {
				throw new TramTrackerServiceException("Tram Tracker returned no results.");
			}

			try {
				responseArray = serviceData.getJSONArray("responseObject");
			} catch (Exception e) {
				throw new TramTrackerServiceException("Tram Tracker returned a bad response");
			}
		}

		return responseArray;
	}

	/**
	 * Parse the given {@link InputStream} into {@link Stop} assuming a JSON format.
	 * 
	 * @return Stop
	 */
	public static Stop parseStopInformation(JSONObject responseObject) throws TramTrackerServiceException {
		// {
		// "responseObject":[
		// {
		// "FlagStopNo":"14",
		// "StopName":"Royal Melbourne Hospital & Flemington Rd",
		// "CityDirection":"towards City",
		// "Latitude":-37.799511541211,
		// "Longitude":144.95492172036,
		// "SuburbName":"Parkville",
		// "IsCityStop":false,
		// "HasConnectingBuses":true,
		// "HasConnectingTrains":false,
		// "HasConnectingTrams":false,
		// "StopLength":31,
		// "IsPlatformStop":true,
		// "Zones":"0,1"
		// }
		// ],
		// "isError":false
		// }

		try {
			Stop stop = new Stop();

			String flagStopNo = responseObject.getString("FlagStopNo");
			String stopName = responseObject.getString("StopName");
			String cityDirection = responseObject.getString("CityDirection");

			stop.setFlagStopNumber(flagStopNo);
			stop.setStopName(stopName);
			stop.setCityDirection(cityDirection);

			return stop;
		} catch (Exception e) {
			throw new TramTrackerServiceException(e);
		}
	}

	/**
	 * Get tram stop information for a given TramTracker ID
	 */
	public Stop getStopInformation(int tramTrackerID) throws TramTrackerServiceException {
		try {
			Stop stop = null;

			// TODO: Update this for JSON
			String url = BASE_URL + "/GetStopInformation.aspx?s=" + tramTrackerID;
			InputStream jsonData = getJSONData(url, null);
			JSONObject serviceData = parseJSONStream(jsonData);
			JSONObject responseObject = getResponseObject(serviceData);
			stop = parseStopInformation(responseObject);
			stop.setTramTrackerID(tramTrackerID);
			return stop;
		} catch (Exception e) {
			// Throw a TramTrackerServiceException to encapsulate all
			// other exceptions
			throw new TramTrackerServiceException(e);
		}
	}

	/**
	 * Parse the given {@link InputStream} into {@link Stop} assuming a JSON format.
	 * 
	 * @return Stop
	 */
	public static List<NextTram> parseNextPredictedRoutesCollection(JSONArray responseArray, Date requestTime) throws TramTrackerServiceException {

		try {
			List<NextTram> nextTrams = new ArrayList<NextTram>();

			int responseObjectCount = responseArray.length();
			for (int i = 0; i < responseObjectCount; i++) {

				JSONObject responseObject = responseArray.getJSONObject(i);

				int internalRouteNo = responseObject.getInt("InternalRouteNo");
				String routeNo = responseObject.getString("RouteNo");
				String headboardRouteNo = responseObject.getString("HeadBoardRouteNo");
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
				tram.setRequestDateTime(requestTime);

				nextTrams.add(tram);
			}

			return nextTrams;
		} catch (Exception e) {
			throw new TramTrackerServiceException(e);
		}

	}

	/**
	 * Get the list of next trams for a given stop and route
	 */
	public List<NextTram> getNextPredictedRoutesCollection(Stop stop, Route route) throws TramTrackerServiceException {

		// Hard code low floor for now
		String lowFloor = String.valueOf(false);

		// Route number
		String routeNumber = "0";
		if (route != null) routeNumber = Integer.toString(route.getInternalNumber());

		// Stop ID
		int tramTrackerID = stop.getTramTrackerID();

		try {
			List<NextTram> nextTrams = null;

			// GetNextPredictedRoutesCollection/{stopNo}/{routeNo}/{lowFloor}/
			String method = "GetNextPredictedRoutesCollection";
			String url = String.format("%s/%s/%s/%s/%s/", BASE_URL, method, tramTrackerID, routeNumber, lowFloor);

			List<NameValuePair> params = new LinkedList<NameValuePair>();
			// This call required the 'cid' value. We usually just use '1' for this.
			params.add(new BasicNameValuePair("cid", "1"));

			InputStream jsonData = getJSONData(url, params);
			JSONObject serviceData = parseJSONStream(jsonData);

			JSONArray responseArray = getResponseArray(serviceData);

			String requestTime = serviceData.getString("timeRequested");
			Date requestDateTime = parseTimestamp(requestTime);

			nextTrams = parseNextPredictedRoutesCollection(responseArray, requestDateTime);

			return nextTrams;

		} catch (Exception e) {
			throw new TramTrackerServiceException(e);
		}

	}

	/**
	 * 
	 */
	public TramRun parseGetNextPredictedArrivalTimeAtStopsForTramNo(JSONObject responseObject, Date requestTime) throws TramTrackerServiceException {

		// {
		// "errorMessage": null,
		// "hasError": false,
		// "hasResponse": true,
		// "responseObject": {
		// "__type": "NextPredictedArrivalTimeAtStopsForTramNoInfo",
		// "    AtLayover": false,
		// "Available": true,
		// "DisruptionMessage": null,
		// "HasDisruption": false,
		// "HasSpecialEvent": false,
		// "HeadBoardRouteNo": "109",
		// "NextPredictedStopsDetails": [
		// {
		// "PredictedArrivalDateTime": "\/Date(1406371302000+1000)\/",
		// "StopNo": 1707,
		// "StopSeq": null
		// },
		// {
		// "PredictedArrivalDateTime": "\/Date(1406371422000+1000)\/",
		// "StopNo": 3357,
		// "StopSeq": null
		// },
		// {
		// "PredictedArrivalDateTime": "\/Date(1406371482000+1000)\/",
		// "StopNo": 3356,
		// "StopSeq": null
		// }
		// ],
		// "RouteNo": "109",
		// "SpecialEventMessage": "",
		// "Up": false,
		// "VehicleNo": 3001
		// },
		// "timeRequested": "\/Date(1406371299805+1000)\/",
		// "timeResponded": "\/Date(1406371302351+1000)\/",
		// "webMethodCalled": "GetNextPredictedArrivalTimeAtStopsForTramNo"
		// }

		mDB = new TramHunterDB();
		TramRun tramRun = new TramRun();

		try {
			int vehicleNumber = responseObject.getInt("VehicleNo");
			boolean isAtLayover = responseObject.getBoolean("AtLayover");
			boolean isAvailable = responseObject.getBoolean("Available");
			boolean hasSpecialEvent = responseObject.getBoolean("HasSpecialEvent");
			boolean hasDisruption = responseObject.getBoolean("HasDisruption");

			tramRun.setVehicleNo(vehicleNumber);
			tramRun.setIsAtLayover(isAtLayover);
			tramRun.setIsAvailable(isAvailable);
			tramRun.setHasSpecialEvent(hasSpecialEvent);
			tramRun.setHasDisruption(hasDisruption);

			JSONArray nextPredictedStopsDetails = responseObject.getJSONArray("NextPredictedStopsDetails");

			int objCount = nextPredictedStopsDetails.length();
			for (int i = 0; i < objCount; i++) {

				JSONObject timeObject = nextPredictedStopsDetails.getJSONObject(i);

				TramRunTime tramRunTime = new TramRunTime();

				// Get stop string and fetch stop from DB
				int stopNumber = timeObject.getInt("StopNo");
				Stop stop = mDB.getStop(stopNumber);
				if(stop==null){
					//YT has built another new tram stop since last DB update
					stop = new Stop();
					stop.setTramTrackerID(stopNumber);
					stop.setPrimaryName(String.format("Unknown stop (%d)", stopNumber));
				}
				tramRunTime.setStop(stop);

				// Get predicted time and parse it to a java datetime
				String predictedArrivalDateTimeString = timeObject.getString("PredictedArrivalDateTime");
				Date predictedArrivalDateTime = parseTimestamp(predictedArrivalDateTimeString);
				tramRunTime.setPredictedArrivalDateTime(predictedArrivalDateTime);

				tramRun.addTramRunTime(tramRunTime);
			}
		} catch (Exception e) {
			throw new TramTrackerServiceException(e.toString());
		} finally {
			mDB.close();
		}

		return tramRun;
	}

	/**
	 * TODO: Update this
	 */
	public TramRun getNextPredictedArrivalTimeAtStopsForTramNo(int tram) throws TramTrackerServiceException {

		TramRun tramRun = null;

		try {
			// GetNextPredictedArrivalTimeAtStopsForTramNo/{vehicleNumber}/
			String method = "GetNextPredictedArrivalTimeAtStopsForTramNo";
			String url = String.format("%s/%s/%s/", BASE_URL, method, tram);

			List<NameValuePair> params = new LinkedList<NameValuePair>();

			InputStream jsonData = getJSONData(url, params);
			JSONObject serviceData = parseJSONStream(jsonData);

			// Actual information object
			JSONObject responseObject = getResponseObject(serviceData);

			String requestTime = serviceData.getString("timeRequested");
			Date requestDateTime = parseTimestamp(requestTime);

			tramRun = parseGetNextPredictedArrivalTimeAtStopsForTramNo(responseObject, requestDateTime);
		} catch (Exception e) {
			throw new TramTrackerServiceException(e);
		}
		return tramRun;
	}

	/**
	 * Convert the JSON timestamp string to a java date object
	 * 
	 * @param timestamp
	 * @return date
	 */
	private static Date parseTimestamp(String timestamp) {
		// "PredictedArrivalDateTime": "\/Date(1305384840000+1000)\/",
		Long fixedDate = Long.parseLong(timestamp.substring(6, 19));
		Date date = new Date(fixedDate);
		return date;
	}

	/**
	 * Fetch a new GUID
	 * 
	 * @throws TramTrackerServiceException
	 * @throws JSONException
	 * @throws IOException
	 */
	public String getNewClientGuid() throws TramTrackerServiceException {

		// {
		// "errorMessage": null,
		// "hasError": false,
		// "hasResponse": true,
		// "responseObject": [
		// {
		// "__type": "AddDeviceTokenInfo",
		// "DeviceToken": "7faf1ace-793f-4139-8082-dda9aee3ab1f"
		// }
		// ],
		// "timeRequested": "\/Date(1337190892475+1000)\/",
		// "timeResponded": "\/Date(1337190892475+1000)\/",
		// "webMethodCalled": "GetDeviceToken"
		// }

		DefaultHttpClient httpClient = new DefaultHttpClient();

		// TODO: Work out what these should really be
		List<NameValuePair> params = new LinkedList<NameValuePair>();
		params.add(new BasicNameValuePair("aid", CLIENT_TYPE));
		params.add(new BasicNameValuePair("devInfo", UserAgent.getUserAgent()));

		String paramString = URLEncodedUtils.format(params, "utf-8");
		String url = BASE_URL + "/GetDeviceToken/" + "?" + paramString;
		if (LOGV) Log.d(TAG, "Fetching URL: " + url);

		try {
			URI uri = new URI(url);
			HttpGet method = new HttpGet(uri);
			HttpResponse response = httpClient.execute(method);
			InputStream jsonData = response.getEntity().getContent();
			JSONObject serviceData = parseJSONStream(jsonData);
			JSONArray responseArray = getResponseArray(serviceData);
			JSONObject responseObject = responseArray.getJSONObject(0);

			String guid = responseObject.getString("DeviceToken");

			// Save our GUID
			mPreferenceHelper.setGUID(guid);

			return guid;

		} catch (Exception e) {
			throw new TramTrackerServiceException(e);
		}

	}

	public String getGUID() throws TramTrackerServiceException {

		// Get our GUID from the database
		String guid = mPreferenceHelper.getGUID();

		// If we don't have a GUID yet, get from from TramTracker
		if (guid == null) {
			if (LOGV) Log.d(TAG, "GUID is null, fetching new one");
			guid = getNewClientGuid();
		}

		return guid;
	}

}
