package com.example.deakyu.musicplayerapp.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import com.example.deakyu.musicplayerapp.database.MusicRepository;
import com.example.deakyu.musicplayerapp.model.Playlist;
import com.example.deakyu.musicplayerapp.model.Song;

import java.util.List;

public class PlaylistViewModel extends AndroidViewModel {

    private MusicRepository mRepository;

    private LiveData<List<Playlist>> mAllPlaylists;

    public PlaylistViewModel(Application app) {
        super(app);
        mRepository = new MusicRepository(app);
        mAllPlaylists = mRepository.getAllPlaylists();
    }

    public LiveData<List<Playlist>> getAllPlaylists() { return mAllPlaylists; }

    public void insert(Playlist playlist) { mRepository.insertPlaylist(playlist); }

}
