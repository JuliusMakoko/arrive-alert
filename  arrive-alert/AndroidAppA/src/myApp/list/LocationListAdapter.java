package myApp.list;

import java.util.ArrayList;

import myApp.androidappa.R;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class LocationListAdapter extends BaseAdapter {

	private Context context;
	private ArrayList<LocationListItem> locationItems;
	
	public LocationListAdapter(Context context, ArrayList<LocationListItem> locationItems){
		this.context = context;
		this.locationItems = locationItems;
	}
	
	public void add(LocationListItem item) {
		locationItems.add(item);
	}
	
	public boolean delete(LocationListItem item) {
		return locationItems.remove(item);
	}
	
	@Override
	public int getCount() {
		return locationItems.size();
	}

	@Override
	public Object getItem(int position) {
		return locationItems.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(R.layout.location_list_item, null);
        }
         
        //ImageView imgIcon = (ImageView)convertView.findViewById(R.id.icon);
        TextView txtName = (TextView)convertView.findViewById(R.id.name);
        TextView txtAddress = (TextView)convertView.findViewById(R.id.address);
        TextView txtRadius = (TextView)convertView.findViewById(R.id.radius);
         
        
        //imgIcon.setImageResource(locationItems.get(position).getIconID());     
        txtName.setText(locationItems.get(position).getName());
        txtAddress.setText(locationItems.get(position).getAddress());
        txtRadius.setText("Radius = " + Float.toString(locationItems.get(position).getRadius()) + "m");
        
        
        return convertView;
	}

}
