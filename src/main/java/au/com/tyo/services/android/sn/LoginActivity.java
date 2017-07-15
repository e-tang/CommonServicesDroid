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

package au.com.tyo.services.android.sn;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import au.com.tyo.services.android.R;
import au.com.tyo.services.android.sn.twitter.TwitterAuthorizationActivity;
import au.com.tyo.services.sn.SNBase;
import au.com.tyo.services.sn.SocialNetwork;

public class LoginActivity extends Activity {
	
	private static final String LOG_TAG = "LoginActivity";
	
	private int type = 0;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.login);
		
		Uri uri = null;
		final Intent intent = this.getIntent();
		if (intent != null && intent.getData() != null) {
			RetrieveSocialNetworkAccessTokenTask task = new RetrieveSocialNetworkAccessTokenTask();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, intent.getData());
			else
				task.execute(intent.getData());
		}
		finish();
//		new Thread(new Runnable() {
//
//			@Override
//			public void run() {
//				new RetrieveSocialNetworkAccessTokenTask().execute(intent.getData());
//			}
//			
//		}).run();
	}
	
	public void getAccessToken(Uri uri) {
		// find out which social network we are dealing with
		String path = uri.getPath();
		
		try {
			type = path.charAt(9) == '/' ? Integer.valueOf(path.substring(10)) : Integer.valueOf(path);
		}
		catch (Exception ex) {
			type = 1;  // twitter for default? 
		}
		
		SNBase sn = SocialNetwork.getInstance().getSocialNetwork(type);
		
		if (sn == null) 
			SocialNetwork.getInstance().setSocialNetwork(sn = SocialNetwork.createSocialNetwork(type, Callback.getDefaultCallback()));
			
        //handle returning from authenticating the user
        Callback callback = (Callback) sn.getCallback();
        callback.setUri(uri);
        try {
            sn.retrieveAccessToken();

            if (sn.hasSecret() && sn.getListener() != null) {
                sn.createInstance();

                sn.getListener().onAppAuthorized(type);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(LOG_TAG, e.getLocalizedMessage() == null ? "Error in processing the twitter access token" : e.getLocalizedMessage());
        }
		//}
	}
	
	private class RetrieveSocialNetworkAccessTokenTask extends
				AsyncTask<Uri, Void, Void> {

		@Override
		protected Void doInBackground(Uri... params) {
			Uri uri = params[0];
			
			getAccessToken(uri);
	         
	         if (type == SocialNetwork.TWITTER)
	        	 TwitterAuthorizationActivity.closeTwitterAuthorizationActivity(LoginActivity.this);
				
	         finish();
			return null;
		}
	}
}
