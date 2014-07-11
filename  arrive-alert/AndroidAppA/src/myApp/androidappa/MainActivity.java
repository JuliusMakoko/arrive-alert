/*
 * Author - Ben Wegher
 * Date   - 6/29/2014
 * Class  - MainActivity.java
 * Description - As the name suggests this is the 'Main'/Home activity.
 * 				 Its primary function is to display the list of the user's
 * 				 current alerts.
 */

package myApp.androidappa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import myApp.database.DatabaseHandler;
import myApp.list.AlertListAdapter;
import myApp.list.AlertListItem;
import myApp.list.LocationListItem;
import myApp.location.EditLocationActivity;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import com.bugsense.trace.BugSenseHandler;
import myApp.geofence.GeofenceRemover;
import myApp.geofence.GeofenceRequester;
import myApp.geofence.GeofenceUtils;
import myApp.geofence.SimpleGeofence;
import myApp.geofence.SimpleGeofenceStore;
import myApp.geofence.GeofenceUtils.REMOVE_TYPE;
import myApp.geofence.GeofenceUtils.REQUEST_TYPE;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;

public class MainActivity extends ListActivity {

	// Global variables
	private ArrayList<AlertListItem> alertListItems;
	private AlertListAdapter mAdapter;
	private DatabaseHandler db;
	private TextView noAlertsHeader;

	// Geofence variables

	/*
	 * Use to set an expiration time for a geofence. After this amount of time
	 * Location Services will stop tracking the geofence. Remember to unregister
	 * a geofence when you're finished with it. Otherwise, your app will use up
	 * battery. To continue monitoring a geofence indefinitely, set the
	 * expiration time to Geofence#NEVER_EXPIRE.
	 */
	private static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;
	private static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS = GEOFENCE_EXPIRATION_IN_HOURS
			* DateUtils.HOUR_IN_MILLIS;

	// Store the current request
	private myApp.geofence.GeofenceUtils.REQUEST_TYPE mRequestType;

	// Store the current type of removal
	private myApp.geofence.GeofenceUtils.REMOVE_TYPE mRemoveType;

	// Persistent storage for geofences
	private SimpleGeofenceStore mPrefs;

	// Store a list of geofences to add
	List<Geofence> mCurrentGeofences;

	// Add geofences handler
	private GeofenceRequester mGeofenceRequester;
	// Remove geofences handler
	private GeofenceRemover mGeofenceRemover;

	/*
	 * An instance of an inner class that receives broadcasts from listeners and
	 * from the IntentService that receives geofence transition events
	 */
	private GeofenceSampleReceiver mBroadcastReceiver;

	// An intent filter for the broadcast receiver
	private IntentFilter mIntentFilter;

	// Store the list of geofences to remove
	private List<String> mGeofenceIdsToRemove;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		BugSenseHandler.initAndStartSession(MainActivity.this, "b52296ad");
		setContentView(R.layout.activity_main);

		// get reference to no alerts header
		noAlertsHeader = (TextView) findViewById(R.id.textViewNoAlerts);
		noAlertsHeader.setText("You haven't created any alerts yet!");
		noAlertsHeader.setVisibility(View.VISIBLE);
		
		db = new DatabaseHandler(this);

		// create an ArrayList of AlertListItems
		alertListItems = new ArrayList<AlertListItem>();

		// load saved alerts from local storage into alertListItems
		alertListItems = db.getAllAlerts();
		if(alertListItems.size() != 0)
			noAlertsHeader.setVisibility(View.GONE);

		// set the list adapter
		mAdapter = new AlertListAdapter(this, alertListItems);
		setListAdapter(mAdapter);
		db.close();

		android.app.ActionBar action = getActionBar();
		action.show();

		registerForContextMenu(getListView());

		// geofence variables

		// Create a new broadcast receiver to receive updates from the listeners
		// and service
		mBroadcastReceiver = new GeofenceSampleReceiver();

		// Create an intent filter for the broadcast receiver
		mIntentFilter = new IntentFilter();

		// Action for broadcast Intents that report successful addition of
		// geofences
		mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_ADDED);

		// Action for broadcast Intents that report successful removal of
		// geofences
		mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_REMOVED);

		// Action for broadcast Intents containing various types of geofencing
		// errors
		mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_ERROR);

		// All Location Services sample apps use this category
		mIntentFilter.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES);

		// Instantiate a new geofence storage area
		mPrefs = new SimpleGeofenceStore(this);

		// Instantiate the current List of geofences
		mCurrentGeofences = new ArrayList<Geofence>();

		// Instantiate a Geofence requester
		mGeofenceRequester = new GeofenceRequester(this);

		// Instantiate a Geofence remover
		mGeofenceRemover = new GeofenceRemover(this);

	}

	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();

		switch (item.getItemId()) {
		case R.id.delete:
			AlertListItem deleteMe = (AlertListItem) mAdapter
					.getItem((int) info.id);

			// ********** TODO Confirmation or Undo feature
			// TODO check if we need to remove geofence
			if(deleteMe.getActive())
				unregisterGeofence(deleteMe);
			db.deleteAlert(deleteMe); // remove alert from database (don't have
										// to do this)
			mAdapter.delete(deleteMe); // remove from adapter

			mAdapter.notifyDataSetChanged();
			
			// if the last alert was just deleted..
			if(mAdapter.getCount() == 0)
				noAlertsHeader.setVisibility(View.VISIBLE);

			Toast.makeText(MainActivity.this,
					deleteMe.getTitle() + " was deleted.", Toast.LENGTH_LONG)
					.show();
			return true;
		case R.id.edit:
			// Open appropriate activity and fill with data
			AlertListItem editMe = (AlertListItem) mAdapter
					.getItem((int) info.id);

			Intent editIntent;
			switch (editMe.getIcon()) {
			// if the item to be edited is an Email alert...
			case (Constants.EMAIL):
				editIntent = new Intent(this, AddNewEmailAlert.class);
				break;
			// if the item to be edited is a Text alert...
			case (Constants.TEXT):
				editIntent = new Intent(this, AddNewTextAlert.class);
				break;
			default:
				Toast.makeText(getApplicationContext(),
						"Item could not be edited at this time",
						Toast.LENGTH_SHORT).show();
				return false;
			}

			editIntent.putExtra("TITLE", editMe.getTitle());
			editIntent.putExtra("CONTACT", editMe.getContact());
			editIntent.putExtra("LOCATION", editMe.getLocation());
			editIntent.putExtra("MESSAGE", editMe.getMessage());
			editIntent.putExtra("WHEN", editMe.getWhen());

			startActivityForResult(editIntent, Constants.UPDATE);
			return true;
		case R.id.activate:
			// this is where we activate or deactivate the selected alert
			AlertListItem activateMe = (AlertListItem) mAdapter
					.getItem((int) info.id);
			if (activateMe.getActive()) {
				// TODO remove geofence if this is the last alert using corresponding geofence
				
				// right now just removing associated geofence by default
				unregisterGeofence(activateMe);

				activateMe.setActive(false);
				Toast.makeText(getApplicationContext(), "Alert Turned Off!",
						Toast.LENGTH_SHORT).show();
			} else {
				// check to see if geofence for this location is already active
				boolean active = false;
				String alertInUse = "";
				int geofenceID = -1;
				for (Geofence g : mCurrentGeofences) {
					geofenceID = Integer.parseInt(g.getRequestId());
					if (geofenceID == activateMe.getLocation()) {
						active = true;
					}
				}
				// TODO
				// if no geofence exists -- create one
				if (active == false) {
					registerGeofence(activateMe);
					// if not then create a new geofence corresponding to alert
					// location
					activateMe.setActive(true);
					Toast.makeText(getApplicationContext(), "Alert Turned On!",
							Toast.LENGTH_SHORT).show();
				} else {
					for(AlertListItem a : alertListItems)
						if(a.getLocation() == geofenceID && !a.equals(activateMe))
							alertInUse = a.getTitle();
					Toast.makeText(getApplicationContext(), "Sorry, that location is already" +
							" being used to monitor '" + alertInUse + "'. Either deactivate it or" +
							" create a new location.",
							Toast.LENGTH_LONG).show();
				}

				
			}

			db.updateAlert(activateMe);
			mAdapter.notifyDataSetChanged();
			db.close();

			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		// if active then show off option, else show on
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		AlertListItem selected = (AlertListItem) mAdapter
				.getItem(info.position);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.context_menu, menu);

		// if alert is active -- change menu item to 'Turn Off'
		if (selected.getActive()) {
			MenuItem activate = menu.getItem(2);
			activate.setTitle("Turn Off");
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			launchSettings();
			return true;
		} else if (id == R.id.action_locations) {
			launchLocations();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	// triggered as onClick() event of 'Add New Alert' button
	public void addNewAlert(View v) {
		RadioButton rb;
		rb = (RadioButton) findViewById(R.id.radio0);

		// if radio0 is filled then launch new text alert activity
		if (rb.isChecked()) {
			Intent intentAddNewAlert = new Intent(this, AddNewTextAlert.class);
			startActivityForResult(intentAddNewAlert, Constants.TEXT);
		}
		// else radio1 is filled then launch new email alert activity
		else {
			Intent intentAddNewAlert = new Intent(this, AddNewEmailAlert.class);
			startActivityForResult(intentAddNewAlert, Constants.EMAIL);
		}

	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		// Choose what to do based on the request code
		switch (requestCode) {

		// If the request code matches the code sent in onConnectionFailed
		case GeofenceUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST:

			switch (resultCode) {
			// If Google Play services resolved the problem
			case Activity.RESULT_OK:

				// If the request was to add geofences
				if (GeofenceUtils.REQUEST_TYPE.ADD == mRequestType) {

					// Toggle the request flag and send a new request
					mGeofenceRequester.setInProgressFlag(false);

					// Restart the process of adding the current geofences
					mGeofenceRequester.addGeofences(mCurrentGeofences);

					// If the request was to remove geofences
				} else if (GeofenceUtils.REQUEST_TYPE.REMOVE == mRequestType) {

					// Toggle the removal flag and send a new removal request
					mGeofenceRemover.setInProgressFlag(false);

					// If the removal was by Intent
					if (GeofenceUtils.REMOVE_TYPE.INTENT == mRemoveType) {

						// Restart the removal of all geofences for the
						// PendingIntent
						mGeofenceRemover
								.removeGeofencesByIntent(mGeofenceRequester
										.getRequestPendingIntent());

						// If the removal was by a List of geofence IDs
					} else {

						// Restart the removal of the geofence list
						mGeofenceRemover
								.removeGeofencesById(mGeofenceIdsToRemove);
					}
				}
				break;

			// If any other result was returned by Google Play services
			default:

				// Report that Google Play services was unable to resolve the
				// problem.
				Log.d(GeofenceUtils.APPTAG, getString(R.string.no_resolution));
			}

		case Constants.EMAIL:
		case Constants.TEXT:
			if (resultCode == RESULT_OK) {
				String title = data.getStringExtra("TITLE");
				String contact = data.getStringExtra("CONTACT");
				int icon = data.getIntExtra("ICON", 0);
				String loc = data.getStringExtra("LOCATION");
				String message = data.getStringExtra("MESSAGE");
				String when = data.getStringExtra("WHEN");

				// get location id from 'locations' table
				int location = db.getLocation(loc).getLocationId();

				// needs a title, icon, contact, message to preview
				AlertListItem addMe = new AlertListItem(title, contact,
						location, message, when, icon);

				db.addAlert(addMe); // add the alert to our database
				mAdapter.add(addMe); // add the alert to the list adapter
				mAdapter.notifyDataSetChanged(); // notify the list adapter
				
				// if the first alert was just added..
				if(mAdapter.getCount() != 0)
					noAlertsHeader.setVisibility(View.GONE);
			}
			break;
		case Constants.UPDATE:
			// restart activity so the updated alert appears
			finish();
			startActivity(getIntent());
			break;
		// If any other request code was received
		default:
			// Report that this Activity received an unknown requestCode
			Log.d(GeofenceUtils.APPTAG,
					getString(R.string.unknown_activity_request_code,
							requestCode));

			break;
		}

		// if (resultCode == RESULT_OK) {
		// //
		// if (requestCode == Constants.EMAIL || requestCode == Constants.TEXT)
		// {
		// String title = data.getStringExtra("TITLE");
		// String contact = data.getStringExtra("CONTACT");
		// int icon = data.getIntExtra("ICON", 0);
		// String loc = data.getStringExtra("LOCATION");
		// String message = data.getStringExtra("MESSAGE");
		// String when = data.getStringExtra("WHEN");
		//
		// // get location id from 'locations' table
		// int location = db.getLocation(loc).getLocationId();
		//
		// // needs a title, icon, contact, message to preview
		// AlertListItem addMe = new AlertListItem(title, contact,
		// location, message, when, icon);
		//
		// db.addAlert(addMe); // add the alert to our database
		// mAdapter.add(addMe); // add the alert to the list adapter
		// mAdapter.notifyDataSetChanged(); // notify the list adapter
		// } else if (requestCode == Constants.UPDATE) {
		// // restart activity so the updated alert appears
		// finish();
		// startActivity(getIntent());
		// }
		//
		// // result was not OK
		// } else {
		//
		// Log.w("Warning", "Warning: activity result not ok");
		// }

	}

	public void launchSettings() {
		Intent intentLaunchSettings = new Intent(this, SettingsActivity.class);

		startActivity(intentLaunchSettings);
	}

	public void launchLocations() {
		Intent intentEditLocations = new Intent(this,
				EditLocationActivity.class);
		startActivity(intentEditLocations);
	}

	/**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {

            // In debug mode, log the status
            Log.d(GeofenceUtils.APPTAG, getString(R.string.play_services_available));

            // Continue
            return true;

        // Google Play services was not available for some reason
        } else {

            // Display an error dialog
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
            if (dialog != null) {
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(dialog);
                //errorFragment.show(getFragmentManager(), GeofenceUtils.APPTAG);
            }
            return false;
        }
    }
    
    /**
     * Called when the user clicks the "Register geofences" button.
     * Get the geofence parameters for each geofence and add them to
     * a List. Create the PendingIntent containing an Intent that
     * Location Services sends to this app's broadcast receiver when
     * Location Services detects a geofence transition. Send the List
     * and the PendingIntent to Location Services.
     */
    public void registerGeofence(AlertListItem alert) {

        /*
         * Record the request as an ADD. If a connection error occurs,
         * the app can automatically restart the add request if Google Play services
         * can fix the error
         */
        mRequestType = GeofenceUtils.REQUEST_TYPE.ADD;

        /*
         * Check for Google Play services. Do this after
         * setting the request type. If connecting to Google Play services
         * fails, onActivityResult is eventually called, and it needs to
         * know what type of request was in progress.
         */
        if (!servicesConnected()) {

            return;
        }

        /*
         * Create a version of geofence that is "flattened" into individual fields. This
         * allows it to be stored in SharedPreferences.
         */
        LocationListItem alertLocation = db.getLocation(alert.getLocation());
        
        if(alertLocation != null) {
        SimpleGeofence newGeofence = new SimpleGeofence(
            String.valueOf(alert.getLocation()),
            // latitude, longitude, and radius from the location
            alertLocation.getLatitude(),
            alertLocation.getLongitude(),
            alertLocation.getRadius(),
            // Set the expiration time
            GEOFENCE_EXPIRATION_IN_MILLISECONDS,
            // Only detect the specified transitions
            alert.getWhenInt());

        // Store this flat version in SharedPreferences
        mPrefs.setGeofence(newGeofence.getId(), newGeofence);

        /*
         * Create a version of geofence 2 that is "flattened" into individual fields. This
         * allows it to be stored in SharedPreferences.
         */
//        mUIGeofence2 = new SimpleGeofence(
//            "2",
//            // Get latitude, longitude, and radius from the UI
//            Double.valueOf(mLatitude2.getText().toString()),
//            Double.valueOf(mLongitude2.getText().toString()),
//            Float.valueOf(mRadius2.getText().toString()),
//            // Set the expiration time
//            GEOFENCE_EXPIRATION_IN_MILLISECONDS,
//            // Detect both entry and exit transitions
//            Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT
//            );

        /*
         * Add Geofence objects to a List. toGeofence()
         * creates a Location Services Geofence object from a
         * flat object
         */
        mCurrentGeofences.add(newGeofence.toGeofence());

        // Start the request. Fail if there's already a request in progress
        try {
        	Toast.makeText(this, "Adding geofence for location '" + alertLocation.getName() + "'",
					Toast.LENGTH_LONG).show();
            // Try to add geofences
            mGeofenceRequester.addGeofences(mCurrentGeofences);
        } catch (UnsupportedOperationException e) {
            // Notify user that previous request hasn't finished.
            Toast.makeText(this, R.string.add_geofences_already_requested_error,
                        Toast.LENGTH_LONG).show();
        }
        } else {
        	Toast.makeText(this, "Could not find location with id#" + alert.getLocation(),
					Toast.LENGTH_LONG).show();
        }
        
    }
    
    /**
     * Called when the user clicks the "Remove geofence 1" button
     * @param view The view that triggered this callback
     */
    public void unregisterGeofence(AlertListItem alert) {
        /*
         * Remove the geofence by creating a List of geofences to
         * remove and sending it to Location Services. The List
         * contains the id of geofence to be removed {location}.
         * The removal happens asynchronously; Location Services calls
         * onRemoveGeofencesByPendingIntentResult() (implemented in
         * the current Activity) when the removal is done.
         */

        // Create a List of 1 Geofence with the ID {location} and store it in the global list
        mGeofenceIdsToRemove = Collections.singletonList(String.valueOf(alert.getLocation()));

        /*
         * Record the removal as remove by list. If a connection error occurs,
         * the app can automatically restart the removal if Google Play services
         * can fix the error
         */
        mRemoveType = GeofenceUtils.REMOVE_TYPE.LIST;

        /*
         * Check for Google Play services. Do this after
         * setting the request type. If connecting to Google Play services
         * fails, onActivityResult is eventually called, and it needs to
         * know what type of request was in progress.
         */
        if (!servicesConnected()) {

            return;
        }

        // Try to remove the geofence
        try {
            mGeofenceRemover.removeGeofencesById(mGeofenceIdsToRemove);

        // Catch errors with the provided geofence IDs
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (UnsupportedOperationException e) {
            // Notify user that previous request hasn't finished.
            Toast.makeText(this, R.string.remove_geofences_already_requested_error,
                        Toast.LENGTH_LONG).show();
        }
    }
	
	/**
	 * Define a Broadcast receiver that receives updates from connection
	 * listeners and the geofence transition service.
	 */
	public class GeofenceSampleReceiver extends BroadcastReceiver {
		/*
		 * Define the required method for broadcast receivers This method is
		 * invoked when a broadcast Intent triggers the receiver
		 */
		@Override
		public void onReceive(Context context, Intent intent) {

			// Check the action code and determine what to do
			String action = intent.getAction();

			// Intent contains information about errors in adding or removing
			// geofences
			if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_ERROR)) {

				handleGeofenceError(context, intent);

				// Intent contains information about successful addition or
				// removal of geofences
			} else if (TextUtils.equals(action,
					GeofenceUtils.ACTION_GEOFENCES_ADDED)
					|| TextUtils.equals(action,
							GeofenceUtils.ACTION_GEOFENCES_REMOVED)) {

				handleGeofenceStatus(context, intent);

				// Intent contains information about a geofence transition
			} else if (TextUtils.equals(action,
					GeofenceUtils.ACTION_GEOFENCE_TRANSITION)) {

				handleGeofenceTransition(context, intent);

				// The Intent contained an invalid action
			} else {
				Log.e(GeofenceUtils.APPTAG,
						getString(R.string.invalid_action_detail, action));
				Toast.makeText(context, R.string.invalid_action,
						Toast.LENGTH_LONG).show();
			}
		}

		/**
		 * If you want to display a UI message about adding or removing
		 * geofences, put it here.
		 * 
		 * @param context
		 *            A Context for this component
		 * @param intent
		 *            The received broadcast Intent
		 */
		private void handleGeofenceStatus(Context context, Intent intent) {

		}

		/**
		 * Report geofence transitions to the UI
		 * 
		 * @param context
		 *            A Context for this component
		 * @param intent
		 *            The Intent containing the transition
		 */
		private void handleGeofenceTransition(Context context, Intent intent) {
			/*
			 * If you want to change the UI when a transition occurs, put the
			 * code here. The current design of the app uses a notification to
			 * inform the user that a transition has occurred.
			 */
		}

		/**
		 * Report addition or removal errors to the UI, using a Toast
		 * 
		 * @param intent
		 *            A broadcast Intent sent by ReceiveTransitionsIntentService
		 */
		private void handleGeofenceError(Context context, Intent intent) {
			String msg = intent
					.getStringExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS);
			Log.e(GeofenceUtils.APPTAG, msg);
			Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Define a DialogFragment to display the error dialog generated in
	 * showErrorDialog.
	 */
	public static class ErrorDialogFragment extends DialogFragment {

		// Global field to contain the error dialog
		private Dialog mDialog;

		/**
		 * Default constructor. Sets the dialog field to null
		 */
		public ErrorDialogFragment() {
			super();
			mDialog = null;
		}

		/**
		 * Set the dialog to display
		 * 
		 * @param dialog
		 *            An error dialog
		 */
		public void setDialog(Dialog dialog) {
			mDialog = dialog;
		}

		/*
		 * This method must return a Dialog to the DialogFragment.
		 */
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return mDialog;
		}
	}

}
