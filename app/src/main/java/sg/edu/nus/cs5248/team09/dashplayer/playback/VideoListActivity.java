package sg.edu.nus.cs5248.team09.dashplayer.playback;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import java.io.File;

import sg.edu.nus.cs5248.team09.dashplayer.AboutActivity;
import sg.edu.nus.cs5248.team09.dashplayer.Constants;
import sg.edu.nus.cs5248.team09.dashplayer.R;
import sg.edu.nus.cs5248.team09.dashplayer.recording.RecordActivity;

/**
 * Author: Prakhar
 *
 * The first activity. Does initializations and lists the videos available.
 */
public class VideoListActivity extends AppCompatActivity {

    private static final String LOG_TAG = "VideoListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initialize();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(VideoListActivity.this, RecordActivity.class);
                VideoListActivity.this.startActivity(intent);
            }
        });

        FloatingActionButton fabSettings = (FloatingActionButton) findViewById(R.id.fab_settings);
        fabSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(VideoListActivity.this, AboutActivity.class);
                VideoListActivity.this.startActivity(intent);
            }
        });

        RecyclerView list = (RecyclerView) findViewById(R.id.recycler_list);
        list.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        list.setLayoutManager(llm);
        final VideoListAdapter adapter = new VideoListAdapter(this);
        list.setAdapter(adapter);

        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_view);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                adapter.refreshDataSet(VideoListActivity.this, new IDownloadCompleteListener() {
                    @Override
                    public void onDownloadComplete(Object... downloads) {
                        // do something here.
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
    }

    void initialize() {
        // Check for permissions
        if(PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Log.v(LOG_TAG, "Permission: " + ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE));
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Setup first");
            builder.setMessage("Maybe check permissions and create folders?");
            builder.setCancelable(true);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                }
            });
            builder.setNegativeButton("Later", null);
            builder.create().show();
        }

        // Create paths
        String samplePath = Constants.SEGMENT_CREATE_FOLDER;
        File outputDir = new File(samplePath);
        if(outputDir.mkdirs()) {
            Log.v(LOG_TAG, "Basic directories created.");
        }
    }
}
