package com.bethena.ffbuilderdemo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class VideoPreviewActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_PATH = "video_path";

    private VideoView videoView;
    private TextView tvVideoInfo;
    private Button btnSaveToGallery;
    private String videoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_preview);

        videoView = findViewById(R.id.preview_video_view);
        tvVideoInfo = findViewById(R.id.tv_video_info);
        btnSaveToGallery = findViewById(R.id.btn_save_to_gallery);

        videoPath = getIntent().getStringExtra(EXTRA_VIDEO_PATH);
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

                // 设置保存按钮点击事件
                btnSaveToGallery.setOnClickListener(v -> saveVideoToGallery());
            }
        }
    }

    private void saveVideoToGallery() {
        if (videoPath == null) {
            Toast.makeText(this, "视频文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        File videoFile = new File(videoPath);
        if (!videoFile.exists()) {
            Toast.makeText(this, "视频文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, "FFmpeg_" + System.currentTimeMillis() + ".mp4");
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/FFmpeg");
            values.put(MediaStore.Video.Media.IS_PENDING, 1);
        }

        ContentResolver resolver = getContentResolver();
        Uri collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri uri = resolver.insert(collection, values);

        if (uri != null) {
            try (OutputStream os = resolver.openOutputStream(uri);
                 FileInputStream fis = new FileInputStream(videoFile)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear();
                    values.put(MediaStore.Video.Media.IS_PENDING, 0);
                    resolver.update(uri, values, null, null);
                }

                Toast.makeText(this, "视频已保存到相册", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                resolver.delete(uri, null, null);
                Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
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