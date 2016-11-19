package neeraj.christopher.frooder;

import static neeraj.christopher.frooder.Constants.ANDROID_BUILDING_ID;
import static neeraj.christopher.frooder.Constants.MY_PERMISSION_ACCESS_COURSE_LOCATION;
import static neeraj.christopher.frooder.Constants.ANDROID_BUILDING_LATITUDE;
import static neeraj.christopher.frooder.Constants.ANDROID_BUILDING_LONGITUDE;
import static neeraj.christopher.frooder.Constants.ANDROID_BUILDING_RADIUS_METERS;
import static neeraj.christopher.frooder.Constants.CONNECTION_FAILURE_RESOLUTION_REQUEST;
import static neeraj.christopher.frooder.Constants.GEOFENCE_EXPIRATION_TIME;
import static neeraj.christopher.frooder.Constants.TAG;
import static neeraj.christopher.frooder.Constants.YERBA_BUENA_ID;
import static neeraj.christopher.frooder.Constants.YERBA_BUENA_LATITUDE;
import static neeraj.christopher.frooder.Constants.YERBA_BUENA_LONGITUDE;
import static neeraj.christopher.frooder.Constants.YERBA_BUENA_RADIUS_METERS;
import static neeraj.christopher.frooder.Constants.DEFAULT_RADIUS;


import android.*;
import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    // Internal List of Geofence objects. In a real app, these might be provided by an API based on
    // locations within the user's proximity.
    List<Geofence> mGeofenceList;

    // These will store hard-coded geofences in this sample app.
    private CustomGeofence mAndroidBuildingGeofence;
    private CustomGeofence mYerbaBuenaGeofence;

    // Persistent storage for geofences.
    private CustomGeofenceStore mGeofenceStorage;

    private LocationServices mLocationService;
    // Stores the PendingIntent used to request geofence monitoring.
    private PendingIntent mGeofenceRequestIntent;
    private GoogleApiClient mApiClient;

    private JSONArray mJsonData;
    private List<FoodPosting> mFoodPostList;

    // Defines the allowable request types (in this example, we only add geofences).
    private enum REQUEST_TYPE {
        ADD
    }

    private REQUEST_TYPE mRequestType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        Log.e(TAG, "Starting up geofencing stuffs.");
        // Rather than displayng this activity, simply display a toast indicating that the geofence
        // service is being created. This should happen in less than a second.
        if (!isGooglePlayServicesAvailable()) {
            Log.e(TAG, "Google Play services unavailable.");
            finish();
            return;
        }

        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mApiClient.connect();

        // Instantiate a new geofence storage area.
        mGeofenceStorage = new CustomGeofenceStore(this);
        // Instantiate the current List of geofences.
        mGeofenceList = new ArrayList<Geofence>();
        mFoodPostList = new ArrayList<FoodPosting>();
//        createGeofences();
        ScheduledExecutorService scheduler =
                Executors.newScheduledThreadPool(1);

        final Context context = this;
        scheduler.schedule(new Runnable() {
            public void run() {refreshData(context);}}, 2, TimeUnit.SECONDS);

    }

    /**
     * In this sample, the geofences are predetermined and are hard-coded here. A real app might
     * dynamically create geofences based on the user's location.
     */
    public void createGeofences() {
        // Create internal "flattened" objects containing the geofence data.
        mAndroidBuildingGeofence = new CustomGeofence(
                ANDROID_BUILDING_ID,                // geofenceId.
                ANDROID_BUILDING_LATITUDE,
                ANDROID_BUILDING_LONGITUDE,
                ANDROID_BUILDING_RADIUS_METERS,
                GEOFENCE_EXPIRATION_TIME,
                Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT
        );
        mYerbaBuenaGeofence = new CustomGeofence(
                YERBA_BUENA_ID,                // geofenceId.
                YERBA_BUENA_LATITUDE,
                YERBA_BUENA_LONGITUDE,
                YERBA_BUENA_RADIUS_METERS,
                GEOFENCE_EXPIRATION_TIME,
                Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT
        );

        // Store these flat versions in SharedPreferences and add them to the geofence list.
        mGeofenceStorage.setGeofence(ANDROID_BUILDING_ID, mAndroidBuildingGeofence);
        mGeofenceStorage.setGeofence(YERBA_BUENA_ID, mYerbaBuenaGeofence);
        mGeofenceList.add(mAndroidBuildingGeofence.toGeofence());
        mGeofenceList.add(mYerbaBuenaGeofence.toGeofence());
    }

    public void refreshData(final Context context) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "http://frooder.herokuapp.com/";

        JsonArrayRequest jsObjRequest = new JsonArrayRequest
                (url, new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        mJsonData = response;
                        mFoodPostList = new ArrayList<>();
                        mGeofenceList = new ArrayList<>();

                        for (int i = 0; i < response.length(); i++) {
                            try {
                                JSONObject curobj = response.getJSONObject(i);
                                FoodPosting post = new FoodPosting(curobj);
                                mFoodPostList.add(post);
                                CustomGeofence geoFence = post.makeGeoFence(""+i,
                                        DEFAULT_RADIUS,
                                        GEOFENCE_EXPIRATION_TIME,
                                        Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT);
                                mGeofenceList.add(geoFence.toGeofence());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                        if (mApiClient.isConnected()) {
                            onConnected(null);
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub

                    }
                });
        //// Add the request to the RequestQueue.
        queue.add(jsObjRequest);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // If the error has a resolution, start a Google Play services activity to resolve it.
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "Exception while resolving connection error.", e);
            }
        } else {
            int errorCode = connectionResult.getErrorCode();
            Log.e(TAG, "Connection to Google Play services failed with error code " + errorCode);
        }
    }

    /**
     * Once the connection is available, send a request to add the Geofences.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.e(TAG, "CONNECTED.");
        // Get the PendingIntent for the geofence monitoring request.
        // Send a request to add the current geofences.
        mGeofenceRequestIntent = getGeofenceTransitionPendingIntent();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions( this, new String[] {  Manifest.permission.ACCESS_FINE_LOCATION  }, MY_PERMISSION_ACCESS_COURSE_LOCATION);
            Log.e(TAG, "NOT PERMITTED LOCATION.");
            return;
        }
        if (mGeofenceList.size() > 0) {
            LocationServices.GeofencingApi.addGeofences(mApiClient, mGeofenceList,
                    mGeofenceRequestIntent);
            Toast.makeText(this, getString(R.string.start_geofence_service), Toast.LENGTH_SHORT).show();
        }
//        finish();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        onConnected(null);
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (null != mGeofenceRequestIntent) {
            LocationServices.GeofencingApi.removeGeofences(mApiClient, mGeofenceRequestIntent);
        }
    }

    /**
     * Checks if Google Play services is available.
     * @return true if it is.
     */
    private boolean isGooglePlayServicesAvailable() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == resultCode) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Google Play services is available.");
            }
            return true;
        } else {
            Log.e(TAG, "Google Play services is unavailable.");
            return false;
        }
    }

    /**
     * Create a PendingIntent that triggers GeofenceTransitionIntentService when a geofence
     * transition occurs.
     */
    private PendingIntent getGeofenceTransitionPendingIntent() {
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));

            final TextView mTextView = (TextView) rootView.findViewById(R.id.section_label);

            // Instantiate the RequestQueue.
            RequestQueue queue = Volley.newRequestQueue(rootView.getContext());
            String url ="http://www.google.com";
            String url2 = "http://frooder.herokuapp.com/";
            // Request a string response from the provided URL.
//            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
//                    new Response.Listener<String>() {
//                        @Override
//                        public void onResponse(String response) {
//                            // Display the first 500 characters of the response string.
//                            mTextView.setText("Response is: "+ response.substring(0,500));
//                        }
//                    }, new Response.ErrorListener() {
//                @Override
//                public void onErrorResponse(VolleyError error) {
//                    mTextView.setText("That didn't work!");
//                }
//            });

            final ViewGroup finalContainer = container;

            JsonArrayRequest jsObjRequest = new JsonArrayRequest
                    (url2, new Response.Listener<JSONArray>() {

                        @Override
                        public void onResponse(JSONArray response) {
                            mTextView.setText("Response: " + response.toString());
                            NotificationCompat.Builder mBuilder =
                                    new NotificationCompat.Builder(finalContainer.getContext())
                                            .setSmallIcon(R.drawable.notification_icon)
                                            .setContentTitle("My notification")
                                            .setContentText("Hello World!");
                            // Creates an explicit intent for an Activity in your app
                            Intent resultIntent = new Intent(finalContainer.getContext(), MainActivity.class);

                            // The stack builder object will contain an artificial back stack for the
                            // started Activity.
                            // This ensures that navigating backward from the Activity leads out of
                            // your application to the Home screen.
                            TaskStackBuilder stackBuilder = TaskStackBuilder.create(finalContainer.getContext());
                            // Adds the back stack for the Intent (but not the Intent itself)
                            stackBuilder.addParentStack(MainActivity.class);
                            // Adds the Intent that starts the Activity to the top of the stack
                            stackBuilder.addNextIntent(resultIntent);
                            PendingIntent resultPendingIntent =
                                    stackBuilder.getPendingIntent(
                                            0,
                                            PendingIntent.FLAG_UPDATE_CURRENT
                                    );
                            mBuilder.setContentIntent(resultPendingIntent);
                            NotificationManager mNotificationManager =
                                    (NotificationManager) finalContainer.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
                            // mId allows you to update the notification later on.
                            int mId = 4;
                            mNotificationManager.notify(mId, mBuilder.build());
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // TODO Auto-generated method stub

                        }
                    });
            //// Add the request to the RequestQueue.
//            queue.add(stringRequest);
            queue.add(jsObjRequest);
            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "SECTION 1";
                case 1:
                    return "SECTION 2";
                case 2:
                    return "SECTION 3";
            }
            return null;
        }
    }
}
