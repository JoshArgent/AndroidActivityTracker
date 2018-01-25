package uk.ac.nottingham.psyja2.activitytracker;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

/**
 * A ContentProvider that provides access to the activity logging data
 * Created by Josh on 15/12/2017.
 */
public class ActivitiesContentProvider extends ContentProvider {

    // Database object
    private DBHelper dbHelper = null;

    // URI matcher object
    private static final UriMatcher uriMatcher;
    static
    {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(ActivitiesContentProviderContract.AUTHORITY, "activities", 1); // Activities table
        uriMatcher.addURI(ActivitiesContentProviderContract.AUTHORITY, "activities/#", 2); // Activity by ID
        uriMatcher.addURI(ActivitiesContentProviderContract.AUTHORITY, "locations", 3); // Locations table
        uriMatcher.addURI(ActivitiesContentProviderContract.AUTHORITY, "locations/#", 4); // Location by ID
        uriMatcher.addURI(ActivitiesContentProviderContract.AUTHORITY, "locations/for_activity/#", 5); // All the locations for a given activity ID
        uriMatcher.addURI(ActivitiesContentProviderContract.AUTHORITY, "activities/distance_this_week", 6); // Calculate the distance this week
        uriMatcher.addURI(ActivitiesContentProviderContract.AUTHORITY, "activities/distance_this_month", 7); // Calculate the distance this month
        uriMatcher.addURI(ActivitiesContentProviderContract.AUTHORITY, "activities/distance_total", 8); // Calculate the total distance
        uriMatcher.addURI(ActivitiesContentProviderContract.AUTHORITY, "activities/average_pace/*", 9); // Average pace for a given activity type
        uriMatcher.addURI(ActivitiesContentProviderContract.AUTHORITY, "activities/best_pace/*", 10); // Best pace for a given activity type

    }

    @Override
    public boolean onCreate()
    {
        // Get the Activity Tracker database helper
        this.dbHelper = new DBHelper(this.getContext(), "activity_tracker", null, 1);
        return true;
    }

    @Override
    public String getType(Uri uri)
    {
        // Gets the type of content (single if getting item by ID, multiple otherwise)
        String contentType;
        if (uri.getLastPathSegment() == null)
        {
            contentType = ActivitiesContentProviderContract.CONTENT_TYPE_MULTIPLE;
        }
        else
        {
            contentType = ActivitiesContentProviderContract.CONTENT_TYPE_SINGLE;
        }
        return contentType;
    }


    @Override
    public Uri insert(Uri uri, ContentValues values)
    {
        // Get the SQLite database object
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Match the table name from the given URI
        String tableName;
        switch(uriMatcher.match(uri))
        {
            case 1:
                tableName = "activities";
                break;
            case 2:
                tableName = "activities";
                break;
            case 3:
                tableName = "locations";
                break;
            case 4:
                tableName = "locations";
                break;
            case 5:
                tableName = "locations";
                // Set the LOCATIONS_ACTIVITY_ID to the activity ID specified in the URI
                if(values.containsKey(ActivitiesContentProviderContract.LOCATIONS_ACTIVITY_ID))
                    values.remove(ActivitiesContentProviderContract.LOCATIONS_ACTIVITY_ID);
                values.put(ActivitiesContentProviderContract.LOCATIONS_ACTIVITY_ID, uri.getLastPathSegment());
                break;
            default:
                return null;
        }

        // Insert the values into the table
        long id = db.insert(tableName, null, values);
        db.close();

        // Notify listeners of change
        Uri nu = ContentUris.withAppendedId(uri, id);
        getContext().getContentResolver().notifyChange(nu, null);

        // Return the new record's URI
        return nu;
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
        // Get the SQLite database object
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Match the URI to the appropriate query
        switch(uriMatcher.match(uri))
        {
            case 5:
                // Get all the location records for a given activity ID
                return db.query("locations", projection, "activity_id = '" + uri.getLastPathSegment() + "' ORDER BY location_time ASC", selectionArgs, null, null, sortOrder);
            case 6:
                // Sum the distance covered in past 7 days
                long sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
                return db.rawQuery("SELECT SUM(activity_distance) FROM activities WHERE activity_complete = '1' AND activity_date > '" + sevenDaysAgo + "'", selectionArgs);
            case 7:
                // Sum the distance covered in past 30 days
                long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
                return db.rawQuery("SELECT SUM(activity_distance) FROM activities WHERE activity_complete = '1' AND activity_date > '" + thirtyDaysAgo + "'", selectionArgs);
            case 8:
                // Sum the distance covered in total
                return db.rawQuery("SELECT SUM(activity_distance) FROM activities WHERE activity_complete = '1'", selectionArgs);
            case 9:
                // Average the pace, for a particular type of activity (only count activities >0.1 miles)
                return db.rawQuery("SELECT AVG(activity_pace) FROM activities WHERE activity_complete = '1' AND activity_type = '" + uri.getLastPathSegment() + "' AND activity_distance > '0.1'", selectionArgs);
            case 10:
                // Find the MIN pace, for a particular type of activity (only count activities >0.1 miles)
                return db.rawQuery("SELECT MIN(activity_pace) FROM activities WHERE activity_complete = '1' AND activity_type = '" + uri.getLastPathSegment() + "' AND activity_distance > '0.1'", selectionArgs);
            case 2:
                // Query by activities ID
                selection = "_id = " + uri.getLastPathSegment();
                return db.query("activities", projection, selection, selectionArgs, null, null, sortOrder);
            case 1:
                // Normal query on the activities table
                return db.query("activities", projection, selection, selectionArgs, null, null, sortOrder);
            case 4:
                // Query by locations ID
                selection = "_id = " + uri.getLastPathSegment();
                return db.query("locations", projection, selection, selectionArgs, null, null, sortOrder);
            case 3:
                // Normal query on the locations table
                return db.query("locations", projection, selection, selectionArgs, null, null, sortOrder);
            default:
                return null;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
        // Get the SQLite database object
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Find what table/rows are to be updated from the URI
        String tableName;
        switch(uriMatcher.match(uri))
        {
            case 1:
                // Activities table based on selection query
                tableName = "activities";
                break;
            case 2:
                // A particular activity ID
                selection = "_id = " + uri.getLastPathSegment();
                tableName = "activities";
                break;
            case 3:
                // Locations table based on selection query
                tableName = "locations";
                break;
            case 4:
                // A particular Location ID
                selection = "_id = " + uri.getLastPathSegment();
                tableName = "locations";
                break;
            default:
                return -1;
        }

        // Execute the update operation on the database
        int result = db.update(tableName, values, selection, selectionArgs);
        db.close();

        // Notify of changes
        getContext().getContentResolver().notifyChange(uri, null);

        return result;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs)
    {
        // Get the SQLite database object
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Find what table/rows are to be deleted from the URI
        String tableName;
        switch(uriMatcher.match(uri))
        {
            case 1:
                // Activities table based on selection query
                tableName = "activities";
                break;
            case 2:
                // A particular activity ID
                selection = "_id = " + uri.getLastPathSegment();
                tableName = "activities";
                break;
            case 3:
                // Locations table based on selection query
                tableName = "locations";
                break;
            case 4:
                // A particular Location ID
                selection = "_id = " + uri.getLastPathSegment();
                tableName = "locations";
                break;
            default:
                return -1;
        }

        // Execute the delete operation on the database
        int result = db.delete(tableName, selection, selectionArgs);
        db.close();

        // Notify of changes
        getContext().getContentResolver().notifyChange(uri, null);

        return result;
    }
}
