package com.example.tourtrack.ui.start;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tourtrack.Login;
import com.example.tourtrack.MainActivity;
import com.example.tourtrack.R;
import com.example.tourtrack.SignUp;
import com.example.tourtrack.ui.home.HomeFragment;

public class StartActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnSignIn = findViewById(R.id.btnSignIn);

        // Start --> LOGIN
        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(StartActivity.this, Login.class);
            startActivity(intent);
            finish();
        });

        // start --> sign up
        btnSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(StartActivity.this, SignUp.class);
            startActivity(intent);
        });
    }


}
