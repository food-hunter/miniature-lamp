package com.foodhunter.bluetoothwifihotspot;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private static final String PREFS_NAME = "BluetoothWiFiHotspotPrefs";
    private static final String PREF_TARGET_DEVICE = "target_device_address";
    private static final String PREF_TARGET_DEVICE_NAME = "target_device_name";

    private BluetoothAdapter bluetoothAdapter;
    private ListView deviceListView;
    private ArrayAdapter<String> deviceListAdapter;
    private List<BluetoothDevice> deviceList;
    private Button scanButton;
    private TextView targetDeviceText;
    private TextView statusText;

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && !deviceList.contains(device)) {
                    deviceList.add(device);
                    updateDeviceList();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                scanButton.setEnabled(true);
                scanButton.setText(R.string.scan_bluetooth);
                if (deviceList.isEmpty()) {
                    statusText.setText(R.string.no_devices_found);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceListView = findViewById(R.id.deviceListView);
        scanButton = findViewById(R.id.scanButton);
        targetDeviceText = findViewById(R.id.targetDeviceText);
        statusText = findViewById(R.id.statusText);

        deviceList = new ArrayList<>();
        deviceListAdapter = new ArrayAdapter<>(this, R.layout.device_item, R.id.deviceName, new ArrayList<>());
        deviceListView.setAdapter(deviceListAdapter);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadTargetDevice();

        scanButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                startBluetoothScan();
            }
        });

        deviceListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < deviceList.size()) {
                BluetoothDevice device = deviceList.get(position);
                selectTargetDevice(device);
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothReceiver, filter);

        if (checkPermissions()) {
            enableBluetooth();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(bluetoothReceiver);
        } catch (Exception e) {
            // Receiver not registered
        }
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter.cancelDiscovery();
            }
        }
    }

    private boolean checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Bluetooth permissions for Android 16
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_ADVERTISE);
        }

        // Location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }

        // Notification permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), REQUEST_PERMISSIONS);
            return false;
        }

        // Check special permissions that require separate intents
        checkSpecialPermissions();

        return true;
    }

    private void checkSpecialPermissions() {
        // Check SYSTEM_ALERT_WINDOW permission
        if (!Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("Display Over Other Apps")
                    .setMessage("This app needs permission to display over other apps for automated hotspot control. Please grant this permission.")
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        startActivity(intent);
                    })
                    .show();
        }

        // Check WRITE_SETTINGS permission
        if (!Settings.System.canWrite(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("Modify System Settings")
                    .setMessage("This app needs permission to modify system settings for WiFi hotspot control. Please grant this permission.")
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                        startActivity(intent);
                    })
                    .show();
        }

        // Prompt for Accessibility Service
        if (HotspotAccessibilityService.getInstance() == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Accessibility Service")
                    .setMessage("For full automation, enable the Hotspot Accessibility Service in Settings > Accessibility.")
                    .setPositiveButton("Open Settings", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                        startActivity(intent);
                    })
                    .setNegativeButton("Later", null)
                    .show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.permission_required)
                        .setMessage(R.string.permission_denied)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            } else {
                enableBluetooth();
            }
        }
    }

    private void enableBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, R.string.bluetooth_disabled, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startBluetoothScan() {
        if (bluetoothAdapter == null) {
            return;
        }

        deviceList.clear();
        deviceListAdapter.clear();
        statusText.setText("");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices != null) {
                deviceList.addAll(pairedDevices);
                updateDeviceList();
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            
            bluetoothAdapter.startDiscovery();
            scanButton.setEnabled(false);
            scanButton.setText("Scanning...");
        }
    }

    private void updateDeviceList() {
        deviceListAdapter.clear();
        for (BluetoothDevice device : deviceList) {
            String deviceName = "Unknown Device";
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                deviceName = device.getName() != null ? device.getName() : "Unknown Device";
            }
            deviceListAdapter.add(deviceName + "\n" + device.getAddress());
        }
        deviceListAdapter.notifyDataSetChanged();
    }

    private void selectTargetDevice(BluetoothDevice device) {
        String deviceName = "Unknown Device";
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            deviceName = device.getName() != null ? device.getName() : "Unknown Device";
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putString(PREF_TARGET_DEVICE, device.getAddress())
                .putString(PREF_TARGET_DEVICE_NAME, deviceName)
                .apply();

        targetDeviceText.setText(getString(R.string.target_device, deviceName));
        targetDeviceText.setVisibility(View.VISIBLE);

        Toast.makeText(this, getString(R.string.target_device_set, deviceName), Toast.LENGTH_SHORT).show();

        Intent serviceIntent = new Intent(this, BluetoothMonitorService.class);
        serviceIntent.putExtra("device_address", device.getAddress());
        serviceIntent.putExtra("device_name", deviceName);
        
        startForegroundService(serviceIntent);
    }

    private void loadTargetDevice() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String deviceAddress = prefs.getString(PREF_TARGET_DEVICE, null);
        String deviceName = prefs.getString(PREF_TARGET_DEVICE_NAME, null);

        if (deviceAddress != null && deviceName != null) {
            targetDeviceText.setText(getString(R.string.target_device, deviceName));
            targetDeviceText.setVisibility(View.VISIBLE);
        }
    }
}
