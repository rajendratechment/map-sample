package com.rajendra.map_sample;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.navigation.ListenableResultFuture;
import com.google.android.libraries.navigation.NavigationApi;
import com.google.android.libraries.navigation.Navigator;
import com.google.android.libraries.navigation.RoutingOptions;
import com.google.android.libraries.navigation.SimulationOptions;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import com.google.android.libraries.navigation.Waypoint;
import com.rajendra.map_sample.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity {

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2424;
    private static final String SYDNEY_OPERA_HOUSE = "ChIJK22rPvflYjkRhY3I_rzSkF4HXCH+XQV,";
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private boolean mLocationPermissionGranted = false;
    private Navigator mNavigator;
    SupportNavigationFragment mNavFragment;
    RoutingOptions mRoutingOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mNavFragment = (SupportNavigationFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        checkPermissionLocal();

        NavigationApi.getNavigator(this, new NavigationApi.NavigatorListener() {
            /**
             * Sets up the navigation UI when the navigator is ready for use.
             */
            @Override
            public void onNavigatorReady(Navigator navigator) {
                displayMessage("Navigator ready.");
                mNavigator = navigator;


                // Optional. Disable the guidance notifications and shut down the app
                // and background service when the user closes the app.
                // mNavigator.setTaskRemovedBehavior(Navigator.TaskRemovedBehavior.QUIT_SERVICE)

                // Optional. Set the last digit of the car's license plate to get
                // route restrictions for supported countries.
                // mNavigator.setLicensePlateRestrictionInfo(getLastDigit(), "BZ");

                // Set the camera to follow the device location with 'TILTED' driving view.
//                mNavFragment.getCamera().followMyLocation(Camera.Perspective.TILTED);
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                mNavFragment.getMapAsync(googleMap -> googleMap.followMyLocation(GoogleMap.CameraPerspective.TILTED));

                // Set the travel mode (DRIVING, WALKING, CYCLING, TWO_WHEELER, or TAXI).
                mRoutingOptions = new RoutingOptions();
                mRoutingOptions.travelMode(RoutingOptions.TravelMode.TAXI);


                // Navigate to a place, specified by Place ID.
                navigateToPlace(SYDNEY_OPERA_HOUSE, mRoutingOptions);
            }

            /**
             * Handles errors from the Navigation SDK.
             * @param errorCode The error code returned by the navigator.
             */
            @Override
            public void onError(@NavigationApi.ErrorCode int errorCode) {
                switch (errorCode) {
                    case NavigationApi.ErrorCode.NOT_AUTHORIZED:
                        displayMessage("Error loading Navigation SDK: Your API key is "
                                + "invalid or not authorized to use the Navigation SDK.");
                        break;
                    case NavigationApi.ErrorCode.TERMS_NOT_ACCEPTED:
                        displayMessage("Error loading Navigation SDK: User did not accept "
                                + "the Navigation Terms of Use.");
                        break;
                    case NavigationApi.ErrorCode.NETWORK_ERROR:
                        displayMessage("Error loading Navigation SDK: Network error.");
                        break;
                    case NavigationApi.ErrorCode.LOCATION_PERMISSION_MISSING:
                        displayMessage("Error loading Navigation SDK: Location permission "
                                + "is missing.");
                        break;
                    default:
                        displayMessage("Error loading Navigation SDK: " + errorCode);
                }
            }
        });


    }

    private void navigateToPlace(String placeId, RoutingOptions travelMode) {
        Waypoint destination;
        try {
            destination = Waypoint.builder().setPlaceIdString(placeId).build();
        } catch (Waypoint.UnsupportedPlaceIdException e) {
            displayMessage("Error starting navigation: Place ID is not supported.");
            return;
        }

        // Create a future to await the result of the asynchronous navigator task.
        ListenableResultFuture<Navigator.RouteStatus> pendingRoute =
                mNavigator.setDestination(destination, travelMode);

        // Define the action to perform when the SDK has determined the route.
        pendingRoute.setOnResultListener(
                new ListenableResultFuture.OnResultListener<Navigator.RouteStatus>() {
                    @Override
                    public void onResult(Navigator.RouteStatus code) {
                        switch (code) {
                            case OK:
                                // Hide the toolbar to maximize the navigation UI.
                                if (getActionBar() != null) {
                                    getActionBar().hide();
                                }

                                // Enable voice audio guidance (through the device speaker).
                                mNavigator.setAudioGuidance(
                                        Navigator.AudioGuidance.VOICE_ALERTS_AND_GUIDANCE);

                                // Simulate vehicle progress along the route for demo/debug builds.
                                if (BuildConfig.DEBUG) {
                                    mNavigator.getSimulator().simulateLocationsAlongExistingRoute(
                                            new SimulationOptions().speedMultiplier(5));
                                }

                                // Start turn-by-turn guidance along the current route.
                                mNavigator.startGuidance();
                                break;
                            // Handle error conditions returned by the navigator.
                            case NO_ROUTE_FOUND:
                                displayMessage("Error starting navigation: No route found.");
                                break;
                            case NETWORK_ERROR:
                                displayMessage("Error starting navigation: Network error.");
                                break;
                            case ROUTE_CANCELED:
                                displayMessage("Error starting navigation: Route canceled.");
                                break;
                            default:
                                displayMessage("Error starting navigation: "
                                        + String.valueOf(code));
                        }
                    }
                });
    }



    private void checkPermissionLocal() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] { android.Manifest.permission.ACCESS_FINE_LOCATION },
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        if (!mLocationPermissionGranted) {
            displayMessage("Error loading Navigation SDK: "
                    + "The user has not granted location permission.");
            return;
        }
    }

    private void displayMessage(String msg) {
        Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_LONG).show();
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


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is canceled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                } else {

                }
            }
        }
    }






























}