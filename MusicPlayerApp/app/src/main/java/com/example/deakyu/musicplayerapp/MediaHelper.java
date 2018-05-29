package com.example.deakyu.musicplayerapp;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.example.deakyu.musicplayerapp.model.Song;

import java.util.ArrayList;
import java.util.List;

public class MediaHelper {
    public static List<Song> getMusicFromStorage(Context activityContext) {
        List<Song> songList = new ArrayList<>();

        ContentResolver contentResolver = activityContext.getContentResolver();
        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = contentResolver.query(songUri, null, null, null, null);

        if(cursor != null && cursor.moveToFirst()) {
            int id = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int title = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artist = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);

            do {
                long currentId = cursor.getLong(id);
                String currentTitle = cursor.getString(title);
                String currentArtist = cursor.getString(artist);
                songList.add(new Song(currentId, currentTitle, currentArtist));
            } while(cursor.moveToNext());
        }
        return songList;
    }
}