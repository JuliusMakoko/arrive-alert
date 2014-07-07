/*
 * Author - Ben Wegher
 * Date   - 6/10/2014
 * Class  - AlertListItem.java
 * Description - This class specifies the attributes that define an Alert. 
 */

package myApp.list;

import myApp.androidappa.Constants;
import myApp.androidappa.R;

public class AlertListItem {
	private String title;   // name of the alert
	private int icon;	    // id for email or text icon
	private String contact; // phone # or email
	private String when;    // "enter" or "exit"
	private String message; // the message being sent
	private int location;   // still unsure how it will be stored
	private boolean active = false;
	
	// Constructors
	public AlertListItem(){
		this.title = "alert name";
		this.contact = "contact@email.com";
		this.message = "Hey mom, I'm leaving school now. See you soon!";
		this.icon = Constants.EMAIL;
		this.location = Constants.LOCATION; // TODO -- change this
		this.when = "EXIT";
	}

//	public AlertListItem(String title, int icon, String contact, String message){
//		this.title = title;
//		this.icon = icon;
//		this.contact = contact;
//		this.message = message;
//	}
	
	public AlertListItem(String title, int icon, String when, String contact, String message){
		this.title = title;
		this.icon = icon;
		this.when = when;
		this.contact = contact;
		this.message = message;
	}
	
	public AlertListItem(String title, String contact, int location, String message, String when, int icon){
		this.title = title;
		this.icon = icon;
		this.when = when;
		this.contact = contact;
		this.message = message;
		this.location = location;
	}

	// GETTERS
	public String getTitle(){
		return this.title;
	}
	
	public int getIcon(){
		return this.icon;
	}
	
	// Used to retrieve the drawable icon resource
	public int getIconID() {
		int returnMe = -1;
		if(this.icon == Constants.EMAIL)
			returnMe = R.drawable.ic_action_email;
		else if(this.icon == Constants.TEXT)
			returnMe = R.drawable.ic_action_chat;
		return returnMe;
	}
	
	public int getLocation() {
		return location;
	}
	
	public String getContact(){
		return this.contact;
	}
	
	public String getWhen(){
		return this.when;
	}
	
	public int getWhenInt() {
		if(this.when.equals("ENTER"))
			return Constants.ENTER;
		else
			return Constants.EXIT;
	}
	
	public String getMessage() {
		return message;
	}
	
	public boolean getActive() {
		return active;
	}
	
	// SETTERS
	public void setTitle(String title){
		this.title = title;
	}
	
	public void setIcon(int icon){
		this.icon = icon;
	}
	
	public void setLocation(int location) {
		this.location = location;
	}
	
	public void setContact(String contact){
		this.contact = contact;
	}
	
	public void setWhen(String when){
		this.when = when;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public void setActive(boolean active) {
		this.active = active;
	}
}
