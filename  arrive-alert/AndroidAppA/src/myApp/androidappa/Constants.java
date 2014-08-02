package myApp.androidappa;

public final class Constants {
	public static final int EMAIL = 1;
	public static final int TEXT = 2;
	public static final int CONTACT_PICKER_RESULT = 3;
	public static final int UPDATE = 4;
	
	public static final int LOCATION = 5;
	public static final int LOCATION_PICKER_RESULT = 6;
	public static final String LOCATION_PICKER_CALL = "LOCATION_CALL";

	public static final String LOCATION_ADD_TEXT = "Add";
	public static final String LOCATION_UPDATE_TEXT = "Update";
	
	// Maximum alerts/locations that the user can create
	public static final int MAX_ALERTS = 15;
	public static final int MAX_LOCATIONS = 15;
	
	// Transition Constants
	public static final int ENTER = 1;
	public static final int EXIT = 2;
	
	// Text Message Constants
	public static final String SENT = "SMS_SENT";
	public static final String DELIVERED = "SMS_DELIVERED";
	public static final int MAX_SMS_MESSAGE_LENGTH = 160;
	
	// Settings Constants
	public static final String SETTINGS_ONCE = "ONCE";
	public static final String SETTINGS_NEVER = "NEVER";
	public static final String SETTINGS_DEFAULT = SETTINGS_ONCE;
	
	// for intents
	public static final String ALERT_NAME = "NAME";
	
	
	public static final String DEBUG_TAG = "Debug";
	
	
}
