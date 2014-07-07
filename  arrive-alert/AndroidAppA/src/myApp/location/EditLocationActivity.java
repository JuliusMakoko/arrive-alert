package myApp.location;

import java.util.ArrayList;

import myApp.androidappa.Constants;
import myApp.androidappa.R;
import myApp.database.DatabaseHandler;
import myApp.geofence.GeofenceUtils;
import myApp.list.LocationListAdapter;
import myApp.list.LocationListItem;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class EditLocationActivity extends ListActivity {

	private ArrayList<LocationListItem> locationListItems;
	private LocationListAdapter mAdapter;
	private DatabaseHandler db;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit_locations);

		db = new DatabaseHandler(this);

		// get the list
		ListView myList = (ListView) findViewById(android.R.id.list);

		// create an ArrayList of LocationListItems
		locationListItems = new ArrayList<LocationListItem>();

		// load saved locations from local storage into locationListItems
		locationListItems = db.getAllLocations();

		// set the list adapter
		mAdapter = new LocationListAdapter(this, locationListItems);
		setListAdapter(mAdapter);

		// register contextMenu
		registerForContextMenu(getListView());

		Intent mIntent = getIntent();
		if (mIntent.getStringExtra("TYPE") != null) {
			Log.w(Constants.DEBUG_TAG,
					"myList onItemClickListener was created.");
			myList.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					// TODO Auto-generated method stub
					Log.w(Constants.DEBUG_TAG, "List Item onClick() was fired.");
					LocationListItem returnMyName = (LocationListItem) parent
							.getItemAtPosition(position);

					Intent intentMessage = new Intent();
					intentMessage.putExtra("NAME", returnMyName.getName());
					setResult(RESULT_OK, intentMessage);

					finish();
				}
			});

		} else {
			Log.w(Constants.DEBUG_TAG,
					"mIntent.getStringExtra(\"TYPE\") IS null");
		}

		db.close();

	}

	// Handles long click selection of list items
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();

		switch (item.getItemId()) {
		case R.id.delete:
			LocationListItem deleteMe = (LocationListItem) mAdapter
					.getItem((int) info.id);

			// ********** TODO Confirmation or Undo feature
			// REMOVE GEOFENCES FIRST & UPDATE AFFECTED ALERTS
			db.deleteLocation(deleteMe); // remove location from database (don't
										 // have to do this)
			mAdapter.delete(deleteMe); // remove from adapter

			mAdapter.notifyDataSetChanged();

			Toast.makeText(EditLocationActivity.this,
					deleteMe.getName() + " was deleted.", Toast.LENGTH_LONG)
					.show();
			return true;
		case R.id.edit:
			// TODO - open appropriate activity and fill with data
			LocationListItem editMe = (LocationListItem) mAdapter
					.getItem((int) info.id);

			Intent editIntent = new Intent(this, AddLocationActivity.class);

			editIntent.putExtra("LOCATION_ID", editMe.getLocationId());
			editIntent.putExtra("NAME", editMe.getName());
			editIntent.putExtra("LATITUDE", editMe.getLatitude());
			editIntent.putExtra("LONGITUDE", editMe.getLongitude());
			editIntent.putExtra("RADIUS", editMe.getRadius());
			editIntent.putExtra("ADDRESS", editMe.getAddress());

			startActivityForResult(editIntent, Constants.UPDATE);

		default:
			return super.onContextItemSelected(item);
		}
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.context_menu, menu);
	}

	// onClick() for Add New Location button
	public void addNewLocation(View v) {
		Intent intentAddNewLocation = new Intent(this,
				AddLocationActivity.class);
		startActivityForResult(intentAddNewLocation, Constants.LOCATION);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
			if (requestCode == Constants.LOCATION) {
				String name = data.getStringExtra("NAME");
				String address = data.getStringExtra("ADDRESS");
				double longitude = data.getDoubleExtra("LONGITUDE",
						GeofenceUtils.INVALID_FLOAT_VALUE);
				double latitude = data.getDoubleExtra("LATITUDE",
						GeofenceUtils.INVALID_FLOAT_VALUE);
				float radius = data.getFloatExtra("RADIUS",
						GeofenceUtils.INVALID_FLOAT_VALUE);

				// needs a name, address and radius to preview
				LocationListItem addMe = new LocationListItem(name, latitude,
						longitude, radius);
				addMe.setAddress(address);

				db.addLocation(addMe); // add the location to our database
				mAdapter.add(addMe); // add the location to the list adapter
				mAdapter.notifyDataSetChanged(); // notify the list adapter
				Log.w("Warning", "EditLocation - onActivityResult ran");
			} else if (requestCode == Constants.UPDATE) {
				// restart activity so the updated alert appears
				finish();
				startActivity(getIntent());
			}

			// result was not OK
		} else {

			Log.w("Warning", "Warning: activity result not ok");
		}

	}

}
