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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import au.com.tyo.services.SNBase;

public class Logout {
	
	public static void popupWanningDialog(Context context, final SNBase sn) {
		Dialog dialog = null;
		int iconId = Resources.getIconResourceId(sn.getType());
		if (iconId == -1)
			iconId = android.R.drawable.ic_dialog_alert;
		
		dialog = new AlertDialog.Builder(context)
		.setIcon(iconId)
		.setTitle(String.format("Loging out %s", sn.getSocialNetworkName()))
		.setMessage(String.format(context.getResources().getString(R.string.logout_prompt), sn.getSocialNetworkName()))
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
		{
		    public void onClick(DialogInterface dialog, int which) {
		    	sn.logout();
		    }
		
		})
		.setNegativeButton(android.R.string.cancel, null)
		.create();
		
		if(dialog != null && context instanceof Activity && !((Activity) context).isFinishing())
			dialog.show();
	}

}
