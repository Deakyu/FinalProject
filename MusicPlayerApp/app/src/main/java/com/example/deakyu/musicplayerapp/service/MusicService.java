package com.example.deakyu.musicplayerapp.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.widget.Toast;

import com.example.deakyu.musicplayerapp.MainActivity;
import com.example.deakyu.musicplayerapp.R;
import com.example.deakyu.musicplayerapp.model.Song;

import java.util.List;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnSeekCompleteListener{

    private final IBinder mBinder = new MyBinder();
    private MediaPlayer mediaPlayer = null;
    private static final String TAG = MusicService.class.getSimpleName();

    private Song curSong;
    private int curIndex;
    private List<Song> songsFromDb;

    private int resumePosition;

    public static final String ACTION_PLAY = "com.example.musicplayerapp.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.example.musicplayerapp.ACTION_PAUSE";
    public static final String ACTION_STOP = "com.example.musicplayerapp.ACTION_STOP";

    //MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;
    private NotificationManagerCompat notificationManager;

    //AudioPlayer notification ID
    private static final int NOTIFICATION_ID = 101;

    // region Init Methods

    private void initMediaPlayer() {
        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, curSong.getMusicId());
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(getApplicationContext(), contentUri);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error from mediaPlayer.setDataSource()", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            stopSelf();
        }
        mediaPlayer.prepareAsync();
    }

    private void initMediaSession(){
        if(mediaSession != null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        }
        mediaSession = new MediaSessionCompat(getApplicationContext(), "AudioPlayer");
        transportControls = mediaSession.getController().getTransportControls();
        mediaSession.setActive(true);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        updateMetadata();

        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                resumeMusic();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onPause() {
                super.onPause();
                pauseMusic();
                buildNotification(PlaybackStatus.PAUSED);
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                skipToNext();
                updateMetadata();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                skipToPrevious();
                updateMetadata();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                stopSelf();
            }

            @Override
            public void onSeekTo(long pos) {
                super.onSeekTo(pos);
            }
        });
    }

    private void updateMetadata() {
        Bitmap albumArt = BitmapFactory.decodeResource(getResources(), R.drawable.placeholder_song);
            mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, curSong.getArtist())
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, curSong.getTitle())
                    .build());
    }

    // endregion

    // region notification methods

    private void buildNotification(PlaybackStatus playbackStatus) {
        int notificationAction;
        PendingIntent play_pauseAction;

        if(playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = android.R.drawable.ic_media_pause;
            play_pauseAction = playbackAction(1);
        } else {
            notificationAction = android.R.drawable.ic_media_play;
            play_pauseAction = playbackAction(0);
        }

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.placeholder_song);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setShowWhen(false)
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0))
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setLargeIcon(largeIcon)
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                .setContentText(curSong.getArtist())
                .setContentTitle(curSong.getTitle())
                .addAction(notificationAction, "pause", play_pauseAction);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager = NotificationManagerCompat.from(getApplicationContext());
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    private void removeNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

    // endregion

    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, MusicService.class);
        switch (actionNumber) {
            case 0:
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    private void handleIncomingActions(Intent playbackAction) {
        if(playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            transportControls.play();
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            transportControls.stop();
        }
    }

    // region MediaPlayer Listeners

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        Toast.makeText(getApplicationContext(), "onPrepared MediaPlayer", Toast.LENGTH_SHORT).show();
        playMusic();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int type, int info) {
        Toast.makeText(getApplicationContext(), "onError MediaPlayer", Toast.LENGTH_SHORT).show();

        switch (type) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d(TAG, "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK: " + info);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d(TAG, "MEDIA ERROR SERVER DIED: " + info);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d(TAG, "MEDIA ERROR UNKNOWN: " + info);
                break;
        }

        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Toast.makeText(getApplicationContext(), "onCompletion MediaPlayer", Toast.LENGTH_SHORT).show();

        stopMusic();
        stopSelf();
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        Toast.makeText(getApplicationContext(), "onSeekComplete MediaPlayer", Toast.LENGTH_SHORT).show();
    }

    // endregion

    // region MediaPlayer Interfaces

    private void playMusic() {
        if(mediaPlayer != null && !mediaPlayer.isPlaying()) mediaPlayer.start();
    }

    private void stopMusic() {
        if(mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.stop();
    }

    private void pauseMusic() {
        if(mediaPlayer != null && mediaPlayer.isPlaying()) {
            resumePosition = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
        }
    }

    private void resumeMusic() {
        if(mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
        }
    }

    private void skipToNext() {
        // TODO:
        if(curIndex >= songsFromDb.size()-1) {
            curIndex = 0;
        } else {
            curIndex++;
        }
        curSong = songsFromDb.get(curIndex);

        stopMusic();
        mediaPlayer.reset();
        initMediaPlayer();
    }

    private void skipToPrevious() {
        // TODO:
        if(curIndex <= 0) {
            curIndex = songsFromDb.size()-1;
        } else {
            curIndex--;
        }
        curSong = songsFromDb.get(curIndex);

        stopMusic();
        mediaPlayer.reset();
        initMediaPlayer();
    }

    // endregion

    // region Service LifeCycle

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            curIndex = intent.getIntExtra("songIndex", 0);
            songsFromDb = intent.getParcelableArrayListExtra("songs");
            curSong = songsFromDb.get(curIndex);
        } catch(NullPointerException e) {
            stopSelf();
        }

        if(mediaSessionManager == null) {
            try {
                initMediaSession();
                initMediaPlayer();
            } catch (Exception e) {
                e.printStackTrace();
                stopSelf();
            }
            buildNotification(PlaybackStatus.PLAYING);
        }

        handleIncomingActions(intent);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        registerPlayNewAudioReceiver();
        registerPauseAudioReceiver();
        registerResumeAudioReceiver();
        registerPlayNextReceiver();
        registerPlayPrevReceiver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(mediaPlayer != null) {
            stopMusic();
            mediaPlayer.release();
        }

        mLocalBroadcastManager.unregisterReceiver(playNewAudio);
        mLocalBroadcastManager.unregisterReceiver(pauseAudio);
        mLocalBroadcastManager.unregisterReceiver(resumeAudio);
        mLocalBroadcastManager.unregisterReceiver(nextAudio);
        mLocalBroadcastManager.unregisterReceiver(prevAudio);
        removeNotification();
    }

    // endregion

    // region Broadcast Receivers

    private LocalBroadcastManager mLocalBroadcastManager;

    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                curIndex = intent.getIntExtra("songIndex", 0);
                curSong = songsFromDb.get(curIndex);
            } catch (NullPointerException e){
                e.printStackTrace();
                Toast.makeText(context, "Error retrieving the song!", Toast.LENGTH_SHORT).show();
            }
            if(curSong != null) {
                stopMusic();
                mediaPlayer.reset();
                initMediaPlayer();
                updateMetadata();
                buildNotification(PlaybackStatus.PLAYING);
            }
        }
    };

    private BroadcastReceiver pauseAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            transportControls.pause();
        }
    };

    private BroadcastReceiver resumeAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            transportControls.play();
        }
    };

    private BroadcastReceiver nextAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            transportControls.skipToNext();
        }
    };

    private BroadcastReceiver prevAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            transportControls.skipToPrevious();
        }
    };

    private void registerPlayNewAudioReceiver() {
        IntentFilter filter = new IntentFilter(MainActivity.BROADCAST_PLAY_NEW_AUDIO);
        mLocalBroadcastManager.registerReceiver(playNewAudio, filter);
    }

    private void registerPauseAudioReceiver() {
        IntentFilter filter = new IntentFilter(MainActivity.BROADCAST_PAUSE_AUDIO);
        mLocalBroadcastManager.registerReceiver(pauseAudio, filter);
    }

    private void registerResumeAudioReceiver() {
        IntentFilter filter = new IntentFilter(MainActivity.BROADCAST_RESUME_AUDIO);
        mLocalBroadcastManager.registerReceiver(resumeAudio, filter);
    }

    private void registerPlayNextReceiver() {
        IntentFilter filter = new IntentFilter(MainActivity.BROADCAST_PLAY_NEXT);
        mLocalBroadcastManager.registerReceiver(nextAudio, filter);
    }

    private void registerPlayPrevReceiver() {
        IntentFilter filter = new IntentFilter(MainActivity.BROADCAST_PLAY_PREV);
        mLocalBroadcastManager.registerReceiver(prevAudio, filter);
    }

    // endregion


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    public class MyBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }
}