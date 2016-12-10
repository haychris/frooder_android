package neeraj.christopher.frooder;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by christopher on 9/18/16.
 */
public class FoodPosting {
    private String title;
    private String body;
    private String time;
    private double lat;
    private double lng;

    FoodPosting(JSONObject obj) {
        this.title = obj.optString("title", "");
        this.body = obj.optString("body", "");
        this.time = obj.optString("time", "");
        this.lat = obj.optDouble("lat", 0.0);
        this.lng = obj.optDouble("lng", 0.0);
    }

    FoodPosting(String title, String body, String time, double lat, double lng) {
        this.title = title;
        this.body = body;
        this.time = time;
        this.lat = lat;
        this.lng = lng;
    }

    public CustomGeofence makeGeoFence(String geofenceId, float radius, long expiration, int transition) {
        return new CustomGeofence(geofenceId, lat, lng, radius, expiration, transition);
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getTime() {
        return time;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

}
