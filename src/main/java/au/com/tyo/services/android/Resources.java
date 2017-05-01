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

import au.com.tyo.services.R;
import au.com.tyo.services.SocialNetwork;

public class Resources {

	public static int getIconResourceId(int type) {
		return getIconResourceId(type, true);
	}
	
	public static int getIconResourceId(int type, boolean lightThemeInUsed) {
		int resId = -1;
		switch (type) {
		case SocialNetwork.TWITTER:
			resId = lightThemeInUsed ? R.drawable.ic_action_twitter_light : R.drawable.ic_action_twitter_dark;
			break;
		case SocialNetwork.FACEBOOK:
			resId = lightThemeInUsed ? R.drawable.ic_action_facebook_light : R.drawable.ic_action_facebook_dark; 
			break;
		case SocialNetwork.GOOGLE_PLUS:
			resId = lightThemeInUsed ? R.drawable.ic_action_google_plus_light : R.drawable.ic_action_google_plus_dark; 
			break;
		case SocialNetwork.LINKED_IN:
			/*
			 * TODO include linked in icons in the res
			 */
			break;
		}
		return resId;
	}
}
