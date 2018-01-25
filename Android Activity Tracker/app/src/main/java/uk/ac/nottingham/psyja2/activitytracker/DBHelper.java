package uk.ac.nottingham.psyja2.activitytracker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Josh on 15/12/2017.
 */
public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version)
    {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        // Create a relational database to hold the activity data:

        /*
        ACTIVITIES TABLE:
        _id = The unique activity ID
        activity_date = the unix timestamp of the date the activity occurred
        activity_type = the type of activity (running/walking/cycling/rowing)
        activity_time = the length of the activity in seconds
        activity_distance = the distance travelled in miles
        activity_pace = the calculate pace of the activity in miles/minute
        activity_complete = will be true once the activity has finished recording
         */
        db.execSQL("CREATE TABLE activities (_id INTEGER PRIMARY KEY AUTOINCREMENT, activity_date INTEGER, activity_type TEXT, activity_time INTEGER, activity_distance REAL, activity_pace REAL, activity_complete BOOLEAN DEFAULT 0);");

        /*
        LOCATIONS TABLE:
        _id = the unique ID of the location
        activity_id = the ID of the associated activity in the ACTIVITIES table
        location_latitude = the latitude of the location log
        location_longitude = the longitude of the location log
        location_altitude = the altitude of the location log
        location_time = the unix time of the location log
         */
        db.execSQL("CREATE TABLE locations (_id INTEGER PRIMARY KEY AUTOINCREMENT, activity_id INTEGER, location_latitude REAL, location_longitude REAL, location_altitude REAL, location_time INTEGER, FOREIGN KEY(activity_id) REFERENCES activities(_id) ON DELETE CASCADE);");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        // This should never get called, unless the app is reinstalled
        db.execSQL("DROP TABLE IF EXISTS activities");
        db.execSQL("DROP TABLE IF EXISTS locations");
        onCreate(db);
    }
}

