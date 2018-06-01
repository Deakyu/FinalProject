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

    private MusicDatabase db;

    public MusicRepository(Application app) {
        db = MusicDatabase.getDatabase(app);
        mSongDao = db.songDao();

        mAllSongs = mSongDao.getAllSongs();
    }

    public LiveData<List<Song>> getAllSongs() { return mAllSongs; }

    public void insertSong(Song song) { new insertSongAsync(mSongDao).execute(song); }

    public void refreshSongs(final List<Song> newSongs) {
        new refreshSongsAsync(mSongDao).execute(newSongs);
    }

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

    private static class refreshSongsAsync extends AsyncTask<List<Song>, Void, Void> {

         private SongDao mAsyncSongDao;

        refreshSongsAsync(SongDao mAsyncSongDao) {
            this.mAsyncSongDao = mAsyncSongDao;
        }

        @Override
        protected Void doInBackground(final List<Song>... lists) {
            mAsyncSongDao.deleteAll();
            for(int i=0 ; i < lists[0].size() ; i++)
                mAsyncSongDao.insert(lists[0].get(i));
            return null;
        }
    }

}
