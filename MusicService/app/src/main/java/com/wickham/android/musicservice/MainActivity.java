package com.wickham.android.musicservice;

/*
 * Copyright (C) 2016 Mark Wickham
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private boolean mIsBound = false;
	private MusicService mServ;
	
	private ServiceConnection Scon = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder binder) {
			mServ = ((MusicService.ServiceBinder)binder).getServiceInstance();
		}
		
		public void onServiceDisconnected(ComponentName name) {
			mServ = null;
		}
	};

	void doBindService() {
	 	bindService(new Intent(this,MusicService.class), Scon,Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	void doUnbindService() {
		if(mIsBound) {
			unbindService(Scon);
	      	mIsBound = false;
		}
	}
		
	@Override
	public void onDestroy() {
		doUnbindService();    
		super.onDestroy();
	}		

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        // Setup the ActionBar and the Spinner in the ActionBar
        getActionBar().setDisplayShowTitleEnabled(true);
        getActionBar().setSubtitle("Practical Android");
        getActionBar().setTitle("Music Service");
        
		setContentView(R.layout.activity_main);
		
		// Bind the Service
		doBindService();
		
		// Start the service
		Intent music = new Intent();
		music.setClass(this,MusicService.class);
		startService(music);
		
		// Set Button handlers
		((Button)findViewById(R.id.btnStart)).setOnClickListener(btnClick);
		((Button)findViewById(R.id.btnStop)).setOnClickListener(btnClick);		
		
		// Add a long click listener on the Start button which will seek forward 30 seconds
		((Button)findViewById(R.id.btnStop)).setOnLongClickListener(longClick);
	}
	
	private View.OnClickListener btnClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			switch(v.getId()) {
				case R.id.btnStart:{
					enableButtons(true);
					mServ.resumeMusic();
					break;
				}
				case R.id.btnStop:{
					enableButtons(false);
					mServ.pauseMusic();
					break;
				}				
			}
		}
	};
	
	private View.OnLongClickListener longClick = new View.OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
			mServ.forwardMusic();
			Toast.makeText(MainActivity.this, "Forwarding 60 seconds", Toast.LENGTH_SHORT).show();
			return true;
		}
	};
	
	private void enableButton(int id,boolean isEnable) {
		((Button)findViewById(id)).setEnabled(isEnable);
	}
		 
	private void enableButtons(boolean isPlaying) {
		enableButton(R.id.btnStart,!isPlaying);
		enableButton(R.id.btnStop,isPlaying);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
