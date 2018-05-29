package com.example.deakyu.musicplayerapp.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;

@Entity(tableName = "songs",
        indices = {@Index("id")})
public class Song implements Parcelable {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") private long id;
    @ColumnInfo(name = "musicId") private long musicId;
    @ColumnInfo(name = "title") private String title;
    @ColumnInfo(name = "artist") private String artist;

    public Song(long musicId, String title, String artist) {
        this.musicId = musicId;
        this.title = title;
        this.artist = artist;
    }

    public long getId() { return id; }
    public long getMusicId() { return musicId; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public void setId(long id) { this.id = id; }
    public void setMusicId(long musicId) { this.musicId = musicId; }
    public void setTitle(String title) { this.title = title; }
    public void setArtist(String artist) { this.artist = artist; }

    @Override
    public int describeContents() {
        return 0;
    }

    protected Song(Parcel in) {
        id = in.readLong();
        musicId = in.readLong();
        title = in.readString();
        artist = in.readString();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeLong(musicId);
        parcel.writeString(title);
        parcel.writeString(artist);
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        @Override
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };
}