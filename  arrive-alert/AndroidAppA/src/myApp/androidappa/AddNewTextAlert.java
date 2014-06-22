/*
 * Author - Ben Wegher
 * Date   - 6/10/2014
 * Class  - AddNewTextAlert.java
 * Description - This activity is a form that lets the user create a specialized
 * 				 text alert. After being validated, the relevant alert data is returned to 
 * 				 MainActivity and inserted into the SQLite Database - alerts table. 
 */

package myApp.androidappa;

import myApp.database.DatabaseHandler;
import myApp.list.AlertListItem;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

public class AddNewTextAlert extends Activity {
	private EditText alertName;
	private EditText phoneAdd;
	private EditText location; // location
	private EditText message;
	private RadioButton enterRadio;
	private RadioButton exitRadio;
	private Button createButton;
	private DatabaseHandler db;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_new_text_alert);
		alertName = (EditText) findViewById(R.id.editText1);
		phoneAdd = (EditText) findViewById(R.id.editTextPhone);
		location = (EditText) findViewById(R.id.editText3);
		message = (EditText) findViewById(R.id.editText4);
		enterRadio = (RadioButton) findViewById(R.id.radio0);
		exitRadio = (RadioButton) findViewById(R.id.radio1);
		createButton = (Button) findViewById(R.id.button2);
		db = new DatabaseHandler(this);

		// grab data received from calling intent
		Intent received = getIntent();
		alertName.setText(received.getStringExtra("TITLE"));
		
		// if alert name is not empty then activity was opened by an Edit intent
		if (!checkEmpty(alertName.getText().toString())) {
			phoneAdd.setText(received.getStringExtra("CONTACT"));
			String loc = "" + received.getIntExtra("LOCATION", 0);
			location.setText(loc);
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

	// onClick() method
	public void addAlert(View V) {

		// if user input is valid then we can add the alert
		if (isInputValid()) {
			// get user data and convert to Strings
			String name = alertName.getText().toString();
			String phone = phoneAdd.getText().toString();
			String text = message.getText().toString();
			String when;
			if (exitRadio.isChecked())
				when = "EXIT";
			else
				when = "ENTER";

			// check for duplicate alert name
			if (db.alertExists(name)) {
				Toast.makeText(
						AddNewTextAlert.this,
						"An alert with that name already exists! Please enter a new name.",
						Toast.LENGTH_LONG).show();
				return;
			}

			// TODO -- CHECK IF LOCATION EXISTS IN LOCATION TABLE!
			// location must be created before it can be used

			Intent intentMessage = new Intent();

			// Debugging toast
			Toast.makeText(AddNewTextAlert.this,
					"Added new text alert: " + name + " " + phone + " " + text,
					Toast.LENGTH_LONG).show();

			// Return alert data to MainActivity
			intentMessage.putExtra("TITLE", name);
			intentMessage.putExtra("CONTACT", phone);
			intentMessage.putExtra("ICON", Constants.TEXT);
			intentMessage.putExtra("MESSAGE", text);
			intentMessage.putExtra("WHEN", when);
			intentMessage.putExtra("LOCATION", 3);

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
			String phone = phoneAdd.getText().toString();
			String text = message.getText().toString();
			String when;
			if (exitRadio.isChecked())
				when = "EXIT";
			else
				when = "ENTER";
			AlertListItem updateMe = new AlertListItem(name, phone, Constants.LOCATION, text, when, Constants.TEXT);

			if (db.updateAlert(updateMe) == 1) {
				Toast.makeText(getApplicationContext(),
						"'" + name + "' was succesfully updated!", Toast.LENGTH_LONG)
						.show();
				Intent intentMessage = new Intent();
				setResult(RESULT_OK, intentMessage);

				finish();
			} else {
				Toast.makeText(getApplicationContext(),
						"Couldn't find an alert named '" + name + "'.", Toast.LENGTH_LONG)
						.show();
			}
			db.close();
		}
	}

	public boolean isInputValid() {
		// get user data and convert to Strings
		String name = alertName.getText().toString();
		String phone = phoneAdd.getText().toString();
		String text = message.getText().toString();

		// Validate input
		if (checkEmpty(name)) { // Ensure name is NOT empty
			Toast.makeText(AddNewTextAlert.this, "Give your alert a name!",
					Toast.LENGTH_LONG).show();
			return false;
		} else if (checkEmpty(phone)) { // Ensure phone # is NOT empty
			Toast.makeText(AddNewTextAlert.this,
					"You forgot to enter a phone number", Toast.LENGTH_LONG)
					.show();
			return false;
		} else if (!checkPhone(phone)) { // Ensure phone # passes validation
			Toast.makeText(
					AddNewTextAlert.this,
					"Phone number must contain only numbers, dashes or parentheses "
							+ phone, Toast.LENGTH_LONG).show();
			return false;
		} else if (checkEmpty(text)) { // Ensure message is NOT empty
			Toast.makeText(AddNewTextAlert.this,
					"You need to enter a message to send to " + phone,
					Toast.LENGTH_LONG).show();
			return false;
		}

		return true;
	}

	// Checks if the given string is empty
	private boolean checkEmpty(String s) {
		return s.equals("");
	}

	// Checks if the given string consists solely of numbers, '-', '(', ')' or
	// ' '
	private boolean checkPhone(String s) {
		char[] phone = s.toCharArray();
		char[] check = { '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '-',
				'(', ')', ' ' };

		boolean checkVal = false;
		// compare each value of phone # to our check array
		for (int i = 0; i < phone.length; i++) {
			for (char c : check)
				if (phone[i] == c)
					checkVal = true;
			if (checkVal == false)
				return false;
			else
				checkVal = false;
		}

		return true;
	}

	// launches a contact picker to select phone # from contacts
	public void launchContactPicker(View view) {
		Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
				Contacts.CONTENT_URI);
		startActivityForResult(contactPickerIntent,
				Constants.CONTACT_PICKER_RESULT);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
			if (requestCode == Constants.CONTACT_PICKER_RESULT) {

				// TODO -- Need to handle multiple phone #s
				// i.e. let user pick between them
				Cursor cursor = null;
				String phone = "";
				try {

					Uri result = data.getData();
					Log.v(Constants.DEBUG_TAG, "Got a contact result: "
							+ result.toString());

					// get the contact id from the Uri
					String id = result.getLastPathSegment();

					// query for everything phone
					cursor = getContentResolver().query(Phone.CONTENT_URI,
							null, Phone.CONTACT_ID + "=?", new String[] { id },
							null);

					int phoneInd = cursor.getColumnIndex(Phone.DATA);

					// let's just get the first text
					if (cursor.moveToFirst()) {
						phone = cursor.getString(phoneInd);
						Log.v(Constants.DEBUG_TAG, "Got phone: " + phone);

						// iterate through additional phone numbers
						while (cursor.moveToNext()) {
							Log.v(Constants.DEBUG_TAG, "Also found phone: "
									+ cursor.getString(phoneInd));
						}

					} else {
						// Toast.makeText(AddNewTextAlert.this,
						// "No phone found for contact.",
						// Toast.LENGTH_LONG).show();
						Log.w(Constants.DEBUG_TAG, "No results");
					}
				} catch (Exception e) {
					Log.e(Constants.DEBUG_TAG, "Failed to get phone data", e);
				} finally {
					if (cursor != null) {
						cursor.close();
					}

					EditText phoneEntry = (EditText) findViewById(R.id.editTextPhone);
					phoneEntry.setText(phone);

					if (phone.length() == 0) {
						Toast.makeText(this,
								"No phone number found for contact.",
								Toast.LENGTH_LONG).show();
					}

				}

			} else {
				// gracefully handle failure
				Log.d(Constants.DEBUG_TAG, "Warning: activity result not ok");
			}
		}

	}

}
