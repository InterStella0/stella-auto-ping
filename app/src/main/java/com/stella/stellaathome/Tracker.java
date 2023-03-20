package com.stella.stellaathome;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tracker {
    private static final String TAG = "Tracker";
    public int TIMEOUT = 2000;
    public int INTERVAL = 3000;
    private final Pattern arpPattern = Pattern.compile(
            "(\\d+\\.\\d+\\.\\d+\\.\\d+) +0x\\d+ +0x\\d+ +(..:..:..:..:..:..)"
    );
    private final String arp = "/proc/net/arp";
    private final String targetMac;
    private boolean previousFound;
    private final Runnable connectCallback;
    private final Runnable disconnectCallback;
    private Thread thread;
    private Context ctx;
    public Tracker(String monitorMac, Runnable onConnect, Runnable onDisconnect){
        this.targetMac = monitorMac;
        this.previousFound = false;
        this.connectCallback = onConnect;
        this.disconnectCallback = onDisconnect;
        thread = null;
        ctx = null;
    }
    private String getMacAddressFromIP(@NonNull String ipFinding) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(arp))){
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                Matcher m = arpPattern.matcher(line);
                if (!m.find())
                    continue;

                String ip = m.group(1);
                assert ip != null;
                if (ip.equalsIgnoreCase(ipFinding))
                    return m.group(2);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return "00:00:00:00";
    }
    private String getSubnetAddress(int address)
    {
        return (address & 0xff) + "." + (address >> 8 & 0xff) + "." + (address >> 16 & 0xff);
    }

    private Thread ping(List<IpInfo> infos, String host) {
        return new Thread(){
            @Override
            public void run() {
                try {
                    if (InetAddress.getByName(host).isReachable(TIMEOUT)) {
                        String strMacAddress = getMacAddressFromIP(host);
                        infos.add(new IpInfo(host, strMacAddress));
                    }
                } catch (Exception ignored) {}
            }
        };
    }
    private void pingNetwork(String subnet) throws InterruptedException {
        ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiState = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiState.getState() != NetworkInfo.State.CONNECTED) {
            Log.d(TAG, "pingNetwork: NO INTERNET. ABORT");
            return;
        }

        List<IpInfo> deviceInfoList  = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 1;i < 256; i++){
            String host = subnet + "." + i;
            Thread thread = ping(deviceInfoList, host);
            threads.add(thread);
            thread.start();
        }
        for (Thread t: threads)
            t.join();

        boolean isFound = false;
        for (IpInfo e : deviceInfoList)
            if (e.macAddress.equalsIgnoreCase(this.targetMac)) {
                isFound = true;
                break;
            }

        dispatch(isFound);
    }
    private void dispatch(boolean isFound){
        if (previousFound != isFound)
            try {
                (isFound? this.connectCallback : this.disconnectCallback).run();
            }catch (Exception ignored){}

        previousFound = isFound;
    }
    public void stop(){
        if (thread != null && thread.isAlive()){
            thread.interrupt();
        }
        thread = null;
        Log.d(TAG, "stop: STOPPED");
    }
    public void start(Context ctx){
        this.ctx = ctx;
        if (thread != null && thread.isAlive()){
            return;
        }
        Context current = ctx.getApplicationContext();
        WifiManager wifiManager = (WifiManager) current.getSystemService(Context.WIFI_SERVICE);
        String subnet = getSubnetAddress(wifiManager.getDhcpInfo().gateway);
        thread = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        pingNetwork(subnet);
                        Thread.sleep(INTERVAL);
                    }
                }catch (InterruptedException e){
                    Log.d(TAG, "run: STOPPED BY INTERRUPT");
                }
            }
        };
        thread.start();
    }
}
