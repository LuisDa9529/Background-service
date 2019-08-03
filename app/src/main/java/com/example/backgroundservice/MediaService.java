package com.example.backgroundservice;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;

import androidx.core.app.NotificationCompat;

import javax.security.auth.login.LoginException;

import static com.example.backgroundservice.App.CHANNEL_ID;

public class MediaService extends Service {

    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_STOP = "action_stop";


    public int[] songs = {R.raw.say_amen, R.raw.high_hopes,R.raw.look_ma};
    private int counter = 0;
    public MediaPlayer mMediaPlayer;
    private MediaSessionCompat mSession;
    private MediaControllerCompat mController;
    private PlaybackStateCompat state;
    private MediaMetadataRetriever mmr = new MediaMetadataRetriever();
    private Bitmap picture;
    private MainActivity main = new MainActivity();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        new DownloadImage().execute();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(mMediaPlayer==null)
            mMediaPlayer = MediaPlayer.create(getApplicationContext(), songs[counter]);

            mSession = new MediaSessionCompat(getApplicationContext(), "simple player session");
        try {
            mController =new MediaControllerCompat(getApplicationContext(), mSession.getSessionToken());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        initMediaSessions();


        handleIntent( intent );

        return super.onStartCommand(intent, flags, startId);
    }

    private void initMediaSessions(){

        mSession.setCallback(new MediaSessionCompat.Callback(){
                                 @Override
                                 public void onPlay() {
                                     super.onPlay();
                                     Log.e( "MediaPlayerService", "onPlay");
                                     mMediaPlayer.start();
                                     state = new PlaybackStateCompat.Builder()
                                             .setState(PlaybackStateCompat.STATE_PLAYING, 1, SystemClock.elapsedRealtime())
                                             .build();
                                     mSession.setPlaybackState(state);
                                     buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE, 1));
                                     Log.e("onPlay", "onStartCommand: " + mMediaPlayer.isPlaying());

                                 }

                                 @Override
                                 public void onPause() {
                                     super.onPause();
                                     Log.e( "MediaPlayerService", "onPause");
                                     mMediaPlayer.pause();
                                     state = new PlaybackStateCompat.Builder()
                                             .setState(PlaybackStateCompat.STATE_PAUSED, 1, SystemClock.elapsedRealtime())
                                             .build();
                                     mSession.setPlaybackState(state);
                                     buildNotification(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY, 2));
                                     stopForeground(false);
                                 }

                                 @Override
                                 public void onSkipToNext() {
                                     super.onSkipToNext();
                                     Log.e( "MediaPlayerService", "onSkipToNext");

                                     if(songs.length < counter+1){
                                         counter+=1;
                                         mMediaPlayer.release();
                                         mMediaPlayer = MediaPlayer.create(getApplicationContext(), songs[counter]);
                                         mMediaPlayer.start();
                                         state = new PlaybackStateCompat.Builder()
                                                 .setState(PlaybackStateCompat.STATE_PLAYING, 1, SystemClock.elapsedRealtime())
                                                 .build();
                                         mSession.setPlaybackState(state);
                                         buildNotification( generateAction( android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE , 1) );
                                     }else{
                                         Toast.makeText(getApplicationContext(), "Reached end of list", Toast.LENGTH_SHORT).show();
                                     }
                                 }

                                 @Override
                                 public void onSkipToPrevious() {
                                     super.onSkipToPrevious();
                                     Log.e( "MediaPlayerService", "onSkipToPrevious");
                                     counter-=1;
                                     mMediaPlayer.release();
                                     mMediaPlayer = MediaPlayer.create(getApplicationContext(), songs[counter]);
                                     mMediaPlayer.start();
                                     state = new PlaybackStateCompat.Builder()
                                             .setState(PlaybackStateCompat.STATE_PLAYING, 1, SystemClock.elapsedRealtime())
                                             .build();
                                     mSession.setPlaybackState(state);
                                     buildNotification( generateAction( android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE , 1) );                                     buildNotification( generateAction( android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE, 1) );
                                 }

                                 @Override
                                 public void onStop() {
                                     super.onStop();
                                     Log.e( "MediaPlayerService", "onStop");
                                     //Stop media player here
                                     mMediaPlayer.release();
                                     Intent intent = new Intent( getApplicationContext(), MediaService.class );
                                     stopForeground( true );
                                     stopSelf();
                                 }

                             }
        );
    }

    private void handleIntent( Intent intent ) {
        if( intent == null || intent.getAction() == null )
            return;

        String action = intent.getAction();

        if( action.equalsIgnoreCase( ACTION_PLAY ) ) {
            mController.getTransportControls().play();
        } else if( action.equalsIgnoreCase( ACTION_PAUSE ) ) {
            mController.getTransportControls().pause();
        } else if( action.equalsIgnoreCase( ACTION_PREVIOUS ) ) {
            mController.getTransportControls().skipToPrevious();
        } else if( action.equalsIgnoreCase( ACTION_NEXT ) ) {
            mController.getTransportControls().skipToNext();
        }else if( action.equalsIgnoreCase( ACTION_STOP ) ) {
            mController.getTransportControls().stop();
        }
    }

    private NotificationCompat.Action generateAction( int icon, String title, String intentAction, int code) {
        Intent intent = new Intent( getApplicationContext(), MediaService.class );
        intent.setAction( intentAction );
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), code, intent, 0);
        return new NotificationCompat.Action.Builder( icon, title, pendingIntent ).build();
    }

    private void buildNotification( NotificationCompat.Action action ) {

        Uri mediaPath = Uri.parse("android.resource://" + getPackageName() + "/" + songs[counter]);
        mmr.setDataSource(this, mediaPath);
        String title=mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        String author=mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR);


        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music)
                .setContentTitle(title)
                .setContentText(author)
                .setLargeIcon(picture)
                .addAction(generateAction( android.R.drawable.ic_media_previous, "Previous", ACTION_PREVIOUS, 3))
                .addAction(action)
                .addAction(generateAction( android.R.drawable.ic_media_next,  "Next", ACTION_NEXT, 4))
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0,1,2)
                        .setMediaSession(mSession.getSessionToken()))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(1, notification);


    }

    @Override
    public boolean onUnbind(Intent intent) {
        mSession.release();
        return super.onUnbind(intent);
    }

    private class DownloadImage extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... URL) {

            String imageURL = "https://res.cloudinary.com/dx5kjjpce/image/upload/d_default.svg/2788f359d686032eeebbbb4c908ea9f6.jpg";

            Bitmap bitmap = null;
            try {
// Download Image from URL
                InputStream input = new java.net.URL(imageURL).openStream();
// Decode Bitmap
                bitmap = BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                Log.e("NO PICTURE", e.toString());
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            picture = result;
            Log.e("PICTURE", "onPostExecute" );
            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE, 1));
        }
    }


}
