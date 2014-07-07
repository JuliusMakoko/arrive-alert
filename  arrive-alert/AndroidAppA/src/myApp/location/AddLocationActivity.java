package myApp.location;

import java.io.IOException;
import java.util.List;

import myApp.androidappa.R;
import myApp.database.DatabaseHandler;
import myApp.list.LocationListItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
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

public class AddLocationActivity extends Activity {
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

	private DatabaseHandler db;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_locations);

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

				// change button text
				createButton.setText("Update Location");
				
				String name = received.getStringExtra("NAME");
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
						new LatLng(latitude, longitude), 14));

				// add a circle around received location
				circle = googleMap.addCircle(new CircleOptions()
						.center(new LatLng(latitude, longitude))
						.radius(radius).strokeColor(Color.RED)
						.fillColor(Color.TRANSPARENT));

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
									.getLongitude()), 14));

			latitude = location.getLatitude();
			longitude = location.getLongitude();
			// add a circle around current location
			circle = googleMap.addCircle(new CircleOptions()
					.center(new LatLng(latitude, longitude)).radius(radius)
					.strokeColor(Color.RED).fillColor(Color.TRANSPARENT));

			// add a marker at current / last known loc
			marker = googleMap.addMarker(new MarkerOptions().position(
					new LatLng(latitude, longitude)).title(
					"Last Known Location"));
			marker.showInfoWindow();
		}
	}

	// onClick() method of 'find' button - searches GoogleMap fragment
	public void go(View v) {
		editTextSearch = (EditText) findViewById(R.id.editText2);

		String check = editTextSearch.getText().toString();
		if (!check.equals("")) {
			List<Address> myList;
			Geocoder gc = new Geocoder(getBaseContext());
			try {
				myList = gc
						.getFromLocationName(editTextSearch.getText().toString(), 1);
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
							new LatLng(latitude, longitude), 14));
					// if a circle exists remove it and add a new one
					if (circle != null && circle.isVisible()) {
						circle.remove();
						circle = googleMap.addCircle(new CircleOptions()
								.center(new LatLng(latitude, longitude))
								.radius(radius).strokeColor(Color.RED)
								.fillColor(Color.TRANSPARENT));
					}
					// if a marker exists remove it and add a new one
					if (marker != null && marker.isVisible()) {
						marker.remove();
						marker = googleMap.addMarker(new MarkerOptions()
								.position(new LatLng(latitude, longitude)));
					}
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
		if (radius > 100)
			radius -= 50;

		// update circle
		if (circle != null)
			circle.setRadius(radius);
	}

	// increase radius of circle
	public void grow(View v) {
		if (radius < 1500)
			radius += 50;

		// update circle
		if (circle != null)
			circle.setRadius(radius);
	}

	// onClick() handler
	public void buttonHandler(View v) {
		if (createButton.getText().equals("Add This Location"))
			addLocation(v);
		else
			updateLocation(v);
	}

	public void addLocation(View v) {
		// if user input is valid then we can add the location
		if (isInputValid()) {
			// get location name
			String name = locationName.getText().toString();

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
					"Added new location: " + name + " latitude = " + latitude
							+ " longitude = " + longitude + " radius = "
							+ radius + " Address: " + address,
					Toast.LENGTH_LONG).show();

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
			String name = locationName.getText().toString();

			
			LocationListItem updateMe = new LocationListItem(locationId, name, latitude, longitude,
					radius, address);

			if (db.updateLocation(updateMe) == 1) {
				Toast.makeText(getApplicationContext(),
						"'" + name + "' was succesfully updated!",
						Toast.LENGTH_LONG).show();
				Intent intentMessage = new Intent();
				setResult(RESULT_OK, intentMessage);

				finish();
			} else {
				Toast.makeText(getApplicationContext(),
						"Couldn't find a location named '" + name + "'.",
						Toast.LENGTH_LONG).show();
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
		} else if (radius < 100 || radius > 1500) {
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

}
