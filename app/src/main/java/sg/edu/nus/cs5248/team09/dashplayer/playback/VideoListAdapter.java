package sg.edu.nus.cs5248.team09.dashplayer.playback;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import sg.edu.nus.cs5248.team09.dashplayer.CommonUtilities;
import sg.edu.nus.cs5248.team09.dashplayer.R;
import sg.edu.nus.cs5248.team09.dashplayer.recording.RecordActivity;
import sg.edu.nus.cs5248.team09.dashplayer.recording.UploadList;

/**
 * Created by Prakhar
 *
 * The adapter that does the heavy lifting of querying the server for video list
 */

public class VideoListAdapter extends RecyclerView.Adapter<VideoListAdapter.VideoListViewHolder>{

    private List<Video> mVideos;
    private Context mContext;
    private boolean isNetworkCardShown = false;
    VideoListAdapter(Context context) {
        mContext = context;
        mVideos = new ArrayList<>();
        refreshDataSet(mContext, null /* downloadCompleteListener */);
    }

    void refreshDataSet(final Context context, final IDownloadCompleteListener sourceDownloadCompleteListener) {
        IDownloadCompleteListener videosDownloadedListener =  new IDownloadCompleteListener() {
            @Override
            public void onDownloadComplete(Object... downloads) {
                List<Video> list = (List<Video>) downloads[0];
                if(list != null && list.size() !=0) {
                    mVideos = list;
                    // No need to sort. Server does that.
                    Collections.reverse(mVideos);
                    if(CommonUtilities.isOnline(context) && !UploadList.get().isEmpty()
                            && UploadList.aggressivePush) {
                        mVideos.add(0, new Video("Network", -1));
                        isNetworkCardShown = true;
                    }
                    notifyDataSetChanged();
                } else {
                    mVideos.clear();
                }

                // Tell our source too.
                if(sourceDownloadCompleteListener != null) {
                    sourceDownloadCompleteListener.onDownloadComplete(downloads);
                }
            }
        };

        new AsyncVideoListGetter(videosDownloadedListener).execute();
    }

    @Override
    public VideoListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.card_item, parent, false);

        return new VideoListViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final VideoListViewHolder holder, int position) {
        if(position == 0 && isNetworkCardShown) {
            holder.network_card.setVisibility(View.VISIBLE);
            holder.basic_card.setVisibility(View.GONE);
            holder.laterListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mVideos.remove(0);
                    isNetworkCardShown = false;
                    notifyDataSetChanged();
                }
            };
            holder.sureListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mVideos.remove(0);
                    isNetworkCardShown = false;
                    Intent intent = new Intent(mContext, RecordActivity.class);
                    intent.putExtra("DoPendingUploads", true);
                    mContext.startActivity(intent);
                }
            };
        } else {
            holder.network_card.setVisibility(View.GONE);
            holder.basic_card.setVisibility(View.VISIBLE);
            holder.vVideoName.setText(mVideos.get(position).getName());
            holder.vImage.setImageResource(R.mipmap.ic_launcher_round);
            holder.vToucher = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Video vid = mVideos.get(holder.getAdapterPosition());
                    Intent intent = new Intent(mContext, MediaPlayerActivity.class);
                    intent.putExtra("VIDEO_ID", vid.getId());
                    intent.putExtra("VIDEO_NAME", vid.getName());
                    mContext.startActivity(intent);
                }
            };
        }
    }

    @Override
    public int getItemCount() {
        return mVideos.size();
    }

    public static class VideoListViewHolder extends RecyclerView.ViewHolder {

        ImageView vImage;
        TextView vVideoName;
        View.OnClickListener vToucher = null;
        View basic_card, network_card;
        View.OnClickListener sureListener, laterListener;
        protected int id;

        VideoListViewHolder(View itemView) {
            super(itemView);
            (itemView.findViewById(R.id.basic_card_container)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(vToucher != null) {
                        vToucher.onClick(view);
                    }
                }
            });
            vImage = itemView.findViewById(R.id.card_icon);
            vVideoName = itemView.findViewById(R.id.card_label);
            basic_card = itemView.findViewById(R.id.basic_card_container);
            network_card = itemView.findViewById(R.id.network_card_container);
            itemView.findViewById(R.id.network_card_sure).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(sureListener != null) {
                        sureListener.onClick(view);
                    }
                }
            });

            itemView.findViewById(R.id.network_card_later).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(laterListener != null) {
                        laterListener.onClick(view);
                    }
                }
            });
        }
    }
}
