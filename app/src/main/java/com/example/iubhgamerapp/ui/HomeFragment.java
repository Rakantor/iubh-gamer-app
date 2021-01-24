package com.example.iubhgamerapp.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.iubhgamerapp.LoginActivity;
import com.example.iubhgamerapp.R;
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

public class HomeFragment extends Fragment {
    private View root;
    private FirebaseUser mUser;
    private DatabaseReference refUsers, refGames, refEventDates;
    private DataSnapshot dsUsers, dsGames;
    private ProgressBar progressBar;
    private TextView welcome, nextDate, nextHost;
    private Button btnSignOut, btnVote, btnSuggest;
    private Spinner spinnerGames;
    private Map<Long, Integer> games = new HashMap<>();
    private String userNickname, nextDateID;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_home, container, false);
        welcome = root.findViewById(R.id.text_home);
        nextDate = root.findViewById(R.id.text_home2);
        nextHost = root.findViewById(R.id.text_home5);
        btnSignOut = root.findViewById(R.id.logout);
        btnVote = root.findViewById(R.id.vote);
        btnSuggest = root.findViewById(R.id.suggest);
        spinnerGames = root.findViewById(R.id.spinner_games);
        progressBar = root.findViewById(R.id.progressBar);

        // Set button listeners
        btnSignOut.setOnClickListener(v -> signOutUser());
        btnVote.setOnClickListener(v ->
                refEventDates.child(nextDateID).child("abstimmung_spiele").child(mUser.getUid()).setValue(spinnerGames.getSelectedItemId()));
        btnSuggest.setOnClickListener(v -> showInputDialog());

        // Display loading bar
        setProgressBar(true);

        // Connect to Firebase realtime database
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        refEventDates = FirebaseDatabase.getInstance().getReference("termine");
        refGames = FirebaseDatabase.getInstance().getReference("spiele");
        refUsers = FirebaseDatabase.getInstance().getReference("spieler");

        // Get list of registered users from database
        refUsers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                dsUsers = dataSnapshot;
                setValues();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), R.string.db_comm_err, Toast.LENGTH_SHORT).show();
            }
        });

        // Get list of available games from database
        refGames.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                dsGames = dataSnapshot;
                updateGamesList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), R.string.db_comm_err, Toast.LENGTH_SHORT).show();
            }
        });

        return root;
    }

    /**
     * Displays a circular loading bar.
     * While active, user interaction is disabled.
     * @param isActive is the progress bar visible (true) or not (false)
     */
    private void setProgressBar(boolean isActive) {
        if(isActive) {
            progressBar.setVisibility(View.VISIBLE);
            // Disable user interaction while progress bar is visible
            Objects.requireNonNull(getActivity()).getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        } else if(progressBar.getVisibility() == View.VISIBLE) {
            progressBar.setVisibility(View.GONE);
            // Re-enable user interaction when progress bar is gone
            Objects.requireNonNull(getActivity()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }
    }

    /**
     * Displays a popup dialog with an text input field. The user is supposed to type in the title
     * of a game that he would like to add to the database.
     */
    private void showInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.input_dialog_title);

        // Set up the input
        final EditText input = new EditText(getContext());
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", (dialog, which) -> {
            String newGameTitle = input.getText().toString().trim();

            // On button press, write to database if input field isn't empty
            if(!newGameTitle.isEmpty()) {
                refGames.child(String.valueOf(dsGames.getChildrenCount())).setValue(newGameTitle);
            }
        });
        builder.setNegativeButton(R.string.btn_cancel, (dialog, which) -> dialog.cancel());

        // Show dialog on screen
        builder.show();
    }

    private void updateGamesList() {
        List<String> list = new ArrayList<>();
        for(long i = 0; i < dsGames.getChildrenCount(); i++) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append((String)dsGames.child(String.valueOf(i)).getValue());
            if(games.containsKey(i)) stringBuilder.append(" (").append(games.get(i)).append(" votes)");

            list.add(stringBuilder.toString());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(root.getContext(),
                android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGames.setAdapter(adapter);
    }

    private void setValues() {
        // Get details of upcoming event from database
        refEventDates.orderByKey().limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    nextDateID = ds.getKey();
                    long epoch = Long.parseLong(nextDateID);

                    if(epoch < (System.currentTimeMillis() / 1000L)) {
                        addUpcomingEvent(epoch);
                        return;
                    }

                    Date date = new Date(epoch * 1000L);
                    String s = new SimpleDateFormat("dd. MMMM YYYY", Locale.getDefault()).format(date);
                    nextDate.setText(s);

                    String sNextHostUID = (String) ds.child("gastgeber").getValue();
                    String sNextHost = (String)dsUsers.child(sNextHostUID).child("nickname").getValue();
                    nextHost.setText(sNextHost + "'s place");

                    if(ds.hasChild("abstimmung_spiele")) {
                        games = new HashMap<>();
                        for(DataSnapshot dataSnapshot1 : ds.child("abstimmung_spiele").getChildren()) {
                            long curInt = (long)dataSnapshot1.getValue();

                            if(games.containsKey(curInt)) games.put(curInt, games.get(curInt)+1);
                            else games.put(curInt, 1);
                        }
                    }
                }
                updateGamesList();
                setProgressBar(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), R.string.db_comm_err, Toast.LENGTH_SHORT).show();
            }
        });

        userNickname = (String)dsUsers.child(mUser.getUid()).child("nickname").getValue();

        String sWelcome = userNickname + getString(R.string.welcome_back);
        welcome.setText(sWelcome);
    }

    /**
     * Determines the date and host of the next event and writes the details to the database.
     * If this was a real life app and not just an example project, it would be advisable to handle
     * event setup automatically with scheduled functions running on the server (a possible
     * solution could be Google Cloud Functions for Firebase).
     * @param prevEventTimestamp Unix epoch time of the previous event
     */
    private void addUpcomingEvent(long prevEventTimestamp) {
        // Programmatically determine the host of the upcoming event by iterating through the list
        // of registered users and comparing the epoch timestamps of their most recently hosted event.
        // The lowest timestamp will determine the new host.
        String nextHostUID = null;
        long ll = 0;
        for(DataSnapshot dataSnapshot : dsUsers.getChildren()) {
            long ts = (long)dataSnapshot.child("zuletzt_gehostet").getValue();
            if(ll == 0 || ts < ll) {
                ll = ts;
                nextHostUID = dataSnapshot.getKey();
            }
        }

        // Calculate epoch timestamp of upcoming event
        // It will take place exactly 1 week after the last event.
        // 7 days * 24 hours * 60 minutes * 60 seconds = 604800
        long nextEventTimestamp = prevEventTimestamp + 604800;

        // Write to database
        refEventDates.child(String.valueOf(nextEventTimestamp)).child("gastgeber").setValue(nextHostUID);
    }

    /**
     * Signs out the current Firebase user and switches to the login interface.
     */
    private void signOutUser() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(getActivity(), LoginActivity.class));
        Objects.requireNonNull(getActivity()).finish();
    }
}