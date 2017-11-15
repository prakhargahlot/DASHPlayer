package sg.edu.nus.cs5248.team09.dashplayer.playback;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import sg.edu.nus.cs5248.team09.dashplayer.CommonUtilities;
import sg.edu.nus.cs5248.team09.dashplayer.Constants;

/**
 * Created by Prakhar.
 *
 * Downloads Available Video List for playback app.
 */

public class AsyncVideoListGetter extends AsyncTask {

    private OkHttpClient httpClient;
    private static final String phpUrl = Constants.Server.VIDEO_LIST_URL;

    private IDownloadCompleteListener mDownloadCompleteListener;

    AsyncVideoListGetter(IDownloadCompleteListener listener) {
        httpClient = CommonUtilities.getOkHttpClient();
        mDownloadCompleteListener = listener;
    }

    @Override
    protected void onPostExecute(Object list) {
        mDownloadCompleteListener.onDownloadComplete(list);
    }

    @Override
    protected List<Video> doInBackground(Object[] objects) {
        Request request = new Request.Builder().url(phpUrl).build();
        try {
            Response response = httpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return parse(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<Video> parse(Response response) {

        ArrayList<Video> list = new ArrayList<>();

        try {
            //noinspection ConstantConditions
            String responseData = response.body().string();
            JSONObject jsonObject = new JSONObject(responseData);

            if(1 == jsonObject.getInt("success")) {
                JSONArray array = jsonObject.getJSONArray("videos");
                for(int i = 0; i < array.length(); i++) {
                    JSONObject jData = array.getJSONObject(i);
                    int id = jData.getInt("ID");
                    String name = jData.getString("Name");

                    list.add(new Video(name, id));
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return list;
    }
}

