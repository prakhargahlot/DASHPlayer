package sg.edu.nus.cs5248.team09.dashplayer.recording;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import sg.edu.nus.cs5248.team09.dashplayer.CommonUtilities;
import sg.edu.nus.cs5248.team09.dashplayer.DashNotificationManager;

/**
 * Created by Meghana on 11-11-2017.
 *
 * Segments the videos
 */

public class SegmentationTask extends AsyncTask<String, Integer, SegmentationInfo>{

    private DashNotificationManager mNotificationManager;
    private ISegmentationCompleteListener mListener;

    private String mVideoPath;

    // Constructor
    SegmentationTask(Context context, ISegmentationCompleteListener listener) {
        mNotificationManager = new DashNotificationManager(context);
        mListener = listener;
    }

    // AsyncTask Methods
    @Override
    protected void onPreExecute() {
        mNotificationManager.createNotification(
                "Please be a little patient while we prepare your video for upload",
                "Preparing your video");
    }

    @Override
    protected SegmentationInfo doInBackground(String... strings) {
        String videoName = strings[0];
        String videoPath = strings[1];
//        retrieveMediaDuration(videoPath);

        return split(videoName, videoPath);
    }

    @Override
    protected void onPostExecute(SegmentationInfo segmentationInfo) {
        mNotificationManager.completed();
        mListener.onVideoSegmentationComplete(segmentationInfo);
    }

//    @Override
//    protected void onProgressUpdate(Integer... values) {
//        mNotificationManager.progressUpdate((int) ((values[0] * 100)/videoDuration));
//    }

    // Methods that actually do the Splitting Work
    private SegmentationInfo split(String videoName, String videoPath) {
        mVideoPath = videoPath;

        float MIN_SEGMENT_LENGTH = 3.0f;
        double startTime = 0.00, endTime = startTime + MIN_SEGMENT_LENGTH;
        int segmentNumber = 0;
        String segmentName = CommonUtilities.getSegmentName(videoName, segmentNumber);

        try {
            while ((endTime = split_segment(startTime, endTime, segmentName)) > 0) {
                publishProgress((int)endTime);
                segmentNumber++;
                startTime = endTime;                  // So that start ends up at 'end'
                endTime = startTime + MIN_SEGMENT_LENGTH;
                segmentName = CommonUtilities.getSegmentName(videoName, segmentNumber);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new SegmentationInfo(videoName, segmentNumber);
    }

    private double split_segment(double startTime, double endTime, String segmentName) throws IOException {
        com.googlecode.mp4parser.authoring.Movie movie = MovieCreator.build(mVideoPath);
        Log.i("DASH", "Movie Time:" +Long.toString(movie.getTimescale()));
        List<Track> tracks = movie.getTracks();
        movie.setTracks(new LinkedList<Track>());

        boolean timeCorrected = false;

        for (Track track : tracks) {
            if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
                if (timeCorrected) {

                    throw new RuntimeException("Already Sampled");
                }
                startTime = correctTimeToSyncSample(track, startTime, true);
                endTime = correctTimeToSyncSample(track, endTime, true);
                timeCorrected = true;
            }
        }
        if (startTime == endTime)
            return -endTime;    // Negative signifies the splitting is complete.

        for (Track track : tracks) {
            long currentSample = 0;
            double currentTime = 0;
            double lastTime = 0;
            long startSample1 = 0;
            long endSample1 = -1;

            for (int i = 0; i < track.getSampleDurations().length; i++) {
                long delta = track.getSampleDurations()[i];

                if (currentTime > lastTime && currentTime <= startTime) {
                    // current sample is still before the new starttime
                    startSample1 = currentSample;
                }
                if (currentTime > lastTime && currentTime <= endTime) {
                    // current sample is after the new start time and still before the new endtime
                    endSample1 = currentSample;
                }

                lastTime = currentTime;
                currentTime += (double) delta / (double) track.getTrackMetaData().getTimescale();
                currentSample++;
            }
            movie.addTrack(new CroppedTrack(track, startSample1, endSample1));
        }
        long start1 = System.currentTimeMillis();

        Container out = new DefaultMp4Builder().build(movie);
        long start2 = System.currentTimeMillis();
        FileOutputStream fos = new FileOutputStream(CommonUtilities.getSegmentPath(segmentName));
        FileChannel fc = fos.getChannel();
        out.writeContainer(fc);
        fc.close();
        fos.close();
        long start3 = System.currentTimeMillis();
        Log.i("DASH", "Building IsoFile took : " + (start2 - start1) + "ms");
        Log.i("DASH", "Writing IsoFile took  : " + (start3 - start2) + "ms");
        return endTime;
    }

    @SuppressWarnings("SameParameterValue")
    private double correctTimeToSyncSample(Track track, double cutHere, boolean next) {
        double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
        long currentSample = 0;
        double currentTime = 0;
        for (int i = 0; i < track.getSampleDurations().length; i++) {
            long delta = track.getSampleDurations()[i];

            if (Arrays.binarySearch(track.getSyncSamples(), currentSample + 1) >= 0) {
                // samples always start with 1 but we start with zero therefore +1
                timeOfSyncSamples[Arrays.binarySearch(track.getSyncSamples(), currentSample + 1)] = currentTime;
            }
            currentTime += (double) delta / (double) track.getTrackMetaData().getTimescale();
            currentSample++;

        }
        double previous = 0;
        for (double timeOfSyncSample : timeOfSyncSamples) {
            if (timeOfSyncSample >= cutHere) {
                if (next) {
                    return timeOfSyncSample;
                } else {
                    return previous;
                }
            }
            previous = timeOfSyncSample;
        }
        return timeOfSyncSamples[timeOfSyncSamples.length - 1];
    }

    // Helper methods
//    private void retrieveMediaDuration(String videoPath) {
//        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
//        retriever.setDataSource(mContext, Uri.fromFile(new File(videoPath)));
//        videoDuration = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
//        retriever.release();
//    }
}

