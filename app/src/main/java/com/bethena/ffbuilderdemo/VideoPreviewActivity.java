package com.bethena.ffbuilderdemo;

import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;

public class VideoPreviewActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_PATH = "video_path";

    private VideoView videoView;
    private TextView tvVideoInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_preview);

        videoView = findViewById(R.id.preview_video_view);
        tvVideoInfo = findViewById(R.id.tv_video_info);

        String videoPath = getIntent().getStringExtra(EXTRA_VIDEO_PATH);
        if (videoPath != null) {
            File videoFile = new File(videoPath);
            if (videoFile.exists()) {
                // 播放视频
                Uri videoUri = Uri.fromFile(videoFile);
                videoView.setVideoURI(videoUri);
                videoView.start();

                // 获取视频信息
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(videoPath);

                String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                String bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
                String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                String rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
                String frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE);

                StringBuilder info = new StringBuilder();
                info.append("分辨率: ").append(width).append(" x ").append(height).append("\n");
                info.append("码率: ").append(bitrate != null ? Integer.parseInt(bitrate) / 1024 : "未知").append(" Kbps\n");
                info.append("时长: ").append(duration != null ? Integer.parseInt(duration) / 1000 : "未知").append(" 秒\n");
                info.append("旋转角度: ").append(rotation != null ? rotation : "0").append("°\n");
                info.append("帧率: ").append(frameRate != null ? frameRate : "未知").append(" fps");

                tvVideoInfo.setText(info.toString());

                try {
                    retriever.release();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoView != null) {
            videoView.stopPlayback();
        }
    }
}