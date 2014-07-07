/*
 * Author - Ben Wegher
 * Date   - 6/10/2014
 * Class  - AddNewEmailAlert.java
 * Description - This activity is a form that lets the user create a specialized
 * 				 email alert. After being validated, the relevant alert data is returned to 
 * 				 MainActivity and inserted into the SQLite Database - alerts table. 
 */

package myApp.androidappa;

import myApp.database.DatabaseHandler;
import myApp.list.AlertListItem;
import myApp.location.EditLocationActivity;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

public class AddNewEmailAlert extends Activity {
	private DatabaseHandler db;
	private EditText alertName;
	private EditText emailAdd;
	private EditText location; // location
	private EditText message;
	private RadioButton enterRadio;
	private RadioButton exitRadio;
	private Button createButton;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_new_email_alert);
		alertName = (EditText) findViewById(R.id.editText1);
		emailAdd = (EditText) findViewById(R.id.editTextEmail);
		location = (EditText) findViewById(R.id.editTextLocation);
		message = (EditText) findViewById(R.id.editText4);
		enterRadio = (RadioButton) findViewById(R.id.radio0);
		exitRadio = (RadioButton) findViewById(R.id.radio1);
		createButton = (Button) findViewById(R.id.button2);
		db = new DatabaseHandler(this);

		// get data received from calling intent
		Intent received = getIntent();
		alertName.setText(received.getStringExtra("TITLE"));

		// if alert name is not empty then activity was opened by an Edit intent
		if (!checkEmpty(alertName.getText().toString())) {
			emailAdd.setText(received.getStringExtra("CONTACT"));

			int locId = received.getIntExtra("LOCATION", 0);
			if (db.locationExists(locId))
				location.setText(db.getLocation(locId).getName());
			else
				location.setText("Location not found");

			message.setText(received.getStringExtra("MESSAGE"));
			if (received.getStringExtra("WHEN").equals("EXIT"))
				exitRadio.setChecked(true);
			else
				enterRadio.setChecked(true);
			createButton.setText("Update Alert");

			// don't let user modify the name
			alertName.setEnabled(false);
		}
	}

	// onClick() handler
	public void buttonHandler(View V) {
		if (createButton.getText().equals("Create New Alert"))
			addAlert(V);
		else
			updateAlert(V);
	}

	// onClick() method (Create New Alert - Button)
	public void addAlert(View V) {
		// if user input is valid then we can add the alert
		if (isInputValid()) {
			// get user data and convert to Strings
			String name = alertName.getText().toString();
			String email = emailAdd.getText().toString();
			String loc = location.getText().toString();
			String text = message.getText().toString();
			String when;
			if (exitRadio.isChecked())
				when = "EXIT";
			else
				when = "ENTER";

			// check for duplicate alert name
			if (db.alertExists(name)) {
				Toast.makeText(
						AddNewEmailAlert.this,
						"An alert with that name already exists! Please enter a new name.",
						Toast.LENGTH_LONG).show();
				return;
			}

			Intent intentMessage = new Intent();

			// Debugging toast
			Toast.makeText(
					AddNewEmailAlert.this,
					"Added new email alert: " + name + " " + email + " " + text,
					Toast.LENGTH_LONG).show();

			intentMessage.putExtra("TITLE", name);
			intentMessage.putExtra("CONTACT", email);
			intentMessage.putExtra("ICON", Constants.EMAIL);
			intentMessage.putExtra("MESSAGE", text);
			intentMessage.putExtra("WHEN", when);
			intentMessage.putExtra("LOCATION", loc);

			setResult(RESULT_OK, intentMessage);

			finish();
		}
	}

	// onClick() method to handle alert updates
	public void updateAlert(View V) {
		// if user input is valid then we can update the alert
		if (isInputValid()) {
			// get user data and convert to Strings
			String name = alertName.getText().toString();
			String email = emailAdd.getText().toString();
			String text = message.getText().toString();
			String loc = location.getText().toString();
			String when;
			if (exitRadio.isChecked())
				when = "EXIT";
			else
				when = "ENTER";

			int locId = -1;
			if (db.locationExists(loc))
				locId = db.getLocation(loc).getLocationId();

			AlertListItem updateMe = new AlertListItem(name, email, locId,
					text, when, Constants.EMAIL);

			if (db.updateAlert(updateMe) == 1) {
				Toast.makeText(getApplicationContext(),
						"'" + name + "' was succesfully updated!",
						Toast.LENGTH_LONG).show();
				Intent intentMessage = new Intent();
				setResult(RESULT_OK, intentMessage);

				finish();
			} else {
				Toast.makeText(getApplicationContext(),
						"Couldn't find an alert named '" + name + "'.",
						Toast.LENGTH_LONG).show();
			}
			db.close();
		}
	}

	public boolean isInputValid() {
		// get user data and convert to Strings
		String name = alertName.getText().toString();
		String email = emailAdd.getText().toString();
		String text = message.getText().toString();
		String loc = location.getText().toString();

		// if a location by that name doesn't exist -- then return false
		if (db.locationExists(loc) == false) {
			Toast.makeText(
					AddNewEmailAlert.this,
					"Couldn't find a location by that name."
							+ " Make sure you've added it to your locations list first.",
					Toast.LENGTH_LONG).show();
			return false;
		}

		// Validate input
		if (checkEmpty(name)) { // Ensure name field is NOT empty
			Toast.makeText(AddNewEmailAlert.this, "Give your alert a name!",
					Toast.LENGTH_LONG).show();
			return false;
		} else if (checkEmpty(email)) { // Ensure email field is NOT empty
			Toast.makeText(AddNewEmailAlert.this,
					"You forgot to enter an email address", Toast.LENGTH_LONG)
					.show();
			return false;
			// Do a tiny amount of email validation
		} else if (!email.contains("@") || !email.contains(".")) {
			Toast.makeText(AddNewEmailAlert.this,
					"That email address isn't valid", Toast.LENGTH_LONG).show();
			return false;
		} else if (checkEmpty(text)) { // Ensure message field is NOT empty
			Toast.makeText(AddNewEmailAlert.this,
					"You need to enter a message to send to " + email,
					Toast.LENGTH_LONG).show();
			return false;
		}

		return true;
	}

	// Checks if the given string is empty
	private boolean checkEmpty(String s) {
		return (s.equals(""));
	}

	// launches a contact picker to select email address from contacts
	public void launchContactPicker(View view) {
		Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
				Contacts.CONTENT_URI);
		startActivityForResult(contactPickerIntent,
				Constants.CONTACT_PICKER_RESULT);
	}

	// onClick() for the 'Location' button
	public void locationChooser(View v) {
		Intent locationIntent = new Intent(this, EditLocationActivity.class);
		locationIntent.putExtra("TYPE", Constants.LOCATION_PICKER_CALL);
		startActivityForResult(locationIntent, Constants.LOCATION_PICKER_RESULT);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
			if (requestCode == Constants.CONTACT_PICKER_RESULT) {

				// TODO -- Need to handle multiple email addresses
				// i.e. let user pick between them
				Cursor cursor = null;
				String email = "";
				try {
					Uri result = data.getData();
					Log.v(Constants.DEBUG_TAG, "Got a contact result: "
							+ result.toString());

					// get the contact id from the Uri
					String id = result.getLastPathSegment();

					// query for everything email
					cursor = getContentResolver().query(Email.CONTENT_URI,
							null, Email.CONTACT_ID + "=?", new String[] { id },
							null);

					int emailInd = cursor.getColumnIndex(Email.DATA);

					// let's just get the first email
					if (cursor.moveToFirst()) {
						email = cursor.getString(emailInd);
						Log.v(Constants.DEBUG_TAG, "Got email: " + email);

						// iterate through additional email addresses
						while (cursor.moveToNext()) {
							Log.v(Constants.DEBUG_TAG, "Also found email: "
									+ cursor.getString(emailInd));
						}
					} else {
						// Toast.makeText(AddNewEmailAlert.this,
						// "No email found for contact.",
						// Toast.LENGTH_LONG).show();
						Log.w(Constants.DEBUG_TAG, "No results");
					}
				} catch (Exception e) {
					Log.e(Constants.DEBUG_TAG, "Failed to get email data", e);
				} finally {
					if (cursor != null) {
						cursor.close();
					}
					EditText emailEntry = (EditText) findViewById(R.id.editTextEmail);
					emailEntry.setText(email);
					if (email.length() == 0) {
						Toast.makeText(this, "No email found for contact.",
								Toast.LENGTH_LONG).show();
					}
				}

			} else if (requestCode == Constants.LOCATION_PICKER_RESULT) {
				String name = data.getStringExtra("NAME");
				EditText locationEntry = (EditText) findViewById(R.id.editTextLocation);
				locationEntry.setText(name);
				Log.d(Constants.DEBUG_TAG, "LOCATION_PICKER_RESULT received - location name = " + name);
			}
		} else {
			// gracefully handle failure
			Log.d(Constants.DEBUG_TAG, "Warning: activity result not ok");
		}

	}

}
