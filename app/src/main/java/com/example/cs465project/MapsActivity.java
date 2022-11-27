package com.example.cs465project;

import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.annotation.SuppressLint;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.view.KeyEvent;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.cs465project.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener,
        LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    ArrayList markerPoints= new ArrayList();


    private ImageButton settingsButton;
    private EditText whereToEditText;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private LatLng currentLatLng;
    private LatLng destinationLatLng;
    private double distanceToDestination = 50000000; //in meters
    private double distanceToDestinationThreshold = 30; //in meters
    private LinearLayout.LayoutParams params;
    private int copyOfWidth = 0;

    private TextView timeText;

    private LinearLayout bottomLinearLayout;
    private LinearLayout topLinearLayout;
    private Button shareLocationButton;
    private Button callButton;
    private Button addTimeButton;

    private CountDownTimer countdownTimer;
    private long msUntilFinished = 15 * 60 * 1000; //milliseconds

    private CountDownTimer locationTimer;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        settingsButton = (ImageButton) findViewById(R.id.button_settings);
        whereToEditText = (EditText) findViewById(R.id.edit_text_where_to);
        timeText = (TextView) findViewById(R.id.text_time);
        shareLocationButton = (Button) findViewById(R.id.button_share_location);
        callButton = (Button) findViewById(R.id.button_call);
        addTimeButton = (Button) findViewById(R.id.button_add_time);

        bottomLinearLayout = (LinearLayout) findViewById(R.id.bottomLinearLayout);
        topLinearLayout = (LinearLayout) findViewById(R.id.top_linear_layout);
        settingsButton.setOnClickListener(this);
        shareLocationButton.setOnClickListener(this);
        callButton.setOnClickListener(this);
        addTimeButton.setOnClickListener(this);

        whereToEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Perform action on key press
                    searchLocation(findViewById(R.id.edit_text_where_to));
                    return true;
                }
                return false;
            }
        });

        //currentLatLng = new LatLng(40.1092, -88.2271); //next to Illini Union //TODO remove
        currentLatLng = new LatLng(40.1125, -88.2269); //Grainger Engineering Library //TODO remove
        destinationLatLng = new LatLng(40.1092, -88.2271); //next to Illini Union
        setInitialUI();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        ActivityCompat.requestPermissions(this, new String[] {
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION },
                1);
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);


        fetchCurrentLocation();
        checkIfNearDestination();

        mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Marker in gg"));
        mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Marker in union"));

        // Getting URL to the Google Directions API
        String url = getDirectionsUrl(currentLatLng, destinationLatLng);

        DownloadTask downloadTask = new DownloadTask();

        // Start downloading json data from Google Directions API
        downloadTask.execute(url);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16));


        locationTimer = new CountDownTimer(Long.MAX_VALUE, 5000) {
            public void onTick(long millisUntilFinished) {
                fetchCurrentLocation();
                checkIfNearDestination();
            }

            public void onFinish() {}
        }.start();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        mGoogleApiClient.connect();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {}

    public void onCheckboxClicked(View view) {
        /*
        if (view.getId() == R.id.contact1) {
            final CheckBox checkBox = (CheckBox) findViewById(R.id.contact1);
            if (checkBox.isChecked()) {
                checkBox.setChecked(false);
            } else {
                checkBox.setChecked(true);
            }
        } else if (view.getId() == R.id.contact2) {
            final CheckBox checkBox = (CheckBox) findViewById(R.id.contact2);
            if (checkBox.isChecked()) {
                checkBox.setChecked(false);
            } else {
                checkBox.setChecked(true);
            }
        } else if (view.getId() == R.id.contact3) {
            final CheckBox checkBox = (CheckBox) findViewById(R.id.contact3);
            if (checkBox.isChecked()) {
                checkBox.setChecked(false);
            } else {
                checkBox.setChecked(true);
            }
        }
        */
    }

    @Override
    public void onClick(View v) {
        //https://developer.android.com/develop/ui/views/components/dialogs
        if (v.getId() == R.id.button_share_location) {
            Log.d(null, "asdf: share location button");
            try {
                SmsManager smsManager = SmsManager.getDefault();
                String smsText = "Akita app user has shared their location with you: (Latitude, Longitude) = (" + currentLatLng.latitude + ", " + currentLatLng.longitude + ")";
                smsManager.sendTextMessage("+17033507439", null, smsText, null, null); //TODO EDIT
                Toast.makeText(this, "Text message sent: \"" + smsText + "\"", Toast.LENGTH_LONG).show();
                Log.d(null, "asdf: " + smsText);
            } catch (Exception e) {
                Toast.makeText(this, "error (failed to send sms): " + e, Toast.LENGTH_LONG).show();
                Log.d(null, "asdf: error (failed to send sms): " + e);
            }
        } else if (v.getId() == R.id.button_call) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = getLayoutInflater().inflate(R.layout.call_emergency, null);
            builder.setView(view);
            AlertDialog dialog = builder.create();
            dialog.show();
            //dialog.getWindow().setLayout(1000,1200);
        } else if (v.getId() == R.id.button_add_time) {
            msUntilFinished += 60 * 1000;

            if ((msUntilFinished / 1000) / 60 > 1) {
                timeText.setTextColor(Color.rgb(255,255,255));
            }

            setCountdownTimer();
            Toast.makeText(this, "1 minute added to countdown timer", Toast.LENGTH_SHORT).show();
        } else if (v.getId() == R.id.button_settings) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            //View view = getLayoutInflater().inflate(R.layout.setting_dialog, null);
            builder//.setView(view)
                    .setTitle("Notification Settings")
                    .setNeutralButton("Add Contact", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //TODO
                        }
                    })
                    .setNegativeButton("Change Notification Settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            createNotificationSettingsDialog();
                        }
                    })
                    .setPositiveButton("End Route", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //TODO
                        }
                    })
            ;
            AlertDialog dialog = builder.create();
            dialog.show();
            //dialog.getWindow().setLayout(1000,1200);
        }
    }

    @Override
    public void onLocationChanged(Location location) { //This function does not actually appear to work on physical devices
        /*
        //mLastLocation = location;
        //if (mCurrLocationMarker != null) {
        //    mCurrLocationMarker.remove();
        //}
        //Place current location marker

        //currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        //calculateDistance();
        checkIfNearDestination();

        redrawMap();

        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
        */
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {}

    public void searchLocation(View view) {
        EditText locationSearch = (EditText) findViewById(R.id.edit_text_where_to);
        String location = locationSearch.getText().toString();
        List<Address> addressList = null;

        if (location != null || !location.equals("")) {
            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(location, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (addressList.size() <= 0) { //error handling
                Toast.makeText(getApplicationContext(),"Location not found",Toast.LENGTH_LONG).show();
                return;
            }

            Address address = addressList.get(0);
            destinationLatLng = new LatLng(address.getLatitude(), address.getLongitude());
            whereToEditText.setVisibility(View.GONE);

            params = (LinearLayout.LayoutParams) topLinearLayout.getLayoutParams();
            copyOfWidth = params.width;
            params.width = 170;
            params.gravity = Gravity.START;

            // Keep the margin left for the menu when there isn't a search bar
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            params.leftMargin = (displayMetrics.widthPixels - (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 370, this.getResources().getDisplayMetrics())) / 2;
            topLinearLayout.setLayoutParams(params);

            //mMap.addMarker(new MarkerOptions().position(latLng).title(location));
            //mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            //Toast.makeText(getApplicationContext(),address.getLatitude()+" "+address.getLongitude(),Toast.LENGTH_LONG).show();

            fetchCurrentLocation();
            checkIfNearDestination();

            MarkerOptions markerOptionsDestination = new MarkerOptions();
            markerOptionsDestination.position(destinationLatLng);
            markerOptionsDestination.title("Destination");
            mMap.addMarker(markerOptionsDestination);

            LatLng southwestBoundLatLng = new LatLng(Math.min(currentLatLng.latitude, destinationLatLng.latitude), Math.min(currentLatLng.longitude, destinationLatLng.longitude));
            LatLng northeastBoundLatLng = new LatLng(Math.max(currentLatLng.latitude, destinationLatLng.latitude), Math.max(currentLatLng.longitude, destinationLatLng.longitude));
            LatLngBounds mapBound = new LatLngBounds(southwestBoundLatLng, northeastBoundLatLng);
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(mapBound, 400));

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View selectContactView = getLayoutInflater().inflate(R.layout.select_contact, null);
            builder.setView(selectContactView)
                    .setCancelable(false)
                    .setNegativeButton("Let's Go!", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    })
            ;
            AlertDialog dialog = builder.create();
            dialog.show();
            //dialog.getWindow().setLayout(1000,1200);

            setCountdownTimer();

            fetchCurrentLocation();
            checkIfNearDestination();

            setTravelUI();
        }
    }

    private void setCountdownTimer() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
        }
        countdownTimer = new CountDownTimer(msUntilFinished, 1000) {
            public void onTick(long millisUntilFinished) {
                msUntilFinished = millisUntilFinished;

                String minutesRemaining = "" + ((millisUntilFinished / 1000) / 60);
                while (minutesRemaining.length() < 2) {
                    minutesRemaining = "0" + minutesRemaining;
                }
                String secondsRemaining = "" + ((millisUntilFinished / 1000) % 60);
                while (secondsRemaining.length() < 2) {
                    secondsRemaining = "0" + secondsRemaining;
                }
                timeText.setText(minutesRemaining + ":" + secondsRemaining);

                // Turn counter to red if the timer is under 2 minutes
                if ((millisUntilFinished / 1000) / 60 <= 1) {
                    timeText.setTextColor(Color.rgb(200,0,0));
                }
            }

            public void onFinish() {
                timeText.setText("Time's Up!");
                createRunOutOfTimeAlert();
                timeText.setTextColor(Color.rgb(255,255,255));
                whereToEditText.setVisibility(View.VISIBLE);
                params.width = copyOfWidth;
                params.gravity = Gravity.CENTER_VERTICAL;
                topLinearLayout.setLayoutParams(params);
                timeText.setVisibility(TextView.GONE);
                bottomLinearLayout.setVisibility(LinearLayout.GONE);

                // Clear all markers on map
                mMap.clear();
            }
        }.start();
    }

    private void createRunOutOfTimeAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View selectContactView = getLayoutInflater().inflate(R.layout.run_outof_time, null);
        builder.setView(selectContactView);
        AlertDialog dialog = builder.create();
        dialog.show();
        //dialog.getWindow().setLayout(1000,2000);
    }

    private void createNotificationSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View selectContactView = getLayoutInflater().inflate(R.layout.notification_settings, null);
        builder.setView(selectContactView);
        AlertDialog dialog = builder.create();
        dialog.show();
        //dialog.getWindow().setLayout(1000,1600);
    }

    private void calculateDistance() {
        try {
            float[] distances = new float[1];
            Location.distanceBetween(currentLatLng.latitude, currentLatLng.longitude, destinationLatLng.latitude, destinationLatLng.longitude, distances);
            distanceToDestination = (double) distances[0];
        } catch (Exception e) {
            //TODO
        }
    }

    private void checkIfNearDestination() {
        calculateDistance();
        Log.d(null, "asdf: dist = " + distanceToDestination);
        if (distanceToDestination < distanceToDestinationThreshold) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View selectContactView = getLayoutInflater().inflate(R.layout.success_arrival, null);
            builder.setView(selectContactView);
            AlertDialog dialog = builder.create();
            dialog.show();
            //dialog.getWindow().setLayout(1000,1600);

            setInitialUI();

            if (countdownTimer != null) {
                countdownTimer.cancel();
            }
            if (locationTimer != null) {
                locationTimer.cancel();
            }

            mMap.clear();
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchCurrentLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        }
                    }
                });
    }

    private void setInitialUI() {
        whereToEditText.setVisibility(EditText.VISIBLE);
        whereToEditText.setText("");
        timeText.setVisibility(TextView.GONE);
        bottomLinearLayout.setVisibility(LinearLayout.GONE);
    }

    private void setTravelUI() {
        whereToEditText.setVisibility(EditText.INVISIBLE);
        whereToEditText.setText("");
        timeText.setVisibility(TextView.VISIBLE);
        bottomLinearLayout.setVisibility(LinearLayout.VISIBLE);
    }

    private class DownloadTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... url) {

            String data = "";

            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();


            parserTask.execute(result);

        }
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = new ArrayList<LatLng>();;
            PolylineOptions lineOptions = new PolylineOptions();;
            LatLng position = null;
            MarkerOptions markerOptions = new MarkerOptions();
            // Traversing through all the routes
            for(int i=0;i<result.size();i++){
                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);
                // Fetching all the points in i-th route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    position = new LatLng(lat, lng);
                    points.add(position);
                }
                // Adding all the points in the route to LineOptions
                mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(10));
                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.RED);
                lineOptions.geodesic(true);
            }
            // Drawing polyline in the Google Map for the i-th route
            if(points.size()!=0)mMap.addPolyline(lineOptions);
            else Log.d("myTag", "no point found");
        }
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";
        String mode = "mode=driving";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode;

        // Output format
        String format = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + format + "?" + parameters + "&key=AIzaSyD6qmaecfx5rCfxQ08Dnm4jz-QZi1L_4NE";


        return url;
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.connect();

            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }
}