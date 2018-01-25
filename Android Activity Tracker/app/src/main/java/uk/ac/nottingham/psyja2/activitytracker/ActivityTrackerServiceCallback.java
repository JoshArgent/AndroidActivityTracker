package uk.ac.nottingham.psyja2.activitytracker;

/**
 * Created by Josh on 18/12/2017.
 */
public interface ActivityTrackerServiceCallback {

    /*
    Called when an activity tracker variable has changed (time/distance travelled/average pace)
     */
    void activityProgressUpdate(int time, float miles, float averagePace);

}
