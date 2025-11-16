package com.example.findmyworker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.RelativeLayout;

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

import java.util.ArrayList;

public class ProfileWorkerActivity extends AppCompatActivity {

    // UI Elements - Profile Section
    ImageButton imgProfilePicture;
    ImageButton btnEditProfilePicture;
    TextView tvUserName;
    TextView tvPhoneNumber;
    TextView tvSpecialties;

    // UI Elements - Portfolio Section
    Button btnAddPortfolioPhoto;
    HorizontalScrollView portfolioScrollView;
    LinearLayout portfolioImagesContainer;

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
    Customer currentWorker;
    ArrayList<String> portfolioUrls;

    // For image upload
    private static final int PICK_PROFILE_IMAGE = 1;
    private static final int PICK_PORTFOLIO_IMAGE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_worker);

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

        portfolioUrls = new ArrayList<>();

        // Initialize UI elements
        initializeViews();

        // Load user data
        loadWorkerData();

        // Set click listeners
        setClickListeners();
    }

    private void initializeViews() {
        // Profile section
        imgProfilePicture = findViewById(R.id.imgProfilePicture);
        btnEditProfilePicture = findViewById(R.id.btnEditProfilePicture);
        tvUserName = findViewById(R.id.tvUserName);
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        tvSpecialties = findViewById(R.id.tvSpecialties);

        // Portfolio section
        btnAddPortfolioPhoto = findViewById(R.id.btnAddPortfolioPhoto);
        portfolioScrollView = findViewById(R.id.portfolioScrollView);
        portfolioImagesContainer = findViewById(R.id.portfolioImagesContainer);

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
                Toast.makeText(ProfileWorkerActivity.this, "View full profile picture", Toast.LENGTH_SHORT).show();
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

        // Add portfolio photo button
        btnAddPortfolioPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGalleryForPortfolio();
            }
        });

        // Bottom navigation
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProfileWorkerActivity.this, "Already on Home", Toast.LENGTH_SHORT).show();
            }
        });

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProfileWorkerActivity.this, "Search coming soon", Toast.LENGTH_SHORT).show();
            }
        });

        btnChats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProfileWorkerActivity.this, "Chats coming soon", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadWorkerData() {
        refCustomer.child(userId).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful() && task.getResult().exists()) {
                    currentWorker = task.getResult().getValue(Customer.class);

                    if (currentWorker != null) {
                        // Display user info
                        tvUserName.setText(currentWorker.getName());
                        tvPhoneNumber.setText(currentWorker.getPhone());
                        tvSpecialties.setText(currentWorker.getSpecialties());

                        // Load profile picture
                        if (currentWorker.getProfilePictureUrl() != null &&
                                !currentWorker.getProfilePictureUrl().isEmpty()) {
                            Glide.with(ProfileWorkerActivity.this)
                                    .load(currentWorker.getProfilePictureUrl())
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_default_profile)
                                    .into(imgProfilePicture);
                        }

                        // Load portfolio images
                        if (currentWorker.getPortfolioUrls() != null) {
                            portfolioUrls = currentWorker.getPortfolioUrls();
                            displayPortfolioImages();
                        }
                    }
                } else {
                    Toast.makeText(ProfileWorkerActivity.this,
                            "Failed to load profile data", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void displayPortfolioImages() {
        // Clear existing images
        portfolioImagesContainer.removeAllViews();

        // Add each portfolio image
        for (int i = 0; i < portfolioUrls.size(); i++) {
            final String imageUrl = portfolioUrls.get(i);
            final int position = i;

            // Create RelativeLayout for image + delete button
            RelativeLayout imageLayout = new RelativeLayout(this);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    dpToPx(120), dpToPx(120)
            );
            layoutParams.setMarginEnd(dpToPx(12));
            imageLayout.setLayoutParams(layoutParams);

            // Create ImageView
            ImageView imageView = new ImageView(this);
            RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT
            );
            imageView.setLayoutParams(imageParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            // Load image with Glide
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_add_photo)
                    .into(imageView);

            // Create delete button
            ImageButton deleteBtn = new ImageButton(this);
            RelativeLayout.LayoutParams deleteParams = new RelativeLayout.LayoutParams(
                    dpToPx(24), dpToPx(24)
            );
            deleteParams.addRule(RelativeLayout.ALIGN_PARENT_END);
            deleteParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            deleteParams.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
            deleteBtn.setLayoutParams(deleteParams);
            deleteBtn.setBackgroundResource(R.drawable.circle_button_background);
            deleteBtn.setImageResource(R.drawable.ic_delete);
            deleteBtn.setScaleType(ImageView.ScaleType.CENTER);
            deleteBtn.setElevation(dpToPx(4));

            // Delete button click listener
            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deletePortfolioImage(position, imageUrl);
                }
            });

            // Add views to layout
            imageLayout.addView(imageView);
            imageLayout.addView(deleteBtn);

            // Add to container
            portfolioImagesContainer.addView(imageLayout);
        }
    }

    private void openGalleryForProfilePicture() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_PROFILE_IMAGE);
    }

    private void openGalleryForPortfolio() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_PORTFOLIO_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();

            if (requestCode == PICK_PROFILE_IMAGE) {
                uploadProfilePicture(imageUri);
            } else if (requestCode == PICK_PORTFOLIO_IMAGE) {
                uploadPortfolioImage(imageUri);
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
                                                    Toast.makeText(ProfileWorkerActivity.this,
                                                            "Profile picture updated!", Toast.LENGTH_SHORT).show();

                                                    // Display updated image
                                                    Glide.with(ProfileWorkerActivity.this)
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
                    Toast.makeText(ProfileWorkerActivity.this,
                            "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadPortfolioImage(Uri imageUri) {
        Toast.makeText(this, "Uploading portfolio image...", Toast.LENGTH_SHORT).show();

        String imageId = System.currentTimeMillis() + "";
        StorageReference portfolioRef = storageRef.child("portfolio/" + userId + "/" + imageId + ".jpg");

        portfolioRef.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get download URL
                        portfolioRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri downloadUri) {
                                String downloadUrl = downloadUri.toString();

                                // Add to portfolioUrls list
                                portfolioUrls.add(downloadUrl);

                                // Update database
                                refCustomer.child(userId).child("portfolioUrls").setValue(portfolioUrls)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(ProfileWorkerActivity.this,
                                                            "Portfolio image added!", Toast.LENGTH_SHORT).show();

                                                    // Refresh display
                                                    displayPortfolioImages();
                                                }
                                            }
                                        });
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileWorkerActivity.this,
                            "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deletePortfolioImage(int position, String imageUrl) {
        // Remove from local list
        portfolioUrls.remove(position);

        // Update database
        refCustomer.child(userId).child("portfolioUrls").setValue(portfolioUrls)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(ProfileWorkerActivity.this,
                                    "Image deleted", Toast.LENGTH_SHORT).show();

                            // Refresh display
                            displayPortfolioImages();

                            // Optional: Delete from Storage as well
                            // StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
                            // imageRef.delete();
                        }
                    }
                });
    }

    // Helper method to convert dp to pixels
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}