package com.example.iubhgamerapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

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

public class RatingFragment extends Fragment {
    private EventDate selectedEvent;
    private FirebaseUser mUser;
    private DatabaseReference refUsers, refEvents;
    private View root;
    private List<EventDate> eventDates;
    private Map<String, String> users;
    private Spinner spinnerPastEvents;
    private RatingBar rbOverall, rbFood, rbHost;
    private TextView tvHost;
    private TextView tvOverallRatings, tvFoodRatings, tvHostRatings;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_rating, container, false);
        spinnerPastEvents = root.findViewById(R.id.spinner_rating);
        rbOverall = root.findViewById(R.id.ratingBar_overall);
        rbFood = root.findViewById(R.id.ratingBar_food);
        rbHost = root.findViewById(R.id.ratingBar_host);
        tvHost = root.findViewById(R.id.textView_rateHost);
        tvOverallRatings = root.findViewById(R.id.textView_rate1);
        tvFoodRatings = root.findViewById(R.id.textView_rate2);
        tvHostRatings = root.findViewById(R.id.textView_rate3);
        Button btnRate = root.findViewById(R.id.rate);

        // Set rate button listener
        btnRate.setOnClickListener(v -> ratePastEvent());

        // Set spinner listener
        spinnerPastEvents.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedEvent = eventDates.get(position);

                rbOverall.setRating(selectedEvent.ownOverallRating);
                rbFood.setRating(selectedEvent.ownFoodRating);
                rbHost.setRating(selectedEvent.ownHostRating);

                tvHost.setText("Host (" + selectedEvent.hostName + ")");

                String sNoRatings = getString(R.string.zero_ratings);

                String str1 = getString(R.string.other_ratings, selectedEvent.avgOverallRating, selectedEvent.overallRatingsCount);
                tvOverallRatings.setText(selectedEvent.overallRatingsCount > 0 ? str1 : sNoRatings);

                String str2 = getString(R.string.other_ratings, selectedEvent.avgFoodRating, selectedEvent.foodRatingsCount);
                tvFoodRatings.setText(selectedEvent.foodRatingsCount > 0 ? str2 : sNoRatings);

                String str3 = getString(R.string.other_ratings, selectedEvent.avgHostRating, selectedEvent.hostRatingsCount);
                tvHostRatings.setText(selectedEvent.hostRatingsCount > 0 ? str3 : sNoRatings);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nothing to do here
            }
        });

        // Connect to Firebase realtime database
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        refUsers = FirebaseDatabase.getInstance().getReference().child("spieler");
        refEvents = FirebaseDatabase.getInstance().getReference().child("termine");

        // Get list of registered users from Firebase database
        refUsers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Save users in HashMap object (key = UID; value = nickname)
                users = new HashMap<>();
                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    users.put(ds.getKey(), (String)ds.child("nickname").getValue());
                }

                getPastEvents();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), R.string.db_comm_err, Toast.LENGTH_SHORT).show();
            }
        });

        return root;
    }

    /**
     * Writes the user's rating of the selected event to database.
     */
    private void ratePastEvent() {
        long ownOverallRating = (long)rbOverall.getRating();
        long ownFoodRating = (long)rbFood.getRating();
        long ownHostRating = (long)rbHost.getRating();

        // Write ratings to database IF the user rated every required item
        if(ownOverallRating > 0 && ownFoodRating > 0 && ownHostRating > 0) {
            DatabaseReference refRatings = refEvents.child(String.valueOf(selectedEvent.epochTimestamp)).child("bewertungen");
            refRatings.child("allgemein").child(mUser.getUid()).setValue(ownOverallRating);
            refRatings.child("essen").child(mUser.getUid()).setValue(ownFoodRating);
            refRatings.child("gastgeber").child(mUser.getUid()).setValue(ownHostRating);
        }
        // Otherwise, display error message
        else {
            Toast.makeText(getContext(), R.string.rate_error, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Reads the most recent events from the database. The user can rate the overall experience,
     * food & drinks as well as the event host.
     * Only the last 3 events can be rated.
     */
    private void getPastEvents() {
        refEvents.orderByKey().limitToLast(4).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Iterate through events to extract metadata
                eventDates = new ArrayList<>();
                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    // Check if the event date is actually in the future.
                    // Skip it, as only past events are unlocked for rating.
                    long epochTimestamp = Long.parseLong(ds.getKey());
                    if(epochTimestamp > (System.currentTimeMillis() / 1000L)) break;

                    // Extract event host user id
                    String hostUID = (String)ds.child("gastgeber").getValue();

                    // Extract individual rating data
                    Map<String, Long> overallRatings = new HashMap<>();
                    Map<String, Long> foodRatings = new HashMap<>();
                    Map<String, Long> hostRatings = new HashMap<>();
                    for(DataSnapshot dsOverallRatings : ds.child("bewertungen").child("allgemein").getChildren()) {
                        overallRatings.put(dsOverallRatings.getKey(), (long)dsOverallRatings.getValue());
                    }
                    for(DataSnapshot dsFoodRatings : ds.child("bewertungen").child("essen").getChildren()) {
                        foodRatings.put(dsFoodRatings.getKey(), (long)dsFoodRatings.getValue());
                    }
                    for(DataSnapshot dsHostRatings : ds.child("bewertungen").child("gastgeber").getChildren()) {
                        hostRatings.put(dsHostRatings.getKey(), (long)dsHostRatings.getValue());
                    }

                    // Add event to ArrayList to make data available to other methods
                    eventDates.add(new EventDate(epochTimestamp, hostUID, overallRatings, foodRatings, hostRatings));
                }

                // Update spinner; set selection to most recent event
                List<String> spinnerEntries = new ArrayList<>();
                for(int i = 0; i < eventDates.size(); i++) spinnerEntries.add(eventDates.get(i).getFormattedDate());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(root.getContext(),
                        android.R.layout.simple_spinner_item, spinnerEntries);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerPastEvents.setAdapter(adapter);
                spinnerPastEvents.setSelection(adapter.getCount() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), R.string.db_comm_err, Toast.LENGTH_SHORT).show();
            }
        });
    }

    class EventDate {
        final long epochTimestamp;
        final long ownOverallRating, ownFoodRating, ownHostRating;
        final int overallRatingsCount, foodRatingsCount, hostRatingsCount;
        final long avgOverallRating, avgFoodRating, avgHostRating;
        final String hostName;

        EventDate(long epochTimestamp, String hostUID, Map<String, Long> overallRatings, Map<String, Long> foodRatings, Map<String, Long> hostRatings) {
            final String ownUID = mUser.getUid();
            this.epochTimestamp = epochTimestamp;
            this.hostName = users.get(hostUID);

            this.overallRatingsCount = overallRatings.size();
            this.foodRatingsCount = foodRatings.size();
            this.hostRatingsCount = hostRatings.size();

            if(overallRatings.containsKey(ownUID)) ownOverallRating = overallRatings.get(ownUID);
            else ownOverallRating = 0;

            if(foodRatings.containsKey(ownUID)) ownFoodRating = foodRatings.get(ownUID);
            else ownFoodRating = 0;

            if(hostRatings.containsKey(ownUID)) ownHostRating = hostRatings.get(ownUID);
            else ownHostRating = 0;

            long temp = 0;
            for(Map.Entry<String, Long> entry : overallRatings.entrySet()) temp += entry.getValue();
            avgOverallRating = overallRatingsCount > 0 ? temp / overallRatingsCount : 0;

            temp = 0;
            for(Map.Entry<String, Long> entry : foodRatings.entrySet()) temp += entry.getValue();
            avgFoodRating = foodRatingsCount > 0 ? temp / foodRatingsCount : 0;

            temp = 0;
            for(Map.Entry<String, Long> entry : hostRatings.entrySet()) temp += entry.getValue();
            avgHostRating = hostRatingsCount > 0 ? temp / hostRatingsCount : 0;
        }

        String getFormattedDate() {
            Date date = new Date(this.epochTimestamp * 1000L);
            return new SimpleDateFormat("EEEE, dd. MMMM yyyy", Locale.getDefault()).format(date);
        }
    }
}