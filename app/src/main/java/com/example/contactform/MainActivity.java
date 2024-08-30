package com.example.contactform;


import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;


import android.location.Location;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.snackbar.Snackbar;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_LOCATION = 1;
    private static final int REQUEST_CAMERA = 2;
    private static final int REQUEST_AUDIO = 3;

    private Spinner genderSpinner;
    private EditText ageInput;
    private ImageView selfiePreview;
    private Button nextButton, previousButton, captureButton, submitButton;

    private MediaRecorder recorder;
    private String audioFilePath, selfieFilePath;

    private LocationManager locationManager;
    private Location userLocation;
    private int currentStep = 1;
    private HashMap<String, Object> answers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

// Check location settings when the activity is created
        checkLocationSettings();

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener, Looper.getMainLooper());
        }



        genderSpinner = findViewById(R.id.genderSpinner);
        ageInput = findViewById(R.id.ageInput);
        selfiePreview = findViewById(R.id.selfiePreview);
        captureButton = findViewById(R.id.captureButton);
        nextButton = findViewById(R.id.nextButton);
        previousButton = findViewById(R.id.previousButton);
        submitButton = findViewById(R.id.submitButton);

        answers = new HashMap<>();

        setupGenderSpinner();
        startRecording();

        captureButton.setOnClickListener(v -> captureSelfie());

        nextButton.setOnClickListener(v -> validateAndProceed());
        previousButton.setOnClickListener(v -> goBack());
        submitButton.setOnClickListener(v -> submitForm());

        requestPermissions();

    }


    private void checkLocationSettings() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    // Location settings are satisfied, start location updates
                    requestLocationAndSubmit();
                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied, but this can be fixed by showing the user a dialog.
                            try {
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                resolvable.startResolutionForResult(MainActivity.this, REQUEST_LOCATION);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied, and we can't fix it here.
                            showError("Location settings are inadequate, and cannot be fixed here. Please enable location manually.");
                            break;
                    }
                }
            }
        });
    }

    private void setupGenderSpinner() {
        String[] genders = {"Male", "Female", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, genders);
        genderSpinner.setAdapter(adapter);
    }

    private void validateAndProceed() {
        switch (currentStep) {
            case 1:
                int genderIndex = genderSpinner.getSelectedItemPosition();
                if (genderIndex == -1) {
                    showError("Please select your gender.");
                } else {
                    answers.put("Q1", genderIndex + 1);
                    moveToStep(2);
                }
                break;
            case 2:
                String age = ageInput.getText().toString();
                if (age.isEmpty()) {
                    showError("Please enter your age.");
                } else {
                    answers.put("Q2", Integer.parseInt(age));
                    moveToStep(3);
                }
                break;
            case 3:
                if (selfieFilePath == null) {
                    showError("Please capture your selfie.");
                } else {
                    moveToStep(4);
                    stopRecording();
                }
                break;
        }
    }

    private void moveToStep(int step) {
        currentStep = step;

        // Set visibility based on the current step
        findViewById(R.id.genderLayout).setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        findViewById(R.id.ageLayout).setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        findViewById(R.id.selfieLayout).setVisibility(step == 3 ? View.VISIBLE : View.GONE);

        // Set pagination buttons visibility
        previousButton.setVisibility(step > 1 ? View.VISIBLE : View.GONE);
        nextButton.setVisibility(step < 3 ? View.VISIBLE : View.GONE);
        submitButton.setVisibility(step == 3 ? View.VISIBLE : View.GONE);
    }
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            userLocation = location;
            locationManager.removeUpdates(this); // Stop updates once location is obtained
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override
        public void onProviderEnabled(String provider) {}
        @Override
        public void onProviderDisabled(String provider) {}
    };


    private void goBack() {
        if (currentStep > 1) moveToStep(currentStep - 1);
    }

    private void captureSelfie() {




        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            cameraIntent.putExtra("android.intent.extras.CAMERA_FACING", 1); // 1 for front camera
            cameraIntent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1); // Some devices use this key
            cameraIntent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true); // Some devices use this key
            startActivityForResult(cameraIntent, REQUEST_CAMERA);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            selfiePreview.setImageBitmap(photo);

            // Save selfie to file
            try {
                File selfieFile = new File(getExternalFilesDir(null), "selfie.png");
                FileOutputStream out = new FileOutputStream(selfieFile);
                photo.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.close();
                selfieFilePath = selfieFile.getAbsolutePath();
                answers.put("Q3", selfieFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Handle the location settings result
        if (requestCode == REQUEST_LOCATION) {
            if (resultCode == RESULT_OK) {
                // Location services are enabled, request location updates
                requestLocationAndSubmit();
            } else {
                // The user did not enable location services
                showError("Location services are required to submit the form.");
            }
        }
    }

    private void submitForm() {

        // Check if location is available
        if (userLocation == null) {
            requestLocationAndSubmit();
        } else {
            proceedWithSubmission();
        }
        if (answers.get("Q1") == null || answers.get("Q2") == null) {
            showError("Please complete all steps before submitting.");
            return;
        }

        // Stop recording immediately
        stopRecording();


    }

    private void requestLocationAndSubmit() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            // Request location updates and wait for location before submitting
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener, Looper.getMainLooper());
            showError("Waiting for location... Please try submitting again shortly.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, request location again
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener, Looper.getMainLooper());
            } else {
                showError("Location permission is required to submit the form.");
            }
        }
    }

    private void proceedWithSubmission() {
        if (userLocation != null) {
            String gps = userLocation.getLatitude() + "," + userLocation.getLongitude();
            answers.put("gps", gps);
        } else {
            answers.put("gps", "Location not available");
        }

        String submissionTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        answers.put("submit_time", submissionTime);
        answers.put("recording", audioFilePath);

        saveDataAsJson();

        Intent intent = new Intent(MainActivity.this, ResultActivity.class);
        intent.putExtra("answers", answers);
        startActivity(intent);
    }



    private void saveDataAsJson() {
        try {
            File jsonFile = new File(getExternalFilesDir(null), "submission.json");
            FileOutputStream out = new FileOutputStream(jsonFile);
            out.write(answers.toString().getBytes());
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        }, REQUEST_LOCATION);
    }

    private void startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO);
        } else {
            File audioFile = new File(getExternalFilesDir(null), "recording.wav");
            audioFilePath = audioFile.getAbsolutePath();

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(audioFilePath);

            try {
                recorder.prepare();
                recorder.start();
                Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecording() {
        if (recorder != null) {
            try {
                recorder.stop();
                recorder.release();
                recorder = null;
                Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
            } catch (RuntimeException e) {
                Toast.makeText(this, "Failed to stop recording", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void showError(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }

}
