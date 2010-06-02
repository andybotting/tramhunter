package com.andybotting.tramhunter;

import java.io.IOException;
import java.util.Vector;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import org.xmlpull.v1.XmlPullParserException;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import android.widget.TextView;


public class TramTrackerRequest {

	private static final String NAMESPACE = "http://www.yarratrams.com.au/pidsservice/";
	private static final String URL = "http://ws.tramtracker.com.au/pidsservice/pids.asmx";
	
	// Start with a blank GUID, and we'll generate one for the 
	// life of the TramTrackerRequest object
	String guid = "";

	// Web Services fields for TramTracker
	private static final String CLIENTTYPE = "WEBPID";
	private static final String CLIENTVERSION = "1.1.0";
	private static final String CLIENTWEBSERVICESVERSION = "6.4.0.0";
	
	private Object makeTramTrackerRequest(SoapObject request) {	
		
		String methodName = request.getName();
		String soapAction = NAMESPACE + methodName;
		
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);	
 		envelope.setOutputSoapObject(request);
 	
 		envelope.setClientType(CLIENTTYPE);
 		envelope.setClientVersion(CLIENTVERSION);
 		envelope.setClientWebServiceVersion(CLIENTWEBSERVICESVERSION);
		envelope.dotNet = true;
 		
 		// Generate a new Guid, if it doesn't exist
 		if (guid == "") {
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

	
	
	public void getNewClientGuid() {
		SoapObject request = new SoapObject(NAMESPACE, "GetNewClientGuid");	  
		Object result =  makeTramTrackerRequest(request);
		guid = result.toString();
	}
		
	
	public Stop GetStopInformation(int tramTrackerID) {

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

	
	public Vector GetNextPredictedRoutesCollection (Stop stop) {
		SoapObject request = new SoapObject(NAMESPACE, "GetNextPredictedRoutesCollection");
		request.addProperty("stopNo", stop.getTramTrackerID());
		request.addProperty("routeNo", "0");
		request.addProperty("lowFloor", "false");
	   
		SoapObject result = null;
		Vector collection = new Vector();
			
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
					tram.setVehicleNo(nextPredicted.getProperty(3).toString());
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
						
					collection.add(tram);
			}
						
		}
		else {
			Log.d("Testing", "result is null");
		}
			
		return collection;

	}
	
	public String toString() {
		return "GUID: " + guid;
	}	

}
