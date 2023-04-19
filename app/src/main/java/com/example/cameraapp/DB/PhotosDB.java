package com.example.cameraapp.DB;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Photos.class}, version = 1)
public abstract class PhotosDB extends RoomDatabase {
    public abstract PhotosDAO photosDAO();

    private static PhotosDB INSTANCE;

    public static PhotosDB getInstance(Context context){

        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(), PhotosDB.class, "NOTE_DB")
                    .allowMainThreadQueries().build();
        }

        return INSTANCE;
    }
}
