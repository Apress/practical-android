package com.wickham.android.pdplayer;

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
 * This app is based off the open source Puredata app copyright by Peter Brinkmann
 */

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdPreferences;
import org.puredata.android.service.PdService;

import org.puredata.core.PdBase;
import org.puredata.core.PdReceiver;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class PdPlayer extends Activity implements OnClickListener, OnEditorActionListener, SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String TAG = "Pd Player";

	private Button stop;
	private Button load;
	private EditText msg;
	private Button prefs;
	private TextView logs;
	
	private File filesDir;
	ArrayList<String> fileList = new ArrayList<String>();
	private ListView listV;
	private myListAdapter listAdapter;

	private PdService pdService = null;

	private Toast toast = null;
	
	private void toast(final String msg) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (toast == null) {
					toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
				}
				toast.setText(TAG + ": " + msg);
				toast.show();
			}
		});
	}

	private void post(final String s) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				logs.append(s);
			}
		});
	}

	private PdReceiver receiver = new PdReceiver() {

		private void pdPost(String msg) {
			toast("Pure Data says, \"" + msg + "\"");
		}

		@Override
		public void print(String s) {
			post(s);
		}

		@Override
		public void receiveBang(String source) {
			pdPost("bang");
		}

		@Override
		public void receiveFloat(String source, float x) {
			pdPost("float: " + x);
		}

		@Override
		public void receiveList(String source, Object... args) {
			pdPost("list: " + Arrays.toString(args));
		}

		@Override
		public void receiveMessage(String source, String symbol, Object... args) {
			pdPost("message: " + Arrays.toString(args));
		}

		@Override
		public void receiveSymbol(String source, String symbol) {
			pdPost("symbol: " + symbol);
		}
	};

	private final ServiceConnection pdConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			pdService = ((PdService.PdBinder)service).getService();
			initPd();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
		}
	};

	@Override
	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// PD patches stored here on the device
        filesDir=new File(android.os.Environment.getExternalStorageDirectory(),"PDPatches");
        if(!filesDir.exists()) filesDir.mkdirs();
		
		AudioParameters.init(this);
		PdPreferences.initPreferences(getApplicationContext());
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
		initGui();
		bindService(new Intent(this, PdService.class), pdConnection, BIND_AUTO_CREATE);
		
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		cleanup();
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (pdService.isRunning()) {
			startAudio();
		}
	}

	private void initGui() {
		setContentView(R.layout.main);
		
        // Setup the ActionBar and the Spinner in the ActionBar
        getActionBar().setDisplayShowTitleEnabled(true);
        getActionBar().setSubtitle("Android Software Development");
        getActionBar().setTitle("Pure Data Player");
		
		stop = (Button) findViewById(R.id.play_button);
		stop.setOnClickListener(this);

		load = (Button) findViewById(R.id.load_button);
		load.setOnClickListener(this);

		prefs = (Button) findViewById(R.id.pref_button);
		prefs.setOnClickListener(this);
		
		logs = (TextView) findViewById(R.id.log_box);
		logs.setMovementMethod(new ScrollingMovementMethod());
	}

	private void initPd() {
		File patchFile = null;
		try {
			PdBase.setReceiver(receiver);
			PdBase.subscribe("android");			
		} finally {
			if (patchFile != null) patchFile.delete();
		}
	}

	private void startAudio() {
		String name = getResources().getString(R.string.app_name);
		try {
			pdService.initAudio(-1, -1, -1, -1);   // negative values will be replaced with defaults/preferences
			pdService.startAudio(new Intent(this, PdPlayer.class), R.drawable.icon, name, "Return to " + name + ".");
		} catch (IOException e) {
			toast(e.toString());
		}
	}

	private void stopAudio() {
		pdService.stopAudio();
		pdService.release();
	}
	
	private void cleanup() {
		try {
			unbindService(pdConnection);
		} catch (IllegalArgumentException e) {
			// already unbound
			pdService = null;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.pd_player_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.about_item:
			AlertDialog.Builder ad = new AlertDialog.Builder(this);
			ad.setTitle(R.string.about_title);
			ad.setMessage(R.string.about_msg);
			ad.setNeutralButton(android.R.string.ok, null);
			ad.setCancelable(true);
			ad.show();
			break;
		default:
			break;
		}
		return true;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {

		case R.id.play_button:
			if (pdService.isRunning()) {
				stopAudio();
			}
			break;
			
		case R.id.load_button:
			stopAudio();
            initPd();
			
			// Give them a popup so they can choose the patch to load
			LayoutInflater factory = LayoutInflater.from(this);            
			final View textEntryView = factory.inflate(R.layout.load_dialog, null);
            
			final CustomDialog customDialog = new CustomDialog(this);
			customDialog.setContentView(textEntryView);
			customDialog.show();
			customDialog.setCancelable(true);
			customDialog.setCanceledOnTouchOutside(true);
			
			// Check for files in the directory- Get an Array of pd files
	        FileFilter filter = new FileFilter() {
				@Override
				public boolean accept(File arg0) {
					return arg0.getName().endsWith(".pd");
				}
			};
			File[] files=filesDir.listFiles(filter);
			
			fileList.clear();
			
            if (files == null) {
            	// dir does not exist or is not a directory
            	Toast.makeText(getApplicationContext(), "No PD files exist", Toast.LENGTH_SHORT).show();
            	customDialog.dismiss();
            } else {
            	Log.v("files list:",files.toString());
            	// Build the array list
            	for (File f:files) fileList.add(f.getName());
            }

			listV = (ListView) customDialog.findViewById(R.id.loadItemList);
            listAdapter = new myListAdapter(PdPlayer.this, R.layout.list_item, fileList);
            listV.setAdapter(listAdapter);  
        
			listV.setOnItemClickListener(new OnItemClickListener() 
			{
				public void onItemClick(AdapterView parent, View v, final int position, long id) 
				{
					try {
						String fname = fileList.get(position);
						File patchFile = new File(filesDir, fname);
						
						PdBase.openPatch(patchFile);
						startAudio();
					} catch (IOException e) {
						Log.v("Ex:","Cant open PD patch");
					}
					customDialog.dismiss();
				}
			}); 	
			break;
		case R.id.pref_button:
			startActivity(new Intent(this, PdPreferences.class));
			break;
		default:
			break;
		}
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		evaluateMessage(msg.getText().toString());
		return true;
	}

	private void evaluateMessage(String s) {
		String dest = "test", symbol = null;
		boolean isAny = s.length() > 0 && s.charAt(0) == ';';
		Scanner sc = new Scanner(isAny ? s.substring(1) : s);
		if (isAny) {
			if (sc.hasNext()) dest = sc.next();
			else {
				toast("Message not sent (empty recipient)");
				return;
			}
			if (sc.hasNext()) symbol = sc.next();
			else {
				toast("Message not sent (empty symbol)");
			}
		}
		List<Object> list = new ArrayList<Object>();
		while (sc.hasNext()) {
			if (sc.hasNextInt()) {
				list.add(Float.valueOf(sc.nextInt()));
			} else if (sc.hasNextFloat()) {
				list.add(sc.nextFloat());
			} else {
				list.add(sc.next());
			}
		}
		if (isAny) {
			PdBase.sendMessage(dest, symbol, list.toArray());
		} else {
			switch (list.size()) {
			case 0:
				PdBase.sendBang(dest);
				break;
			case 1:
				Object x = list.get(0);
				if (x instanceof String) {
					PdBase.sendSymbol(dest, (String) x);
				} else {
					PdBase.sendFloat(dest, (Float) x);
				}
				break;
			default:
				PdBase.sendList(dest, list.toArray());
				break;
			}
		}
	}
	
    private class myListAdapter extends BaseAdapter {
        private ArrayList<String> data;
        private LayoutInflater inflater=null;
    	
        public myListAdapter(Context context, int resource, ArrayList<String> d) {
        	super();
            this.data = d;
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            data = d;      
        }
    	public int getCount() {
            return data.size();
        }
    	public Object getItem(int position) {
            return position;
        }
        public long getItemId(int position) {
            return position;
        }
        class ViewHolder {
            public TextView fname;
        }
        
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            // Inflate the view
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.list_item, null);
                holder = new ViewHolder(); 
                holder.fname = (TextView) convertView.findViewById(R.id.list_title);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
			String fname = fileList.get(position);
        	holder.fname.setText(fname); 
            return convertView;
        }
    }	
}