package com.example.iubhgamerapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private EditText user, pw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        user = findViewById(R.id.username);
        pw = findViewById(R.id.password);
        Button btnSignIn = findViewById(R.id.login);
        btnSignIn.setOnClickListener(v -> signInUser());

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if user is signed in (non-null) and update UI accordingly
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null) startMainActivity();
    }

    /**
     * Starts the main activity and closes the login activity.
     */
    private void startMainActivity() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    /**
     * Authenticates the user with Firebase using email and password.
     */
    private void signInUser() {
        String sUser = user.getText().toString().trim();
        String sPassword = pw.getText().toString().trim();

        // Display an error message if the user didn't fill in their email or password
        if(sUser.equals("") || sPassword.equals("")) {
            Toast.makeText(LoginActivity.this, R.string.firebase_credentials_missing, Toast.LENGTH_SHORT).show();
        }
        // Otherwise, attempt authentication with Firebase.
        // Start main activity on success or display error message
        else {
            Toast.makeText(getApplicationContext(), R.string.firebase_signin_progress, Toast.LENGTH_SHORT).show();
            mAuth.signInWithEmailAndPassword(sUser, sPassword)
                    .addOnCompleteListener(this, task -> {
                        if(task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            startMainActivity();
                        } else {
                            // If sign in fails, display an error message to the user
                            Toast.makeText(LoginActivity.this, R.string.firebase_auth_failed,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }
}
