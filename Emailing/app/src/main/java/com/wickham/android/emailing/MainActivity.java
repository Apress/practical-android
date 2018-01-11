package com.wickham.android.emailing;

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

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends Activity {
	
	private static String sendEmailScript = 	"http://www.your-server.com/sendemail.php";
	private static String emailAccount =		"your-email-accout";
	private static String emailPassword =		"your-email-account-password";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	
        setContentView(R.layout.activity_main);
        
        // Setup the ActionBar and the Spinner in the ActionBar
        getActionBar().setDisplayShowTitleEnabled(true);
        getActionBar().setTitle("Emailing");
        getActionBar().setSubtitle("Practical Android");
        
        // AWS button
        Button btnPHP = (Button) findViewById(R.id.butPhp);
        btnPHP.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
        		final ProgressDialog pd = ProgressDialog.show(MainActivity.this,"Sending","Sending AWS Email...",true, false);
        		new Thread(new Runnable() {
        			public void run() {	
        				try {     
        					EditText fromET = (EditText) findViewById(R.id.box1);
        					EditText toET = (EditText) findViewById(R.id.box2);
        					EditText subjET = (EditText) findViewById(R.id.box3);
        					EditText bodyET = (EditText) findViewById(R.id.box4);
                	
        					String emailFrom = fromET.getText().toString();
        					String emailTo = toET.getText().toString();
        					String emailSubj = subjET.getText().toString();
        					String emailBody = bodyET.getText().toString();

                            OkHttpClient httpClient = new OkHttpClient();
                            RequestBody formBody = new FormBody.Builder()
                                    .add("name", emailFrom)
                                    .add("to", emailTo)
                                    .add("from", emailFrom)
                                    .add("subject", emailSubj)
                                    .add("message", emailBody)
                                    .build();
                            Request request = new Request.Builder()
                                    .url(sendEmailScript)
                                    .post(formBody)
                                    .build();
                            Response response = httpClient.newCall(request).execute();
        				}
        				catch (Exception e) {
        					e.printStackTrace();
        				}
        				pd.dismiss();
        			}
        		}).start();
        	}
        });
    
        // Intent button
        Button btnInt = (Button) findViewById(R.id.butIntent);
        btnInt.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
            	EditText fromET = (EditText) findViewById(R.id.box1);
            	EditText toET = (EditText) findViewById(R.id.box2);
            	EditText subjET = (EditText) findViewById(R.id.box3);
            	EditText bodyET = (EditText) findViewById(R.id.box4);
            	
            	String emailFrom = fromET.getText().toString();
            	String emailTo = toET.getText().toString();
            	String emailSubj = subjET.getText().toString();
            	String emailBody = bodyET.getText().toString();
            	
       		    StringBuilder builder = new StringBuilder("mailto:" + Uri.encode(emailTo));
       		    if (emailSubj != null) {
       		        builder.append("?subject=" + Uri.encode(Uri.encode(emailSubj)));
       		        if (emailBody != null) {
       		            builder.append("&body=" + Uri.encode(Uri.encode(emailBody)));
       		        }
       		    }
       		    String uri = builder.toString();
       		    Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(uri));
       		    startActivity(intent);
        	}
        });
        
        // Java button
        Button btnJava = (Button) findViewById(R.id.butJava);
        btnJava.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
        		final ProgressDialog pd = ProgressDialog.show(MainActivity.this,"Sending","Sending Gmail Java API Email...",true, false);
        		new Thread(new Runnable(){
        			public void run(){	
                        try {
                        	EditText fromET = (EditText) findViewById(R.id.box1);
                        	EditText toET = (EditText) findViewById(R.id.box2);
                        	EditText subjET = (EditText) findViewById(R.id.box3);
                        	EditText bodyET = (EditText) findViewById(R.id.box4);
                        	
                        	String emailFrom = fromET.getText().toString();
                        	String emailTo = toET.getText().toString();
                        	String emailSubj = subjET.getText().toString();
                        	String emailBody = bodyET.getText().toString();
                        	
                            GMailSender sender = new GMailSender(emailAccount, emailPassword);
                            sender.sendMail(emailSubj,   
                                            emailBody,   
                                            emailFrom,   
                                    		emailTo);   
                        } catch (Exception e) {   
                            Log.e("SendMail", e.getMessage(), e);   
                        } 
        				pd.dismiss();
        			}
        		}).start(); 
        	}
        });
        
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