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
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import static com.example.backgroundservice.App.CHANNEL_ID;

public class MediaService extends Service {

    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_STOP = "action_stop";

//    private ArrayList<HashMap<String,String>> songList = new ArrayList<>();

    public int[] songs = {R.raw.say_amen, R.raw.high_hopes,R.raw.look_ma};
    private int counter = 0;
    public MediaPlayer mMediaPlayer;
    private MediaSessionCompat mSession;
    private MediaControllerCompat mController;
    private PlaybackStateCompat state;
    private MediaMetadataRetriever mmr = new MediaMetadataRetriever();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
                                     buildNotification( generateAction( android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE , 1) );
                                     Log.e("onPlay", "onStartCommand: "+mMediaPlayer.isPlaying());
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
                                 }

                                 @Override
                                 public void onSkipToNext() {
                                     super.onSkipToNext();
                                     Log.e( "MediaPlayerService", "onSkipToNext");
                                     counter+=1;
                                     mMediaPlayer.release();
                                     mMediaPlayer = MediaPlayer.create(getApplicationContext(), songs[counter]);
                                     mMediaPlayer.start();
                                     state = new PlaybackStateCompat.Builder()
                                             .setState(PlaybackStateCompat.STATE_PLAYING, 1, SystemClock.elapsedRealtime())
                                             .build();
                                     mSession.setPlaybackState(state);
                                     buildNotification( generateAction( android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE , 1) );
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
        byte [] data = mmr.getEmbeddedPicture();
        Bitmap picture;
        if(data != null){
            picture = BitmapFactory.decodeResource(getResources(), data.length);
        }else {
            picture = BitmapFactory.decodeResource(getResources(), R.drawable.enroute);
        }

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

//    public class getPlayList extends AsyncTask {
//        @Override
//        protected Object doInBackground(Object[] objects) {
//
//            Uri mediaPath = Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath());
//            Log.e("async", String.valueOf(mediaPath));
//            songList=getPlayList(String.valueOf(mediaPath));
////            Log.e("asyncTask", String.valueOf(songList.size()));
//            if(songList!=null){
//                for(int i=0;i<songList.size();i++){
//                    String fileName=songList.get(i).get("file_name");
//                    String filePath=songList.get(i).get("file_path");
//                    //here you will get list of file name and file path that present in your device
//                    Log.e("file details "," name ="+fileName +" path = "+filePath);
//                }
//            }
//            return null;
//        }
//
//        public ArrayList<HashMap<String,String>> getPlayList(String rootPath) {
//            ArrayList<HashMap<String,String>> fileList = new ArrayList<>();
//
//            try {
//                File rootFolder = new File(rootPath);
//                File[] files = rootFolder.listFiles(); //here you will get NPE if directory doesn't contains  any file,handle it like this.
//                Log.e("asyncTask", String.valueOf(files.length));
//                for (File file : files) {
//                    if (file.isDirectory()) {
//                        if (getPlayList(file.getAbsolutePath()) != null) {
//                            fileList.addAll(getPlayList(file.getAbsolutePath()));
//                        } else {
//                            break;
//                        }
//                    } else if (file.getName().endsWith(".mp3")) {
//                        HashMap<String, String> song = new HashMap<>();
//                        song.put("file_path", file.getAbsolutePath());
//                        song.put("file_name", file.getName());
//                        fileList.add(song);
//                    }
//                }
//                return fileList;
//            } catch (Exception e) {
//                Log.e("ERROR", String.valueOf(e));
//                return null;
//            }
//        }
//
//    }


    @Override
    public boolean onUnbind(Intent intent) {
        mSession.release();
        return super.onUnbind(intent);
    }
}
