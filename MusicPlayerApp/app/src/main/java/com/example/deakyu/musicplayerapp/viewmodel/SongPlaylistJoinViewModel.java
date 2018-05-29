package com.example.deakyu.musicplayerapp.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import com.example.deakyu.musicplayerapp.database.MusicRepository;
import com.example.deakyu.musicplayerapp.model.Song;
import com.example.deakyu.musicplayerapp.model.SongPlaylistJoin;

import java.util.List;

public class SongPlaylistJoinViewModel extends AndroidViewModel {

    private MusicRepository mRepository;

    public SongPlaylistJoinViewModel(Application app) {
        super(app);
        mRepository = new MusicRepository(app);
    }

    public LiveData<List<Song>> getSongsByPlaylistId(long playlistId) {
        return mRepository.getSongsByPlaylistId(playlistId);
    }

    public void insert(SongPlaylistJoin songPlaylistJoin) {
        mRepository.insertSongPlaylistJoin(songPlaylistJoin);
    }

}
