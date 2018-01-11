package com.wickham.android;

/*
 * Copyright (C) 2017 Mark Wickham
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

import java.util.ArrayList;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.iid.FirebaseInstanceId;

public class MainActivity extends Activity {	

	public static ListView list;
	public static ArrayAdapter<?> incomingPushMsg;
	public static ArrayList<String> items = new ArrayList<String>();

	private static final String TAG = MainActivity.class.getSimpleName();

	private BroadcastReceiver mRegistrationBroadcastReceiver;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
		setContentView(R.layout.activity_main);
		
        // Setup the ActionBar and the Spinner in the ActionBar
        getActionBar().setDisplayShowTitleEnabled(true);
        getActionBar().setSubtitle("Practical Android");
        getActionBar().setTitle("Push FCM");
		
		items.clear();

        // Set a name for the device and truncate to last four characters
        Global.DEVICE_NAME = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
        Global.DEVICE_NAME = Global.DEVICE_NAME.substring(Global.DEVICE_NAME.length()-4, Global.DEVICE_NAME.length());
        ((TextView) findViewById(R.id.uniqueid)).setText(Global.DEVICE_NAME);
		
		if (!isConnected()) {			
	    	AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
	    	alertDialog.setTitle("Connection");
	    	alertDialog.setIcon(android.R.drawable.stat_sys_warning);
	    	alertDialog.setMessage("Data connection not Available.");
	    	alertDialog.setCancelable(false); 
	    	alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dialog, int which) {
	    		} });
	    	alertDialog.show();
		}

        // Display the token if the device is already registered
        String token = FirebaseInstanceId.getInstance().getToken();
        ((TextView) findViewById(R.id.token)).setText(token);
        //Log.i (TAG, "Token=" + token);
		
        // create our list and custom adapter  
        list = (ListView) findViewById(R.id.list); 
        incomingPushMsg = new ArrayAdapter<String>(this,R.layout.simple_list_item_1,items);
        list.setAdapter(incomingPushMsg);
        list.setPadding(2,2,2,2);
        list.setCacheColorHint(0);
        list.setFadingEdgeLength(0);
		list.setLongClickable(true);
		
		final Button clearButton = ((Button) findViewById(R.id.clear_button));
  	  	clearButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				items.clear();
				incomingPushMsg.notifyDataSetChanged();
			}
		});

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Check the intent type
                if (intent.getAction().equals(Global.REGISTRATION_COMPLETE)) {
                    // FCM successfully registered
                    //Log.i (TAG, "Receiver=" + Global.REGISTRATION_COMPLETE);
                    items.add(0, "FCM Device has been registered");
                    incomingPushMsg.notifyDataSetChanged();
                    Toast.makeText(getApplicationContext(), "FCM Registered!", Toast.LENGTH_LONG).show();
                    // Display the token
                    String token = FirebaseInstanceId.getInstance().getToken();
                    ((TextView) findViewById(R.id.token)).setText(token);
                    Log.i (TAG, "Refreshed Token");
                } else if (intent.getAction().equals(Global.PUSH_NOTIFICATION)) {
                    // FCM message received
                    //Log.i (TAG, "Receiver=" + Global.PUSH_NOTIFICATION);
                    //String newMessage = intent.getExtras().getString(Global.PUSH_NOTIFICATION);
                    String newMessage = intent.getStringExtra(Global.EXTRA_MESSAGE);
                    items.add(0, newMessage);
                    incomingPushMsg.notifyDataSetChanged();
                    Toast.makeText(getApplicationContext(), "New Message: " + newMessage, Toast.LENGTH_LONG).show();
                }
            }
        };
	}
	
    private boolean isConnected(){
        ConnectivityManager connectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
        	NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
            	for (int i = 0; i < info.length; i++)
            		if (info[i].getState() == NetworkInfo.State.CONNECTED) {
            			return true;
            		}
        }
        return false;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	new MenuInflater(this).inflate(R.menu.actions, menu);
    	return(super.onCreateOptionsMenu(menu));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item.getItemId() == R.id.exit) {
    		finish();
    		return(true);
    	}
    	return(super.onOptionsItemSelected(item));
    }

	@Override
	protected void onResume() {
		super.onResume();
		// FCM registration complete receiver
		LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
				new IntentFilter(Global.REGISTRATION_COMPLETE));

		// FCM new message receiver
		LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
				new IntentFilter(Global.PUSH_NOTIFICATION));
	}

	@Override
	protected void onPause() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
		super.onPause();
	}
    
}
