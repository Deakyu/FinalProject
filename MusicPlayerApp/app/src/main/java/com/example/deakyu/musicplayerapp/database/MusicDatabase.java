package com.example.deakyu.musicplayerapp.database;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.example.deakyu.musicplayerapp.dao.SongDao;
import com.example.deakyu.musicplayerapp.model.Playlist;
import com.example.deakyu.musicplayerapp.model.Song;
import com.example.deakyu.musicplayerapp.model.SongPlaylistJoin;

@Database(entities = {Song.class},
          version = 3)
public abstract class MusicDatabase extends RoomDatabase {
    public abstract SongDao songDao();

    private static MusicDatabase INSTANCE;

    public static MusicDatabase getDatabase(final Context context) {
        if(INSTANCE == null) {
            synchronized (MusicDatabase.class) {
                if(INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            MusicDatabase.class, "music_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
