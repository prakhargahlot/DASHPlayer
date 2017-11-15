package sg.edu.nus.cs5248.team09.dashplayer.playback;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import sg.edu.nus.cs5248.team09.dashplayer.CommonUtilities;
import sg.edu.nus.cs5248.team09.dashplayer.Constants;

/**
 * Created by Prakhar/Meghana.
 *
 * The buffer used to play downloaded videos. Also contains the Download Task as subclass
 */

class MediaBuffer {

    // TODO: Make these values customizable for demo
    private static final int LOW_RES_THRESHOLD = 3;
    private static final int PRELOAD_COUNT = 4;
    private static final int HIGH_RES_THRESHOLD = 5;

    private static final String LOG_TAG = "MediaPlayerLogging";

    private List<String> synchronizedBuffer;
    private AsyncTask preLoadTask, downLoadTask;

    MediaBuffer() {
        synchronizedBuffer = Collections.synchronizedList(new ArrayList<String>());
    }

    void preLoad(int startFrom, List<String[]> representations, IDownloadCompleteListener listener) {
        Log.v(LOG_TAG, "Preload Called from " + startFrom);
        preLoadTask = new VideoDownloader().execute(representations.subList(startFrom, Math.min(representations.size(), startFrom + PRELOAD_COUNT)),
                                        false /* downloadIntelligently */, listener);
    }

    void downloadVideo(List<String[]> representations, IDownloadCompleteListener listener) {
        Log.v(LOG_TAG, "Loading Called");
        int startFrom = Math.min(representations.size(), PRELOAD_COUNT);
        downLoadTask = new VideoDownloader().execute(representations.subList(startFrom, representations.size()),
                                        true /* downloadIntelligently */, listener);
    }

    void cancelDownloads() {
        preLoadTask.cancel(true);
        downLoadTask.cancel(true);
    }

    String getNextSegmentToPlay() {
        Log.i(LOG_TAG, "Next segment to play called. Size: " + synchronizedBuffer.size());
        if(synchronizedBuffer.isEmpty()) {
            return null;
        }
        return synchronizedBuffer.remove(0);
    }

    private class VideoDownloader extends AsyncTask {

        IDownloadCompleteListener mListener;
        private boolean finishingDownload;

        @Override
        protected void onCancelled() {
            finishingDownload = true;
        }

        @Override
        protected Object doInBackground(Object ... params) {
            List<String[]> representations = (List<String[]>) params[0];
            boolean downloadIntelligently = (boolean) params[1];
            mListener = (IDownloadCompleteListener) params[2];

            if(representations == null || representations.size() == 0) {
                // Nothing to do.
                return null;
            }

            // Ensure that a folder exists before we go ahead downloading stuff.
            String samplePath = representations.get(0)[0];
            File outputDir = new File(Constants.LOCAL_HOME + samplePath.substring(0, 1 + samplePath.indexOf(File.separator)));
            if(outputDir.mkdirs()) {
                Log.v(LOG_TAG, "Output Directories created");
            }

            for(int i = 0; i < representations.size() && !finishingDownload; i++) {
                int resType = Constants.Resolutions.HIGH;
                if(downloadIntelligently) {
                    if(synchronizedBuffer.size() < LOW_RES_THRESHOLD) {
                        resType = Constants.Resolutions.LOW;
                    } else if (synchronizedBuffer.size() < HIGH_RES_THRESHOLD) {
                        resType = Constants.Resolutions.MID;
                    }
                }
                Log.i(LOG_TAG, "Adding " + representations.get(i)[resType] + " to size " + synchronizedBuffer.size());
                String filePath = download(representations.get(i)[resType]);
                if(filePath != null) {
                    synchronizedBuffer.add(filePath);
                }
            }
            return null;
        }

        private String download(String fileName)
        {
            OkHttpClient client = CommonUtilities.getOkHttpClient();
            Request request = new Request.Builder().url(Constants.Server.BASE_URL + fileName).build();
            Response response;

            try {
                response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to download file: " + response);
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "here?" + e.getMessage());
                return null;
            }

            FileOutputStream fos;
            String localFile = Constants.LOCAL_HOME + fileName;
            try {
                fos = new FileOutputStream(localFile);
                fos.write(response.body().bytes());
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return localFile;
        }

        @Override
        protected void onPostExecute(Object o) {
            if(mListener != null) {
                mListener.onDownloadComplete();
            }
        }
    }
}
