package com.procastia.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Arranca el vigilante del botón Home cuando el Fire TV enciende. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        HomeWatcherService.start(context);
        Intent i = new Intent(context, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }
}
