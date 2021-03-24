package com.example.geofox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.example.geofox.Models.TripsModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import timber.log.Timber;

public class TripDataActivity extends AppCompatActivity {

    private TextView tripDate, tripDistance, tripDuration, tripOrigin, tripDestination, tripMode;
    private DatabaseReference myRef;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_data);

        tripDate = findViewById(R.id.tripDataActivity_textDate);
        tripDistance = findViewById(R.id.tripDataActivity_textDistance);
        tripDuration = findViewById(R.id.tripDataActivity_textDuration);
        tripOrigin = findViewById(R.id.tripDataActivity_textOrigin);
        tripDestination = findViewById(R.id.tripDataActivity_textDestination);
        tripMode = findViewById(R.id.tripDataActivity_tripMode);

        tripDate.setText(getIntent().getStringExtra("dateRecorded"));

        myRef = FirebaseDatabase.getInstance().getReference();
        user = FirebaseAuth.getInstance().getCurrentUser();

        myRef.child("users").child(user.getUid()).child("tripsTaken").orderByChild("dateRecorded").equalTo(tripDate.getText().toString()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for(DataSnapshot ds: snapshot.getChildren()){

                    TripsModel tripsModel = ds.getValue(TripsModel.class);

                    tripDistance.setText(tripsModel.getTripDistance());
                    tripDuration.setText(tripsModel.getTripDuration());
                    tripOrigin.setText(tripsModel.getTripOrigin());
                    tripDestination.setText(tripsModel.getTripDestination());
                    tripMode.setText(tripsModel.getTravelMode());

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                Timber.e("Error: %s", error.getMessage());

            }
        });

        tripDate.setText(getIntent().getStringExtra("dateRecorded"));

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Intent tripDataIntent = new Intent(TripDataActivity.this, MainActivity.class);

        tripDataIntent.putExtra("backToTripData", "true");

        startActivity(tripDataIntent);

        this.finish();

    }
}