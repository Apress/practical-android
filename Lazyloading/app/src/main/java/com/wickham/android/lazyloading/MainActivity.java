package com.wickham.android.lazyloading;

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
 * This project includes a modified version of the LazyList project Copyright (c) 2009-2012 Fedor Vlasov
 * which was licensed under the MIT license
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends Activity implements OnNavigationListener {
	
    private ListView list;
	private Gallery gallery;
    private GridView grid;
    
	private File configFile;
	
	private int currentViewID;
    
    private LazyGridAdapter gridAdapter;
    private LazyGallAdapter gallAdapter;
    private LazyListAdapter listAdapter;
    
    private String serverFileBase = "http://www.your-server.com/";
    private String serverPicBase = "http://www.your-server.com/pics/";
    private String fileName = "lazyloadconfig.txt";
    
    private String lazyLoadConfig = "";
    private static JSONArray configFileJson = null;

    private boolean masterAvail = false;
    
	private static ArrayList<String> fetchURL = new ArrayList<String>();
	private static ArrayList<String> imageTitle = new ArrayList<String>();
	private static ArrayList<String> imageDesc = new ArrayList<String>();
    
	//IDs for the views
	private static final int ID_LIST = 0;
	private static final int ID_GRID = 1;
	private static final int ID_GALL = 2;
	
	private int listPicHeight, galPicHeight, gridPicHeight;
	
	@Override
	protected void onDestroy() {
	    switch (currentViewID) {
	    case 0:
	    	list.setOnItemClickListener(null);
	    	list.setAdapter(null);
	    	break;
	    case 1:
	    	grid.setOnItemClickListener(null);
	    	grid.setAdapter(null);
	    	break;
	    case 2:
			gallery.setOnItemClickListener(null);
			gallery.setAdapter(null);
	    	break;	
	    }
	    super.onDestroy();
	}
	
//	Need handler for callbacks to the UI thread
    final Handler mHandler = new Handler();
    
//	Create runnable for posting
    final Runnable mUpdateResults = new Runnable() {
        public void run() {
        	if (currentViewID == ID_LIST) {
        		setContentView(R.layout.activity_main_list);
        		list = (ListView) findViewById(R.id.list); 
        		listAdapter = new LazyListAdapter(MainActivity.this, fetchURL, MainActivity.this); 
        		list.setAdapter(listAdapter);
    		    list.setOnItemClickListener(new OnItemClickListener() {
    	            public void onItemClick(AdapterView parent, View v, final int position, long id) {
    	            	Toast.makeText(MainActivity.this, "List item selected: " + position, Toast.LENGTH_SHORT).show();
    	            }	    	
    		    });
        	} else 
        	if (currentViewID == ID_GRID) {
        		setContentView(R.layout.activity_main_grid);
        		grid = (GridView) findViewById(R.id.grid);
        		gridAdapter = new LazyGridAdapter(MainActivity.this, fetchURL, MainActivity.this); 
        		grid.setAdapter(gridAdapter);
    	        int picHeight = gridPicHeight;
    	        int picLength = (int) ((float)picHeight / 1.5);
    	        grid.setColumnWidth(picLength);
    		    grid.setOnItemClickListener(new OnItemClickListener() {
    	    		public void onItemClick(AdapterView parent, View v, final int position, long id) {
    	    			Toast.makeText(MainActivity.this, "Grid item selected: " + position, Toast.LENGTH_SHORT).show();
    	    		}
    		    });
        	} else
        	if (currentViewID == ID_GALL) {
        		setContentView(R.layout.activity_main_gallery);
        		gallery = (Gallery) findViewById(R.id.gallery);
        		gallAdapter = new LazyGallAdapter(MainActivity.this, fetchURL, MainActivity.this); 
        		gallery.setAdapter(gallAdapter);
    		    gallery.setOnItemClickListener(new OnItemClickListener() {
    	    		public void onItemClick(AdapterView parent, View v, final int position, long id) {
    	    			Toast.makeText(MainActivity.this, "Gallery item selected: " + position, Toast.LENGTH_SHORT).show();
    	    		}
    		    });
        	}
        }
    };
    
//	Create runnables for failed connections
    final Runnable mNoConnection = new Runnable() {
		public void run() {
			noConnection();
		}
	};

	final Runnable mExConnection = new Runnable() {
		public void run() {
			exConnection();
		}
	};

    final Runnable mJsonConnection = new Runnable() {
        public void run() {
            jsonConnection();
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        configFile = new File(getExternalFilesDir(null),fileName);
        
        // Setup the ActionBar and the Spinner in the ActionBar
        getActionBar().setDisplayShowTitleEnabled(true);
        getActionBar().setSubtitle("Practical Android");
        getActionBar().setTitle("Lazyloading");
        
        Context context = getActionBar().getThemedContext();
        ArrayAdapter<CharSequence> listviews = ArrayAdapter.createFromResource(context, R.array.Views, R.layout.simple_spinner_item);
        listviews.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        getActionBar().setListNavigationCallbacks(listviews, this);
        
        // Default to ListView
        currentViewID = 0;
        
        gridPicHeight = Utils.getGridPicHeight(MainActivity.this);
        galPicHeight = Utils.getGallPicHeight(MainActivity.this);
        listPicHeight = Utils.getListPicHeight(MainActivity.this);
        
        // Clear the ArrayLists
    	fetchURL.clear();
    	imageTitle.clear();
    	imageDesc.clear();
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
    	
    	if (item.getItemId() == R.id.delete) {
            // delete all the files in the app storage space
	        final ProgressDialog pd = ProgressDialog.show(MainActivity.this,"Deleting","Deleting files from device storage...",true, false);
	        new Thread(new Runnable(){
		        public void run(){
		    	    switch (currentViewID) {
                        case 0:
                            listAdapter.imageLoader.fileCache.clear();
                            break;
                        case 1:
                            gridAdapter.imageLoader.fileCache.clear();
                            break;
                        case 2:
                            gallAdapter.imageLoader.fileCache.clear();
                            break;
		    	    }
		        	// delete all the images in the directory
                    File path = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                    Utils.deleteDirectory(path);
		        	// delete all items from the ArrayLists
		        	fetchURL.clear();
		        	imageTitle.clear();
		        	imageDesc.clear();
                    // delete the configFile
                    configFile.delete();
		        	// Update on the UI thread
		        	mHandler.post(mUpdateResults); 
		        	pd.dismiss();
		        }
	        }).start();	
    		return(true);
    	}
    	
    	if (item.getItemId() == R.id.load) {
    		final ProgressDialog pd = ProgressDialog.show(MainActivity.this,"Loading","Loading file list and images...",true, false);
	        new Thread(new Runnable(){
		        public void run(){	
		        	//File masterFil = new File(configFile);
					if (configFile.exists()) {
						try {
							lazyLoadConfig = Utils.ReadLocalFile(configFile);
							masterAvail = true;
						} catch (IOException e) {
							mHandler.post(mExConnection);
						}
					} else if (checkInternetConnection()) {
				        lazyLoadConfig = Utils.DownloadText(serverFileBase + fileName);
						//save the configFile to the local storage so we can lazyload the images offline
						BufferedWriter writer;
						try {
							writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile, true), "UTF-8"));
							writer.write(lazyLoadConfig);
							writer.flush();
							writer.close();
							masterAvail = true;
						} catch (Exception e) {
							mHandler.post(mExConnection);
						}
		            } else {
		            	mHandler.post(mNoConnection);
		            }

					if (masterAvail) {
		        		// setup the fetchURL ArrayLists		        		
		            	try {
		        			configFileJson = new JSONArray(lazyLoadConfig);
		                	for (int i=0; i<configFileJson.length(); i++){
		                		String title = jsonGetter2(configFileJson.getJSONArray(i),"title").toString();
		                		imageTitle.add(title);
		                		String fname = jsonGetter2(configFileJson.getJSONArray(i),"filename").toString();
		                		fetchURL.add(fname);
		                		String desc = jsonGetter2(configFileJson.getJSONArray(i),"desc").toString();
		                		imageDesc.add(desc);
		                	}
		        		} catch (JSONException e) {
		        			e.printStackTrace();
		        		}
		        		mHandler.post(mUpdateResults);
					} else {
						mHandler.post(mJsonConnection);
					}
					pd.dismiss();
		        }
	        }).start(); 
    		return(true);
    	}
    	return(super.onOptionsItemSelected(item));
    }  
    
    private class LazyGridAdapter extends BaseAdapter {	
        private Activity activity;
        private ArrayList<String> data;
        private LayoutInflater inflater=null;
        public ImageLoader imageLoader; 
        LinearLayout.LayoutParams params;
    	
        public LazyGridAdapter(Context context, ArrayList<String> d, Activity a) {
        	super();
            imageLoader=new ImageLoader(context);
            this.data = d;
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            activity = a;
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
            public ImageView image;
            public TextView title;
        }
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            // Inflate the view
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.grid_item, null);
                holder = new ViewHolder(); 
                holder.image = (ImageView) convertView.findViewById(R.id.image);
                holder.title = (TextView) convertView.findViewById(R.id.imgTit);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            	
            String imageurl = data.get(position);
            holder.image.setTag(imageurl);
            
            int picHeight = gridPicHeight;
            int picLength = (int) ((float)picHeight / 1.5);

            params = new LinearLayout.LayoutParams(picLength,picHeight);
            params.gravity=Gravity.CENTER;
            params.height=picHeight;
            params.width=picLength;

            holder.image.setLayoutParams(params);
            holder.title.setText(imageTitle.get(position));

            imageLoader.DisplayImage(serverPicBase + imageurl, holder.image);  

            return convertView;
        }
    }
    
    private class LazyGallAdapter extends BaseAdapter {	    
        private Activity activity;
        private ArrayList<String> data;
        private LayoutInflater inflater=null;
        public ImageLoader imageLoader; 
        LinearLayout.LayoutParams params;
    	
        public LazyGallAdapter(Context context, ArrayList<String> d, Activity a) {
        	super();
            imageLoader=new ImageLoader(context);
            this.data = d;
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            activity = a;
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
            public ImageView image;
            public TextView title;
        }
        
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            // Inflate the view
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.gal_item, null);
                holder = new ViewHolder(); 
                holder.image = (ImageView) convertView.findViewById(R.id.image);
                holder.title = (TextView) convertView.findViewById(R.id.imgCaption);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            	
            String imageurl = data.get(position);
            holder.image.setTag(imageurl);
            
            int picHeight = galPicHeight;
            int picLength = (int) ((float)picHeight / 1.5);
            
            params = new LinearLayout.LayoutParams(picLength,picHeight);

            holder.image.setLayoutParams(params);
        	holder.title.setText(imageTitle.get(position));

            imageLoader.DisplayImage(serverPicBase + imageurl, holder.image);  

            return convertView;
        }
    }
    
    private class LazyListAdapter extends BaseAdapter {	    
        private Activity activity;
        private ArrayList<String> data;
        private LayoutInflater inflater=null;
        public ImageLoader imageLoader; 
        LinearLayout.LayoutParams params;
    	
        public LazyListAdapter(Context context, ArrayList<String> d, Activity a) {
        	super();
            imageLoader=new ImageLoader(context);
            this.data = d;
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            activity = a;
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
            public ImageView image;
            public TextView title;
            public TextView desc;
        }
        
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            // Inflate the view
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.list_item, null);
                holder = new ViewHolder(); 
                holder.image = (ImageView) convertView.findViewById(R.id.list_image);
                holder.title = (TextView) convertView.findViewById(R.id.list_title);
                holder.desc = (TextView) convertView.findViewById(R.id.list_desc);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            	
            String imageurl = data.get(position);
            holder.image.setTag(imageurl);
            
            int picHeight = listPicHeight;
            int picLength = (int) ((float) picHeight / 1.5);

            params = new LinearLayout.LayoutParams(picLength,picHeight);
            params.gravity=Gravity.CENTER;
            params.height=picHeight;
            params.width=picLength;
            
            holder.image.setLayoutParams(params);
        	holder.title.setText(imageTitle.get(position));
        	holder.desc.setText(imageDesc.get(position));

            imageLoader.DisplayImage(serverPicBase + imageurl, holder.image);  

            return convertView;
        }
    }
    
    public void noConnection() {
    	AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
    	alertDialog.setTitle("Connection");
    	alertDialog.setIcon(android.R.drawable.stat_sys_warning);
    	alertDialog.setMessage("Server not reachable.");
    	alertDialog.setCancelable(false); 
    	alertDialog.setButton("Back", new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int which) {
    		} });
    	alertDialog.show();
    }

	public void exConnection() {
		AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
		alertDialog.setTitle("Exception");
		alertDialog.setIcon(android.R.drawable.stat_sys_warning);
		alertDialog.setMessage("Exception.");
		alertDialog.setCancelable(false);
		alertDialog.setButton("Back", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			} });
		alertDialog.show();
	}

    public void jsonConnection() {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("JSON Error");
        alertDialog.setIcon(android.R.drawable.stat_sys_warning);
        alertDialog.setMessage("Problem parsing JSON file.");
        alertDialog.setCancelable(false);
        alertDialog.setButton("Back", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            } });
        alertDialog.show();
    }

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		if (itemPosition == ID_LIST) {
	        currentViewID = 0;
		    mHandler.post(mUpdateResults);
		} else if (itemPosition == ID_GRID) {
	        currentViewID = 1;
		    mHandler.post(mUpdateResults);
		} else if (itemPosition == ID_GALL) {
	        currentViewID = 2;
		    mHandler.post(mUpdateResults);
		}
		return false;
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
}