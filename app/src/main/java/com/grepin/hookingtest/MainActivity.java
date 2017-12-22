package com.grepin.hookingtest;

import android.app.ActivityManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.grepin.hookingtest.common.CheckSecure;
import com.grepin.hookingtest.common.Consts;
import com.grepin.hookingtest.receivers.UsbReceiver;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dalvik.system.DexFile;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    /** layout */
    Button btn_chk_stack_trace;
    Button btn_chk_process;
    Button btn_chk_usb_conn;
    Button btn_chk_native_method;

    TextView tv_msg;
    TextView tv_thread_log;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layout_init();
/*
        checkProcesses();
//        showMessage(checkDebuggingMode());
        showMessage(checkUSBconnect());
        checkStackTrace();
*/
/*

        UsbReceiver usbReceiver = new UsbReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver, filter);
*/

        CheckThread chkThread = new CheckThread();
        chkThread.start();
    }


    private void layout_init() {
        btn_chk_stack_trace = (Button) findViewById(R.id.btn_chk_stack_trace);
        btn_chk_process = (Button) findViewById(R.id.btn_chk_process);
        btn_chk_usb_conn = (Button) findViewById(R.id.btn_chk_usb_conn);
        btn_chk_native_method = (Button) findViewById(R.id.btn_chk_native_method);

        btn_chk_stack_trace.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkStackTrace();
            }
        });

        btn_chk_process.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkProcesses();
            }
        });

        btn_chk_usb_conn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkUSBconnect();
            }
        });

        btn_chk_native_method.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkNativeMethod();
            }
        });

        tv_msg = (TextView) findViewById(R.id.tv_msg);
        tv_msg.setMovementMethod(new ScrollingMovementMethod());

        tv_thread_log = (TextView) findViewById(R.id.tv_thread_log);
    }


    private void showMessage(String msg) {
//        Log.w(TAG, "showMessage(" + String.valueOf(msg) + ")");
        tv_msg.setText(msg);
    }


    /**
     * USB 디버깅 여부 체크
     */
    private boolean checkDebuggingMode() {
        CheckSecure secure = new CheckSecure();
        boolean isDebugginMode = secure.isDebuggingMode(this);

        Log.w(TAG, "checkDebuggingMode() " + isDebugginMode);
        showMessage("checkDebuggingMode() " + isDebugginMode);

        return isDebugginMode;
    }


    /**
     * USB 연결 여부 체크
     */
    private boolean checkUSBconnect() {
        boolean connected = false;
        Intent intent = this.registerReceiver(null, new IntentFilter("android.hardware.usb.action.USB_STATE"));
        connected = intent.getExtras().getBoolean("connected");

        Log.w(TAG, "checkUSBconnect() " + connected);
        showMessage("checkUSBconnect() " + connected);
        return connected;
    }


    /**
     * 프로세스 체크
     */
    private void checkProcesses() {
        ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();

        // TEST
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<procInfos.size(); i++) {
            ActivityManager.RunningAppProcessInfo proc = procInfos.get(i);
            Log.w(TAG, "proc[" + i + "] " + proc.processName);
            sb.append("proc[" + i + "] " + proc.processName + "\n");
        }
        showMessage(sb.toString());
    }

    /**
     * http://d3adend.org/blog/?p=589
     */
    private void checkStackTrace() {
        Log.w(TAG, "checkStackTrace()");
        StringBuilder sb = new StringBuilder();
        try {
            throw new Exception("checkStackTrace() Throw Exception.");
        } catch (Exception e) {
            for(StackTraceElement stackTrace : e.getStackTrace()) {
                Log.w(TAG, stackTrace.getClassName() + " > " + stackTrace.getMethodName());
                sb.append(stackTrace.getClassName() + " > " + stackTrace.getMethodName() + "\n");
            }
        } finally {
            showMessage(sb.toString());
        }
    }


    /**
     * http://d3adend.org/blog/?p=589
     * Stab 3: Check for native methods that shouldn’t be native.
     * 루팅 시 변경되는 네이티브 메소드 체크.
     */
    private void checkNativeMethod() {
        Log.w(TAG, "checkNativeMethod()");

        boolean isHooked = false;
        String msg = "";

        PackageManager pm = getApplicationContext().getPackageManager();
        List<ApplicationInfo> applicationInfoList = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo applicationInfo : applicationInfoList) {
            if (applicationInfo.processName.equals("com.grepin.hookingtest")) {
                Set<String> classes = new HashSet();
                DexFile dex;
                try {
                    dex = new DexFile(applicationInfo.sourceDir);
                    Enumeration entries = dex.entries();
                    while(entries.hasMoreElements()) {
                        String entry = entries.nextElement().toString();
                        classes.add(entry);
                    }
                    dex.close();
                }
                catch (IOException e) {
                    msg = "후킹되었다!!!\n" + e.toString();
                    isHooked = true;
                    Log.e(TAG, msg);
                }
                for(String className : classes) {
                    if(className.startsWith("com.grepin.hookingtest")) {
                        try {
                            Class clazz = MainActivity.class.forName(className);
                            for(Method method : clazz.getDeclaredMethods()) {
                                if(Modifier.isNative(method.getModifiers())) {
                                    msg = "후킹되었다!!!\nNative 함수발견 :\n" + clazz.getCanonicalName() + " -> " + method.getName();
                                    isHooked = true;
                                    Log.wtf(TAG, msg);
                                }
                            }
                        }
                        catch(ClassNotFoundException e) {
                            msg = "후킹되었다!!!\n" + e.toString();
                            isHooked = true;
                            Log.wtf(TAG, msg);
                        }
                    }
                }
                break;
            }
        }
//        tv_thread_log.setText(msg);
        showMessage(msg);

        if(isHooked) {
            Log.wtf(TAG, "Hooking Detected.");
//            Toast.makeText(getApplicationContext(), "후킹감지 : 강제종료", Toast.LENGTH_SHORT);
//            System.exit(0);
        }
    }


    private class CheckThread extends Thread {
        private boolean runningState = true;
        @Override
        public void run() {
            super.run();
            while(runningState) {
                try {
                    Thread.sleep(1000);
                    Message msg = handler.obtainMessage();
                    handler.sendMessage(msg);

                    Log.w(TAG, "Consts.USB_CONNECTION : " + Consts.USB_CONNECTION);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
//            checkNativeMethod();
            tv_thread_log.setText("USB_CONNECTION : " + Consts.USB_CONNECTION);
            return true;
        }
    });
}
