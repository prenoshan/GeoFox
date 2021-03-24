package com.example.geofox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.geofox.Models.SettingsModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import timber.log.Timber;

public class SettingsActivity extends AppCompatActivity {

    private Spinner spnUnit, spnUnitTravel;
    private TextView text_user;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        text_user = findViewById(R.id.settingsView_text_welcomeUser);

        if(FirebaseAuth.getInstance().getCurrentUser() != null){

            text_user.setText(getString(R.string.welcome_user, FirebaseAuth.getInstance().getCurrentUser().getDisplayName()));

        }

        spnUnit = findViewById(R.id.settingsView_spn_units);
        spnUnitTravel = findViewById(R.id.settingsView_spn_unitTravel);

        ArrayAdapter<CharSequence> unitAdapter = ArrayAdapter.createFromResource(this, R.array.units, R.layout.custom_spinner);

        ArrayAdapter<CharSequence> unitTravelAdapter = ArrayAdapter.createFromResource(this, R.array.unitsTravel, R.layout.custom_spinner);

        unitAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown);

        unitTravelAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown);

        spnUnit.setAdapter(unitAdapter);
        spnUnitTravel.setAdapter(unitTravelAdapter);

    }

    public void saveSettings(View view){

        String units, unitsTravel, uuid = "";
        units = spnUnit.getSelectedItem().toString().toLowerCase();
        unitsTravel = spnUnitTravel.getSelectedItem().toString().toLowerCase();

        //setting values for the model
        SettingsModel settingsModel = new SettingsModel();
        settingsModel.setUnits(units);
        settingsModel.setUnitsTravel(unitsTravel);

        //get logged in user id
        if(FirebaseAuth.getInstance().getCurrentUser() != null){

            uuid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        }

        final Snackbar savingDataSnack = Snackbar.make(findViewById(android.R.id.content),"Saving Data...",Snackbar.LENGTH_INDEFINITE);
        savingDataSnack.show();

        mDatabase.child("users").child(uuid).setValue(settingsModel).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                savingDataSnack.dismiss();

                Toast.makeText(SettingsActivity.this, "Settings saved successfully", Toast.LENGTH_LONG).show();

                startActivity(new Intent(SettingsActivity.this, MainActivity.class));

                finish();

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                Snackbar.make(findViewById(android.R.id.content),"There was a problem saving your settings",Snackbar.LENGTH_LONG).show();

                if(e.getMessage() != null){

                    Timber.d(e);

                }

            }
        });

    }
    
}