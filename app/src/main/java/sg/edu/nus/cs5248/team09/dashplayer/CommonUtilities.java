package sg.edu.nus.cs5248.team09.dashplayer;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.OkHttpClient;

/**
 * Created by Prakhar
 *
 * Common methods that are needed in the app
 */

public class CommonUtilities {

    private static OkHttpClient mOkHttpClient;

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm != null ? cm.getActiveNetworkInfo() : null;
        return (netInfo != null && netInfo.isConnected());
    }

    public static String newVideoName() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return "Video" + timeStamp;
    }

    public static String getVideoPath(String videoName) {
        return Constants.RECORD_VIDEO_FOLDER + videoName + Constants.UPLOAD_VID_EXTENSION;
    }

    public static String getSegmentName(String videoName, int segmentNumber) {
        return videoName +
                Constants.JOINER +
                "segment" +
                segmentNumber +
                Constants.UPLOAD_VID_EXTENSION;
    }

    public static String getSegmentPath(String segmentName) {
        return Constants.SEGMENT_CREATE_FOLDER + segmentName;
    }

    public static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static String getSegmentPath(String videoName, int segmentNumber) {
        return getSegmentPath(getSegmentName(videoName, segmentNumber));
    }

    public static OkHttpClient getOkHttpClient() {
        if(mOkHttpClient == null) {
            mOkHttpClient = new OkHttpClient();
        }
        return mOkHttpClient;
    }

}
