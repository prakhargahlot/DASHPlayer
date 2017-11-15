package sg.edu.nus.cs5248.team09.dashplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Prakhar on 14-11-2017.
 *
 * To show Android Notifications. Doesn't work properly
 */

public class DashNotificationManager {
    private Context mContext;
    private int NOTIFICATION_ID = 1;
    private Notification mNotification;
    private NotificationManager mNotificationManager;
    private boolean mIsShowing;

    public DashNotificationManager(Context context)
    {
        mContext = context;
    }

    /**
     * Put the notification into the status bar
     */
    public void createNotification(CharSequence tickerText, CharSequence title) {
        //get the notification manager
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        //create the notification
        int icon = android.R.drawable.stat_sys_download;
        long when = System.currentTimeMillis();
        mNotification = new Notification(icon, tickerText, when);

        //create the content which is shown in the notification pulldown
        CharSequence contentText = "0% complete"; //Text of the notification in the pull down

        //you have to set a PendingIntent on a notification to tell the system what you want it to do when the notification is selected
        //I don't want to use this here so I'm just creating a blank one
        Intent notificationIntent = new Intent();
        PendingIntent.getActivity(mContext, 0, notificationIntent, 0);

        //make this notification appear in the 'Ongoing events' section
        mNotification.flags = Notification.FLAG_ONGOING_EVENT;

        //show the notification
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
        mIsShowing = true;
    }

    /**
     * Receives progress updates from the background task and updates the status bar notification appropriately
     * @param percentageComplete
     */
    public void progressUpdate(int percentageComplete) {
        //build up the new status message
        CharSequence contentText = percentageComplete + "% complete";
        //publish it to the status bar
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
    }

    /**
     * called when the background task is complete, this removes the notification from the status bar.
     * We could also use this to add a new ‘task complete’ notification
     */
    public void completed()    {
        //remove the notification from the status bar
        if(mIsShowing) {
            mNotificationManager.cancel(NOTIFICATION_ID);
        }
    }
}
