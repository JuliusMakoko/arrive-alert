package myApp.settings;

import myApp.androidappa.Constants;
import myApp.androidappa.R;
import myApp.androidappa.R.layout;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

public class SettingsActivity extends Activity {
	private SettingsPreferences mPrefs;
	private RadioButton radioNever;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        mPrefs = new SettingsPreferences(this);
        radioNever = (RadioButton) findViewById(R.id.radioNever);
        if(mPrefs.getSettings().equals(Constants.SETTINGS_NEVER))
        	radioNever.setChecked(true);
        
    }
	
	// 'save' button onClick handler
	public void save(View v) {
		// TODO - let user know if they changed settings that it will turn off all their active alerts
		String newSetting;
		if(radioNever.isChecked())
			newSetting = Constants.SETTINGS_NEVER;
		else
			newSetting = Constants.SETTINGS_ONCE;
		
		mPrefs.saveSettings(newSetting);
		Toast.makeText(this, "Settings saved succesfully", Toast.LENGTH_SHORT).show();
		finish();
	}
}
