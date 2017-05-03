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

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;

import java.util.LinkedList;

import au.com.tyo.services.Message;
import au.com.tyo.services.OnShareToSocialNetworkListener;
import au.com.tyo.services.SNBase;
import au.com.tyo.services.SocialNetwork;
import au.com.tyo.services.twitter.Tweet;
import twitter4j.TwitterException;

public class SocialNetworkService extends Service {
	
    public final static int QUEUE_SIZE_LIMIT_DEFAULT = 50;
    
    public final static int QUEUE_SIZE_LIMIT_DEFAULT_ON_LOW_MEMORY = 10;
    
    public final static int ATTEMPTS_TO_TRY_BEFORE_GIVING_UP = 5;

	private final IBinder mBinder = new SocialNetworkBinder();
    
    private LinkedList<Message> queue;
    
    private boolean keepItRunning;

	private SocialNetwork sn;
	
	private OnShareToSocialNetworkListener listener;
	
	private boolean hasNetwork;
	
	private Thread messageHandlingTask;

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
	
	public void addMessage(Message msg) {
		synchronized (this) {
			queue.offer(msg);
		}
		
		if (sn.getSocialNetwork(msg.getSocialNetworkToShare()).isAuthenticated())
			startMessageHandlingTask();
	}
	
    public synchronized boolean isKeepItRunning() {
		return keepItRunning;
	}

	public synchronized void setKeepItRunning(boolean keepItRunning) {
		this.keepItRunning = keepItRunning;
	}
	
    @Override
	public void onCreate() {
		super.onCreate();
		
		queue = new LinkedList<Message>();
		
		keepItRunning = true;
		
		listener = null;
		
		sn = SocialNetwork.getInstance();
		
		hasNetwork = true; //assuming has it
	}
    
    public void startMessageHandlingTask() {
    	if (messageHandlingTask == null)
    		messageHandlingTask = new Thread(new MessageHandlingTask());
    	
    	if (!messageHandlingTask.isAlive())
    		messageHandlingTask.start();
    }
	
	public void setOnShareToSocialNetworkListener(OnShareToSocialNetworkListener listener) {
		this.listener = listener;
	}
	
	public OnShareToSocialNetworkListener getOnShareToSocialNetworkListener() {
		return listener;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		keepItRunning = false;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}
	
	private class MessageHandlingTask implements Runnable/*AsyncTask<Void, Void, Void>*/ {
		
		public MessageHandlingTask() {
			Thread.currentThread().setName("MessageHandlingTask");
		}

		@Override
//		protected Void doInBackground(Void... params) {
		public void run() {
			
			while (keepItRunning) {
				if (queue.size() > 0) {
					Message msg;
					
					synchronized (SocialNetworkService.this) {
						msg = queue.poll();
						if (sn.isServiceReady(msg.getSocialNetworkToShare()))
							new MessageSharingTask().execute(msg);
						else
							SocialNetworkService.this.addMessage(msg);
					}
				}	
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
				}
			}
		}
	}
		
	private class MessageSharingTask extends AsyncTask<Message, Void, Void> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			Thread.currentThread().setName("MessageSharingTask");
		}

		@Override
		protected Void doInBackground(Message... params) {
			Message msg = params[0];
			boolean successful = false;
			int resultCode = 0;
			if (msg != null) {
				try {
					msg.setAttempts(msg.getAttempts() + 1);
					
					successful = sn.share(msg);
				}
				catch (Exception ex) {
					successful = false;
					
					if (ex instanceof TwitterException) {
						TwitterException te = (TwitterException) ex;
						resultCode = te.getErrorCode();
						if (resultCode == 186) { // over limit
							msg.getStatus().shrinkToFit(Tweet.CHARACTER_NUMBER_TO_REMOVE);
						}
						else {
							/*
							 * normally, image is the problem for failing updating status
							 */
							if (msg.getImageUrl() != null) 
								msg.removeImageUrl();
							resultCode = 0;
						}
					}
				}								

				if (!successful) {
					if (msg.getAttempts() <= ATTEMPTS_TO_TRY_BEFORE_GIVING_UP
							&& resultCode != 189)
						SocialNetworkService.this.addMessage(msg);
					else {
						if (listener != null)
							listener.onShareToSocialNetworkError();
					}
				}
				else 
					if (listener != null)
						listener.onShareToSocialNetworkSuccessfully(msg.getTitle());
			}
			return null;
		}
		
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
	
	public int getQueueSize() {
		return queue.size();
	}
}
