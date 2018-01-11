package com.wickham.android.serverspinner;

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
 * This project includes a modified version of the LasyList project Copyright (c) 2009-2012 Fedor Vlasov
 * which was licensed under the MIT license
 *
 */

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ScaleXSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends Activity {
	
    private static String serverPath =			"http://www.your-server.com/return204.php";
    private static String serverPicBase =		"http://www.your-server.com/pics/";
	private static String listFilesScript =		"http://www.your-server.com/pics/listfiles-a.php";
	private static String uploadFilesScript = 	"http://www.your-server.com/pics/uploadfile.php";
	private static String deleteFileScript = 	"http://www.your-server.com/pics/deletefile.php";

    private String fileList;
    
    private File picDir = new File(android.os.Environment.getExternalStorageDirectory(),"ServerSpinner");
	private String path1 = Environment.getExternalStorageDirectory() + "/ServerSpinner/takepicOrig.jpg";
	private String path2 = Environment.getExternalStorageDirectory() + "/ServerSpinner/";
	
	protected boolean pictureTaken;	
	protected static final String PHOTO_TAKEN = "photo_taken";
	
	ArrayList<String> spinList = new ArrayList<String>();
	private ImageLoader imageLoader;
	private Button picButton;
	private int checkedPic;
	private String selectedPicName;
	private static String[] items = new String[] {};

//	Need handler for callbacks to the UI thread
    final Handler mHandler = new Handler();
    
//	Create runnables for posting
    final Runnable updateResults = new Runnable() {
        public void run() {
        	// update the file list
        	TextView tv = (TextView) findViewById(R.id.textFileList);
        	String replaced = fileList.replace(" ", "<br />");
        	tv.setText(Html.fromHtml(replaced));
        	// update the count on the button
    		picButton = (Button) findViewById(R.id.spinnerPic);
    		picButton.setText("Server Files (" + spinList.size() + ")");
    		// clear the selected image
    		clearPicture();
        }
    };
    final Runnable noConnection = new Runnable() {
        public void run() {
        	noConnection();
        }
    };
    final Runnable exceptionConnection = new Runnable() {
        public void run() {
        	exConnection();
        }
    };	
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    	getWindow().setFlags(
    			WindowManager.LayoutParams.FLAG_FULLSCREEN,
    			WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	
    	if (!picDir.exists()) picDir.mkdirs();
    	
        setContentView(R.layout.activity_main);
        
        // Setup the ActionBar and the Spinner in the ActionBar
        getActionBar().setDisplayShowTitleEnabled(true);
        getActionBar().setSubtitle("Practical Android");
        getActionBar().setTitle("Server Spinner");
        
        imageLoader=new ImageLoader(MainActivity.this);
        
        spinList.clear();
        
		picButton = (Button) findViewById(R.id.spinnerPic);
		picButton.setText("Server Files (" + spinList.size() + ")");
	    picButton.setOnClickListener(new OnClickListener() {
	    	  public void onClick(View v) {
	    		  showPicDialog();
	    	  }
	    });
        
        // sync server button
        Button btnSync = (Button) findViewById(R.id.sync);
        btnSync.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {	
        		clearPicture();
        		sync();
        	}
        });

        // take a photo button
        Button btnPhoto = (Button) findViewById(R.id.photo);
        btnPhoto.setOnClickListener( new CameraClickHandler() );
    
        // delete button
        Button btnDel = (Button) findViewById(R.id.delete);
        btnDel.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
        		clearPicture();
        		new fileDelete().execute();
        	}
        });
    }
    
    public void sync() {
        // download the file list for the server spinner
		final ProgressDialog pd = ProgressDialog.show(MainActivity.this,"Syncing","Syncing file list from server...",true, false);
		new Thread(new Runnable() {
			public void run() {	
			// see if we can ping the server first
            try {
                OkHttpClient httpClient = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(serverPath)
                        .build();
                Response response = httpClient.newCall(request).execute();
                if ((response.code() == 200) || (response.code() == 204)) {
                    Log.v("SYNC", "syncing");
                    fileList = Utils.DownloadText(listFilesScript);
                    fileList = fileList.substring(0,fileList.length()-1);
                    items = fileList.split(" ");
                    spinList.clear();
                    for(int i=0;i<items.length;i++) {
                        spinList.add(i,items[i]);
                    }
                    mHandler.post(updateResults);
                } else {
                    Log.v("SYNC", "No Conn");
                    mHandler.post(noConnection);
                }
            } catch (Exception e) {
                Log.v("SYNC", "Ex=" + e);
                mHandler.post(exceptionConnection);
            }
            pd.dismiss();
			}
		}).start(); 
    }
    
	protected void showPicDialog() {
		DialogInterface.OnClickListener picDialogListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
		    	String str = spinList.get(which);
		    	TextView txt = (TextView) findViewById(R.id.selectedTitle);
				txt.setText(str);
				selectedPicName = str;
				
				// Load a thumb of the selected picture
 				ImageView img = (ImageView) findViewById(R.id.spinnerImg);
				String fetchURL = serverPicBase + str;
			    imageLoader.DisplayImage(fetchURL, img);            
				img.setTag(fetchURL);
				
				txt = (TextView) findViewById(R.id.selectedURL);
				txt.setText(fetchURL);	
				dialog.dismiss();
			}
		};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.get_file));
		// need the pics in a string [] to pass in multi
		String[] tmpArr = new String[spinList.size()];
		for (int i = 0; i < spinList.size(); i++) {
		    tmpArr[i] = spinList.get(i);  
		}
		builder.setSingleChoiceItems(tmpArr, checkedPic, picDialogListener);
		AlertDialog dialog = builder.create();
		dialog.show();
	}
    
	// picture taking handler
    public class CameraClickHandler implements View.OnClickListener {
    	public void onClick( View view ){
        	File file = new File( path1 );
        	Uri outputFileUri = Uri.fromFile( file );
        	Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE );
        	intent.putExtra( MediaStore.EXTRA_OUTPUT, outputFileUri );
        	startActivityForResult( intent, 0 );
    	}
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {	
    	switch( resultCode ) {
			case 0:
				break;
			case -1:
				onPhotoTaken();	
				break;
    	}
    }
    
    protected void onPhotoTaken() {
    	pictureTaken = true;
    			
		// pop up a dialog so we can get a name for the new pic and upload it
		final Dialog dialogName = new Dialog(MainActivity.this);
        dialogName.setContentView(R.layout.newpic_name);
        dialogName.setCancelable(true);
        dialogName.setCanceledOnTouchOutside(true);
          
        // lets scale the title on the popup box
        String tit = getString(R.string.new_pic_title);                 
        SpannableStringBuilder ssBuilser = new SpannableStringBuilder(tit);
        StyleSpan span = new StyleSpan(Typeface.BOLD);
        ScaleXSpan span1 = new ScaleXSpan(2);
        ssBuilser.setSpan(span, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        ssBuilser.setSpan(span1, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        dialogName.setTitle(ssBuilser);
          
        TextView picText=(TextView) dialogName.findViewById(R.id.newPicText);
        picText.setText(getString(R.string.new_pic_text1)); 

        Button picCancel = (Button) dialogName.findViewById(R.id.newPicCancel);
        picCancel.setText(getString(R.string.cancel));
        picCancel.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	dialogName.dismiss();
            }
        });	
        
        Button picSave = (Button) dialogName.findViewById(R.id.newPicAdd);
        picSave.setText(getString(R.string.save));
        picSave.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	EditText nameET = (EditText) dialogName.findViewById(R.id.newPicEdit);
            	String name = nameET.getText().toString();
            	name = name.replaceAll("[^\\p{L}\\p{N}]", "");
            	if (name.equalsIgnoreCase("")) name = "newpic";
            	name = name.toLowerCase() + ".jpg";
            	selectedPicName = name;

            	//adjust for camera orientation
            	Bitmap bitmapOrig = BitmapFactory.decodeFile( path1);
                int width = bitmapOrig.getWidth();
                int height = bitmapOrig.getHeight();
                // the following are reverse because we are going to rotate the image 90 due to portrait pics always used
                int newWidth = 150;
                int newHeight = 225;
                // calculate the scale
                float scaleWidth = ((float) newWidth) / width;
                float scaleHeight = ((float) newHeight) / height;
                // create a matrix for the manipulation
                Matrix matrix = new Matrix();
                // resize the bit map
                matrix.postScale(scaleWidth, scaleHeight);
            	
                // save a scaled down Bitmap
                Bitmap resizedBitmap = Bitmap.createBitmap(bitmapOrig, 0, 0, width, height, matrix, true);
                
                File file2 = new File (path2 + selectedPicName);
                
                try {
                    FileOutputStream out = new FileOutputStream(file2);
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush();
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            	
                // update the picture
        		ImageView img = (ImageView) findViewById(R.id.spinnerImg);
        		img.setImageBitmap(resizedBitmap);
            	
            	// save new name
		    	TextView txt = (TextView) findViewById(R.id.selectedTitle);
				txt.setText(name);
				
				txt = (TextView) findViewById(R.id.selectedURL);
				txt.setText(serverPicBase + name);	
				
        		spinList.add(name);
        		
        		// upload the new picture to the server
        		new fileUpload().execute();
        		
            	dialogName.dismiss();
            }
        });	
        dialogName.show();
    }
    
    @Override 
    protected void onRestoreInstanceState( Bundle savedInstanceState) {
    	if( savedInstanceState.getBoolean( PHOTO_TAKEN ) ) {
    		onPhotoTaken();
    	}
    }
    
    @Override
    protected void onSaveInstanceState( Bundle outState ) {
    	outState.putBoolean( PHOTO_TAKEN, pictureTaken );
    }
    
    private void clearPicture() {
    	// clear the selected image
    	TextView tv = (TextView) findViewById(R.id.selectedTitle);
    	tv.setText("---");
    	ImageView img = (ImageView) findViewById(R.id.spinnerImg);          
    	img.setImageResource(R.drawable.nopic);
    	tv = (TextView) findViewById(R.id.selectedURL);
    	tv.setText("---");	
    }
    
	private class fileUpload extends AsyncTask<Void, String, Void> {
		@Override
		protected Void doInBackground(Void... unused) {
			// upload new picture to the server
			try {    			
				String postURL = uploadFilesScript;
    	    	File file = new File(path2, selectedPicName);
    	    	// upload the new picture
    	    	Utils.Uploader(postURL, file, selectedPicName);
    	    	// did the upload, update the spinner and count
    	   		fileList = Utils.DownloadText(listFilesScript);
    	   		fileList = fileList.substring(0,fileList.length()-1);
    			items = fileList.split(" ");
    			spinList.clear();
    			for(int i=0;i<items.length;i++) {
    				spinList.add(i,items[i]);
    			}
    			mHandler.post(updateResults);
				} catch (Exception e) {
				}
			return null; 
		}
		@Override
		protected void onPostExecute(Void unused) {}	
		@Override
		protected void onPreExecute() {}
		protected void onProgressUpdate(String... item) {}
	}
	
	private class fileDelete extends AsyncTask<Void, String, Void> {
		@Override
		protected Void doInBackground(Void... unused) {
			// delete picture from server
			try {
				String postURL = deleteFileScript;
    	    	// delete the selected picture
				if (spinList.contains(selectedPicName)) {
					Utils.Deleter(postURL, selectedPicName);
					// did the delete, so update the spinner and count
					fileList = Utils.DownloadText(listFilesScript);
					fileList = fileList.substring(0,fileList.length()-1);
					items = fileList.split(" ");
					spinList.clear();
					for(int i=0;i<items.length;i++) {
						spinList.add(i,items[i]);
					}
					mHandler.post(updateResults);
				}
				} catch (Exception e) {
					Log.v("EX", "ex=" + e);
				}
			return null; 
		}
		@Override
		protected void onPostExecute(Void unused) {}	
		@Override
		protected void onPreExecute() {}
		protected void onProgressUpdate(String... item) {}
	}
    
    public void noConnection() {
    	AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
    	alertDialog.setTitle("Connection");
    	alertDialog.setIcon(android.R.drawable.stat_sys_warning);
    	alertDialog.setMessage("Server not reachable. Status");
    	alertDialog.setCancelable(false); 
    	alertDialog.setButton("Back", new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int which) {
    		} });
    	alertDialog.show();
    }
    
    public void exConnection() {
    	AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
    	alertDialog.setTitle("Connection");
    	alertDialog.setIcon(android.R.drawable.stat_sys_warning);
    	alertDialog.setMessage("Exception occurred.");
    	alertDialog.setCancelable(false); 
    	alertDialog.setButton("Back", new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int which) {
    		} });
    	alertDialog.show();
    }
    
    public void exUpload() {
    	AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
    	alertDialog.setTitle("Connection");
    	alertDialog.setIcon(android.R.drawable.stat_sys_warning);
    	alertDialog.setMessage("Upload failed.");
    	alertDialog.setCancelable(false); 
    	alertDialog.setButton("Back", new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int which) {
    		} });
    	alertDialog.show();
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