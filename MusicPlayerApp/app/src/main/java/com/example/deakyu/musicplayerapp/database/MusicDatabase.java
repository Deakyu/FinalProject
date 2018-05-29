package com.example.deakyu.musicplayerapp.database;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.example.deakyu.musicplayerapp.dao.PlaylistDao;
import com.example.deakyu.musicplayerapp.dao.SongDao;
import com.example.deakyu.musicplayerapp.dao.SongPlaylistJoinDao;
import com.example.deakyu.musicplayerapp.model.Playlist;
import com.example.deakyu.musicplayerapp.model.Song;
import com.example.deakyu.musicplayerapp.model.SongPlaylistJoin;

@Database(entities = {Song.class, Playlist.class, SongPlaylistJoin.class},
          version = 2)
public abstract class MusicDatabase extends RoomDatabase {
    public abstract SongDao songDao();
    public abstract PlaylistDao playlistDao();
    public abstract SongPlaylistJoinDao songPlaylistJoinDao();

    private static MusicDatabase INSTANCE;

    public static MusicDatabase getDatabase(final Context context) {
        if(INSTANCE == null) {
            synchronized (MusicDatabase.class) {
                if(INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            MusicDatabase.class, "music_database")
                            .fallbackToDestructiveMigration()
//                            .addCallback(sRoomDatabaseCallback) // TODO: Remove this eventually
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            new PopulateDbAsync(INSTANCE).execute();
        }
    };

    private static class PopulateDbAsync extends AsyncTask<Void, Void, Void> {
        private final SongDao songDao;

        PopulateDbAsync(MusicDatabase db) {
            songDao = db.songDao();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            songDao.deleteAll();
            return null;
        }
    }
}
