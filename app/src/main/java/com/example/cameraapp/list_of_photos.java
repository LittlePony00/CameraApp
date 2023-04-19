package com.example.cameraapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

import com.example.cameraapp.DB.Photos;
import com.example.cameraapp.DB.PhotosDB;
import com.example.cameraapp.customAdapter.CustomAdapter;

import java.util.ArrayList;
import java.util.List;

public class list_of_photos extends AppCompatActivity {
    PhotosDB db;
    CustomAdapter customAdapter;
    Button back_button, delete;
    List<Photos> photosList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_of_photos);

        initRecycleView();
        new_photo();
    }

    private void initRecycleView() {
        RecyclerView recyclerView = findViewById(R.id.recycle_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        customAdapter = new CustomAdapter(this);
        recyclerView.setAdapter(customAdapter);
        customAdapter.setPhotosList(photosList);
        back_button = findViewById(R.id.backButton);
        delete = findViewById(R.id.delete);

        back_button.setOnClickListener(view -> {
            startActivity(new Intent(list_of_photos.this, MainActivity.class));
        });

        delete.setOnClickListener(view -> {
            if (photosList.size() != 0) {
                db.photosDAO().delete(photosList.get(0));
            }

            photosList = db.photosDAO().getAllPhotos();
            customAdapter.setPhotosList(photosList);
        });
    }

    private void new_photo() {
        db = PhotosDB.getInstance(this.getApplicationContext());
        photosList = db.photosDAO().getAllPhotos();
        customAdapter.setPhotosList(photosList);
    }
}