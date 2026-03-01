package com.example.findmyworker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileWorkerActivity extends AppCompatActivity {

    private static final String TAG = "ProfileWorkerActivity";
    private static final int REQUEST_CODE_PICK_GALLERY = 1;
    private static final int REQUEST_CODE_CAMERA = 3;
    private static final int REQUEST_CODE_AUTOCOMPLETE_LOCATION = 1001;

    private static final String NODE_CUSTOMER = "Customer";
    private static final String PATH_PROFILE_PICTURES = "profile_pictures/";
    private static final String PATH_PORTFOLIO = "portfolio/";
    private static final String CHILD_PROFILE_PICTURE_URL = "profilePictureUrl";
    private static final String CHILD_PORTFOLIO_URLS = "portfolioUrls";
    private static final String PROPERTY_LATITUDE = "latitude";
    private static final String PROPERTY_LONGITUDE = "longitude";
    private static final String PROPERTY_ADDRESS = "address";

    private static final String KEY_WORKER_ID = "workerId";
    private static final String USER_TYPE_WORKER = "worker";

    private ImageView imgProfilePicture;
    private View btnEditProfilePicture;
    private ImageButton btnMenu;
    private TextView tvUserName, tvPhoneNumber, tvSpecialties, tvLocationStatus;
    private Button btnEditProfile, btnAutoLocation, btnSearchAddress, btnAddPortfolioPhoto;
    private HorizontalScrollView portfolioScrollView;
    private LinearLayout portfolioImagesContainer;

    private BottomNavigationView bottomNavigation;

    private FirebaseAuth refAuth;
    private DatabaseReference refCustomer;
    private StorageReference storageRef;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private String userId;
    private String currentLoggedInUserId;
    private Customer currentWorker;
    private ArrayList<String> portfolioUrls;
    private boolean isOwnProfile = true;

    private Uri cameraImageUri;
    private boolean isSelectingProfilePic = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_worker);

        initializeFirebaseAndServices();

        String intentWorkerId = getIntent().getStringExtra(KEY_WORKER_ID);
        FirebaseUser user = refAuth.getCurrentUser();

        if (user != null) {
            currentLoggedInUserId = user.getUid();
            if (intentWorkerId != null && !intentWorkerId.equals(currentLoggedInUserId)) {
                userId = intentWorkerId;
                isOwnProfile = false;
            } else {
                userId = currentLoggedInUserId;
                isOwnProfile = true;
            }
        } else if (intentWorkerId != null) {
            userId = intentWorkerId;
            isOwnProfile = false;
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        initializeViews();
        setupLocationPermissionLauncher();
        loadWorkerData();
        setClickListeners();
        setupBottomNavigation();
        initializePlacesSDK();

        if (!isOwnProfile) {
            btnEditProfilePicture.setVisibility(View.GONE);
            btnEditProfile.setVisibility(View.GONE);
            btnAutoLocation.setVisibility(View.GONE);
            btnSearchAddress.setVisibility(View.GONE);
            btnAddPortfolioPhoto.setVisibility(View.GONE);
            btnMenu.setVisibility(View.GONE);
        }
    }

    private void initializeFirebaseAndServices() {
        refAuth = FirebaseAuth.getInstance();
        refCustomer = FirebaseDatabase.getInstance().getReference(NODE_CUSTOMER);
        storageRef = FirebaseStorage.getInstance().getReference();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        portfolioUrls = new ArrayList<>();
    }

    private void initializeViews() {
        imgProfilePicture = findViewById(R.id.imgProfilePicture);
        btnEditProfilePicture = findViewById(R.id.btnEditProfilePicture);
        btnMenu = findViewById(R.id.btnMenu);
        tvUserName = findViewById(R.id.tvUserName);
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        tvSpecialties = findViewById(R.id.tvSpecialties);
        tvLocationStatus = findViewById(R.id.tvLocationStatus);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnAutoLocation = findViewById(R.id.btnAutoLocation);
        btnSearchAddress = findViewById(R.id.btnSearchAddress);
        btnAddPortfolioPhoto = findViewById(R.id.btnAddPortfolioPhoto);
        portfolioScrollView = findViewById(R.id.portfolioScrollView);
        portfolioImagesContainer = findViewById(R.id.portfolioImagesContainer);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(isOwnProfile ? R.id.nav_home : R.id.nav_search);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                if (!isOwnProfile) redirectToOwnProfile();
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

    // --- UPDATED: Using BuildConfig to hide the Maps API Key ---
    private void initializePlacesSDK() {
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);
        }
    }

    private void setupLocationPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) getCurrentLocation();
                });
    }

    private void setClickListeners() {
        imgProfilePicture.setOnClickListener(v -> showFullScreenImage(currentWorker != null ? currentWorker.getProfilePictureUrl() : null));

        if (isOwnProfile) {
            btnEditProfilePicture.setOnClickListener(v -> showImageSourceDialog(true));
            btnAddPortfolioPhoto.setOnClickListener(v -> showImageSourceDialog(false));

            btnEditProfile.setOnClickListener(v -> startActivity(new Intent(this, UpdateProfileActivity.class)));
            btnAutoLocation.setOnClickListener(v -> checkLocationPermissionAndGetGPS());
            btnSearchAddress.setOnClickListener(v -> launchPlacesAutocomplete());
            btnMenu.setOnClickListener(this::showSettingsMenu);
        }
    }

    private void showSettingsMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add(0, 1, 0, "Settings").setIcon(R.drawable.ic_settings);
        popup.getMenu().add(0, 2, 1, "Log Out").setIcon(R.drawable.ic_logout);

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 2) {
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

    private void redirectToOwnProfile() {
        if (currentLoggedInUserId == null) return;
        refCustomer.child(currentLoggedInUserId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                Customer user = task.getResult().getValue(Customer.class);
                if (user != null) {
                    Intent intent;
                    if (USER_TYPE_WORKER.equals(user.getUserType())) intent = new Intent(this, ProfileWorkerActivity.class);
                    else intent = new Intent(this, ProfileClientActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }

    private void checkLocationPermissionAndGetGPS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) saveLocationToFirebase(location.getLatitude(), location.getLongitude(), "Current GPS Location");
        });
    }

    private void launchPlacesAutocomplete() {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(this);
        startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE_LOCATION);
    }

    private void saveLocationToFirebase(double lat, double lon, String addressName) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(PROPERTY_LATITUDE, lat);
        updates.put(PROPERTY_LONGITUDE, lon);
        updates.put(PROPERTY_ADDRESS, addressName);
        refCustomer.child(userId).updateChildren(updates).addOnSuccessListener(aVoid -> Toast.makeText(this, "Location updated!", Toast.LENGTH_SHORT).show());
    }

    private void loadWorkerData() {
        refCustomer.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentWorker = snapshot.getValue(Customer.class);
                if (currentWorker != null) {
                    updateProfileUI();
                    portfolioUrls = currentWorker.getPortfolioUrls() != null ? currentWorker.getPortfolioUrls() : new ArrayList<>();
                    displayPortfolioImages();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateProfileUI() {
        tvUserName.setText(currentWorker.getName());
        tvPhoneNumber.setText(currentWorker.getPhone());
        tvSpecialties.setText(currentWorker.getSpecialties());
        tvLocationStatus.setText("Location: " + (currentWorker.getAddress() != null ? currentWorker.getAddress() : "Not Set"));

        if (currentWorker.getProfilePictureUrl() != null && !currentWorker.getProfilePictureUrl().isEmpty()) {
            Glide.with(this).load(currentWorker.getProfilePictureUrl()).circleCrop().placeholder(R.drawable.ic_default_profile).into(imgProfilePicture);
        } else {
            imgProfilePicture.setImageResource(R.drawable.ic_default_profile);
        }
    }

    private void displayPortfolioImages() {
        portfolioImagesContainer.removeAllViews();
        for (int i = 0; i < portfolioUrls.size(); i++) {
            portfolioImagesContainer.addView(createPortfolioImageView(portfolioUrls.get(i), i));
        }
    }

    private View createPortfolioImageView(String url, int position) {
        RelativeLayout layout = new RelativeLayout(this);
        layout.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(120), dpToPx(120)));
        ((LinearLayout.LayoutParams) layout.getLayoutParams()).setMarginEnd(dpToPx(12));

        ImageView image = new ImageView(this);
        image.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(this).load(url).placeholder(R.drawable.ic_add_photo).into(image);
        image.setOnClickListener(v -> showFullScreenImage(url));
        layout.addView(image);

        if (isOwnProfile) {
            ImageButton delete = new ImageButton(this);
            RelativeLayout.LayoutParams delParams = new RelativeLayout.LayoutParams(dpToPx(24), dpToPx(24));
            delParams.addRule(RelativeLayout.ALIGN_PARENT_END);
            delParams.setMargins(0, dpToPx(4), dpToPx(4), 0);
            delete.setLayoutParams(delParams);
            delete.setImageResource(R.drawable.ic_delete);
            delete.setBackgroundResource(R.drawable.circle_button_background);

            delete.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Delete Picture")
                        .setMessage("Are you sure you want to delete this picture from your portfolio?")
                        .setPositiveButton("Delete", (dialog, which) -> deletePortfolioImage(position, url))
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .show();
            });
            layout.addView(delete);
        }
        return layout;
    }

    private void showImageSourceDialog(boolean forProfilePic) {
        isSelectingProfilePic = forProfilePic;
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
        startActivityForResult(intent, REQUEST_CODE_PICK_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        if (requestCode == REQUEST_CODE_AUTOCOMPLETE_LOCATION && data != null) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            if (place.getLatLng() != null && place.getAddress() != null) {
                saveLocationToFirebase(place.getLatLng().latitude, place.getLatLng().longitude, place.getAddress());
            }
        } else if (requestCode == REQUEST_CODE_PICK_GALLERY && data != null && data.getData() != null) {
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
                .setPositiveButton("Accept (V)", (dialog, which) -> {
                    String path = isSelectingProfilePic ?
                            PATH_PROFILE_PICTURES + userId + ".jpg" : PATH_PORTFOLIO + userId + "/" + System.currentTimeMillis() + ".jpg";
                    uploadImage(imageUri, path, isSelectingProfilePic);
                })
                .setNegativeButton("Retake (X)", (dialog, which) -> {
                    dialog.dismiss();
                    if (isFromCamera) launchCamera();
                    else launchGallery();
                })
                .setCancelable(false)
                .show();
    }

    private void uploadImage(Uri uri, String path, boolean isProfile) {
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();
        StorageReference ref = storageRef.child(path);
        ref.putFile(uri).addOnSuccessListener(task -> ref.getDownloadUrl().addOnSuccessListener(downloadUri -> {
            if (isProfile) {
                refCustomer.child(userId).child(CHILD_PROFILE_PICTURE_URL).setValue(downloadUri.toString());
            } else {
                portfolioUrls.add(downloadUri.toString());
                refCustomer.child(userId).child(CHILD_PORTFOLIO_URLS).setValue(portfolioUrls);
            }
        }));
    }

    private void deletePortfolioImage(int position, String url) {
        FirebaseStorage.getInstance().getReferenceFromUrl(url).delete().addOnSuccessListener(aVoid -> {
            portfolioUrls.remove(position);
            refCustomer.child(userId).child(CHILD_PORTFOLIO_URLS).setValue(portfolioUrls);
        });
    }

    private void showFullScreenImage(String url) {
        final android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_full_screen_image);
        ImageView view = dialog.findViewById(R.id.imgFullScreen);
        Glide.with(this).load(url != null && !url.isEmpty() ? url : R.drawable.ic_default_profile).into(view);
        dialog.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}