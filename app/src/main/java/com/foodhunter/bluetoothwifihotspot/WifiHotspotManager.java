package com.foodhunter.bluetoothwifihotspot;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Method;

public class WifiHotspotManager {

    private static final String TAG = "WifiHotspotManager";
    private final Context context;
    private final WifiManager wifiManager;

    public WifiHotspotManager(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public void enableHotspot() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // For Android 8.0 and above, we need to use the WifiManager's startLocalOnlyHotspot
                // Note: This requires user interaction or system permissions
                Log.d(TAG, "Hotspot control requires manual setup on Android 8.0+");
                // On Android 8.0+, apps cannot programmatically enable/disable hotspot
                // The user must enable it manually or the app needs to be a system app
            } else {
                // For older Android versions, use reflection
                Method method = wifiManager.getClass().getMethod("setWifiApEnabled", android.net.wifi.WifiConfiguration.class, boolean.class);
                method.invoke(wifiManager, null, true);
                Log.d(TAG, "WiFi Hotspot enabled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error enabling WiFi hotspot", e);
        }
    }

    public void disableHotspot() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // For Android 8.0 and above
                Log.d(TAG, "Hotspot control requires manual setup on Android 8.0+");
            } else {
                // For older Android versions, use reflection
                Method method = wifiManager.getClass().getMethod("setWifiApEnabled", android.net.wifi.WifiConfiguration.class, boolean.class);
                method.invoke(wifiManager, null, false);
                Log.d(TAG, "WiFi Hotspot disabled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error disabling WiFi hotspot", e);
        }
    }

    public boolean isHotspotEnabled() {
        try {
            Method method = wifiManager.getClass().getMethod("isWifiApEnabled");
            return (Boolean) method.invoke(wifiManager);
        } catch (Exception e) {
            Log.e(TAG, "Error checking WiFi hotspot status", e);
            return false;
        }
    }
}
