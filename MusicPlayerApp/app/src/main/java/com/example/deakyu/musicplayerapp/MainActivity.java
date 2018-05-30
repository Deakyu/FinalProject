package com.example.deakyu.musicplayerapp;

import android.Manifest;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.deakyu.musicplayerapp.adapter.ItemClickListener;
import com.example.deakyu.musicplayerapp.adapter.SongListAdapter;
import com.example.deakyu.musicplayerapp.database.StorageUtil;
import com.example.deakyu.musicplayerapp.service.MusicService;
import com.example.deakyu.musicplayerapp.model.Song;
import com.example.deakyu.musicplayerapp.viewmodel.SongViewModel;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ItemClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int EXTERNAL_STORAGE_PERMISSION_REQUEST = 1;

    private LocalBroadcastManager mLocalBroadcastManager;

    List<Song> songList;
    List<Song> songsFromDb;
    int curPos;
    MusicService musicService;
    boolean serviceBound = false;

    private SongListAdapter adapter;
    private SongViewModel songViewModel;

    private ImageButton pauseBtn;
    private ImageButton playBtn;
    private ImageButton skipPrevBtn;
    private ImageButton skipNextBtn;
    private ImageView album;
    private TextView mediaTitle;
    private TextView mediaArtist;
    private SeekBar progressBar;
    private TextView currentDurationPos;
    private TextView totalDuration;

    // region Activity LifeCycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        registerMediaReadyReceiver();
        setRecyclerView();
        setSongViewModel();
        setMediaControls();

        executeRuntimePermission(); // Don't try to access files from here below
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocalBroadcastManager.unregisterReceiver(mediaReadyReceiver);
    }

    // endregion

    // region Storage Permission Flow

    private void executeRuntimePermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Needs to ask permission runtime after M
            if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestStoragePermission();
            } else {
                continueWithPermission();
            }
        } else {
            continueWithPermission();
        }
    }

    private void requestStoragePermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.permission_title)
                    .setMessage(R.string.permission_info)
                    .setPositiveButton(R.string.allow, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_PERMISSION_REQUEST);
                        }
                    })
                    .setNegativeButton(R.string.deny, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }).create().show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == EXTERNAL_STORAGE_PERMISSION_REQUEST) {
            if(grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // Permission denied
                Toast.makeText(this, R.string.exit_app, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                continueWithPermission();
            }
        }
    }

    private void continueWithPermission() { // Permission granted, proceed to work
        songList = MediaHelper.getMusicFromStorage(this); // Songs from storage


        songViewModel.refreshSongs(songList);
        // TODO: Strategy to only insert these for the first time the user enters the app
//        for(int i=0 ; i < songList.size() ; i++) {
//            songViewModel.insert(songList.get(i));
//        }
    }

    // endregion

    // region UI setup + ViewModel

    private void setRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        adapter = new SongListAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter.setClickListener(this); // onClick Listener for each item view
    }

    private void setSongViewModel() {
        songViewModel = ViewModelProviders.of(this).get(SongViewModel.class);

        songViewModel.getAllSongs().observe(this, new Observer<List<Song>>() {
            @Override
            public void onChanged(@Nullable List<Song> songs) {
                songsFromDb = songs;
                adapter.setSongs(songsFromDb);
            }
        });
    }

    @Override
    public void onClick(View view, int pos) {
        curPos = pos;
        playAudio(pos);
    }

    private void setMediaControls() {
        album = findViewById(R.id.album);
        mediaTitle = findViewById(R.id.media_title);
        mediaArtist = findViewById(R.id.media_artist);
        pauseBtn = findViewById(R.id.media_pause);
        playBtn = findViewById(R.id.media_play);
        skipNextBtn = findViewById(R.id.skip_next);
        skipPrevBtn = findViewById(R.id.skip_prev);
        progressBar = findViewById(R.id.progress_bar);
        currentDurationPos = findViewById(R.id.progress_current);
        totalDuration = findViewById(R.id.progress_remaining);

        pauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseBtn.setVisibility(View.GONE);
                playBtn.setVisibility(View.VISIBLE);
                if(serviceBound) musicService.pauseAudio();
                mSeekbarUpdateHandler.removeCallbacks(mUpdateSeekbar);
            }
        });

        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseBtn.setVisibility(View.VISIBLE);
                playBtn.setVisibility(View.GONE);
                if(serviceBound) musicService.resumeAudio();
                mSeekbarUpdateHandler.postDelayed(mUpdateSeekbar, 0);
            }
        });

        skipNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(curPos >= songsFromDb.size() - 1) {
                    curPos = 0;
                } else {
                    curPos++;
                }
                playNext(curPos);
            }
        });

        skipPrevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(curPos <= 0) {
                    curPos = songsFromDb.size()-1;
                } else {
                    curPos--;
                }
                playPrev(curPos);
            }
        });

        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    mSeekbarUpdateHandler.removeCallbacks(mUpdateSeekbar);
                    musicService.seekTo(progress);
                    mSeekbarUpdateHandler.postDelayed(mUpdateSeekbar, 0);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    // endregion

    private ServiceConnection musicServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MusicService.MyBinder binder = (MusicService.MyBinder) iBinder;
            musicService = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            serviceBound = false;
        }
    };

    private StorageUtil util;

    private void playAudio(int songIndex) {
        if(!serviceBound) { // start a fresh service
            Intent intent = new Intent(this, MusicService.class);
            intent.putExtra("songIndex", songIndex);

            util = new StorageUtil(getApplicationContext());
            util.storeSongs(songsFromDb);
            util.storeSongIndex(songIndex);

            startService(intent);
            bindService(intent, musicServiceConnection, Context.BIND_AUTO_CREATE);
            setUIAfterPlay(songIndex);
        } else { // Service is already running, so put different Song object
            util = new StorageUtil(getApplicationContext());
            util.storeSongIndex(songIndex);

            musicService.playNewAudio();
            setUIAfterPlay(songIndex);
        }
    }

    private void playNext(int songIndex) {
        util = new StorageUtil(getApplicationContext());
        util.storeSongIndex(songIndex);
        if(serviceBound) musicService.nextAudio();
        setUIAfterPlay(songIndex);
    }

    private void playPrev(int songIndex) {
        util = new StorageUtil(getApplicationContext());
        util.storeSongIndex(songIndex);
        if(serviceBound) musicService.prevAudio();
        setUIAfterPlay(songIndex);
    }

    private void setUIAfterPlay(int songIndex) {
        playBtn.setVisibility(View.GONE);
        pauseBtn.setVisibility(View.VISIBLE);

        mediaTitle.setText(songsFromDb.get(songIndex).getTitle());
        mediaArtist.setText(songsFromDb.get(songIndex).getArtist());
        // TODO: https://stackoverflow.com/questions/17573972/how-can-i-display-album-art-using-mediastore-audio-albums-album-art/38873747
    }

    private BroadcastReceiver mediaReadyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int duration = musicService.getDuration();
            String strDuration = MediaHelper.convertDurationToString(duration);
            int currentSongIndex = musicService.getCurrentSongIndex();

            setUIAfterPlay(currentSongIndex);

            totalDuration.setText(strDuration);
            progressBar.setMax(duration);
            mSeekbarUpdateHandler.postDelayed(mUpdateSeekbar, 0);
        }
    };

    private void registerMediaReadyReceiver() {
        IntentFilter filter = new IntentFilter(MusicService.BROADCAST_MEDIA_READY);
        mLocalBroadcastManager.registerReceiver(mediaReadyReceiver, filter);
    }

    private Handler mSeekbarUpdateHandler = new Handler();
    private Runnable mUpdateSeekbar = new Runnable() {
        @Override
        public void run() {
            int curPos = musicService.getCurrentPosition();
            progressBar.setProgress(curPos);
            currentDurationPos.setText(MediaHelper.convertDurationToString(curPos));
            mSeekbarUpdateHandler.postDelayed(this, 50);
        }
    };

}
