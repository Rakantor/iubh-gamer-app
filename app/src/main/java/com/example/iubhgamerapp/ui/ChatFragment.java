package com.example.iubhgamerapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iubhgamerapp.R;
import com.example.iubhgamerapp.RVAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ChatFragment extends Fragment {
    private FirebaseUser mUser;
    private DatabaseReference refChatMessages, refUsers;
    private Map<String, String> users;
    private List<ChatMessage> chatMessages;
    private RecyclerView rv;
    private EditText input;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_chat, container, false);
        rv = root.findViewById(R.id.rv);
        input = root.findViewById(R.id.chat_input);
        Button btnSend = root.findViewById(R.id.chat_send);
        btnSend.setOnClickListener(v -> sendTextMessage());

        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        rv.setLayoutManager(llm);

        // Connect to Firebase realtime database
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        refChatMessages = FirebaseDatabase.getInstance().getReference().child("nachrichten");
        refUsers = FirebaseDatabase.getInstance().getReference().child("spieler");

        // Get list of registered users from Firebase database
        refUsers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Save users in HashMap object (key = UID; value = nickname)
                users = new HashMap<>();
                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    users.put(ds.getKey(), (String)ds.child("nickname").getValue());
                }

                getMessagesFromDatabase();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), R.string.db_comm_err, Toast.LENGTH_SHORT).show();
            }
        });

        return root;
    }

    /**
     * Reads the most recent chat messages from the Firebase realtime database.
     * Limited to the 100 most recent messages.
     */
    private void getMessagesFromDatabase() {
        refChatMessages.orderByKey().limitToLast(100).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatMessages = new ArrayList<>();

                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    long timestamp = Long.parseLong(Objects.requireNonNull(ds.getKey()));
                    String user = (String)ds.child("absender").getValue();
                    String text = (String)ds.child("text").getValue();

                    chatMessages.add(new ChatMessage(timestamp, user, text));

                    // Initialize custom RecyclerView.Adapter and scroll to the bottom
                    RVAdapter adapter = new RVAdapter(chatMessages);
                    rv.setAdapter(adapter);
                    rv.scrollToPosition(adapter.getItemCount() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), R.string.db_comm_err, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Writes a new text message to database. Metadata includes the current timestamp
     * (Unix epoch time), the sender's user id and the actual message.
     */
    private void sendTextMessage() {
        long currentEpochTimestamp = System.currentTimeMillis() / 1000L;
        String senderUid = mUser.getUid();
        String textMessage = input.getText().toString().trim();

        // If the input TextView isn't empty, write to database
        if(!textMessage.isEmpty()) {
            input.getText().clear();
            refChatMessages.child(String.valueOf(currentEpochTimestamp)).child("absender").setValue(senderUid);
            refChatMessages.child(String.valueOf(currentEpochTimestamp)).child("text").setValue(textMessage);
        }
    }

    /**
     * This class represents a chat message object.
     * It contains the text message and metadata such as the sender's name and user id as well as
     * the exact time the message was sent (Unix epoch timestamp).
     */
    public class ChatMessage {
        final public String senderUID, senderName, text, date;
        final long timestamp;

        ChatMessage(long timestamp, String senderUID, String text) {
            this.timestamp = timestamp;
            this.senderUID = senderUID;
            this.text = text;

            // Convert epoch timestamp to formatted date string
            Date date = new Date(timestamp * 1000L);
            this.date = new SimpleDateFormat("dd.MM.yyyy  HH:mm", Locale.getDefault()).format(date);

            // Check if the sender's user id still exist in the database.
            // If so, extract their nickname
            if(users.containsKey(senderUID)) this.senderName = users.get(senderUID);
            // If the user was deleted at some point, use generic term instead
            else this.senderName = getString(R.string.chat_invalid_user);
        }
    }
}