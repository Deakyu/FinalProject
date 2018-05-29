package com.example.deakyu.musicplayerapp;

import android.Manifest;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.deakyu.musicplayerapp.adapter.ItemClickListener;
import com.example.deakyu.musicplayerapp.adapter.SongListAdapter;
import com.example.deakyu.musicplayerapp.service.MusicService;
import com.example.deakyu.musicplayerapp.model.Song;
import com.example.deakyu.musicplayerapp.viewmodel.SongViewModel;

import java.util.List;

public class MainActivity extends AppCompatActivity implements ItemClickListener {

    public static final String BROADCAST_PLAY_NEW_AUDIO = "com.example.musicplayerapp.PlayNewAudio";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int EXTERNAL_STORAGE_PERMISSION_REQUEST = 1;

    List<Song> songList;
    List<Song> songsFromDb;
    MusicService musicService;
    boolean serviceBound = false;

    private Button shutdownServiceBtn;


    private SongListAdapter adapter;
    private SongViewModel songViewModel;

    // region Activity LifeCycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        shutdownServiceBtn = findViewById(R.id.shut_down_service_button);
        shutdownServiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(serviceBound) {
                    unbindService(musicServiceConnection);
                    musicService.stopSelf();
                }
            }
        });

        setRecyclerView();
        setSongViewModel();

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

        // TODO: Strategy to only insert these for the first time the user enters the app
        for(int i=0 ; i < songList.size() ; i++) {
            songViewModel.insert(songList.get(i));
        }
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
        Song clickedSong = songsFromDb.get(pos);
        playAudio(clickedSong);
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

    private void playAudio(Song song) {
        if(!serviceBound) { // start a fresh service
            Intent intent = new Intent(this, MusicService.class);
            intent.putExtra("curSong", song);
            startService(intent);
            bindService(intent, musicServiceConnection, Context.BIND_AUTO_CREATE);
        } else { // Service is already running, so put different Song object
            Intent newAudioIntent = new Intent(BROADCAST_PLAY_NEW_AUDIO);
            newAudioIntent.putExtra("curSong", song);
            sendBroadcast(newAudioIntent);
        }
    }


    private void dummy() {
        System.out.println("Delete this function!!!!");
    }

}
