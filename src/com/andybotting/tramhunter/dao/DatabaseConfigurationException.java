package com.andybotting.tramhunter.dao;

public class DatabaseConfigurationException extends RuntimeException {
	private static final long serialVersionUID = 7277028990516406213L;

	public DatabaseConfigurationException(String detailMessage,
			Throwable throwable) {
		super(detailMessage, throwable);
	}

	public DatabaseConfigurationException(String detailMessage) {
		super(detailMessage);
	}

}
