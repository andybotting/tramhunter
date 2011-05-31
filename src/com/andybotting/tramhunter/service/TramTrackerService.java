package com.andybotting.tramhunter.service;

import java.util.List;

import com.andybotting.tramhunter.objects.NextTram;
import com.andybotting.tramhunter.objects.Route;
import com.andybotting.tramhunter.objects.Stop;

/**
 * Service for retrieving Tram Tracking information.
 */
public interface TramTrackerService {

	Stop getStopInformation(int tramTrackerID) throws TramTrackerServiceException;

	List<NextTram> getNextPredictedRoutesCollection(Stop stop, Route route) throws TramTrackerServiceException;
	
	String getGUID();

}