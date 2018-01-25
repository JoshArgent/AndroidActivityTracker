package uk.ac.nottingham.psyja2.activitytracker;

import android.net.Uri;

/**
 * Created by Josh on 15/12/2017.
 */
public class ActivitiesContentProviderContract {

    public static final String AUTHORITY = "uk.ac.nottingham.psyja2.ActivitiesContentProvider";

    // ACTIVITIES table URI
    public static final Uri ACTIVITIES_URI = Uri.parse("content://"+AUTHORITY+"/activities");

    // ACTIVITIES table fields
    public static final String ID = "_id";
    public static final String DATE = "activity_date";
    public static final String TYPE = "activity_type";
    public static final String TIME = "activity_time";
    public static final String DISTANCE = "activity_distance";
    public static final String PACE = "activity_pace";
    public static final String COMPLETE = "activity_complete";

    // ACTIVITY types
    public static final String ACTIVITY_TYPE_RUNNING = "Running";
    public static final String ACTIVITY_TYPE_WALKING = "Walking";
    public static final String ACTIVITY_TYPE_CYCLING = "Cycling";
    public static final String ACTIVITY_TYPE_ROWING = "Rowing";

    // ACTIVITY statistics
    public static final Uri ACTIVITY_DISTANCE_THIS_WEEK_URI = Uri.parse("content://"+AUTHORITY+"/activities/distance_this_week");
    public static final Uri ACTIVITY_DISTANCE_THIS_MONTH_URI = Uri.parse("content://"+AUTHORITY+"/activities/distance_this_month");
    public static final Uri ACTIVITY_DISTANCE_TOTAL_URI = Uri.parse("content://"+AUTHORITY+"/activities/distance_total");
    public static final Uri ACTIVITY_AVERAGE_PACE_URI = Uri.parse("content://"+AUTHORITY+"/activities/average_pace");
    public static final Uri ACTIVITY_BEST_PACE_URI = Uri.parse("content://"+AUTHORITY+"/activities/best_pace");

    // LOCATIONS table URI
    public static final Uri LOCATIONS_URI = Uri.parse("content://"+AUTHORITY+"/locations");

    // LOCATIONS_FOR_ACTIVITY table URI (All the location records for a given activity)
    public static final Uri LOCATIONS_FOR_ACTIVITY_URI = Uri.parse("content://"+AUTHORITY+"/locations/for_activity");

    // LOCATIONS table fields
    public static final String LOCATIONS_ACTIVITY_ID = "activity_id";
    public static final String LATITUDE = "location_latitude";
    public static final String LONGITUDE = "location_longitude";
    public static final String ALTITUDE = "location_altitude";
    public static final String LOCATION_TIME = "location_time";

    public static final String CONTENT_TYPE_SINGLE = "vnd.android.cursor.item/ActivitiesContentProvider.data.text";
    public static final String CONTENT_TYPE_MULTIPLE = "vnd.android.cursor.dir/ActivitiesContentProvider.data.text";

}
