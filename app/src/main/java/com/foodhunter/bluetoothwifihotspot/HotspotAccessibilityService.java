package com.foodhunter.bluetoothwifihotspot;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class HotspotAccessibilityService extends AccessibilityService {

    private static final String TAG = "HotspotAccessibilityService";
    private static HotspotAccessibilityService instance;
    private boolean shouldEnableHotspot = false;
    private boolean shouldDisableHotspot = false;

    public static HotspotAccessibilityService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG, "HotspotAccessibilityService created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        Log.d(TAG, "HotspotAccessibilityService destroyed");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            
            if (shouldEnableHotspot) {
                tryEnableHotspot();
            } else if (shouldDisableHotspot) {
                tryDisableHotspot();
            }
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Service interrupted");
    }

    public void requestEnableHotspot() {
        shouldEnableHotspot = true;
        shouldDisableHotspot = false;
        Log.d(TAG, "Hotspot enable requested");
    }

    public void requestDisableHotspot() {
        shouldEnableHotspot = false;
        shouldDisableHotspot = true;
        Log.d(TAG, "Hotspot disable requested");
    }

    private void tryEnableHotspot() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            return;
        }

        // Search for hotspot toggle
        AccessibilityNodeInfo hotspotNode = findNodeByText(rootNode, "Hotspot");
        if (hotspotNode == null) {
            hotspotNode = findNodeByText(rootNode, "Wi-Fi hotspot");
        }
        if (hotspotNode == null) {
            hotspotNode = findNodeByText(rootNode, "Portable hotspot");
        }

        if (hotspotNode != null) {
            // Check if it's currently off
            if (!hotspotNode.isChecked()) {
                performClick(hotspotNode);
                shouldEnableHotspot = false;
                Log.d(TAG, "Hotspot toggle clicked to enable");
            } else {
                shouldEnableHotspot = false;
                Log.d(TAG, "Hotspot already enabled");
            }
        }

        rootNode.recycle();
    }

    private void tryDisableHotspot() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            return;
        }

        // Search for hotspot toggle
        AccessibilityNodeInfo hotspotNode = findNodeByText(rootNode, "Hotspot");
        if (hotspotNode == null) {
            hotspotNode = findNodeByText(rootNode, "Wi-Fi hotspot");
        }
        if (hotspotNode == null) {
            hotspotNode = findNodeByText(rootNode, "Portable hotspot");
        }

        if (hotspotNode != null) {
            // Check if it's currently on
            if (hotspotNode.isChecked()) {
                performClick(hotspotNode);
                shouldDisableHotspot = false;
                Log.d(TAG, "Hotspot toggle clicked to disable");
            } else {
                shouldDisableHotspot = false;
                Log.d(TAG, "Hotspot already disabled");
            }
        }

        rootNode.recycle();
    }

    private AccessibilityNodeInfo findNodeByText(AccessibilityNodeInfo node, String text) {
        if (node == null) {
            return null;
        }

        List<AccessibilityNodeInfo> nodes = node.findAccessibilityNodeInfosByText(text);
        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo n : nodes) {
                if (n.isClickable() || n.isCheckable()) {
                    return n;
                }
            }
        }

        return null;
    }

    private void performClick(AccessibilityNodeInfo node) {
        if (node != null) {
            if (node.isClickable()) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            } else {
                // Try to click parent
                AccessibilityNodeInfo parent = node.getParent();
                if (parent != null && parent.isClickable()) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    parent.recycle();
                } else {
                    // Try gesture click
                    Rect bounds = new Rect();
                    node.getBoundsInScreen(bounds);
                    performGestureClick(bounds.centerX(), bounds.centerY());
                }
            }
        }
    }

    private void performGestureClick(int x, int y) {
        Path clickPath = new Path();
        clickPath.moveTo(x, y);
        
        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        GestureDescription.StrokeDescription strokeDescription = 
            new GestureDescription.StrokeDescription(clickPath, 0, 100);
        gestureBuilder.addStroke(strokeDescription);
        
        dispatchGesture(gestureBuilder.build(), null, null);
        Log.d(TAG, "Performed gesture click at (" + x + ", " + y + ")");
    }
}
