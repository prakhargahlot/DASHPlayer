package sg.edu.nus.cs5248.team09.dashplayer.playback;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.File;
import java.io.IOException;
import java.util.List;

import sg.edu.nus.cs5248.team09.dashplayer.R;

/**
 * Author: Meghana
 *
 * Activity to play the downloaded videos
 */
public class MediaPlayerActivity extends AppCompatActivity implements IDownloadCompleteListener {

    private static final String LOG_TAG = "MediaPlayerLogging";
    private MediaBuffer mBuffer;
    private MediaPlayer mMediaPlayer;
    private SurfaceHolder mSurfaceHolder;
    private Button mPlayButton;
    private boolean canPlay;
    private List<String[]> mSegments;
    private String mLastPlayed = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);
        mSurfaceHolder = ((VideoView) findViewById(R.id.videoView)).getHolder();
        mBuffer = new MediaBuffer();
        int v_id = getIntent().getIntExtra("VIDEO_ID", -11);

        // See playing conditions here
        canPlay = true;

        // Set the title
        String title = getIntent().getStringExtra("VIDEO_NAME");
        if (title != null) {
            ((TextView) findViewById(R.id.playbackVideoTitle)).setText(title);
        }

        // Handle the pause button
        mPlayButton = (Button) findViewById(R.id.playButton);
        mPlayButton.setText(R.string.pause);
        mPlayButton.setEnabled(false);
        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                    mPlayButton.setText(R.string.play);
                } else {
                    mMediaPlayer.start();
                    mPlayButton.setText(R.string.pause);
                }
            }
        });
        new AsyncMpdDownloader(this, v_id).execute();
    }

    public void onSegmentsReceived(final List<String[]> segments, int startPlaybackFrom) {
        mSegments = segments;
        mBuffer.preLoad(startPlaybackFrom, mSegments, new IDownloadCompleteListener() {
            @Override
            public void onDownloadComplete(Object... downloads) {
                // Throw Toast.
                Log.v(LOG_TAG, "Preload Done");
                queueDownloadsAndPlayback();
            }
        });
    }

    private void queueDownloadsAndPlayback() {
        if(canPlay) {
            // Launch Rest of the download.
            mBuffer.downloadVideo(mSegments, MediaPlayerActivity.this);

            // Launch Media Player.
            startPlayingNextSegment();
        }
    }

    private void startPlayingNextSegment() {

        Log.v(LOG_TAG, "startPlayingNextSegment called");

        // Free up resources
        if(mMediaPlayer != null) {
            clearUp();
        }

        // See if there's anything to play
        final String path = mBuffer.getNextSegmentToPlay();
        if(path == null) {
            if(getLastSegmentNumber() + 1 < mSegments.size()) {
                // Network issue. Check after some time.
                new Handler(Looper.myLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        onSegmentsReceived(mSegments, getLastSegmentNumber() + 1);
                    }
                }, 1000);
            }
            videoPlaybackFinished();
            return;
        }
        Log.v(LOG_TAG, "File to play: " + path);

        try {
            // Make sure Player is in Idle state
            mMediaPlayer = new MediaPlayer();
            Log.v(LOG_TAG, "Media Player created");

            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                    Log.e(LOG_TAG, "what: " + what + ", extra: " + extra);
                    //mMediaPlayer.reset();
                    startPlayingNextSegment();
                    return true;
                }
            });
            mMediaPlayer.setDisplay(mSurfaceHolder);
            Log.v(LOG_TAG, "Media Player display set");
            mMediaPlayer.setScreenOnWhilePlaying(true);

            // Move to Initialized state
            mMediaPlayer.setDataSource(path);
            Log.v(LOG_TAG, "Media Player data source set");

            // Move to Prepared state
            mMediaPlayer.prepare();
            Log.i(LOG_TAG, "prepare called");

        } catch (IOException e) {
            e.printStackTrace();
        }

        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                Log.i(LOG_TAG, "Received onCompletion callback");
                mLastPlayed = path;
                startPlayingNextSegment();
            }
        });

        Log.i(LOG_TAG, "Calling start");
        // Start the video
        mPlayButton.setEnabled(true);
        mMediaPlayer.start();
    }

    private void videoPlaybackFinished() {
        Log.v(LOG_TAG, "Playback Done");
    }

    @Override
    public void onDownloadComplete(Object... downloads) {
        if(downloads != null && downloads.length > 0 && downloads[0] != null && downloads[0] instanceof List) {
            onSegmentsReceived((List<String[]>) downloads[0], 0);
        }
        Log.v(LOG_TAG, "Download Done");
    }

    void clearUp() {
        Log.v(LOG_TAG, "Media Player not null. Resetting and releasing.");
        mPlayButton.setEnabled(false);
        mMediaPlayer.reset();
        mMediaPlayer.release();
        mMediaPlayer = null;
    }

    int getLastSegmentNumber() {
        String lastVideoSegmentName = mLastPlayed.substring(1 + mLastPlayed.lastIndexOf(File.separator));
        return Integer.parseInt(lastVideoSegmentName.substring(0, lastVideoSegmentName.indexOf('_')));
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(LOG_TAG, "Stop called");
        canPlay = false;
        if(mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            clearUp();
        }

        mBuffer.cancelDownloads();
        mLastPlayed = null;
    }
}
