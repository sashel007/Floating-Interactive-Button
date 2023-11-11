package ru.ikar.floatingbutton_ikar;

import android.accessibilityservice.AccessibilityService;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;


public class MyAccessibilityService extends AccessibilityService {
    private static final String TAG = "MyAccessibilityService";
    private BroadcastReceiver myReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        myReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.myapp.ACTION_PERFORM_BACK".equals(intent.getAction())) {
                    performBackPress();
                } else if ("com.myapp.ACTION_SHOW_RECENT_APPS".equals(intent.getAction())) {
                    performShowRecentApps();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.myapp.ACTION_PERFORM_BACK");
        filter.addAction("com.myapp.ACTION_SHOW_RECENT_APPS");
        registerReceiver(myReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myReceiver); // Не забудьте отменить регистрацию приемника
    }

    public void performShowRecentApps() {
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
    }
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(TAG, "ЗАПУЩЕН: ");
    }

    @Override
    public void onInterrupt() {
        // Обработка прерываний
    }

    public void performBackPress() {
        performGlobalAction(GLOBAL_ACTION_BACK);
    }
}




