package com.tdm.sigmaexossaiexample;


import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Tracks;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.ads.AdsLoader;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionOverride;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.MimeTypes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.sigma.drm.SigmaHelper;
import com.tdm.adstracking.AdsTracking;
import com.tdm.adstracking.FullLog;
import com.tdm.adstracking.core.listener.ResponseInitListener;
import com.tdm.adstracking.define.LogLevel;

public class MainActivity extends AppCompatActivity implements Player.Listener {
    private final String TAG = "MainActivity";
    ExoPlayer player;
    PlayerView playerView;
    public String sourceUrl = "https://cdn-lrm-test.sigma.video/manifest/origin04/scte35-av4s-sigma-drm/master.m3u8?sigma.dai.adsEndpoint=d7078543-b7b8-47d9-807c-b7429c05c81d";
    EditText editTextSource = null;
    Button reloadButton = null;
    private Context mainContext = null;
    Player.Listener playerListener = null;

    private String DRM_MERCHANT_ID = "d5321abd-6676-4bc1-a39e-6bb763029e54";
    private String DRM_SESSION_ID = "SESSION_ID";
    private String DRM_USER_ID = "USER_ID";
    private String DRM_APP_ID = "7444c496-67be-4998-8b29-82152668ba20";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AdsTracking.getInstance().startServer();

        mainContext = this;

        setContentView(R.layout.activity_main);
        playerView = findViewById(R.id.player_view_id);
        editTextSource = findViewById(R.id.source_hls);
        reloadButton = findViewById(R.id.reload_player);

        editTextSource.setText(sourceUrl);

        // Initialize the ProgressDialogManager to fake loading
        ProgressDialogManager.getInstance().init(this);

        reloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleReloadPlayer();
            }
        });
        playerView.post(new Runnable() {
            @Override
            public void run() {
                initAdsTracking();
            }
        });
    }

    private void initAdsTracking() {
        AdsTracking.getInstance().init(
                this,
                playerView,
                sourceUrl,
                new ResponseInitListener() {
                    @Override
                    public void onInitSuccess(String url) {
                        ProgressDialogManager.getInstance().hideLoading();
                        configPlayer(url);
                    }

                    @Override
                    public void onInitFailed(String url, String msg) {
                        Toast.makeText(mainContext, msg, Toast.LENGTH_SHORT).show();
                        ProgressDialogManager.getInstance().hideLoading();
                    }
                });
    }

    private void configPlayer(String url) {
        SigmaHelper.instance().configure(DRM_MERCHANT_ID, DRM_APP_ID, DRM_USER_ID, DRM_SESSION_ID);

        player = new ExoPlayer.Builder(this).build();
        AdsTracking.getInstance().initPlayer(player);
        playerView.setPlayer(player);
        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(url));
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(true);

        if (playerListener != null) {
            player.removeListener(playerListener);
        }

        playerListener = new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e(TAG + "_onError=>:", error.getMessage());
                ProgressDialogManager.getInstance().hideLoading();
            }
        };
        player.addListener(playerListener);
    }

    private void handleReloadPlayer() {
        ProgressDialogManager.getInstance().showLoading();

        sourceUrl = editTextSource.getText().toString();

        player.stop();
        player.release();
        AdsTracking.getInstance().destroy();

        //time out to fake load content
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            initAdsTracking();
        }, 1000);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onDestroy() {
        AdsTracking.getInstance().destroy();
        player.removeListener(playerListener);
        super.onDestroy();
    }


}