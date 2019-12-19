package com.clickau.thermostat;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SetupFirebaseActivity extends AppCompatActivity {

    private static final String TAG = "SetupFirebaseActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_firebase);
        Toolbar toolbar = findViewById(R.id.setup_firebase_toolbar);
        setSupportActionBar(toolbar);

        // if activity was opened manually by the user from the more menu, show the back button in action bar
        String openMethod = getIntent().getStringExtra("openMethod");
        if (openMethod != null && openMethod.equals("manual")) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null)
                actionBar.setDisplayHomeAsUpEnabled(true);
        }

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
