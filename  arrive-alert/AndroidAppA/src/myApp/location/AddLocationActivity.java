package myApp.location;

import java.io.IOException;
import java.util.List;

import myApp.androidappa.Constants;
import myApp.androidappa.R;
import myApp.database.DatabaseHandler;
import myApp.list.LocationListItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AddLocationActivity extends Activity implements
		OnMapLongClickListener {
	private GoogleMap googleMap;
	private EditText editTextSearch;
	private EditText locationName;
	private Button createButton;
	private Circle circle;
	private Marker marker;
	private double latitude;
	private double longitude;
	private float radius = 400;
	private String address;
	private int locationId;
	private String name;

	private static final float MIN_RADIUS = 150f;
	private static final float MAX_RADIUS = 1500f;
	private static final int DEFAULT_ZOOM = 15;
	private static final int DEFAULT_CIRCLE_COLOR = Color.RED;
	private static final int DEFAULT_FILL_COLOR = 0x40ff0000;

	private DatabaseHandler db;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_location);

		// get db handler
		db = new DatabaseHandler(this);

		createButton = (Button) findViewById(R.id.button2);
		locationName = (EditText) findViewById(R.id.editText1);

		// Getting Google Play availability status
		int status = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(getBaseContext());

		// Showing status
		if (status != ConnectionResult.SUCCESS) { // Google Play Services are
													// not available

			int requestCode = 10;
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this,
					requestCode);
			dialog.show();

		} else { // Google Play Services are available

			// Getting reference to the SupportMapFragment of activity_main.xml
			MapFragment fm = (MapFragment) getFragmentManager()
					.findFragmentById(R.id.map);

			// Getting GoogleMap object from the fragment
			googleMap = fm.getMap();
			googleMap.setPadding(0, 0, 0, 140);
			googleMap.setOnMapLongClickListener(this);
			
			// Enabling MyLocation Layer of Google Map
			// googleMap.setMyLocationEnabled(true);

			// Getting LocationManager object from System Service
			// LOCATION_SERVICE
			LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

			// Creating a criteria object to retrieve provider
			Criteria criteria = new Criteria();

			// Getting the name of the best provider
			String provider = locationManager.getBestProvider(criteria, true);

			// Getting Current Location
			Location location = locationManager.getLastKnownLocation(provider);

			// check if activity was called by EditLocationActivity...
			Intent received = getIntent();
			boolean isEdit = (received.getStringExtra("NAME") != null);

			if (location != null && isEdit == false) {
				// center the map on user
				Log.w("Warning", "Centering on user");
				centerMapOnMyLocation();

			} else if (isEdit) { // if activity was called by edit intent...

				Toast.makeText(this, "Edit Intent...", Toast.LENGTH_LONG)
						.show();

				// change button text
				createButton.setText(Constants.LOCATION_UPDATE_TEXT);

				name = received.getStringExtra("NAME");
				locationName.setText(name);

				// disable locationName editText box
				locationName.setEnabled(false);

				// update globals
				latitude = received.getDoubleExtra("LATITUDE", latitude);
				longitude = received.getDoubleExtra("LONGITUDE", longitude);
				radius = received.getFloatExtra("RADIUS", radius);
				address = received.getStringExtra("ADDRESS");
				locationId = received.getIntExtra("LOCATION_ID", -1);

				// center map on location to be edited
				googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
						new LatLng(latitude, longitude), DEFAULT_ZOOM));

				// add a circle around received location
				circle = googleMap.addCircle(new CircleOptions()
						.center(new LatLng(latitude, longitude)).radius(radius)
						.strokeColor(DEFAULT_CIRCLE_COLOR).strokeWidth(2)
						.fillColor(DEFAULT_FILL_COLOR));

				// add a marker at received location
				marker = googleMap.addMarker(new MarkerOptions().position(
						new LatLng(latitude, longitude)).title(name));
				marker.showInfoWindow();
			}

		}
	}

	private void centerMapOnMyLocation() {
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();

		Location location = locationManager
				.getLastKnownLocation(locationManager.getBestProvider(criteria,
						false));

		if (location != null) {
			googleMap
					.animateCamera(CameraUpdateFactory.newLatLngZoom(
							new LatLng(location.getLatitude(), location
									.getLongitude()), DEFAULT_ZOOM));

			latitude = location.getLatitude();
			longitude = location.getLongitude();
			// add a fence and marker at current location
			updateFence(new LatLng(latitude, longitude), "Last Known Location");

		}
	}

	private void updateFence(LatLng latLng, String name) {
		// add a circle around given lat and long
		if(circle != null) 
			circle.remove();
		if(marker != null)
			marker.remove();
		
		circle = googleMap.addCircle(new CircleOptions()
				.center(latLng).radius(radius)
				.strokeColor(DEFAULT_CIRCLE_COLOR)
				.fillColor(DEFAULT_FILL_COLOR)
				.strokeWidth(2));

		// add a marker at loc
		marker = googleMap.addMarker(new MarkerOptions().position(latLng).title(name));
		marker.showInfoWindow();

	}

	// onClick() method of 'find' button - searches GoogleMap fragment
	public void go(View v) {
		editTextSearch = (EditText) findViewById(R.id.editText2);

		String check = editTextSearch.getText().toString();
		if (!check.equals("")) {
			List<Address> myList;
			Geocoder gc = new Geocoder(getBaseContext());
			try {
				myList = gc.getFromLocationName(editTextSearch.getText()
						.toString(), 1);
				if (myList.size() > 0) {
					Address a = myList.get(0);

					// update global long and lat
					latitude = a.getLatitude();
					longitude = a.getLongitude();

					// update global address
					StringBuilder s = new StringBuilder();
					int i = 0;
					while (a.getAddressLine(i) != null) {
						s.append(a.getAddressLine(i));
						s.append("\n");
						i++;
					}
					if (s.length() > 0)
						address = s.toString();

					googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
							new LatLng(latitude, longitude), DEFAULT_ZOOM));
					// if a circle exists remove it and add a new one
					updateFence(new LatLng(latitude, longitude), editTextSearch.getText()
							.toString());

				} else {
					Toast.makeText(this, "Sorry, couldn't find that location",
							Toast.LENGTH_LONG).show();
				}
			} catch (IOException e) {
				Toast.makeText(this, "Sorry, couldn't find that location",
						Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
		} else {
			Toast.makeText(this, "You didn't enter a location!",
					Toast.LENGTH_SHORT).show();
		}
	}

	// decrease radius of circle
	public void shrink(View v) {
		if (radius > MIN_RADIUS)
			radius -= 50;

		// update circle
		if (circle != null)
			circle.setRadius(radius);
	}

	// increase radius of circle
	public void grow(View v) {
		if (radius < MAX_RADIUS)
			radius += 50;

		// update circle
		if (circle != null)
			circle.setRadius(radius);
	}

	// onClick() handler
	public void buttonHandler(View v) {
		if (createButton.getText().equals(Constants.LOCATION_ADD_TEXT))
			addLocation(v);
		else if (createButton.getText().equals(Constants.LOCATION_UPDATE_TEXT))
			updateLocation(v);
	}

	public void addLocation(View v) {
		// if user input is valid then we can add the location
		if (isInputValid()) {
			// get location name
			String name = locationName.getText().toString();

			// update address based on current lat + long
			updateAddress();
			
			// check for duplicate location name
			if (db.locationExists(name)) {
				Toast.makeText(
						this,
						"A location with that name already exists! Please enter a new name.",
						Toast.LENGTH_LONG).show();
				return;
			}

			// Debugging toast
			Toast.makeText(
					this,
					"Added new location: ID#" + locationId + " " + name
							+ " latitude = " + latitude + " longitude = "
							+ longitude + " radius = " + radius + " Address: "
							+ address, Toast.LENGTH_LONG).show();

			Intent intentMessage = new Intent();

			intentMessage.putExtra("NAME", name);
			intentMessage.putExtra("ADDRESS", address);
			intentMessage.putExtra("LONGITUDE", longitude);
			intentMessage.putExtra("LATITUDE", latitude);
			intentMessage.putExtra("RADIUS", radius);

			setResult(RESULT_OK, intentMessage);
			finish();
		}
	}

	public void updateLocation(View v) {
		// if user input is valid then we can update the location
		if (isInputValid()) {
			// get user data and convert to Strings
			// String name = locationName.getText().toString();

			// update address based on current lat + long
			updateAddress();
			
			LocationListItem updateMe = new LocationListItem(locationId, name,
					latitude, longitude, radius, address);

			if (db.updateLocation(updateMe) == 1) {
				Toast.makeText(getApplicationContext(),
						"'" + name + "' was succesfully updated!",
						Toast.LENGTH_LONG).show();
				Intent intentMessage = new Intent();
				setResult(RESULT_OK, intentMessage);

				finish();
			} else {
				Toast.makeText(
						getApplicationContext(),
						"Couldn't find a location named '" + name
								+ "'. With id#" + locationId, Toast.LENGTH_LONG)
						.show();
			}
			db.close();
		}
	}

	public boolean isInputValid() {
		String name = locationName.getText().toString();

		if (name.equals("")) {
			Toast.makeText(this, "Please enter a name for this location.",
					Toast.LENGTH_LONG).show();
			return false;
		} else if (radius < MIN_RADIUS || radius > MAX_RADIUS) {
			Toast.makeText(this, "Invalid radius = " + radius,
					Toast.LENGTH_LONG).show();
			return false;
		} else if (longitude < -180 || longitude > 180 || latitude < -90
				|| latitude > 90) {
			Toast.makeText(this,
					"Invalid long " + longitude + " or lat " + latitude,
					Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}

	@Override
	public void onMapLongClick(LatLng point) {
		// update global variables
		latitude = point.latitude;
		longitude = point.longitude;
		
		updateFence(point, "New Location");
	}
	
	// returns a string containing the address of the current location 
	public void updateAddress() {
		address = "No address found.";
		List<Address> myList;
		Geocoder gc = new Geocoder(getBaseContext());
		try {
			myList = gc.getFromLocation(latitude, longitude, 1);
			if (myList.size() > 0) {
				Address temp = myList.get(0);
				StringBuilder s = new StringBuilder();
				int i = 0;
				while (temp.getAddressLine(i) != null) {
					s.append(temp.getAddressLine(i));
					s.append("\n");
					i++;
				}
				if (s.length() > 0)
					address = s.toString();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
