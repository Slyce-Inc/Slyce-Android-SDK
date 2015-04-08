package com.android.slyce.utils;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by davidsvilem on 3/23/15.
 */
public class Utils {

    private static final String TAG = Utils.class.getSimpleName();

    public static String decodeBase64(String base64) {

        String result = null;

        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
        try {
            result = new String(bytes, "UTF-8");

        } catch (UnsupportedEncodingException e) {
            SlyceLog.i(TAG, "decodeBase64: UnsupportedEncodingException");
        }
        return result;
    }

    public static Bitmap scaleDown(Bitmap realImage, float maxImageSize) {

        float ratio = Math.min(
                (float) maxImageSize / realImage.getWidth(),
                (float) maxImageSize / realImage.getHeight());

        int width = Math.round((float) ratio * realImage.getWidth());
        int height = Math.round((float) ratio * realImage.getHeight());

        Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width,
                height, true);
        return newBitmap;
    }

    public static int uploadBitmap(Bitmap bitmap, String requestUrl) {

        int serverResponseCode = 0;

        HttpsURLConnection conn = null;
        DataOutputStream dos = null;

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;

        if (bitmap == null) {
            SlyceLog.e("uploadFile", "Bitmap Does not exist");
            return 0;
        }

        // Bitmap to input stream
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0 /* ignored for PNG */, bos);
        byte[] bitmapData = bos.toByteArray();
        ByteArrayInputStream bs = new ByteArrayInputStream(bitmapData);

        try { // open a URL connection to the Servlet

            URL url = new URL(requestUrl);
            conn = (HttpsURLConnection) url.openConnection(); // Open a HTTP

            // connection to
            // the URL
            conn.setDoOutput(true); // Allow Outputs
            conn.setUseCaches(false); // Don't use a Cached Copy
            conn.setRequestMethod("PUT");

            conn.setRequestProperty("Content-Type", "image/jpeg");

            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            dos = new DataOutputStream(conn.getOutputStream());

            bytesAvailable = bs.available(); // create a buffer of maximum size

            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            // read file and write it into form...
            bytesRead = bs.read(buffer, 0, bufferSize);

            while (bytesRead > 0) {
                dos.write(buffer, 0, bufferSize);
                bytesAvailable = bs.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = bs.read(buffer, 0, bufferSize);
            }

            // Responses from the server (code and message)
            serverResponseCode = conn.getResponseCode();
            String serverResponseMessage = conn.getResponseMessage();

            SlyceLog.i(TAG, "uploadFile HTTP Response is : "
                    + serverResponseMessage + ": " + serverResponseCode);

            if (serverResponseCode == 200) {
                SlyceLog.i(TAG, "uploadFile File Upload Complete.");
            }

            // close the streams //
            bs.close();
            dos.flush();
            dos.close();

        } catch (MalformedURLException ex) {
            SlyceLog.e(TAG, "Upload file to server error: " + ex.getMessage());
        } catch (Exception e) {
            SlyceLog.e(TAG, "Upload file to server Exception : " + e.getMessage());
        }

        return serverResponseCode;
    }

    public static String getDeviceID(Context context){
        return Settings.Secure.getString(context.getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static String getOSVersion(){
        return Build.VERSION.RELEASE;
    }

    public static String getDeviceManufacturer(){
        return Build.MANUFACTURER;
    }

    public static String getDeviceModel(){
        return Build.MODEL;
    }

    public static String getHostAppName(Context context){
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo = null;
        try {

            applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);

        } catch (final PackageManager.NameNotFoundException e) {

        }

        String title = applicationInfo != null ?
                (String)packageManager.getApplicationLabel(applicationInfo) :
                "App Name Not Found";

        return title;
    }

    public static String getHostAppVersion(Context context){

        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo = null;
        try {

            packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);

        } catch (final PackageManager.NameNotFoundException e) {

        }

        return  packageInfo.versionName;
    }

    public static String getTimestamp(){

        SimpleDateFormat noMilliSecondsFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = noMilliSecondsFormatter.format(System.currentTimeMillis());

        return time;
    }

    public static String getAndroidID(Context context){
        String android_id = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        return android_id;
    }

    public static String getDeviceType(){

        String deviceType;

        deviceType = Devices.getDeviceName(Build.DEVICE);

        if(TextUtils.isEmpty(deviceType)){
            deviceType = "No Device Type";
        }

        return deviceType;
    }

//    public static int upload(Bitmap bitmap, String uploadUrl){
//
//        List<String> response = new ArrayList<String>();
//
//        int serverResponseCode = -1;
//        String serverResponseMessage;
//
//        URL url = null;
//        try {
//            url = new URL(uploadUrl);
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        }
//        HttpsURLConnection connection = null;
//        try {
//            connection = (HttpsURLConnection) url.openConnection();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        connection.setDoOutput(true);
//        connection.setRequestProperty("Content-Type", "image/jpeg");
//
//        try {
//            connection.setRequestMethod("PUT");
//        } catch (ProtocolException e) {
//            e.printStackTrace();
//        }
//
//        OutputStream outputStream = null;
//
//        try {
//            outputStream = connection.getOutputStream();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        DataOutputStream dos = new DataOutputStream(outputStream);
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, dos);
//
//        try {
//
//            serverResponseCode = connection.getResponseCode();
//            serverResponseMessage = connection.getResponseMessage();
//
//            if (serverResponseCode == HttpURLConnection.HTTP_OK) {
//                BufferedReader reader = new BufferedReader(new InputStreamReader(
//                        connection.getInputStream()));
//                String line = null;
//                while ((line = reader.readLine()) != null) {
//                    response.add(line);
//                }
//                reader.close();
//                connection.disconnect();
//            } else {
//                throw new IOException("Server returned non-OK status: " + serverResponseCode + " : " + serverResponseMessage);
//            }
//
//            dos.close();
//            outputStream.close();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return serverResponseCode;
//    }
}
