package com.example.recorderaudio;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;

import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;

import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    MediaRecorder recorder;
    File audiofile = null;
    static final String TAG = "MediaRecording";
    Button startButton,stopButton;
    Chronometer timer;
    TextView txtStatus;
    int RECORDER_SAMPLERATE = 44100;
    int RECORDET_ENCODER_BITRATE = 16;
    int NUMBER_CHANNELS = 1;

    String[] appPermissoes = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };

    public static final int CODIGO_PERMISSOES_REQUERIDAS = 1;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startButton =  findViewById(R.id.btnStart);
        stopButton = findViewById(R.id.btnStop);
        txtStatus = findViewById(R.id.txtStatus);
        timer = findViewById(R.id.record_timer);

    }

    private boolean verificarPermissoes() {
        List<String> permissoesRequeridas = new ArrayList<>();

        for(String permissao: appPermissoes){
            if (ContextCompat.checkSelfPermission(this, permissao) != PackageManager.PERMISSION_GRANTED){
                permissoesRequeridas.add(permissao);
            }
        }

        if(!permissoesRequeridas.isEmpty()){
            ActivityCompat.requestPermissions(this,
                    permissoesRequeridas.toArray(new String[permissoesRequeridas.size()]), CODIGO_PERMISSOES_REQUERIDAS);
            return false;
        }

        //Toast.makeText(this, "Todas as permissões estão ativas", Toast.LENGTH_LONG).show();
        return true;
    }

    public void startRecording(View view) throws IOException {
        txtStatus.setVisibility(txtStatus.VISIBLE);

        //setBackgroundColor(getResources().getColor(R.color.purple_200));
        startButton.setBackgroundColor(getResources().getColor(R.color.marrom_escuro));
        stopButton.setBackgroundColor(getResources().getColor(R.color.marrom));

        if (verificarPermissoes()) {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            //Creating file

            File dir = Environment.getExternalStorageDirectory();

            try {
                audiofile = File.createTempFile("sound_" , ".wav", dir);
            } catch (IOException e) {
                Log.e(TAG, "external storage access error");
                return;
            }

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setAudioEncodingBitRate(RECORDET_ENCODER_BITRATE);
            recorder.setAudioSamplingRate(RECORDER_SAMPLERATE);
            recorder.setAudioChannels(NUMBER_CHANNELS);
            recorder.setOutputFile(audiofile.getAbsolutePath());
            recorder.prepare();
            recorder.start();

            timer.setBase(SystemClock.elapsedRealtime());
            timer.start();
        }

        txtStatus.setText("Recording");

    }

    public void stopRecording(View view) {
        stopButton.setBackgroundColor(getResources().getColor(R.color.marrom_escuro));
        startButton.setBackgroundColor(getResources().getColor(R.color.marrom));

        timer.stop();
        try {
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            //stopping recorder
            recorder.stop();
            recorder.release();
            //after stopping the recorder, create the sound file and add it to media library.
            addRecordingToMediaLibrary();
            txtStatus.setText("Gravação salva");
        }catch (Exception e){
            txtStatus.setText("Inicie a gravação");
        }

        //txtStatus.setVisibility(txtStatus.INVISIBLE);
    }

    protected void addRecordingToMediaLibrary() {
        //creating content values of size 4
        ContentValues values = new ContentValues(4);
        long current = System.currentTimeMillis();
        values.put(MediaStore.Audio.Media.TITLE, "audio" + audiofile.getName());
        values.put(MediaStore.Audio.Media.DATE_ADDED, (int) (current / 1000));
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/3gpp");
        values.put(MediaStore.Audio.Media.DATA, audiofile.getAbsolutePath());

        //creating content resolver and storing it in the external content uri
        ContentResolver contentResolver = getContentResolver();
        Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Uri newUri = contentResolver.insert(base, values);

        //sending broadcast message to scan the media file so that it can be available
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, newUri));
        Toast.makeText(this, "Added File " + newUri, Toast.LENGTH_LONG).show();
    }
}