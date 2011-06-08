/*  
 * Copyright 2010 Andy Botting <andy@andybotting.com>  
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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import android.content.Context;
import android.util.Log;

import com.andybotting.tramhunter.objects.NextTram;
import com.andybotting.tramhunter.objects.Route;
import com.andybotting.tramhunter.objects.Stop;
import com.andybotting.tramhunter.util.PreferenceHelper;


public class TramTrackerServiceSOAP implements TramTrackerService {
	
    private static final String TAG = "TramTrackerService";
    private static final boolean LOGV = Log.isLoggable(TAG, Log.INFO);

	private static final String NAMESPACE = "http://www.yarratrams.com.au/pidsservice/";
	private static final String URL = "http://ws.tramtracker.com.au/pidsservice/pids.asmx";

	private static final String CLIENTTYPE = "TRAMHUNTER";
	private static final String CLIENTVERSION = "0.6.0";
	private static final String CLIENTWEBSERVICESVERSION = "6.4.0.0";
	
	private Context mContext;
	private PreferenceHelper mPreferenceHelper;
	
	String guid = "";

	
	public TramTrackerServiceSOAP(Context context) {
		this.mContext = context;
		this.mPreferenceHelper = new PreferenceHelper(mContext);
	}
	
	
	/**
	 * Parse the timestamp given my Tram Tracker
	 * @param dateString
	 * @return
	 */
	private Date parseTimestamp(String dateString) {
		
		//<PredictedArrivalDateTime>2010-05-30T19:00:48+10:00</PredictedArrivalDateTime>
		//<RequestDateTime>2010-05-30T18:59:54.2212858+10:00</RequestDateTime>
		
		DateFormat df = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("Australia/Melbourne"));
		Date date = new Date();
				
		try {
			String fixedDate = dateString.substring(0, 18);
			date = df.parse(fixedDate);
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
		}	
		
		return date;
	}
	
	
	/**
	 * Get Stop information
	 */
	public Stop getStopInformation(int tramTrackerID) throws TramTrackerServiceException {

		SoapObject request = new SoapObject(NAMESPACE, "GetStopInformation");
		request.addProperty("stopNo", tramTrackerID);

		try {
			SoapObject result = (SoapObject) makeTramTrackerRequest(request);
			
			Stop stop = new Stop();
			
			if (result != null) {
				
				SoapObject stopResult2 = (SoapObject) result.getProperty("diffgram");
				SoapObject stopResult3 = (SoapObject) stopResult2.getProperty("DocumentElement");
				SoapObject stopResult4 = (SoapObject) stopResult3.getProperty("StopInformation");
				
				stop.setTramTrackerID(tramTrackerID);
				stop.setFlagStopNumber(stopResult4.getProperty(0).toString());
				stop.setStopName(stopResult4.getProperty(1).toString());
				stop.setCityDirection(stopResult4.getProperty(2).toString());
				//stop.setLatitude(Float.parseFloat(stopResult4.getProperty(3).toString()));
				//stop.setLongitude(Float.parseFloat(stopResult4.getProperty(4).toString()));
				stop.setSuburb(stopResult4.getProperty(5).toString());
			}
		
			return stop;
		}
		catch (Exception e) {
			throw new TramTrackerServiceException(e);
		}

	}

	
	/**
	 * Get next tram departures
	 */
	public List<NextTram> getNextPredictedRoutesCollection (Stop stop, Route route) throws TramTrackerServiceException {
		
		SoapObject result = null;
		final List<NextTram> nextTrams = new ArrayList<NextTram>();
		
		SoapObject request = new SoapObject(NAMESPACE, "GetNextPredictedRoutesCollection");
		request.addProperty("stopNo", stop.getTramTrackerID());
		request.addProperty("lowFloor", "false");
		
		// Filter the results by route
		if (route == null)
			request.addProperty("routeNo", "0");
		else
			request.addProperty("routeNo", route.getNumber());
		
					
		try {
			result = (SoapObject) makeTramTrackerRequest(request);
		}
		catch (ClassCastException e) {
			throw new TramTrackerServiceException(e);
		}

		if (result != null) {
				
			SoapObject result1 = (SoapObject)result.getProperty("diffgram");
			SoapObject result2 = (SoapObject)result1.getProperty("DocumentElement");

			
			for (int i = 0; i < result2.getPropertyCount(); i++) {
				
					SoapObject nextPredicted = (SoapObject)result2.getProperty(i);
					
					NextTram tram = new NextTram();
					
		            int internalRouteNo = Integer.parseInt(nextPredicted.getProperty(0).toString());
		            String routeNo = nextPredicted.getProperty(1).toString();
		            String headboardRouteNo = nextPredicted.getProperty(2).toString();
		            int vehicleNo = Integer.parseInt(nextPredicted.getProperty(3).toString());
		            String destination = nextPredicted.getProperty(4).toString();
		            boolean hasDisruption = nextPredicted.getProperty(5).toString().equalsIgnoreCase("true") ? true : false;
		            boolean isTTAvailable = nextPredicted.getProperty(6).toString().equalsIgnoreCase("true") ? true : false;
		            boolean isLowFloorTram = nextPredicted.getProperty(7).toString().equalsIgnoreCase("true") ? true : false;
		            boolean airConditioned = nextPredicted.getProperty(8).toString().equalsIgnoreCase("true") ? true : false;
		            boolean displayAC = nextPredicted.getProperty(9).toString().equalsIgnoreCase("true") ? true : false;
		            boolean hasSpecialEvent = nextPredicted.getProperty(10).toString().equalsIgnoreCase("true") ? true : false;
		            String specialEventMessage = nextPredicted.getProperty(11).toString();
		            String predictedArrivalDateTimeString = nextPredicted.getProperty(12).toString();
		            String requestDateTimeString = nextPredicted.getProperty(13).toString();
		            
		            // Parse timestamps
		            Date predictedArrivalDateTime = parseTimestamp(predictedArrivalDateTimeString);
		            Date requestDateTime = parseTimestamp(requestDateTimeString);
		            
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
			
		}
		else {
			throw new TramTrackerServiceException("No results");
		}
			
		return nextTrams;

	}	
	
	private void getNewClientGuid() throws TramTrackerServiceException {

		try {
			SoapObject request = new SoapObject(NAMESPACE, "GetNewClientGuid");	 
			Object result = makeTramTrackerRequest(request);
	
			guid = result.toString();
			mPreferenceHelper.setGUID(guid);
		}
		catch (Exception e) {
			throw new TramTrackerServiceException("Error getting GUID");
		}
	}
	
	
	/**
	 * Get the GUID
	 */
	public String getGUID() {
		return guid;
	}
	
	
	/**
	 * toString method
	 */
	public String toString() {
		return "GUID: " + guid;
	}

	
	/**
	 * Make a Tram Tracker request
	 * @param request
	 * @return
	 * @throws TramTrackerServiceException
	 */
	private Object makeTramTrackerRequest(SoapObject request) throws TramTrackerServiceException {	
		
		String methodName = request.getName();
		String soapAction = NAMESPACE + methodName;

		try {
			SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);	
			envelope.setOutputSoapObject(request);
		
			envelope.setClientType(CLIENTTYPE);
			envelope.setClientVersion(CLIENTVERSION);
			envelope.setClientWebServiceVersion(CLIENTWEBSERVICESVERSION);
			envelope.dotNet = true;
			
			// Get our GUID from the database
			guid = mPreferenceHelper.getGUID();
			
			// If we don't have a GUID yet, get from from TramTracker
			if (guid == "") {
				Log.d("Testing","GUID is null, methodName is " + methodName);
				
				if (methodName != "GetNewClientGuid") {
					getNewClientGuid();
		
					// Sleep for a sec so that our GUID works with the next TT Request
					try {
						Thread.sleep(1000);
					} 
					catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
				
			envelope.setGuid(guid);
					
			HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);
			androidHttpTransport.debug = true;
			
			androidHttpTransport.call(soapAction, envelope);
			return envelope.getResponse();
			
		} 
		catch (Exception e) {
			throw new TramTrackerServiceException(e);
		} 
		
	}	

}
