package com.clickau.thermostat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.main_activity_toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences pref = getSharedPreferences("firebase_credentials", Context.MODE_PRIVATE);
        final String firebaseUrl = pref.getString("url", null);
        final String firebaseSecret = pref.getString("secret", null);
        if (firebaseUrl != null && firebaseSecret != null) {
            FirebaseService.initialize(firebaseUrl, firebaseSecret);
        } else {
            Log.d(TAG, "Firebase not initialized");
            finish();
            Intent intent = new Intent(this, SetupFirebaseActivity.class);
            startActivity(intent);
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        // open the dashboard when the app is opened, but not when the phone is rotated
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_dashboard);
            ActionBar actionBar = getSupportActionBar();
            // if we set the title from the toolbar, it won't work when the application is first started
            if (actionBar != null)
                getSupportActionBar().setTitle(R.string.bottom_nav_dashboard);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId())
        {
            case R.id.nav_dashboard:
                toolbar.setTitle(R.string.bottom_nav_dashboard);
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new DashboardFragment()).commit();
                break;
            case R.id.nav_schedules:
                toolbar.setTitle(R.string.bottom_nav_schedules);
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SchedulesFragment()).commit();
                break;
            case R.id.nav_more:
                toolbar.setTitle(R.string.bottom_nav_more);
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new MoreFragment()).commit();
                break;
        }
        return true;
    }
}
