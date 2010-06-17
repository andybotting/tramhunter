package com.andybotting.tramhunter.service;

import java.util.List;

import com.andybotting.tramhunter.NextTram;
import com.andybotting.tramhunter.Stop;

/**
 * Service for retrieving Tram Tracking information.
 */
public interface TramTrackerService {

	Stop getStopInformation(int tramTrackerID);

	List<NextTram> getNextPredictedRoutesCollection(Stop stop);

}