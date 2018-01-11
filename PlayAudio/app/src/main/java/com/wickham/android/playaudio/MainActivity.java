package com.wickham.android.playaudio;

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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.media.SoundPool.OnLoadCompleteListener;
import android.media.audiofx.EnvironmentalReverb;
import android.media.audiofx.PresetReverb;
import android.net.Uri;

public class MainActivity extends Activity {
	private MediaPlayer mp;
	private Thread playingThread = null;
    private GridView gridview;
    private GridAdapter gridAdapter;
    private StateListDrawable states;
    
    ArrayList<String> buttonName = new ArrayList<String>();
    ArrayList<Integer> buttonType = new ArrayList<Integer>();
    ArrayList<Integer> buttonResID = new ArrayList<Integer>();
    ArrayList<Integer> buttonParam1 = new ArrayList<Integer>();
    
	private static JSONArray soundFileJson = null;
	private static String soundFileTxt = "";

	private static String[] soundTypeButtonColors = new String[] {
	"#cd5067","#247fca","#9553c5","#3e8872","#fe7f3d"};
	
	private static String[] soundTypeButtonTextColors = new String[] {
	"#ffffff","#ffffff","#ffffff","#ffffff","#111111"};
	
	/*
	 * The JSON file soundfile.txt defines the sounds assigned to each button on the GridView and how they will be played
	 * 
	 * Sound Name : 	String - Name which get displayed on the Grid buttons
	 * 
	 * Sound Type:		Int
	 * 					0 : Media Player
	 * 					1 : Sound Pool
	 * 					2 : Audio Track Tone
	 * 					3 : Audio Track
	 * 
	 * Resource ID:		Int - Corresponds to the audio resources in res/raw
	 */
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
        // Setup the ActionBar and the Spinner in the ActionBar
        getActionBar().setDisplayShowTitleEnabled(true);
        getActionBar().setSubtitle("Practical Android");
        getActionBar().setTitle("Play Audio");
		
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		
    	buttonName.clear();
    	buttonType.clear();
    	buttonResID.clear();
    	buttonParam1.clear();
    	
    	// Read in the JSON file and load up the 3 ArrayLists for the sounds
        try {
            Resources res = getResources();
            InputStream in_s = res.openRawResource(R.raw.soundfile);

            byte[] b = new byte[in_s.available()];
            in_s.read(b);
            soundFileTxt = (new String(b));
        } catch (Exception e) {
            e.printStackTrace();
        }
    	
    	try {
			soundFileJson = new JSONArray(soundFileTxt);
        	for(int i=0; i<soundFileJson.length(); i++){        		
        		String title = jsonGetter2(soundFileJson.getJSONArray(i),"name").toString();
        		buttonName.add(title);
        		int type = (Integer) jsonGetter2(soundFileJson.getJSONArray(i),"soundType");
        		buttonType.add(type);
        		String resString = jsonGetter2(soundFileJson.getJSONArray(i),"resID").toString();
        		int resId = getResources().getIdentifier(resString, "raw", getPackageName());
        		buttonResID.add(resId);
        		int param1 = (Integer) jsonGetter2(soundFileJson.getJSONArray(i),"param1");
        		buttonParam1.add(param1);
        	}
		} catch (JSONException e) {
			e.printStackTrace();
		}	

		// update the gridView
        gridview = (GridView) findViewById(R.id.gridView1);
        gridAdapter = new GridAdapter(MainActivity.this, R.layout.array_list_item, buttonName);
        gridview.setAdapter(gridAdapter);
        
        gridview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) 
            {
        		int resid = buttonResID.get(position);
        		int type = buttonType.get(position);
        		
        		switch (type) {
        		case 0:
        			try {      
                		// Release any resources from previous MediaPlayer
                		if (mp != null) mp.release();
        				mp = new MediaPlayer();
        				Uri u = Uri.parse("android.resource://com.wickham.android.playaudio/" + resid);
						mp.setDataSource(MainActivity.this, u);
	        			mp.prepare();
	        			mp.start();
					} catch (Exception e) {
						e.printStackTrace();
					}
        			break;
        		case 1:
        			playSoundPool(resid);
        			break;
        		case 2:
            		int resFreq = buttonParam1.get(position);
            		int resDur = 500;
        			AudioTrack soundAtSpecificFrequency =   generateTone(resFreq, resDur);
        			soundAtSpecificFrequency.play();
        			break;
        		case 3:
        			playSound(resid);
        			break;
        		}
            }
        }); 
		
	}
	
	private void playSoundPool(int soundID) {
	    int MAX_STREAMS = 2;
	    int REPEAT = 0;
	    SoundPool soundPool = new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, REPEAT);
	    soundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
	        @Override
	        public void onLoadComplete(SoundPool soundPool, int soundId, int status) {
	            int priority = 0;
	            int repeat = 0;
	            float rate = 1.f; // Frequency Rate can be from .5 to 2.0
	    	    // Set volume
	            AudioManager mgr = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
	            float streamVolumeCurrent = mgr.getStreamVolume(AudioManager.STREAM_MUSIC);
	            float streamVolumeMax = mgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
	            float volume = streamVolumeCurrent / streamVolumeMax;	            
	    		// Play it
	            soundPool.play(soundId, volume, volume, priority, repeat, rate);              
	        }
	    });
	    soundPool.load(this, soundID, 1);
	}
	
	private AudioTrack generateTone(double freqHz, int durationMs)
	{
	    int count = (int)(44100.0 * 2.0 * (durationMs / 1000.0)) & ~1;
	    short[] samples = new short[count];
	    for(int i = 0; i < count; i += 2){
	        short sample = (short)(Math.sin(2 * Math.PI * i / (44100.0 / freqHz)) * 0x7FFF);
	        samples[i + 0] = sample;
	        samples[i + 1] = sample;
	    }
	    AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
	        AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
	        count * (Short.SIZE / 8), AudioTrack.MODE_STATIC);

	    track.write(samples, 0, count);
	    
	    return track;
	}
	
	private void playSound(final int soundID) {
		playingThread = new Thread(new Runnable() {
			@Override
			public void run() {
			    int minBufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, 
		                AudioFormat.ENCODING_PCM_16BIT);
				AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_STEREO, 
		                AudioFormat.ENCODING_PCM_16BIT, minBufferSize, AudioTrack.MODE_STREAM); 
				audioTrack.play();
				
			    int i = 0;
			    int bufferSize = 512;
			    byte [] buffer = new byte[bufferSize];
			    InputStream inputStream = getResources().openRawResource(soundID);
			    try {
			        while((i = inputStream.read(buffer)) != -1)
			            audioTrack.write(buffer, 0, i);
			    } catch (IOException e) {
			        // TODO Auto-generated catch block
			        e.printStackTrace();
			    }
			    try {
			        inputStream.close();
			    } catch (IOException e) {
			        // TODO Auto-generated catch block
			        e.printStackTrace();
			    }	
			}
		},"AudioRecorder Thread");
		playingThread.start();		
	}
		
    private class GridAdapter extends ArrayAdapter {
    	Context ctxt;
    	private ArrayList<String> data;
	    int textSize;
	    
    	GridAdapter(Context ctxt, int resource, ArrayList<String> items) {
    		super(ctxt, resource, items);
    		this.ctxt=ctxt;
    		data=items;
    		textSize = (getFontSize(MainActivity.this));
    	}
    	
    	public View getView(int position, View convertView, ViewGroup parent) {
    		TextView label=(TextView)convertView;
    		if (convertView==null) {
    			convertView=new TextView(ctxt);
    			label=(TextView)convertView;
    		}
    		int index = buttonType.get(position);
    		String tempString=data.get(position);
    		SpannableString spanString = new SpannableString(tempString);
    		spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
    		label.setText(spanString);
    		
    		label.setTextSize(textSize);
    		label.setHeight((int) (getWindowButtonHeight(MainActivity.this)));
        	
     		states = new StateListDrawable();
     		states.addState(new int[] {android.R.attr.state_pressed}, new ColorDrawable(Color.parseColor(soundTypeButtonColors[4])));	// pressed color
     		states.addState(new int[] { }, new ColorDrawable(Color.parseColor(soundTypeButtonColors[index])));	// button color based on sound type
     		label.setBackgroundDrawable(states);
     		label.setTextColor(Color.parseColor(soundTypeButtonTextColors[index]));	// black or white text depending on the button color
     		label.setShadowLayer((float) 0.02,2,2,Color.parseColor(soundTypeButtonTextColors[4]));	// shadow

    		label.setPadding(4, 2, 4, 2);	// l,t,r,b
    		label.setGravity(Gravity.CENTER);
    		return(convertView);
    	}
    }
    
    public static int getWindowButtonHeight(Activity activity) { 
    	DisplayMetrics dMetrics = new DisplayMetrics();
    	activity.getWindowManager().getDefaultDisplay().getMetrics(dMetrics);
    	// return a fraction of the screen height
    	final float HIGH = activity.getResources().getDisplayMetrics().heightPixels;
    	int valueHigh = (int)(HIGH / 11.0f);
    	return valueHigh;
    }
    
    public static int getFontSize (Activity activity) { 
    	DisplayMetrics dMetrics = new DisplayMetrics();
    	activity.getWindowManager().getDefaultDisplay().getMetrics(dMetrics);
    	switch(dMetrics.densityDpi) {
    		case DisplayMetrics.DENSITY_HIGH:
    			return 12;
    		case DisplayMetrics.DENSITY_MEDIUM:
    			return 14;
    		case DisplayMetrics.DENSITY_LOW:
    			return 16;
    	}
    	return 14;
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
	
    private long getCurrentTime() {
        return System.nanoTime() / 1000000;
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	@Override
	protected void onDestroy() {
		if(null!=mp) {
			mp.release();
		}
		super.onDestroy();
	}
}
