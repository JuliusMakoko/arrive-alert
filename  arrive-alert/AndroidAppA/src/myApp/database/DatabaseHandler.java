/*
 * Author - Ben Wegher
 * Date   - 6/10/2014
 * Class  - DatabaseHandler.java
 * Description - This class specifies all the necessary methods for my SQLite database.
 * 				 Has 2 tables - alerts, locations. Currently alerts is the only one in use.
 * 
 */

package myApp.database;

import java.util.ArrayList;
import myApp.list.AlertListItem;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHandler extends SQLiteOpenHelper {

    // Database Version
    private static final int DATABASE_VERSION = 1;
 
    // Database name
    private static final String DATABASE_NAME = "alertsManager.db";
 
    // Table names
    private static final String TABLE_ALERTS = "alerts";
    private static final String TABLE_LOCATIONS = "locations";
 
    // common column names
    private static final String KEY_NAME = "name";
    private static final String KEY_LOCATION_ID = "location";
    
    // alerts table columns names
    private static final String KEY_ID = "id";
    private static final String KEY_CONTACT = "contact";  
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_TRIGGER = "trigger";
    private static final String KEY_ICON = "icon";
    
 // locations table columns names
    // TODO -- coords, radius
	
	public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_ALERTS_TABLE = "CREATE TABLE " + TABLE_ALERTS + "("
//				+ KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_NAME + " TEXT PRIMARY KEY,"
				+ KEY_CONTACT + " TEXT,"
				+ KEY_LOCATION_ID + " INTEGER,"
				+ KEY_MESSAGE + " TEXT,"
				+ KEY_TRIGGER + " TEXT,"
                + KEY_ICON + " INTEGER" + ")";
		
		// TODO
		// String CREATE_LOCATIONS_TABLE = "";
		
        db.execSQL(CREATE_ALERTS_TABLE);
        //db.execSQL(CREATE_LOCATIONS_TABLE);
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALERTS);
 
        // Create tables again
        onCreate(db);
	}
	
	/**
     * All CRUD(Create, Read, Update, Delete) Operations
     */
	
	// Adding a new alert
    public void addAlert(AlertListItem alert) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, alert.getTitle()); // Alert Name
        values.put(KEY_CONTACT, alert.getContact()); // Alert contact (phone/email/...)
        
        // locations should be stored in a separate table with unique IDs (here)
        // table itself will have corresponding ID, name, coordinates and radius
        values.put(KEY_LOCATION_ID, alert.getLocation()); // NEEDS TO BE CHANGED
        
        values.put(KEY_MESSAGE, alert.getMessage());  // Message to be sent
        values.put(KEY_TRIGGER, alert.getWhen());  // 'ENTER' or 'EXIT'
        values.put(KEY_ICON, alert.getIcon());  // R.id of corresponding icon
 
        // Inserting Row
        db.insert(TABLE_ALERTS, null, values);
        db.close(); // Closing database connection
    }
	
    // Getting single alert by name (Primary Key)
    AlertListItem getAlert(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
 
        Cursor cursor = db.query(TABLE_ALERTS, new String[] { KEY_NAME,
                KEY_CONTACT, KEY_LOCATION_ID, KEY_MESSAGE, KEY_TRIGGER, KEY_ICON }, KEY_NAME + "=?",
                new String[] { name }, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();
 
        AlertListItem alert = new AlertListItem(cursor.getString(0),
                cursor.getString(1), Integer.parseInt(cursor.getString(2)), cursor.getString(3),
                cursor.getString(4), Integer.parseInt(cursor.getString(5)));
        // return alert
        return alert;
    }
    
 // Getting All Alerts
    public ArrayList<AlertListItem> getAllAlerts() {
        ArrayList<AlertListItem> alertList = new ArrayList<AlertListItem>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_ALERTS;
 
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
 
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
            	AlertListItem alert = new AlertListItem();
            	alert.setTitle(cursor.getString(0));
            	alert.setContact(cursor.getString(1));
            	alert.setLocation(Integer.parseInt(cursor.getString(2)));
            	alert.setMessage(cursor.getString(3));
            	alert.setWhen(cursor.getString(4));
            	alert.setIcon(Integer.parseInt(cursor.getString(5)));
                // Adding alert to list
                alertList.add(alert);
            } while (cursor.moveToNext());
        }
 
        // return alert list
        return alertList;
    }
    
    // Updating single alert
    public int updateAlert(AlertListItem alert) {
        SQLiteDatabase db = this.getWritableDatabase();
 
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, alert.getTitle());
        values.put(KEY_CONTACT, alert.getContact());
        values.put(KEY_LOCATION_ID, alert.getLocation());
        values.put(KEY_MESSAGE, alert.getMessage());
        values.put(KEY_TRIGGER, alert.getWhen());
        values.put(KEY_ICON, alert.getIcon());
 
        // updating row
        return db.update(TABLE_ALERTS, values, KEY_NAME + " = ?",
                new String[] { alert.getTitle() });
    }
 
    // Deleting single contact
    public void deleteAlert(AlertListItem alert) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ALERTS, KEY_NAME + " = ?",
                new String[] { alert.getTitle() });
        db.close();
    }
 
    public boolean alertExists(String name) {
    	SQLiteDatabase db = this.getWritableDatabase();
    	Cursor cursor = db.rawQuery("select 1 from " + TABLE_ALERTS + " where name=?", 
    	     new String[] { name });
    	boolean exists = (cursor.getCount() > 0);
    	cursor.close();
    	db.close();
    	return exists;
    }

}
