package com.andybotting.tramhunter.service;

import java.util.Vector;

import com.andybotting.tramhunter.NextTram;
import com.andybotting.tramhunter.Stop;

/**
 * Service for retrieving Tram Tracking information.
 */
public interface TramTrackerService {

	Stop getStopInformation(int tramTrackerID);

	Vector<NextTram> getNextPredictedRoutesCollection(Stop stop);

}