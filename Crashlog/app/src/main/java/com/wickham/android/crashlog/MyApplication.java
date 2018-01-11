package com.wickham.android.crashlog;

import org.acra.annotation.ReportsCrashes;
import org.acra.*;
import android.app.Application;

@ReportsCrashes(
		        customReportContent = { ReportField.REPORT_ID,
										ReportField.APP_VERSION_CODE,
										ReportField.APP_VERSION_NAME, 
										ReportField.PACKAGE_NAME, 
										ReportField.PHONE_MODEL, 
										ReportField.ANDROID_VERSION, 
										ReportField.STACK_TRACE,
										ReportField.TOTAL_MEM_SIZE,
										ReportField.AVAILABLE_MEM_SIZE,
										ReportField.DISPLAY,
										ReportField.USER_APP_START_DATE,
										ReportField.USER_CRASH_DATE,
										ReportField.LOGCAT,
										ReportField.DEVICE_ID,
										ReportField.SHARED_PREFERENCES },
				//formKey = "",						
                formUri = "http://www.your-server.com/crashed.php",
                httpMethod = org.acra.sender.HttpSender.Method.POST,
                mode = ReportingInteractionMode.TOAST,
                resToastText = R.string.msg_crash_text)

public class MyApplication extends Application
{
    @Override
    public void onCreate() 
    {
        super.onCreate();
        ACRA.init(this);
    }
}