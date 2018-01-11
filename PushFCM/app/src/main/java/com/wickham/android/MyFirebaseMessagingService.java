package com.wickham.android;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = MyFirebaseMessagingService.class.getSimpleName();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        String message = remoteMessage.getNotification().getBody();
        String data = remoteMessage.getData().toString();
        //Log.i(TAG, "Message= " + message);

        if (remoteMessage.getNotification() != null) {
            Intent intent = new Intent(Global.PUSH_NOTIFICATION);
            intent.putExtra(Global.EXTRA_MESSAGE, message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }
}
