package com.example.contactform;

import android.app.AlertDialog;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.util.HashMap;

public class ResultActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private int pausePosition = 0;
    private String currentAudioFilePath;  // Class-level variable to store the audio file path

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        HashMap<String, Object> answers = (HashMap<String, Object>) getIntent().getSerializableExtra("answers");

        TableLayout table = findViewById(R.id.resultTable);

        // Add table rows with the data
        for (String key : answers.keySet()) {
            TableRow row = new TableRow(this);

            TextView keyView = new TextView(this);
            keyView.setText(key);
            keyView.setPadding(8, 8, 8, 8);

            TextView valueView = new TextView(this);
            valueView.setText(answers.get(key).toString());
            valueView.setPadding(8, 8, 8, 8);

            row.addView(keyView);
            row.addView(valueView);

            // Add "Play Audio" button if the key is "recording"
            if (key.equals("recording")) {
                Button playButton = new Button(this);
                playButton.setText("Play Audio");
                playButton.setOnClickListener(v -> playAudio(answers.get(key).toString()));
                row.addView(playButton);

                Button stopButton = new Button(this);
                stopButton.setText("Stop Audio");
                stopButton.setOnClickListener(v -> stopAudio());
                row.addView(stopButton);

                Button resumeButton = new Button(this);
                resumeButton.setText("Resume Audio");
                resumeButton.setOnClickListener(v -> resumeAudio());
                row.addView(resumeButton);

                Button restartButton = new Button(this);
                restartButton.setText("Play from Start");
                restartButton.setOnClickListener(v -> restartAudio());
                row.addView(restartButton);
            }

            // Add "View Image" button if the key is "Q3" (selfie)
            if (key.equals("Q3")) {
                Button viewButton = new Button(this);
                viewButton.setText("View Image");
                viewButton.setOnClickListener(v -> viewImage(answers.get(key).toString()));
                row.addView(viewButton);
            }

            table.addView(row);
        }
    }

    private void playAudio(String audioFilePath) {
        stopAudio(); // Stop any existing playback before starting a new one
        currentAudioFilePath = audioFilePath; // Save the audio file path
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioFilePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            Toast.makeText(this, "Audio Playing ...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            pausePosition = 0; // Reset pause position
            Toast.makeText(this, "Audio Stop ...", Toast.LENGTH_SHORT).show();
        }
    }

    private void resumeAudio() {
        if (mediaPlayer == null && currentAudioFilePath != null) {
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(currentAudioFilePath);
                mediaPlayer.prepare();
                mediaPlayer.seekTo(pausePosition);
                mediaPlayer.start();
                Toast.makeText(this, "Audio Resume...", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(pausePosition);
            mediaPlayer.start();
        }
    }

    private void restartAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(0);
            mediaPlayer.start();
            Toast.makeText(this, "Restart Audio...", Toast.LENGTH_SHORT).show();
        } else if (currentAudioFilePath != null) {
            playAudio(currentAudioFilePath);
        }
    }

    private void viewImage(String imageFilePath) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Selfie Image");

        ImageView imageView = new ImageView(this);
        imageView.setImageURI(android.net.Uri.fromFile(new File(imageFilePath)));

        builder.setView(imageView);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
