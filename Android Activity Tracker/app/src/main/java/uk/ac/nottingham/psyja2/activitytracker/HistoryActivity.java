package uk.ac.nottingham.psyja2.activitytracker;

import android.content.ContentUris;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class HistoryActivity extends AppCompatActivity {

    // GUI components
    private ListView historyListView;

    // The observer for the ActivityContentProvider
    private ActivitiesContentProviderObserver contentProviderObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Set the title bar text
        this.setTitle("Activity History");

        // Get the UI components
        historyListView = (ListView) findViewById(R.id.historyListView);

        // When an item in the history list view is pressed, launch the ViewActivity for it
        historyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // Get the CursorAdapter from the ListView
                SimpleCursorAdapter cursorAdapter = (SimpleCursorAdapter) historyListView.getAdapter();

                // Get the ID of the item clicked, from the cursor object
                Cursor cursor = cursorAdapter.getCursor();
                cursor.moveToPosition(i);
                int activityID = cursor.getInt(cursor.getColumnIndex(ActivitiesContentProviderContract.ID));

                // Launch the ViewActivity activity and pass this ID via a bundle
                Intent viewActivityIntent = new Intent(HistoryActivity.this, ViewActivity.class);
                Bundle bundle = new Bundle();
                bundle.putInt("activity_ID", activityID);
                viewActivityIntent.putExtras(bundle);
                startActivity(viewActivityIntent);
            }
        });

        // When an item is pressed for a long time, show an option to delete it
        historyListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                SimpleCursorAdapter cursorAdapter = (SimpleCursorAdapter) historyListView.getAdapter();

                // Get the ID of the item clicked, from the cursor object
                Cursor cursor = cursorAdapter.getCursor();
                cursor.moveToPosition(i);
                final int activityID = cursor.getInt(cursor.getColumnIndex(ActivitiesContentProviderContract.ID));

                // Create a popup menu with a delete button
                PopupMenu popup = new PopupMenu(HistoryActivity.this, view);
                popup.getMenuInflater().inflate(R.menu.delete_menu, popup.getMenu());

                // When the only item in the popup is pressed (the 'delete' button), delete this activity record
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem)
                    {
                        // Delete the selected activity from the content provider
                        Uri activityURI = ContentUris.withAppendedId(ActivitiesContentProviderContract.ACTIVITIES_URI, activityID);
                        getContentResolver().delete(activityURI, null, null);

                        // Notify the user that the action is complete
                        Toast.makeText(HistoryActivity.this, "Activity log has been deleted!", Toast.LENGTH_SHORT).show();

                        // Return true to indicate the button press was handled
                        return true;
                    }
                });

                // Show the popup menu
                popup.show();

                // Return true to indicate the button press was handled
                return true;
            }
        });

        // Register a content provider observer to listen to changes to the data and then update the list
        contentProviderObserver = new ActivitiesContentProviderObserver(new Handler());
        getContentResolver().registerContentObserver(ActivitiesContentProviderContract.ACTIVITIES_URI, true, contentProviderObserver);
    }

    @Override
    protected void onResume()
    {
        // Populate the list view with records from the ACTIVITIES table
        populateListView();

        super.onResume();
    }

    @Override
    protected void onDestroy()
    {
        // Unregister the content provider observer
        getContentResolver().unregisterContentObserver(contentProviderObserver);

        super.onDestroy();
    }

    /*
    The ContentObserver to re-populate the ListView whenever any data changes
     */
    class ActivitiesContentProviderObserver extends ContentObserver
    {
        public ActivitiesContentProviderObserver(Handler handler)
        {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange)
        {
            this.onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri)
        {
            // Update the list view as there has been a change with the data
            populateListView();
        }
    }

    /*
    Re-populate the ListView with the records in the ACTIVITIES table
     */
    private void populateListView()
    {
        // Setup the projection for the query
        String[] projection = new String[] {
                ActivitiesContentProviderContract.ID,
                ActivitiesContentProviderContract.DATE,
                ActivitiesContentProviderContract.DISTANCE,
                ActivitiesContentProviderContract.TIME,
                ActivitiesContentProviderContract.TYPE
        };

        // Only select records which are marked as complete = true
        String selection = ActivitiesContentProviderContract.COMPLETE + " = ?";
        String[] selectionArgs = new String[] { "1" };

        // Perform the query on the ACTIVITIES table
        Uri queryUri = ActivitiesContentProviderContract.ACTIVITIES_URI;

        // Execute the query to the content provider and get the cursor object
        // Sort the results by newest first
        Cursor cursor = getContentResolver().query(queryUri, projection, selection, selectionArgs, ActivitiesContentProviderContract.DATE + " DESC");

        // Filter the columns that need to be shown in the list
        String colsToDisplay [] = new String[] {
                ActivitiesContentProviderContract.DATE,
                ActivitiesContentProviderContract.DISTANCE,
                ActivitiesContentProviderContract.TIME,
                ActivitiesContentProviderContract.TYPE,
                ActivitiesContentProviderContract.TYPE
        };

        // Associated IDs for the ListView menu item
        int[] colResIds = new int[]{
                R.id.title1,
                R.id.subtitle1,
                R.id.subtitle2,
                R.id.title2,
                R.id.icon
        };

        // I do not want to display the raw values from the database, they need to be nicely formatted.
        // This overridden adapter will handle this.
        // The code for modifying the values used in a SimpleCursorAdapter was modified from StackOverflow:
        // https://stackoverflow.com/questions/3609126/changing-values-from-cursor-using-simplecursoradapter ~ Manas
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.activity_tracker_list_item, cursor, colsToDisplay, colResIds)
        {
            @Override
            public void setViewText(TextView v, String text)
            {
                super.setViewText(v, formatListItemTextFields(v, text));
            }

            @Override
            public void setViewImage(ImageView v, String value)
            {
                // Replace the activity type with the associated icon resource
                // Icons are from https://icons8.com/icon/pack/sports/color
                // Free for personal/commercial use so long as link to website is included
                switch(value)
                {
                    case "Running":
                        v.setImageResource(R.mipmap.running_icon);
                        break;
                    case "Walking":
                        v.setImageResource(R.mipmap.walking_icon);
                        break;
                    case "Cycling":
                        v.setImageResource(R.mipmap.cycling_icon);
                        break;
                    case "Rowing":
                        v.setImageResource(R.mipmap.rowing_icon);
                        break;
                    default:
                        super.setViewImage(v, value);
                }
            }
        };

        // Set the adapter of the ListView
        historyListView.setAdapter(adapter);
    }

    /*
    This function formats the raw text values into nicer forms
    eg. "1.2344566" => "1.23 miles"
     */
    private String formatListItemTextFields(TextView v, String text)
    {
        switch (v.getId())
        {
            case R.id.title1:
                return Utils.timestampToDateString(Long.valueOf(text)) + ": ";
            case R.id.subtitle1:
                return String.format("%.2f miles, ", Float.valueOf(text));
            case R.id.subtitle2:
                return Utils.secondsToTimeString(Integer.valueOf(text)) + " minutes";
        }
        return text;
    }


}
