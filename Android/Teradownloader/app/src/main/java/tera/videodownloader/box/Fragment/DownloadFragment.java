package tera.videodownloader.box.Fragment;


import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jeffmony.downloader.VideoDownloadConfig;
import com.jeffmony.downloader.common.DownloadConstants;
import com.jeffmony.downloader.listener.DownloadListener;
import com.jeffmony.downloader.listener.IDownloadInfosCallback;
import com.jeffmony.downloader.model.VideoTaskItem;
import com.jeffmony.downloader.utils.LogUtils;
import tera.videodownloader.box.Utils.PrefManager;
import tera.videodownloader.box.R;
import tera.videodownloader.box.Adapter.VideoDownloadListAdapter;
import tera.videodownloader.box.Utils.VideoDownloadManager;
import tera.videodownloader.box.Utils.VideoTaskDBHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class DownloadFragment extends Fragment {

    private View view;


    private String TAG = "MainFragment";
    private PrefManager prefManager;

    private VideoDownloadListAdapter mAdapter;
    private List<VideoTaskItem> mVideoTaskItems = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private RelativeLayout noDownloadLayout;

    private VideoTaskDBHelper dbHelper;

    public DownloadFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        view = inflater.inflate(R.layout.activity_download_list, container, false);

        prefManager = new PrefManager(getContext());

        noDownloadLayout = view.findViewById(R.id.noDownloadLayout);

        mRecyclerView = view.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));  // Use a linear layout manager

        dbHelper = new VideoTaskDBHelper(getContext());

        // Initialize the adapter with an empty list of tasks
        mAdapter = new VideoDownloadListAdapter(getContext(), mVideoTaskItems);
        mRecyclerView.setAdapter(mAdapter);


        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String downloadDirPath = downloadsDir.getAbsolutePath();

        VideoDownloadConfig config = new VideoDownloadManager.Build(getContext())
                .setCacheRoot(downloadDirPath)
                .setTimeOut(DownloadConstants.READ_TIMEOUT, DownloadConstants.CONN_TIMEOUT)
                .setConcurrentCount(DownloadConstants.CONCURRENT)
                .setIgnoreCertErrors(true)
                .setShouldM3U8Merged(true)
                .buildConfig();
        VideoDownloadManager.getInstance().initConfig(config);
        VideoDownloadManager.getInstance().setGlobalDownloadListener(mListener);



        // Initialize data when the activity is created
        // Load previously saved videos from SharedPreferences

        // Check if we received data from MainActivity to add a new video task
//        String videoUrl = getIntent().getStringExtra("video_url");
//        String thumbnailUrl = getIntent().getStringExtra("thumbnail_url");
//        String videoName = getIntent().getStringExtra("video_name");
//        String videoGroup = getIntent().getStringExtra("video_group");
//
//        Log.d("ymgs thunb", thumbnailUrl + "");
//
//        if (videoUrl != null && !videoUrl.isEmpty()) {
//            VideoTaskItem newItem = new VideoTaskItem(videoUrl, thumbnailUrl, videoName, videoGroup);
//
//            dbHelper.addVideoTaskItem(newItem);
//
//            loadVideoList(); // Reload the list from database
//
//        }
//
//        loadVideoList(); // Reload the list from database

        mAdapter.setOnItemClickListener(new VideoDownloadListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, VideoTaskItem item, int position) {
                if (item.isInitialTask()) {
                    // Start the download if the task is in the initial state
                    VideoDownloadManager.getInstance().startDownload(item);
                } else if (item.isRunningTask()) {
                    // Pause the download if it's running
                    VideoDownloadManager.getInstance().pauseDownloadTask(item.getUrl());
                } else if (item.isInterruptTask()) {
                    // Resume the download if it was interrupted
                    VideoDownloadManager.getInstance().resumeDownload(item.getUrl());
                }
            }
        });


        return view;
    }

    private void loadVideoList() {
        // Load video tasks from the SQLite database
        mVideoTaskItems = dbHelper.getAllVideoTasks();

        // Check if the list is empty
        if (mVideoTaskItems == null || mVideoTaskItems.isEmpty()) {
            // Show a toast message if there is no data
            noDownloadLayout.setVisibility(View.VISIBLE);
        } else {
            // Update the adapter with the new data
            mAdapter.updateData(mVideoTaskItems);
            noDownloadLayout.setVisibility(View.GONE);
        }
    }


    private long mLastProgressTimeStamp;
    private long mLastSpeedTimeStamp;

    private DownloadListener mListener = new DownloadListener() {

        @Override
        public void onDownloadDefault(VideoTaskItem item) {
            LogUtils.w(TAG,"onDownloadDefault: " + item);
            notifyChanged(item);
        }

        @Override
        public void onDownloadPending(VideoTaskItem item) {
            LogUtils.w(TAG,"onDownloadPending: " + item);
            notifyChanged(item);
        }

        @Override
        public void onDownloadPrepare(VideoTaskItem item) {
            LogUtils.w(TAG,"onDownloadPrepare: " + item);
            notifyChanged(item);
        }

        @Override
        public void onDownloadStart(VideoTaskItem item) {
            LogUtils.w(TAG,"onDownloadStart: " + item);
            notifyChanged(item);
        }

        @Override
        public void onDownloadProgress(VideoTaskItem item) {
            long currentTimeStamp = System.currentTimeMillis();
            if (currentTimeStamp - mLastProgressTimeStamp > 1000) {
                LogUtils.w(TAG, "onDownloadProgress: " + item.getPercentString() + ", curTs=" + item.getCurTs() + ", totalTs=" + item.getTotalTs());
                notifyChanged(item);
                mLastProgressTimeStamp = currentTimeStamp;
                //mDatabaseHelper.updateVideoTask(item);
            }
        }

        @Override
        public void onDownloadSpeed(VideoTaskItem item) {
            long currentTimeStamp = System.currentTimeMillis();
            if (currentTimeStamp - mLastSpeedTimeStamp > 1000) {
                notifyChanged(item);
                mLastSpeedTimeStamp = currentTimeStamp;
            }
        }

        @Override
        public void onDownloadPause(VideoTaskItem item) {
            LogUtils.w(TAG,"onDownloadPause: " + item.getUrl());
            notifyChanged(item);
        }

        @Override
        public void onDownloadError(VideoTaskItem item) {
            LogUtils.w(TAG,"onDownloadError: " + item.getUrl());
            notifyChanged(item);
        }

        @Override
        public void onDownloadSuccess(VideoTaskItem item) {
            LogUtils.w(TAG,"onDownloadSuccess: " + item);
            notifyChanged(item);
        }
    };

    private void notifyChanged(final VideoTaskItem item) {
        getActivity().runOnUiThread(() -> mAdapter.notifyChanged(mVideoTaskItems, item));
    }

    private IDownloadInfosCallback mInfosCallback =
            new IDownloadInfosCallback() {
                @Override
                public void onDownloadInfos(List<VideoTaskItem> items) {
                    for (VideoTaskItem item : items) {
                        notifyChanged(item);
                    }
                }
            };



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        VideoDownloadManager.getInstance().removeDownloadInfosCallback(mInfosCallback);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadVideoList();
    }
}