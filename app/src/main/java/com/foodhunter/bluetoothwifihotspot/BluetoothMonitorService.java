package com.foodhunter.bluetoothwifihotspot;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

public class BluetoothMonitorService extends Service {

    private static final String TAG = "BluetoothMonitorService";
    private static final String CHANNEL_ID = "BluetoothMonitorChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final long SCAN_INTERVAL = 10000; // 10 seconds

    private BluetoothAdapter bluetoothAdapter;
    private String targetDeviceAddress;
    private String targetDeviceName;
    private WifiHotspotManager hotspotManager;
    private boolean isTargetInRange = false;
    private Handler scanHandler;
    private Runnable scanRunnable;

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.class, BluetoothDevice.EXTRA_DEVICE);
                if (device != null && device.getAddress().equals(targetDeviceAddress)) {
                    onTargetDeviceFound();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (!isTargetInRange) {
                    onTargetDeviceLost();
                }
                scheduleNextScan();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        hotspotManager = new WifiHotspotManager(this);
        scanHandler = new Handler(Looper.getMainLooper());

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification("Monitoring started"));

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            targetDeviceAddress = intent.getStringExtra("device_address");
            targetDeviceName = intent.getStringExtra("device_name");
            
            if (targetDeviceAddress != null) {
                Log.d(TAG, "Monitoring target device: " + targetDeviceName);
                startMonitoring();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMonitoring();
        try {
            unregisterReceiver(bluetoothReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receiver", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Bluetooth Monitoring",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Monitors Bluetooth device proximity");
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(String contentText) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.monitoring_service))
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String contentText) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, createNotification(contentText));
    }

    private void startMonitoring() {
        scanRunnable = new Runnable() {
            @Override
            public void run() {
                performBluetoothScan();
            }
        };
        performBluetoothScan();
    }

    private void stopMonitoring() {
        if (scanHandler != null && scanRunnable != null) {
            scanHandler.removeCallbacks(scanRunnable);
        }
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                bluetoothAdapter.cancelDiscovery();
            }
        }
    }

    private void performBluetoothScan() {
        if (bluetoothAdapter == null || targetDeviceAddress == null) {
            return;
        }

        isTargetInRange = false;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            
            bluetoothAdapter.startDiscovery();
        }
    }

    private void scheduleNextScan() {
        if (scanHandler != null && scanRunnable != null) {
            scanHandler.postDelayed(scanRunnable, SCAN_INTERVAL);
        }
    }

    private void onTargetDeviceFound() {
        if (!isTargetInRange) {
            isTargetInRange = true;
            Log.d(TAG, "Target device found: " + targetDeviceName);
            updateNotification("Target device in range");
            
            // Enable WiFi hotspot
            hotspotManager.enableHotspot();
        }
    }

    private void onTargetDeviceLost() {
        if (isTargetInRange) {
            isTargetInRange = false;
            Log.d(TAG, "Target device lost: " + targetDeviceName);
            updateNotification("Target device out of range");
            
            // Disable WiFi hotspot
            hotspotManager.disableHotspot();
        }
    }
}
