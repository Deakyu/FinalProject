package com.example.deakyu.musicplayerapp.database;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import com.example.deakyu.musicplayerapp.dao.PlaylistDao;
import com.example.deakyu.musicplayerapp.dao.SongDao;
import com.example.deakyu.musicplayerapp.dao.SongPlaylistJoinDao;
import com.example.deakyu.musicplayerapp.model.Playlist;
import com.example.deakyu.musicplayerapp.model.Song;
import com.example.deakyu.musicplayerapp.model.SongPlaylistJoin;

import java.util.List;

public class MusicRepository {
    private SongDao mSongDao;
    private PlaylistDao mPlaylistDao;
    private SongPlaylistJoinDao mSongPlaylistJoinDao;

    private LiveData<List<Song>> mAllSongs;
    private LiveData<List<Playlist>> mAllPlaylists;

    public MusicRepository(Application app) {
        MusicDatabase db = MusicDatabase.getDatabase(app);
        mSongDao = db.songDao();
        mPlaylistDao = db.playlistDao();
        mSongPlaylistJoinDao = db.songPlaylistJoinDao();

        mAllSongs = mSongDao.getAllSongs();
        mAllPlaylists = mPlaylistDao.getAllPlaylists();
    }

    public LiveData<List<Song>> getAllSongs() { return mAllSongs; }
    public LiveData<List<Playlist>> getAllPlaylists() { return mAllPlaylists; }
    public LiveData<List<Song>> getSongsByPlaylistId(long playlistId) {
        return mSongPlaylistJoinDao.getSongsByPlaylistId(playlistId);
    }

    public void insertSong(Song song) { new insertSongAsync(mSongDao).execute(song); }
    public void insertPlaylist(Playlist playlist) { new insertPlaylistAsync(mPlaylistDao).execute(playlist); }
    public void insertSongPlaylistJoin(SongPlaylistJoin songPlaylistJoin) {
        new insertSongPlaylistAsync(mSongPlaylistJoinDao).execute(songPlaylistJoin);
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

    private static class insertPlaylistAsync extends AsyncTask<Playlist, Void, Void> {
        private PlaylistDao mAsyncPlaylistDao;

        insertPlaylistAsync(PlaylistDao dao) {
            mAsyncPlaylistDao = dao;
        }

        @Override
        protected Void doInBackground(Playlist... playlists) {
            mAsyncPlaylistDao.insert(playlists[0]);
            return null;
        }
    }

    private static class insertSongPlaylistAsync extends AsyncTask<SongPlaylistJoin, Void, Void> {
        private SongPlaylistJoinDao mAsyncDao;

        insertSongPlaylistAsync(SongPlaylistJoinDao dao) {
            mAsyncDao = dao;
        }

        @Override
        protected Void doInBackground(SongPlaylistJoin... songPlaylistJoins) {
            mAsyncDao.insert(songPlaylistJoins[0]);
            return null;
        }
    }
}
