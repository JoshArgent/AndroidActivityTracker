package uk.ac.nottingham.psyja2.activitytracker;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteCallbackList;
import android.support.v7.app.NotificationCompat;
import android.util.Log;


/**
 * Android Service to track the movement of a device during an activity
 * Records all data in the ActivitiesContentProvider
 * Created by Josh on 18/12/2017.
 */
public class ActivityTrackerService extends Service {

    // Constants:
    private static final float METRES_PER_MILE = 0.000621371f;
    private static final int NOTIFICATION_ID = 1;

    // A flag that other components can use to see if the service is already running
    public static boolean serviceRunning = false;

    // Callback listeners
    private RemoteCallbackList<ActivityTrackerBinder> remoteCallbackList = new RemoteCallbackList<ActivityTrackerBinder>();

    // Timer thread
    private ActivityTrackerTimer timerThread;
    // Location manager and listener objects
    private LocationManager locationManager;
    private ActivityTrackerLocationListener locationListener;

    // State variables
    private int time = 0;
    private float miles = 0;
    private float averagePace = 0;
    private int activityID = -1;
    private Uri activityURI;
    private Location previousLocation;
    private String activityType;

    /*
    A timer thread that will increment the timer variable every second
     */
    public class ActivityTrackerTimer extends Thread implements Runnable
    {
        // Set this flag to false to stop the timer thread
        public boolean running = true;

        public ActivityTrackerTimer()
        {
            // Start the thread
            time = 0;
            this.start();
        }

        public void run()
        {
            while(this.running)
            {
                // Pause for 1 seconds
                try {Thread.sleep(1000);} catch(Exception e) {return;}

                // Increment the timer
                time += 1;

                // Notify callback listeners of update
                doCallbacks(time, miles, averagePace);
            }
        }
    }

    /*
    Location listener will listen for changes in the GPS location service
     */
    public class ActivityTrackerLocationListener implements LocationListener
    {
        @Override
        public void onLocationChanged(Location location)
        {
            // If this is not the first location, recalculate the distance and pace values
            if(previousLocation != null)
            {
                miles += location.distanceTo(previousLocation) * METRES_PER_MILE;
                averagePace = time / miles;
            }

            // Save this location for future calculations
            previousLocation = location;

            // Insert this location into the LOCATIONS table using the content provider
            ContentValues values = new ContentValues();
            values.put(ActivitiesContentProviderContract.LATITUDE, location.getLatitude());
            values.put(ActivitiesContentProviderContract.LONGITUDE, location.getLongitude());
            values.put(ActivitiesContentProviderContract.ALTITUDE, location.getAltitude());
            values.put(ActivitiesContentProviderContract.LOCATION_TIME, System.currentTimeMillis());
            getContentResolver().insert(activityURI, values);

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
        }

        @Override
        public void onProviderEnabled(String provider)
        {
        }

        @Override
        public void onProviderDisabled(String provider)
        {
        }
    }

    /*
    Tell all the callback listeners that the tracking values have changed (time/distance/pace)
     */
    public void doCallbacks(int time, float miles, float averagePace)
    {
        // Loop through each callback listener
        final int n = remoteCallbackList.beginBroadcast();
        for (int i = 0; i < n; i++)
        {
            remoteCallbackList.getBroadcastItem(i).callback.activityProgressUpdate(time, miles, averagePace);
        }
        remoteCallbackList.finishBroadcast();

        // Update the service notification to display the new time/distance values
        updateNotification(time, miles);
    }

    /*
    The Binder class for the tracker service
     */
    public class ActivityTrackerBinder extends Binder implements IInterface
    {
        @Override
        public IBinder asBinder()
        {
            return this;
        }

        /*
        Return the Database ID of the activity that is been recorded
         */
        public int getActivityID()
        {
            return activityID;
        }

        /*
        Return the distance travelled so far
         */
        public float getDistance()
        {
            return miles;
        }

        /*
        Return the average pace
         */
        public float getAveragePace()
        {
            return averagePace;
        }

        /*
        Return the time elapsed so far
         */
        public int getTime()
        {
            return time;
        }

        /*
        Return the type of activity been tracked
         */
        public String getActivityType()
        {
            return activityType;
        }

        /*
        Register a callback object
         */
        public void registerCallback(ActivityTrackerServiceCallback callback)
        {
            this.callback = callback;
            remoteCallbackList.register(ActivityTrackerBinder.this);
        }

        /*
        Unregister a callback object
         */
        public void unregisterCallback(ActivityTrackerServiceCallback callback)
        {
            remoteCallbackList.unregister(ActivityTrackerBinder.this);
        }

        ActivityTrackerServiceCallback callback;
    }

    /*
    Update the notification with the latest distance and time values
     */
    private void updateNotification(int time, float miles)
    {
        // Create the distance and time strings to display
        String milesStr = String.format("%.2f miles", miles);
        String timeStr = Utils.secondsToTimeString(time);

        // Display a notification to indicate the service is running
        // The pending intent should open the ProgressActivity
        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, ProgressActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(this)
                .setTicker(("message"))
                .setSmallIcon(R.drawable.ic_directions_walk_24dp)
                .setContentTitle("Activity Tracker")
                .setContentText(timeStr + " " + milesStr)
                .setContentIntent(pi)
                .setAutoCancel(false)
                .build();

        // Set the service to run in the foreground with this notification
        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        // Create a new record in the ACTIVITIES table and save the ID
        ContentValues activityValues = new ContentValues();
        activityValues.put(ActivitiesContentProviderContract.DATE, System.currentTimeMillis());
        activityValues.put(ActivitiesContentProviderContract.TYPE, ActivitiesContentProviderContract.ACTIVITY_TYPE_RUNNING); // default to running
        Uri queryResult = getContentResolver().insert(ActivitiesContentProviderContract.ACTIVITIES_URI, activityValues);
        activityID = Integer.valueOf(queryResult.getLastPathSegment()); // Store the ID of the newly created activity record
        activityURI = ContentUris.withAppendedId(ActivitiesContentProviderContract.LOCATIONS_FOR_ACTIVITY_URI, activityID);

        // Create and start a new timer thread
        timerThread = new ActivityTrackerTimer();

        // Register for GPS location updates from the system Location Service
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new ActivityTrackerLocationListener();
        try
        {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    5, // minimum time interval between updates
                    5, // minimum distance between updates, in metres
                    locationListener);
        } catch(SecurityException e) {
            Log.d("g53mdp", e.toString());
        }

        // Display the foreground notification
        updateNotification(0, 0);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return new ActivityTrackerBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        // Check to see if anything was passed in the intent bundle
        if(intent != null && intent.getExtras() != null)
        {
            if(intent.getExtras().containsKey("activity_type"))
            {
                // The activity type has been passed
                activityType = intent.getExtras().getString("activity_type");
                // Update the ACTIVITY type in the content provider to what was passed via the bundle
                ContentValues activityValues = new ContentValues();
                activityValues.put(ActivitiesContentProviderContract.TYPE, activityType);
                Uri updateURI = ContentUris.withAppendedId(ActivitiesContentProviderContract.ACTIVITIES_URI, activityID);
                getContentResolver().update(updateURI, activityValues, null, null);
            }
            else if(intent.getExtras().containsKey("low_battery_warning"))
            {
                // Received low battery broadcast
                try
                {
                    locationManager.removeUpdates(locationListener);
                    // Re-register for location updates but with less accurate/frequent readings
                    // At least 5 metres of movement and 10 second time intervals
                    // This will hopefully reduce the battery consumption rate
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                            10000, // minimum time interval between updates
                            50, // minimum distance between updates, in metres
                            locationListener);
                } catch(SecurityException e) {
                    Log.d("g53mdp", e.toString());
                }
            }
        }

        // Set the serviceRunning flag to true
        serviceRunning = true;

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        // Stop the timer thread
        timerThread.running = false;
        timerThread = null;

        // Unregister the GPS location listener from the system Location Service
        try
        {
            locationManager.removeUpdates(locationListener);
        } catch(SecurityException e) {
            Log.d("g53mdp", e.toString());
        }

        // Update the ACTIVITY record with the final distance, time, pace values
        ContentValues activityValues = new ContentValues();
        activityValues.put(ActivitiesContentProviderContract.TIME, time);
        activityValues.put(ActivitiesContentProviderContract.DISTANCE, miles);
        activityValues.put(ActivitiesContentProviderContract.PACE, averagePace);
        activityValues.put(ActivitiesContentProviderContract.COMPLETE, true);
        Uri updateURI = ContentUris.withAppendedId(ActivitiesContentProviderContract.ACTIVITIES_URI, activityID);
        getContentResolver().update(updateURI, activityValues, null, null);

        // Unset the service running flag
        serviceRunning = false;

        super.onDestroy();
    }

    @Override
    public void onRebind(Intent intent)
    {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        return super.onUnbind(intent);
    }

}
