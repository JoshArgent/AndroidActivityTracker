package uk.ac.nottingham.psyja2.activitytracker;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ViewActivity extends AppCompatActivity {

    // GUI components
    private TextView distanceLabel;
    private TextView timeLabel;
    private TextView paceLabel;
    private ActivityPaceGraphView graphView;
    private Button gpxExportButton;

    // The record ID of the activity been displayed
    private int activityID;

    // The location and time data for each recorded point
    private List<Location> locations;
    private List<Long> times;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);

        // Get the UI components
        distanceLabel = (TextView) findViewById(R.id.distanceLabel);
        timeLabel = (TextView) findViewById(R.id.timeLabel);
        paceLabel = (TextView) findViewById(R.id.paceLabel);
        graphView = (ActivityPaceGraphView) findViewById(R.id.mapView);
        gpxExportButton = (Button) findViewById(R.id.gpxExportButton);

        // When the GPX Export button is pressed, export the route to a GPX file and launch the email activity
        gpxExportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                // Convert the recorded data to GPX XML data
                String gpxContent = Utils.convertToGPXFile("Activity Tracker Route", locations, times);

                // Save this data to a file in the external storage directory
                File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "activity_tracker_route.gpx");
                try
                {
                    FileOutputStream stream = new FileOutputStream(file);
                    stream.write(gpxContent.getBytes());
                    stream.close();
                }
                catch (FileNotFoundException e)
                {
                    e.printStackTrace();
                    return;
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    return;
                }

                // Launching an email intent - credit to Shankar Agarwal
                // https://stackoverflow.com/questions/9974987/how-to-send-an-email-with-a-file-attachment-in-android/9975439

                // Code for using a File Content Provider from:
                // https://inthecheesefactory.com/blog/how-to-share-access-to-file-with-fileprovider-on-android-nougat/en

                // Launch an email intent to send this file as an attachment
                Uri path = FileProvider.getUriForFile(ViewActivity.this,
                        BuildConfig.APPLICATION_ID + ".provider",
                        file);
                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                // set the type to 'email'
                emailIntent.setType("vnd.android.cursor.dir/email");
                // the attachment
                emailIntent.putExtra(Intent.EXTRA_STREAM, path);
                // the mail subject
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Activity Tracker GPX Export");
                startActivity(Intent.createChooser(emailIntent , "Send email..."));
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        Bundle bundle = getIntent().getExtras();
        if(bundle != null)
        {
            if(bundle.containsKey("activity_ID"))
            {
                // Get the ID of the activity to display
                activityID = bundle.getInt("activity_ID");

                // Setup the projection for the query
                String[] projection = new String[] {
                        ActivitiesContentProviderContract.ID,
                        ActivitiesContentProviderContract.DATE,
                        ActivitiesContentProviderContract.TYPE,
                        ActivitiesContentProviderContract.TIME,
                        ActivitiesContentProviderContract.DISTANCE,
                        ActivitiesContentProviderContract.PACE
                };

                // Run a query to the content provider and get the cursor object for the activity with id=activityID
                Uri queryUri = ContentUris.withAppendedId(ActivitiesContentProviderContract.ACTIVITIES_URI, activityID);
                Cursor cursor = getContentResolver().query(queryUri, projection, null, null, null);

                // Get the activity data from the returned record (if it exists)
                if(cursor.moveToFirst())
                {
                    // Get the raw values
                    long date = cursor.getLong(cursor.getColumnIndex(ActivitiesContentProviderContract.DATE));
                    String type = cursor.getString(cursor.getColumnIndex(ActivitiesContentProviderContract.TYPE));
                    int time = cursor.getInt(cursor.getColumnIndex(ActivitiesContentProviderContract.TIME));
                    float distance = cursor.getFloat(cursor.getColumnIndex(ActivitiesContentProviderContract.DISTANCE));
                    float pace = cursor.getFloat(cursor.getColumnIndex(ActivitiesContentProviderContract.PACE));

                    // Format the values into nice strings
                    String dateStr = Utils.timestampToDateString(date);
                    String distanceStr = String.format("%.2f miles", distance);
                    String timeStr = Utils.secondsToTimeString(time) + " minutes";
                    String paceStr = Utils.secondsToTimeString((int)pace) + " minutes/mile";

                    // Display the values
                    this.setTitle(dateStr + ": " + type);
                    distanceLabel.setText(distanceStr);
                    timeLabel.setText(timeStr);
                    paceLabel.setText(paceStr);

                    // Now load the recorded locations and display the pace graph
                    loadLocationData();
                }
                else
                {
                    // No record with ID=activityID exists
                    finish();
                }
            }
            else
            {
                // No activity ID specified, can not show any data
                finish();
            }
        }
        else
        {
            // No activity ID specified, can not show any data
            finish();
        }
    }

    /*
    Queries the recorded location points and updates the pace graph data
     */
    private void loadLocationData()
    {
        // Setup the projection for the query
        String[] projection = new String[] {
                ActivitiesContentProviderContract.ID,
                ActivitiesContentProviderContract.LATITUDE,
                ActivitiesContentProviderContract.LONGITUDE,
                ActivitiesContentProviderContract.ALTITUDE,
                ActivitiesContentProviderContract.LOCATION_TIME
        };

        // Run a query to the content provider and get the cursor object for the activity with id=activityID
        Uri queryUri = ContentUris.withAppendedId(ActivitiesContentProviderContract.LOCATIONS_FOR_ACTIVITY_URI, activityID);
        Cursor cursor = getContentResolver().query(queryUri, projection, null, null, null);

        // Read all the locations from the result
        locations = new ArrayList<>();
        times = new ArrayList<>();
        while(cursor.moveToNext())
        {
            float lat = cursor.getFloat(cursor.getColumnIndex(ActivitiesContentProviderContract.LATITUDE));
            float lng = cursor.getFloat(cursor.getColumnIndex(ActivitiesContentProviderContract.LONGITUDE));
            Location location = new Location("");
            location.setLatitude(lat);
            location.setLongitude(lng);
            locations.add(location);
            times.add(cursor.getLong(cursor.getColumnIndex(ActivitiesContentProviderContract.LOCATION_TIME)));
        }

        // Send this data to the pace graph and redraw it
        graphView.loadData(locations, times);
        graphView.invalidate();

    }

}
