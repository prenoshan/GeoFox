package com.example.geofox.ui.chat;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.geofox.Models.ChatModel;
import com.example.geofox.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
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
import java.util.UUID;

import timber.log.Timber;

public class ChatFragment extends Fragment {

    private ListView lvChat;
    private List<String> messages;
    private EditText edMessage;
    private FirebaseUser user;
    private DatabaseReference myRef;
    private Button btnSend;
    private ChatModel chatModel;
    private TextView tvNoMessages;
    private long messageID = 0;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.chat_fragment, container, false);

        chatModel = new ChatModel();
        user = FirebaseAuth.getInstance().getCurrentUser();
        myRef = FirebaseDatabase.getInstance().getReference();

        tvNoMessages = root.findViewById(R.id.chatFragment_noMessages);
        edMessage = root.findViewById(R.id.chatFragment_edMessage);
        btnSend = root.findViewById(R.id.chatFragment_btnSendText);
        lvChat = root.findViewById(R.id.chatFragment_lvChat);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        messages = new ArrayList<>();

        final Snackbar gettingMessages = Snackbar.make(getActivity().findViewById(android.R.id.content),"Getting messages",Snackbar.LENGTH_INDEFINITE);

        gettingMessages.show();

        myRef.child("chat").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                messages.clear();

                for(DataSnapshot ds: snapshot.getChildren()){

                    messages.add(ds.child("username").getValue().toString() + ": " + ds.child("message").getValue().toString());

                }

                ArrayAdapter<String> messagesAdapter = new ArrayAdapter<>(
                        root.getContext(),
                        android.R.layout.simple_list_item_1,
                        messages);

                if(messagesAdapter.getCount() == 0){

                    lvChat.setVisibility(View.GONE);
                    tvNoMessages.setVisibility(View.VISIBLE);

                }

                else{

                    lvChat.setVisibility(View.VISIBLE);
                    tvNoMessages.setVisibility(View.GONE);

                }

                lvChat.setAdapter(messagesAdapter);

                gettingMessages.dismiss();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                Timber.e("Error: %s", error.getMessage());

            }
        });

        return root;
    }

    public void sendMessage(){

        chatModel.setUsername(user.getDisplayName());
        chatModel.setMessage(edMessage.getText().toString().trim());

        final Snackbar creatingMessage = Snackbar.make(getActivity().findViewById(android.R.id.content),"Sending message",Snackbar.LENGTH_INDEFINITE);

        creatingMessage.show();

        if(chatModel.getMessage().equals("")){

            creatingMessage.dismiss();

            Snackbar.make(getActivity().findViewById(android.R.id.content),"Message cannot be empty",Snackbar.LENGTH_LONG).show();

        }

        else{

            myRef.child("chat").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    if(snapshot.exists()){
                        messageID = (snapshot.getChildrenCount());
                    }

                    myRef.child("chat").child(String.valueOf(messageID + 1)).setValue(chatModel).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            creatingMessage.dismiss();

                            Snackbar.make(getActivity().findViewById(android.R.id.content),"Message sent",Snackbar.LENGTH_LONG).show();

                            edMessage.getText().clear();

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                            Timber.e("Error: %s", e.getMessage());

                            creatingMessage.dismiss();

                            Snackbar.make(getActivity().findViewById(android.R.id.content),"Error sending message",Snackbar.LENGTH_LONG).show();
                        }
                    });

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                    Timber.e("Error: %s", error.getMessage());
                }
            });

        }

    }
}