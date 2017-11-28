package com.yunthink.supercleanmaster.service;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.text.format.Formatter;
import android.widget.Toast;

import com.yunthink.supercleanmaster.R;
import com.yunthink.supercleanmaster.bean.AppProcessInfo;
import com.yunthink.supercleanmaster.utils.ProcessManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class CoreService extends Service {

    public static final String ACTION_CLEAN_AND_EXIT = "com.yzy.service.cleaner.CLEAN_AND_EXIT";

    private static final String TAG = "CleanerService";


    private OnPeocessActionListener mOnActionListener;
    private boolean mIsScanning = false;
    private boolean mIsCleaning = false;

    ActivityManager activityManager = null;
    List<AppProcessInfo> list = null;
    PackageManager packageManager = null;
    Context mContext;
    private long mCacheSize = 0;

    public static interface OnPeocessActionListener {
        public void onScanStarted(Context context);

        public void onScanProgressUpdated(Context context, int current, int max,long mNub);

        public void onScanCompleted(Context context, List<AppProcessInfo> apps);

        public void onCleanStarted(Context context);

        public void onCleanCompleted(Context context, long cacheSize);
    }

    public class ProcessServiceBinder extends Binder {

        public CoreService getService() {
            return CoreService.this;
        }
    }

    private ProcessServiceBinder mBinder = new ProcessServiceBinder();


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        mContext = getApplicationContext();

        try {
            activityManager = (ActivityManager)
                    getSystemService(Context.ACTIVITY_SERVICE);
            packageManager = getApplicationContext()
                    .getPackageManager();
        } catch (Exception e) {

        }


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();

        if (action != null) {
            if (action.equals(ACTION_CLEAN_AND_EXIT)) {
                setOnActionListener(new OnPeocessActionListener() {
                    @Override
                    public void onScanStarted(Context context) {

                    }

                    @Override
                    public void onScanProgressUpdated(Context context, int current, int max,long mNub) {

                    }

                    @Override
                    public void onScanCompleted(Context context, List<AppProcessInfo> apps) {
                        //   if (getCacheSize() > 0) {
                        //     cleanCache();
                        // }
                    }

                    @Override
                    public void onCleanStarted(Context context) {

                    }

                    @Override
                    public void onCleanCompleted(Context context, long cacheSize) {
                        String msg = getString(R.string.cleaned, Formatter.formatShortFileSize(
                                CoreService.this, cacheSize));


//                        Toast.makeText(CoreService.this, msg, Toast.LENGTH_LONG).show();

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                stopSelf();
                            }
                        }, 5000);
                    }
                });

                scanRunProcess();
            }
        }

        return START_NOT_STICKY;
    }


    private class TaskScan extends AsyncTask<Void, Integer, List<AppProcessInfo>> {

        private int mAppCount = 0;

        @Override
        protected void onPreExecute() {
            if (mOnActionListener != null) {
                mOnActionListener.onScanStarted(CoreService.this);
            }
        }

        @Override
        protected List<AppProcessInfo> doInBackground(Void... params) {
            mCacheSize = 0;

            list = new ArrayList<AppProcessInfo>();
            ApplicationInfo appInfo = null;
            AppProcessInfo abAppProcessInfo = null;
//        判断sdk版本，4.4以下
            String sdkInt = android.os.Build.VERSION.SDK ;

            int sdk_bb = Integer.parseInt(sdkInt);

            if(sdk_bb <= 19){
                List<ActivityManager.RunningAppProcessInfo> appProcessList = activityManager
                        .getRunningAppProcesses();
                publishProgress(0, appProcessList.size());
                for (ActivityManager.RunningAppProcessInfo appProcessInfo : appProcessList) {
                    publishProgress(++mAppCount, appProcessList.size());
                    abAppProcessInfo = new AppProcessInfo(
                            appProcessInfo.processName, appProcessInfo.pid,
                            appProcessInfo.uid);
                    try {
                        appInfo = packageManager.getApplicationInfo(appProcessInfo.processName, 0);


                        if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                            abAppProcessInfo.isSystem = true;
                        } else {
                            abAppProcessInfo.isSystem = false;
                        }
                        Drawable icon = appInfo.loadIcon(packageManager);
                        String appName = appInfo.loadLabel(packageManager)
                                .toString();
                        abAppProcessInfo.icon = icon;
                        abAppProcessInfo.appName = appName;
                    } catch (PackageManager.NameNotFoundException e) {
                        // :服务的命名
                        if (appProcessInfo.processName.indexOf(":") != -1) {
                            appInfo = getApplicationInfo(appProcessInfo.processName.split(":")[0]);
                            if (appInfo != null) {
                                Drawable icon = appInfo.loadIcon(packageManager);
                                abAppProcessInfo.icon = icon;
                            }else{
                                abAppProcessInfo.icon = mContext.getResources().getDrawable(R.drawable.ic_launcher);
                            }

                        }else{
                            abAppProcessInfo.icon = mContext.getResources().getDrawable(R.drawable.ic_launcher);
                        }
                        abAppProcessInfo.isSystem = true;
                        abAppProcessInfo.appName = appProcessInfo.processName;
                    }


                    long memsize = activityManager.getProcessMemoryInfo(new int[]{appProcessInfo.pid})[0].getTotalPrivateDirty() * 1024;
                    abAppProcessInfo.memory = memsize;



                    String app_name = "Booster";
                    int nub = abAppProcessInfo.appName.indexOf(app_name);

                    if (nub<0) {
                        if (!abAppProcessInfo.isSystem) {
                            mCacheSize += abAppProcessInfo.memory;
                        }
                        list.add(abAppProcessInfo);
                    }else{
                    }

                }

            }else{
                List<ProcessManager.Process> runningProcesses = ProcessManager.getRunningProcesses();
                publishProgress(0, runningProcesses.size());
                for (ProcessManager.Process appProcessInfo : runningProcesses) {
                    publishProgress(++mAppCount, runningProcesses.size());
                    abAppProcessInfo = new AppProcessInfo(
                            appProcessInfo.name, appProcessInfo.pid,
                            appProcessInfo.uid);
                    try {
                        appInfo = packageManager.getApplicationInfo(appProcessInfo.name, 0);


                        if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                            abAppProcessInfo.isSystem = true;
                        } else {
                            abAppProcessInfo.isSystem = false;
                        }
                        Drawable icon = appInfo.loadIcon(packageManager);
                        String appName = appInfo.loadLabel(packageManager)
                                .toString();
                        abAppProcessInfo.icon = icon;
                        abAppProcessInfo.appName = appName;
                    } catch (PackageManager.NameNotFoundException e) {
                        //   e.printStackTrace();

                        // :服务的命名

                        if (appProcessInfo.name.indexOf(":") != -1) {
                            appInfo = getApplicationInfo(appProcessInfo.name.split(":")[0]);
                            if (appInfo != null) {
                                Drawable icon = appInfo.loadIcon(packageManager);
                                abAppProcessInfo.icon = icon;
                            }else{
                                abAppProcessInfo.icon = mContext.getResources().getDrawable(R.drawable.ic_launcher);
                            }

                        }else{
                            abAppProcessInfo.icon = mContext.getResources().getDrawable(R.drawable.ic_launcher);
                        }
                        abAppProcessInfo.isSystem = true;
                        abAppProcessInfo.appName = appProcessInfo.name;
                    }


                    long memsize = activityManager.getProcessMemoryInfo(new int[]{appProcessInfo.pid})[0].getTotalPrivateDirty() * 1024;
                    abAppProcessInfo.memory = memsize;

                    String app_name = "Booster";
                    int nub = abAppProcessInfo.appName.indexOf(app_name);


                    if (nub<0) {
                        if (!abAppProcessInfo.isSystem) {
                            mCacheSize += abAppProcessInfo.memory;
                        }
                        list.add(abAppProcessInfo);
                    }else{
                    }
                }
            }
//
            return list;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (mOnActionListener != null) {
                mOnActionListener.onScanProgressUpdated(CoreService.this, values[0], values[1],mCacheSize);
            }
        }

        @Override
        protected void onPostExecute(List<AppProcessInfo> result) {
//            通过此处，传递应用程序信息到手机加速界面的listview
            mCacheSize = 0;
            if (mOnActionListener != null) {
                mOnActionListener.onScanCompleted(CoreService.this, result);
            }

            mIsScanning = false;
        }
    }

    public void scanRunProcess() {
        // mIsScanning = true;

        new TaskScan().execute();
    }


    public void killBackgroundProcesses(String processName) {
        // mIsScanning = true;

        String packageName = null;
        try {
            if (processName.indexOf(":") == -1) {
                packageName = processName;
            } else {
                packageName = processName.split(":")[0];
            }
            activityManager.killBackgroundProcesses(packageName);
            //
            Method forceStopPackage = activityManager.getClass()
                    .getDeclaredMethod("forceStopPackage", String.class);
            forceStopPackage.setAccessible(true);
            forceStopPackage.invoke(activityManager, packageName);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private class TaskClean extends AsyncTask<Void, Void, Long> {

        @Override
        protected void onPreExecute() {
            if (mOnActionListener != null) {
                mOnActionListener.onCleanStarted(CoreService.this);
            }
        }

        @Override
        protected Long doInBackground(Void... params) {
            long beforeMemory = 0;
            long endMemory = 0;
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            beforeMemory = memoryInfo.availMem
            ;

            List<ActivityManager.RunningAppProcessInfo> appProcessList = activityManager
                    .getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo info : appProcessList) {
                killBackgroundProcesses(info.processName);
            }
            activityManager.getMemoryInfo(memoryInfo);
            endMemory = memoryInfo.availMem;
            return endMemory - beforeMemory;
        }

        @Override
        protected void onPostExecute(Long result) {


            if (mOnActionListener != null) {
                mOnActionListener.onCleanCompleted(CoreService.this, result);
            }


        }
    }


    public long getAvailMemory(Context context) {
        // 获取android当前可用内存大小
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        // 当前系统可用内存 ,将获得的内存大小规格化

        return memoryInfo.availMem;
    }

    public void cleanAllProcess() {
        //  mIsCleaning = true;

        new TaskClean().execute();
    }

    public void setOnActionListener(OnPeocessActionListener listener) {
        mOnActionListener = listener;
    }

    public ApplicationInfo getApplicationInfo( String processName) {
        if (processName == null) {
            return null;
        }
        List<ApplicationInfo> appList = packageManager
                .getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
        for (ApplicationInfo appInfo : appList) {
            if (processName.equals(appInfo.processName)) {
                return appInfo;
            }
        }
        return null;
    }

    public boolean isScanning() {
        return mIsScanning;
    }

    public boolean isCleaning() {
        return mIsCleaning;
    }


}
