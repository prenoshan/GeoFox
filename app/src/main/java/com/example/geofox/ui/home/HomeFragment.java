package com.example.geofox.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.geofox.MainActivity;
import com.example.geofox.Models.SettingsModel;
import com.example.geofox.Models.TripsModel;
import com.example.geofox.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.geocoding.v5.GeocodingCriteria;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.core.exceptions.ServicesException;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;

public class HomeFragment extends Fragment implements OnMapReadyCallback, PermissionsListener {

    private static final int REQUEST_CODE_AUTOCOMPLETE = 1;
    // variables for adding location layer
    private MapView mapView;
    private MapboxMap mapboxMap;
    // variables for adding location layer
    private PermissionsManager permissionsManager;
    private LocationComponent locationComponent;
    // variables for calculating and drawing a route
    private DirectionsRoute currentRoute;
    private static final String TAG = "DirectionsActivity";
    private NavigationMapRoute navigationMapRoute;
    // variables needed to initialize navigation
    private Button btnStartNavigation;
    private Point destination;
    private DatabaseReference myRef;
    private FirebaseUser user;
    private View root;
    private double distanceTraveled, durationTraveled;
    private TripsModel tripsModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        myRef = FirebaseDatabase.getInstance().getReference();
        user = FirebaseAuth.getInstance().getCurrentUser();

        Mapbox.getInstance(getActivity(), getString(R.string.access_token));

        root = inflater.inflate(R.layout.fragment_home, container, false);

        mapView = root.findViewById(R.id.homeFragment_mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        btnStartNavigation = root.findViewById(R.id.homeFragment_btnStartNavigation);

        if (destination == null) {

            btnStartNavigation.setEnabled(false);
            btnStartNavigation.setBackgroundColor(getActivity().getColor(R.color.mapboxGrayLight));

        }

        tripsModel = new TripsModel();

        return root;
    }

    private void initSearchFab() {
        root.findViewById(R.id.homeFragment_fab_location_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new PlaceAutocomplete.IntentBuilder()
                        .accessToken(Mapbox.getAccessToken() != null ? Mapbox.getAccessToken() : getString(R.string.access_token))
                        .placeOptions(PlaceOptions.builder()
                                .backgroundColor(Color.parseColor("#EEEEEE"))
                                .limit(10)
                                .country("ZA")
                                .build(PlaceOptions.MODE_CARDS))
                        .build(getActivity());
                startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {

            btnStartNavigation.setEnabled(false);
            btnStartNavigation.setBackgroundColor(getActivity().getColor(R.color.mapboxGrayLight));

            final Snackbar drawingRouteSnack = Snackbar.make(getActivity().findViewById(android.R.id.content), "Calculating Route...", Snackbar.LENGTH_INDEFINITE);

            drawingRouteSnack.show();

            Point origin = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                    locationComponent.getLastKnownLocation().getLatitude());

            CarmenFeature selectedCarmenFeature = PlaceAutocomplete.getPlace(data);

            destination = Point.fromLngLat((((Point) selectedCarmenFeature.geometry()).longitude()), ((Point) selectedCarmenFeature.geometry()).latitude());

            myRef.child("users").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    String units, unitsTravel;

                    SettingsModel settingsModel = snapshot.getValue(SettingsModel.class);

                    units = settingsModel.getUnits();
                    unitsTravel = settingsModel.getUnitsTravel();

                    NavigationRoute.builder(getActivity())
                            .accessToken(Mapbox.getAccessToken())
                            .voiceUnits(units)
                            .profile(unitsTravel)
                            .origin(origin)
                            .destination(destination)
                            .build()
                            .getRoute(new Callback<DirectionsResponse>() {
                                @Override
                                public void onResponse(@NotNull Call<DirectionsResponse> call, @NotNull Response<DirectionsResponse> response) {

                                    // You can get the generic HTTP info about the response
                                    Timber.d("Response code: %s", response.code());
                                    if (response.body() == null) {
                                        Timber.e("No routes found, make sure you set the right user and access token.");
                                        return;
                                    } else if (response.body().routes().size() < 1) {
                                        Timber.e("No routes found");
                                        return;
                                    }

                                    currentRoute = response.body().routes().get(0);

                                    distanceTraveled = currentRoute.distance();
                                    durationTraveled = currentRoute.duration();

                                    if (units.equals("metric")) {

                                        distanceTraveled /= 1000;
                                        tripsModel.setTripDistance(String.format("%.2f", distanceTraveled) + " Kms");
                                    } else if (units.equals("imperial")) {

                                        distanceTraveled /= 1609;
                                        tripsModel.setTripDistance(String.format("%.2f", distanceTraveled) + " Miles");

                                    }

                                    calculateTripDuration((int) durationTraveled);

                                    tripsModel.setTripDestination(selectedCarmenFeature.placeName());
                                    setOriginLocation(origin);
                                    tripsModel.setTravelMode(unitsTravel);

                                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                                    Date currentDate = new Date();
                                    tripsModel.setDateRecorded(dateFormat.format(currentDate));

                                    // Draw the route on the map
                                    if (navigationMapRoute != null) {
                                        navigationMapRoute.removeRoute();
                                    } else {
                                        navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                                    }
                                    navigationMapRoute.addRoute(currentRoute);

                                    drawingRouteSnack.dismiss();

                                    GeoJsonSource source = mapboxMap.getStyle().getSourceAs("destination-source-id");
                                    if (source != null) {
                                        source.setGeoJson(Feature.fromGeometry(destination));
                                    }

                                    btnStartNavigation.setEnabled(true);
                                    btnStartNavigation.setBackground(getActivity().getDrawable(R.drawable.auth_button));

                                    Snackbar.make(getActivity().findViewById(android.R.id.content), "Route Calculated, You can begin navigation now", Snackbar.LENGTH_LONG).show();
                                }

                                @Override
                                public void onFailure(@NotNull Call<DirectionsResponse> call, @NotNull Throwable throwable) {
                                    Timber.e("Error: %s", throwable.getMessage());
                                }
                            });


                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                    Timber.e("Error: %s", error.getMessage());

                }
            });

        }

    }

    private void calculateTripDuration(int durationSeconds){

        int hours = durationSeconds / 3600;
        int minutes = (durationSeconds % 3600) / 60;
        int seconds = durationSeconds % 60;

        tripsModel.setTripDuration(hours + "hrs " + minutes + "mins " + seconds + " seconds");

    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;

        mapboxMap.setStyle(getString(R.string.navigation_guidance_day), new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {

                enableLocationComponent(style);

                addDestinationIconSymbolLayer(style);

                initSearchFab();

                btnStartNavigation.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        myRef.child("users").child(user.getUid()).child("tripsTaken").child(UUID.randomUUID().toString()).setValue(tripsModel).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {

                                NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                                        .directionsRoute(currentRoute)
                                        .shouldSimulateRoute(true)
                                        .build();

                                NavigationLauncher.startNavigation(getActivity(), options);

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                                Timber.e("Error inserting trip data: %s", e.getMessage());

                            }
                        });

                    }
                });
            }
        });
    }

    private void setOriginLocation(Point origin) {

        try {
            MapboxGeocoding client = MapboxGeocoding.builder()
                    .accessToken(getString(R.string.access_token))
                    .query(origin)
                    .geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
                    .build();

            client.enqueueCall(new Callback<GeocodingResponse>() {
                @Override
                public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {

                    if (response.body() != null) {
                        List<CarmenFeature> results = response.body().features();
                        if (results.size() > 0) {

                            CarmenFeature feature = results.get(0);
                            tripsModel.setTripOrigin(feature.placeName());

                        }
                    }

                }

                @Override
                public void onFailure(Call<GeocodingResponse> call, Throwable throwable) {
                    Timber.e("Geocoding Failure: %s", throwable.getMessage());
                }
            });
        } catch (ServicesException servicesException) {
            Timber.e("Error geocoding: %s", servicesException.toString());
            servicesException.printStackTrace();
        }

    }

    private void addDestinationIconSymbolLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addImage("destination-icon-id",
                BitmapFactory.decodeResource(this.getResources(), R.drawable.mapbox_marker_icon_default));
        GeoJsonSource geoJsonSource = new GeoJsonSource("destination-source-id");
        loadedMapStyle.addSource(geoJsonSource);
        SymbolLayer destinationSymbolLayer = new SymbolLayer("destination-symbol-layer-id", "destination-source-id");
        destinationSymbolLayer.withProperties(
                iconImage("destination-icon-id"),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
        );
        loadedMapStyle.addLayer(destinationSymbolLayer);
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(getActivity())) {
            // Activate the MapboxMap LocationComponent to show user location
            // Adding in LocationComponentOptions is also an optional parameter
            locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(getActivity(), loadedMapStyle);
            locationComponent.setLocationComponentEnabled(true);
            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(getActivity());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(getActivity(), R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationComponent(mapboxMap.getStyle());
        } else {
            Toast.makeText(getActivity(), R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            getActivity().finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

}