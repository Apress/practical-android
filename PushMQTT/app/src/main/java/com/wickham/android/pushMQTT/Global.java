package com.wickham.android.pushMQTT;

import java.util.ArrayList;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.internal.MemoryPersistence;

public class Global {
					
	public static ArrayList<String> items = new ArrayList<String>();
	
	// MQTT params	
	public static final String 		MQTT_URL_FORMAT = "tcp://%s:%d"; // URL Format
	
	// Public or Private Broker URL or IP address
	public static final String		MQTT_BROKER = "52.52.152.59";

	public static final int 		MQTT_PORT = 1883;
	
	public static final int			MQTT_QOS_0 = 0; // QOS Level 0 (Delivery Once no confirmation)
	public static final int 		MQTT_QOS_1 = 1; // QOS Level 1 (Delivery at least Once with confirmation)
	public static final int			MQTT_QOS_2 = 2; // QOS Level 2 (Delivery only once with confirmation with handshake)
	
	public static final int 		MQTT_KEEP_ALIVE = 300000; // KeepAlive Interval in MS
	public static final String		MQTT_KEEP_ALIVE_TOPIC_FORMAT = "%s/keepalive"; // Topic format for KeepAlives
	public static final byte[] 		MQTT_KEEP_ALIVE_MESSAGE = { 0 }; // Keep Alive message to send
	public static final int			MQTT_KEEP_ALIVE_QOS = MQTT_QOS_0; // Default Keep alive QOS
	public static final boolean 	MQTT_CLEAN_SESSION = true; // Start a clean session?

	// If the server is setup, we can use password access
	public static final String		MQTT_USER = "your-mqtt-user";
	public static final char[]		MQTT_PASSWORD = new char[]{'y','o','u','r','P','W'};
	
}