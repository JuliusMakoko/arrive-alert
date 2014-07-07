package myApp.location;

public class Location {
	// Instance variables
		private final String id;
		private String name;
		private final double mLatitude;
		private final double mLongitude;
		private final float mRadius;

		/**
		 * @param id
		 *            The location's request ID
		 * @param name
		 *            The location's name
		 * @param latitude
		 *            Latitude of the location.
		 * @param longitude
		 *            Longitude of the location.
		 * @param radius
		 *            Radius of the location's geofence circle.
		 */
		
		public Location(String id, String name, double latitude, double longitude,
				float radius) {
			// Set the instance fields from the constructor
			this.id = id;
			this.name = name;
			this.mLatitude = latitude;
			this.mLongitude = longitude;
			this.mRadius = radius;
		}

		// Instance field getters
		public String getId() {
			return id;
		}
		
		public String getName() {
			return name;
		}

		public double getLatitude() {
			return mLatitude;
		}

		public double getLongitude() {
			return mLongitude;
		}

		public float getRadius() {
			return mRadius;
		}
		
		// Setters for location name
		public void setName(String name) {
			this.name = name;
		}
}
