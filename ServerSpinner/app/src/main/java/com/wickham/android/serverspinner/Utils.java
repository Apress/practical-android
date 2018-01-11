package com.wickham.android.serverspinner;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

import android.app.Activity;
import android.util.Log;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Utils {
    public static void CopyStream(InputStream is, OutputStream os)
    {
        final int buffer_size=1024;
        try
        {
            byte[] bytes=new byte[buffer_size];
            for(;;)
            {
              int count=is.read(bytes, 0, buffer_size);
              if(count==-1)
                  break;
              os.write(bytes, 0, count);
            }
        }
        catch(Exception ex){}
    }
    
    public static CharSequence readFile(Activity activity, int id) {
        BufferedReader in = null;
        try {
          in =
              new BufferedReader(new InputStreamReader(activity.getResources()
                  .openRawResource(id)));
          String line;
          StringBuilder buffer = new StringBuilder();
          while ((line = in.readLine()) != null) {
            buffer.append(line).append('\n');
          }
          // Chomp the last newline
          buffer.deleteCharAt(buffer.length() - 1);
          return buffer;
        } catch (Exception e) {
          return "";
        } finally {
          closeStream(in);
        }
    }

    private static void closeStream(Closeable stream) {
      if (stream != null) {
        try {
          stream.close();
        } catch (IOException e) {
          // Ignore
        }
      }
    }  

    public static InputStream OpenHttpConnection(String urlString) 
      throws IOException
      {
          InputStream in = null;
          int response = -1; 
          URL url = new URL(urlString); 
          URLConnection conn = url.openConnection();
          if (!(conn instanceof HttpURLConnection))                     
              throw new IOException("Not an HTTP connection");
          try{
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
          catch (Exception ex)
          {
              throw new IOException("Error connecting");            
          }
          return in;     
    }

    public static String DownloadText(String URL) 
    	{
    	int BUFFER_SIZE = 2000;
    	InputStream in = null;
    	try {
          in = OpenHttpConnection(URL);
    	} catch (Exception e1) {
          return "";
    	}    		
    	InputStreamReader isr;
    	try {
    		isr = new InputStreamReader(in, "UTF-8");
    	} catch (Exception e) {
    		return "";
    	}
    	int charRead;
        String str = "";
        char[] inputBuffer = new char[BUFFER_SIZE];          
        try {
          while ((charRead = isr.read(inputBuffer))>0)
          {                    
              String readString = 
                  String.copyValueOf(inputBuffer, 0, charRead);                    
              str += readString;
              inputBuffer = new char[BUFFER_SIZE];
          }
          in.close();
        } catch (Exception e) {
          return "";
        }  
        return str;        
    }
    
    public static void Uploader(String postURL, File file, String fname) {
        try {
            OkHttpClient httpClient = new OkHttpClient();
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("MAX_FILE_SIZE", "100000")
                    .addFormDataPart("filename",fname)
                    .addFormDataPart("uploadedfile", fname, RequestBody.create(MediaType.parse("image/jpg"), file))
                    .addFormDataPart("result", "my_image")
                    .build();

            Request request = new Request.Builder()
                    .header("Content-Type", "multipart/form-data; boundary=--32530126183148")
                    .url(postURL)
                    .post(requestBody)
                    .build();
            Response response = httpClient.newCall(request).execute();
            if ((response.code() == 200) || (response.code() == 204)) {
                Log.v("UPLOAD", "Success: URL=" + postURL + " fname=" + fname + " file.length=" + file.length() + " response=" + response.code());
            } else {
                // Handle upload fail
                Log.v("UPLOAD", "Fail: URL=" + postURL + " fname=" + fname + " file.length=" + file.length() + " response=" + response.code());
            }
        } catch (Throwable e) {
            Log.v("EX", "ex=" + e);
        }
    }
    
    public static void Deleter(String delURL, String fname) {
        try {
            OkHttpClient httpClient = new OkHttpClient();
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("inputfile",fname)
                    .build();
            Request request = new Request.Builder()
                    .header("Content-Type", "multipart/form-data; boundary=--32530126183148")
                    .url(delURL)
                    .post(requestBody)
                    .build();
            Response response = httpClient.newCall(request).execute();
            if ((response.code() == 200) || (response.code() == 204)) {
                Log.v("DELETE", "Success: URL=" + delURL + " fname=" + fname + " response=" + response.code());
            } else {
                // Handle upload fail
                Log.v("DELETE", "Fail: URL=" + delURL + " fname=" + fname + " response=" + response.code());
            }
        } catch (Throwable e) {
        	Log.v("EX", "ex=" + e);
        }
    }
}