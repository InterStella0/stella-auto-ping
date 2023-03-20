package com.stella.stellaathome.background;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.stella.stellaathome.Credential;
import com.stella.stellaathome.MainActivity;
import com.stella.stellaathome.R;
import com.stella.stellaathome.StellaService;
import com.stella.stellaathome.Tracker;

public class PingService extends Service {
    private String TAG = "MyService";
    public static boolean isServiceRunning;
    private String CHANNEL_ID = "NOTIFICATION_CHANNEL";
    private Tracker tracker;
    private StellaService service;
    public PingService() {
        Log.d(TAG, "constructor called");
        isServiceRunning = false;
        String mac = Credential.macTarget;
        tracker = new Tracker(mac, this::onMacConnect, this::onMacDisconnect);
        service = new StellaService();
    }
    private void onMacConnect(){
        Log.d(TAG, "onMacConnect: It is connected");
        service.connected();
    }
    private void onMacDisconnect(){
        Log.d(TAG, "onMacConnect: It is disconnected");
        service.disconnected();
    }
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate called");
        createNotificationChannel();
        isServiceRunning = true;
        tracker.start(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand called");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Service is Running")
                .setContentText("Listening for Screen Off/On events")
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String appName = getString(R.string.app_name);
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    appName,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy called");
        isServiceRunning = false;
        stopForeground(true);
        tracker.stop();

        Intent broadcastIntent = new Intent(this, WorkerReceiver.class);
        sendBroadcast(broadcastIntent);

        super.onDestroy();
    }
}