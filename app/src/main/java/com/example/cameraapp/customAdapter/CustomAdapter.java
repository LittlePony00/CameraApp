package com.example.cameraapp.customAdapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cameraapp.DB.Photos;
import com.example.cameraapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.CustomViewHolder> {
    Context context;
    List<Photos> photosList = new ArrayList<>();
    Boolean isImageScaled = false;

    public CustomAdapter(Context context) {
        this.context = context;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setPhotosList(List<Photos> photosList) {
        this.photosList = photosList;
        notifyDataSetChanged();
    }

    public void deleteItem(int position){
        this.photosList.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.customlayout, parent, false);
        return new CustomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomViewHolder holder, int position) {
        holder.textView.setText(photosList.get(position).result);
        holder.imageView.setImageURI(Uri.parse(photosList.get(position).path));

        holder.imageView.setOnClickListener(view -> {
            if (view.getScaleX() != 2.0f) {
                view.animate().scaleX(2.0f).scaleY(2.0f).setDuration(500);}
            else {
                view.animate().scaleX(1f).scaleY(1f).setDuration(500);
            }
            isImageScaled = !isImageScaled;
        });
    }

    @Override
    public int getItemCount() {
        return photosList.size();
    }

    public static class CustomViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textView;
        public CustomViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image);
            textView = itemView.findViewById(R.id.layout_title);
        }
    }
}
