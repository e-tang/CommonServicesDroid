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


import android.net.Uri;

public class Callback extends au.com.tyo.services.Callback {

	private Uri uri;

    public Callback(String schema, String host, String path) {
        super(schema, host, path);
    }

    public Uri toUri() {
		return Uri.parse(scheme+"://" + host + "/" + path);
	}

	public String getHomeUrl() {
		return Uri.parse(scheme+"://" + host).toString();
	}

	public String getLastPathSegment(String imgUrl) {
		Uri uri = Uri.parse(imgUrl);
		return uri.getLastPathSegment();
	}

	public String getQueryParameter(String key) {
		if (uri == null)
			uri = toUri();

		return uri.getQueryParameter(key);
	}
}
