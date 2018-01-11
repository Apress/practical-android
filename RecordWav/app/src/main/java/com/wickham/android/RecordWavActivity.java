package com.wickham.android;

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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.wickham.android.recordwav.R;
 
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class RecordWavActivity extends Activity {
	
	private static final int RECORDER_BPP = 16;
	private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
	private static final String AUDIO_RECORDER_FOLDER = "media/audio/music";
	private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
	private static int RECORDER_SAMPLERATE;	// Samplerate is derived from the platform
	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	 
	private AudioRecord recorder = null;
	private int bufferSize = 0;
	private Thread recordingThread = null;
	private boolean isRecording = false;
	
    private TextView mTimerTextView;
    private AlertDialog mAlertDialog;
    private double mRecordingTime;
    private long mRecordingLastUpdateTime;
    private ProgressDialog mProgressDialog;
    private Thread mSaveSoundFileThread;

	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

		setContentView(R.layout.activity_main);
		
        // Setup the ActionBar and the Spinner in the ActionBar
        getActionBar().setDisplayShowTitleEnabled(true);
        getActionBar().setSubtitle("Practical Android");
        getActionBar().setTitle("Record Wav");
		 
		setButtonHandlers();
		 
		bufferSize = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
		
		// Get the sample rate from the platform
		
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
    	String sr = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
    	RECORDER_SAMPLERATE = Integer.parseInt(sr);
		
		// Show the sample rate and buffer size being used
		((TextView)findViewById(R.id.audioSampleRate)).setText(" " + RECORDER_SAMPLERATE + " ");
		((TextView)findViewById(R.id.audioBufferSize)).setText(" " + bufferSize + " ");
		
		// Setup the recording folder
		String filepath = Environment.getExternalStorageDirectory().getPath();
		File file = new File(filepath,AUDIO_RECORDER_FOLDER);
		if(!file.exists()){
			file.mkdirs();
		}
    }
		 
	private void setButtonHandlers() {
		((Button)findViewById(R.id.btnStart)).setOnClickListener(btnClick);
		((Button)findViewById(R.id.btnStart)).setEnabled(true);
	}
		 
	private String getTempFilename() {
		String filepath = Environment.getExternalStorageDirectory().getPath();
		File file = new File(filepath,AUDIO_RECORDER_FOLDER);
		if(!file.exists()) file.mkdirs();
		File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);
		if(tempFile.exists()) tempFile.delete();
		return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
	}
		 
	private void startRecording() {
		// Provide a dialog box with timer
        AlertDialog.Builder adBuilder = new AlertDialog.Builder(RecordWavActivity.this);
        adBuilder.setTitle(getResources().getText(R.string.progress_dialog_recording));
        // Only way out is to press STOP button on the dialog box
        adBuilder.setCancelable(false);
        adBuilder.setPositiveButton(
            getResources().getText(R.string.progress_dialog_stop),
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
					mAlertDialog.dismiss();
					stopRecording();          
                }
            });
        adBuilder.setView(getLayoutInflater().inflate(R.layout.record_audio, null));
        mAlertDialog = adBuilder.show();
        mTimerTextView = (TextView)mAlertDialog.findViewById(R.id.record_audio_timer);
		
		// Ready to record
        mRecordingLastUpdateTime = getCurrentTime();
        
		recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
								   RECORDER_SAMPLERATE, 
								   RECORDER_CHANNELS,
								   RECORDER_AUDIO_ENCODING, 
								   bufferSize);
		 
		int i = recorder.getState();
		if (i==1) recorder.startRecording();
		isRecording = true;
		recordingThread = new Thread(new Runnable() {
			@Override
			public void run() {
				writeAudioDataToFile();
			}
		},"AudioRecorder Thread");
		recordingThread.start();
	}
		 
	private void writeAudioDataToFile() {
		byte data[] = new byte[bufferSize];
		String filename = getTempFilename();
		FileOutputStream os = null;
		Log.v("tmp",filename); 
		try {
			os = new FileOutputStream(filename);
			Log.v("good",filename);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			Log.v("oops",filename);
			e.printStackTrace();
		}
		 
		int read = 0;
		float elapsedSamples = 0;
		if(null != os){
			while(isRecording){
				read = recorder.read(data, 0, bufferSize);
				elapsedSamples = elapsedSamples + bufferSize / 4;	// The byte rate needs to be divided by 4
				if(AudioRecord.ERROR_INVALID_OPERATION != read){
					try {
						os.write(data);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				long now = getCurrentTime();
				if (now - mRecordingLastUpdateTime > 100) {		// Update the recording clock every 1/10 of a second
					mRecordingTime = elapsedSamples / RECORDER_SAMPLERATE;
					runOnUiThread(new Runnable() {
						public void run() {
							int min = (int)(mRecordingTime/60);
							float sec = (float)(mRecordingTime - 60 * min);
							mTimerTextView.setText(String.format("%d:%05.1f", min, sec));
						}
					});
					mRecordingLastUpdateTime = now;
				}
			}
			try {
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
		 
	private void stopRecording() {
		if(null != recorder){
			Log.v("ffstop1",getTempFilename());
			isRecording = false;
			int i = recorder.getState();
			if (i==1) recorder.stop();
			recorder.release();
			recorder = null;
			recordingThread = null;
		} 
		copyWaveFile(getTempFilename());
	}
		 
	private void deleteTempFile() {
		File file = new File(getTempFilename());
		file.delete();
	}
		 
	private void copyWaveFile(final String inFilename) {
        final Handler handler = new Handler() {
            public void handleMessage(Message response) {
                final String filename = response.obj.toString();
                // Do the saving here. Create an indeterminate progress dialog since it could be a long running operation
                mProgressDialog = new ProgressDialog(RecordWavActivity.this);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mProgressDialog.setTitle(R.string.progress_dialog_saving);
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();

                // Save the sound file in a background thread
                mSaveSoundFileThread = new Thread() {
                    public void run() {
                		try {
                			FileInputStream in = null;
                			FileOutputStream out = null;
                			long totalAudioLen = 0;
                			long totalDataLen = totalAudioLen + 36;
                			long longSampleRate = RECORDER_SAMPLERATE;
                			int channels = 2;
                			long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;
                			byte[] data = new byte[bufferSize];
                			
                			in = new FileInputStream(inFilename);	// Read from the tmpfile
                			
                			// And save to the output file
                			String filepath = Environment.getExternalStorageDirectory().getPath();
                			File file = new File(filepath,AUDIO_RECORDER_FOLDER);
                			// Strip any extension that was entered, we will append the .wav
                			String nameWithoutExtension = filename;
                			if (filename.indexOf(".") > 0) {
                			    nameWithoutExtension = filename.substring(0, filename.lastIndexOf("."));
                			}
                			String outFilename = file.getAbsolutePath() + "/" + nameWithoutExtension + AUDIO_RECORDER_FILE_EXT_WAV;
                			out = new FileOutputStream(outFilename);
                			
                			totalAudioLen = in.getChannel().size();
                			totalDataLen = totalAudioLen + 36;
                			// First the header
                			WriteWaveFileHeader(out, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);
                			// Followed by the audio samples
                			while(in.read(data) != -1){
                				out.write(data);
                			} 
                			in.close();
                			out.close();
                			deleteTempFile();	// Clean up the tmp file since we wrote out the wav file
                		} catch (FileNotFoundException e) {
                			e.printStackTrace();
                		} catch (IOException e) {
                			e.printStackTrace();
                		}
            			mProgressDialog.dismiss();
                    }
                };
                mSaveSoundFileThread.start();
            }
        };
        // Show the dialog, they can either Save or Cancel this recording
        Message message = Message.obtain(handler);
        FileSaveDialog dlog = new FileSaveDialog(this, getResources(), getString(R.string.file_save_title), message);
        dlog.show();		
	}
		 
	private void WriteWaveFileHeader(
		FileOutputStream out, long totalAudioLen,
		long totalDataLen, long longSampleRate, int channels,
		long byteRate) throws IOException {
		 
		byte[] header = new byte[44];
		 
		header[0] = 'R'; // RIFF/WAVE header
		header[1] = 'I';
		header[2] = 'F';
		header[3] = 'F';
		header[4] = (byte) (totalDataLen & 0xff);
		header[5] = (byte) ((totalDataLen >> 8) & 0xff);
		header[6] = (byte) ((totalDataLen >> 16) & 0xff);
		header[7] = (byte) ((totalDataLen >> 24) & 0xff);
		header[8] = 'W';
		header[9] = 'A';
		header[10] = 'V';
		header[11] = 'E';
		header[12] = 'f'; // 'fmt ' chunk
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';
		header[16] = 16; // 4 bytes: size of 'fmt ' chunk
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1; // format = 1
		header[21] = 0;
		header[22] = (byte) channels;
		header[23] = 0;
		header[24] = (byte) (longSampleRate & 0xff);
		header[25] = (byte) ((longSampleRate >> 8) & 0xff);
		header[26] = (byte) ((longSampleRate >> 16) & 0xff);
		header[27] = (byte) ((longSampleRate >> 24) & 0xff);
		header[28] = (byte) (byteRate & 0xff);
		header[29] = (byte) ((byteRate >> 8) & 0xff);
		header[30] = (byte) ((byteRate >> 16) & 0xff);
		header[31] = (byte) ((byteRate >> 24) & 0xff);
		header[32] = (byte) (2 * 16 / 8); // block align
		header[33] = 0;
		header[34] = RECORDER_BPP; // bits per sample
		header[35] = 0;
		header[36] = 'd';
		header[37] = 'a';
		header[38] = 't';
		header[39] = 'a';
		header[40] = (byte) (totalAudioLen & 0xff);
		header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
		header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
		header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
		out.write(header, 0, 44);
	}
		 
	private View.OnClickListener btnClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			switch(v.getId()) {
				case R.id.btnStart:{
					startRecording();
					break;
				}
			}
		}
	};
	
    private long getCurrentTime() {
        return System.nanoTime() / 1000000;
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
