package com.clickau.thermostat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

@SuppressWarnings("WeakerAccess") // the code inspector shows this warning, but IntelliSense does not
public class MoreFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_more, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton setupWifiButton = view.findViewById(R.id.more_setup_wifi_button);
        setupWifiButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SetupWifiActivity.class);
            startActivity(intent);
        });

        MaterialButton setupFirebaseButton = view.findViewById(R.id.more_setup_firebase_button);
        setupFirebaseButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SetupFirebaseActivity.class);
            intent.putExtra("openMethod", "manual");
            startActivity(intent);
        });
    }
}
