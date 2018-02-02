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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import au.com.tyo.services.sn.Secret;

public class SecretOnDroid {

	public static final String PREF_STORED_SECRET_TOKEN = "pref_sn4j_stored_secret_token";
	
	public static final String PREF_STORED_SECRET_SECRET = "pref_sn4j_stored_secret_secret";
	
//	public static final String PREF_STORED_SECRET_TYPE = "pref_sn4j_stored_secret_type";
//	
//	public static final String PREF_STORED_SECRET_AUTH_TYPE = "pref_sn4j_stored_secret_auth_type";
	
	private Context context;
	
	private String prefTokenStr;
	
	private String prefSecretStr;
	
//	private String prefTypeStr;
//	
//	private String prefAuthTypeStr;
	
	public SecretOnDroid(Context context) {
		this.context = context;	
	}
	
	public void setType(Secret secret) {
		
		String ext =  "_" + secret.buildKey();

		prefTokenStr = PREF_STORED_SECRET_TOKEN + ext;
		
		prefSecretStr = PREF_STORED_SECRET_SECRET + ext;
		
//		prefTypeStr = PREF_STORED_SECRET_TYPE  + ext;
//		
//		prefAuthTypeStr = PREF_STORED_SECRET_AUTH_TYPE + ext;
	}
	
	public void load(Secret secret) {
		this.setType(secret);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		secret.setToken(prefs.getString(prefTokenStr, ""));
		secret.setSecret(prefs.getString(prefSecretStr, ""));
//		secret.setType(prefs.getInt(PREF_STORED_SECRET_TYPE, SocialNetwork.ANY));
//		secret.setTypeAuth(prefs.getInt(PREF_STORED_SECRET_AUTH_TYPE, SocialNetwork.INFORMATION_ANY));
	}

	public void save(Secret secret) {
		this.setType(secret);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		SharedPreferences.Editor editor = prefs.edit();
		
		editor.putString(prefTokenStr, secret.getToken());
		
		editor.putString(prefSecretStr , secret.getSecret());
		
//		editor.putInt(prefTypeStr, secret.getType());
//		
//		editor.putInt(prefAuthTypeStr, secret.getTypeAuth());
		
		editor.commit();
	}
	
}
