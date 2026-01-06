package tera.videodownloader.box.Fragment;


import static tera.videodownloader.box.Config.API_KEY;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.jeffmony.downloader.model.VideoTaskItem;
import com.solodroid.ads.sdk.ui.MediumNativeAdView;
import tera.videodownloader.box.Activity.MainActivity;
import tera.videodownloader.box.Activity.PlayerActivity;
import tera.videodownloader.box.Config;
import tera.videodownloader.box.Utils.AdsManager;
import tera.videodownloader.box.Utils.PrefManager;
import tera.videodownloader.box.R;
import tera.videodownloader.box.Utils.VideoDownloadManager;
import tera.videodownloader.box.Utils.VideoTaskDBHelper;
import com.ymg.ymgdevelopers.YmgTools;

import org.json.JSONException;
import org.json.JSONObject;


public class HistoryFragment extends Fragment {

    private View view;
    private TextView tvTitle;
    private TextView tvSize;
    private TextView btnFetch;
    private ImageView myImageView;
    private RelativeLayout btnDownload;
    private RelativeLayout btnPlay;
    private RelativeLayout btnCopy;
    private CardView itemLayout;
    private EditText editTextURL;
    private String TAG = "MainFragment";
    private PrefManager prefManager;
    private VideoTaskDBHelper dbHelper;
    private String url;
    private Dialog dialog;
    private ProgressBar progressBar;
    private AdsManager adsManager;

    public HistoryFragment() {
        // Required empty public constructor
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        view = inflater.inflate(R.layout.fragment_gallery, container, false);

        prefManager = new PrefManager(getContext());
        dbHelper = new VideoTaskDBHelper(getContext());
        adsManager = new AdsManager(getActivity());
        adsManager.loadInterstitialAd(true, Config.INTERSTITIAL_AD_INTERVAL);
        MediumNativeAdView mediumNativeAdView = view.findViewById(R.id.mediumNativeAdView);
        adsManager.loadNativeAdViewFrgment(true, mediumNativeAdView);

        myImageView = view.findViewById(R.id.myImageView);
        btnFetch = view.findViewById(R.id.btnFetch);
        tvTitle = view.findViewById(R.id.tvTitle);
        tvSize = view.findViewById(R.id.tvSize);
        btnDownload = view.findViewById(R.id.btnDownload);
        btnPlay = view.findViewById(R.id.btnPlay);
        editTextURL = view.findViewById(R.id.editTextURL);
        itemLayout = view.findViewById(R.id.itemLayout);
        progressBar = view.findViewById(R.id.progressBar);
        btnCopy = view.findViewById(R.id.btnCopy);


        btnFetch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editTextURL.clearFocus();
                itemLayout.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                url = editTextURL.getText().toString();
                fetchFile(url);
                adsManager.showInterstitialAd();
            }
        });

        editTextURL.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Detect touch events on the drawable end (right side)
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    // Check if the touch event is on the drawable end
                    if (event.getRawX() >= (editTextURL.getRight() - editTextURL.getCompoundDrawables()[2].getBounds().width())) {
                        // Trigger the drawable click action
                        handleDrawableClick();
                        return true; // Indicate that we have handled the touch event
                    }
                }
                return false;
            }
        });



        return view;
    }

    private void handleDrawableClick() {
        // Get clipboard contents
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null && clip.getItemCount() > 0) {
            String pasteData = clip.getItemAt(0).getText().toString();
            editTextURL.setText(pasteData);
            hideKeyboard();
        }
    }

    private void hideKeyboard() {
        editTextURL.clearFocus();
        // Get the InputMethodManager system service
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getActivity() != null && imm != null && getActivity().getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
    }



    private void fetchFile(String url){
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("url", url);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        byte[] decodedBytes = Base64.decode(API_KEY, Base64.DEFAULT);
        String dString = new String(decodedBytes);
        // Create the request using Volley
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                dString,
                jsonBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Get the metadata object from the response
                            JSONObject metadata = response.getJSONObject("metadata");

                            // Extract the required fields
                            String serverFilename = metadata.getString("server_filename");
                            int size = metadata.getInt("size");
                            String dlink = metadata.getString("dlink");
                            String url3 = metadata.getJSONObject("thumbs").getString("url3");
                            String fastdlink = metadata.getString("fastdlink");
                            String fdlink = metadata.getString("fdlink");
                            String streamingUrl = response.getJSONObject("streamingUrl").getString("streaming_url");

                            prefManager.setInt(serverFilename+"_size", size);

                            if (serverFilename.contains("mp4") || serverFilename.contains("mkv") || serverFilename.contains("3gp") ||
                            serverFilename.contains("avi") || serverFilename.contains("mov") || serverFilename.contains("webm") ||
                            serverFilename.contains("mpeg") || serverFilename.contains("mpg") || serverFilename.contains("ts"))
                            {
                                setDataLayout(serverFilename, size, dlink, url3, streamingUrl);
                            } else {
                                showErrorDialog();
                            }



                        } catch (JSONException e) {
                            e.printStackTrace();
                            showErrorDialog();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Handle the error
                        Log.e("TAG", "Error: " + error);
                        Toast.makeText(getContext(), "Request failed", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                }
        );

        // Set the retry policy (increase timeout if needed)
        int socketTimeout = 20000;
        RetryPolicy policy = new DefaultRetryPolicy(
                socketTimeout,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        );

        jsonObjectRequest.setRetryPolicy(policy);

        // Add the request to the request queue
        Volley.newRequestQueue(getContext()).add(jsonObjectRequest);
    }

    private void showErrorDialog(){
        new AlertDialog.Builder(getContext())  // Use getContext() if inside a Fragment
                .setTitle("File is not a Video File")
                .setMessage("The selected file doesn't appear to be a supported video format.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editTextURL.setText("");
                        progressBar.setVisibility(View.GONE);
                    }
                })
                .show();
    }

    @SuppressLint({"SetTextI18n", "ClickableViewAccessibility"})
    private void setDataLayout(String serverFilename, int size, String dlink, String url3, String streamingUrl) {
        itemLayout.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);

        Glide.with(getContext()).load(url3).placeholder(R.drawable.placeholder).into(myImageView);
        tvTitle.setText(serverFilename);
        tvSize.setText("Size : "+formatFileSize(size));

        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                YmgTools.copyText(getContext(), dlink);
                Toast.makeText(getContext(), "Direct Download link Copied !", Toast.LENGTH_SHORT).show();
                adsManager.showInterstitialAd();
            }
        });

        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (streamingUrl.contains("m3u8")){

                    if (dbHelper.isVideoAlreadyAdded(serverFilename)) {
                        new AlertDialog.Builder(getContext())  // Use getContext() if inside a Fragment
                                .setTitle("File Already in Download List")
                                .setMessage("This video is already in the download list. Do you want to delete the old one and download again?")
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Delete the old item and add the new one
                                        VideoTaskItem newItem = new VideoTaskItem(streamingUrl, url3, serverFilename, serverFilename);
                                        deleteOldVideoAndDownload(newItem);

                                    }
                                })
                                .setNegativeButton("No", null) // Do nothing on "No"
                                .show();
                    }else {
                        VideoTaskItem newItem = new VideoTaskItem(streamingUrl, url3, serverFilename, serverFilename);
                        dbHelper.addVideoTaskItem(newItem);
                        VideoDownloadManager.getInstance().startDownload(newItem);
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).swipeToSecondFragment();
                        }
                    }
                    adsManager.showInterstitialAd();
                }else {
                    showAboutDialog();
                    fetchFileVideo(url, "Download");
                }
            }
        });

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (streamingUrl.contains("m3u8")){
                    //open player
                    Intent intent = new Intent(getActivity(), PlayerActivity.class);
                    intent.putExtra("url", streamingUrl);
                    startActivity(intent);
                }else {
                    showAboutDialog();
                    fetchFileVideo(url, "Play");
                }
            }
        });

    }

    private void deleteOldVideoAndDownload(VideoTaskItem newItem) {
        // Delete the old video from the database based on group name
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(VideoTaskDBHelper.TABLE_NAME, VideoTaskDBHelper.COLUMN_GROUP_NAME + " = ?", new String[]{newItem.getGroupName()});
        db.close();
        // Add the new video to the database
        dbHelper.addVideoTaskItem(newItem);
        // Start the download
        VideoDownloadManager.getInstance().startDownload(newItem);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).swipeToSecondFragment();
        }
    }



    private void showAboutDialog() {
        dialog = new Dialog(getActivity(), R.style.DialogCustomTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        dialog.setContentView(R.layout.dialog_loading);

        AppCompatButton btn_done = dialog.findViewById(R.id.btn_done);
        btn_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }


    private void fetchFileVideo(String url, String type){
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("url", url);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        byte[] decodedBytes = Base64.decode(API_KEY, Base64.DEFAULT);
        String dString = new String(decodedBytes);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                dString,
                jsonBody,
                response -> {
                    try {
                        // Get the metadata object from the response
                        JSONObject metadata = response.getJSONObject("metadata");
                        String serverFilename = metadata.getString("server_filename");
                        String url3 = metadata.getJSONObject("thumbs").getString("url3");
                        String streamingUrl = response.getJSONObject("streamingUrl").getString("streaming_url");

                        if (streamingUrl.contains("m3u8")){
                            dialog.dismiss();
                            if (type.contains("Download")){
                                if (dbHelper.isVideoAlreadyAdded(serverFilename)) {
                                    new AlertDialog.Builder(getContext())  // Use getContext() if inside a Fragment
                                            .setTitle("File Already in Download List")
                                            .setMessage("This video is already in the download list. Do you want to delete the old one and download again?")
                                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    // Delete the old item and add the new one
                                                    VideoTaskItem newItem = new VideoTaskItem(streamingUrl, url3, serverFilename, serverFilename);
                                                    deleteOldVideoAndDownload(newItem);
                                                }
                                            })
                                            .setNegativeButton("No", null) // Do nothing on "No"
                                            .show();
                                }else {
                                    VideoTaskItem newItem = new VideoTaskItem(streamingUrl, url3, serverFilename, serverFilename);
                                    dbHelper.addVideoTaskItem(newItem);
                                    VideoDownloadManager.getInstance().startDownload(newItem);
                                    if (getActivity() instanceof MainActivity) {
                                        ((MainActivity) getActivity()).swipeToSecondFragment();
                                    }
                                }
                            }else{
                                Intent intent = new Intent(getActivity(), PlayerActivity.class);
                                intent.putExtra("url", streamingUrl);
                                startActivity(intent);
                            }
                        }else {
                            fetchFileVideo(url, type);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    // Handle the error
                    Log.e("TAG", "Error: " + error);
                    Toast.makeText(getContext(), "Request failed", Toast.LENGTH_SHORT).show();
                }
        );

        // Set the retry policy (increase timeout if needed)
        int socketTimeout = 20000;
        RetryPolicy policy = new DefaultRetryPolicy(
                socketTimeout,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        );

        jsonObjectRequest.setRetryPolicy(policy);

        // Add the request to the request queue
        Volley.newRequestQueue(getContext()).add(jsonObjectRequest);
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
    public void onResume() {
        super.onResume();
    }
}