package com.foodhunter.bluetoothwifihotspot;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.Method;

public class WifiHotspotManager {

    private static final String TAG = "WifiHotspotManager";
    private final Context context;
    private final WifiManager wifiManager;
    private final ConnectivityManager connectivityManager;
    private volatile WifiManager.LocalOnlyHotspotReservation hotspotReservation;

    public WifiHotspotManager(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public void enableHotspot() {
        try {
            // For Android 16, try multiple approaches
            
            // Method 1: Try TetheringManager with reflection (privileged API)
            if (tryTetheringManager(true)) {
                Log.d(TAG, "Hotspot enabled via TetheringManager");
                return;
            }
            
            // Method 2: Try shell commands with WRITE_SECURE_SETTINGS
            if (tryShellCommand(true)) {
                Log.d(TAG, "Hotspot enabled via shell command");
                return;
            }
            
            // Method 3: Use accessibility service to automate settings
            if (useAccessibilityService(true)) {
                Log.d(TAG, "Hotspot enable requested via accessibility service");
                return;
            }
            
            // Method 4: Fallback to LocalOnlyHotspot
            enableLocalOnlyHotspot();
            
        } catch (Exception e) {
            Log.e(TAG, "Error enabling WiFi hotspot", e);
            openHotspotSettings();
        }
    }

    public void disableHotspot() {
        try {
            // Try same methods for disabling
            
            if (tryTetheringManager(false)) {
                Log.d(TAG, "Hotspot disabled via TetheringManager");
                return;
            }
            
            if (tryShellCommand(false)) {
                Log.d(TAG, "Hotspot disabled via shell command");
                return;
            }
            
            if (useAccessibilityService(false)) {
                Log.d(TAG, "Hotspot disable requested via accessibility service");
                return;
            }
            
            disableLocalOnlyHotspot();
            
        } catch (Exception e) {
            Log.e(TAG, "Error disabling WiFi hotspot", e);
        }
    }

    private boolean tryTetheringManager(boolean enable) {
        try {
            Class<?> cmClass = Class.forName("android.net.ConnectivityManager");
            Method method;
            
            if (enable) {
                // Try to start tethering
                method = cmClass.getDeclaredMethod("startTethering", int.class, boolean.class,
                        Class.forName("android.net.ConnectivityManager$OnStartTetheringCallback"),
                        Handler.class);
                method.setAccessible(true);
                
                Object callback = java.lang.reflect.Proxy.newProxyInstance(
                    context.getClassLoader(),
                    new Class[] { Class.forName("android.net.ConnectivityManager$OnStartTetheringCallback") },
                    (proxy, m, args) -> {
                        Log.d(TAG, "Tethering callback: " + m.getName());
                        return null;
                    }
                );
                
                method.invoke(connectivityManager, 0, false, callback, new Handler(Looper.getMainLooper()));
                return true;
            } else {
                // Try to stop tethering
                method = cmClass.getDeclaredMethod("stopTethering", int.class);
                method.setAccessible(true);
                method.invoke(connectivityManager, 0);
                return true;
            }
        } catch (Exception e) {
            Log.d(TAG, "TetheringManager method failed", e);
            return false;
        }
    }

    private boolean tryShellCommand(boolean enable) {
        try {
            // Try using Runtime.exec with shell commands
            // This requires WRITE_SECURE_SETTINGS permission
            String command = enable ? 
                "settings put global tether_dun_required 0 && svc wifi enable_softap 0" :
                "svc wifi disable_softap";
            
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
            int result = process.waitFor();
            
            if (result == 0) {
                Log.d(TAG, "Shell command executed successfully");
                return true;
            }
        } catch (Exception e) {
            Log.d(TAG, "Shell command failed", e);
        }
        return false;
    }

    private boolean useAccessibilityService(boolean enable) {
        HotspotAccessibilityService service = HotspotAccessibilityService.getInstance();
        if (service != null) {
            if (enable) {
                service.requestEnableHotspot();
            } else {
                service.requestDisableHotspot();
            }
            // Open settings to allow accessibility service to work
            openHotspotSettings();
            return true;
        }
        Log.d(TAG, "Accessibility service not available");
        return false;
    }

    private void enableLocalOnlyHotspot() {
        try {
            wifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {
                @Override
                public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                    super.onStarted(reservation);
                    hotspotReservation = reservation;
                    Log.d(TAG, "LocalOnlyHotspot started successfully");
                }

                @Override
                public void onStopped() {
                    super.onStopped();
                    Log.d(TAG, "LocalOnlyHotspot stopped");
                    hotspotReservation = null;
                }

                @Override
                public void onFailed(int reason) {
                    super.onFailed(reason);
                    Log.e(TAG, "LocalOnlyHotspot failed with reason: " + reason);
                    openHotspotSettings();
                }
            }, new Handler(Looper.getMainLooper()));
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception starting LocalOnlyHotspot", e);
            openHotspotSettings();
        }
    }

    private void disableLocalOnlyHotspot() {
        if (hotspotReservation != null) {
            hotspotReservation.close();
            hotspotReservation = null;
            Log.d(TAG, "LocalOnlyHotspot reservation closed");
        }
    }

    private void openHotspotSettings() {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setClassName("com.android.settings", "com.android.settings.TetherSettings");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.d(TAG, "Opened hotspot settings");
        } catch (Exception e) {
            Log.e(TAG, "Error opening hotspot settings, trying alternative", e);
            try {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception ex) {
                Log.e(TAG, "Error opening wireless settings", ex);
            }
        }
    }

    public boolean isHotspotEnabled() {
        try {
            Method method = wifiManager.getClass().getMethod("isWifiApEnabled");
            return (Boolean) method.invoke(wifiManager);
        } catch (Exception e) {
            if (hotspotReservation != null) {
                return true;
            }
            Log.d(TAG, "Unable to check hotspot status", e);
            return false;
        }
    }

    public void cleanup() {
        disableLocalOnlyHotspot();
    }
}

