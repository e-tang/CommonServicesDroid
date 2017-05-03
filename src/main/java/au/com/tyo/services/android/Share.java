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

package au.com.tyo.services.android;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Parcelable;

public class Share {
	
	private static final String LOG_TAG = "Share";
	
	private Context context;
	
	public Share(Context context) {
		this.context = context;
	}

	public List<Intent> getShareIntents(String type, String subject, String text) {
	    Intent sendIntent = new Intent(android.content.Intent.ACTION_SEND);
	    
	    List<Intent> list = new ArrayList<Intent>();

	    List<ResolveInfo> resolveInfo = context.getPackageManager().queryIntentActivities(sendIntent, 0);
	    if (!resolveInfo.isEmpty()){
	        for (ResolveInfo info : resolveInfo) {
	            if (info.activityInfo.packageName.toLowerCase().contains(type) || 
	                    info.activityInfo.name.toLowerCase().contains(type) ) {
	            	
	            	Intent share = new Intent(android.content.Intent.ACTION_SEND);
	            	share.setType("text/plain");
	            	
	                share.putExtra(Intent.EXTRA_SUBJECT,  subject);
	                share.putExtra(Intent.EXTRA_TEXT, text);
	                
	                share.setPackage(info.activityInfo.packageName);

	                break;
	            }
	        }
	    }
        return list;
	}
	
	/**
	 * 
	 * e.g. createSelectedShareIntents(new String[] {"facebook", "twitter", "gmail"}, "Subject Text", "Body Text");
	 * 
	 * @param sns
	 * @param subject
	 * @param text
	 */
	public List<Intent> createSelectedShareIntents(String[] sns, String subject, String text) {
		List<Intent> intents = new ArrayList<Intent>();

		for (String sn : sns) {
			List<Intent> newIntents = getShareIntents(sn, subject, text);
			intents.addAll(newIntents);
		}

		return intents;
	}
	
	public void startShareChooserActivity(List<Intent> intents) {
		Intent chooser = Intent.createChooser(intents.remove(0),  context.getResources().getString(R.string.share_via));

		chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toArray(new Parcelable[]{}));

		context.startActivity(chooser);
	}
}
