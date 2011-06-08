/*  
 * Copyright 2010 Andy Botting <andy@andybotting.com>  
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
			
		}
	}
	

}