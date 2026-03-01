package com.example.findmyworker;

import static com.example.findmyworker.FBRef.refAuth;
import static com.example.findmyworker.FBRef.refCustomer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private static final int REQUEST_CODE_AUTOCOMPLETE_LOCATION = 1001;
    private static final String USER_TYPE_WORKER = "worker";
    private static final String USER_TYPE_CLIENT = "client";

    MaterialButtonToggleGroup toggleUserType;
    TextInputEditText etEmail, etPassword, etConfirmPassword, etName, etPhone;
    LinearLayout layoutSpecialties;
    CheckBox cbGardening, cbPaint, cbSmallFixes, cbCleaning, cbTransferring;
    Button btnRegister, btnToLogin, btnSelectLocation;
    TextView tvMessage, tvSelectedLocation;

    boolean isWorker = false;
    double selectedLat = 0.0;
    double selectedLon = 0.0;
    String selectedAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeViews();
        setupListeners();
        initializePlacesSDK();
    }

    private void initializeViews() {
        toggleUserType = findViewById(R.id.toggleUserType);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        layoutSpecialties = findViewById(R.id.layoutSpecialties);
        cbGardening = findViewById(R.id.cbGardening);
        cbPaint = findViewById(R.id.cbPaint);
        cbSmallFixes = findViewById(R.id.cbSmallFixes);
        cbCleaning = findViewById(R.id.cbCleaning);
        cbTransferring = findViewById(R.id.cbTransferring);
        btnRegister = findViewById(R.id.btnRegister);
        tvMessage = findViewById(R.id.tvMessage);
        btnToLogin = findViewById(R.id.btnToLogin);
        btnSelectLocation = findViewById(R.id.btnGetLocation);
        tvSelectedLocation = findViewById(R.id.tvLocationStatus);
    }

    private void initializePlacesSDK() {
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.MAPS_API_KEY));
        }
    }

    private void setupListeners() {
        toggleUserType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                isWorker = (checkedId == R.id.btnTypeWorker);
                layoutSpecialties.setVisibility(isWorker ? View.VISIBLE : View.GONE);
                if (!isWorker) clearCheckboxes();
            }
        });

        btnSelectLocation.setOnClickListener(v -> launchPlacesAutocomplete());
        btnRegister.setOnClickListener(v -> registerUser());
        btnToLogin.setOnClickListener(v -> finish());
    }

    private void launchPlacesAutocomplete() {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(this);
        startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE_LOCATION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        if (requestCode == REQUEST_CODE_AUTOCOMPLETE_LOCATION) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            if (place.getLatLng() != null && place.getAddress() != null) {
                selectedLat = place.getLatLng().latitude;
                selectedLon = place.getLatLng().longitude;
                selectedAddress = place.getAddress();
                tvSelectedLocation.setText("Location: " + selectedAddress);
            }
        }
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        // 1. Password length validation (Max 20 chars)
        if (password.length() > 20) {
            tvMessage.setText("Password cannot exceed 20 characters.");
            return;
        }

        // 2. Name validation: Max 20 chars AND letters/spaces only using Regex
        if (name.length() > 20) {
            tvMessage.setText("Name cannot exceed 20 characters.");
            return;
        }
        if (!name.matches("^[a-zA-Z\\s]+$")) {
            tvMessage.setText("Name can only contain letters.");
            return;
        }

        // 3. Basic empty field and password match validation
        if (email.isEmpty() || password.isEmpty() || !password.equals(confirmPassword)) {
            tvMessage.setText("Please check empty fields and ensure passwords match.");
            return;
        }

        btnRegister.setEnabled(false);
        tvMessage.setText("Registering...");

        refAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String userId = refAuth.getCurrentUser().getUid();
                Customer newUser = new Customer();
                newUser.setUserId(userId);
                newUser.setEmail(email);
                newUser.setName(name);
                newUser.setPhone(phone);
                newUser.setUserType(isWorker ? USER_TYPE_WORKER : USER_TYPE_CLIENT);

                if (isWorker) {
                    newUser.setLatitude(selectedLat);
                    newUser.setLongitude(selectedLon);
                    newUser.setAddress(selectedAddress);

                    ArrayList<String> specs = new ArrayList<>();
                    if (cbGardening.isChecked()) specs.add("Gardening");
                    if (cbPaint.isChecked()) specs.add("Paint");
                    if (cbSmallFixes.isChecked()) specs.add("Small Fixes");
                    if (cbCleaning.isChecked()) specs.add("Cleaning");
                    if (cbTransferring.isChecked()) specs.add("Transferring");
                    newUser.setSpecialties(String.join(", ", specs));
                }

                refCustomer.child(userId).setValue(newUser).addOnCompleteListener(dbTask -> {
                    btnRegister.setEnabled(true);
                    if (dbTask.isSuccessful()) finish();
                });
            } else {
                btnRegister.setEnabled(true);
                tvMessage.setText("Auth error: " + task.getException().getMessage());
            }
        });
    }

    private void clearCheckboxes() {
        cbGardening.setChecked(false);
        cbPaint.setChecked(false);
        cbSmallFixes.setChecked(false);
        cbCleaning.setChecked(false);
        cbTransferring.setChecked(false);
    }
}