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

/**
 * A class that contains any  framework's none public data
 */
public class PlatformConfig {

    /**
     * from the framework's Intent.java
     */
    public static final String ACTION_REQUEST_SHUTDOWN = "android.intent.action.ACTION_REQUEST_SHUTDOWN";

    /**
     * from the framework's Intent.java
     */
    public static final String EXTRA_KEY_CONFIRM = "android.intent.extra.KEY_CONFIRM";

    /**
     * from the framework's AlarmManager.java
     */
    public static final int RTC_POWEROFF_WAKEUP = 4;
    
    
    public static final String MSIM_TELEPHONY_SERVICE = "phone_msim";
    public static final String TELEPHONY_SERVICE = "phone";
    
    public static final String SENSOR_MUTE = "sensor_mute_value";
}
