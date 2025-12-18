# Bluetooth WiFi Hotspot Android App

An Android 16 application that automatically enables WiFi hotspot when a selected Bluetooth device comes into range.

## Features

- **Bluetooth Device Scanning**: Scan and discover nearby Bluetooth devices
- **Target Device Selection**: Select a target Bluetooth device from the list
- **Automatic Hotspot Control**: Automatically enables WiFi hotspot when the target device is in range
- **Background Monitoring**: Runs as a foreground service to continuously monitor Bluetooth device proximity
- **Android 16 Optimized**: Designed specifically for Android 16 (API 35) with all necessary permissions

## Permissions

The app requests comprehensive permissions for full automation on Android 16:

### Bluetooth Permissions
- `BLUETOOTH_SCAN` - Scan for Bluetooth devices
- `BLUETOOTH_CONNECT` - Connect to Bluetooth devices
- `BLUETOOTH_ADVERTISE` - Advertise Bluetooth services

### Location Permissions
- `ACCESS_FINE_LOCATION` - Required for Bluetooth scanning
- `ACCESS_COARSE_LOCATION` - Coarse location access
- `ACCESS_BACKGROUND_LOCATION` - Background location for continuous monitoring

### WiFi & Network Permissions
- `ACCESS_WIFI_STATE` - Check WiFi state
- `CHANGE_WIFI_STATE` - Modify WiFi state
- `CHANGE_NETWORK_STATE` - Modify network state
- `ACCESS_NETWORK_STATE` - Access network state

### System-Level Permissions (Invasive)
- `WRITE_SETTINGS` - Modify system settings
- `WRITE_SECURE_SETTINGS` - Modify secure system settings
- `NETWORK_SETTINGS` - Access network settings
- `TETHER_PRIVILEGED` - Privileged tethering control
- `CONNECTIVITY_USE_RESTRICTED_NETWORKS` - Use restricted networks
- `CONNECTIVITY_INTERNAL` - Internal connectivity APIs

### Display Permissions
- `SYSTEM_ALERT_WINDOW` - Display over other apps
- `SYSTEM_OVERLAY_WINDOW` - System-level overlay window

### Accessibility
- `BIND_ACCESSIBILITY_SERVICE` - Accessibility service for automated interactions

### Service Permissions
- `FOREGROUND_SERVICE` - Run foreground service
- `FOREGROUND_SERVICE_CONNECTED_DEVICE` - Connected device foreground service
- `FOREGROUND_SERVICE_SPECIAL_USE` - Special use foreground service
- `POST_NOTIFICATIONS` - Post notifications
- `WAKE_LOCK` - Keep device awake
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` - Ignore battery optimization

## How to Use

1. **Launch the App**: Open the Bluetooth WiFi Hotspot app
2. **Grant Permissions**: Allow all required permissions when prompted:
   - Bluetooth, Location, Notifications (standard permissions)
   - Display over other apps (special permission)
   - Modify system settings (special permission)
   - Enable Accessibility Service (manual setup)
3. **Scan for Devices**: Tap the "Scan Bluetooth Devices" button
4. **Select Target Device**: Tap on a device from the list to set it as the target
5. **Automatic Monitoring**: The app will now monitor for the target device and automatically enable WiFi hotspot when in range

## WiFi Hotspot Automation Strategies

The app uses multiple strategies to automate WiFi hotspot control on Android 16:

### Strategy 1: TetheringManager API (Preferred)
- Uses reflection to access hidden TetheringManager APIs
- Requires system-level permissions (`TETHER_PRIVILEGED`, `NETWORK_SETTINGS`)
- Provides full programmatic control

### Strategy 2: Shell Commands
- Executes shell commands with `WRITE_SECURE_SETTINGS` permission
- Modifies system settings directly
- Works on many devices

### Strategy 3: Accessibility Service (Fallback)
- Uses accessibility service to automate UI interactions
- Navigates to settings and toggles hotspot
- Works when other methods fail
- Requires user to enable accessibility service

### Strategy 4: LocalOnlyHotspot API
- Creates a temporary local hotspot
- Fully automated, no special permissions required
- Limited to local subnet devices

### Strategy 5: Settings Intent
- Opens hotspot settings for manual control
- Last resort when automation isn't possible

## Technical Details

### Components

- **MainActivity**: Main UI for scanning and selecting Bluetooth devices, handles permission requests
- **BluetoothMonitorService**: Foreground service that monitors target device proximity
- **WifiHotspotManager**: Manages WiFi hotspot with multiple automation strategies
- **HotspotAccessibilityService**: Accessibility service for automated UI interactions

### Building the App

Requirements:
- Android SDK with API level 35 (Android 16)
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
│   │   ├── WifiHotspotManager.java
│   │   └── HotspotAccessibilityService.java
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml
│   │   │   └── device_item.xml
│   │   ├── values/
│   │   │   └── strings.xml
│   │   ├── xml/
│   │   │   └── accessibility_service_config.xml
│   │   └── mipmap-*/
│   └── AndroidManifest.xml
└── build.gradle
```

## Requirements

- **Android 16 (API 35)**: This app is specifically designed for Android 16
- **System Permissions**: Some features require system-level permissions that need manual setup
- **Accessibility Service**: Must be manually enabled in Settings > Accessibility for full automation
- **Battery Optimization**: App should be exempted from battery optimization

## Limitations and Notes

- **System Permissions**: Some invasive permissions like `WRITE_SECURE_SETTINGS` and `TETHER_PRIVILEGED` are protected and may require ADB commands or system app installation
- **Manufacturer Variations**: Different device manufacturers may have custom hotspot implementations
- **Root Access**: Not required, but would enable even more control options
- **Accessibility Service**: Must be manually enabled by the user for UI automation to work

## ADB Setup for System Permissions (Optional)

To grant system-level permissions via ADB:

```bash
# Grant WRITE_SECURE_SETTINGS
adb shell pm grant com.foodhunter.bluetoothwifihotspot android.permission.WRITE_SECURE_SETTINGS

# Grant WRITE_SETTINGS  
adb shell appops set com.foodhunter.bluetoothwifihotspot WRITE_SETTINGS allow
```

## License

This project is open source.