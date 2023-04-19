package com.example.cameraapp.DB;


import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PhotosDAO {
    @Query("SELECT * from photosDB")
    List<Photos> getAllPhotos();

    @Insert()
    void insert(Photos... photos);

    @Update
    void update(Photos photos);

    @Delete()
    void delete(Photos photos);
}
