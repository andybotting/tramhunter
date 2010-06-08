package com.andybotting.tramhunter;

public class Preferences {
	private int id;
	private boolean displayWelcomeMessage;
	private boolean goToFavouriteOnLaunch;
	
	public Preferences(int id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
	public boolean isDisplayWelcomeMessage() {
		return displayWelcomeMessage;
	}
	public void setDisplayWelcomeMessage(boolean displayWelcomeMessage) {
		this.displayWelcomeMessage = displayWelcomeMessage;
	}
	public boolean isGoToFavouriteOnLaunch() {
		return goToFavouriteOnLaunch;
	}
	public void setGoToFavouriteOnLaunch(boolean goToFavouriteOnLaunch) {
		this.goToFavouriteOnLaunch = goToFavouriteOnLaunch;
	}
}
