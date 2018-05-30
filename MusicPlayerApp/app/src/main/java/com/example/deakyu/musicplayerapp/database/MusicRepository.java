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

    public void deleteAllSongs() { new deleteSongsAsync(mSongDao).execute(); }

    public void refreshSongs(final List<Song> newSongs) {
        new refreshSongsAsync(db, this).execute(newSongs);
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

    private static class deleteSongsAsync extends AsyncTask<Void, Void, Void> {
        private SongDao mAsyncSongDao;

        deleteSongsAsync(SongDao dao) { mAsyncSongDao = dao; }

        @Override
        protected Void doInBackground(Void... voids) {
            mAsyncSongDao.deleteAll();
            return null;
        }
    }

    private static class refreshSongsAsync extends AsyncTask<List<Song>, Void, Void> {

        private MusicDatabase db;
        private MusicRepository repo;

        refreshSongsAsync(MusicDatabase db, MusicRepository repo) {
            this.db = db;
            this.repo = repo;
        }

        @Override
        protected Void doInBackground(final List<Song>... lists) {
            db.runInTransaction(new Runnable() {
                @Override
                public void run() {
                    repo.deleteAllSongs();
                    for(int i=0 ; i < lists[0].size() ; i++) {
                        repo.insertSong(lists[0].get(i));
                    }
                }
            });
            return null;
        }
    }

}
