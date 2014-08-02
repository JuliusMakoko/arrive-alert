package myApp.settings;

import myApp.androidappa.Constants;
import myApp.geofence.SimpleGeofence;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class SettingsPreferences {
	// The SharedPreferences object in which settings are stored
    private final SharedPreferences mPrefs;
    
	private static final String SETTINGS_PREFERENCES = "SettingsPreferences";
	public static final String KEY_FREQUENCY = "myApp.settings.KEY_FREQUENCY";
	
	// Create the SharedPreferences storage with private access only
	public SettingsPreferences(Context context) {
		mPrefs = context.getSharedPreferences(SETTINGS_PREFERENCES,
					Context.MODE_PRIVATE);
	}
	
	/*
	 * return (getters)
	 */
	public String getSettings() {	
		return mPrefs.getString(KEY_FREQUENCY, Constants.SETTINGS_DEFAULT);
	}

	/*
	 * save (setters)
	 */
	
	public void saveSettings(String newSetting) {
		// if settings were actually changed, then update sharedPreferences
		if(!newSetting.equals(getSettings())) {
			Editor editor = mPrefs.edit();
		
			editor.putString(KEY_FREQUENCY, newSetting);
		
			// Commit the changes
			editor.commit();
		}
	}

}
