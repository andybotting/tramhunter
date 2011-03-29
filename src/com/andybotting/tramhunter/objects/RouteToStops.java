package com.andybotting.tramhunter.objects;

import java.util.List;

public class RouteToStops {
	private final Route route;
	private final List<Stop> stops;

	public RouteToStops(Route route, List<Stop> stops) {
		this.route = route;
		this.stops = stops;
	}

	public Route getRoute() {
		return route;
	}

	public List<Stop> getStops() {
		return stops;
	}
}
