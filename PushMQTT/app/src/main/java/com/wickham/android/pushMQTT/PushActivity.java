package com.wickham.android.pushMQTT;

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
 * This app is based on the original Dale Lane implementation which was based
 * on the Google long lived service example. See links in Chapter references.
 *
 */

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDefaultFilePersistence;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.internal.MemoryPersistence;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings.Secure;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PushActivity extends Activity {
	
	private String mDeviceID, mUniqueID;
    ListView list;
    private ArrayAdapter<?> incomingPushMsg;
    
    private String brokerTopic = "";
    private String brokerMsg = "";
    
	// MQTT Objects
	private MemoryPersistence 		mMemStore; // On Fail reverts to MemoryStore
	private MqttConnectOptions 		mOpts; // Connection Options
	private MqttClient 				mClient; // Mqtt Client
    
    @Override
    protected void onResume() {
    	super.onResume();
    	
        this.registerReceiver(messageReceiver, new IntentFilter(MqttService.NEW_MESSAGE));
        this.registerReceiver(messageReceiver, new IntentFilter(MqttService.START_SERVICE));
        this.registerReceiver(messageReceiver, new IntentFilter(MqttService.STOP_SERVICE));
        this.registerReceiver(messageReceiver, new IntentFilter(MqttService.LAST_KEEP_ALIVE));
        this.registerReceiver(messageReceiver, new IntentFilter(MqttService.DELIVERY_COMPLETE));
        this.registerReceiver(messageReceiver, new IntentFilter(MqttService.TOPIC));
        this.registerReceiver(messageReceiver, new IntentFilter(MqttService.CONNECTION_STATUS));
       
  	  	SharedPreferences p = getSharedPreferences(MqttService.TAG, MODE_PRIVATE);
  	  	boolean started = p.getBoolean(MqttService.PREF_STARTED, false);
  	  	
  		((Button) findViewById(R.id.start_button)).setEnabled(!started);
  		((Button) findViewById(R.id.stop_button)).setEnabled(started);
  		
        incomingPushMsg.notifyDataSetChanged();
    }
    
	@Override
	public void onPause()
    {
		this.unregisterReceiver(messageReceiver);    
		super.onPause();
    }
	
//	Need handler for callbacks to the UI thread
    final Handler mHandler = new Handler();
	
    final Runnable feedbackNotSent = new Runnable() {
        public void run() {
        	failedAuth0();
        }
    };
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Global.items.clear();
        
        setContentView(R.layout.main);
        
        // Setup the ActionBar and the Spinner in the ActionBar
        getActionBar().setDisplayShowTitleEnabled(true);
        getActionBar().setSubtitle("Android Software Development");
        getActionBar().setTitle("Push MQTT");
        
        mUniqueID = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);    
  	    ((TextView) findViewById(R.id.uid_text)).setText(mUniqueID);
  	    
  	    // set up our device ID with the last 4 chars of the uniqueID
        mDeviceID = mUniqueID.substring(mUniqueID.length()-4, mUniqueID.length());
        // when we push messages, set up the default topic to be ourself
        brokerTopic = mDeviceID;
  	  	((TextView) findViewById(R.id.target_text)).setText(mDeviceID);
  	  	
  	  	((TextView) findViewById(R.id.broker_text)).setText(Global.MQTT_BROKER);
  	  	
    	Editor editor = getSharedPreferences(MqttService.TAG, MODE_PRIVATE).edit();
    	editor.putString(MqttService.PREF_DEVICE_ID, mDeviceID);
    	editor.commit();
  	  	
        // create our list and custom adapter  
        list = (ListView) findViewById(R.id.list); 
        incomingPushMsg = new ArrayAdapter<String>(this,R.layout.simple_list_item_1,Global.items);
        list.setAdapter(incomingPushMsg);
        list.setPadding(2,2,2,2);
        list.setCacheColorHint(0);
        list.setFadingEdgeLength(0);
		list.setLongClickable(true);
		
  	  	final Button startButton = ((Button) findViewById(R.id.start_button));
  	  	final Button stopButton = ((Button) findViewById(R.id.stop_button));
  	  	final Button clearButton = ((Button) findViewById(R.id.clear_button));
  	  	final Button pushButton = ((Button) findViewById(R.id.push_button));
  	  	
  	  	startButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				MqttService.actionStart(getApplicationContext());		        				
			}
		});
  	  	stopButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MqttService.actionStop(getApplicationContext());		        												
			}
		});
  	  	clearButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Global.items.clear();
				incomingPushMsg.notifyDataSetChanged();
			}
		});
  	  	pushButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
  	            final Dialog dialogPush = new Dialog(PushActivity.this);

                dialogPush.setContentView(R.layout.push_message);
                dialogPush.setCancelable(true);
                dialogPush.setCanceledOnTouchOutside(true);
                dialogPush.setTitle("Push a message");
                
                // display the brokerIP
        		TextView brok = (TextView) dialogPush.findViewById(R.id.brokerName);
        		String url = String.format(Locale.US, Global.MQTT_URL_FORMAT, Global.MQTT_BROKER, Global.MQTT_PORT);
        		brok.setText(url);
                
                // set a default topic to our own device
        		EditText topET = (EditText) dialogPush.findViewById(R.id.topicET);
        		topET.setText(brokerTopic);
				       	   
                Button push = (Button) dialogPush.findViewById(R.id.ButtonPush);
                push.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
        				// get the topic
                		EditText topET = (EditText) dialogPush.findViewById(R.id.topicET);
                		brokerTopic =  topET.getText().toString();
        				// get the msg
                		EditText msgET = (EditText) dialogPush.findViewById(R.id.messageET);
                		brokerMsg =  msgET.getText().toString();
                		pushItOut(brokerTopic, brokerMsg);                    	
                		dialogPush.dismiss();
                    }
                });
                dialogPush.show();
			}
		});
    }
        
    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) { 
            String action = intent.getAction();
            if(action.equalsIgnoreCase(MqttService.NEW_MESSAGE)){    
            	Bundle extra = intent.getExtras();
            	String message = extra.getString("message");
            	String topic = extra.getString("topic");
                Toast.makeText(PushActivity.this, "A new message with topic=" + topic + " has arrived", Toast.LENGTH_LONG).show();           
                incomingPushMsg.notifyDataSetChanged();
                list.scrollTo(0, 0);
            }
            if(action.equalsIgnoreCase(MqttService.STOP_SERVICE)){   
          	  	Button startButton = ((Button) findViewById(R.id.start_button));
          	  	Button stopButton = ((Button) findViewById(R.id.stop_button));
		  		startButton.setEnabled(true);
		  		stopButton.setEnabled(false);  
            }            
            if(action.equalsIgnoreCase(MqttService.START_SERVICE)){ 
          	  	Button startButton = ((Button) findViewById(R.id.start_button));
          	  	Button stopButton = ((Button) findViewById(R.id.stop_button));
		  		startButton.setEnabled(false);
		  		stopButton.setEnabled(true);
            }
            if(action.equalsIgnoreCase(MqttService.LAST_KEEP_ALIVE)){ 
        	    TextView textTim = (TextView) findViewById(R.id.keep_text);
                textTim.setText(GetTime()); 
            }
            if(action.equalsIgnoreCase(MqttService.DELIVERY_COMPLETE)){ 
        	    TextView textDeliv = (TextView) findViewById(R.id.deliv_text);
                textDeliv.setText(GetTime()); 
            }
            if(action.equalsIgnoreCase(MqttService.TOPIC)){    
            	Bundle extra = intent.getExtras();
            	String topic = extra.getString("topic");
        	    TextView textTop = (TextView) findViewById(R.id.topic_text);
                textTop.setText(topic); 
            }
            if(action.equalsIgnoreCase(MqttService.CONNECTION_STATUS)){    
            	Bundle extra = intent.getExtras();
            	String status = extra.getString("status");
        	    TextView textCon = (TextView) findViewById(R.id.connected_text);
                textCon.setText(status); 
            }
        }
    };
    
    private void pushItOut(final String top, final String msg) {
    	final ProgressDialog pd = ProgressDialog.show(PushActivity.this,"Pushing","Push a message...",true, false); 
    	new Thread(new Runnable(){
    		public void run(){	
    			try {
    				if (checkInternetConnection()) {	
    					// publish the message
    					String url = String.format(Locale.US, Global.MQTT_URL_FORMAT, Global.MQTT_BROKER, Global.MQTT_PORT);
    					mMemStore = new MemoryPersistence();
    					mClient = new MqttClient(url,mUniqueID,mMemStore);
    					// publish the msg to the topic
    					MqttTopic mqttTopic = mClient.getTopic(top);
    					MqttMessage message = new MqttMessage(msg.getBytes());
    					message.setQos(Global.MQTT_QOS_2);
    					mOpts = new MqttConnectOptions();
    					mOpts.setCleanSession(Global.MQTT_CLEAN_SESSION);
    					//mOpts.setUserName(Global.MQTT_USER);
    					//mOpts.setPassword(Global.MQTT_PASSWORD);
    					mClient.connect(mOpts);
    					mqttTopic.publish(message);
    					mClient.disconnect();
    				} else {
    					mHandler.post(feedbackNotSent);
    				}
    			} catch (Exception e) {
    				mHandler.post(feedbackNotSent);
    			} 
    			pd.dismiss();
    		}
    	}).start();     
    }
    
    
    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.wickham.android.pushMQTT.MqttService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    public boolean checkInternetConnection() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        // test for connection
        if (cm.getActiveNetworkInfo() != null
                && cm.getActiveNetworkInfo().isAvailable()
                && cm.getActiveNetworkInfo().isConnected()) {
            return true;
        } else {
            return false;
        }
    }
    
    public void failedAuth0() {
    	AlertDialog alertDialog = new AlertDialog.Builder(PushActivity.this).create();
    	alertDialog.setTitle("Pushing");
    	alertDialog.setIcon(android.R.drawable.stat_sys_warning);
    	alertDialog.setMessage("Your push message could not be sent at this time.");
    	alertDialog.setCancelable(false); 
    	alertDialog.setButton("Back", new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int which) {
    		} });
    	alertDialog.show();
    }
    
    public static String GetTime() {
    	Date dt = new Date();
    	Integer hours = dt.getHours();
    	String formathr = String.format("%02d", hours);
    	Integer minutes = dt.getMinutes();
    	String formatmin = String.format("%02d", minutes);
    	Integer seconds = dt.getSeconds();
    	String formatsec = String.format("%02d", seconds);
    	String curTime = formathr + ":" + formatmin + ":" + formatsec;
    	return curTime;
    }
        
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	new MenuInflater(this).inflate(R.menu.actions, menu);
    	return(super.onCreateOptionsMenu(menu));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item.getItemId() == R.id.exit) {
    		//MqttService.actionStop(getApplicationContext());
    		finish();
    		return(true);
    	}
    	return(super.onOptionsItemSelected(item));
    }  

}