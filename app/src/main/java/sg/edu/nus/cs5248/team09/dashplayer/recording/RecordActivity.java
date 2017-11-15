package sg.edu.nus.cs5248.team09.dashplayer.recording;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import sg.edu.nus.cs5248.team09.dashplayer.CommonUtilities;
import sg.edu.nus.cs5248.team09.dashplayer.R;

/**
 * Created by Meghana on 11-11-2017.
 */

public class RecordActivity extends AppCompatActivity implements SurfaceHolder.Callback{

    private Button recordButton = null;
    private Button reviewButton = null;
    private Button uploadButton = null;
    private Button customFile = null;
    private SurfaceView videoView = null;
    private SurfaceHolder holder = null;
    private MediaRecorder recorder;
    private SegmentationInfo segmentationInfo;
    private Camera camera;
    private final String TAG = "DASHRecorder";
    private String mVideoPath;
    private String mVideoName;
    private UploadTask mUploadTask;
    private boolean isRecording = false;
    private TextView mCustomFileNameHolder;

    private EditText customName;
    private boolean customFileSet = false;
    boolean ask;
    boolean forceDoPendingUploads;
    private ConnectivityManager.NetworkCallback networkCallback;

    private static final int MEDIA_PICK = 172;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_video);

        videoView = (SurfaceView) findViewById(R.id.videoView);
        recordButton = (Button) findViewById(R.id.record_button);
        reviewButton = (Button) findViewById(R.id.review_button);
        uploadButton = (Button) findViewById(R.id.upload_button);
        customName = (EditText) findViewById(R.id.userVidName);
        customFile = (Button) findViewById(R.id.customFile);
        mCustomFileNameHolder = (TextView) findViewById(R.id.customFileName);

        View.OnClickListener listenerForButtons = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tapped(view);
            }
        };

        recordButton.setOnClickListener(listenerForButtons);
        reviewButton.setOnClickListener(listenerForButtons);
        uploadButton.setOnClickListener(listenerForButtons);
        customFile.setOnClickListener(listenerForButtons);

        ask = getIntent().getBooleanExtra("AskForUploadOnLaunch", UploadList.aggressivePush);
        forceDoPendingUploads = getIntent().getBooleanExtra("DoPendingUploads", false);
    }

    @TargetApi(Build.VERSION_CODES.N)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onStart() {
        super.onStart();

        // Register for network changes in case we lose network while uploading.
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLost(Network network) {
                super.onLost(network);
                if(mUploadTask != null) {
                    mUploadTask.onNetworkLost();
                }
            }

            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                if(forceDoPendingUploads) {
                    doPendingUploads();
                }
                if(!UploadList.get().isEmpty() && ask) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(RecordActivity.this);
                    builder.setTitle("Network is Back");
                    builder.setMessage("Do you want to resume uploads?");
                    builder.setCancelable(true);
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            doPendingUploads();
                        }
                    });
                    builder.setNegativeButton("Later", null);
                    builder.create().show();
                }
            }
        };
        cm.registerDefaultNetworkCallback(networkCallback);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onStop() {
        super.onStop();
        stopRecording();
        // Deregister before leaving.
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.unregisterNetworkCallback(networkCallback);
    }

    private void doPendingUploads() {
        Iterator<SegmentationInfo> iterator = UploadList.get().iterator();
        while (iterator.hasNext()) {
            SegmentationInfo info = iterator.next();
            upload(info, true, false);
            iterator.remove();
        }
    }

    public void tapped(View view)
    {
        switch (view.getId())
        {
            case R.id.record_button:
                if(!isRecording) {
                    if(customFileSet) {
                        new AlertDialog.Builder(this)
                                .setMessage("The uploaded file will be removed.")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        mVideoName = null;
                                        mVideoPath = null;
                                        customFileSet = false;
                                        mCustomFileNameHolder.setText("");
                                        mCustomFileNameHolder.setEnabled(false);
                                        tapped(recordButton);
                                    }
                                })
                                .setNegativeButton("No", null).create().show();
                        break;
                    }
                    // Behaves as start button
                    initRecorder();
                    startRecording();
                    isRecording = true;
                    recordButton.setText("Stop Recording");
                } else  {
                    // Behave as stop button
                    stopRecording();
                    isRecording = false;
                    recordButton.setText("Start Recording");
                }
                break;
            case R.id.review_button:
                playRecording();
                break;
            case R.id.upload_button:
                // User wants to upload. Segment now.
                new SegmentationTask(this, new ISegmentationCompleteListener() {
                    @Override
                    public void onVideoSegmentationComplete(SegmentationInfo info) {
                        segmentationInfo = info;
                        // See if they have a custom name defined for the video.
                        if(customName != null && customName.getText()!= null && !customName.getText().toString().trim().equals("")) {
                            segmentationInfo.setUploadName(customName.getText().toString());
                        } else {
                            segmentationInfo.setUploadName(mVideoName);
                        }
                        upload(segmentationInfo, false, true);
                    }
                }).execute(mVideoName, mVideoPath);
                break;
            case R.id.customFile:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("video/*");// MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, MEDIA_PICK);
                break;
        }
    }

    private void initRecorder() {
        if(recorder != null) {
            return;
        }

        mVideoName = CommonUtilities.newVideoName();
        mVideoPath = CommonUtilities.getVideoPath(mVideoName);

        try
        {
            camera.stopPreview();
            camera.unlock();
            recorder = new MediaRecorder();
            recorder.setCamera(camera);
            recorder.setOrientationHint(90);
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            recorder.setOutputFile(mVideoPath);
            recorder.setVideoSize(720, 480);
            recorder.setVideoEncodingBitRate(5000000);
            recorder.setVideoFrameRate(30);
            recorder.setPreviewDisplay(holder.getSurface());
            recorder.setMaxDuration(20000000);
            recorder.setMaxFileSize(2000000000);

            recorder.prepare();
            Log.v(TAG,"MediaRecorder initialized");
        } catch (Exception e)
        {
            Log.v(TAG,"MediaRecorder failed to initialize");
            e.printStackTrace();
        }
    }

    private void startRecording() {
        Log.d(TAG, "Started recording");

        try {
            recorder.start();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        if(recorder != null){
            try {
                recorder.stop();
            }
            catch (IllegalStateException e) {
                Log.e(TAG,"Got IllegalStateException in stopRecording");
            }
            releaseRecorder();
            releaseCamera();
        }
        reviewButton.setEnabled(true);
        uploadButton.setEnabled(true);
    }

    private void playRecording(){
        MediaPlayer play = new MediaPlayer();
        try {
            play.setDataSource(mVideoPath);
        } catch (IOException e) {
            Log.v(TAG,"Error1");
        }
        play.setDisplay(holder);
        try {
            play.prepare();
        } catch (IOException e) {
            Log.v(TAG,"Error2");
        }
        play.start();
        play.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                         @Override
                                         public void onCompletion(MediaPlayer play) {
                                             play.release();
                                         }
                                     }
        );
    }

    private void upload(SegmentationInfo info, boolean isResumingUpload, boolean doSilently)
    {
        if(info != null) {
            CommonUtilities.toast(RecordActivity.this, "total number of segments: " + info.size());
            mUploadTask = new UploadTask(RecordActivity.this);
            mUploadTask.execute(info, isResumingUpload);
        }
    }

    private void releaseRecorder() {
        if(recorder != null)
        {
            recorder.release();
            recorder = null;
        }

    }
    private void releaseCamera() {
        if(camera != null)
        {
            try
            {
                camera.reconnect();
            }catch (IOException e)
            {
                e.printStackTrace();
            }
            camera.release();
            camera = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG,"in Surface created");
        try
        {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        }catch (IOException e)
        {
            Log.v(TAG,"Could not start the preview");
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "Surface changed called");
        Camera.Parameters arg = camera.getParameters();
        arg.setPreviewSize(720, 480);
        camera.setParameters(arg);
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (IOException e) {
            Log.i(TAG, "Some camera exception :" + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "Surface Destroyed");
        stopRecording();
    }
    @Override
    protected void onResume(){
        Log.v(TAG,"in onResume");
        super.onResume();
        reviewButton.setEnabled(false);
        if(!customFileSet) {
            uploadButton.setEnabled(false);
        }
        if(!initCamera())
            finish();
    }

    private boolean initCamera() {
        try {
            camera = Camera.open();
            camera.setDisplayOrientation(90);
            camera.lock();
            holder = videoView.getHolder();
            holder.addCallback(this);
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
        catch (RuntimeException re)
        {
            Log.v(TAG,"Could not initialize the Camera");
            re.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == MEDIA_PICK && resultCode == RESULT_OK) {
            Uri video = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(video,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            mVideoPath = cursor.getString(columnIndex);
            cursor.close();

            //mVideoPath = video.getPath();
            mVideoName = mVideoPath.substring(mVideoPath.lastIndexOf(File.separator) + 1,
                                                mVideoPath.lastIndexOf("."));

            // Show the name of the file
            mCustomFileNameHolder.setVisibility(View.VISIBLE);
            mCustomFileNameHolder.setText(mVideoName);
            if(customName.getText() != null && !customName.getText().toString().trim().equals("")) {
                Snackbar.make(mCustomFileNameHolder,
                        "Beware. Custom name is set. That will be the name uploaded.",
                        Snackbar.LENGTH_LONG).show();
            }
            customFileSet = true;
            uploadButton.setEnabled(true);
        }
    }
}