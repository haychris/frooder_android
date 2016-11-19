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
        try {
            this.title = obj.getString("title");
            this.body = obj.getString("body");
            this.time = obj.getString("time");
            this.lat = obj.getDouble("lat");
            this.lng = obj.getDouble("lng");
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
