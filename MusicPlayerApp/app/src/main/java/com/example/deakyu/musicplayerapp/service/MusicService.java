package com.example.deakyu.musicplayerapp.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
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

import com.example.deakyu.musicplayerapp.R;
import com.example.deakyu.musicplayerapp.database.StorageUtil;
import com.example.deakyu.musicplayerapp.model.Song;

import java.util.List;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnSeekCompleteListener{

    private static final String TAG = MusicService.class.getSimpleName();

    private final IBinder mBinder = new MyBinder();
    private MediaPlayer mediaPlayer = null;

    private Song curSong;
    private int curIndex;
    private List<Song> songsFromDb;
    private int resumePosition;

    private StorageUtil util;

    // Notification Intent actions
    public static final String ACTION_PLAY = "com.example.musicplayerapp.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.example.musicplayerapp.ACTION_PAUSE";
    public static final String ACTION_STOP = "com.example.musicplayerapp.ACTION_STOP";
    public static final String ACTION_NEXT = "com.example.musicplayerapp.ACTION_NEXT";
    public static final String ACTION_PREV = "com.example.musicplayerapp.ACTION_PREV";
    public static final String BROADCAST_MEDIA_READY = "com.example.musicplayerapp.MEDIA_READY";

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
                        .setShowActionsInCompactView(0, 1, 2))
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setLargeIcon(largeIcon)
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                .setContentText(curSong.getArtist())
                .setContentTitle(curSong.getTitle())
                .addAction(android.R.drawable.ic_media_previous, "previous", playbackAction(3))
                .addAction(notificationAction, "pause", play_pauseAction)
                .addAction(android.R.drawable.ic_media_next, "next", playbackAction(2));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager = NotificationManagerCompat.from(getApplicationContext());
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    private void removeNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, MusicService.class);
        switch (actionNumber) {
            case 0:
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                playbackAction.setAction(ACTION_PREV);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    // endregion

    private void handleIncomingActions(Intent playbackAction) {
        if(playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            transportControls.play();
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            transportControls.stop();
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            transportControls.skipToNext();
        } else if (actionString.equalsIgnoreCase(ACTION_PREV)) {
            transportControls.skipToPrevious();
        }
    }

    // region MediaPlayer Listeners

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        Toast.makeText(getApplicationContext(), "onPrepared MediaPlayer", Toast.LENGTH_SHORT).show();
        sendPreparedBroadcast();
        playMusic();
    }

    private LocalBroadcastManager mLocalBroadcastManager;

    private void sendPreparedBroadcast() {
        Intent intent = new Intent(BROADCAST_MEDIA_READY);
        mLocalBroadcastManager.sendBroadcast(intent);
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
        curIndex = curIndex >= songsFromDb.size()-1 ? 0 : curIndex+1;
        curSong = songsFromDb.get(curIndex);

        util.storeSongIndex(curIndex);

        stopMusic();
        mediaPlayer.reset();
        initMediaPlayer();
    }

    private void skipToPrevious() {
        curIndex = curIndex <= 0 ? songsFromDb.size()-1 : curIndex-1;
        curSong = songsFromDb.get(curIndex);

        util.storeSongIndex(curIndex);

        stopMusic();
        mediaPlayer.reset();
        initMediaPlayer();
    }

    public void playNewAudio() {
        try {
            curIndex = util.loadSongIndex();
            curSong = songsFromDb.get(curIndex);
        } catch (NullPointerException e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Music Not Found", Toast.LENGTH_SHORT).show();
        }
        if(curSong != null) {
            stopMusic();
            mediaPlayer.reset();
            initMediaPlayer();
            updateMetadata();
            buildNotification(PlaybackStatus.PLAYING);
        }
    }

    public void pauseAudio() { transportControls.pause(); }
    public void resumeAudio() { transportControls.play(); }
    public void nextAudio() { transportControls.skipToNext(); }
    public void prevAudio() { transportControls.skipToPrevious(); }

    public void seekTo(int pos) {
        mediaPlayer.seekTo(pos);
        transportControls.seekTo(pos);
    }

    public int getDuration() { return mediaPlayer.getDuration(); }
    public int getCurrentPosition() { return mediaPlayer.getCurrentPosition(); }
    public int getCurrentSongIndex() { return curIndex; }

    // endregion

    // region Service LifeCycle

    @Override
    public void onCreate() {
        super.onCreate();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(util == null) {
            util = new StorageUtil(getApplicationContext());
        }

        try {
            songsFromDb = util.loadSongs();
            curIndex = util.loadSongIndex();
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
    public void onDestroy() {
        super.onDestroy();

        if(mediaPlayer != null) {
            stopMusic();
            mediaPlayer.release();
        }

        removeNotification();
        util.clearCachedSongList();
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