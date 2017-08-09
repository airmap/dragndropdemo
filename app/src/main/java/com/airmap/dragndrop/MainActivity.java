package com.airmap.dragndrop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerView;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";

    private MapView mapView;
    private MapboxMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mapView = (MapView) findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    protected void onStart() {
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
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        map = mapboxMap;

        LatLng santaMonicaLatLng = new LatLng(34.0195, -118.4912);
        map.addMarker(getMarker(santaMonicaLatLng));
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(santaMonicaLatLng, 11));

        setupMapDragging();
    }

    private void setupMapDragging() {
        final float screenDensity = getResources().getDisplayMetrics().density;

        mapView.setOnTouchListener(new View.OnTouchListener() {
            //This onTouch code is a copy of the AnnotationManager#onTap code, except
            //I'm dragging instead of clicking, and it's being called for every touch event rather than just a tap
            //This code also makes some simplifications to the selection logic

            //If dragging ever stops working, this is the first place to look
            //The onTouch is based on AnnotationManager#onTap
            //Look for any changes in that function, and make those changes here too
            //Also need to look at AnnotationManager#getMarkersInRect, which is how I'm getting close-by markers right now
            //It might end up getting renamed, something about it may change, which won't be apparent since right now it uses reflection to be invoked

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event != null) {
                    if (event.getPointerCount() > 1) {
                        return false; //Don't drag if there are multiple fingers on screen
                    }

                    PointF tapPoint = new PointF(event.getX(), event.getY());
                    float toleranceSides = 4 * screenDensity;
                    float toleranceTopBottom = 10 * screenDensity;
                    float averageIconWidth = 42;
                    float averageIconHeight = 42;
                    RectF tapRect = new RectF(tapPoint.x - averageIconWidth / 2 - toleranceSides,
                            tapPoint.y - averageIconHeight / 2 - toleranceTopBottom,
                            tapPoint.x + averageIconWidth / 2 + toleranceSides,
                            tapPoint.y + averageIconHeight / 2 + toleranceTopBottom);

                    Marker newSelectedMarker = null;
                    List<MarkerView> nearbyMarkers = map.getMarkerViewsInRect(tapRect);
                    List<Marker> selectedMarkers = map.getSelectedMarkers();

                    if (selectedMarkers.isEmpty() && !nearbyMarkers.isEmpty()) {
                        Collections.sort(nearbyMarkers);
                        for (Marker marker : nearbyMarkers) {
                            if (marker instanceof MarkerView && !((MarkerView) marker).isVisible()) {
                                continue; //Don't let user click on hidden midpoints
                            }

                            newSelectedMarker = marker;
                            break;
                        }
                    } else if (!selectedMarkers.isEmpty()) {
                        newSelectedMarker = selectedMarkers.get(0);
                    }

                    if (newSelectedMarker != null && newSelectedMarker instanceof MarkerView) {
                        boolean doneDragging = event.getAction() == MotionEvent.ACTION_UP;

                        // Drag! Trying to put most logic in the drag() function
                        map.selectMarker(newSelectedMarker); //Use the marker selection state to prevent selecting another marker when dragging over it
                        newSelectedMarker.hideInfoWindow();
                        newSelectedMarker.setPosition(map.getProjection().fromScreenLocation(tapPoint));

                        if (doneDragging) {
                            map.deselectMarker(newSelectedMarker);
                        }
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private MarkerViewOptions getMarker(LatLng latLng) {
        MarkerViewOptions options = new MarkerViewOptions();
        options.position(latLng);

        Icon icon = IconFactory.getInstance(this).fromBitmap(getBitmapForDrawable(this, R.drawable.airmap_flight_marker));
        options.icon(icon);
        options.title("My Flight");
        options.anchor(0.5f, 0.5f);
        return options;
    }

    private static Bitmap getBitmapForDrawable(Context context, @DrawableRes int id) {
        Drawable drawable = ContextCompat.getDrawable(context, id);
        Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            bitmap = bitmapDrawable.getBitmap();
        } else {
            if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
                bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            } else {
                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            }

            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }
        return bitmap;
    }
}
