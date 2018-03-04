package com.technorabit.ibeyonde.adaptor;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import com.brightcove.player.event.Event;
import com.brightcove.player.event.EventEmitter;
import com.brightcove.player.event.EventListener;
import com.brightcove.player.event.EventType;
import com.brightcove.player.model.Video;
import com.brightcove.player.view.BrightcoveExoPlayerVideoView;
import com.brightcove.player.view.BrightcoveVideoView;
import com.bumptech.glide.Glide;
import com.dms.datalayerapi.network.Http;
import com.dms.datalayerapi.util.CommonPoolExecutor;
import com.dms.datalayerapi.util.GetUrlMaker;
import com.technorabit.ibeyonde.Dashboard;
import com.technorabit.ibeyonde.LoginActivity;
import com.technorabit.ibeyonde.R;
import com.technorabit.ibeyonde.connection.HttpClientManager;
import com.technorabit.ibeyonde.constants.AppConstants;
import com.technorabit.ibeyonde.costom.VideoEnabledWebChromeClient;
import com.technorabit.ibeyonde.costom.VideoEnabledWebView;
import com.technorabit.ibeyonde.fragment.TabFragment;
import com.technorabit.ibeyonde.fragment.dailog.BaseFragmentDialog;
import com.technorabit.ibeyonde.model.LoginRes;
import com.technorabit.ibeyonde.model.VideoItem;
import com.technorabit.ibeyonde.util.SharedUtil;
import com.technorabit.ibeyonde.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.concurrent.Executor;

/**
 * Created by raja on 18/02/18.
 */

public class VideoListAdaptor extends RecyclerView.Adapter<VideoListAdaptor.VideoViewHolder> {


    private Context context;
    private TabFragment.Type type;
    private ArrayList<VideoItem> videoItems = new ArrayList<>();

    public VideoListAdaptor(Context context, TabFragment.Type type) {
        this.type = type;
        this.context = context;
    }


    @Override
    public VideoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.video_item, parent, false);
        return new VideoViewHolder(itemView);
    }

    public void setVideoItems(ArrayList<VideoItem> videoItems) {
        this.videoItems = videoItems;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(VideoViewHolder holder, int position) {
        VideoItem videoItem = videoItems.get(position);
        holder.device_name.setText(videoItem.uuid + ":" + videoItem.device_name);
        holder.history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        if (type == TabFragment.Type.LIVE) {
            initLiveCall(videoItem, holder);
            holder.history.setVisibility(View.GONE);
            holder.img_item.setVisibility(View.GONE);
            holder.video_item.setVisibility(View.VISIBLE);
        } else {
            initMotionCall(videoItem, holder.img_item);
            holder.history.setVisibility(View.VISIBLE);
            holder.img_item.setVisibility(View.VISIBLE);
            holder.video_item.setVisibility(View.GONE);
        }

    }

    private void initMotionCall(VideoItem videoItem, final ImageView img_item) {
        GetUrlMaker getUrlMaker = GetUrlMaker.getMaker();
        HttpClientManager client = HttpClientManager.get(context);
        client.setUsername(SharedUtil.get(context).getString("username"));
        client.setPassword(SharedUtil.get(context).getString("password"));
        client.diskCacheEnable(false);
        String url = AppConstants.LATEST_ALERTS.replace(AppConstants.REPLACER, "");
        url = url + "&uuid=" + videoItem.uuid;
        client.new NetworkTask<Void, String>(null, Http.GET) {
            @Override
            protected void onPostExecute(final String liveUrl) {
                super.onPostExecute(liveUrl);
                if (liveUrl != null) {
                    img_item.setVisibility(View.VISIBLE);
                    loadToImageView(liveUrl, img_item);
                } else {

                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getUrlMaker.getPathForGetUrl(url));
    }

    private void loadToImageView(final String liveUrl, final ImageView img_item) {
        try {
            final JSONArray jsonArray = new JSONArray(liveUrl);
            CommonPoolExecutor.get().startDownload(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        Message msg = new Message();
                        try {
                            msg.obj = new ViewHolder(img_item, jsonArray.getJSONArray(i).getString(0));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        handler.sendMessage(msg);
                        try {
                            Thread.sleep(i * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    class ViewHolder {
        public ViewHolder(ImageView imageView, String url) {
            this.imageView = imageView;
            this.url = url;
        }

        public ImageView imageView;
        public String url;
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            ViewHolder img_item = (ViewHolder) msg.obj;
            Glide.with(context).load(img_item.url).into(img_item.imageView);
            super.handleMessage(msg);
        }
    };


//    @Override
//    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
//        super.onDetachedFromRecyclerView(recyclerView);
//        int childCount = recyclerView.getChildCount();
//        //We need to stop the player to avoid a potential memory leak.
//        for (int i = 0; i < childCount; i++) {
//            VideoViewHolder holder = (VideoViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
//            if (holder != null && holder.videoView != null) {
//                holder.videoView.stopPlayback();
//            }
//        }
//    }

    @Override
    public void onViewAttachedToWindow(VideoViewHolder holder) {
        super.onViewAttachedToWindow(holder);
//        holder.videoView.start();
    }

    @Override
    public void onViewDetachedFromWindow(VideoViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
//        holder.videoView.stopPlayback();
    }

    private void initLiveCall(final VideoItem videoItem, final VideoViewHolder holder) {
        GetUrlMaker getUrlMaker = GetUrlMaker.getMaker();
        HttpClientManager client = HttpClientManager.get(context);
        client.setUsername(SharedUtil.get(context).getString("username"));
        client.setPassword(SharedUtil.get(context).getString("password"));
        client.diskCacheEnable(false);
        String url = AppConstants.LIVE_VIEW.replace(AppConstants.REPLACER, "");
        url = url + "&uuid=" + videoItem.uuid + "&quality=BINI";
        client.new NetworkTask<Void, String>(null, Http.GET) {
            @Override
            protected void onPostExecute(final String liveUrl) {
                super.onPostExecute(liveUrl);
                if (liveUrl != null) {
                    holder.videoView.setVisibility(View.VISIBLE);
                    try {
                        holder.videoView.loadUrl(URLEncoder.encode(liveUrl,"UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
//                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(liveUrl));
//                    context.startActivity(browserIntent);
                } else {

                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getUrlMaker.getPathForGetUrl(url));
    }

    @Override
    public int getItemCount() {
        return videoItems.size();
    }

    class VideoViewHolder extends RecyclerView.ViewHolder {
        private final VideoEnabledWebChromeClient webChromeClient;
        public VideoEnabledWebView videoView;
        public TextView history;
        public ImageView img_item;
        public TextView device_name;
        public FrameLayout video_item;

        public VideoViewHolder(View itemView) {
            super(itemView);
            device_name = itemView.findViewById(R.id.device_name);
            img_item = itemView.findViewById(R.id.img_item);
            history = itemView.findViewById(R.id.history);
            video_item = itemView.findViewById(R.id.video_item);
            videoView = new VideoEnabledWebView(context);
            videoView.clearCache(true);
            videoView.setWebChromeClient(new WebChromeClient());
            videoView.setWebViewClient(new WebViewClient());
            WebSettings set = videoView.getSettings();
            set.setAllowFileAccess(true);
            set.setAllowFileAccessFromFileURLs(true);
            set.setAllowUniversalAccessFromFileURLs(true);
            set.setJavaScriptEnabled(true);
            set.setBuiltInZoomControls(true);
            webChromeClient = new VideoEnabledWebChromeClient() // See all available constructors...
            {
                @Override
                public void onProgressChanged(WebView view, int progress)
                {
                    // Your code...
                }
            };
            webChromeClient.setOnToggledFullscreen(new VideoEnabledWebChromeClient.ToggledFullscreenCallback() {
                @Override
                public void toggledFullscreen(boolean fullscreen) {

                }
            });
            videoView.setWebChromeClient(webChromeClient);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            video_item.addView(videoView, params);
        }
    }

}
