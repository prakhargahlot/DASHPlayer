package sg.edu.nus.cs5248.team09.dashplayer.recording;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sg.edu.nus.cs5248.team09.dashplayer.CommonUtilities;
import sg.edu.nus.cs5248.team09.dashplayer.Constants;

/**
 * Created by Meghana on 11-11-2017.
 *
 * Task to upload the segmented videos
 */

class UploadTask extends AsyncTask {

    private static final String TAG = "UploadTask";
    private ProgressDialog progress;
    private Context mContext;
    private SegmentationInfo mSegmentationInfo;
    private boolean mNetworkAvailable = true;

    UploadTask(Context context) {
        this.mContext = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progress = new ProgressDialog(mContext);
        progress.setTitle("Uploading to Server");
        progress.setMessage("We're uploading your video to the server. Please wait.");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setProgressNumberFormat("");
        progress.show();
    }

    @Override
    protected void onPostExecute(Object o) {
        progress.dismiss();
        if(o != null && o instanceof Integer) {
            new AlertDialog.Builder(mContext)
                    .setPositiveButton("OK", null)
                    .setMessage("Seems like network is lost. We will notify you when network resumes.")
                    .setCancelable(true)
                    .create().show();
        } else {
            new AlertDialog.Builder(mContext)
                    .setPositiveButton("OK", null)
                    .setMessage("Upload successfully completed. You may refresh the video list.")
                    .setCancelable(true)
                    .create().show();
        }
    }

    @Override
    protected void onProgressUpdate(Object[] values) {
        progress.setProgress((Integer) values[0]);
    }

    @Override
    protected Object doInBackground(Object[] params) {
        mSegmentationInfo = (SegmentationInfo) params[0];
        String videoName = mSegmentationInfo.getOriginalVideoName();

        int start = 0;
        // param[1] is resumeUpload
        if (params.length > 1 && (boolean) params[1]) {
            start = 1 + queryServerForLastAvailableSegmentNumber(mSegmentationInfo.getOriginalVideoName());
        }

        for (int i = start; i < mSegmentationInfo.size(); i++) {
            File file = new File(CommonUtilities.getSegmentPath(videoName, i));
            if(file.exists()) {
                // If there's no network, stop doing work. Return.
                if(!mNetworkAvailable) {
                    addSegmentationToPendingUploads();
                    return 1;
                }
                // Publish the progress
                publishProgress((int) ((i / (float) mSegmentationInfo.size()) * 100));
                doUpload(mSegmentationInfo.getUploadName(), i, file);
            }
        }

        if(mNetworkAvailable) {
            // Trigger playlists creation
            try {
                Response response = CommonUtilities.getOkHttpClient()
                        .newCall(new Request.Builder().url(Constants.Server.MPD_URL).build())
                        .execute();
                Log.v(TAG, "Response while calling slay.php: " + response.code());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            addSegmentationToPendingUploads();
        }
        return null;
    }

    private void doUpload(String videoName, int segmentNumber, final File fileToUpload) {

        String contentType = MimeTypeMap.getSingleton().
                getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(fileToUpload.getPath()));
        String filePath = fileToUpload.getAbsolutePath();

        OkHttpClient client = CommonUtilities.getOkHttpClient();

        RequestBody fileBody = RequestBody.create(MediaType.parse(contentType), fileToUpload);

        RequestBody request_body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(Constants.FormAttributes.TYPE, contentType)
                .addFormDataPart(Constants.FormAttributes.VID_NAME, videoName)
                .addFormDataPart(Constants.FormAttributes.SEG_NO, String.valueOf(segmentNumber))
                //TODO: See that there's a better way to write the following name
                .addFormDataPart(Constants.FormAttributes.FILE_TO_UPLOAD,
                        filePath.substring(filePath.lastIndexOf("/") + 1), fileBody)
                .build();

        Request request = new Request.Builder()
                .url(Constants.Server.UPLOAD_URL)
                .post(request_body)
                .build();

        try {
            Response response = client.newCall(request).execute();
            String responseData = response.body().string();
            JSONObject jsonObject = new JSONObject(responseData);
            JSONArray array = jsonObject.getJSONArray("messages");
            StringBuilder messages = new StringBuilder();
            for (int i = 0; i < array.length(); i++) {
                JSONObject jData = array.getJSONObject(i);
                messages.append("\n").append(jData.getString("msg"));
            }
            Log.i(TAG, "Messages received from the server: \n" + messages);

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private int queryServerForLastAvailableSegmentNumber(String videoName) {
        RequestBody request_body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(Constants.FormAttributes.VID_NAME, videoName)
                .build();

        Request request = new Request.Builder()
                .url(Constants.Server.LAST_SEGMENT_URL)
                .post(request_body)
                .build();


        Response response;
        int result = -1;
        try {
            response = CommonUtilities.getOkHttpClient().newCall(request).execute();
            String responseData = response.body().string();
            JSONObject jsonObject = new JSONObject(responseData);
            result = jsonObject.getInt(Constants.FormAttributes.LAST_SEGMENT);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        Log.v(TAG, "Queried server and got " + result);
        return result;
    }

    // Should be called on the UI thread.
    void onNetworkLost() {
        addSegmentationToPendingUploads();
        // Set the flag so that no more uploads are attempted.
        mNetworkAvailable = false;
    }

    private void addSegmentationToPendingUploads() {
        UploadList.get().add(mSegmentationInfo);
    }
}