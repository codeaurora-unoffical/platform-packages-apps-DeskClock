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

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;
import android.app.KeyguardManager;
import android.os.Handler;
import android.os.Message;
public class PowerOnAlert  extends Activity{

    private KeyguardManager mKeyguardManager;
    private KeyguardManager.KeyguardLock mKeyguardLock;
    private static final int TIME_EXPIRED_NO_OPERATOR = 30;     // 2 * 60;
    private static final int NO_OPERATOR_ALL_TIME=100;
    private Handler mHandler = new Handler() 
    {
        @Override
        public void handleMessage(Message msg) {
          
            switch (msg.what) {
                case NO_OPERATOR_ALL_TIME:
                    Alarms.poweroffImme(PowerOnAlert.this);		
                    finish();
                    break;  
            }
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        System.out.println("power on");
        setContentView(R.layout.poweron_alert);
	    mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        Button BtnYes = (Button) findViewById(R.id.poweron_yes);
        BtnYes.requestFocus();
        BtnYes.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
		


        Button BtnNo = (Button) findViewById(R.id.poweron_no);
        BtnNo.requestFocus();
        BtnNo.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
		finish();
                Alarms.poweroffImme(PowerOnAlert.this);
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        mHandler.removeMessages(NO_OPERATOR_ALL_TIME);
        mHandler.sendEmptyMessageDelayed(NO_OPERATOR_ALL_TIME,1000 * TIME_EXPIRED_NO_OPERATOR);
        disableKeyguard();
	//AlarmAlertWakeLock.acquireCpuWakeLock(PowerOnAlert.this);
    }
    
    @Override
    protected void onStop() {
        super.onStop();
	mHandler.removeMessages(NO_OPERATOR_ALL_TIME);	
        
        enableKeyguard();		

    }
 private synchronized void enableKeyguard() {
        if (mKeyguardLock != null) {
            mKeyguardLock.reenableKeyguard();
            mKeyguardLock = null;
        }
    }

    private synchronized void disableKeyguard() {
        if (mKeyguardLock == null) {
            mKeyguardLock = mKeyguardManager.newKeyguardLock("PowerOnAlert");
            mKeyguardLock.disableKeyguard();
        }
    }
}
