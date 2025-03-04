package com.bethena.ffbuilderdemo;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MediaPickerActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MediaAdapter adapter;
    private List<MediaItem> mediaItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_picker);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        mediaItems = new ArrayList<>();
        adapter = new MediaAdapter(mediaItems, item -> {
            Intent data = new Intent();
            data.setData(item.uri);
            setResult(RESULT_OK, data);
            finish();
        });
        recyclerView.setAdapter(adapter);

        loadVideos();
    }

    private void loadVideos() {
        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA
        };

        try (Cursor cursor = getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Video.Media.DATE_ADDED + " DESC")) {

            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    String path = cursor.getString(dataColumn);

                    MediaItem item = new MediaItem(
                            ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id),
                            name,
                            path
                    );
                    mediaItems.add(item);
                }
                adapter.notifyDataSetChanged();
            }
        }
    }

    private static class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.ViewHolder> {
        private final List<MediaItem> items;
        private final OnItemClickListener listener;

        MediaAdapter(List<MediaItem> items, OnItemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_media, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MediaItem item = items.get(position);
            holder.textView.setText(item.name);
            
            // 加载视频缩略图
            Bitmap thumbnail = null;
            try {
                thumbnail = MediaStore.Video.Thumbnails.getThumbnail(
                        holder.itemView.getContext().getContentResolver(),
                        ContentUris.parseId(item.uri),
                        MediaStore.Video.Thumbnails.MINI_KIND,
                        null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            if (thumbnail != null) {
                holder.imageView.setImageBitmap(thumbnail);
            } else {
                holder.imageView.setImageResource(R.drawable.ic_video_placeholder);
            }
            
            holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView imageView;
            final TextView textView;

            ViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.image_view);
                textView = view.findViewById(R.id.text_view);
            }
        }
    }

    interface OnItemClickListener {
        void onItemClick(MediaItem item);
    }

    static class MediaItem {
        final android.net.Uri uri;
        final String name;
        final String path;

        MediaItem(android.net.Uri uri, String name, String path) {
            this.uri = uri;
            this.name = name;
            this.path = path;
        }
    }
}