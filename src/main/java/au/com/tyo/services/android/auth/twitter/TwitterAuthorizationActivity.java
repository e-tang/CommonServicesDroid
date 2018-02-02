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

package au.com.tyo.services.android.auth.twitter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;

import au.com.tyo.services.android.R;

public class TwitterAuthorizationActivity extends Activity {
	
	private static final String LOG_TAG = "TwitterAuthorizationActivity";
	
	private static final String AUTHORIZATION_URL = "AUTHORIZATION_URL";
	private static final String AUTHORIZATION_STATUS = "AUTHORIZATION_STATUS";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		
		this.setContentView(R.layout.twitter_authorization);
		
		WebView wv = (WebView) findViewById(R.id.wv_twitter_authorization);
		
		Intent intent = this.getIntent();
			
		Bundle extras = intent.getExtras();
		if (extras != null) {
			String url = intent.getStringExtra(AUTHORIZATION_URL);
			
			wv.loadUrl(url);
		}
		else
			finish();
	}

	public static void startTwitterAuthorizationActivity(Context context, String authorizationUrl) {
		Intent intent = new Intent(context, TwitterAuthorizationActivity.class);
		intent.putExtra(AUTHORIZATION_STATUS, false); // starting the authorization
		intent.putExtra(AUTHORIZATION_URL, authorizationUrl);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		if (context instanceof Activity)
			((Activity) context).startActivity(intent);
		else
			context.startActivity(intent);
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (intent.getExtras() != null) {
			boolean status = intent.getExtras().getBoolean(AUTHORIZATION_STATUS);
			if (status)
				finish();
		}
	}

	public static void closeTwitterAuthorizationActivity(Context context) {
		Intent intent = new Intent(context, TwitterAuthorizationActivity.class);
		intent.putExtra(AUTHORIZATION_STATUS, true); // finish. close the authorization activity
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		if (context instanceof Activity)
			((Activity) context).startActivity(intent);
		else
			context.startActivity(intent);        
	}
}
