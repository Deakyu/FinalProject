package com.example.deakyu.musicplayerapp.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;

@Entity(tableName = "songs_playlists",
        primaryKeys = {"songId", "playlistId"},
        foreignKeys = {
            @ForeignKey(entity = Song.class,
                        parentColumns = "id",
                        childColumns = "songId"),
            @ForeignKey(entity = Playlist.class,
                        parentColumns = "id",
                        childColumns = "playlistId")
        })
public class SongPlaylistJoin {

    @ColumnInfo(name = "songId") private long songId;
    @ColumnInfo(name = "playlistId") private long playlistId;

    public SongPlaylistJoin(long songId, long  playlistId) {
        this.songId = songId;
        this.playlistId = playlistId;
    }

    public long getSongId() { return songId; }
    public void setSongId(long songId) { this.songId = songId; }
    public long getPlaylistId() { return playlistId; }
    public void setPlaylistId(long playlistId) { this.playlistId = playlistId; }
}
