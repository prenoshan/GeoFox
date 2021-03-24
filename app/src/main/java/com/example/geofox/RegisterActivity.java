package com.example.geofox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import timber.log.Timber;

public class RegisterActivity extends AppCompatActivity {

    //variables
    private EditText edDisplayName, edEmail ,edPassword, edConfirmPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        edDisplayName = findViewById(R.id.regView_ed_DisplayName);
        edEmail = findViewById(R.id.regView_ed_Email);
        edPassword = findViewById(R.id.regView_ed_Password);
        edConfirmPassword = findViewById(R.id.regView_ed_ConfirmPassword);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        getSupportActionBar().hide();
        getWindow().setStatusBarColor(Color.rgb(203, 53, 107));

    }

    public void registerUser(View view){

        final String displayName, email, password, confirmPassword;

        //sets the values required for user registration
        displayName = edDisplayName.getText().toString().trim();
        email = edEmail.getText().toString().trim();
        password = edPassword.getText().toString().trim();
        confirmPassword = edConfirmPassword.getText().toString().trim();

        //checks if all fields are populated
        if(displayName.equals("") || email.equals("") || password.equals("") || confirmPassword.equals("")){

            Snackbar.make(findViewById(android.R.id.content),"All fields are required",Snackbar.LENGTH_LONG).show();

        }

        //checks the password length
        else if(password.length() < 6){

            Snackbar.make(findViewById(android.R.id.content),"Password must be at least 6 characters long",Snackbar.LENGTH_LONG).show();

        }

        //checks if the email is a valid email address
        else if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){

            Snackbar.make(findViewById(android.R.id.content),"Please enter a valid email address",Snackbar.LENGTH_LONG).show();

        }

        //checks if the password and confirmed password is equal
        else if(!password.equals(confirmPassword)) {

            Snackbar.make(findViewById(android.R.id.content),"Passwords don't match",Snackbar.LENGTH_LONG).show();

        }

        else {

            final Snackbar creatingUserSnack = Snackbar.make(findViewById(android.R.id.content),"Creating user...",Snackbar.LENGTH_INDEFINITE);

            creatingUserSnack.show();

            //firebase method to register a new user
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if(task.isSuccessful()){

                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                                if(user != null){

                                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(displayName).build();
                                    user.updateProfile(profileUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {

                                            if(task.isSuccessful()){

                                                creatingUserSnack.dismiss();

                                                startActivity(new Intent(RegisterActivity.this, SettingsActivity.class));

                                                finish();

                                            }

                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {

                                            Timber.d(e);

                                        }
                                    });

                                }

                            }

                            else{

                                Snackbar.make(findViewById(android.R.id.content),"Error creating user", Snackbar.LENGTH_LONG).show();
                            }
                        }
                    });
        }

    }

    public void goToLogin(View view){

        startActivity(new Intent(this, LoginActivity.class));

    }

}