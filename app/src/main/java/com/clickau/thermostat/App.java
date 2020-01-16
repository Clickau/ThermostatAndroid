package com.clickau.thermostat;

import android.app.Application;
import android.content.res.Resources;

// used to make project resources available from any class, without needing a context
public class App extends Application {
    private static App instance;
    private static Resources res;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        res = getResources();
    }

    public static App getInstance() {
        return instance;
    }

    public static Resources getRes() {
        return res;
    }
}
