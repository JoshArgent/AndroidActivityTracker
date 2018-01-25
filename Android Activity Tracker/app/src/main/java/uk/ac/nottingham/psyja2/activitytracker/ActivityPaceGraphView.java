package uk.ac.nottingham.psyja2.activitytracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.location.Location;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * An Android component to display a graph of distance travelled vs. the pace (minutes per mile)
 * Created by Josh on 27/12/2017.
 */
public class ActivityPaceGraphView extends View {

    private float xAxis[];
    private float yAxis[];
    private float highestPace = 0;
    private float totalDistance = 0;
    private static final float METRES_PER_MILE = 0.000621371f;

    public ActivityPaceGraphView(Context context)
    {
        super(context);
    }

    public ActivityPaceGraphView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public ActivityPaceGraphView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    public ActivityPaceGraphView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /*
    Load the recorded activity data to display on the graph
     */
    public void loadData(List<Location> locations, List<Long> times)
    {
        // If there is not enough data; do not load anything
        if(times.size() <= 2 || locations.size() <= 2)
            return;

        // Work out the pace for each item in the location list and build a list of distances
        List<Float> paces = new ArrayList<Float>();
        List<Float> distances = new ArrayList<Float>();
        totalDistance = 0;

        for(int n = 1; n < locations.size(); n++)
        {
            // Work out the distance and pace values
            float distance = locations.get(n - 1).distanceTo(locations.get(n)) * METRES_PER_MILE;
            long time = (times.get(n) - times.get(n - 1)) / 1000L;
            float pace = ((float)time) / distance;
            totalDistance += distance;
            distances.add(totalDistance);
            paces.add(pace);
        }

        // Decide how many segments to split the data into...
        // Either 0.05mile parts OR 10 individual parts, whichever is bigger
        int segments = (int) Math.max(10, Math.ceil(totalDistance / 0.05f));
        float distanceIncrement = totalDistance / (float)segments;
        xAxis = new float[segments];
        yAxis = new float[segments];

        // Interpolate the pace values for each segment
        highestPace = 0;
        for(int i = 0; i < segments; i++)
        {
            // Calculate the distance for this segment
            xAxis[i] = i * distanceIncrement;

            // Calculate the average pace for this segment
            float paceSum = 0;
            int paceNum = 0;
            for(int j = 0; j < distances.size(); j++)
            {
                // Get the pace values from the current distance values to the i+1 distance value
                if(distances.get(j) > xAxis[i] && distances.get(j) <= (i+1) * distanceIncrement)
                {
                    paceSum += paces.get(j);
                    paceNum++;
                }
            }
            yAxis[i] = paceSum / paceNum; // Calculate the mean

            // See if this is the highest pace so far
            highestPace = highestPace > yAxis[i] ? highestPace : yAxis[i];
        }
    }

    @Override
    public void draw(Canvas canvas)
    {
        // Only draw the graph if data have been set
        if(xAxis != null && yAxis != null)
        {
            // the graph origin values define the offset from the edges of the View
            final int GRAPH_X_ORIGIN = 100;
            final int GRAPH_Y_ORIGIN = 40;
            final int graphWidth = canvas.getWidth() - GRAPH_X_ORIGIN;
            final int graphHeight = canvas.getHeight() - GRAPH_Y_ORIGIN;

            // Calculate the xFactor and yFactor - use these to calculate the x/y values from pace/distance values
            final float yFactor = graphHeight / highestPace;
            final float xFactor = graphWidth / totalDistance;

            // Define the graph line and fill paints
            Paint paint = new Paint();
            paint.setColor(Color.parseColor("#0e5faf"));
            paint.setStrokeWidth(6);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);

            Paint fillPaint = new Paint();
            fillPaint.setColor(Color.parseColor("#3e8bd8"));
            paint.setStyle(Paint.Style.FILL);

            // Draw the graph line and fill the area underneath
            float prevX = GRAPH_X_ORIGIN;
            float prevY = graphHeight - yAxis[0] * yFactor;
            for(int i = 1; i < xAxis.length; i++)
            {
                // Calculate the x and y values for this data point
                float x = xAxis[i] * xFactor + GRAPH_X_ORIGIN;
                float y = graphHeight - yAxis[i] * yFactor;

                // Create the graph fill object
                Path sectionFill = new Path();
                sectionFill.moveTo(prevX, prevY);
                sectionFill.lineTo(x, y);
                sectionFill.lineTo(x, graphHeight);
                sectionFill.lineTo(prevX, graphHeight);
                sectionFill.close();

                // Draw and fill the graph
                canvas.drawPath(sectionFill, fillPaint);
                canvas.drawLine(prevX, prevY, x, y, paint);

                prevX = x;
                prevY = y;
            }

            // Decide what X-Axis scale marker spacing to use
            float xAxisMarkerSpacing;
            for(xAxisMarkerSpacing = 0.05f; totalDistance / xAxisMarkerSpacing > 10; xAxisMarkerSpacing *= 2);
            xAxisMarkerSpacing *= xFactor;

            // Decide what Y-Axis scale marker spacing to use
            float yAxisMarkerSpacing;
            for(yAxisMarkerSpacing = 1f; highestPace / yAxisMarkerSpacing > 10; )
            {
                if(yAxisMarkerSpacing == 1)
                    yAxisMarkerSpacing = 10;
                else if(yAxisMarkerSpacing == 10)
                    yAxisMarkerSpacing = 30;
                else if(yAxisMarkerSpacing == 30)
                    yAxisMarkerSpacing = 60;
                else
                    yAxisMarkerSpacing *= 2;
            }
            float yAxisMarkerSpacingValue = yAxisMarkerSpacing;
            yAxisMarkerSpacing *= yFactor;

            // Draw the graph axis
            paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setStrokeWidth(4);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(GRAPH_X_ORIGIN, 0, GRAPH_X_ORIGIN, graphHeight, paint);
            canvas.drawLine(GRAPH_X_ORIGIN, graphHeight, graphWidth + GRAPH_X_ORIGIN, graphHeight, paint);

            // Define the scale text paint
            Paint textPaint = new Paint();
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(11 * getResources().getDisplayMetrics().density);

            // Define the paint for the y-axis horizontal lines
            Paint horizontalLinePaint = new Paint();
            horizontalLinePaint.setColor(Color.parseColor("#80808080")); // 50% opacity gray line
            horizontalLinePaint.setStrokeWidth(4);
            horizontalLinePaint.setStyle(Paint.Style.STROKE);

            // Draw the graph x scale
            for(float x = GRAPH_X_ORIGIN; x < graphWidth; x += xAxisMarkerSpacing)
            {
                canvas.drawLine(x, graphHeight, x, graphHeight + 10, paint);
                String value = String.format("%.2f", (x - GRAPH_X_ORIGIN) / xFactor);
                canvas.drawText(value, x - textPaint.measureText(value) / 2, graphHeight + GRAPH_Y_ORIGIN, textPaint);
            }

            // Draw the graph y scale and horizontal lines
            float yAxisValue = 0;
            for(float y = 0; y < graphHeight; y += yAxisMarkerSpacing)
            {
                // Only draw the marker if it is not cut off the screen
                if(graphHeight - (y + textPaint.getTextSize()) > 0)
                {
                    canvas.drawLine(GRAPH_X_ORIGIN, graphHeight - y, GRAPH_X_ORIGIN - 10, graphHeight - y, paint);
                    canvas.drawLine(GRAPH_X_ORIGIN, graphHeight - y, GRAPH_X_ORIGIN + graphWidth, graphHeight - y, horizontalLinePaint);
                    String value = Utils.secondsToTimeString((int) yAxisValue);
                    canvas.drawText(value, 0, graphHeight - y, textPaint);
                }
                yAxisValue += yAxisMarkerSpacingValue;
            }
        }

        super.draw(canvas);
    }


}
