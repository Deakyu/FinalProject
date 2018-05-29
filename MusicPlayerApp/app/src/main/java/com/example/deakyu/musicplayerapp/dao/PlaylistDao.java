package com.example.deakyu.musicplayerapp.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.example.deakyu.musicplayerapp.model.Playlist;

import java.util.List;

@Dao
public interface PlaylistDao {

    @Insert
    void insert(Playlist playlist);

    @Query("DELETE FROM playlists")
    void deleteAll();

    @Query("SELECT * FROM playlists")
    LiveData<List<Playlist>> getAllPlaylists();

}
