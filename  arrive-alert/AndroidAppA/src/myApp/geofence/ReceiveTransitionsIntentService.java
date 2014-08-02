package myApp.geofence;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import myApp.androidappa.Constants;
import myApp.androidappa.MainActivity;
import myApp.androidappa.R;
import myApp.database.DatabaseHandler;
import myApp.list.AlertListItem;

/**
 * This class receives geofence transition events from Location Services, in the
 * form of an Intent containing the transition type and geofence id(s) that triggered
 * the event.
 */
public class ReceiveTransitionsIntentService extends IntentService {

	private DatabaseHandler db;
    /**
     * Sets an identifier for this class' background thread
     */
    public ReceiveTransitionsIntentService() {
        super("ReceiveTransitionsIntentService");
    }

    /**
     * Handles incoming intents
     * @param intent The Intent sent by Location Services. This Intent is provided
     * to Location Services (inside a PendingIntent) when you call addGeofences()
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        // Create a local broadcast Intent
        Intent broadcastIntent = new Intent();

        // Give it the category for all intents sent by the Intent Service
        broadcastIntent.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES);

        // First check for errors
        if (LocationClient.hasError(intent)) {

            // Get the error code
            int errorCode = LocationClient.getErrorCode(intent);

            // Get the error message
            String errorMessage = LocationServiceErrorMessages.getErrorString(this, errorCode);

            // Log the error
            Log.e(GeofenceUtils.APPTAG,
                    getString(R.string.geofence_transition_error_detail, errorMessage)
            );

            // Set the action and error message for the broadcast intent
            broadcastIntent.setAction(GeofenceUtils.ACTION_GEOFENCE_ERROR)
                           .putExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, errorMessage);

            // Broadcast the error *locally* to other components in this app
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

        // If there's no error, get the transition type and create a notification
        // TODO -- handle transitions here
        } else {

            // Get the type of transition (entry or exit)
            int transition = LocationClient.getGeofenceTransition(intent);
            
            // get the triggered alert(s)
            db = new DatabaseHandler(this);
            ArrayList<AlertListItem> alerts = db.getAllAlerts();
            db.close();
            AlertListItem alert = null;
            
            
            // Test that a valid transition was reported
            if (
                    (transition == Geofence.GEOFENCE_TRANSITION_ENTER)
                    ||
                    (transition == Geofence.GEOFENCE_TRANSITION_EXIT)
               ) {

                // Post a notification
                List<Geofence> geofences = LocationClient.getTriggeringGeofences(intent);
                String[] geofenceIds = new String[geofences.size()];
                
                // 
                for (int index = 0; index < geofences.size() ; index++) {
                    geofenceIds[index] = geofences.get(index).getRequestId();
                    for(int i = 0; i < alerts.size(); i++) {
                    	if( geofenceIds[index].equals(String.valueOf(alerts.get(i).getLocation())) )   
                    		if(alerts.get(i).getWhenInt() == transition)
                    			alert = alerts.get(i);
                    }
                }
                
                
                String ids = TextUtils.join(GeofenceUtils.GEOFENCE_ID_DELIMITER,geofenceIds);
                String transitionType = getTransitionString(transition);

                // TODO handle the actual alert -- send message (email or text)
                switch(alert.getIcon()) {
                case Constants.EMAIL:
                	Intent emailIntent = new Intent(Intent.ACTION_SEND);
                	emailIntent.putExtra(Intent.EXTRA_EMAIL  , new String[]{alert.getContact()});
                	emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Arrive Alert - (Android App)");
                	emailIntent.putExtra(Intent.EXTRA_TEXT   , alert.getMessage());
                	
                	// TODO send email behind the scenes
                	
                	break;
                
                case Constants.TEXT:
                	sendSMS(alert.getContact(), "via ArriveAlert: " + alert.getMessage());
                	break;
                	
                	default:
                		// not supposed to happen
                }
                
                if(alert != null)
                	sendNotification(transitionType, ids, alert);

                // Log the transition type and a message
                Log.d(GeofenceUtils.APPTAG,
                        getString(
                                R.string.geofence_transition_notification_title,
                                transitionType,
                                ids));
                Log.d(GeofenceUtils.APPTAG,
                        getString(R.string.geofence_transition_notification_text));

            // An invalid transition was reported
            } else {
                // Always log as an error
                Log.e(GeofenceUtils.APPTAG,
                        getString(R.string.geofence_transition_invalid_type, transition));
            }
        }
    }

    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the main Activity.
     * @param transitionType The type of transition that occurred.
     *
     */
    private void sendNotification(String transitionType, String ids, AlertListItem alert) {

        // Create an explicit content Intent that starts the main Activity
        Intent notificationIntent =
                new Intent(getApplicationContext(),MainActivity.class);

        // Construct a task stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Adds the main Activity to the task stack as the parent
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        // Set the notification contents
//        builder.setSmallIcon(R.drawable.ic_notification)
//               .setContentTitle(
//                       getString(R.string.geofence_transition_notification_title,
//                               transitionType, ids))
//               .setContentText(getString(R.string.geofence_transition_notification_text))
//               .setContentIntent(notificationPendingIntent);
        String typeSent;
        if(alert.getIcon() == Constants.EMAIL)
        	typeSent = "Sent email to " + alert.getContact() + "\n";
        else
        	typeSent = "Sent text message to " + alert.getContact() + "\n";
        
        builder.setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(alert.getTitle())
        .setContentText(typeSent + alert.getMessage())
        .setContentIntent(notificationPendingIntent);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }

    /**
     * Maps geofence transition types to their human-readable equivalents.
     * @param transitionType A transition type constant defined in Geofence
     * @return A String indicating the type of transition
     */
    private String getTransitionString(int transitionType) {
        switch (transitionType) {

            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return getString(R.string.geofence_transition_entered);

            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return getString(R.string.geofence_transition_exited);

            default:
                return getString(R.string.geofence_transition_unknown);
        }
    }
    
 // ---sends an SMS message to another device---
    public void sendSMS(String phoneNumber, String message) {

        PendingIntent piSent = PendingIntent.getBroadcast(this, 0, new Intent(Constants.SENT), 0);
        PendingIntent piDelivered = PendingIntent.getBroadcast(this, 0,new Intent(Constants.DELIVERED), 0);
        SmsManager smsManager = SmsManager.getDefault();

        int length = message.length();          
        if(length > Constants.MAX_SMS_MESSAGE_LENGTH) {
            ArrayList<String> messagelist = smsManager.divideMessage(message);          
            smsManager.sendMultipartTextMessage(phoneNumber, null, messagelist, null, null);
        }
        else
            smsManager.sendTextMessage(phoneNumber, null, message, piSent, piDelivered);
      }
    
}

