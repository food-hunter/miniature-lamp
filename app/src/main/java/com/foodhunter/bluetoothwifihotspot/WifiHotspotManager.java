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
    private WifiManager.LocalOnlyHotspotReservation hotspotReservation;

    public WifiHotspotManager(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public void enableHotspot() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ - Use TetheringManager if available, otherwise open settings
                enableHotspotViaSettings();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8.0-12 - Use LocalOnlyHotspot API
                enableLocalOnlyHotspot();
            } else {
                // Android 7.x and below - Use reflection
                enableHotspotViaReflection(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error enabling WiFi hotspot", e);
            // Fallback to opening settings
            openHotspotSettings();
        }
    }

    public void disableHotspot() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ - Close reservation or open settings
                disableLocalOnlyHotspot();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8.0-12 - Stop LocalOnlyHotspot
                disableLocalOnlyHotspot();
            } else {
                // Android 7.x and below - Use reflection
                enableHotspotViaReflection(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error disabling WiFi hotspot", e);
        }
    }

    private void enableLocalOnlyHotspot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                wifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {
                    @Override
                    public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                        super.onStarted(reservation);
                        hotspotReservation = reservation;
                        WifiConfiguration config = null;
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            config = reservation.getWifiConfiguration();
                        }
                        if (config != null) {
                            Log.d(TAG, "LocalOnlyHotspot started. SSID: " + config.SSID + ", Password: " + config.preSharedKey);
                        } else {
                            Log.d(TAG, "LocalOnlyHotspot started (config not available on Android 13+)");
                        }
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
                        // Fallback to opening settings
                        openHotspotSettings();
                    }
                }, new Handler(Looper.getMainLooper()));
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception starting LocalOnlyHotspot", e);
                openHotspotSettings();
            }
        }
    }

    private void disableLocalOnlyHotspot() {
        if (hotspotReservation != null) {
            hotspotReservation.close();
            hotspotReservation = null;
            Log.d(TAG, "LocalOnlyHotspot reservation closed");
        }
    }

    private void enableHotspotViaReflection(boolean enable) {
        try {
            Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(wifiManager, null, enable);
            Log.d(TAG, "WiFi Hotspot " + (enable ? "enabled" : "disabled") + " via reflection");
        } catch (Exception e) {
            Log.e(TAG, "Error controlling hotspot via reflection", e);
            throw new RuntimeException(e);
        }
    }

    private void enableHotspotViaSettings() {
        // For Android 13+, open settings panel for quick access
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            openSettingsPanel();
        } else {
            openHotspotSettings();
        }
    }

    private void openSettingsPanel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                Intent panelIntent = new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY);
                panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(panelIntent);
                Log.d(TAG, "Opened Settings Panel for hotspot control");
            } catch (Exception e) {
                Log.e(TAG, "Error opening settings panel", e);
                openHotspotSettings();
            }
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
                // Fallback to WiFi settings
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
            // Try reflection first
            Method method = wifiManager.getClass().getMethod("isWifiApEnabled");
            return (Boolean) method.invoke(wifiManager);
        } catch (Exception e) {
            // Check if LocalOnlyHotspot is active
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
