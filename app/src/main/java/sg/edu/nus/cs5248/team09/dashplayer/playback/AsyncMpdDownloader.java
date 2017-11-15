package sg.edu.nus.cs5248.team09.dashplayer.playback;

import android.content.res.XmlResourceParser;
import android.os.AsyncTask;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import sg.edu.nus.cs5248.team09.dashplayer.Constants;

import static sg.edu.nus.cs5248.team09.dashplayer.Constants.FILE_MPD;
import static sg.edu.nus.cs5248.team09.dashplayer.Constants.Server.BASE_URL;

/**
 * Created by Prakhar
 *
 * This task Downloads the MPD file for a given video.
 */

public class AsyncMpdDownloader extends AsyncTask {

    private int vidId;
    private String queryUrl;
    private IDownloadCompleteListener mListener;

    AsyncMpdDownloader(IDownloadCompleteListener listener, int vidId) {
        this.vidId = vidId;
        queryUrl = BASE_URL + vidId + FILE_MPD;
        mListener = listener;
    }

    @Override
    protected List<String[]> doInBackground(Object[] objects) {

        // Download the XML. The method will handle those exceptions.
        XmlPullParser xmlData = tryDownloadXML(queryUrl);

        // Try now to parse it.
        try {
            if(xmlData != null)
                return parse(xmlData);
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<String[]> parse(XmlPullParser xmlData) throws XmlPullParserException, IOException {
        // The result will be a list of String arrays. Each string array has the resolutions low, mid and high.
        List<String[]> segments = new ArrayList<>();
        int resType = Constants.Resolutions.NONE;

        int eventType = xmlData.getEventType();
        while (eventType != XmlResourceParser.END_DOCUMENT) {
            String tagName = xmlData.getName();
            Log.i("Log134", "type: " + eventType + "; name: " + (tagName == null? "null" : tagName));

            switch (eventType) {
                case XmlResourceParser.START_TAG:
                    // Start of a record, so pull values encoded as attributes.
                    if (tagName!= null && tagName.equals(Constants.XMLNames.Tags.REPRESENTATION)) {
                        int width = Integer.parseInt(xmlData.getAttributeValue(null, "width"));
                        switch (width){
                            case Constants.Resolutions.Dimensions.LOW:
                                resType = Constants.Resolutions.LOW;
                                break;
                            case Constants.Resolutions.Dimensions.MID:
                                resType = Constants.Resolutions.MID;
                                break;
                            case Constants.Resolutions.Dimensions.HIGH:
                                resType = Constants.Resolutions.HIGH;
                                break;
                        }

                    } else if(tagName!= null && tagName.equals(Constants.XMLNames.Tags.SEGMENT_URL)) {
                        String fileName = xmlData.getAttributeValue(null, Constants.XMLNames.Attributes.MEDIA);
                        int seg_no = Integer.parseInt(fileName.split("_")[0]);
                        if(segments.size() <= seg_no) {
                            // We may need to create the String Array
                            for(int i = segments.size(); i <= seg_no; i++) {
                                segments.add(new String[3]);
                            }
                        }
                        String[] segmentResolutions = segments.get(seg_no);
                        segmentResolutions[resType] = vidId + "/" + fileName;
                    }
                    break;
            }
            eventType = xmlData.next();
        }

        return segments;
    }

    private XmlPullParser tryDownloadXML(String queryUrl) {
        try {

            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(new URL(queryUrl).openStream(), null);

            return parser;

        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object segments) {
        mListener.onDownloadComplete(segments);
    }
}
