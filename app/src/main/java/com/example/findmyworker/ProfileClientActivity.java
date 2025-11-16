package com.example.findmyworker;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class ProfileClientActivity extends AppCompatActivity {

    // UI Elements - Profile Section
    ImageButton imgProfilePicture;
    ImageButton btnEditProfilePicture;
    TextView tvUserName;
    TextView tvEmail;
    TextView tvPhoneNumber;
    TextView tvBookingsInfo;

    // UI Elements - Bottom Navigation
    LinearLayout btnHome;
    LinearLayout btnSearch;
    LinearLayout btnChats;

    // Firebase
    FirebaseAuth refAuth;
    DatabaseReference refCustomer;
    StorageReference storageRef;

    // Data
    String userId;
    Customer currentClient;

    // For image upload
    private static final int PICK_PROFILE_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_client);

        // Initialize Firebase
        refAuth = FirebaseAuth.getInstance();
        refCustomer = FirebaseDatabase.getInstance().getReference("Customer");
        storageRef = FirebaseStorage.getInstance().getReference();

        // Get current user ID
        if (refAuth.getCurrentUser() != null) {
            userId = refAuth.getCurrentUser().getUid();
        } else {
            // User not logged in, redirect to login
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Initialize UI elements
        initializeViews();

        // Load user data
        loadClientData();

        // Set click listeners
        setClickListeners();
    }

    private void initializeViews() {
        // Profile section
        imgProfilePicture = findViewById(R.id.imgProfilePicture);
        btnEditProfilePicture = findViewById(R.id.btnEditProfilePicture);
        tvUserName = findViewById(R.id.tvUserName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        tvBookingsInfo = findViewById(R.id.tvBookingsInfo);

        // Bottom navigation
        btnHome = findViewById(R.id.btnHome);
        btnSearch = findViewById(R.id.btnSearch);
        btnChats = findViewById(R.id.btnChats);
    }

    private void setClickListeners() {
        // Profile picture click - view full screen (placeholder for now)
        imgProfilePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProfileClientActivity.this, "View full profile picture", Toast.LENGTH_SHORT).show();
                // TODO: Implement full screen image view
            }
        });

        // Edit profile picture button
        btnEditProfilePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGalleryForProfilePicture();
            }
        });

        // Bottom navigation
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProfileClientActivity.this, "Already on Home", Toast.LENGTH_SHORT).show();
            }
        });

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProfileClientActivity.this, "Search coming soon", Toast.LENGTH_SHORT).show();
            }
        });

        btnChats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProfileClientActivity.this, "Chats coming soon", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadClientData() {
        refCustomer.child(userId).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful() && task.getResult().exists()) {
                    currentClient = task.getResult().getValue(Customer.class);

                    if (currentClient != null) {
                        // Display user info
                        tvUserName.setText(currentClient.getName());
                        tvEmail.setText("Email: " + currentClient.getEmail());
                        tvPhoneNumber.setText("Phone: " + currentClient.getPhone());

                        // Load profile picture
                        if (currentClient.getProfilePictureUrl() != null &&
                                !currentClient.getProfilePictureUrl().isEmpty()) {
                            Glide.with(ProfileClientActivity.this)
                                    .load(currentClient.getProfilePictureUrl())
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_default_profile)
                                    .into(imgProfilePicture);
                        }

                        // Bookings info (placeholder for now)
                        tvBookingsInfo.setText("You have no active bookings");
                    }
                } else {
                    Toast.makeText(ProfileClientActivity.this,
                            "Failed to load profile data", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void openGalleryForProfilePicture() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_PROFILE_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();

            if (requestCode == PICK_PROFILE_IMAGE) {
                uploadProfilePicture(imageUri);
            }
        }
    }

    private void uploadProfilePicture(Uri imageUri) {
        Toast.makeText(this, "Uploading profile picture...", Toast.LENGTH_SHORT).show();

        StorageReference profilePicRef = storageRef.child("profile_pictures/" + userId + ".jpg");

        profilePicRef.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get download URL
                        profilePicRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri downloadUri) {
                                String downloadUrl = downloadUri.toString();

                                // Update database
                                refCustomer.child(userId).child("profilePictureUrl").setValue(downloadUrl)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(ProfileClientActivity.this,
                                                            "Profile picture updated!", Toast.LENGTH_SHORT).show();

                                                    // Display updated image
                                                    Glide.with(ProfileClientActivity.this)
                                                            .load(downloadUrl)
                                                            .circleCrop()
                                                            .into(imgProfilePicture);
                                                }
                                            }
                                        });
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileClientActivity.this,
                            "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
