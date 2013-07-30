/*  
 * Copyright 2013 Andy Botting <andy@andybotting.com>
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

package com.andybotting.tramhunter.util;

import java.util.Date;

public class StringUtil {

	/**
	 * @param dateStr
	 * @return
	 */
	public static String humanFriendlyDate(long time) {
		
		// today
		Date today = new Date();
	
		// how much time since (ms)
		Long duration = today.getTime() - time;
	
		int second = 1000;
		int minute = second * 60;
		int hour = minute * 60;
		int day = hour * 24;
	
		if (duration < minute) {
			int n = (int) Math.floor(duration / second);
			return n + "s";
		}

		if (duration < hour) {
			int n = (int) Math.floor(duration / minute);
			return n + "m";
		}
	
		if (duration < day) {
			int n = (int) Math.floor(duration / hour);
			return n + "h";
		}

		if (duration < day * 365) {
			int n = (int) Math.floor(duration / day);
			return n + "d";
		} else {
			return ">1y";
		}
	}
	
	/**
	 * @param dateStr
	 * @return
	 */
	public static String humanFriendlyDate(Date date) {
		return humanFriendlyDate(date.getTime());
	}
	
}
