package com.procastia.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Servidor web dentro del Fire TV (puerto 8080).
 * El iPhone abre http://<IP-del-Fire-TV>:8080 y lo guarda como app.
 * No necesita PC.
 */
public class RemoteServer extends Thread {

    public static final int PORT = 8765;
    private final Context ctx;
    private volatile boolean running = true;
    private ServerSocket server;

    public RemoteServer(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket();
            server.setReuseAddress(true);
            server.bind(new java.net.InetSocketAddress(PORT));
            while (running) {
                Socket s = server.accept();
                new Thread(() -> handle(s)).start();
            }
        } catch (Exception ignored) { }
    }

    public void shutdown() {
        running = false;
        try { if (server != null) server.close(); } catch (Exception ignored) { }
    }

    private void handle(Socket s) {
        try (Socket sock = s) {
            sock.setSoTimeout(5000);
            InputStream in = sock.getInputStream();
            // Leer la petición COMPLETA (todas las cabeceras) antes de responder.
            // Si cerramos con datos sin leer, el iPhone recibe un "reset"
            // y Safari dice que se perdió la conexión.
            String first = null;
            StringBuilder line = new StringBuilder();
            int c;
            while ((c = in.read()) != -1) {
                if (c == '\r') continue;
                if (c == '\n') {
                    if (first == null) first = line.toString();
                    else if (line.length() == 0) break;
                    line.setLength(0);
                } else {
                    line.append((char) c);
                }
            }
            if (first == null) first = line.toString();
            String[] parts = first.trim().split(" ");
            String path = parts.length > 1 ? parts[1] : "/";

            OutputStream out = sock.getOutputStream();
            if (path.startsWith("/k?")) {
                String msg = command(param(path, "b"), param(path, "pkg"));
                send(out, 200, "text/plain; charset=utf-8", msg.getBytes(StandardCharsets.UTF_8));
            } else if (path.startsWith("/icon.png")) {
                send(out, 200, "image/png", asset("icon.png"));
            } else if (path.startsWith("/sirio.png")) {
                send(out, 200, "image/png", asset("sirio.png"));
            } else if (path.startsWith("/manifest.json")) {
                send(out, 200, "application/manifest+json", asset("manifest.json"));
            } else {
                send(out, 200, "text/html; charset=utf-8", asset("remote.html"));
            }
        } catch (Exception ignored) { }
    }

    private String command(String b, String pkg) {
        if (b == null) return "Sin botón";
        switch (b) {
            case "home":
                Intent i = new Intent(ctx, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                ctx.startActivity(i);
                MainActivity.sendKey("home");
                return "Home: PROCASTIA";
            case "app":
                if (pkg == null) return "Falta app";
                PackageManager pm = ctx.getPackageManager();
                Intent a = pm.getLeanbackLaunchIntentForPackage(pkg);
                if (a == null) a = pm.getLaunchIntentForPackage(pkg);
                if (a == null) return "No instalada";
                a.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(a);
                return "Abriendo app";
            default:
                // Flechas, OK y Atrás: mueven el menú PROCASTIA
                return MainActivity.sendKey(b) ? "OK: " + b : "Abre tu menú con HOME";
        }
    }

    private static String param(String path, String key) {
        int q = path.indexOf('?');
        if (q < 0) return null;
        for (String kv : path.substring(q + 1).split("&")) {
            String[] p = kv.split("=", 2);
            if (p.length == 2 && p[0].equals(key)) {
                try { return URLDecoder.decode(p[1], "UTF-8"); } catch (Exception e) { return p[1]; }
            }
        }
        return null;
    }

    private byte[] asset(String name) throws Exception {
        try (InputStream is = ctx.getAssets().open(name)) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
            return bo.toByteArray();
        }
    }

    private static void send(OutputStream out, int code, String type, byte[] body) throws Exception {
        String head = "HTTP/1.1 " + code + " OK\r\n"
                + "Content-Type: " + type + "\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Cache-Control: no-cache\r\n"
                + "Connection: close\r\n\r\n";
        out.write(head.getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.flush();
        try { Thread.sleep(50); } catch (InterruptedException ignored) { }
    }
}
