package com.clickau.thermostat;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;

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

    @ColorInt
    public static int resolveColorAttr(Context context, @AttrRes int colorAttr) {
        TypedValue resolvedAttr = resolveThemeAttr(context, colorAttr);
        // resourceId is used if it's a ColorStateList, and data if it's a color reference or a hex color
        int colorRes = resolvedAttr.resourceId != 0 ? resolvedAttr.resourceId : resolvedAttr.data;
        return ContextCompat.getColor(context, colorRes);
    }

    public static TypedValue resolveThemeAttr(Context context, @AttrRes int attrRes) {
        Resources.Theme theme = context.getTheme();
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(attrRes, typedValue, true);
        return typedValue;
    }
}
