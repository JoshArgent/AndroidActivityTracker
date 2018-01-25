package uk.ac.nottingham.psyja2.activitytracker;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SummaryActivity extends AppCompatActivity {

    // GUI components
    private Button newActivityButton;
    private Button fullHistoryButton;
    private TextView milesThisWeek;
    private TextView milesThisMonth;
    private TextView milesTotal;
    private TextView avgPaceRunning;
    private TextView avgPaceWalking;
    private TextView avgPaceCycling;
    private TextView avgPaceRowing;
    private TextView bestPaceRunning;
    private TextView bestPaceWalking;
    private TextView bestPaceCycling;
    private TextView bestPaceRowing;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        // Set the title bar text
        this.setTitle("Summary");

        // Get the UI components
        newActivityButton = (Button) findViewById(R.id.newActivityButton);
        fullHistoryButton = (Button) findViewById(R.id.fullHistoryButton);
        milesThisWeek = (TextView) findViewById(R.id.milesThisWeek);
        milesThisMonth = (TextView) findViewById(R.id.milesThisMonth);
        milesTotal = (TextView) findViewById(R.id.milesTotal);
        avgPaceRunning = (TextView) findViewById(R.id.avgPaceRunning);
        avgPaceWalking = (TextView) findViewById(R.id.avgPaceWalking);
        avgPaceCycling = (TextView) findViewById(R.id.avgPaceCycling);
        avgPaceRowing = (TextView) findViewById(R.id.avgPaceRowing);
        bestPaceRunning = (TextView) findViewById(R.id.bestPaceRunning);
        bestPaceWalking = (TextView) findViewById(R.id.bestPaceWalking);
        bestPaceCycling = (TextView) findViewById(R.id.bestPaceCycling);
        bestPaceRowing = (TextView) findViewById(R.id.bestPaceRowing);

        // When the 'New Activity' button is pressed a 'NewActivity' is started
        newActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(SummaryActivity.this, NewActivity.class);
                startActivity(intent);
            }
        });

        // When the 'Full History' button is pressed launch the HistoryActivity
        fullHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                // Start the HistoryActivity
                Intent intent = new Intent(SummaryActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onStart()
    {
        super.onStart();

        // This is the launch activity for the application
        // Check if the ActivityTrackerService is already running
        // If it is, launch the ProgressActivity
        if(ActivityTrackerService.serviceRunning)
        {
            startActivity(new Intent(this, ProgressActivity.class));
        }

    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // Use the content provider to get the users activity stats
        float milesThisWeekVal = queryStat(ActivitiesContentProviderContract.ACTIVITY_DISTANCE_THIS_WEEK_URI);
        float milesThisMonthVal = queryStat(ActivitiesContentProviderContract.ACTIVITY_DISTANCE_THIS_MONTH_URI);
        float milesTotalVal = queryStat(ActivitiesContentProviderContract.ACTIVITY_DISTANCE_TOTAL_URI);
        float avgPaceRunningVal = queryStat(Uri.parse(ActivitiesContentProviderContract.ACTIVITY_AVERAGE_PACE_URI + "/" + ActivitiesContentProviderContract.ACTIVITY_TYPE_RUNNING));
        float avgPaceWalkingVal = queryStat(Uri.parse(ActivitiesContentProviderContract.ACTIVITY_AVERAGE_PACE_URI + "/" + ActivitiesContentProviderContract.ACTIVITY_TYPE_WALKING));
        float avgPaceCyclingVal = queryStat(Uri.parse(ActivitiesContentProviderContract.ACTIVITY_AVERAGE_PACE_URI + "/" + ActivitiesContentProviderContract.ACTIVITY_TYPE_CYCLING));
        float avgPaceRowingVal = queryStat(Uri.parse(ActivitiesContentProviderContract.ACTIVITY_AVERAGE_PACE_URI + "/" + ActivitiesContentProviderContract.ACTIVITY_TYPE_ROWING));
        float bestPaceRunningVal = queryStat(Uri.parse(ActivitiesContentProviderContract.ACTIVITY_BEST_PACE_URI + "/" + ActivitiesContentProviderContract.ACTIVITY_TYPE_RUNNING));
        float bestPaceWalkingVal = queryStat(Uri.parse(ActivitiesContentProviderContract.ACTIVITY_BEST_PACE_URI + "/" + ActivitiesContentProviderContract.ACTIVITY_TYPE_WALKING));
        float bestPaceCyclingVal = queryStat(Uri.parse(ActivitiesContentProviderContract.ACTIVITY_BEST_PACE_URI + "/" + ActivitiesContentProviderContract.ACTIVITY_TYPE_CYCLING));
        float bestPaceRowingVal = queryStat(Uri.parse(ActivitiesContentProviderContract.ACTIVITY_BEST_PACE_URI + "/" + ActivitiesContentProviderContract.ACTIVITY_TYPE_ROWING));

        // Display the values in a nicely formatted way. If no data is available then display "--"
        milesThisWeek.setText(milesThisWeekVal <= 0 ? "--" : String.format("%.2f", milesThisWeekVal));
        milesThisMonth.setText(milesThisMonthVal <= 0 ? "--" : String.format("%.2f", milesThisMonthVal));
        milesTotal.setText(milesTotalVal <= 0 ? "--" : String.format("%.2f", milesTotalVal));
        avgPaceRunning.setText(avgPaceRunningVal <= 0 ? "--" : Utils.secondsToTimeString((int)avgPaceRunningVal));
        avgPaceWalking.setText(avgPaceWalkingVal <= 0 ? "--" : Utils.secondsToTimeString((int) avgPaceWalkingVal));
        avgPaceCycling.setText(avgPaceCyclingVal <= 0 ? "--" : Utils.secondsToTimeString((int) avgPaceCyclingVal));
        avgPaceRowing.setText(avgPaceRowingVal <= 0 ? "--" : Utils.secondsToTimeString((int) avgPaceRowingVal));
        bestPaceRunning.setText(bestPaceRunningVal <= 0 ? "--" : Utils.secondsToTimeString((int)bestPaceRunningVal));
        bestPaceWalking.setText(bestPaceWalkingVal <= 0 ? "--" : Utils.secondsToTimeString((int)bestPaceWalkingVal));
        bestPaceCycling.setText(bestPaceCyclingVal <= 0 ? "--" : Utils.secondsToTimeString((int)bestPaceCyclingVal));
        bestPaceRowing.setText(bestPaceRowingVal <= 0 ? "--" : Utils.secondsToTimeString((int)bestPaceRowingVal));
    }

    /*
    Executes a query on the content provider and returns the first value of the first record, as a float
     */
    private float queryStat(Uri uri)
    {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if(cursor != null && cursor.moveToFirst())
            return cursor.getFloat(0);
        else
            return -1;
    }


}
