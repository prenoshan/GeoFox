package com.example.geofox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    //variables
    private EditText edEmail, edPassword;
    private FirebaseAuth auth;
    private int LOCATION_PERMISSION_REQUEST_CODE = 1101;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this, "Permission granted, You can continue now", Toast.LENGTH_SHORT).show();

            } else { // if permission is not granted

                Toast.makeText(this, "You need to grant location permission to use this app", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        auth = FirebaseAuth.getInstance();

        //checks if the user is logged in and redirects them to the home page
        if (auth.getCurrentUser() != null) {

            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();

        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (Build.VERSION.SDK_INT >= 23) { // Marshmallow

                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            }

        }

        edEmail = findViewById(R.id.loginView_ed_Email);
        edPassword = findViewById(R.id.loginView_ed_Password);

        getSupportActionBar().hide();
        getWindow().setStatusBarColor(Color.rgb(203, 53, 107));
    }

    public void loginUser(View view) {

        String email, password;

        //gets the email and password a user sets
        email = edEmail.getText().toString().trim();
        password = edPassword.getText().toString().trim();

        final Snackbar loginSnack = Snackbar.make(findViewById(android.R.id.content), "Logging you in...", Snackbar.LENGTH_INDEFINITE);

        loginSnack.show();

        //firebase method to login a user
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if (task.isSuccessful()) {

                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();

                    loginSnack.dismiss();

                } else {


                    loginSnack.dismiss();

                    Snackbar.make(findViewById(android.R.id.content), "Login failed", Snackbar.LENGTH_LONG).show();

                }

            }
        });
    }

    public void goToReg(View view){

        startActivity(new Intent(this, RegisterActivity.class));

    }

}