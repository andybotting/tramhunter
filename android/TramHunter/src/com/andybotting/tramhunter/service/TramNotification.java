/*  
 * Copyright 2012 Tarcio Saraiva <tarcio@gmail.com> 
 * Copyright 2012 Andy Botting <andy@andybotting.com> 
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
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andybotting.tramhunter.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.activity.HomeActivity;

public class TramNotification extends BroadcastReceiver {

	private NotificationManager notificationManager;

	@Override
	public void onReceive(final Context context, final Intent intent) {
		notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		// creating a new notification
		final int icon = R.drawable.icon;
		final CharSequence tickerText = context.getString(R.string.notification_title);

		final Notification notification = new Notification(icon, tickerText, System.currentTimeMillis());
		notification.defaults |= Notification.DEFAULT_VIBRATE;
		notification.defaults |= Notification.DEFAULT_SOUND;

		// define the notification message and intent
		final CharSequence contentTitle = context.getString(R.string.notification_title);
		final CharSequence contentText = "Run Forrest, run!";

		final Intent notificationIntent = new Intent(context, HomeActivity.class);
		final PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

		notificationManager.notify(1, notification);
	}

}
