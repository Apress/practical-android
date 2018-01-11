<?php
		$dt = new DateTime('now', new DateTimeZone('Asia/Hong_Kong'));
		// save the orderday DD and ordermon MM based on the adjusted TZ
		// ex, 2013-11-11 13:24:56
		$orderday = substr($dt->format('Y-m-d H:i:s'),8,2);
		$ordermon = substr($dt->format('Y-m-d H:i:s'),5,2);
		$ordertim = substr($dt->format('Y-m-d H:i:s'),11,2) . substr($dt->format('Y-m-d H:i:s'),14,2) . substr($dt->format('Y-m-d H:i:s'),17,2);

		$fname = $ordermon . $orderday . $ordertim . ".txt";

		$FileLog = $_SERVER['DOCUMENT_ROOT'] . "/crashlogs/" . $fname;

		$HandleLog = fopen($FileLog, 'a');

		fwrite($HandleLog, "REPORT_ID=" . $_POST['REPORT_ID'] . "\r\n");
		fwrite($HandleLog, "APP_VERSION_CODE=" . $_POST['APP_VERSION_CODE'] . "\r\n");
		fwrite($HandleLog, "APP_VERSION_NAME=" . $_POST['APP_VERSION_NAME'] . "\r\n");
		fwrite($HandleLog, "PACKAGE_NAME=" . $_POST['PACKAGE_NAME'] . "\r\n");
		fwrite($HandleLog, "FILE_PATH=" . $_POST['FILE_PATH'] . "\r\n");
		fwrite($HandleLog, "PHONE_MODEL=" . $_POST['PHONE_MODEL'] . "\r\n");
		fwrite($HandleLog, "ANDROID_VERSION=" . $_POST['ANDROID_VERSION'] . "\r\n");
		fwrite($HandleLog, "STACK_TRACE=" . $_POST['STACK_TRACE'] . "\r\n"); 
		fwrite($HandleLog, "LOGCAT=" . $_POST['LOGCAT'] . "\r\n");    
		fwrite($HandleLog, "BUILD=" . $_POST['BUILD'] . "\r\n");
		fwrite($HandleLog, "BRAND=" . $_POST['BRAND'] . "\r\n");
		fwrite($HandleLog, "PRODUCT=" . $_POST['PRODUCT'] . "\r\n");
		fwrite($HandleLog, "TOTAL_MEM_SIZE=" . $_POST['TOTAL_MEM_SIZE'] . "\r\n");
		fwrite($HandleLog, "AVAILABLE_MEM_SIZE=" . $_POST['AVAILABLE_MEM_SIZE'] . "\r\n");
		fwrite($HandleLog, "CUSTOM_DATA=" . $_POST['CUSTOM_DATA'] . "\r\n");
		fwrite($HandleLog, "STACK_TRACE=" . $_POST['STACK_TRACE'] . "\r\n");
		fwrite($HandleLog, "INITIAL_CONFIGURATION=" . $_POST['INITIAL_CONFIGURATION'] . "\r\n");
		fwrite($HandleLog, "CRASH_CONFIGURATION=" . $_POST['CRASH_CONFIGURATION'] . "\r\n");
		fwrite($HandleLog, "DISPLAY=" . $_POST['DISPLAY'] . "\r\n");
		fwrite($HandleLog, "USER_COMMENT=" . $_POST['USER_COMMENT'] . "\r\n");
		fwrite($HandleLog, "USER_APP_START_DATE=" . $_POST['USER_APP_START_DATE'] . "\r\n");
		fwrite($HandleLog, "USER_CRASH_DATE=" . $_POST['USER_CRASH_DATE'] . "\r\n");
		fwrite($HandleLog, "DUMPSYS_MEMINFO=" . $_POST['DUMPSYS_MEMINFO'] . "\r\n");
		fwrite($HandleLog, "DROPBOX=" . $_POST['DROPBOX'] . "\r\n");
		fwrite($HandleLog, "EVENTSLOG=" . $_POST['EVENTSLOG'] . "\r\n");
		fwrite($HandleLog, "RADIOLOG=" . $_POST['RADIOLOG'] . "\r\n");
		fwrite($HandleLog, "IS_SILENT=" . $_POST['IS_SILENT'] . "\r\n");
		fwrite($HandleLog, "DEVICE_ID=" . $_POST['DEVICE_ID'] . "\r\n");
		fwrite($HandleLog, "INSTALLATION_ID=" . $_POST['INSTALLATION_ID'] . "\r\n");
		fwrite($HandleLog, "USER_EMAIL=" . $_POST['USER_EMAIL'] . "\r\n");
		fwrite($HandleLog, "DEVICE_FEATURES ENVIRONMENT=" . $_POST['DEVICE_FEATURE_ENVIRONMENT'] . "\r\n");
		fwrite($HandleLog, "SETTINGS_SYSTEM SETTINGS_SECURE=" . $_POST['SETTINGS_SYSTEM_SETTINGS_SECURE'] . "\r\n");
		fwrite($HandleLog, "SETTINGS_GLOBAL =" . $_POST['SETTINGS_GLOBAL'] . "\r\n");
		fwrite($HandleLog, "SHARED_PREFERENCES=" . $_POST['SHARED_PREFERENCES'] . "\r\n");
		fwrite($HandleLog, "MEDIA_CODEC_LIST=" . $_POST['MEDIA_CODEC_LIST'] . "\r\n");
		fwrite($HandleLog, "APPLICATION_LOG=" . $_POST['APPLICATION_LOG'] . "\r\n");
		fwrite($HandleLog, "THREAD_DETAILS=" . $_POST['THREAD_DETAILS'] . "\r\n");
		fwrite($HandleLog, "USER_IP=" . $_POST['USER_IP'] . "\r\n");

		fclose($HandleLog);
?>