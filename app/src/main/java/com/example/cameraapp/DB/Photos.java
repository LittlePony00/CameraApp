package com.example.cameraapp.DB;

import android.net.Uri;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "photosDB")
public class Photos {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo
    public String path;

    @ColumnInfo
    public String result;
}
