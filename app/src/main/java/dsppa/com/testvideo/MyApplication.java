package dsppa.com.testvideo;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.SaveListener;

/**
 * Created by cxg on 2017/8/31.
 */

public class MyApplication extends Application implements Thread.UncaughtExceptionHandler{

    private static final String TAG = "MyApplication";
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;
    private Map<String,String> exceptionInfos = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();

        initExceptionHandler();
    }

    private void initExceptionHandler() {
        uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (uncaughtExceptionHandler != null && !catchCrashException(e)){
            uncaughtExceptionHandler.uncaughtException(t,e);
        }else {
            exitApp();
        }
    }

    private void exitApp() {
        Log.e(TAG,"----------exitApp------------");
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(getApplicationContext(),"哎呀，程序中断了",Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
        }).start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //让应用退出
        android.os.Process.killProcess(Process.myPid());

    }

    private boolean catchCrashException(Throwable e) {
        if (e == null){
            return false;
        }

        //收集信息
        collectInfos(e);

        //保存到本地
        saveCrashInfos();

        //上传到服务器Bmob
        uploadCrashInfos();

        return true;
    }

    private void uploadCrashInfos() {
        CrashInfo crashInfo = new CrashInfo();
        crashInfo.setSysVersion(exceptionInfos.get("sysVerision"));
        crashInfo.setPackageInfo(exceptionInfos.get("packageInfo"));
        crashInfo.setExceptionInfo(exceptionInfos.get("exceptionInfo"));
        crashInfo.save(new SaveListener<String>() {
            @Override
            public void done(String s, BmobException e) {
                if (e == null){
                    Log.e(TAG,"上传Crash到bmob成功："+s);
                }else {
                    Log.e(TAG,"上传Crash到bmob失败，错误码："+e.getErrorCode()+"\n错误信息："+e.getMessage());
                }
            }
        });
    }

    private void saveCrashInfos() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(exceptionInfos.get("packageInfo"));
        stringBuffer.append(exceptionInfos.get("sysVerision"));
        stringBuffer.append(exceptionInfos.get("exceptionInfo"));
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = simpleDateFormat.format(new Date());
        String fileName = "CrashLog-"+time+".log";
        String fileDir = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Log/";
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            File file = new File(fileDir);
            if (!file.exists()){
                boolean a = file.mkdir();
                if (!a){
                    return;
                }
            }

            try {
                FileOutputStream fileOutputStream = new FileOutputStream(fileDir+fileName);
                fileOutputStream.write(stringBuffer.toString().getBytes());
                fileOutputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void collectInfos(Throwable e) {
        //收集产生异常的信息
        String exceptionInfo = collectExceptionInfos(e);
        //获取应用参数包信息
        Map<String,String> packageInfo = collectPackageInfos();
        //收集系统硬件，版本信息
        String sysVersion = collectSysVersion();

        exceptionInfos.put("exceptionInfo",exceptionInfo);
        exceptionInfos.put("packageInfo",packageInfo.toString());
        exceptionInfos.put("sysVerision",sysVersion);
    }

    private String collectSysVersion() {
        StringBuffer versionInfo = new StringBuffer();
        versionInfo.append("\n\n");
        versionInfo.append("手机型号："+Build.MODEL+"\n");
        versionInfo.append("SDK版本："+ Build.VERSION.SDK_INT+"\n");
        versionInfo.append("系统版本"+Build.VERSION.RELEASE+"\n");
        versionInfo.append("手机制造商："+Build.MANUFACTURER+"\n\n");
        return versionInfo.toString();
    }


    private Map<String,String> collectPackageInfos() {
        Map<String,String> stringMap = new HashMap<>();
        PackageManager packageManager = this.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(),PackageManager.GET_ACTIVITIES);
            if (packageInfo != null){
                String versionName = packageInfo.versionName == null ? "null":packageInfo.versionName;
                String versionCode = packageInfo.versionCode+"";
                stringMap.put("VERSION_NAME",versionName);
                stringMap.put("VERSION_CODE",versionCode);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return stringMap;
    }

    private String collectExceptionInfos(Throwable e) {
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        e.printStackTrace(printWriter);
        e.printStackTrace();
        Throwable throwable = e.getCause();
        while (throwable != null){
            throwable.printStackTrace(printWriter);
            printWriter.append("\r\n");
            throwable = throwable.getCause();
        }

        printWriter.close();
        return writer.toString();

    }
}
