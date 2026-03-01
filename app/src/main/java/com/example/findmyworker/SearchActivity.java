package com.example.findmyworker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.slider.Slider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchActivity extends AppCompatActivity {

    private RecyclerView rvSearchResults;
    private Button btnOpenSearchPopup;
    private BottomNavigationView bottomNavigation;
    private TextView tvUserCoords;
    private TextView tvNoResults;

    private ArrayList<Customer> allWorkers = new ArrayList<>();
    private ArrayList<Customer> filteredWorkers = new ArrayList<>();
    private WorkerAdapter adapter;

    private FusedLocationProviderClient fusedLocationClient;
    private Location clientLocation;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private String lastQuery = "";
    private ArrayList<String> lastSelectedSpecs = new ArrayList<>();
    private int lastSortOption = 1; // 0: Ranking, 1: Default, 2: Distance
    private String lastMaxDistance = "20";
    private boolean isDistanceFilterActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivitiesIfAvailable(this.getApplication());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initializeLocationServices();
        initializeViews();
        setupFirebaseListener();
        setupNavigation();
    }

    private void initializeLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) getClientLocation();
                });
        checkPermissionAndGetLocation();
    }

    private void initializeViews() {
        rvSearchResults = findViewById(R.id.rvSearchResults);
        btnOpenSearchPopup = findViewById(R.id.btnOpenSearchPopup);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        tvNoResults = findViewById(R.id.tvNoResults);

        tvUserCoords = new TextView(this);
        tvUserCoords.setTextSize(10);
        tvUserCoords.setPadding(20, 0, 0, 0);
        tvUserCoords.setVisibility(View.GONE);

        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WorkerAdapter(this, filteredWorkers, clientLocation, worker -> {
            Intent intent = new Intent(SearchActivity.this, ProfileWorkerActivity.class);
            intent.putExtra("workerId", worker.getUserId());
            startActivity(intent);
        });
        rvSearchResults.setAdapter(adapter);

        btnOpenSearchPopup.setOnClickListener(v -> showSearchFiltersDialog());
    }

    private void setupFirebaseListener() {
        FBRef.refCustomer.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allWorkers.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Customer worker = data.getValue(Customer.class);
                    if (worker != null) allWorkers.add(worker);
                }
                applyFiltersAndSort();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SearchActivity.this, "Database error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_search);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                redirectToOwnProfile();
                return true;
            } else if (itemId == R.id.nav_search) {
                return true;
            } else if (itemId == R.id.nav_chats) {
                Toast.makeText(this, "Chats coming soon", Toast.LENGTH_SHORT).show();
                return false;
            }
            return false;
        });
    }

    private void redirectToOwnProfile() {
        String currentLoggedInUserId = FirebaseAuth.getInstance().getUid();
        if (currentLoggedInUserId == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        FBRef.refCustomer.child(currentLoggedInUserId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                Customer user = task.getResult().getValue(Customer.class);
                if (user != null) {
                    Intent intent;
                    if ("worker".equals(user.getUserType())) {
                        intent = new Intent(this, ProfileWorkerActivity.class);
                    } else {
                        intent = new Intent(this, ProfileClientActivity.class);
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }

    private void checkPermissionAndGetLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            getClientLocation();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void getClientLocation() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null && (location.getLatitude() != 0.0 || location.getLongitude() != 0.0)) {
                    clientLocation = location;
                    tvUserCoords.setText(String.format(Locale.getDefault(), "My Location: %.4f, %.4f", location.getLatitude(), location.getLongitude()));
                    tvUserCoords.setVisibility(View.VISIBLE);
                    if (adapter != null) {
                        adapter.setClientLocation(clientLocation);
                    }
                    applyFiltersAndSort();
                }
            });
        } catch (SecurityException e) {
            Log.e("SearchActivity", "Location error", e);
        }
    }

    private void showSearchFiltersDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_search_filters, null);
        dialog.setContentView(view);

        // Standard Filter Views
        EditText etPopupSearch = view.findViewById(R.id.etPopupSearch);
        Chip chipGardening = view.findViewById(R.id.chipGardening);
        Chip chipPaint = view.findViewById(R.id.chipPaint);
        Chip chipSmallFixes = view.findViewById(R.id.chipSmallFixes);
        Chip chipCleaning = view.findViewById(R.id.chipCleaning);
        Chip chipTransferring = view.findViewById(R.id.chipTransferring);
        Slider sliderDistance = view.findViewById(R.id.sliderDistance);
        MaterialButtonToggleGroup toggleSort = view.findViewById(R.id.toggleSort);
        Button btnApply = view.findViewById(R.id.btnApplyFilters);

        // New AI Views
        EditText etAiProblem = view.findViewById(R.id.etAiProblem);
        Button btnAskAI = view.findViewById(R.id.btnAskAI);

        // AI Button Logic
        btnAskAI.setOnClickListener(v -> {
            String problem = etAiProblem.getText().toString().trim();
            if (problem.isEmpty()) {
                Toast.makeText(this, "Please describe your problem first.", Toast.LENGTH_SHORT).show();
                return;
            }

            btnAskAI.setEnabled(false);
            btnAskAI.setText("AI is thinking...");

            askAIToSelectChips(problem, chipGardening, chipPaint, chipSmallFixes, chipCleaning, chipTransferring, btnAskAI);
        });

        // Restore previous state
        etPopupSearch.setText(lastQuery);
        chipGardening.setChecked(lastSelectedSpecs.contains("Gardening"));
        chipPaint.setChecked(lastSelectedSpecs.contains("Paint"));
        chipSmallFixes.setChecked(lastSelectedSpecs.contains("Small Fixes"));
        chipCleaning.setChecked(lastSelectedSpecs.contains("Cleaning"));
        chipTransferring.setChecked(lastSelectedSpecs.contains("Transferring"));

        try {
            sliderDistance.setValue(Float.parseFloat(lastMaxDistance));
        } catch (Exception e) {
            sliderDistance.setValue(20.0f);
        }

        if (lastSortOption == 0) toggleSort.check(R.id.btnSortRankings);
        else if (lastSortOption == 2) toggleSort.check(R.id.btnSortDistance);
        else toggleSort.check(R.id.btnSortDefault);

        btnApply.setOnClickListener(v -> {
            isDistanceFilterActive = true;
            lastQuery = etPopupSearch.getText().toString().trim().toLowerCase();

            lastSelectedSpecs.clear();
            if (chipGardening.isChecked()) lastSelectedSpecs.add("Gardening");
            if (chipPaint.isChecked()) lastSelectedSpecs.add("Paint");
            if (chipSmallFixes.isChecked()) lastSelectedSpecs.add("Small Fixes");
            if (chipCleaning.isChecked()) lastSelectedSpecs.add("Cleaning");
            if (chipTransferring.isChecked()) lastSelectedSpecs.add("Transferring");

            lastMaxDistance = String.valueOf((int) sliderDistance.getValue());

            int checkedSortId = toggleSort.getCheckedButtonId();
            if (checkedSortId == R.id.btnSortRankings) lastSortOption = 0;
            else if (checkedSortId == R.id.btnSortDistance) lastSortOption = 2;
            else lastSortOption = 1;

            applyFiltersAndSort();
            dialog.dismiss();
        });

        dialog.show();
    }

    // --- AI LOGIC METHODS ---

    private void askAIToSelectChips(String problem, Chip c1, Chip c2, Chip c3, Chip c4, Chip c5, Button btnAskAI) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            String aiResponse = makeGeminiRequest(problem);

            mainHandler.post(() -> {
                btnAskAI.setEnabled(true);
                btnAskAI.setText("Auto-Select Specialties");

                if (aiResponse != null && !aiResponse.isEmpty()) {
                    c1.setChecked(false); c2.setChecked(false); c3.setChecked(false); c4.setChecked(false); c5.setChecked(false);

                    if (aiResponse.contains("Gardening")) c1.setChecked(true);
                    if (aiResponse.contains("Paint")) c2.setChecked(true);
                    if (aiResponse.contains("Small Fixes")) c3.setChecked(true);
                    if (aiResponse.contains("Cleaning")) c4.setChecked(true);
                    if (aiResponse.contains("Transferring")) c5.setChecked(true);

                    Toast.makeText(SearchActivity.this, "AI selected: " + aiResponse, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(SearchActivity.this, "AI could not determine the specialty.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private String makeGeminiRequest(String promptText) {
        // TODO: PASTE YOUR ACTUAL API KEY HERE
        String apiKey = "AIzaSyAPwWUu5vKI0Zr_TrlH8gxdnJYRJzrRWdo";

        // UPDATED: Pointing to the official 'v1' production API using the standard 'gemini-1.5-flash' model
        String endpoint = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + apiKey;
        try {
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Stricter prompt to prevent markdown formatting
            String instruction = "You are an assistant for a home services app. The user has this problem: '" + promptText + "'. " +
                    "Which of these exact specialties are needed to fix it: Gardening, Paint, Small Fixes, Cleaning, Transferring. " +
                    "Reply ONLY with the exact matching specialties separated by commas. Do not write sentences. Do not use markdown. If none apply, reply 'None'.";

            JSONObject textPart = new JSONObject().put("text", instruction);
            JSONObject parts = new JSONObject().put("parts", new JSONArray().put(textPart));
            JSONObject payload = new JSONObject().put("contents", new JSONArray().put(parts));

            OutputStream os = conn.getOutputStream();
            os.write(payload.toString().getBytes("UTF-8"));
            os.close();

            // CHECK FOR HTTP ERRORS
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                StringBuilder errorResponse = new StringBuilder();
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    errorResponse.append(errorLine.trim());
                }
                Log.e("GeminiAI", "API HTTP Error: " + responseCode + " - " + errorResponse.toString());
                return null;
            }

            // READ SUCCESSFUL RESPONSE
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }

            JSONObject jsonResponse = new JSONObject(response.toString());
            String rawAiText = jsonResponse.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text").trim();

            Log.d("GeminiAI", "Raw AI Reply: " + rawAiText);
            return rawAiText;

        } catch (Exception e) {
            Log.e("GeminiAI", "Code Crash Error: ", e);
            return null;
        }
    }

    // --- STANDARD FILTER LOGIC ---

    private void applyFiltersAndSort() {
        filteredWorkers.clear();
        int maxDist;
        try {
            maxDist = Integer.parseInt(lastMaxDistance);
        } catch (Exception e) {
            maxDist = 20;
        }

        for (Customer worker : allWorkers) {
            if (worker.getUserType() == null || !worker.getUserType().equalsIgnoreCase("worker")) continue;

            boolean matchesQuery = lastQuery.isEmpty() ||
                    (worker.getName() != null && worker.getName().toLowerCase().contains(lastQuery)) ||
                    (worker.getSpecialties() != null && worker.getSpecialties().toLowerCase().contains(lastQuery));

            boolean matchesAllSpecs = true;
            if (!lastSelectedSpecs.isEmpty()) {
                String workerSpecs = worker.getSpecialties() != null ? worker.getSpecialties() : "";
                for (String spec : lastSelectedSpecs) {
                    if (!workerSpecs.contains(spec)) {
                        matchesAllSpecs = false;
                        break;
                    }
                }
            }

            boolean withinDistance = true;
            if (isDistanceFilterActive && clientLocation != null) {
                if (worker.getLatitude() != 0.0 && worker.getLongitude() != 0.0) {
                    float[] results = new float[1];
                    Location.distanceBetween(clientLocation.getLatitude(), clientLocation.getLongitude(),
                            worker.getLatitude(), worker.getLongitude(), results);
                    double distanceInKm = results[0] / 1000.0;
                    if (distanceInKm > maxDist) withinDistance = false;
                } else {
                    withinDistance = false;
                }
            }

            if (matchesQuery && matchesAllSpecs && withinDistance) {
                filteredWorkers.add(worker);
            }
        }

        sortResults();
        updateDisplayList();
    }

    private void sortResults() {
        if (lastSortOption == 0) {
            Collections.sort(filteredWorkers, (c1, c2) -> Double.compare(c2.getRanking(), c1.getRanking()));
        } else if (lastSortOption == 2 && clientLocation != null) {
            Collections.sort(filteredWorkers, (c1, c2) -> {
                boolean c1Has = c1.getLatitude() != 0.0;
                boolean c2Has = c2.getLatitude() != 0.0;
                if (c1Has && !c2Has) return -1;
                if (!c1Has && c2Has) return 1;
                if (!c1Has && !c2Has) return 0;

                float[] d1 = new float[1], d2 = new float[1];
                Location.distanceBetween(clientLocation.getLatitude(), clientLocation.getLongitude(), c1.getLatitude(), c1.getLongitude(), d1);
                Location.distanceBetween(clientLocation.getLatitude(), clientLocation.getLongitude(), c2.getLatitude(), c2.getLongitude(), d2);
                return Float.compare(d1[0], d2[0]);
            });
        }
    }

    private void updateDisplayList() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            tvNoResults.setVisibility(filteredWorkers.isEmpty() ? View.VISIBLE : View.GONE);
            rvSearchResults.setVisibility(filteredWorkers.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }
}