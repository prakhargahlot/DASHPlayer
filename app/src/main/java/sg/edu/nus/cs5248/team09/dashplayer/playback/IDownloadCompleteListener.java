package sg.edu.nus.cs5248.team09.dashplayer.playback;

/**
 * Created by Prakhar on 07-11-2017.
 *
 * Interface for anyone downloading anything
 */

public interface IDownloadCompleteListener {
    void onDownloadComplete(Object... downloads);
}
