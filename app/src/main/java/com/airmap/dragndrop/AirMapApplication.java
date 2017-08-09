package com.airmap.dragndrop;

import android.support.multidex.MultiDexApplication;
import com.mapbox.mapboxsdk.Mapbox;

public class AirMapApplication extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();

        Mapbox.getInstance(this, YOUR_MAP_BOX_KEY_HERE);
    }
}
