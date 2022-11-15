package com.example.cs465project;

import static java.lang.Math.max;
import static java.lang.Math.min;

import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.annotation.SuppressLint;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.KeyEvent;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.cs465project.databinding.ActivityMapsBinding;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener,
        LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private FusedLocationProviderClient fusedLocationClient; //https://developer.android.com/training/location/retrieve-current
    private LatLng currentLatLng;
    private LatLng destinationLatLng;
    private double distanceToDestination = -1.0;  //in meters

    private Button settingsButton;
    private EditText whereToEditText;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;
    private Marker mCurrLocationMarker;

    private TextView timeText;

    private Button shareLocationButton;
    private Button callButton;
    private Button addTimeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        settingsButton = (Button) findViewById(R.id.button_settings);
        whereToEditText = (EditText) findViewById(R.id.edit_text_where_to);
        timeText = (TextView) findViewById(R.id.text_time);
        shareLocationButton = (Button) findViewById(R.id.button_share_location);
        callButton = (Button) findViewById(R.id.button_call);
        addTimeButton = (Button) findViewById(R.id.button_add_time);

        settingsButton.setOnClickListener(this);
        shareLocationButton.setOnClickListener(this);
        callButton.setOnClickListener(this);
        addTimeButton.setOnClickListener(this);




        new CountDownTimer(70 * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                String minutesRemaining = "" + ((millisUntilFinished / 1000) / 60);
                while (minutesRemaining.length() < 2) {
                    minutesRemaining = "0" + minutesRemaining;
                }
                String secondsRemaining = "" + ((millisUntilFinished / 1000) % 60);
                while (secondsRemaining.length() < 2) {
                    secondsRemaining = "0" + secondsRemaining;
                }
                timeText.setText("Countdown to estimated time of arrival: " + minutesRemaining + ":" + secondsRemaining);
            }

            public void onFinish() {
                timeText.setText("Estimated time of arrival has passed");
            }
        }.start();

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

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        }
        else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }
        
        destinationLatLng = new LatLng(40.107778, -88.222778); //TODO remove hardcoding to Krannert Center
        
        new CountDownTimer(Long.MAX_VALUE, 5 * 1000) {
            public void onTick(long millisUntilFinished) {
                updateCurrentLatLngAndDistance();
                redrawMap();
            }

            public void onFinish() {
                timeText.setText("Estimated time of arrival has passed");
            }
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
    public void onConnectionSuspended(int i) {

    }

    public void onCheckboxClicked(View view) {
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
    }

    @Override
    public void onClick(View v) {
        //https://developer.android.com/develop/ui/views/components/dialogs
        if (v.getId() == R.id.button_share_location) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("title")
                    .setMessage("button_share_location");
            AlertDialog dialog = builder.create();
            dialog.show();
        } else if (v.getId() == R.id.button_call) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage("+17033507439", null, "android studio test sms message", null, null); //TODO EDIT
                Toast.makeText(this, "button_call: sms sent", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "button_call: failed to send sms: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        } else if (v.getId() == R.id.button_add_time) {
            Toast.makeText(this, "button_add_time", Toast.LENGTH_SHORT).show();
        } else if (v.getId() == R.id.button_settings) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = getLayoutInflater().inflate(R.layout.setting_dialog, null);
            builder.setView(view)
                    .setCancelable(false)
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    })
            ;
            AlertDialog dialog = builder.create();
            dialog.show();
            dialog.getWindow().setLayout(1000,1200);
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }
        //Place current location marker
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        mCurrLocationMarker = mMap.addMarker(markerOptions);

        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

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
            Address address = addressList.get(0);
            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            mMap.addMarker(new MarkerOptions().position(latLng).title(location));
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            Toast.makeText(getApplicationContext(),address.getLatitude()+" "+address.getLongitude(),Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("MissingPermission")
    private void updateCurrentLatLngAndDistance() {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                            float[] results = new float[1];
                            Location.distanceBetween(currentLatLng.latitude, currentLatLng.longitude, destinationLatLng.latitude, destinationLatLng.longitude, results);
                            distanceToDestination = (double) results[0];
                        }
                    }
                });
    }

    private void redrawMap() {
        if (currentLatLng != null && destinationLatLng != null) {
            mMap.clear();

            mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Current location").icon(BitmapDescriptorFactory.fromResource(R.drawable.current_location))); //https://developers.google.com/maps/documentation/android-sdk/marker //https://commons.wikimedia.org/wiki/File:White_circle_in_blue_background.svg

            mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Destination"));

            LatLng southwestBoundLatLng = new LatLng(min(currentLatLng.latitude, destinationLatLng.latitude), min(currentLatLng.longitude, destinationLatLng.longitude));
            LatLng northeastBoundLatLng = new LatLng(max(currentLatLng.latitude, destinationLatLng.latitude), max(currentLatLng.longitude, destinationLatLng.longitude));
            LatLngBounds mapBound = new LatLngBounds(southwestBoundLatLng, northeastBoundLatLng);
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(mapBound, 200));
        }
    }
}