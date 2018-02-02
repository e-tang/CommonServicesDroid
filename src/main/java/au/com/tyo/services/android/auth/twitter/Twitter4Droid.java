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

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.util.Base64;

import au.com.tyo.android.utils.BitmapUtils;
import au.com.tyo.services.android.R;
import au.com.tyo.services.android.auth.SecretSafe;
import au.com.tyo.services.sn.twitter.SNTwitter;
import twitter4j.TwitterException;

public class Twitter4Droid extends SNTwitter {
	
	public static final String LOG_TAG = "Twitter4Droid";

	private Context context;
	
	public Twitter4Droid(Context context) {
		super();
		
		this.context = context;
		
		setAppId(context.getResources().getString(R.string.app_socialnetwork_id));
		
		consumerKey = context.getResources().getString(R.string.oauth_key_twitter);
		
		consumerKeySecret = context.getResources().getString(R.string.oauth_secret_twitter);
		
		secrets = SecretSafe.getInstance();
		
		if (secrets == null)
			SecretSafe.setInstance((secrets = new SecretSafe(context)));
		
		this.loadSecretsFromSafe();
		
		this.createInstance();
	}
	
	public void authenticate() throws NotFoundException, TwitterException {	
		authenticate(consumerKey, consumerKeySecret);
	}
	
	@Override
	protected void openAuthorizationURL(String authorizationUrl) {
		TwitterAuthorizationActivity.startTwitterAuthorizationActivity(context, authorizationUrl);
	}

	@Override
	public void createInstance() {
		/*
		 * Android doesn't like accessing the network from main thread 
		 */
		new Thread(new Runnable() {

			@Override
			public void run() {
				Twitter4Droid.super.createInstance();
				
				if (Twitter4Droid.this.userProfileImageUrl != null)  {
					Bitmap bitmap = BitmapUtils.getBitmapFromURL(Twitter4Droid.this.userProfileImageUrl);
					if (bitmap != null) {
						String encodedImage = new String(Base64.encode(BitmapUtils.bitmapToBytesInPNG(bitmap), Base64.DEFAULT));
						getUserInfo().setBase64EncodedImage(encodedImage);
						saveUserInfo();
					}
				}
			}
			
		}).start();
		
	}

}
