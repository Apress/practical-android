/*
 * $Id$
 */

package com.wickham.android.pushMQTT;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Environment;

public class ConnectionLog
{
	private String mPath;
	private Writer mWriter;

	private static final SimpleDateFormat TIMESTAMP_FMT = 
	  new SimpleDateFormat("[HH:mm:ss] ");

	public ConnectionLog()
	  throws IOException
	{
		
		File logDir;
        // Set the directory to save text files
		logDir = new File(android.os.Environment.getExternalStorageDirectory(),"PushMQTT");
        if (!logDir.exists()) {
        	logDir.mkdirs();
        	// do not allow media scan
            new File(logDir, ".nomedia").createNewFile();
        } 
		open(logDir.getAbsolutePath() + "/log");
			
	}

	public ConnectionLog(String basePath)
	  throws IOException
	{
		open(basePath);
	}

	protected void open(String basePath)
	  throws IOException
	{
		File f = new File(basePath + "-" + getTodayString());
		mPath = f.getAbsolutePath();
		mWriter = new BufferedWriter(new FileWriter(mPath), 2048);

		println("Opened log.");
	}

	private static String getTodayString()
	{
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-hhmmss");
		return df.format(new Date());
	}

	public String getPath()
	{
		return mPath;
	}

	public void println(String message)
	  throws IOException
	{
		mWriter.write(TIMESTAMP_FMT.format(new Date()));
		mWriter.write(message);
		mWriter.write('\n');
		mWriter.flush();
	}

	public void close()
	  throws IOException
	{
		mWriter.close();
	}
}
