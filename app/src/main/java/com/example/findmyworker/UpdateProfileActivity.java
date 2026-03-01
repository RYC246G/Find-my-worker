package com.example.findmyworker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UpdateProfileActivity extends AppCompatActivity {
    private static final int AUTOCOMPLETE_REQUEST_CODE = 1002;

    TextInputEditText etName, etPhone;
    MaterialCheckBox cbGardening, cbPaint, cbSmallFixes, cbCleaning, cbTransferring;
    Button btnSave, btnUpdateLocation;
    TextView tvLocationStatus;

    DatabaseReference refCustomer;
    String userId;

    double updatedLat = 0.0;
    double updatedLon = 0.0;
    String updatedAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        refCustomer = FirebaseDatabase.getInstance().getReference("Customer").child(userId);

        initializeViews();
        loadCurrentData();
        setupListeners();

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.MAPS_API_KEY));
        }
    }

    private void initializeViews() {
        etName = findViewById(R.id.etUpdateName);
        etPhone = findViewById(R.id.etUpdatePhone);
        cbGardening = findViewById(R.id.cbUpdateGardening);
        cbPaint = findViewById(R.id.cbUpdatePaint);
        cbSmallFixes = findViewById(R.id.cbUpdateSmallFixes);
        cbCleaning = findViewById(R.id.cbUpdateCleaning);
        cbTransferring = findViewById(R.id.cbUpdateTransferring);
        btnSave = findViewById(R.id.btnSaveUpdate);
        btnUpdateLocation = findViewById(R.id.btnUpdateLocation);
        tvLocationStatus = findViewById(R.id.tvUpdateLocationStatus);
    }

    private void loadCurrentData() {
        refCustomer.get().addOnSuccessListener(snapshot -> {
            Customer c = snapshot.getValue(Customer.class);
            if (c != null) {
                etName.setText(c.getName());
                etPhone.setText(c.getPhone());
                updatedLat = c.getLatitude();
                updatedLon = c.getLongitude();
                updatedAddress = c.getAddress();

                if (updatedAddress != null) tvLocationStatus.setText("Location: " + updatedAddress);

                if (c.getSpecialties() != null) {
                    String s = c.getSpecialties();
                    cbGardening.setChecked(s.contains("Gardening"));
                    cbPaint.setChecked(s.contains("Paint"));
                    cbSmallFixes.setChecked(s.contains("Small Fixes"));
                    cbCleaning.setChecked(s.contains("Cleaning"));
                    cbTransferring.setChecked(s.contains("Transferring"));
                }
            }
        });
    }

    private void setupListeners() {
        btnUpdateLocation.setOnClickListener(v -> {
            List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS);
            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(this);
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
        });

        btnSave.setOnClickListener(v -> saveProfile());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            if (place.getLatLng() != null) {
                updatedLat = place.getLatLng().latitude;
                updatedLon = place.getLatLng().longitude;
                updatedAddress = place.getAddress();
                tvLocationStatus.setText("Location Updated: " + updatedAddress);
            }
        }
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        // 1. Name validation: Max 20 chars AND letters/spaces only using Regex
        if (name.length() > 20) {
            Toast.makeText(this, "Name cannot exceed 20 characters.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!name.matches("^[a-zA-Z\\s]+$")) {
            Toast.makeText(this, "Name can only contain letters.", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<String> specs = new ArrayList<>();
        if (cbGardening.isChecked()) specs.add("Gardening");
        if (cbPaint.isChecked()) specs.add("Paint");
        if (cbSmallFixes.isChecked()) specs.add("Small Fixes");
        if (cbCleaning.isChecked()) specs.add("Cleaning");
        if (cbTransferring.isChecked()) specs.add("Transferring");

        refCustomer.child("name").setValue(name);
        refCustomer.child("phone").setValue(phone);
        refCustomer.child("latitude").setValue(updatedLat);
        refCustomer.child("longitude").setValue(updatedLon);
        refCustomer.child("address").setValue(updatedAddress);
        refCustomer.child("specialties").setValue(String.join(", ", specs))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}