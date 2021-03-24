package com.example.geofox.ui.trips;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.geofox.R;
import com.example.geofox.TripDataActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class TripsFragment extends Fragment {

    private ListView lvTripsTaken;
    private DatabaseReference myRef;
    private FirebaseUser user;
    private List<String> tripDates;
    private TextView noTripsTaken;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.trips_fragment, container, false);

        tripDates = new ArrayList<>();

        lvTripsTaken = root.findViewById(R.id.tripsFragment_lvTripsTaken);
        noTripsTaken = root.findViewById(R.id.tripsFragment_text_trips);

        lvTripsTaken.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent tripDataActivity = new Intent(getActivity(), TripDataActivity.class);

                tripDataActivity.putExtra("dateRecorded", lvTripsTaken.getItemAtPosition(position).toString());

                startActivity(tripDataActivity);
                getActivity().finish();

            }
        });

        myRef = FirebaseDatabase.getInstance().getReference();

        user = FirebaseAuth.getInstance().getCurrentUser();

        final Snackbar gettingTripsSnack = Snackbar.make(getActivity().findViewById(android.R.id.content), "Getting all your trips...", Snackbar.LENGTH_INDEFINITE);

        gettingTripsSnack.show();

        myRef.child("users").child(user.getUid()).child("tripsTaken").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for(DataSnapshot ds: snapshot.getChildren()){

                    tripDates.add(ds.child("dateRecorded").getValue().toString());

                }

                ArrayAdapter<String> tripDatesAdapter = new ArrayAdapter<String>(
                        getActivity(),
                        android.R.layout.simple_list_item_1,
                        tripDates);

                gettingTripsSnack.dismiss();

                if(tripDatesAdapter.getCount() == 0){

                    lvTripsTaken.setVisibility(View.GONE);
                    noTripsTaken.setVisibility(View.VISIBLE);

                }

                else{

                    lvTripsTaken.setVisibility(View.VISIBLE);
                    noTripsTaken.setVisibility(View.GONE);

                }

                lvTripsTaken.setAdapter(tripDatesAdapter);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                Timber.e("Error: %s", error.getMessage());
            }
        });

        return root;
    }

}