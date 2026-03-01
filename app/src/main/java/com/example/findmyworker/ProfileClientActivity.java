package com.example.findmyworker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

public class ProfileClientActivity extends AppCompatActivity {

    private static final String TAG = "ProfileClientActivity";
    private static final int REQUEST_CODE_PICK_PROFILE_IMAGE = 1;
    private static final int REQUEST_CODE_CAMERA = 3;

    private static final String NODE_CUSTOMER = "Customer";
    private static final String PATH_PROFILE_PICTURES = "profile_pictures/";
    private static final String CHILD_PROFILE_PICTURE_URL = "profilePictureUrl";

    private ImageView imgProfilePicture;
    private View btnEditProfilePicture;
    private ImageButton btnMenu;
    private TextView tvUserName, tvEmail, tvPhoneNumber, tvBookingsInfo;

    private BottomNavigationView bottomNavigation;

    private FirebaseAuth refAuth;
    private DatabaseReference refCustomer;
    private StorageReference storageRef;

    private String userId;
    private Customer currentClient;
    private Uri cameraImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_client);

        initializeFirebaseAndServices();

        if (refAuth.getCurrentUser() != null) {
            userId = refAuth.getCurrentUser().getUid();
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        initializeViews();
        loadClientData();
        setClickListeners();
        setupBottomNavigation();
    }

    private void initializeFirebaseAndServices() {
        refAuth = FirebaseAuth.getInstance();
        refCustomer = FirebaseDatabase.getInstance().getReference(NODE_CUSTOMER);
        storageRef = FirebaseStorage.getInstance().getReference();
    }

    private void initializeViews() {
        imgProfilePicture = findViewById(R.id.imgProfilePicture);
        btnEditProfilePicture = findViewById(R.id.btnEditProfilePicture);
        btnMenu = findViewById(R.id.btnMenu);
        tvUserName = findViewById(R.id.tvUserName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        tvBookingsInfo = findViewById(R.id.tvBookingsInfo);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_search) {
                startActivity(new Intent(this, SearchActivity.class));
                return true;
            } else if (itemId == R.id.nav_chats) {
                Toast.makeText(this, "Chats coming soon", Toast.LENGTH_SHORT).show();
                return false;
            }
            return false;
        });
    }

    private void setClickListeners() {
        imgProfilePicture.setOnClickListener(v -> {
            String url = (currentClient != null) ? currentClient.getProfilePictureUrl() : null;
            showFullScreenImage(url);
        });

        btnEditProfilePicture.setOnClickListener(v -> showImageSourceDialog());
        btnMenu.setOnClickListener(this::showSettingsMenu);
    }

    private void showSettingsMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add(0, 1, 0, "Settings").setIcon(R.drawable.ic_settings);
        popup.getMenu().add(0, 2, 1, "Log Out").setIcon(R.drawable.ic_logout);

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
                return true;
            } else if (item.getItemId() == 2) {
                refAuth.signOut();
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showFullScreenImage(String imageUrl) {
        final android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_full_screen_image);
        ImageView imgFullScreen = dialog.findViewById(R.id.imgFullScreen);
        if (imageUrl != null && !imageUrl.isEmpty()) Glide.with(this).load(imageUrl).placeholder(R.drawable.ic_default_profile).into(imgFullScreen);
        else imgFullScreen.setImageResource(R.drawable.ic_default_profile);
        dialog.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void loadClientData() {
        refCustomer.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentClient = snapshot.getValue(Customer.class);
                if (currentClient != null) {
                    tvUserName.setText(currentClient.getName());
                    tvEmail.setText("Email: " + currentClient.getEmail());
                    tvPhoneNumber.setText("Phone: " + currentClient.getPhone());
                    if (currentClient.getProfilePictureUrl() != null && !currentClient.getProfilePictureUrl().isEmpty()) {
                        Glide.with(ProfileClientActivity.this).load(currentClient.getProfilePictureUrl()).circleCrop().placeholder(R.drawable.ic_default_profile).into(imgProfilePicture);
                    } else {
                        imgProfilePicture.setImageResource(R.drawable.ic_default_profile);
                    }
                    tvBookingsInfo.setText("You have no active bookings");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showImageSourceDialog() {
        String[] options = {"Take Photo (Camera)", "Choose from Gallery"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Image Source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) launchCamera();
                    else launchGallery();
                }).show();
    }

    private Uri createCameraImageUri() {
        File imagePath = new File(getCacheDir(), "images");
        if (!imagePath.exists()) imagePath.mkdirs();
        File newFile = new File(imagePath, "camera_img_" + System.currentTimeMillis() + ".jpg");
        return FileProvider.getUriForFile(this, "com.example.findmyworker.fileprovider", newFile);
    }

    private void launchCamera() {
        cameraImageUri = createCameraImageUri();
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        startActivityForResult(intent, REQUEST_CODE_CAMERA);
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_PICK_PROFILE_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        if (requestCode == REQUEST_CODE_PICK_PROFILE_IMAGE && data != null && data.getData() != null) {
            showImageConfirmationDialog(data.getData(), false);
        } else if (requestCode == REQUEST_CODE_CAMERA) {
            showImageConfirmationDialog(cameraImageUri, true);
        }
    }

    private void showImageConfirmationDialog(Uri imageUri, boolean isFromCamera) {
        View view = getLayoutInflater().inflate(R.layout.dialog_image_confirmation, null);
        ImageView imgPreview = view.findViewById(R.id.imgPreview);
        Glide.with(this).load(imageUri).into(imgPreview);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Keep this picture?")
                .setView(view)
                .setPositiveButton("Accept (V)", (dialog, which) -> uploadProfilePicture(imageUri))
                .setNegativeButton("Retake (X)", (dialog, which) -> {
                    dialog.dismiss();
                    if (isFromCamera) launchCamera();
                    else launchGallery();
                })
                .setCancelable(false)
                .show();
    }

    private void uploadProfilePicture(Uri imageUri) {
        Toast.makeText(this, "Uploading profile picture...", Toast.LENGTH_SHORT).show();
        StorageReference profilePicRef = storageRef.child(PATH_PROFILE_PICTURES + userId + ".jpg");
        profilePicRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> profilePicRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    String downloadUrl = downloadUri.toString();
                    refCustomer.child(userId).child(CHILD_PROFILE_PICTURE_URL).setValue(downloadUrl)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(ProfileClientActivity.this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
                                    Glide.with(ProfileClientActivity.this).load(downloadUrl).circleCrop().into(imgProfilePicture);
                                }
                            });
                }))
                .addOnFailureListener(e -> Toast.makeText(ProfileClientActivity.this, "Upload failed", Toast.LENGTH_LONG).show());
    }
}