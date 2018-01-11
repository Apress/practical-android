package com.wickham.android.connections;

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

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	private ImageView img;
	
	private static int 		timeoutConnection = 5000;
	private static int 		timeoutSocket = 	5000;
	private static Integer	timeoutReachable =	5000;
	private static Integer	updateInterval =	10000;
	
	// Ping Array Lists:
	// pingType (int): 0=isReachable 1=httpclient, 
	// connName (Str), 
	// URL or IP address (Str)
	// Response (Str)	
    ArrayList<Integer> pingType = new ArrayList<Integer>();
    ArrayList<String>  connName = new ArrayList<String>();
    ArrayList<String>  connURL  = new ArrayList<String>();
    ArrayList<String>  response = new ArrayList<String>();
    
	private static JSONArray connectionFileJson = null;
	private static String connectionFileTxt = "";
	
	// Array of TextViews which will be used to dynamically build the result screen, maximum 20.
	private static TextView[][] pingTextView = new TextView[20][3];
	
	// Define Green color and Red color values
	private static String[] textColor = new String[] {"#5d9356","#ff0000"};
	
    // Status Thread
 	Thread m_statusThread;
 	boolean m_bStatusThreadStop;
 	
 	// Clock thread
 	Thread m_clockThread;
 	boolean m_bClockThreadStop;
 	
    protected void onResume() {
       super.onResume();
       IntentFilter filter = new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
       this.registerReceiver(wifiStatusReceiver, filter);
    }
 	
	@Override
	public void onPause() {
		this.unregisterReceiver(wifiStatusReceiver);    
		super.onPause();
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
		
        // Setup the ActionBar and the Spinner in the ActionBar
        getActionBar().setDisplayShowTitleEnabled(true);
        getActionBar().setSubtitle("Practical Android");
        getActionBar().setTitle("Connections");
        
		setContentView(R.layout.main_layout);
		
    	pingType.clear();
    	connName.clear();
    	connURL.clear();
    	response.clear();
    	
    	// Read in the JSON file and build the ArrayLists
        try {
            Resources res = getResources();
            InputStream in_s = res.openRawResource(R.raw.connectionfile);

            byte[] b = new byte[in_s.available()];
            in_s.read(b);
            connectionFileTxt = (new String(b));
        } catch (Exception e) {
            e.printStackTrace();
        }
    	// build the Array lists
    	try {
			connectionFileJson = new JSONArray(connectionFileTxt);
        	for(int i=0; i<connectionFileJson.length(); i++){        		

        		int type = (Integer) jsonGetter2(connectionFileJson.getJSONArray(i),"type");
        		pingType.add(type);
        		
        		String cname = jsonGetter2(connectionFileJson.getJSONArray(i),"name").toString();
        		connName.add(cname);
        		
        		String url = jsonGetter2(connectionFileJson.getJSONArray(i),"url").toString();
        		connURL.add(url);
        		
        		String resp = jsonGetter2(connectionFileJson.getJSONArray(i),"res").toString();
        		response.add(resp);
        	}
		} catch (JSONException e) {
			e.printStackTrace();
		}	
	
		// create and run status thread
		createAndRunStatusThread(this);
	
		// create and run clock thread
		createAndRunClockThread(this);
	}

	private void updateConnectionStatus() {	
		
		// update the wi-fi status
		img = (ImageView) findViewById(R.id.image1); 
		img.setBackgroundResource(R.drawable.presence_invisible);
		if (checkInternetConnection()) {
			img.setBackgroundResource(R.drawable.presence_online);
		} else {
			img.setBackgroundResource(R.drawable.presence_busy);
		}		
		
		// Grab the LinearLayout where we will dynamically add LL for the ping Work List
        LinearLayout pingLinearLayout = (LinearLayout) findViewById(R.id.insertPings);
        pingLinearLayout.removeAllViews();
        
        // Set a LayoutParams for the new Layouts we will add for the ping Work List items status
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(5, 5, 5, 5);
        layoutParams.gravity= Gravity.CENTER;
        
        // LayoutParams for the TextViews
        final float WIDE = this.getResources().getDisplayMetrics().widthPixels;
        int valueWide = (int)(WIDE * 0.30f);	// set the columns for 1/3 screen width
        LinearLayout.LayoutParams layoutParamsTV = new LinearLayout.LayoutParams(valueWide, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParamsTV.setMargins(5, 5, 5, 5); // left,top,right.bottom
        layoutParamsTV.gravity = Gravity.CENTER;
        
        // setup a screen proportional font size
    	DisplayMetrics dMetrics = new DisplayMetrics();
    	this.getWindowManager().getDefaultDisplay().getMetrics(dMetrics);
    	int fontSize = (int)(WIDE / 36.0f / (dMetrics.scaledDensity));  // 36 is arbitrary and gives approximately 70 chars across a 7" tablet
		
		// loop through the work list, fire off a ping for each item based on the Type
        for(int i=0; i<pingType.size(); i++){
            // create the new HORizontal linearLayout for this item i
            LinearLayout newLL;
            newLL = new LinearLayout(MainActivity.this);
            newLL.setLayoutParams(layoutParams);
            newLL.setOrientation(LinearLayout.HORIZONTAL);
            newLL.setHorizontalGravity(Gravity.CENTER);
            pingLinearLayout.addView(newLL, i);
            
      	  	pingTextView[i][0] = new TextView(MainActivity.this);
      	  	pingTextView[i][0].setText(connName.get(i));
      	  	pingTextView[i][0].setTextSize(fontSize);
      	  	newLL.addView(pingTextView[i][0], 0, layoutParamsTV);
      	  	
      	  	pingTextView[i][1] = new TextView(MainActivity.this);
      	  	pingTextView[i][1].setText(connURL.get(i));
      	  	pingTextView[i][1].setTextSize(fontSize);
      	  	newLL.addView(pingTextView[i][1], 1, layoutParamsTV);
      	  	
      	  	pingTextView[i][2] = new TextView(MainActivity.this);
      	  	pingTextView[i][2].setText(response.get(i));
      	  	pingTextView[i][2].setTextSize(fontSize);
      	  	newLL.addView(pingTextView[i][2], 2, layoutParamsTV);

      	  	if (pingType.get(i) == 0) {
        		// send the ping with ICMP
        		new pingICMP(connURL.get(i),i).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      	  	}

      	  	if (pingType.get(i) == 1) {
        		// send the ping with Http
        		new pingHTTP(connURL.get(i),i).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      	  	}        
        }
		
		// update the refresh time
	    TextView textRefr = (TextView) findViewById(R.id.textUpdate);
        textRefr.setText(GetTime()); 
	}
	
	private void updateClock() {
	    TextView textTim = (TextView) findViewById(R.id.textTime);
        textTim.setText(GetTime()); 
	}
		
	// check for connectivity using ICMP
	public class pingICMP extends AsyncTask<Void, String, Integer> {
		private String ip1;
		private boolean code;
		private int item;
	    private InetAddress in1;
		
		public pingICMP(String ip, int i) {
			ip1 = ip;
			in1 = null;
			item = i;
			code = false;
		}
		protected void onPreExecute(Void ...params) {
		}
		protected Integer doInBackground(Void ...params) {
		    try {
		    	in1 = InetAddress.getByName(ip1);
		    } catch (Exception e) {
		    	code = false;
		    }
		    try {
		    	if(in1.isReachable(timeoutReachable)) {
		    		code = true;
		    	} else {
		    		code = false;
		    	}
		    } catch (Exception e) {
		    	code = false;
		    }			
			return 1; 
		}
		protected void onProgressUpdate(String msg) {	
		}
		protected void onPostExecute(Integer result) {
	    	if (code) {
	    		pingTextView[item][2].setText("Reachable");
	    		pingTextView[item][2].setTextColor(Color.parseColor(textColor[0]));  // green
	    	} else {
	    		pingTextView[item][2].setText("Not Reachable");	
	    		pingTextView[item][2].setTextColor(Color.parseColor(textColor[1]));  // red
	    	}
		}
	}

	// check for connectivity using HTTP
	private class pingHTTP extends AsyncTask<Void, String, Integer> {
		private String urlString;
		private boolean ping_success;
		private int item;
		private int status;

		private pingHTTP(String ip, int i) {
			ping_success = false;
			item = i;
			urlString = ip;
		}
		protected void onPreExecute(Void ...params) { }
		protected Integer doInBackground(Void ...params) {
			try {
				URL url = new URL(urlString);
				HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
				httpConn.setAllowUserInteraction(false);
				httpConn.setInstanceFollowRedirects(true);
				httpConn.setRequestMethod("GET");
				httpConn.connect();
				status = httpConn.getResponseCode();
				// Check for successful status code = 200 or 204
				if ((status == HttpURLConnection.HTTP_OK) || (status == HttpURLConnection.HTTP_NO_CONTENT)) ping_success = true;
			} catch (Exception e) {
				// Handle exception
				ping_success = false;
			}
			return 1;
		}
		protected void onProgressUpdate(String msg) { }
		protected void onPostExecute(Integer result) {
			if (ping_success) {
				pingTextView[item][2].setText("Status Code= " + status);
				pingTextView[item][2].setTextColor(Color.parseColor(textColor[0]));  // green
			} else {
				pingTextView[item][2].setText("Status Code= " + status);
				pingTextView[item][2].setTextColor(Color.parseColor(textColor[1]));  // red
			}
		}
	}
	
    private boolean checkInternetConnection() {
    	
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        
	    TextView connType = (TextView) findViewById(R.id.textType);
	    TextView connAvail = (TextView) findViewById(R.id.textAvail);
	    TextView connConn = (TextView) findViewById(R.id.textConn);
        
        connType.setText(getString(R.string.unknown));
        
        if ((cm != null) && (netInfo != null)) {
        	// update the connection type
        	if (netInfo.getTypeName().equalsIgnoreCase("WIFI"))
        		connType.setText(getString(R.string.wifi));    	

        	if (netInfo.getTypeName().equalsIgnoreCase("MOBILE"))
        		connType.setText(getString(R.string.mobile));  	
            
        	// update the connection status
        	if (netInfo.isAvailable()) {
        		connAvail.setText(getString(R.string.available)); 
        		connAvail.setTextColor(Color.parseColor(textColor[0]));  // green
        		if (netInfo.isConnected()) {
        			connConn.setText(getString(R.string.connected));
        			connConn.setTextColor(Color.parseColor(textColor[0]));  // green
        			return true;
        		} else {
        			connConn.setText(getString(R.string.notconnected));
        			connConn.setTextColor(Color.parseColor(textColor[1]));  // red
        			return false;
        		}
        	} else {
        		connAvail.setText(getString(R.string.notavailable));
        		connAvail.setTextColor(Color.parseColor(textColor[1]));  // red
        		return false;
        	}
        } else {
        	connType.setText(getString(R.string.unknown));
        	
        	connConn.setText(getString(R.string.notconnected));
        	connConn.setTextColor(Color.parseColor(textColor[1]));  // red
        	
        	connAvail.setText(getString(R.string.notavailable));
        	connAvail.setTextColor(Color.parseColor(textColor[1]));  // red
    		
        	return false; 
        }
    }	
	
    public void createAndRunStatusThread(final Activity act) {
        m_bStatusThreadStop=false;
		m_statusThread = new Thread(new Runnable() {
		   	public void run() {
		   		while(!m_bStatusThreadStop) {
		   			try {
		   		    	act.runOnUiThread(new Runnable() {
	   			    		public void run() {
	   			    			updateConnectionStatus();
	   			    		}
	   			    	});
		   				Thread.sleep(updateInterval);
		   			}
		   			catch(InterruptedException e) {
		   				m_bStatusThreadStop = true;
		   				messageBox(act, "Exception in status thread: " + e.toString() + " - " + e.getMessage(), "createAndRunStatusThread Error");
		   			}
		   		}
		   	}
	   	});
		m_statusThread.start();
    }
    
    public void createAndRunClockThread(final Activity act) {
        m_bClockThreadStop=false;
		m_clockThread = new Thread(new Runnable() {
		   	public void run() {
		   		while(!m_bClockThreadStop) {
		   			try {
		   		    	act.runOnUiThread(new Runnable() {
	   			    		public void run() {
	   			    			updateClock();
	   			    		}
	   			    	});
		   				Thread.sleep(1000);
		   			}
		   			catch(InterruptedException e) {
		   				m_bStatusThreadStop = true;
		   				messageBox(act, "Exception in clock thread: " + e.toString() + " - " + e.getMessage(), "createAndRunClockThread Error");
		   			}
		   		}
		   	}
	   	});
		m_clockThread.start();
    }
    
    public void messageBox(final Context context, final String message, final String title) {
    	this.runOnUiThread(
			new Runnable() {
				public void run() {
					final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
					alertDialog.setTitle(title);
					alertDialog.setIcon(android.R.drawable.stat_sys_warning);
					alertDialog.setMessage(message);
					alertDialog.setCancelable(false);
					alertDialog.setButton("Back", new DialogInterface.OnClickListener()
					{
						public void onClick(DialogInterface dialog, int which) {               
							alertDialog.cancel();
						}
					});    	
					alertDialog.show();   	
				}
			}
    	);
    }
    
    BroadcastReceiver wifiStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TextView textBR = (TextView) findViewById(R.id.textBroadcastReceiver);     	
           	SupplicantState supState;
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            supState = wifiInfo.getSupplicantState();
            if (supState.equals(SupplicantState.COMPLETED)) {
            	// wifi is connected
            	textBR.setText(getString(R.string.connected));
            	textBR.setTextColor(Color.parseColor(textColor[0]));  // green
            } else if (supState.equals(SupplicantState.SCANNING)) {
            	// no wifi so give an update
            	textBR.setText(getString(R.string.scanning));
            	textBR.setTextColor(Color.parseColor(textColor[1]));  // red
            } else if (supState.equals(SupplicantState.DISCONNECTED)) {
            	// wifi not connected
            	textBR.setText(getString(R.string.notconnected));
            	textBR.setTextColor(Color.parseColor(textColor[1]));  // red
            }
            checkInternetConnection();
        }
    };
        
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
    
	private Object jsonGetter2(JSONArray json, String key) {
		Object value = null;
		for (int i=0; i<json.length(); i++) {
			try {
				JSONObject obj = json.getJSONObject(i);
				if (obj.has(key)) {
					value = obj.get(key);
				}
			} catch (JSONException e) {
				Log.v("jsonGetter2 Exception",e.toString());
			}
		}
		return value;
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
    
}