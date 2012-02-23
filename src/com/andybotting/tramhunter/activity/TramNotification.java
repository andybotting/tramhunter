package com.andybotting.tramhunter.activity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.andybotting.tramhunter.R;

public class TramNotification extends BroadcastReceiver {

  private NotificationManager notificationManager;

  @Override
  public void onReceive(final Context context, final Intent intent) {
    notificationManager = (NotificationManager) context
        .getSystemService(Context.NOTIFICATION_SERVICE);

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
    final PendingIntent contentIntent = PendingIntent
        .getActivity(context, 0, notificationIntent, 0);

    notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

    notificationManager.notify(1, notification);
  }

}
