package org.kivy.android;

import android.os.Build;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import android.app.Service;
import android.os.IBinder;
import android.os.Bundle;
import android.content.Intent;
import android.content.Context;
import android.util.Log;
import android.app.Notification;
import android.app.PendingIntent;
import android.os.Process;
import java.io.File;

//imports for channel definition
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;

import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;

public class PythonService extends Service implements Runnable {

    // Thread for Python code
    private Thread pythonThread = null;

    // Python environment variables
    private String androidPrivate;
    private String androidArgument;
    private String pythonName;
    private String pythonHome;
    private String pythonPath;
    private String serviceEntrypoint;
    // Argument to pass to Python code,
    private String pythonServiceArgument;


    public static PythonService mService = null;
    private Intent startIntent = null;

    private boolean autoRestartService = false;

    public void setAutoRestartService(boolean restart) {
        autoRestartService = restart;
    }

    public int startType() {
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (pythonThread != null) {
            Log.v("python service", "service exists, do not start again");
            return startType();
        }
    //intent is null if OS restarts a STICKY service
        if (intent == null) {
            Context context = getApplicationContext();
            intent = getThisDefaultIntent(context, "");
        }

        startIntent = intent;
        Bundle extras = intent.getExtras();
        androidPrivate = extras.getString("androidPrivate");
        androidArgument = extras.getString("androidArgument");
        serviceEntrypoint = extras.getString("serviceEntrypoint");
        pythonName = extras.getString("pythonName");
        pythonHome = extras.getString("pythonHome");
        pythonPath = extras.getString("pythonPath");
        boolean serviceStartAsForeground = (
            extras.getString("serviceStartAsForeground").equals("true")
        );
        pythonServiceArgument = extras.getString("pythonServiceArgument");
        pythonThread = new Thread(this);
        pythonThread.start();

        if (serviceStartAsForeground) {
            doStartForeground(extras);
        }

        return startType();
    }

    protected int getServiceId() {
        return 1;
    }

    protected Intent getThisDefaultIntent(Context ctx, String pythonServiceArgument) {
        return null;
    }

    protected void doStartForeground(Bundle extras) {
        String serviceTitle = extras.getString("serviceTitle");
        String serviceDescription = extras.getString("serviceDescription");
        Notification notification;
        Context context = getApplicationContext();
        Intent contextIntent = new Intent(context, PythonActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, contextIntent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notification = new Notification(
                context.getApplicationInfo().icon, serviceTitle, System.currentTimeMillis());
            try {
                // prevent using NotificationCompat, this saves 100kb on apk
                Method func = notification.getClass().getMethod(
                    "setLatestEventInfo", Context.class, CharSequence.class,
                    CharSequence.class, PendingIntent.class);
                func.invoke(notification, context, serviceTitle, serviceDescription, pIntent);
            } catch (NoSuchMethodException | IllegalAccessException |
                     IllegalArgumentException | InvocationTargetException e) {
            }
        } else {
            // for android 8+ we need to create our own channel
            // https://stackoverflow.com/questions/47531742/startforeground-fail-after-upgrade-to-android-8-1
            String NOTIFICATION_CHANNEL_ID = "uk.co.liberatedsystems.occ.reticulum";
            String channelName = "Background Service";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, 
                NotificationManager.IMPORTANCE_NONE);
            
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            chan.setShowBadge(false);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(chan);

            Notification.Builder builder = new Notification.Builder(context, NOTIFICATION_CHANNEL_ID);
            builder.setContentTitle("openCom Companion Active");
            // builder.setContentText("Reticulum Active");
            // builder.setContentTitle("Reticulum available");
            // builder.setContentText("Reticulum available");
            builder.setContentIntent(pIntent);
            // builder.setOngoing(true);

            String files_path = context.getFilesDir().getPath();
            Bitmap icon_bitmap = BitmapFactory.decodeFile(files_path+"/app/assets/notification_icon_black.png");
            Icon service_icon = Icon.createWithBitmap(icon_bitmap);
            // builder.setSmallIcon(context.getApplicationInfo().icon);
            builder.setSmallIcon(service_icon);
            notification = builder.build();
        }
        startForeground(getServiceId(), notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        pythonThread = null;
        if (autoRestartService && startIntent != null) {
            Log.v("python service", "service restart requested");
            startService(startIntent);
        }
        Process.killProcess(Process.myPid());
    }

    /**
     * Stops the task gracefully when killed.
     * Calling stopSelf() will trigger a onDestroy() call from the system.
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        //sticky servcie runtime/restart is managed by the OS. leave it running when app is closed
        if (startType() != START_STICKY) {
            //stopSelf();
        }
    }

    @Override
    public void run(){
        String app_root =  getFilesDir().getAbsolutePath() + "/app";
        File app_root_file = new File(app_root);
        PythonUtil.loadLibraries(app_root_file,
            new File(getApplicationInfo().nativeLibraryDir));
        this.mService = this;
        nativeStart(
            androidPrivate, androidArgument,
            serviceEntrypoint, pythonName,
            pythonHome, pythonPath,
            pythonServiceArgument);
        stopSelf();
    }

    // Native part
    public static native void nativeStart(
            String androidPrivate, String androidArgument,
            String serviceEntrypoint, String pythonName,
            String pythonHome, String pythonPath,
            String pythonServiceArgument);
}
