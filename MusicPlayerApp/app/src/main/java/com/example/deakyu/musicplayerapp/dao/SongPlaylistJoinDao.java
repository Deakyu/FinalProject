package com.example.deakyu.musicplayerapp.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.example.deakyu.musicplayerapp.model.Song;
import com.example.deakyu.musicplayerapp.model.SongPlaylistJoin;

import java.util.List;

@Dao
public interface SongPlaylistJoinDao {

    @Insert
    void insert(SongPlaylistJoin songPlaylistJoin);

    @Query("SELECT songs.* FROM songs, songs_playlists" +
            " WHERE songs.id=songs_playlists.songId" +
            " AND songs_playlists.playlistId = :playlistId")
    LiveData<List<Song>> getSongsByPlaylistId(long playlistId);

}
