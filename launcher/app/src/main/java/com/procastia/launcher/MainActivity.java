package com.procastia.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class MainActivity extends Activity {

    private WebView web;
    private static MainActivity current;
    private static boolean resumed;

    /** Llamado por el control remoto: manda una tecla al menú si está al frente. */
    public static boolean sendKey(String b) {
        final MainActivity a = current;
        if (a == null || a.web == null) return false;
        final String js = "window.remoteKey && window.remoteKey('" + b.replaceAll("[^a-z]", "") + "')";
        a.runOnUiThread(() -> a.web.evaluateJavascript(js, null));
        return resumed || b.equals("home");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        web.setWebViewClient(new WebViewClient());
        web.setBackgroundColor(0xFF0B1712);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
        web.requestFocus();
        HomeWatcherService.start(this);
        current = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        current = this;
        if (web != null) web.evaluateJavascript("window.onHome && window.onHome()", null);
    }

    @Override
    public void onBackPressed() {
        // Este es el menú principal: "Atrás" no lo cierra.
        if (web != null) web.evaluateJavascript("window.onBack && window.onBack()", null);
    }

    private void toast(String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }

    /** Funciones que el HTML puede llamar con window.Android.<nombre>() */
    private class Bridge {

        @JavascriptInterface
        public void openApp(String pkg) {
            PackageManager pm = getPackageManager();
            Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
            if (i == null) i = pm.getLaunchIntentForPackage(pkg);
            if (i == null) {
                toast("Esa app no está instalada");
                return;
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }

        @JavascriptInterface
        public void openUrl(String url) {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            } catch (Exception e) {
                toast("No se pudo abrir");
            }
        }

        @JavascriptInterface
        public void openSettings() {
            Intent i = new Intent(Settings.ACTION_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }

        /** Lista de apps instaladas como JSON: [{name, pkg}] */
        @JavascriptInterface
        public String listApps() {
            JSONArray out = new JSONArray();
            try {
                PackageManager pm = getPackageManager();
                Intent q = new Intent(Intent.ACTION_MAIN);
                q.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
                List<ResolveInfo> apps = pm.queryIntentActivities(q, 0);
                for (ResolveInfo r : apps) {
                    String pkg = r.activityInfo.packageName;
                    if (pkg.equals(getPackageName())) continue;
                    JSONObject o = new JSONObject();
                    o.put("name", r.loadLabel(pm).toString());
                    o.put("pkg", pkg);
                    out.put(o);
                }
            } catch (Exception ignored) { }
            return out.toString();
        }
    }
}
