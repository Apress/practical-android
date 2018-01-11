package com.wickham.android.pushMQTT;

import java.io.IOException;
import java.util.Locale;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDefaultFilePersistence;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.internal.MemoryPersistence;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;

public class MqttService extends Service implements MqttCallback
{
	public static final String 		DEBUG_TAG = "MqttService"; // Debug TAG
	
	private static final String		MQTT_THREAD_NAME = "MqttService[" + DEBUG_TAG + "]"; // Handler Thread ID
	
	private static final String 	ACTION_START 	 = DEBUG_TAG + ".START"; // Action to start
	private static final String 	ACTION_STOP		 = DEBUG_TAG + ".STOP"; // Action to stop
	private static final String 	ACTION_KEEPALIVE = DEBUG_TAG + ".KEEPALIVE"; // Action to keep alive used by alarm manager
	private static final String 	ACTION_RECONNECT = DEBUG_TAG + ".RECONNECT"; // Action to reconnect
	
	// Notification title
	public static String			NOTIF_TITLE = "New Message Arrived"; 	
	
	public static final String 		TAG = "MqttService";
	
	// Connection log for the push service. "Must Have" for debugging.
	private ConnectionLog 			mLog;

	public static final String		PREF_DEVICE_ID = "deviceID";
	public static final String		NEW_MESSAGE = "newmessage";
	public static final String		START_SERVICE = "startservice";
	public static final String		STOP_SERVICE = "stopservice";
	public static final String		PREF_STARTED = "isStarted";
	public static final String		LAST_KEEP_ALIVE = "lastkeepalive";
	public static final String		DELIVERY_COMPLETE = "deliverycomplete";
	public static final String		TOPIC = "topic";
	public static final String		CONNECTION_STATUS = "connectionstatus";

	private boolean 				mStarted = false; // Is the Client started?
	
	private String 					mDeviceId; // Device ID, Use a random number, or we could use Secure.ANDROID_ID, Note 23 char limit!!
	
	private Handler 				mConnHandler; // Separate Handler thread for networking
	
	// MQTT Objects
	private MqttTopic 				mqttTopic;
	private MqttDefaultFilePersistence mDataStore; // Defaults to FileStore
	private MemoryPersistence 		mMemStore; // On Fail reverts to MemoryStore
	private MqttConnectOptions 		mOpts; // Connection Options
	private MqttTopic 				mKeepAliveTopic; // Instance Variable for Keepalive topic
	private MqttClient 				mClient; // Mqtt Client
	
	private AlarmManager 			mAlarmManager; // Alarm manager to perform repeating tasks
	private ConnectivityManager 	mConnectivityManager; // To check for connectivity changes
	private SharedPreferences       mPrefs; // used to store service state and uniqueId

	// Start MQTT Client
	public static void actionStart(Context ctx) {
		Intent i = new Intent(ctx,MqttService.class);
		i.setAction(ACTION_START);
		ctx.startService(i);
	}
	
	// Stop MQTT Client
	public static void actionStop(Context ctx) {
		Intent i = new Intent(ctx,MqttService.class);
		i.setAction(ACTION_STOP);
		ctx.startService(i);
	}
	
	// Send a KeepAlive Message
	public static void actionKeepalive(Context ctx) {
		Intent i = new Intent(ctx,MqttService.class);
		i.setAction(ACTION_KEEPALIVE);
		ctx.startService(i);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		try {
			mLog = new ConnectionLog();
			Log.i(TAG, "Opened log at " + mLog.getPath());
		} catch (IOException e) {
			Log.e(TAG, "Failed to open log", e);
		}
		log("Creating service");
		HandlerThread thread = new HandlerThread(MQTT_THREAD_NAME);
		thread.start();
		mConnHandler = new Handler(thread.getLooper());
		try {
			mDataStore = new MqttDefaultFilePersistence(getCacheDir().getAbsolutePath());
		} catch(MqttPersistenceException e) {
			e.printStackTrace();
			mDataStore = null;
			mMemStore = new MemoryPersistence();
		}
		mOpts = new MqttConnectOptions();
		mOpts.setCleanSession(Global.MQTT_CLEAN_SESSION);
		
		// If you implement user/password control on the server, following is required
		//mOpts.setUserName(Global.MQTT_USER);
		//mOpts.setPassword(Global.MQTT_PASSWORD);
		
		mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		mPrefs = getSharedPreferences(TAG, MODE_PRIVATE);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		// For deviceID we will use the id supplied by PushActivity via Preferences
		mDeviceId = mPrefs.getString(PREF_DEVICE_ID, null);
				
		String action = intent.getAction();
		//log("Received action of " + action + " with DeviceID=" + mDeviceId);
		if(action == null) {
			log("Starting service with no action, Probably from a crash");
		} else {
			if(action.equals(ACTION_START)) {
				start();
			} else if(action.equals(ACTION_STOP)) {
				stop();
			} else if(action.equals(ACTION_KEEPALIVE)) {
					// update the activity with the last received keep alive
					Intent i = new Intent(LAST_KEEP_ALIVE);  
					sendBroadcast(i);
					// start a new keep alive
					keepAlive();
			} else if(action.equals(ACTION_RECONNECT)) {
				if(isNetworkAvailable()) {
					reconnectIfNecessary();
				}
			}
		}
		return START_REDELIVER_INTENT;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		log("Service destroyed (started=" + mStarted + ")");
		// Stop the services, if it has been started
		if (mStarted == true) {
			stop();
		} else {
			setStarted(false);
			// Let the Activity know the service is STOPPED
			Intent i = new Intent(STOP_SERVICE);  
			sendBroadcast(i);
		}
		try {
			if (mLog != null)
				mLog.close();
		} catch (IOException e) {}		
	}
	
	// Connect to the Mqtt Broker, listen for Connectivity changes via ConnectivityManager.CONNECTVITIY_ACTION BroadcastReceiver
	private synchronized void start() {
		if(mStarted) {
			log("Attempt to start while already started");
			return;
		}
		if(hasScheduledKeepAlives()) {
			stopKeepAlives();
		}
		connect();
		setStarted(true);
		// Let the Activity know the service is STARTED
		Intent i = new Intent(START_SERVICE);  
		sendBroadcast(i);
		
		registerReceiver(mConnectivityReceiver,new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}
	
	// Attempts to stop the Mqtt client as well as halting all keep alive messages queued in the alarm manager
	private synchronized void stop() {
		if(!mStarted) {
			log("Attempt to stop connection that isn't running");
			setStarted(false);
			return;
		}
		if(mClient != null) {
			mConnHandler.post(new Runnable() {
				@Override
				public void run() {
					try {
						log("mClient.disconnect");
						mClient.disconnect();
						Intent i = new Intent(CONNECTION_STATUS);  
						i.putExtra("status", "Not Connected");
						sendBroadcast(i);
						i = new Intent(TOPIC);  
						i.putExtra("topic", "");
						sendBroadcast(i);
					} catch(MqttException ex) {
						log("stop exception ex=" + ex);
					}
					mClient = null;
					mStarted = false;
					// Save stopped state in the preferences
					setStarted(false);
					stopKeepAlives();
					// Let the Activity know the service is STOPPED
					Intent i = new Intent(STOP_SERVICE);  
					sendBroadcast(i);
				}
			});
		}
		unregisterReceiver(mConnectivityReceiver);
	}
	
	// Connects to the broker with the appropriate datastore
	private synchronized void connect() {
		String url = String.format(Locale.US, Global.MQTT_URL_FORMAT, Global.MQTT_BROKER, Global.MQTT_PORT);
		try {
			if(mDataStore != null) {
				mClient = new MqttClient(url,mDeviceId,mDataStore);
				log("Connecting url=" + url + " mDeviceId=" + mDeviceId + " mDataStore=" + mDataStore);
			} else {
				mClient = new MqttClient(url,mDeviceId,mMemStore);
				log("Connecting url=" + url + " mDeviceId=" + mDeviceId + " mMemStore=" + mMemStore);
			}
		} catch(MqttException e) {
			log("Sync Exception e=" + e);
		}
		mConnHandler.post(new Runnable() {
			@Override
			public void run() {
				try {
					// Set the callback first
					mClient.setCallback(MqttService.this);
					// Connect
					mClient.connect(mOpts);
					// Update the activity
					Intent i = new Intent(CONNECTION_STATUS);  
					i.putExtra("status", "Connected");
					sendBroadcast(i);
					
					log("Connect Opts=" + mOpts);
										
					// Subscribe to an initial topic, which is combination of client ID and device ID.
					String initTopic = mDeviceId;

					if ((mClient == null) || (mClient.isConnected() == false)) {
						// quick sanity check - don't try and subscribe if we don't have a connection
						log("Connection error. No connection while trying to subscribe");	
					} else {									
						log("Subscribing to: " + initTopic);
						mClient.subscribe(initTopic, Global.MQTT_QOS_0);
						i = new Intent(TOPIC);  
						i.putExtra("topic", initTopic);
						sendBroadcast(i);
						// Also include a topic so we can boradcast to everyone
						mClient.subscribe("everyone", Global.MQTT_QOS_0);
					}
					
					mStarted = true; // Service is now connected
					setStarted(true);
					log("Successfully connected and subscribed starting keep alives");
					startKeepAlives();
				} catch(MqttException e) {
					log("ConnHandle Exception e=" + e);
				}
			}
		});
	}
	
	// Schedule KeepAlive via a PendingIntent in the Alarm Manager
	private void startKeepAlives() {
		Intent i = new Intent();
		i.setClass(this, MqttService.class);
		i.setAction(ACTION_KEEPALIVE);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
				System.currentTimeMillis() + Global.MQTT_KEEP_ALIVE,
				Global.MQTT_KEEP_ALIVE, pi);
	}
	
	// Cancels the Pending Intent in the alarm manager
	private void stopKeepAlives() {
		Intent i = new Intent();
		i.setClass(this, MqttService.class);
		PendingIntent pi = PendingIntent.getService(this, 0, i , 0);
		mAlarmManager.cancel(pi);
	}
	
	// Publishes a KeepALive to the topic in the broker
	private synchronized void keepAlive() {
		if(isConnected()) {
			try {
				sendKeepAlive();
				return;
			} catch(MqttConnectivityException ex) {
				log("exception cex=" + ex);
				reconnectIfNecessary();
			} catch(MqttPersistenceException ex) {
				log("exception pex=" + ex);
			} catch(MqttException ex) {
				log("exception ex=" + ex);
				return;
			}
		}
	}
	
	// Received Message from broker
	@Override
	public void messageArrived(MqttTopic topic, MqttMessage message) throws Exception {
		String msg = new String(message.getPayload());
		String top = new String(topic.getName());
		log("Received message: Topic=" + top + " Message=" + msg + " QoS=" + message.getQos());	
		generateNotification(getApplicationContext(), top);
		Global.items.add(0, msg);
		Intent i = new Intent(NEW_MESSAGE);  
		i.putExtra("message", msg);
		i.putExtra("topic", top);
		sendBroadcast(i);
	}
	
	// Log helper function
	private void log(String message) {
		log(message, null);
	}
	private void log(String message, Throwable e) {
		if (e != null) {
			Log.e(TAG, message, e);
		} else {
			Log.i(TAG, message);			
		}
		if (mLog != null)
		{
			try {
				mLog.println(message);
			} catch (IOException ex) {}
		}		
	}
	
	// Checks the current connectivity and reconnects if it is required.
	private synchronized void reconnectIfNecessary() {
		if(mStarted && mClient == null) {	
			connect();
		}
	}
	
	// Query's the NetworkInfo via ConnectivityManager to return the current connected state
	private boolean isNetworkAvailable() {
		NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
		return (info == null) ? false : info.isAvailable();
	}
	
	// Verifies the client State with our local connected state
	private boolean isConnected() {
		if(mStarted && mClient != null && !mClient.isConnected()) {
			Log.i(DEBUG_TAG,"Mismatch between what we think is connected and what is connected");
		}
		if(mClient != null) {
			return (mStarted && mClient.isConnected()) ? true : false;
		}
		return false;
	}
	
	// Receiver that listens for connectivity change via ConnectivityManager
	private final BroadcastReceiver mConnectivityReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Get network info
			NetworkInfo info = (NetworkInfo)intent.getParcelableExtra (ConnectivityManager.EXTRA_NETWORK_INFO);
			// Is there connectivity?
			boolean hasConnectivity = (info != null && info.isConnected()) ? true : false;
			log("Connectivity changed: connected=" + hasConnectivity);
			if(!hasConnectivity) {
				Intent i = new Intent(CONNECTION_STATUS);  
				i.putExtra("status", "Not Connected");
				sendBroadcast(i);
				i = new Intent(TOPIC);  
				i.putExtra("topic", "");
				sendBroadcast(i); 
			} else {
				log("Attempting reconnect");
				reconnectIfNecessary();
				//connect();
			}
		}
	};
	
	// Sends a Keep Alive message to the specified topic
	private synchronized MqttDeliveryToken sendKeepAlive()
		throws MqttConnectivityException, MqttPersistenceException, MqttException {
		if(!isConnected()) throw new MqttConnectivityException();
		mKeepAliveTopic = mClient.getTopic(String.format(Locale.US, Global.MQTT_KEEP_ALIVE_TOPIC_FORMAT,mDeviceId));
		log("Sending Keep Alive to: " + Global.MQTT_BROKER + " Topic: " + String.format(Locale.US, Global.MQTT_KEEP_ALIVE_TOPIC_FORMAT,mDeviceId));
		MqttMessage message = new MqttMessage(Global.MQTT_KEEP_ALIVE_MESSAGE);
		message.setQos(Global.MQTT_KEEP_ALIVE_QOS);
		return mKeepAliveTopic.publish(message);
	}
	
	// Query's the AlarmManager to check if there is a keep alive currently scheduled
	private synchronized boolean hasScheduledKeepAlives() {
		Intent i = new Intent();
		i.setClass(this, MqttService.class);
		i.setAction(ACTION_KEEPALIVE);
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, PendingIntent.FLAG_NO_CREATE);
		return (pi != null) ? true : false;
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	// Connectivity Lost from broker
	@Override
	public void connectionLost(Throwable arg0) {
		log("ConnectionLost");
        // we protect against the phone switching off while we're doing this by requesting a wake lock
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
        wl.acquire();
		stopKeepAlives();
		//setStarted(false);
		Intent i = new Intent(CONNECTION_STATUS);  
		i.putExtra("status", "Not Connected");
		sendBroadcast(i);
		i = new Intent(TOPIC);  
		i.putExtra("topic", "");
		sendBroadcast(i);
		mClient = null;
		if(isNetworkAvailable()) {
			log("Attempting reconnect");
			reconnectIfNecessary();
		}
        // we're finished - if the phone is switched off, it's okay for the CPU to sleep now
        wl.release();
	}
	
	// Publish Message Completion
	@Override
	public void deliveryComplete(MqttDeliveryToken arg0) {
		Intent i = new Intent(DELIVERY_COMPLETE);  
		sendBroadcast(i);
	}
	
	// MqttConnectivityException Exception class
	private class MqttConnectivityException extends Exception {
		private static final long serialVersionUID = -7385866796799469420L; 
	}
	
	// Reads whether or not the service has been started from the preferences
	private boolean wasStarted() {
		return mPrefs.getBoolean(PREF_STARTED, false);
	}
	// Sets whether or not the service has been started in the preferences.
	private void setStarted(boolean started) {
		mPrefs.edit().putBoolean(PREF_STARTED, started).commit();		
		mStarted = started;
	}	
	
    // Issues a notification to inform the user that server has sent a message.
    private static void generateNotification(Context context, String message) {
        int icon = R.drawable.ic_menu_compose;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(context, PushActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

		Notification.Builder builder = new Notification.Builder(context);

		builder.setAutoCancel(false);
		builder.setContentTitle("Push MQTT");
		builder.setContentText("You have a new message");
		builder.setSmallIcon(icon);
		builder.setContentIntent(pendingIntent);
		builder.setOngoing(true);
		builder.setNumber(100);
		builder.build();

        Notification notification = builder.getNotification();
        notificationManager.notify(0, notification);
    }
    
}