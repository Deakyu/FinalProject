package com.example.deakyu.musicplayerapp.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import com.example.deakyu.musicplayerapp.database.MusicRepository;
import com.example.deakyu.musicplayerapp.model.Song;

import java.util.List;

public class SongViewModel extends AndroidViewModel {

    private MusicRepository mRepository;

    private LiveData<List<Song>> mAllSongs;

    public SongViewModel(Application app) {
        super(app);
        mRepository = new MusicRepository(app);
        mAllSongs = mRepository.getAllSongs();
    }

    public LiveData<List<Song>> getAllSongs() { return mAllSongs; }

    public void insert(Song song) { mRepository.insertSong(song); }

}
