package com.example.backgroundservice;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity{

    public int[] songs = {R.raw.say_amen, R.raw.high_hopes,R.raw.look_ma};
    public List<String> names = new ArrayList<>();
    private ListView listView;
    private ImageView nextView, prevView;
    public ImageView playView;
    private ArrayAdapter adapter;
    private Uri mediaPath;
    private MediaMetadataRetriever mmr = new MediaMetadataRetriever();
    public Boolean isMusicPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.trackList);

        nextView = findViewById(R.id.nextB);
        playView = findViewById(R.id.playPauseB);
        prevView = findViewById(R.id.prevB);

        for(int i=0;i<songs.length;i++){
            Uri mediaPath = Uri.parse("android.resource://" + getPackageName() + "/" + songs[i]);
            mmr.setDataSource(this, mediaPath);
            names.add(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
        }

        adapter = new ArrayAdapter(this, R.layout.track, R.id.song, names);
        listView.setAdapter(adapter);

        nextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextTrack();
            }
        });
        playView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playPause();
            }
        });
        prevView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                previousTrack();
             }
        });

//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//
//                Intent serviceIntent = new Intent(getApplicationContext(), MediaService.class);
//                serviceIntent.setAction( MediaService.ACTION_PLAY );
//                serviceIntent.putExtra("TRACK", songs[position]);
//                serviceIntent.putExtra("NEWTRACK", true);
//                ContextCompat.startForegroundService(getApplicationContext(), serviceIntent);
//            }
//
//        });

    }

    public void previousTrack(){
        Intent serviceIntent = new Intent(getApplicationContext(), MediaService.class);
        serviceIntent.setAction( MediaService.ACTION_PREVIOUS );
        startService(serviceIntent);
    }

    public void playPause(){
        Intent serviceIntent = new Intent(getApplicationContext(), MediaService.class);
        if(!isMusicPlaying) {
            serviceIntent.setAction(MediaService.ACTION_PLAY);
//            playView.setImageResource(R.drawable.ic_pause);
            isMusicPlaying = true;
        }else {
            serviceIntent.setAction(MediaService.ACTION_PAUSE);
//            playView.setImageResource(R.drawable.ic_play);
            isMusicPlaying = false;
        }
        startService(serviceIntent);
    }

    public void changeMedia(Boolean playing){
        ImageView view = findViewById(R.id.playPauseB);
        if(playing){
            view.setImageResource(R.drawable.ic_pause);
        }else{
            view.setImageResource(R.drawable.ic_play);
        }
    }

    public void nextTrack(){
        Intent serviceIntent = new Intent(getApplicationContext(), MediaService.class);
        serviceIntent.setAction( MediaService.ACTION_NEXT );
        startService(serviceIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent serviceIntent = new Intent(this, MediaService.class);
        serviceIntent.setAction( MediaService.ACTION_STOP );
        startService(serviceIntent);
    }



}