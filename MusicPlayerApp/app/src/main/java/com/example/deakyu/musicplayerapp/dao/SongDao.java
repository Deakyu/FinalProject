package com.example.deakyu.musicplayerapp.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.example.deakyu.musicplayerapp.model.Song;

import java.util.List;

@Dao
public interface SongDao {

    @Insert
    void insert(Song song);

    @Query("DELETE FROM songs")
    void deleteAll();

    @Query("SELECT * FROM songs")
    LiveData<List<Song>> getAllSongs();

}
