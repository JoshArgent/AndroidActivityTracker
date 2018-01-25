package uk.ac.nottingham.psyja2.activitytracker;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

public class ProgressActivity extends AppCompatActivity {

    // GUI components
    private TextView timeLabel;
    private TextView distanceLabel;
    private TextView paceLabel;
    private Button stopButton;

    // Service binder
    private ActivityTrackerService.ActivityTrackerBinder trackerService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        // Set the default title bar text
        this.setTitle("Activity Progress");

        // Get the UI components
        timeLabel = (TextView) findViewById(R.id.timeLabel);
        distanceLabel = (TextView) findViewById(R.id.distanceLabel);
        paceLabel = (TextView) findViewById(R.id.paceLabel);
        stopButton = (Button) findViewById(R.id.stopButton);

        // When the stop button is pressed, stop the tracker service
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                // Unbind and stop the tracker service
                int activityID = trackerService.getActivityID();
                unbindService(serviceConnection);
                serviceConnection = null;
                stopService(new Intent(ProgressActivity.this, ActivityTrackerService.class));

                // Start the ViewActivity activity and display the result for the activity that was been tracked
                Intent viewActivityIntent = new Intent(ProgressActivity.this, ViewActivity.class);
                Bundle viewActivityBundle = new Bundle(); // The activity ID is passed in a bundle so it knows which record to display
                viewActivityBundle.putInt("activity_ID", activityID);
                viewActivityIntent.putExtras(viewActivityBundle);
                startActivity(viewActivityIntent); // Launch the activity
                finish(); // End this activity
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // Bind to the tracker service (if the service is not running, immediately close this activity)
        if(ActivityTrackerService.serviceRunning)
        {
            bindService(new Intent(this, ActivityTrackerService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        }
        else
        {
            // Service is not running. The service should be started BEFORE this activity is started
            finish();
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // Unbind from the service (if it is running)
        if(serviceConnection != null)
        {
            if(ActivityTrackerService.serviceRunning)
                unbindService(serviceConnection);
            serviceConnection = null;
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public void onBackPressed()
    {
        // Prevent going back by disabling the back button.
        // super.onBackPressed();   <-- this disables it
    }

    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            // Register the callback
            trackerService = (ActivityTrackerService.ActivityTrackerBinder) service;
            trackerService.registerCallback(trackerServiceCallback);

            // Display the progress values from the service
            timeLabel.setText(Utils.secondsToTimeString(trackerService.getTime()));
            distanceLabel.setText(String.format("%.2f miles", trackerService.getDistance()));
            paceLabel.setText(Utils.secondsToTimeString((int) trackerService.getAveragePace()) + " minutes/mile");
            // Update the title to include the activity type
            setTitle("Activity Progress: " + trackerService.getActivityType());
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            // Unregister the callback
            trackerService.unregisterCallback(trackerServiceCallback);
            trackerService = null;
        }
    };

    ActivityTrackerServiceCallback trackerServiceCallback = new ActivityTrackerServiceCallback() {

        @Override
        public void activityProgressUpdate(final int time, final float miles, final float averagePace)
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run()
                {
                    // On the UI thread, update the GUI with the latest values from the tracker service
                    timeLabel.setText(Utils.secondsToTimeString(time));
                    distanceLabel.setText(String.format("%.2f miles", miles));
                    paceLabel.setText(Utils.secondsToTimeString((int) averagePace) + " minutes/mile");
                }
            });
        }

    };

}
