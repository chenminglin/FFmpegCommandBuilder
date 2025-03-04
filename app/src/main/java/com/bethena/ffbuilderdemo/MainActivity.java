package com.bethena.ffbuilderdemo;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.app.ProgressDialog;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.bethena.ffmpegcmdbuilder.FFmpegCommandBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private TextView tvResult;
    private Button btnPickVideo;
    private Button btnConvert;
    private VideoView videoView;
    private String selectedVideoPath;
    private ProgressDialog progressDialog;
    
    private final ActivityResultLauncher<Intent> videoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedVideoUri = result.getData().getData();
                    selectedVideoPath = selectedVideoUri.toString();
                    if (selectedVideoPath != null) {
                        // 播放选中的视频
                        videoView.setVideoURI(selectedVideoUri);
                        videoView.start();
                        btnConvert.setEnabled(true);
                    } else {
                        Toast.makeText(this, "无法获取视频文件路径", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvResult = findViewById(R.id.tv_result);
        btnPickVideo = findViewById(R.id.btn_pick_video);
        btnConvert = findViewById(R.id.btn_convert);
        videoView = findViewById(R.id.video_view);

        btnPickVideo.setOnClickListener(v -> checkPermissionAndPickVideo());
        btnConvert.setOnClickListener(v -> convertVideo());
        btnConvert.setEnabled(false);
    }

    private void checkPermissionAndPickVideo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_VIDEO},
                        PERMISSION_REQUEST_CODE);
            } else {
                openVideoPicker();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                openVideoPicker();
            }
        }
    }

    private void openVideoPicker() {
        Intent intent = new Intent(this, MediaPickerActivity.class);
        videoPickerLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openVideoPicker();
            } else {
                Toast.makeText(this, "需要存储权限才能选择视频文件", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void convertVideo() {
        if (selectedVideoPath == null) {
            Toast.makeText(this, "请先选择视频文件", Toast.LENGTH_SHORT).show();
            return;
        }

        // 将视频文件复制到缓存目录
        File cacheDir = getCacheDir();
        String fileName = "input_" + System.currentTimeMillis() + ".mp4";
        File cacheFile = new File(cacheDir, fileName);
        try {
            ContentResolver contentResolver = getContentResolver();
            Uri videoUri = Uri.parse(selectedVideoPath);
            InputStream inputStream = contentResolver.openInputStream(videoUri);
            FileOutputStream fos = new FileOutputStream(cacheFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            inputStream.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "复制文件失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        String outputPath = new File(cacheDir, "output_" + System.currentTimeMillis() + ".mp4").getAbsolutePath();

        // 使用构建者模式创建FFmpeg命令
        String command = FFmpegCommandBuilder.create()
                .input(cacheFile.getAbsolutePath())
                .videoCodec("h264")  // 使用基础的 h264 编码器
                .audioCodec("aac")
                .videoBitRate("1M")
                .audioBitRate("128k")
                .resolution(640, 360)
                .frameRate(30)
                .output(outputPath)
                .build();
        Log.d("FFmpeg", command);
        // 创建并显示进度对话框
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setTitle("视频转码中");
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
        progressDialog.show();

        // 获取视频时长
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(cacheFile.getAbsolutePath());
        String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long totalDuration = Long.parseLong(durationStr);
        try {
            retriever.release();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 执行FFmpeg命令
        FFmpegKit.executeAsync(command, session -> {
            // 关闭进度对话框
            runOnUiThread(() -> {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            });
            // 删除缓存文件
            cacheFile.delete();
            // 命令执行完成的回调
            if (session.getReturnCode().isValueSuccess()) {
                runOnUiThread(() -> {
                    tvResult.setText("视频转换成功！输出文件：" + outputPath);
                    // 启动视频预览页面
                    Intent intent = new Intent(MainActivity.this, VideoPreviewActivity.class);
                    intent.putExtra(VideoPreviewActivity.EXTRA_VIDEO_PATH, outputPath);
                    startActivity(intent);
                });
            } else {
                new File(outputPath).delete(); // 删除失败的输出文件
                runOnUiThread(() -> tvResult.setText("视频转换失败：" + session.getFailStackTrace()));
            }
        }, log -> {
            // 命令执行过程中的日志回调
            Log.d("FFmpeg", log.getMessage());
        }, statistics -> {
            Log.d("FFmpeg", statistics.toString());
            // 更新进度
            if (statistics.getTime() > 0 && totalDuration > 0) {
                double progress = (statistics.getTime() * 100.0f) / totalDuration;
                runOnUiThread(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.setProgress((int) progress);
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}