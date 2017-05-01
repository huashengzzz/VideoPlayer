package com.vientiane.asset;

import android.app.ActivityManager;
import android.app.Application;
import android.text.TextUtils;

import java.util.Iterator;
import java.util.List;

/**
 * Created by Gs on 2016/8/23.
 */
public class VientianeApplication extends Application {
    private static VientianeApplication instance;

    public static VientianeApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

          int pid = android.os.Process.myPid();
        String curProcessName = getAppName(pid);
        if (TextUtils.isEmpty(curProcessName) || !curProcessName.equals(getPackageName())) {
            return;
        }
        instance = this;

    }

    private String getAppName(int pID) {
        String processName = null;
        ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List l = am.getRunningAppProcesses();
        Iterator i = l.iterator();
        while (i.hasNext()) {
            ActivityManager.RunningAppProcessInfo info = (ActivityManager.RunningAppProcessInfo) (i.next());
            try {
                if (info.pid == pID) {
                    processName = info.processName;
                    return processName;
                }
            } catch (Exception e) {
                // Log.d("Process", "Error>> :"+ e.toString());
            }
        }
        return null;
    }



}
