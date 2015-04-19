package com.android.slyce.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
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

    public static int uploadBitmapToSlyce(Bitmap bitmap, String requestUrl) {

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
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
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

        } catch (MalformedURLException e) {
            SlyceLog.e(TAG, "Upload file to server error: " + e.getMessage());
        } catch (Exception e) {
            SlyceLog.e(TAG, "Upload file to server Exception : " + e.getMessage());
        }

        return serverResponseCode;
    }

    public static JSONObject uploadBitmapToMS(String serverUrl, Bitmap bitmap) {

        JSONObject response = null;

        int serverResponseCode;

        HttpURLConnection conn;
        DataOutputStream dos;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;

        try {

            // open a URL connection to the Servlet
            URL url = new URL(serverUrl);

            // Open a HTTP  connection to  the URL
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true); // Allow Inputs
            conn.setDoOutput(true); // Allow Outputs
            conn.setUseCaches(false); // Don't use a Cached Copy
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("ENCTYPE", "multipart/form-data");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            String digest = "Digest username=\"3jygvjimebpivrohfxyf\", realm=\"Moodstocks API\", nonce=\"MTQyOTQzNDY2OCBlMWFlZTg1Y2Y4NjM2ODgxYWEzOTQxODExYjc0NmI2NA==\", uri=\"/v2/search\", response=\"36b171db79e93cd5ffa2fdbee85481a0\", opaque=\"b1a8d1044b0de768f7905b15aa7f95de\", qop=auth, nc=00000001, cnonce=\"8712f44508f59394\"";
            conn.setRequestProperty("Authorization", digest);

            dos = new DataOutputStream(conn.getOutputStream());

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"image_file\"; filename=\"ms_image.jpg\"\n" + lineEnd);
            dos.writeBytes(lineEnd);

            // Bitmap to input stream
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
            byte[] bitmapdata = bos.toByteArray();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bitmapdata);

            // create a buffer of maximum size
            bytesAvailable = byteArrayInputStream.available();

            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            // read file and write it into form...
            bytesRead = byteArrayInputStream.read(buffer, 0, bufferSize);

            while (bytesRead > 0) {
                dos.write(buffer, 0, bufferSize);
                bytesAvailable = byteArrayInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = byteArrayInputStream.read(buffer, 0, bufferSize);
            }

            // send multipart form data necesssary after file data...
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            // Responses from the server (code and message)
            serverResponseCode = conn.getResponseCode();
            String serverResponseMessage = conn.getResponseMessage();

            Log.i("uploadFile", "HTTP Response is : "
                    + serverResponseMessage + ": " + serverResponseCode);

            if (serverResponseCode == 200) {

                SlyceLog.i(TAG, "uploadFile File Upload Complete.");

                String result = readInputStreamToString(conn);

                response = new JSONObject(result);
            }

            //close the streams //
            byteArrayInputStream.close();
            dos.flush();
            dos.close();

        } catch (MalformedURLException e) {
            SlyceLog.e(TAG, "Upload file to server error: " + e.getMessage());
        } catch (Exception e) {
            SlyceLog.e(TAG, "Upload file to server Exception : " + e.getMessage());
        }

        return response;

    } // End else block


    private static String readInputStreamToString(HttpURLConnection connection) {
        String result = null;
        StringBuffer sb = new StringBuffer();
        BufferedInputStream bis = null;

        try {

            bis = new BufferedInputStream(connection.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(bis));
            String inputLine = "";
            while ((inputLine = br.readLine()) != null) {
                sb.append(inputLine);
            }
            result = sb.toString();
        } catch (Exception e) {
            SlyceLog.i(TAG, "Error reading InputStream");
            result = null;
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    SlyceLog.i(TAG, "Error closing InputStream");
                }
            }
        }

        return result;
    }

    public static String getDeviceID(Context context) {
        return Settings.Secure.getString(context.getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static String getOSVersion() {
        return Build.VERSION.RELEASE;
    }

    public static String getDeviceManufacturer() {
        return Build.MANUFACTURER;
    }

    public static String getDeviceModel() {
        return Build.MODEL;
    }

    public static String getHostAppName(Context context) {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo = null;
        try {

            applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);

        } catch (final PackageManager.NameNotFoundException e) {

        }

        String title = applicationInfo != null ?
                (String) packageManager.getApplicationLabel(applicationInfo) :
                "App Name Not Found";

        return title;
    }

    public static String getHostAppVersion(Context context) {

        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo = null;
        try {

            packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);

        } catch (final PackageManager.NameNotFoundException e) {

        }

        return packageInfo.versionName;
    }

    public static String getTimestamp() {

        SimpleDateFormat noMilliSecondsFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = noMilliSecondsFormatter.format(System.currentTimeMillis());

        return time;
    }

    public static String getAndroidID(Context context) {
        String android_id = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        return android_id;
    }

    public static String getDeviceType() {

        String deviceType;

        deviceType = Devices.getDeviceName(Build.DEVICE);

        if (TextUtils.isEmpty(deviceType)) {
            deviceType = "No Device Type";
        }

        return deviceType;
    }
}


