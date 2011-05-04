package com.andybotting.tramhunter.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.util.Log;

import com.andybotting.tramhunter.dao.TramHunterDB;
import com.andybotting.tramhunter.objects.NextTram;
import com.andybotting.tramhunter.objects.Route;
import com.andybotting.tramhunter.objects.Stop;
import com.andybotting.tramhunter.util.PreferenceHelper;


public class TramTrackerServiceSOAP implements TramTrackerService {

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
	
	public Stop getStopInformation(int tramTrackerID) {

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
		catch (ClassCastException e)  {
			System.out.println("Stop not found");
		}

		System.out.println("Stop not found");
		
		return null;
	}

	public List<NextTram> getNextPredictedRoutesCollection (Stop stop, Route route) {
		SoapObject request = new SoapObject(NAMESPACE, "GetNextPredictedRoutesCollection");
		
		request.addProperty("stopNo", stop.getTramTrackerID());
		
		// Filter the results by route
		if (route == null)
			request.addProperty("routeNo", "0");
		else
			request.addProperty("routeNo", route.getNumber());
		
		request.addProperty("lowFloor", "false");
	   
		SoapObject result = null;
		final List<NextTram> nextTrams = new ArrayList<NextTram>();
			
		try {
			result = (SoapObject) makeTramTrackerRequest(request);
		}
		catch (ClassCastException exc) {
			Log.d("Testing", "ClassCastException getting SOAP results: " + exc);
		}

		if (result != null) {
				
			SoapObject result1 = (SoapObject)result.getProperty("diffgram");
			SoapObject result2 = (SoapObject)result1.getProperty("DocumentElement");

			
			for (int i = 0; i < result2.getPropertyCount(); i++) {
				
					SoapObject nextPredicted = (SoapObject)result2.getProperty(i);
					
					NextTram tram = new NextTram();

					tram.setInternalRouteNo(nextPredicted.getProperty(0).toString());
					tram.setRouteNo(nextPredicted.getProperty(1).toString());
					tram.setHeadboardRouteNo(nextPredicted.getProperty(2).toString());
					tram.setVehicleNo(Integer.parseInt(nextPredicted.getProperty(3).toString()));
					tram.setDestination(nextPredicted.getProperty(4).toString());
					tram.setHasDisruption(nextPredicted.getProperty(5).toString());
					tram.setIsTTAvailable(nextPredicted.getProperty(6).toString());
					tram.setIsLowFloorTram(nextPredicted.getProperty(7).toString());
					tram.setAirConditioned(nextPredicted.getProperty(8).toString());
					tram.setDisplayAC(nextPredicted.getProperty(9).toString());
					tram.setHasSpecialEvent(nextPredicted.getProperty(10).toString());
					tram.setSpecialEventMessage(nextPredicted.getProperty(11).toString());
					tram.setPredictedArrivalDateTime(nextPredicted.getProperty(12).toString());
					tram.setRequestDateTime(nextPredicted.getProperty(13).toString());
					
					nextTrams.add(tram);
			}		
			
		}
		else {
			Log.d("Testing", "result is null");
		}
			
		return nextTrams;

	}	
	
	private void getNewClientGuid() {
		
		Log.d("Testing", "Getting new GUID from TT");
		
		SoapObject request = new SoapObject(NAMESPACE, "GetNewClientGuid");	 
		Object result = makeTramTrackerRequest(request);

		guid = result.toString();
		
		Log.d("Testing", "GUID from TT is: " + guid);
		
		// Create out DB instance
		mPreferenceHelper.setGUID(guid);
	}
	
	public String getGUID() {
		return guid;
	}
	
	public String toString() {
		return "GUID: " + guid;
	}

	private Object makeTramTrackerRequest(SoapObject request) {	
		
		String methodName = request.getName();
		String soapAction = NAMESPACE + methodName;
		
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
		
		// Call the URL
		try {
			androidHttpTransport.call(soapAction, envelope);
		} 
		catch (IOException e) {
			e.printStackTrace();
		} 
		catch (XmlPullParserException e) {
			e.printStackTrace();
		}
		
		// Parse SOAP response
		Object result = null;
		try {
			result = envelope.getResponse();
		} catch (SoapFault e) {
			Log.d("Testing", "Error getting SOAP envelope response");
			e.printStackTrace();
		}
		return result;
	}	

}
