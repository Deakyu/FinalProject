package com.example.deakyu.musicplayerapp.database;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.deakyu.musicplayerapp.model.Song;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class StorageUtil {

    private final String STORAGE = "com.example.deakyu.musicplayerapp.STORAGE";
    private SharedPreferences preferences;
    private Context context;

    public StorageUtil(Context context) {
        this.context = context;
    }

    public void storeSongs(List<Song> songs) {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(songs);
        editor.putString("songs", json);
        editor.apply();
    }

    public List<Song> loadSongs() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);

        Gson gson = new Gson();
        String json = preferences.getString("songs", null);
        Type type = new TypeToken<List<Song>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public void storeSongIndex(int index) {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("songIndex", index);
        editor.apply();
    }

    public int loadSongIndex() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        return preferences.getInt("songIndex", -1);
    }

    public void clearCachedSongList() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }

}
