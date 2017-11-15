package sg.edu.nus.cs5248.team09.dashplayer.recording;

/**
 * Created by Prakhar on 14-11-2017.
 *
 * Interface for anyone that is segmenting a video
 */

public interface ISegmentationCompleteListener {
    void onVideoSegmentationComplete(SegmentationInfo info);
}
