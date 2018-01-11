package com.wickham.android.musicservice;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

public class MusicService extends Service implements MediaPlayer.OnErrorListener{
	
	private final IBinder mBinder = new ServiceBinder(); 
    MediaPlayer mPlayer;
    private int length = 0;
    private int seekForwardTime = 60000;
   
    public MusicService() { } 
    
    public class ServiceBinder extends Binder {
     	 public MusicService getServiceInstance() {
    		return MusicService.this;
    	 }
    }
  
    @Override
    public IBinder onBind(Intent arg0){return mBinder;}

    @Override
    public void onCreate () {
    	super.onCreate(); 
    	mPlayer = MediaPlayer.create(this, R.raw.nogabe);
    	mPlayer.setOnErrorListener(this);

    	if(mPlayer!= null) {
    		mPlayer.setLooping(false);
    		mPlayer.setVolume(100,100);
    	}
    	mPlayer.setOnErrorListener(new OnErrorListener() {
    		public boolean onError(MediaPlayer mp, int what, int extra) {
        		onError(mPlayer, what, extra);
        		return true;
        	}
        });
    }
	
    @Override
	public int onStartCommand (Intent intent, int flags, int startId) {
         mPlayer.start();
		return START_NOT_STICKY;
         
	}
	
	public void pauseMusic() {
		if(mPlayer.isPlaying()) {
			mPlayer.pause();
			length=mPlayer.getCurrentPosition(); 		
		}
	}
	
	public void forwardMusic() {
		if(mPlayer.isPlaying()) {
            int currentPosition = mPlayer.getCurrentPosition();
            // check if seekForward time is lesser than song duration
            if (currentPosition + seekForwardTime <= mPlayer.getDuration()) {
                // forward song
                mPlayer.seekTo(currentPosition + seekForwardTime);
            }
		}
	}
	
	public void resumeMusic() {
		if(mPlayer.isPlaying()==false)
		{
			mPlayer.seekTo(length);
			mPlayer.start();
		}
	}
	
	public void stopMusic()	{
		mPlayer.stop();
		mPlayer.release();
		mPlayer = null;
	}
	
	@Override
	public void onDestroy () {
		super.onDestroy();
		if(mPlayer != null) {
			try {
				mPlayer.stop();
				mPlayer.release();
			} finally {
				mPlayer = null;
			}
		}
	}

	public boolean onError(MediaPlayer mp, int what, int extra) {	
		Toast.makeText(this, "music player failed", Toast.LENGTH_SHORT).show();	
		if(mPlayer != null) {
			try {
				mPlayer.stop();
				mPlayer.release();
			} finally {
				mPlayer = null;
			}
		}
		return false;
	}
}
