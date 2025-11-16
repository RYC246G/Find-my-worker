package com.example.findmyworker;


import static com.example.findmyworker.FBRef.refAuth;
import static com.example.findmyworker.FBRef.refCustomer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

import java.util.ArrayList;

public class RegisterActivity extends AppCompatActivity {

    // UI Elements
    Switch switchUserType;
    EditText etEmail, etPassword, etConfirmPassword, etName, etPhone;
    LinearLayout layoutSpecialties;
    CheckBox cbGardening, cbPaint, cbSmallFixes, cbCleaning, cbTransferring;
    Button btnRegister, btnToLogin;
    TextView tvMessage;

    // User type flag
    boolean isWorker = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize UI elements
        switchUserType = findViewById(R.id.switchUserType);
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

        // Set switch listener to show/hide specialties
        switchUserType.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isWorker = isChecked;

                if (isChecked) {
                    // Show specialties for Worker
                    layoutSpecialties.setVisibility(View.VISIBLE);
                } else {
                    // Hide specialties for Client
                    layoutSpecialties.setVisibility(View.GONE);
                    // Uncheck all boxes when hiding
                    clearCheckboxes();
                }
            }
        });

        // Set button click listener
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        btnToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void registerUser() {
        // Get input values
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        // Validate inputs
        if (email.isEmpty()) {
            tvMessage.setText("Please enter email");
            return;
        }


        if (password.isEmpty()) {
            tvMessage.setText("Please enter password");
            return;
        }

        if (password.length() < 6) {
            tvMessage.setText("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            tvMessage.setText("Passwords do not match");
            return;
        }

        if (name.isEmpty()) {
            tvMessage.setText("Please enter your name");
            return;
        }

        if (phone.isEmpty()) {
            tvMessage.setText("Please enter phone number");
            return;
        }
        if (!isValidIsraeliPhone(phone)) {
            tvMessage.setText("Please enter a valid Israeli phone number");
            return;
        }

        // If worker, validate specialties
        if (isWorker) {
            if (!cbGardening.isChecked() && !cbPaint.isChecked() &&
                    !cbSmallFixes.isChecked() && !cbCleaning.isChecked() &&
                    !cbTransferring.isChecked()) {
                tvMessage.setText("Please select at least one specialty");
                return;
            }
        }

        // Disable button to prevent multiple clicks
        btnRegister.setEnabled(false);
        tvMessage.setText("Registering...");

        // Create user in Firebase Authentication
        refAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Get user ID
                            String userId = refAuth.getCurrentUser().getUid();

                            // Create Customer object
                            Customer newUser = new Customer();
                            newUser.setUserId(userId);
                            newUser.setEmail(email);
                            newUser.setName(name);
                            newUser.setPhone(phone);

                            // Set user type
                            if (isWorker) {
                                newUser.setUserType("worker");

                                // Get selected specialties
                                ArrayList<String> selectedSpecialties = new ArrayList<>();
                                if (cbGardening.isChecked()) selectedSpecialties.add("Gardening");
                                if (cbPaint.isChecked()) selectedSpecialties.add("Paint");
                                if (cbSmallFixes.isChecked()) selectedSpecialties.add("Small Fixes");
                                if (cbCleaning.isChecked()) selectedSpecialties.add("Cleaning");
                                if (cbTransferring.isChecked()) selectedSpecialties.add("Transfering");

                                // Convert to comma-separated string
                                String specialties = String.join(", ", selectedSpecialties);
                                newUser.setSpecialties(specialties);
                            } else {
                                newUser.setUserType("client");
                            }

                            // Save to Firebase Realtime Database
                            refCustomer.child(userId).setValue(newUser)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            btnRegister.setEnabled(true);

                                            if (task.isSuccessful()) {
                                                String userTypeText = isWorker ? "Worker" : "Client";
                                                tvMessage.setText("Registration successful!");
                                                Toast.makeText(RegisterActivity.this,
                                                        "Welcome " + name + " (" + userTypeText + ")!",
                                                        Toast.LENGTH_SHORT).show();

                                                // Clear fields
                                                clearFields();

                                                // Optional: Navigate based on user type
                                                // if (isWorker) {
                                                //     Intent intent = new Intent(RegisterActivity.this, WorkerMainActivity.class);
                                                //     startActivity(intent);
                                                // } else {
                                                //     Intent intent = new Intent(RegisterActivity.this, ClientMainActivity.class);
                                                //     startActivity(intent);
                                                // }
                                                // finish();
                                            } else {
                                                tvMessage.setText("Failed to save user data: " +
                                                        task.getException().getMessage());
                                            }
                                        }
                                    });
                        } else {
                            btnRegister.setEnabled(true);
                            tvMessage.setText("Registration failed: " +
                                    task.getException().getMessage());
                        }
                    }
                });
    }

    private void clearFields() {
        etEmail.setText("");
        etPassword.setText("");
        etConfirmPassword.setText("");
        etName.setText("");
        etPhone.setText("");
        clearCheckboxes();
        switchUserType.setChecked(false);
    }

    private void clearCheckboxes() {
        cbGardening.setChecked(false);
        cbPaint.setChecked(false);
        cbSmallFixes.setChecked(false);
        cbCleaning.setChecked(false);
        cbTransferring.setChecked(false);
    }

    private boolean isValidIsraeliPhone(String phone) {
        // Remove all spaces, dashes, and other non-digit characters
        String cleanPhone = phone.replaceAll("[^0-9]", "");

        // Israeli phone numbers can be:
        // Remove +972 or 972 prefix if exists
        if (cleanPhone.startsWith("972")) {
            cleanPhone = "0" + cleanPhone.substring(3);
        }

        // Check if it's a valid Israeli number
        if (cleanPhone.length() == 10 && cleanPhone.startsWith("05")) {
            // Valid mobile number (05X-XXXXXXX)
            return true;
        } else if (cleanPhone.length() == 9 && cleanPhone.startsWith("0")) {
            // Valid landline (0X-XXXXXXX where X is 2,3,4,8,9)
            char secondDigit = cleanPhone.charAt(1);
            return secondDigit == '2' || secondDigit == '3' || secondDigit == '4' ||
                    secondDigit == '8' || secondDigit == '9';
        }

        return false;
    }
}
