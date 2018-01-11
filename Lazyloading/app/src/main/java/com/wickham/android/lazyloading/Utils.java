package com.wickham.android.lazyloading;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.util.Log;

public class Utils {
    public static void CopyStream(InputStream is, OutputStream os) {
        final int buffer_size=1024;
        try {
            byte[] bytes=new byte[buffer_size];
            for(;;) {
              int count=is.read(bytes, 0, buffer_size);
              if(count==-1) break;
              os.write(bytes, 0, count);
            }
        }
        catch(Exception ex){}
    }

    public static InputStream OpenHttpConnection(String urlString) throws IOException {
    	InputStream in = null;
    	int response = -1; 
    	URL url = new URL(urlString); 
    	URLConnection conn = url.openConnection();
    	if (!(conn instanceof HttpURLConnection)) throw new IOException("Not an HTTP connection");
    	try {
    		HttpURLConnection httpConn = (HttpURLConnection) conn;
    		httpConn.setAllowUserInteraction(false);
    		httpConn.setInstanceFollowRedirects(true);
    		httpConn.setRequestMethod("GET");
    		httpConn.connect();
    		response = httpConn.getResponseCode();                 
    		if (response == HttpURLConnection.HTTP_OK) {
    			in = httpConn.getInputStream();                                 
    		}
    	}
    	catch (Exception ex) {
    		throw new IOException("Error connecting");            
    	}
    	return in;
    }
    
    public static String ReadLocalFile(File fname) throws IOException {
    	int BUFFER_SIZE = 10000;
    	FileInputStream fstream = null;
    	try {
    		fstream = new FileInputStream(fname);
    	} catch (Exception e1) {
    		return "";
    	}    		
    	BufferedReader bufreader;
    	try {
    		bufreader = new BufferedReader(new InputStreamReader(fstream, "UTF-8"));
    	} catch (Exception e) {
    		return "";
    	}
    	int charRead;
    	String str = "";
    	char[] inputBuffer = new char[BUFFER_SIZE];          
    	try {
    		while ((charRead = bufreader.read(inputBuffer))>0) {                    
    			String readString = String.copyValueOf(inputBuffer, 0, charRead);                    
    			str += readString;
    			inputBuffer = new char[BUFFER_SIZE];
    		}
    		fstream.close();
    	} catch (Exception e) {
    		return "";
    	}  
    	return str;        
	} 
    
    public static String DownloadText(String URL) {
    	int BUFFER_SIZE = 10000;
    	InputStream in = null;
    	String str = "";
    	try {
    		in = OpenHttpConnection(URL);
    		InputStreamReader isr;
    		isr = new InputStreamReader(in, "UTF-8");
    		int charRead;
    		char[] inputBuffer = new char[BUFFER_SIZE];          
    		while ((charRead = isr.read(inputBuffer)) > 0) {                    
    			String readString = String.copyValueOf(inputBuffer, 0, charRead);                    
    			str += readString;
    			inputBuffer = new char[BUFFER_SIZE];
    		}
    		in.close();
    	} catch (Exception e) {
    		return "";
    	}  
    	return str;        
    }    

    public static int getListPicHeight (Activity activity) { 
    	DisplayMetrics dMetrics = new DisplayMetrics();
    	activity.getWindowManager().getDefaultDisplay().getMetrics(dMetrics);
    	// give back a % of the screen width for the height
    	final float WIDE = activity.getResources().getDisplayMetrics().widthPixels;
    	int value = (int)(WIDE / 4.0f);
    	return value;
    }    
    
    public static int getGridPicHeight (Activity activity) { 
    	DisplayMetrics dMetrics = new DisplayMetrics();
    	activity.getWindowManager().getDefaultDisplay().getMetrics(dMetrics);
    	// give back a % of the screen width for the height
    	final float WIDE = activity.getResources().getDisplayMetrics().widthPixels;
    	int value = (int)(WIDE / 2.0f);
    	return value;
    }
    
    public static int getGallPicHeight (Activity activity) { 
    	DisplayMetrics dMetrics = new DisplayMetrics();
    	activity.getWindowManager().getDefaultDisplay().getMetrics(dMetrics);
    	// give back a height which is a % of the screen width
    	final float WIDE = activity.getResources().getDisplayMetrics().widthPixels;
    	int valueWide = (int)(WIDE * 1.00);
    	return valueWide;
    }
    
    public static boolean deleteDirectory(File path) {
    	if (path.exists()) {
    		File[] files = path.listFiles();
    		if (files == null) {
    			return true;
    		}
    		if (files != null) {
    			for (int i=0; i<files.length; i++) {
    				if (files[i].isDirectory()) {
    					deleteDirectory(files[i]);
    				}
    				else {
    					files[i].delete();
    				}
    			}
    		}
    	}
    	return(path.delete());
    }

}