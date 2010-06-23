package com.andybotting.tramhunter.objects;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class StopsList extends ArrayList<Stop> implements Parcelable{

	private static final long serialVersionUID = 663585476779879096L;

	public StopsList(){
		
	}
	
	public StopsList(Parcel in){
		readFromParcel(in);
	}
	
	@SuppressWarnings("unchecked")
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public StopsList createFromParcel(Parcel in) {
			return new StopsList(in);
		}

		public Object[] newArray(int arg0) {
			return null;
		}

	};
	
	private void readFromParcel(Parcel in) {
		this.clear();

		//First we have to read the list size
		int size = in.readInt();

		//Reading remember that we wrote first the Name and later the Phone Number.
		//Order is fundamental
		
		for (int i = 0; i < size; i++) {
			Stop s = new Stop();
			
			s.setId(in.readLong());
			s.setTramTrackerID(in.readInt());
			s.setFlagStopNumber(in.readString());
			s.setPrimaryName(in.readString());
			s.setSecondaryName(in.readString());
			s.setCityDirection(in.readString());
			s.setLatitude(in.readDouble());
			s.setLongitude(in.readDouble());
			s.setSuburb(in.readString());
			s.setStarred(in.readInt());
			
			this.add(s);
		}
		
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		int size = this.size();
		//We have to write the list size, we need him recreating the list
		dest.writeInt(size);
		//We decided arbitrarily to write first the Name and later the Phone Number.
		for (int i = 0; i < size; i++) {
			Stop s = this.get(i);
			
			dest.writeLong(s.getId());
			dest.writeInt(s.getTramTrackerID());
			dest.writeString(s.getFlagStopNumber());
			dest.writeString(s.getPrimaryName());
			dest.writeString(s.getSecondaryName());
			dest.writeString(s.getCityDirection());
			dest.writeDouble(s.getLatitude());
			dest.writeDouble(s.getLongitude());
			dest.writeString(s.getSuburb());
			
			if (s.isStarred())
				dest.writeInt(1);
			else
				dest.writeInt(0);
			
		}
	}
	

}