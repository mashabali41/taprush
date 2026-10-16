package com.example.taprush;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class GameActivity extends AppCompatActivity {

    private TextView tvGreeting, tvBestScore, tvTimer, tvScore;
    private MaterialButton btnTap, btnSignOut;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private GoogleSignInClient mGoogleSignInClient;

    private CountDownTimer countDownTimer;
    private boolean isGameRunning = false;
    private int currentScore = 0;
    private long bestScore = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        tvGreeting = findViewById(R.id.tv_greeting);
        tvBestScore = findViewById(R.id.tv_best_score);
        tvTimer = findViewById(R.id.tv_timer);
        tvScore = findViewById(R.id.tv_score);
        btnTap = findViewById(R.id.btn_tap);
        btnSignOut = findViewById(R.id.btn_sign_out);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
            return;
        }

        setupUserHeader(currentUser);
        loadUserDataFromFirestore(currentUser.getUid());

        btnTap.setOnClickListener(v -> handleTap());
        btnSignOut.setOnClickListener(v -> performSignOut());
    }

    private void setupUserHeader(FirebaseUser user) {
        // Priority 1: Check if name was passed directly from SignUp / Login intent
        String passedName = getIntent() != null ? getIntent().getStringExtra("user_name") : null;
        if (passedName != null && !passedName.trim().isEmpty()) {
            tvGreeting.setText("Hi, " + passedName.trim());
            return;
        }

        // Priority 2: Use FirebaseUser displayName
        String displayName = user.getDisplayName();
        if (displayName != null && !displayName.trim().isEmpty()) {
            tvGreeting.setText("Hi, " + displayName.trim());
        } else if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            tvGreeting.setText("Hi, " + user.getEmail().trim());
        } else {
            tvGreeting.setText("Hi, Player");
        }

        // Reload user profile in background
        user.reload().addOnSuccessListener(unused -> {
            FirebaseUser refreshed = mAuth.getCurrentUser();
            if (refreshed != null && refreshed.getDisplayName() != null && !refreshed.getDisplayName().trim().isEmpty()) {
                tvGreeting.setText("Hi, " + refreshed.getDisplayName().trim());
            }
        });
    }

    private void loadUserDataFromFirestore(String uid) {
        mFirestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long savedHighScore = documentSnapshot.getLong("highScore");
                        if (savedHighScore != null) {
                            bestScore = savedHighScore;
                            tvBestScore.setText(String.valueOf(bestScore));
                        }
                        String firestoreName = documentSnapshot.getString("name");
                        if (firestoreName != null && !firestoreName.trim().isEmpty()) {
                            tvGreeting.setText("Hi, " + firestoreName.trim());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(GameActivity.this, "Could not load score: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void handleTap() {
        if (!isGameRunning) {
            startGame();
        } else {
            currentScore++;
            tvScore.setText(String.valueOf(currentScore));
        }
    }

    private void startGame() {
        isGameRunning = true;
        currentScore = 0;
        tvScore.setText(String.valueOf(currentScore));
        btnTap.setText("TAP!");
        tvTimer.setText("10s");

        countDownTimer = new CountDownTimer(10000, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) Math.ceil(millisUntilFinished / 1000.0);
                tvTimer.setText(secondsLeft + "s");
            }

            @Override
            public void onFinish() {
                isGameRunning = false;
                tvTimer.setText("0s");
                btnTap.setText("PLAY AGAIN");
                handleGameFinish();
            }
        }.start();
    }

    private void handleGameFinish() {
        if (currentScore > bestScore) {
            bestScore = currentScore;
            tvBestScore.setText(String.valueOf(bestScore));
            Toast.makeText(this, "New High Score: " + bestScore + "!", Toast.LENGTH_LONG).show();

            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("highScore", bestScore);
                String name = user.getDisplayName();
                if (name != null && !name.trim().isEmpty()) {
                    updateData.put("name", name.trim());
                }

                mFirestore.collection("users").document(user.getUid())
                        .set(updateData, SetOptions.merge())
                        .addOnFailureListener(e -> Toast.makeText(GameActivity.this, "Error saving score: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        } else {
            Toast.makeText(this, "Game Over! You scored " + currentScore, Toast.LENGTH_SHORT).show();
        }
    }

    private void performSignOut() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // 1. Sign out from Firebase Auth
        mAuth.signOut();

        // 2. Safely trigger Google Sign Out without blocking navigation
        if (mGoogleSignInClient != null) {
            try {
                mGoogleSignInClient.signOut();
            } catch (Exception ignored) {}
        }

        // 3. Immediately return to Login screen
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(GameActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
