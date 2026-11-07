package com.procastia.launcher;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/**
 * Vigila qué app está al frente. Si aparece el Home de Amazon
 * (porque presionaste Home), abre el menú PROCASTIA encima.
 * Necesita el permiso de uso, que se da una vez por adb:
 *   adb shell appops set com.procastia.launcher GET_USAGE_STATS allow
 */
public class HomeWatcherService extends Service {

    private static final String[] AMAZON_HOME = {
            "com.amazon.tv.launcher",
            "com.amazon.firebat"
    };
    private static final long POLL_MS = 400;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private UsageStatsManager usm;
    private long lastLaunch = 0;
    private RemoteServer remote;

    public static void start(Context ctx) {
        Intent i = new Intent(ctx, HomeWatcherService.class);
        if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i);
        else ctx.startService(i);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        startForeground(1, buildNotification());
        handler.post(tick);
        remote = new RemoteServer(this);
        remote.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            try {
                String top = foregroundPackage();
                if (top != null && isAmazonHome(top)) {
                    long now = System.currentTimeMillis();
                    if (now - lastLaunch > 1500) {
                        lastLaunch = now;
                        Intent i = new Intent(HomeWatcherService.this, MainActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(i);
                    }
                }
            } catch (Exception ignored) { }
            handler.postDelayed(this, POLL_MS);
        }
    };

    private String foregroundPackage() {
        if (usm == null) return null;
        long now = System.currentTimeMillis();
        UsageEvents events = usm.queryEvents(now - 3000, now);
        UsageEvents.Event e = new UsageEvents.Event();
        String last = null;
        while (events.hasNextEvent()) {
            events.getNextEvent(e);
            if (e.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                last = e.getPackageName();
            }
        }
        return last;
    }

    private boolean isAmazonHome(String pkg) {
        for (String p : AMAZON_HOME) if (p.equals(pkg)) return true;
        return false;
    }

    private Notification buildNotification() {
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            NotificationChannel ch = new NotificationChannel(
                    "home", "Botón Home", NotificationManager.IMPORTANCE_MIN);
            nm.createNotificationChannel(ch);
            b = new Notification.Builder(this, "home");
        } else {
            b = new Notification.Builder(this);
        }
        return b.setContentTitle("PROCASTIA")
                .setContentText("Home lleva a tu menú")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .build();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tick);
        if (remote != null) remote.shutdown();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
