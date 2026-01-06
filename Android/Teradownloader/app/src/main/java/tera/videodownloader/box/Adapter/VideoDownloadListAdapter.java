package tera.videodownloader.box.Adapter;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.jeffmony.downloader.model.VideoTaskItem;
import com.jeffmony.downloader.model.VideoTaskState;
import tera.videodownloader.box.R;
import tera.videodownloader.box.Utils.PrefManager;
import tera.videodownloader.box.Utils.VideoDownloadManager;

import java.io.File;
import java.util.List;

public class VideoDownloadListAdapter extends RecyclerView.Adapter<VideoDownloadListAdapter.ViewHolder> {

    private Context mContext;
    private List<VideoTaskItem> mItems;
    private PrefManager prefManager;

    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, VideoTaskItem obj, int position);
    }

    public void setOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mOnItemClickListener = mItemClickListener;
    }

    public VideoDownloadListAdapter(Context context, List<VideoTaskItem> items) {
        this.mContext = context;
        this.mItems = items;
        prefManager = new PrefManager(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout
        View view = LayoutInflater.from(mContext).inflate(R.layout.download_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VideoTaskItem item = mItems.get(position);

        // Set data to the views
        holder.urlTextView.setText(item.getGroupName());

        Log.d("ymgs", item.getUrl());

        Glide.with(mContext)
                .load(item.getCoverUrl())
                .into(holder.myImageView);




        setStateText(holder.stateTextView, holder.playBtn, item);

        holder.stateTextView.setText(prefManager.getString(item.getGroupName()+"_status"));

        int currentProgress = prefManager.getInt(item.getGroupName()+"_progress");
        holder.progress_horizontal.setProgress(currentProgress);


        int currentSize = prefManager.getInt(item.getGroupName()+"_size");
        holder.tvSize.setText(formatFileSize(currentSize));

        if (currentProgress == 100){
            holder.stateTextView.setText("Downloaded");
            holder.layoutText.setVisibility(View.GONE);
            holder.layoutProgress.setVisibility(View.GONE);
            holder.tvPlay.setVisibility(View.VISIBLE);
            holder.playBtn.setImageResource(R.drawable.round_check_circle_outline_24);

        }else {
            holder.layoutText.setVisibility(View.VISIBLE);
            holder.layoutProgress.setVisibility(View.VISIBLE);
            holder.tvPlay.setVisibility(View.GONE);

        }

        setDownloadInfoText(holder.tvSpeed, holder.tvProgress, holder.tvDownloaded, holder.progress_horizontal, holder.layoutText, holder.layoutProgress, holder.tvPlay, holder.playBtn, item);

        holder.tvPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_MEDIA_VIDEO)
                        != PackageManager.PERMISSION_GRANTED) {

                    // Requesting the permission
                    ActivityCompat.requestPermissions((Activity) mContext,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_MEDIA_VIDEO}, 1);
                } else {
                    // Permission is granted, proceed with accessing the file
                    openVideoPlayer(item.getGroupName());
                }


            }
        });

        holder.playBtn.setOnClickListener(view -> {
            int clickedPosition = holder.getAdapterPosition(); // Get the current adapter position

            // Check if the position is valid
            if (clickedPosition != RecyclerView.NO_POSITION) {
                VideoTaskItem clickedItem = mItems.get(clickedPosition);

                // Handle the download task based on its current state
                if (clickedItem.isInitialTask()) {
                    // Start the download if the task is in the initial state
                    VideoDownloadManager.getInstance().startDownload(clickedItem);
                } else if (clickedItem.isRunningTask()) {
                    // Pause the download if it's running
                    VideoDownloadManager.getInstance().pauseDownloadTask(clickedItem.getUrl());
                } else if (clickedItem.isInterruptTask()) {
                    // Resume the download if it was interrupted
                    VideoDownloadManager.getInstance().resumeDownload(clickedItem.getUrl());
                }
            }
        });
    }

    private void openVideoPlayer(String fileName){
        String videoPath = "/storage/emulated/0/Download/"+fileName;
        File videoFile = new File(videoPath);
        Uri videoUri = FileProvider.getUriForFile(
                mContext, "tera.videodownloader.box.fileprovider",videoFile
        );
        // Create an Intent to open the video with a player
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(videoUri, "video/*");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Handle the case where no video player is installed
            Toast.makeText(mContext, "No video player found", Toast.LENGTH_SHORT).show();
        }
    }

    public String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B"; // Bytes
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0); // Kilobytes
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024)); // Megabytes
        } else if (bytes < 1024L * 1024 * 1024 * 1024) {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024)); // Gigabytes
        } else {
            return String.format("%.2f TB", bytes / (1024.0 * 1024 * 1024 * 1024)); // Terabytes
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    // ViewHolder class to hold the views for each item
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView urlTextView, stateTextView, tvSpeed, tvProgress, tvDownloaded, tvSize, tvPlay;
        ImageView playBtn, myImageView;
        ProgressBar progress_horizontal;
        LinearLayout layoutText, layoutProgress;

        public ViewHolder(View itemView) {
            super(itemView);
            urlTextView = itemView.findViewById(R.id.url_text);
            stateTextView = itemView.findViewById(R.id.status_txt);
            playBtn = itemView.findViewById(R.id.ivPlay);
            myImageView = itemView.findViewById(R.id.myImageView);
            tvDownloaded = itemView.findViewById(R.id.tvDownloaded);
            tvProgress = itemView.findViewById(R.id.tvProgress);
            tvSpeed = itemView.findViewById(R.id.tvSpeed);
            progress_horizontal = itemView.findViewById(R.id.progress_horizontal);
            tvSize = itemView.findViewById(R.id.tvSize);
            layoutText = itemView.findViewById(R.id.layoutText);
            layoutProgress = itemView.findViewById(R.id.layoutProgress);
            tvPlay = itemView.findViewById(R.id.tvPlay);
        }
    }

    public void updateData(List<VideoTaskItem> items) {
        this.mItems = items;
        notifyDataSetChanged();
    }

    private void setStateText(TextView stateView, ImageView playBtn, VideoTaskItem item) {
        switch (item.getTaskState()) {
            case VideoTaskState.PENDING:
            case VideoTaskState.PREPARE:
                playBtn.setImageResource(R.drawable.round_pause_circle_outline_24);
                stateView.setText(mContext.getResources().getString(R.string.waiting));
                prefManager.setString(item.getGroupName()+"_status", "Waiting..");
                break;
            case VideoTaskState.START:
            case VideoTaskState.DOWNLOADING:
                playBtn.setImageResource(R.drawable.round_pause_circle_outline_24);
                stateView.setText(mContext.getResources().getString(R.string.downloading));
                prefManager.setString(item.getGroupName()+"_status", "Downloading..");
                break;
            case VideoTaskState.PAUSE:
                playBtn.setImageResource(R.drawable.ic_menu_play);
                stateView.setText("Paused");
                prefManager.setString(item.getGroupName()+"_status", "Paused");
                break;
            case VideoTaskState.SUCCESS:
                playBtn.setImageResource(R.drawable.round_check_circle_outline_24);
                stateView.setText(String.format(mContext.getResources().getString(R.string.download_completed_total_size), item.getDownloadSizeString()));
                prefManager.setString(item.getGroupName()+"_status", "Downloaded");
                break;
            case VideoTaskState.ERROR:
                playBtn.setImageResource(R.drawable.round_error_outline_24);
                stateView.setText(mContext.getResources().getString(R.string.download_error));
                prefManager.setString(item.getGroupName()+"_status", "Download Error");
                break;
            default:
                playBtn.setImageResource(R.drawable.ic_menu_play);
                stateView.setText(prefManager.getString(item.getGroupName()+"_status"));
                break;
        }
    }

    private void setDownloadInfoText(TextView tvSpeed, TextView tvProgress, TextView tvDownloaded, ProgressBar progress_horizontal, LinearLayout layoutText, LinearLayout layoutProgress, TextView tvPlay, ImageView playBtn, VideoTaskItem item) {
        switch (item.getTaskState()) {
            case VideoTaskState.DOWNLOADING:
                tvSpeed.setText("Speed : " + item.getSpeedString());
                tvProgress.setText("" + item.getPercentString());
                tvDownloaded.setText("Downloaded : " + item.getDownloadSizeString());
                progress_horizontal.setProgress((int) item.getPercent());
                prefManager.setInt(item.getGroupName()+"_progress", (int) item.getPercent());
                break;
            case VideoTaskState.SUCCESS:
                prefManager.setInt(item.getGroupName()+"_progress", (int) item.getPercent());

                playBtn.setVisibility(View.VISIBLE);
                layoutText.setVisibility(View.GONE);
                layoutProgress.setVisibility(View.GONE);
                tvPlay.setVisibility(View.VISIBLE);

            case VideoTaskState.PAUSE:
                tvSpeed.setText("Speed : " + item.getSpeedString());
                tvProgress.setText("" + item.getPercentString());
                tvDownloaded.setText("Downloaded : " + item.getDownloadSizeString());
                progress_horizontal.setProgress((int) item.getPercent());
                prefManager.setInt(item.getGroupName()+"_progress", (int) item.getPercent());

                break;
            default:
                break;
        }
    }

    // Method to update the list
    public void notifyChanged(List<VideoTaskItem> items, VideoTaskItem item) {
        for (int index = 0; index < mItems.size(); index++) {
            if (mItems.get(index).equals(item)) {
                mItems.set(index, item);
                notifyItemChanged(index);  // Notify the change on a specific item
                break;
            }
        }
    }
}
