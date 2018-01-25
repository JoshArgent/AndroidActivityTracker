package uk.ac.nottingham.psyja2.activitytracker;

import android.location.Location;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Contains static methods for various things
 * Created by Josh on 19/12/2017.
 */
public abstract class Utils {

    /*
    Format a number of seconds into an hours, minutes, seconds string
    eg. 01:34:12 or 33:12
     */
    public static String secondsToTimeString(int seconds)
    {
        // Work out how many hours, minutes and seconds there are
        int hours = seconds / 3600;
        seconds -= hours * 3600;
        int minutes = seconds / 60;
        seconds -= minutes * 60;

        if(hours > 0)
        {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        else
        {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    /*
    Format a unix timestamp (in milliseconds) into a date string
     */
    public static String timestampToDateString(long timestamp)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM yyyy");
        Date dateObj = new Date(timestamp);
        String dateStr = dateFormat.format(dateObj);
        return dateStr;
    }

    /*
    Converts a set of locations and times into a GPX xml file string
     */
    public static String convertToGPXFile(String name, List<Location> locations, List<Long> times)
    {
        // GPX Format information from: https://en.wikipedia.org/wiki/GPS_Exchange_Format
        // Define the GPX file header
        String gpx = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n";
        gpx += "<gpx version=\"1.0\">\n";
        gpx += "<name>" + name + "</name>\n";
        gpx += "<trk><name>" + name + "</name><number>1</number><trkseg>\n";

        // Loop through each coordinate and add it to the gpx file
        for(int n = 0; n < locations.size(); n++)
        {
            Location location = locations.get(n);
            long timestamp = times.get(n);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String time = dateFormat.format(new Date(timestamp));
            gpx += "<trkpt lat=\"" + location.getLatitude() + "\" lon=\"" + location.getLongitude() + "\"><ele>" + location.getAltitude() + "</ele><time>" + time + "</time></trkpt>\n";
        }

        // Close the GPX file
        gpx += "</trkseg></trk>\n";
        gpx += "</gpx>\n";
        return gpx;
    }

}
