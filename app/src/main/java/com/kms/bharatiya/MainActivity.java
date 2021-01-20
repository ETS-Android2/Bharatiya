package com.kms.bharatiya;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.JsonElement;
import com.mapbox.android.gestures.AndroidGesturesManager;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.GeoJson;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;

//Additional imports for Markers:
//import com.mapbox.mapboxandroiddemo.R; not found
//imports for Firestore database:
//Java imports:


public class MainActivity extends AppCompatActivity {
    private String curstl= "mapbox://styles/thesalmansahel/ckh50xh0w00se19pfe4krhrxa";
    private MapView mapView;
    private MapboxMap mapboxMap;
    public FloatingActionButton ft;
    //For icons of map markers
    private static final String SOURCE_ID = "SOURCE_ID";
    private static final String ICON_ID = "ICON_ID";
    private static final String LAYER_ID = "LAYER_ID";
    private List<Feature> HouseList = new ArrayList<>();    //Contains list of houses where markers need to be placed.
    private FirebaseFirestore db = FirebaseFirestore.getInstance(); //Contains Firestore database.

    private boolean placedHousemarker=false;


    public GeoPoint cursor;    //Firestore stores location as geopoint.
    //just set the location to cursor and you're done.

    @Override
    protected void onStart() {

        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_main);
        ft = findViewById(R.id.floatingActionButton);
        ft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!placedHousemarker){
                    Toast.makeText(MainActivity.this,"Please place Marker on map first.", Toast.LENGTH_LONG).show();
                }
                else {
                    Intent i = new Intent(MainActivity.this, RegistrationV2.class);
                    startActivity(i);
                }
            }
        });



        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {

            @Override
            public void onMapReady(@NonNull MapboxMap mapboxMap) {
                MainActivity.this.mapboxMap=mapboxMap;
                loadFromDatabase();
                addListeners();
            }

            private void loadFromDatabase(){
                db.collection("Houses")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if(task.isSuccessful()){
                                for(QueryDocumentSnapshot doc:task.getResult()){

                                    GeoPoint cur= doc.getGeoPoint("LatLong");
                                    Log.d("LATLONG",cur.getLongitude() + " " + cur.getLatitude());

                                    Feature ft=Feature.fromGeometry(Point.fromLngLat(cur.getLongitude(),cur.getLatitude()));
                                    //Adding Features here:
                                    Log.d("FEATX",ft.properties().toString());
                                    ft.properties().addProperty("Area",(Number)doc.getData().get("Area"));
                                    ft.properties().addProperty("Rent",(Number)doc.getData().get("Rent"));
                                    ft.properties().addProperty("Number of Rooms",(Number)doc.getData().get("NumberOfRooms"));

                                    //private property to identify unique user that posted it for notification system:
                                    //ft.properties().addProperty("userid",(String)doc.getData().get("userID"));
                                    HouseList.add(ft);
                                }
                                mapboxMap.setStyle(new Style.Builder().fromUri(curstl)
                                    //Note: Icons are added on a separate layer, as an overlay above the map.
                                    .withImage(ICON_ID, BitmapFactory.decodeResource( MainActivity.this.getResources(), R.drawable.mapbox_marker_icon_default))

                                    //Place the marker icons above the houses for rent, GeoJson translates lat long into map coordinates.
                                    .withSource(new GeoJsonSource(SOURCE_ID, FeatureCollection.fromFeatures(HouseList)))

                                    //Creating the actual Layer as overlay, and adding an offset to the icon so the marker arrow points at the house.

                                    .withLayer(new SymbolLayer(LAYER_ID, SOURCE_ID)
                                    .withProperties(iconImage(ICON_ID), iconAllowOverlap(true), iconIgnorePlacement(true))),

                                        new Style.OnStyleLoaded() {
                                            @Override
                                            public void onStyleLoaded(@NonNull Style style) {

                                            }
                                        }
                                    );
                                }
                                else{
                                    Log.d("Dbgeterror", String.valueOf(task.getException()));
                                }
                            }
                        });
            }

            private boolean handleClickIcon(PointF screenPoint) {
                List<Feature> features = mapboxMap.queryRenderedFeatures(screenPoint);
                if (!features.isEmpty()) {
                    Log.d("ONCLICK","marker clicked");
                    Feature feature = features.get(0);

                    if(feature.properties().get("Rent")!=null){ //Clicked on a valid marker
                        Toast.makeText(MainActivity.this,feature.properties().toString(), Toast.LENGTH_LONG).show();
                    }
                    else{
                        //place a new marker. set housemarker to true.
                    }


                } else {
                    Log.d("ONCLICK","map clicked");
                }
                return true;
            }

            private void addListeners(){

                mapboxMap.addOnMapClickListener(new MapboxMap.OnMapClickListener() {    //Adds map click event listener.
                    @Override
                    public boolean onMapClick(@NonNull LatLng point) {
                        return handleClickIcon(mapboxMap.getProjection().toScreenLocation(point));
                    }
                });

            }

        });



        }
}