package neeraj.christopher.frooder;

import android.*;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
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

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import neeraj.christopher.frooder.dummy.DummyContent;

import static neeraj.christopher.frooder.Constants.CONNECTION_FAILURE_RESOLUTION_REQUEST;
import static neeraj.christopher.frooder.Constants.DEFAULT_RADIUS;
import static neeraj.christopher.frooder.Constants.GEOFENCE_EXPIRATION_TIME;
import static neeraj.christopher.frooder.Constants.MY_PERMISSION_ACCESS_COURSE_LOCATION;
import static neeraj.christopher.frooder.Constants.TAG;

public class MainTabbedActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, FoodPostingFragment.OnListFragmentInteractionListener, OnMapReadyCallback {

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

    public List<FoodPosting> getFoods() {
        return mFoodPostList;
    }

    @Override
    public void onListFragmentInteraction(FoodPosting item) {
        return;
    }

    // Defines the allowable request types (in this example, we only add geofences).
    private enum REQUEST_TYPE {
        ADD
    }

    private MainTabbedActivity.REQUEST_TYPE mRequestType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_tabbed);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });


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

    public void refreshData(final Context context) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(context);
//        String url = "http://frooder.herokuapp.com/";
        String url = "http://frooder.herokuapp.com/1612"; //TODO: only for testing, change back to default
        Log.d(TAG, "Refreshing food data");

        JsonArrayRequest jsObjRequest = new JsonArrayRequest
                (url, new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        Log.d(TAG, "Food data response: " + response);
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
                        updateListFragmentData();
                        updateMapFragmentData();
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

    private void updateListFragmentData() {
//        Fragment frag = this.getSupportFragmentManager().findFragmentByTag("food_posting_list_tag");
//        if (frag instanceof FoodPostingFragment) {
//            Log.e(TAG, "FOUND FRAGMENT");
//        } else {
//            Log.e(TAG, "DID NOT FIND FRAGMENT");
//        }
//
//        frag = this.getSupportFragmentManager().findFragmentById(R.id.foodposting_list);
//        if (frag instanceof FoodPostingFragment) {
//            Log.e(TAG, "FOUND FRAGMENT");
//        } else {
//            Log.e(TAG, "DID NOT FIND FRAGMENT");
//        }
//        frag = mSectionsPagerAdapter.mFoodPostingFragment;
//        if (frag instanceof FoodPostingFragment) {
//            Log.e(TAG, "FOUND FRAGMENT");
//        } else {
//            Log.e(TAG, "DID NOT FIND FRAGMENT");
//        }
        MyFoodPostingRecyclerViewAdapter adapter = mSectionsPagerAdapter.mFoodPostingFragment.getListAdapter();
        adapter.resetList(mFoodPostList);
    }

    private void updateMapFragmentData() {
        mSectionsPagerAdapter.mMapFragment.getMapAsync(this);
//        mSectionsPagerAdapter.mMapFragment.getTag();
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
            ActivityCompat.requestPermissions( this, new String[] {  android.Manifest.permission.ACCESS_FINE_LOCATION  }, MY_PERMISSION_ACCESS_COURSE_LOCATION);
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
    public void onMapReady(GoogleMap googleMap) {
        LatLng minLatLng = new LatLng(40.346577, -74.654717); // default to princeton
        LatLng maxLatLng = minLatLng;
        for (FoodPosting food : mFoodPostList) {
            if (food.getLat() != 0.0) {
                googleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(food.getLat(), food.getLng()))
                        .title(food.getTitle()));

                // update min/max latitudes
                if (food.getLat() < minLatLng.latitude) {
                    minLatLng = new LatLng(food.getLat(), minLatLng.longitude);
                } else if (food.getLat() > maxLatLng.latitude) {
                    maxLatLng = new LatLng(food.getLat(), maxLatLng.longitude);
                }

                // update min/max longitudes
                if (food.getLng() < minLatLng.longitude) {
                    minLatLng = new LatLng(minLatLng.latitude, food.getLng());
                } else if (food.getLng() > maxLatLng.longitude) {
                    maxLatLng = new LatLng(maxLatLng.latitude, food.getLng());
                }
            }
        }
        LatLngBounds bounds = new LatLngBounds(minLatLng, maxLatLng);
        int mapPadding = 10;
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, mapPadding));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_tabbed, menu);
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
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

//    /**
//     * A placeholder fragment containing a simple view.
//     */
//    public static class PlaceholderFragment extends Fragment {
//        /**
//         * The fragment argument representing the section number for this
//         * fragment.
//         */
//        private static final String ARG_SECTION_NUMBER = "section_number";
//
//        public PlaceholderFragment() {
//        }
//
//        /**
//         * Returns a new instance of this fragment for the given section
//         * number.
//         */
//        public static PlaceholderFragment newInstance(int sectionNumber) {
//            PlaceholderFragment fragment = new PlaceholderFragment();
//            Bundle args = new Bundle();
//            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
//            fragment.setArguments(args);
//            return fragment;
//        }
//
//        @Override
//        public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                                 Bundle savedInstanceState) {
//            View rootView = inflater.inflate(R.layout.fragment_main_tabbed, container, false);
//            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
//            textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));
//            return rootView;
//        }
//    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        public FoodPostingFragment mFoodPostingFragment;
        public SupportMapFragment mMapFragment;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            mFoodPostingFragment = FoodPostingFragment.newInstance(1);
            mMapFragment = SupportMapFragment.newInstance();
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
//            return PlaceholderFragment.newInstance(position + 1);
            switch (position) {
                case 0:
                    return mFoodPostingFragment;
                case 1:
                    return mMapFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Food Postings";
                case 1:
                    return "Food Map";
            }
            return null;
        }
    }
}
