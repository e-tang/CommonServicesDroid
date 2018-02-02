/*
 * Copyright (C) 2015 TYONLINE TECHNOLOGY PTY. LTD.
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

package au.com.tyo.services.android.auth;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import au.com.tyo.services.sn.SNBase;
import au.com.tyo.services.sn.SocialNetwork;

public class SocialNetworkService extends Service {

	private final IBinder mBinder = new SocialNetworkBinder();

	private SocialNetwork sn;
	
	private boolean hasNetwork;

	public SocialNetwork getSocialNetwork() {
		return sn;
	}

	public class SocialNetworkBinder extends Binder {
        public SocialNetworkService getService() {
            return SocialNetworkService.this;
        }
    }

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	public boolean isServiceRunning() {
	    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (SocialNetworkService.class.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}

	public boolean isAuthenticated(int type) {
		return sn.getSocialNetwork(type).isAuthenticated();
	}
	
    @Override
	public void onCreate() {
		super.onCreate();
		
		sn = SocialNetwork.getInstance();
		
		hasNetwork = true; //assuming has it
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}
	
	public void onNetworkOnline() {
		hasNetwork = true;
		for (SNBase one : sn.getSocialNetworkList()) {
			one.createInstance();
		}
	}
	
	public void onNetworkOffline() {
		hasNetwork = false;
	}

}
