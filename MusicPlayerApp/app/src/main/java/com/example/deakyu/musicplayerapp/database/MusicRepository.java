package com.example.deakyu.musicplayerapp.database;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import com.example.deakyu.musicplayerapp.dao.SongDao;
import com.example.deakyu.musicplayerapp.model.Song;

import java.util.List;

public class MusicRepository {
    private SongDao mSongDao;

    private LiveData<List<Song>> mAllSongs;

    public MusicRepository(Application app) {
        MusicDatabase db = MusicDatabase.getDatabase(app);
        mSongDao = db.songDao();

        mAllSongs = mSongDao.getAllSongs();
    }

    public LiveData<List<Song>> getAllSongs() { return mAllSongs; }

    public void insertSong(Song song) { new insertSongAsync(mSongDao).execute(song); }

    public void deleteAllSongs() { new deleteSongsAsync(mSongDao).execute(); }

    private static class insertSongAsync extends AsyncTask<Song, Void, Void> {
        private SongDao mAsyncSongDao;

        insertSongAsync(SongDao dao) {
            mAsyncSongDao = dao;
        }

        @Override
        protected Void doInBackground(Song... songs) {
            mAsyncSongDao.insert(songs[0]);
            return null;
        }
    }

    private static class deleteSongsAsync extends AsyncTask<Void, Void, Void> {
        private SongDao mAsyncSongDao;

        deleteSongsAsync(SongDao dao) { mAsyncSongDao = dao; }

        @Override
        protected Void doInBackground(Void... voids) {
            mAsyncSongDao.deleteAll();
            return null;
        }
    }

}
