package uk.ac.nottingham.psyja2.activitytracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;

public class NewActivity extends AppCompatActivity {

    // The ID of the permission request
    private static final int PERMISSION_REQUEST_ID = 1;

    // GUI components
    private Spinner activityTypeSpinner;
    private Button startButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new);

        // Set the title bar text
        this.setTitle("New Activity");

        // Get the UI components
        activityTypeSpinner = (Spinner) findViewById(R.id.activityTypeSpinner);
        startButton = (Button) findViewById(R.id.startButton);

        // When the start button is pressed start the ActivityTrackerService and launch the progress activity
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                // Immediately start the tracker service and send the activity type via a bundle
                Intent activityServiceIntent = new Intent(NewActivity.this, ActivityTrackerService.class);
                Bundle bundle = new Bundle();
                bundle.putString("activity_type", activityTypeSpinner.getSelectedItem().toString());
                activityServiceIntent.putExtras(bundle);
                startService(activityServiceIntent);

                // Launch the progress activity to display the progress (distance, time, etc..)
                Intent progressActivityIntent = new Intent(NewActivity.this, ProgressActivity.class);
                startActivity(progressActivityIntent);

                // End this activity
                finish();
            }
        });
    }

    @Override
    protected  void onResume()
    {
        // Make sure the user has appropriate permissions for GPS and Storage
        // Permission checking credit: https://developer.android.com/training/permissions/requesting.html
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            // One or more permission is missing, launch a permission request
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_ID);
        }

        super.onResume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        // Handle when a permission request has been completed
        // Credit: https://developer.android.com/training/permissions/requesting.html
        if(requestCode == PERMISSION_REQUEST_ID)
        {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            {
                // Still not been granted all the required permissions
                // Finish this activity - app is not usable without these permissions
                finish();
            }
        }
    }
}
