package uk.ac.nottingham.psyja2.activitytracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * BroadcastReceiver for the LowBattery system broadcast.
 * Will inform the ActivityTrackerService (if it is running) that there is low battery
 * Created by Josh on 27/12/2017.
 */
public class BatteryBroadcastReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent)
    {
        // Received a low battery warning
        // Tell the service, if it is running, that it has used too much battery power
        if(ActivityTrackerService.serviceRunning)
        {
            // Inform the activity tracker service that the warning was received
            Intent serviceIntent = new Intent(context, ActivityTrackerService.class);
            serviceIntent.putExtra("low_battery_warning", true);
            context.startService(serviceIntent);
        }
    }

}
