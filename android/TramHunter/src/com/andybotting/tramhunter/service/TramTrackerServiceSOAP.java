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

import android.util.Log;

import com.andybotting.tramhunter.dao.TramHunterDB;
import com.andybotting.tramhunter.objects.NextTram;
import com.andybotting.tramhunter.objects.Route;
import com.andybotting.tramhunter.objects.Stop;
import com.andybotting.tramhunter.objects.TramRun;
import com.andybotting.tramhunter.objects.TramRunTime;
import com.andybotting.tramhunter.util.PreferenceHelper;

public class TramTrackerServiceSOAP implements TramTrackerService {

	private static final String TAG = "TTServiceSOAP";
	private static final boolean LOGV = Log.isLoggable(TAG, Log.INFO);

	private TramHunterDB mDB;

	private static final String NAMESPACE = "http://www.yarratrams.com.au/pidsservice/";
	private static final String URL = "http://ws.tramtracker.com.au/pidsservice/pids.asmx";

	private static final String CLIENTTYPE = "TRAMHUNTER";
	private static final String CLIENTVERSION = "1.3.0";
	private static final String CLIENTWEBSERVICESVERSION = "6.4.0.0";

	private PreferenceHelper mPreferenceHelper;

	public TramTrackerServiceSOAP() {
		this.mPreferenceHelper = new PreferenceHelper();
	}

	/**
	 * Parse the timestamp given my Tram Tracker
	 * 
	 * @param dateString
	 * @return
	 */
	private Date parseTimestamp(String dateString) {

		// <PredictedArrivalDateTime>2010-05-30T19:00:48+10:00</PredictedArrivalDateTime>
		// <RequestDateTime>2010-05-30T18:59:54.2212858+10:00</RequestDateTime>

		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
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
				// stop.setLatitude(Float.parseFloat(stopResult4.getProperty(3).toString()));
				// stop.setLongitude(Float.parseFloat(stopResult4.getProperty(4).toString()));
				stop.setSuburb(stopResult4.getProperty(5).toString());
			}

			return stop;
		} catch (Exception e) {
			throw new TramTrackerServiceException(e);
		}

	}

	/**
	 * Get next tram departures
	 */
	public List<NextTram> getNextPredictedRoutesCollection(Stop stop, Route route) throws TramTrackerServiceException {

		SoapObject soapResult = null;
		final List<NextTram> nextTrams = new ArrayList<NextTram>();

		SoapObject request = new SoapObject(NAMESPACE, "GetNextPredictedRoutesCollection");
		request.addProperty("stopNo", stop.getTramTrackerID());
		request.addProperty("lowFloor", "false");

		// Filter the results by route
		if (route == null)
			request.addProperty("routeNo", "0");
		else
			request.addProperty("routeNo", route.getNumber());

		Object result = makeTramTrackerRequest(request);

		try {
			soapResult = (SoapObject) result;
		} catch (ClassCastException e) {
			// SOAP result might just be an error string
			throw new TramTrackerServiceException(result.toString());
		}

		if (soapResult != null) {

			try {
				SoapObject soapResult1 = (SoapObject) soapResult.getProperty("diffgram");
				SoapObject soapResult2 = (SoapObject) soapResult1.getProperty("DocumentElement");

				for (int i = 0; i < soapResult2.getPropertyCount(); i++) {

					SoapObject nextPredicted = (SoapObject) soapResult2.getProperty(i);

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
					
					// This is a dirty hack to save the clock offset between the
					// TramTracker API and the local device. This is important
					// for API calls which return times, but don't return a
					// 'request' timestamp.
					Date now = new Date();
					long clockOffset = now.getTime() - requestDateTime.getTime();
					mPreferenceHelper.setClockOffset(clockOffset);

					// Those Yarra Trams guys are a pack of bastards
					if (specialEventMessage.contains("Android")) {
						specialEventMessage = "";
						hasSpecialEvent = false;
					}

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

			} catch (Exception e) {
				throw new TramTrackerServiceException("No results");
			}

		} else {
			throw new TramTrackerServiceException("No results");
		}

		return nextTrams;

	}

	/**
	 * Make a Tram Tracker request
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
			String guid = getGUID();
			envelope.setGuid(guid);

			HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);
			androidHttpTransport.debug = true;

			androidHttpTransport.call(soapAction, envelope);

			Object response = envelope.getResponse();
			// if (LOGV) Log.d(TAG, "SOAP Response: " + response.toString());

			return response;

		} catch (Exception e) {
			throw new TramTrackerServiceException(e);
		}

	}

	/**
	 * Get next tram departures
	 */
	public TramRun getNextPredictedArrivalTimeAtStopsForTramNo(int tram) throws TramTrackerServiceException {

		//	<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
		//		<soap:Body>
		//		    <GetNextPredictedArrivalTimeAtStopsForTramNoResponse xmlns="http://www.yarratrams.com.au/pidsservice/">
		//		    	<GetNextPredictedArrivalTimeAtStopsForTramNoResult>
		//		    	    <diffgr:diffgram xmlns:diffgr="urn:schemas-microsoft-com:xml-diffgram-v1" xmlns:msdata="urn:schemas-microsoft-com:xml-msdata">
		//		    	        <NewDataSet xmlns="">
		//		    	            <TramNoRunDetailsTable diffgr:id="TramNoRunDetailsTable1" msdata:rowOrder="0">
		//		    	                <VehicleNo>2064</VehicleNo>
		//		    	                <AtLayover>false</AtLayover>
		//		    	                <Available>true</Available>
		//		    	                <RouteNo>59</RouteNo>
		//		    	                <HeadBoardRouteNo>59</HeadBoardRouteNo>
		//		    	                <Up>true</Up>
		//		    	                <HasSpecialEvent>false</HasSpecialEvent>
		//		    	                <HasDisruption>false</HasDisruption>
		//		    	            </TramNoRunDetailsTable>
		//		    	            <NextPredictedStopsDetailsTable diffgr:id="NextPredictedStopsDetailsTable1" msdata:rowOrder="0">
		//		    	                <StopNo>1318</StopNo>
		//		    	                <PredictedArrivalDateTime>2012-08-12T17:48:00+10:00</PredictedArrivalDateTime>
		//		    	            </NextPredictedStopsDetailsTable>
		//		    	            <NextPredictedStopsDetailsTable diffgr:id="NextPredictedStopsDetailsTable2" msdata:rowOrder="1">
		//		    	                <StopNo>1317</StopNo>
		//		    	                <PredictedArrivalDateTime>2012-08-12T17:49:00+10:00</PredictedArrivalDateTime>
		//		    	            </NextPredictedStopsDetailsTable>
		//		    	            <NextPredictedStopsDetailsTable diffgr:id="NextPredictedStopsDetailsTable3" msdata:rowOrder="2">
		//		    	                <StopNo>1316</StopNo>
		//		    	                <PredictedArrivalDateTime>2012-08-12T17:50:00+10:00</PredictedArrivalDateTime>
		//		    	            </NextPredictedStopsDetailsTable>
		//		    	            <NextPredictedStopsDetailsTable diffgr:id="NextPredictedStopsDetailsTable4" msdata:rowOrder="3">
		//		    	                <StopNo>1315</StopNo>
		//		    	                <PredictedArrivalDateTime>2012-08-12T17:50:00+10:00</PredictedArrivalDateTime>
		//		    	            </NextPredictedStopsDetailsTable>
		//		    	        </NewDataSet>
		//		    	    </diffgr:diffgram>
		//		    	</GetNextPredictedArrivalTimeAtStopsForTramNoResult>
		//		    	<validationResult/>
		//			</GetNextPredictedArrivalTimeAtStopsForTramNoResponse>
		//		</soap:Body>
		//	</soap:Envelope>

		SoapObject soapResult = null;
		mDB = new TramHunterDB();

		TramRun tramRun = new TramRun();

		SoapObject request = new SoapObject(NAMESPACE, "GetNextPredictedArrivalTimeAtStopsForTramNo");
		request.addProperty("tramNo", tram);

		Object result = makeTramTrackerRequest(request);

		try {
			soapResult = (SoapObject) result;
		} catch (ClassCastException e) {
			// SOAP result might just be an error string
			throw new TramTrackerServiceException(result.toString());
		}

		if (soapResult != null) {

			try {
				SoapObject diffgram = (SoapObject) soapResult.getProperty("diffgram");
				SoapObject dataSet = (SoapObject) diffgram.getProperty("NewDataSet");

				SoapObject runDetails = (SoapObject) dataSet.getProperty("TramNoRunDetailsTable");

				int vehicleNumber = Integer.parseInt(runDetails.getProperty("VehicleNo").toString());
				boolean isAtLayover = runDetails.getProperty("AtLayover").toString().equalsIgnoreCase("true") ? true : false;
				boolean isAvailable = runDetails.getProperty("Available").toString().equalsIgnoreCase("true") ? true : false;

				boolean hasSpecialEvent = runDetails.getProperty("HasSpecialEvent").toString().equalsIgnoreCase("true") ? true : false;
				boolean hasDisruption = runDetails.getProperty("AtLayover").toString().equalsIgnoreCase("true") ? true : false;

				tramRun.setVehicleNo(vehicleNumber);
				tramRun.setIsAtLayover(isAtLayover);
				tramRun.setIsAvailable(isAvailable);
				tramRun.setHasSpecialEvent(hasSpecialEvent);
				tramRun.setHasDisruption(hasDisruption);

				// Start at 1 becuase the dataset starts with the details table.
				for (int i = 1; i < dataSet.getPropertyCount(); i++) {

					SoapObject timeObject = (SoapObject) dataSet.getProperty(i);

					TramRunTime tramRunTime = new TramRunTime();

					// Get stop string and fetch stop from DB
					int stopNumber = Integer.parseInt(timeObject.getProperty("StopNo").toString());
					Stop stop = mDB.getStop(stopNumber);
					tramRunTime.setStop(stop);

					// Get predicted time and parse it to a java datetime
					String predictedArrivalDateTimeString = timeObject.getProperty("PredictedArrivalDateTime").toString();
					Date predictedArrivalDateTime = parseTimestamp(predictedArrivalDateTimeString);
					tramRunTime.setPredictedArrivalDateTime(predictedArrivalDateTime);

					tramRun.addTramRunTime(tramRunTime);
				}

			} catch (Exception e) {
				throw new TramTrackerServiceException("No results");
			} finally {
				mDB.close();
			}

		} else {
			throw new TramTrackerServiceException("No results");
		}

		return tramRun;

	}

	/**
	 * Fetch a new GUID
	 */
	public String getNewClientGuid() throws TramTrackerServiceException {

		SoapObject request = new SoapObject(NAMESPACE, "GetNewClientGuid");

		String methodName = request.getName();
		String soapAction = NAMESPACE + methodName;

		try {
			SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
			envelope.setOutputSoapObject(request);
			envelope.setClientType(CLIENTTYPE);
			envelope.setClientVersion(CLIENTVERSION);
			envelope.setClientWebServiceVersion(CLIENTWEBSERVICESVERSION);
			envelope.dotNet = true;

			HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);
			androidHttpTransport.debug = true;
			androidHttpTransport.call(soapAction, envelope);
			Object response = envelope.getResponse();
			String guid = response.toString();
			// Save our GUID
			mPreferenceHelper.setGUID(guid);
			return guid;
		} catch (Exception e) {
			throw new TramTrackerServiceException("Error fetching GUID from TramTracker: " + e);
		}
	}

	/**
	 * Get the GUID
	 */
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
