# Bluetooth WiFi Hotspot Android App

An Android application that automatically enables WiFi hotspot when a selected Bluetooth device comes into range.

## Features

- **Bluetooth Device Scanning**: Scan and discover nearby Bluetooth devices
- **Target Device Selection**: Select a target Bluetooth device from the list
- **Automatic Hotspot Control**: Automatically enables WiFi hotspot when the target device is in range
- **Background Monitoring**: Runs as a foreground service to continuously monitor Bluetooth device proximity
- **Android 16 Compatible**: Designed to work on Android 16 and compatible with older versions

## Permissions

The app requests the following permissions:

- **Bluetooth Permissions**: For scanning and connecting to Bluetooth devices
  - `BLUETOOTH` (Android 11 and below)
  - `BLUETOOTH_ADMIN` (Android 11 and below)
  - `BLUETOOTH_SCAN` (Android 12+)
  - `BLUETOOTH_CONNECT` (Android 12+)

- **Location Permissions**: Required for Bluetooth scanning on Android 6+
  - `ACCESS_FINE_LOCATION`
  - `ACCESS_COARSE_LOCATION`

- **WiFi Permissions**: For controlling WiFi hotspot
  - `ACCESS_WIFI_STATE`
  - `CHANGE_WIFI_STATE`
  - `CHANGE_NETWORK_STATE`
  - `ACCESS_NETWORK_STATE`

- **Service Permissions**: For background monitoring
  - `FOREGROUND_SERVICE`
  - `FOREGROUND_SERVICE_CONNECTED_DEVICE`
  - `POST_NOTIFICATIONS` (Android 13+)

## How to Use

1. **Launch the App**: Open the Bluetooth WiFi Hotspot app
2. **Grant Permissions**: Allow all required permissions when prompted
3. **Scan for Devices**: Tap the "Scan Bluetooth Devices" button
4. **Select Target Device**: Tap on a device from the list to set it as the target
5. **Automatic Monitoring**: The app will now monitor for the target device and automatically enable WiFi hotspot when in range

## Technical Details

### Components

- **MainActivity**: Main UI for scanning and selecting Bluetooth devices
- **BluetoothMonitorService**: Foreground service that monitors target device proximity
- **WifiHotspotManager**: Manages WiFi hotspot enable/disable operations

### Building the App

To build the app, you need:
- Android SDK with API level 34
- Java 8 or higher
- Gradle 8.0 or higher

Build commands:
```bash
./gradlew assembleDebug
```

### Important Notes

- **Android 8.0+ Limitations**: Starting from Android 8.0 (API level 26), apps cannot programmatically enable/disable WiFi hotspot without system-level permissions. On these versions, the app will attempt to use available APIs but may require manual hotspot setup.
- **Battery Optimization**: The app uses a foreground service to ensure continuous monitoring. Users should exempt the app from battery optimization for best results.
- **Bluetooth Range**: Detection range depends on Bluetooth signal strength and environmental factors.

## Project Structure

```
app/
├── src/main/
│   ├── java/com/foodhunter/bluetoothwifihotspot/
│   │   ├── MainActivity.java
│   │   ├── BluetoothMonitorService.java
│   │   └── WifiHotspotManager.java
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml
│   │   │   └── device_item.xml
│   │   ├── values/
│   │   │   └── strings.xml
│   │   └── mipmap-*/
│   └── AndroidManifest.xml
└── build.gradle
```

## License

This project is open source.