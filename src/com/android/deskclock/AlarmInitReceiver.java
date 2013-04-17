/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.deskclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;

import com.android.deskclock.timer.TimerObj;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.AlarmManager;

public class AlarmInitReceiver extends BroadcastReceiver {

    // A flag that indicates that switching the volume button default was done
    private static final String PREF_VOLUME_DEF_DONE = "vol_def_done";
	private static final int ALARM_THRESHOLD = 50*1000; // 50s

    /**
     * Sets alarm on ACTION_BOOT_COMPLETED.  Resets alarm on
     * TIME_SET, TIMEZONE_CHANGED
     */
    @Override
    public void onReceive(final Context context, Intent intent) {
        final String action = intent.getAction();
        if (Log.LOGV) Log.v("AlarmInitReceiver " + action);
        
        if (context.getContentResolver() == null) {
						Log.e("AlarmInitReceiver: FAILURE unable to get content resolver.  Alarms inactive.");
						return;
				}

        final PendingResult result = goAsync();
        final WakeLock wl = AlarmAlertWakeLock.createPartialWakeLock(context);
        wl.acquire();
        AsyncHandler.post(new Runnable() {
            @Override public void run() {
                // Remove the snooze alarm after a boot.
                if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {


        			SharedPreferences prefs = context.getSharedPreferences(
        					AlarmClock.PREFERENCES, 0);

        			final long now = System.currentTimeMillis();
					boolean reason = false;
					AlarmManager am1 = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);	           
            		int reason_dev = am1.getPowerOnReason();
					Log.v("*******************The reason_dev is:"+reason_dev);
					if(reason_dev == 1){
						reason = true;
					}
        			//boolean reason = RtcPowerOn();
					Log.v("--------------------The reason of RtcPowerOn is:"+reason);
        			// power on because of alarm
        			int savedAlarmId = prefs.getInt(Alarms.PREF_POWER_OFF_ALARM_ID, 0);
        			long savedAlarmTime = prefs.getLong(Alarms.PREF_POWER_OFF_ALARM_TIME, 0);
        			if (reason == true) {
        				 // Fire power-off alarm, and return to avoid to call
						     // setNextAlert() agai
        				 if ((savedAlarmId > 0)&&(now >= savedAlarmTime)&&(now - savedAlarmTime <=ALARM_THRESHOLD)){
                             Log.v("alarm  startActivityHelper !!!!!! " );

                             startActivityHelper(context, savedAlarmId);
        					 
        					 result.finish();
        		             wl.release();
     						return;
        				 }
        			}
					else{
		   			if ((now - savedAlarmTime < ALARM_THRESHOLD)&&(now > savedAlarmTime)&&(savedAlarmId > 0))
                		{
                    		startActivityHelper(context, savedAlarmId);
							
							result.finish();
        		            wl.release();
                    		return;
                		}
					}
                    // Clear stopwatch and timers data
                    SharedPreferences prefs1 =
                            PreferenceManager.getDefaultSharedPreferences(context);
                    Log.v("AlarmInitReceiver - Reset timers and clear stopwatch data");
                    TimerObj.resetTimersInSharedPrefs(prefs1);
                    Utils.clearSwSharedPref(prefs1);

                    if (!prefs.getBoolean(PREF_VOLUME_DEF_DONE, false)) {
                        // Fix the default
                        Log.v("AlarmInitReceiver - resetting volume button default");
                        switchVolumeButtonDefault(prefs);
                    }
					Alarms.saveSnoozeAlert(context, Alarms.INVALID_ALARM_ID, -1);
                    Alarms.disableExpiredAlarms(context);
                }
                Alarms.setNextAlert(context);
                result.finish();
                Log.v("AlarmInitReceiver finished");
                wl.release();
            }
        });
    }

    private void switchVolumeButtonDefault(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(SettingsActivity.KEY_VOLUME_BEHAVIOR,
            SettingsActivity.DEFAULT_VOLUME_BEHAVIOR);

        // Make sure we do it only once
        editor.putBoolean(PREF_VOLUME_DEF_DONE, true);
        editor.apply();
    }
	private static void startActivityHelper(Context context, int poAlarmId) {

		ContentResolver contentResolver = context.getContentResolver();
		final Alarm poAlarm = Alarms.getAlarm(contentResolver, poAlarmId);
		AlarmAlertFullScreen.mTypeFlag = true;
		// Maintain a cpu wake lock until the AlarmAlert and AlarmKlaxon can
		// pick it up.
		//AlarmAlertWakeLock.acquireCpuWakeLock(context);

		// Close dialogs and window shade
		Intent closeDialogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		context.sendBroadcast(closeDialogs);

		// Decide which activity to start based on the state of the keyguard.
		Class c = AlarmAlert.class;
		KeyguardManager km = (KeyguardManager) context
				.getSystemService(Context.KEYGUARD_SERVICE);
		if (km.inKeyguardRestrictedInputMode()) {
			// Use the full screen activity for security.
			c = AlarmAlertFullScreen.class;
		}

		// launch UI, explicitly stating that this is not due to user action
		// so that the current app's notification management is not disturbed
		Intent alarmAlert = new Intent(context, c);
		poAlarm.type=Alarm.ALARM_TYPE_POWERON;
		alarmAlert.putExtra(Alarms.ALARM_INTENT_EXTRA, poAlarm);
		alarmAlert.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_NO_USER_ACTION);
		context.startActivity(alarmAlert);

		// Disable all the expired or snoozed alarm.
		Alarms.disableExpiredAlarms(context);

		// Set next alarm.
		Alarms.setNextAlert(context);

		// Play the alarm alert and vibrate the device.
		Intent playAlarm = new Intent(Alarms.ALARM_ALERT_ACTION);
		playAlarm.putExtra(Alarms.ALARM_INTENT_EXTRA, poAlarm);
		playAlarm.putExtra(Alarms.POWER_OFF_ALARM_FLAG, true); // power off alarm flag
		context.startService(playAlarm);

		// Trigger a notification that, when clicked, will show the alarm alert
		// dialog. No need to check for fullscreen since this will always be
		// launched from a user action.
		Intent notify = new Intent(context, AlarmAlert.class);
		notify.putExtra(Alarms.ALARM_INTENT_EXTRA, poAlarm);
		PendingIntent pendingNotify = PendingIntent.getActivity(context,
				poAlarm.id, notify, 0);

		// Use the alarm's label or the default label as the ticker text and
		// main text of the notification.
		String label = poAlarm.getLabelOrDefault(context);
		Notification n = new Notification(R.drawable.stat_notify_alarm, label,
				poAlarm.time);
		n.setLatestEventInfo(context, label,
				context.getString(R.string.alarm_notify_text), pendingNotify);
		n.flags |= Notification.FLAG_SHOW_LIGHTS;
		n.ledARGB = 0xFF00FF00;
		n.ledOnMS = 500;
		n.ledOffMS = 500;

		// Set the deleteIntent for when the user clicks
		// "Clear All Notifications"
		//Intent clearAll = new Intent(context, AlarmReceiver.class);
		//clearAll.setAction(Alarms.CLEAR_NOTIFICATION);
		//n.deleteIntent = PendingIntent.getBroadcast(context, 0, clearAll, 0);

		// Send the notification using the alarm id to easily identify the
		// correct notification.
		NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(poAlarm.id, n);
		//AlarmAlertWakeLock.releaseCpuLock();
	}

	private boolean RtcPowerOn() {
		BufferedReader in = null;
		String strReason = null;

		try {
			//in = new BufferedReader(new FileReader(new File(
			//		"/sys/devices/platform/bck/power_reason")));
			Log.v("/sys/bootinfo/powerup_reason");
			in = new BufferedReader(new FileReader(new File(
					"/sys/bootinfo/powerup_reason")));				
			strReason = in.readLine();
			Log.v("*********The reason get from driver is :"+strReason);
		} catch (java.io.FileNotFoundException e) {
			Log.v("read fail:file not found" + e);
		} catch (java.io.IOException e) {
			Log.v("read fail" + e);
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (java.io.IOException e) {
				Log.e("Error closing", e);
			}
		}
		
		if (strReason != null) {
			//int iReason = (Integer.valueOf(strReason) & 0x100);
			//Log.v("--iReason = " + iReason);
			//return iReason==0x100;
			if(strReason.equals("rtc_alarm"))
				return true;
		}

		return false;
	}
}
