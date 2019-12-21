package com.clickau.thermostat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.main_activity_toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences pref = getSharedPreferences("firebase_credentials", Context.MODE_PRIVATE);
        final String firebaseUrl = pref.getString("url", null);
        final String firebaseSecret = pref.getString("secret", null);
        if (firebaseUrl != null && firebaseSecret != null) {
            FirebaseService.initialize(firebaseUrl, firebaseSecret);
            Log.d(TAG, "Firebase URL: " + firebaseUrl);
            Log.d(TAG, "Firebase Secret: " + firebaseSecret);
        } else {
            Log.d(TAG, "Firebase not initialized");
            finish();
            Intent intent = new Intent(this, SetupFirebaseActivity.class);
            startActivity(intent);
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        // open the dashboard when the app is opened, but not when the phone is rotated
        if (savedInstanceState == null)
            bottomNavigationView.setSelectedItemId(R.id.nav_dashboard);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId())
        {
            case R.id.nav_dashboard:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new DashboardFragment()).commit();
                break;
            case R.id.nav_schedules:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SchedulesFragment()).commit();
                break;
            case R.id.nav_more:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new MoreFragment()).commit();
                break;
        }
        return true;
    }
}
