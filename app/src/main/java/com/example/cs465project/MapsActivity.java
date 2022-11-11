package com.example.cs465project;

import static java.lang.Math.max;
import static java.lang.Math.min;

import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.cs465project.databinding.ActivityMapsBinding;
import com.google.android.gms.tasks.OnSuccessListener;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private FusedLocationProviderClient fusedLocationClient; //https://developer.android.com/training/location/retrieve-current

    private Button settingsButton;
    private EditText whereToEditText;

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
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        settingsButton = (Button) findViewById(R.id.button_settings);
        whereToEditText = (EditText) findViewById(R.id.edit_text_where_to);
        timeText = (TextView) findViewById(R.id.text_time);
        shareLocationButton = (Button) findViewById(R.id.button_share_location);
        callButton = (Button) findViewById(R.id.button_call);
        addTimeButton = (Button) findViewById(R.id.button_add_time);

        settingsButton.setOnClickListener(this);
        //whereToEditText
        //timeText
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

        // Add a marker in Sydney and move the camera

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object

                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Current location"));

                            LatLng destinationLatLng = new LatLng(40.107778, -88.222778); //TODO remove hardcoding to Krannert Center
                            mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Destination"));

                            LatLng southwestBoundLatLng = new LatLng(min(currentLatLng.latitude, destinationLatLng.latitude), min(currentLatLng.longitude, destinationLatLng.longitude));
                            LatLng northeastBoundLatLng = new LatLng(max(currentLatLng.latitude, destinationLatLng.latitude), max(currentLatLng.longitude, destinationLatLng.longitude));
                            LatLngBounds mapBound = new LatLngBounds(southwestBoundLatLng, northeastBoundLatLng);
                            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(mapBound, 200));
                        }
                    }
                });
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
            Toast.makeText(this, "button_call", Toast.LENGTH_SHORT).show();
        } else if (v.getId() == R.id.button_add_time) {
            Toast.makeText(this, "button_add_time", Toast.LENGTH_SHORT).show();
        } else if (v.getId() == R.id.button_settings) {
            Toast.makeText(this, "button_settings", Toast.LENGTH_SHORT).show();
        }
    }
}