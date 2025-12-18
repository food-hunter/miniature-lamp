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

### WiFi Hotspot Automation

The app uses different strategies to enable WiFi hotspot depending on Android version:

#### **Android 7.x and Earlier**
- **Full Automation**: Uses reflection to directly control the WiFi hotspot
- Hotspot is automatically enabled/disabled based on device proximity

#### **Android 8.0 - 12.x**
- **LocalOnlyHotspot API**: Automatically creates a temporary local hotspot
- No user interaction required
- Automatically generated SSID and password
- Limited to local subnet devices
- Hotspot is managed by the system and automatically cleaned up

#### **Android 13+**
- **Settings Panel Integration**: Opens quick settings panel for easy manual control
- Provides fastest path to hotspot controls
- One-tap access when device is in range

#### **Fallback Behavior**
- If automation fails, the app opens the hotspot settings page
- Provides clear guidance for manual setup
- Ensures functionality on all device manufacturers

### Important Notes

- **Battery Optimization**: The app uses a foreground service to ensure continuous monitoring. Users should exempt the app from battery optimization for best results.
- **Bluetooth Range**: Detection range depends on Bluetooth signal strength and environmental factors.
- **Hotspot Compatibility**: LocalOnlyHotspot works on most devices running Android 8.0+. Some manufacturers may have additional restrictions.

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