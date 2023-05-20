package mod.accessibility.service;


import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Path;
import android.os.Handler;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.List;

public class Accessibility extends AccessibilityService {
    boolean splitIsActive = false;

    private GestureResultCallback callback;
    private BroadcastReceiver receiver;

    @Override
    public void onServiceConnected() {
        AccessibilityServiceInfo info = getServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_ANNOUNCEMENT;
        info.notificationTimeout = 100;
        setServiceInfo(info);
        callback = new AccessibilityService.GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
            }
        };
        receiver();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if (event.getEventType() == AccessibilityEvent.TYPE_ANNOUNCEMENT) {
            String text = String.valueOf(event.getText());
            if (text.length() > 2) {
                text = text.substring(1, text.length() - 1);
                if (text.startsWith("SplitScreenOff")) {
                    closeSplitScreen();
                } else if (text.startsWith("Click")) {
                    text = text.substring(text.indexOf(":") + 1);
                    float x = Float.parseFloat(text.substring(0, text.indexOf(":")));
                    text = text.substring(text.indexOf(":") + 1);
                    float y = Float.parseFloat(text);
                    click(x, y);
                } else if (text.startsWith("SplitScreenOn")) {

                    text = text.substring(text.indexOf(":") + 1);
                    String app1 = text.substring(0, text.indexOf(":"));
                    text = text.substring(text.indexOf(":") + 1);
                    String app2 = text.substring(0, text.indexOf(":"));
                    text = text.substring(text.indexOf(":") + 1);
                    int delay = Integer.parseInt(text);
                    startSplitScreenApps(app1,app2,delay);

                }
            }
        }
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public boolean onUnbind(Intent intent) {
        if(receiver!=null) {
            this.unregisterReceiver(receiver);
        }
        return false;
    }



    private void startSplitScreenApps(String app1, String app2, int delay) {

        if (app1.length() > 0 & app2.length() > 0 & !splitIsActive) {

            splitIsActive = true;
            Intent splitOne = getPackageManager().getLaunchIntentForPackage(app1);
            splitOne.addCategory(Intent.CATEGORY_LAUNCHER);

            splitOne.setFlags(
                    Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT |
                            Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            Intent splitTwo = getPackageManager().getLaunchIntentForPackage(app2);
            splitTwo.addCategory(Intent.CATEGORY_LAUNCHER);
            splitTwo.setFlags(
                    Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT |
                            Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);
            new Handler().postDelayed(() -> {
                startActivity(splitOne);
                new
                        Handler().

                        postDelayed(() -> {
                            startActivity(splitTwo);
                            splitIsActive = false;
                        }, delay);
            }, delay);

        }
    }

    private void closeSplitScreen() {
        if (inSplitScreenMode(getWindows()) & !splitIsActive) {
            performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);
        }
    }

    private boolean inSplitScreenMode(List<AccessibilityWindowInfo> windows) {
        for (AccessibilityWindowInfo window : windows) {
            if (window.getType() == AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER) {
                return true;
            }
        }
        return false;
    }

    private GestureDescription createClick(float x, float y) {
        final int DURATION = 1;
        Path clickPath = new Path();
        clickPath.moveTo(x, y);
        GestureDescription.StrokeDescription clickStroke =
                new GestureDescription.StrokeDescription(clickPath, 0, DURATION);
        GestureDescription.Builder clickBuilder = new GestureDescription.Builder();
        clickBuilder.addStroke(clickStroke);
        return clickBuilder.build();
    }

    public void click(float x, float y) {
        dispatchGesture(createClick(x, y), callback, null);
    }


    public void receiver(){
        IntentFilter intentFilter=new IntentFilter("MyAccessibilityIsWorking?");
         receiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                answer();
            }
        };
        receiver.goAsync();
        this.registerReceiver(receiver,intentFilter);
    }
    public void answer(){
        Intent intent=new Intent("AccessibilityIsWorking");
        this.sendBroadcast(intent);
    }

}