package com.example.application_layer_software_framework_for_solar_powered_iot_systems;

import android.os.Bundle;


import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavView =
                findViewById(R.id.bottom_nav);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.fragment_container,
                            new home()
                    )
                    .commit();
        }


        bottomNavView.setOnItemSelectedListener(item -> {

            Fragment selectedFragment = null;

            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new home();

            } else if (itemId == R.id.nav_usage) {
                selectedFragment = new usage();

            } else if (itemId == R.id.nav_efficiency) {
                selectedFragment = new efficiency();

            } else if (itemId == R.id.nav_account) {
                selectedFragment = new account();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.fragment_container,
                                selectedFragment
                        )
                        .commit();

                return true;
            }

            return false;
        });


        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {
                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            0
                    );

                    return insets;
                }
        );
    }

}