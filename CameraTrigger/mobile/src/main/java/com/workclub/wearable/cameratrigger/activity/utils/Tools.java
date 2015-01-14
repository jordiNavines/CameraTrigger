package com.workclub.wearable.cameratrigger.activity.utils;


import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Tools {



    /**
     * Check if the directory where we want to store the package is created if not we create the directory
     *
     * @param location
     */
    public static void dirChecker(String location) {
        File f = new File(location);

        if (!f.isDirectory()) {
            f.mkdirs();
        }
    }

    /**
     * Remove the directory
     *
     * @param location
     */
    public static void dirRemover(String location) {
        File f = new File(location);

        if (f!=null && f.exists()) {
            deleteFiles(f);
        }
    }

    public static void deleteFiles(File f){
        if (f!=null && f.isDirectory()) {
            String[] children = f.list();
            for (int i = 0; i < children.length; i++) {
                File f1= new File(f, children[i]);
                if (f1!=null){
                    if (f1.isDirectory()){
                        deleteFiles(f1);
                    }else{
                        f1.delete();
                    }
                }
            }
        }
        f.delete();
    }


    public static boolean isSdPresent() {
        return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
    }


    public static Calendar stringToCalendar(String strDate) throws ParseException {
        String FORMAT_DATETIME = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_DATETIME);
        Date date = sdf.parse(strDate);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }


    public static boolean isNumeric(String number) {
        boolean isValid = false;
        if (number.matches("[0-9]+")) {
            // mEditText only contains numbers
            isValid = true;
        } else {
            // mEditText contains number + text, or text only.
            isValid = false;
        }
        return isValid;
    }

    public static boolean isEmailValid(String email) {
        boolean isValid = false;

        String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
        CharSequence inputStr = email;

        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(inputStr);
        if (matcher.matches()) {
            isValid = true;
        }
        return isValid;
    }


    public static boolean isConnected(Context ctx) {
        //ConnectivityManager cm = ( ConnectivityManager ) ctx.getSystemService( Context.CONNECTIVITY_SERVICE );
        //NetworkInfo ni = cm.getActiveNetworkInfo();
        // if(ni == null) return false;
        //return ni.isConnected();
        ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isAvailable()) {
            return true;
        }
        return false;
    }


    public static boolean isWifiConnected(Context ctx) {
        ConnectivityManager connManager = (ConnectivityManager) ctx.getSystemService(ctx.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWifi.isConnected()) {
            return true;
        }
        return false;
    }


    public static void showKeyboard(final Context ctx, final EditText target) {
        new Handler().post(new Runnable() {

            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
                boolean xx = imm.showSoftInput(target, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    public static void hideKeyboard(final Context ctx, final IBinder windowToken) {
        new Handler().post(new Runnable() {

            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
                boolean xx = imm.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });
    }

    /**
     * Checks if the application is being sent in the background (i.e behind
     * another application's Activity).
     *
     * @param context the context
     * @return <code>true</code> if another application will be above this one.
     */
    public static boolean isApplicationSentToBackground(final Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;
            if (!topActivity.getPackageName().equals(context.getPackageName())) {
                return true;
            }
        }

        return false;
    }




    public static float convertDIPtoPixels(Context ctx, int value) {
        Resources r = ctx.getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, r.getDisplayMetrics());
        return px;
    }


    public static String[] transformArrayListToArray(ArrayList<String> arraylist) {
        String[] array = new String[arraylist.size()];
        array = arraylist.toArray(array);
        return array;
    }

}
