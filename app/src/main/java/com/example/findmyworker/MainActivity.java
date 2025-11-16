package com.example.findmyworker;

import static com.example.findmyworker.FBRef.refAuth;
import static com.example.findmyworker.FBRef.refCustomer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private EditText eTEmail;
    private EditText eTPass;
    private TextView tVMsg;
    private AlertDialog pd;
    private Button btnLoginUser;
    private Button btnToRegister;
    private Button btnUpdateEmail;

    private boolean userLogInFlag = false;
    private String userTypeFlag = "client";

    // Lists for the ListView and its Adapter
    ArrayList<String> customerList = new ArrayList<>();
    ArrayList<Customer> customerValues = new ArrayList<>();
    ListView lv;
    ArrayAdapter<String> adp;

    // This list will hold the results of the latest database search
    private ArrayList<Customer> currentSearchResults = new ArrayList<>();

    interface CustomerCallback {
        void onCustomerReceived(Customer customer);
    }

    interface MultipleCustomersCallback {
        void onCustomersReceived(ArrayList<Customer> customers);
    }


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);


        eTEmail = findViewById(R.id.eTEmail);
        eTPass = findViewById(R.id.eTPass);
        tVMsg = findViewById(R.id.tVMsg);
        btnLoginUser = findViewById(R.id.btnLoginUser);
        btnToRegister = findViewById(R.id.btnToRegister);
        btnUpdateEmail = findViewById(R.id.btnUpdateEmail);
        lv = findViewById(R.id.lv);


        btnLoginUser.setOnClickListener(v -> loginUser(v));

        btnToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });


        btnUpdateEmail.setOnClickListener(v -> updateCustomerEmail(v));

        // Setup the adapter for the ListView
        adp = new ArrayAdapter<>(MainActivity.this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, customerList);
        lv.setAdapter(adp);

        // Initial data load (optional, you might want to show an empty list first)
        loadAllCustomers();
    }

    /**
     * Loads all customers from Firebase and displays them in the ListView.
     */
    private void loadAllCustomers() {
        refCustomer.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dS) {
                ArrayList<Customer> allCustomers = new ArrayList<>();
                for (DataSnapshot data : dS.getChildren()) {
                    Customer customerTemp = data.getValue(Customer.class);
                    if (customerTemp != null) {
                        allCustomers.add(customerTemp);
                    }
                }
                updateListView(allCustomers);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("DB_ERROR", "loadAllCustomers:onCancelled", error.toException());
            }
        });
    }


    //<editor-fold desc="Firebase User Authentication (Login, Register)">
    public void createUser(View view) {
        String email = eTEmail.getText().toString();
        String pass = eTPass.getText().toString();
        if (email.isEmpty() || pass.isEmpty()) {
            tVMsg.setText("Please fill all fields");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.activity_main, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        pd = builder.create();
        pd.show();

        refAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    pd.dismiss();
                    if (task.isSuccessful()) {
                        FirebaseUser user = refAuth.getCurrentUser();
                        tVMsg.setText("User created successfully\nUid: " + user.getUid());

                        Customer customer = new Customer();
                        customer.setUserId(user.getUid());
                        customer.setUserType(userTypeFlag);
                        customer.setEmail(email);
                        refCustomer.child(customer.getUserId()).setValue(customer);
                    } else {
                        handleAuthError(task.getException());
                    }
                });
    }

    public void loginUser(View view) {
        String email = eTEmail.getText().toString();
        String pass = eTPass.getText().toString();
        if (email.isEmpty() || pass.isEmpty()) {
            tVMsg.setText("Please fill all fields");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.activity_main, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        pd = builder.create();
        pd.show();

        refAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(this, task -> {
            pd.dismiss();
            if (task.isSuccessful()) {
                tVMsg.setText("User logged in successfully");
                userLogInFlag = true;
                checkUserTypeAndRedirect(refAuth.getCurrentUser().getUid());
            } else {
                handleAuthError(task.getException());
            }
        });
    }

    private void handleAuthError(Exception exp) {
        if (exp instanceof FirebaseAuthWeakPasswordException) {
            tVMsg.setText("Password too weak.");
        } else if (exp instanceof FirebaseAuthUserCollisionException) {
            tVMsg.setText("User already exists.");
        } else if (exp instanceof FirebaseAuthInvalidCredentialsException) {
            tVMsg.setText("Invalid email or password.");
        } else if (exp instanceof FirebaseNetworkException) {
            tVMsg.setText("Network error. Please check your connection.");
        } else {
            tVMsg.setText("An error occurred. Please try again later.");
        }
    }


    /**
     * PRIMARY SEARCH: Fetches workers from Firebase based on specialty.
     * This is a database operation and updates the master search results list.
     * @param specialty The specialty to search for (e.g., "Plumber").
     */
    public void searchWorkersBySpecialty(String specialty) {
        refCustomer.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ArrayList<Customer> workersWithSpecialty = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot workerSnapshot : dataSnapshot.getChildren()) {
                        Customer worker = workerSnapshot.getValue(Customer.class);
                        if (worker != null && worker.getSpecialties() != null) {
                            String specialtiesList = "," + worker.getSpecialties() + ",";
                            String specialtyToFind = "," + specialty + ",";

                            if (specialtiesList.toLowerCase().contains(specialtyToFind.toLowerCase())) {
                                workersWithSpecialty.add(worker);
                            }
                        }
                    }
                }

                // IMPORTANT: Save these results as the new master list for further filtering
                currentSearchResults.clear();
                currentSearchResults.addAll(workersWithSpecialty);

                // Update the UI with the results of this primary search
                if (!currentSearchResults.isEmpty()) {
                    updateListView(currentSearchResults);
                    tVMsg.setText("Showing " + currentSearchResults.size() + " workers with specialty: " + specialty);
                } else {
                    updateListView(new ArrayList<>()); // Clear the list view
                    tVMsg.setText("No workers found with specialty: " + specialty);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("DB_ERROR", "searchWorkersBySpecialty:onCancelled", databaseError.toException());
                tVMsg.setText("Error searching for workers.");
            }
        });
    }

    private void checkUserTypeAndRedirect(String userId) {
        // Get user data from database
        refCustomer.child(userId).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful() && task.getResult().exists()) {
                    Customer user = task.getResult().getValue(Customer.class);

                    if (user != null) {
                        String userType = user.getUserType();

                        Intent intent;
                        if ("worker".equals(userType)) {
                            // Redirect to Worker Profile
                            intent = new Intent(MainActivity.this, ProfileWorkerActivity.class);

                        } else {
                            // Redirect to Client Profile (default)
                            intent = new Intent(MainActivity.this, ProfileClientActivity.class);

                        }
                        startActivity(intent);
                        finish(); // Close MainActivity so user can't go back to login

                    } else {
                        btnLoginUser.setEnabled(true);
                        tVMsg.setText("Error: User data not found");
                    }
                } else {
                    btnLoginUser.setEnabled(true);
                    tVMsg.setText("Error: Could not retrieve user data");
                    Toast.makeText(MainActivity.this,
                            "Failed to load user data", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * SECONDARY FILTER: Filters the CURRENT search results by a worker's name.
     * This is an in-memory operation and does NOT query the database.
     * @param nameQuery The name (or part of a name) to search for.
     */
    public void filterCurrentResultsByName(String nameQuery) {
        if (currentSearchResults.isEmpty()) {
            tVMsg.setText("Please perform a specialty search first.");
            return;
        }

        // If the second search query is empty, show all results from the first search
        if (nameQuery == null || nameQuery.trim().isEmpty()) {
            updateListView(currentSearchResults);
            return;
        }

        // Create a temporary list to hold the sub-filtered results
        ArrayList<Customer> filteredByName = new ArrayList<>();
        for (Customer worker : currentSearchResults) {
            if (worker.getName() != null && worker.getName().toLowerCase().contains(nameQuery.toLowerCase())) {
                filteredByName.add(worker);
            }
        }

        // Update the UI with the final, doubly-filtered list
        updateListView(filteredByName);
        tVMsg.setText("Showing " + filteredByName.size() + " results matching '" + nameQuery + "'.");
    }


    /**
     * Central method to update the ListView with a given list of customers.
     * @param customersToShow The list of Customer objects to display.
     */
    private void updateListView(ArrayList<Customer> customersToShow) {
        // Clear the lists that the adapter is directly using
        customerValues.clear();
        customerList.clear();

        // Populate those lists with the new data
        customerValues.addAll(customersToShow);
        for (Customer c : customerValues) {
            // Format the string for display in the ListView
            customerList.add(c.getName() + " (" + c.getSpecialties() + ")");
        }

        // Notify the adapter that the underlying data has changed, causing a UI refresh
        if (adp != null) {
            adp.notifyDataSetChanged();
        }
    }

    public void getCustomer(String userId, CustomerCallback callback) {
        refCustomer.child(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                Customer cusUpdate = task.getResult().getValue(Customer.class);
                callback.onCustomerReceived(cusUpdate);
            } else {
                callback.onCustomerReceived(null);
            }
        });
    } //for single customer

    public void updateCustomerEmail(View view) {
        if (userLogInFlag) {
            String userId = refAuth.getCurrentUser().getUid();
            getCustomer(userId, customer -> {
                if (customer != null) {
                    customer.setEmail(eTEmail.getText().toString());
                    refCustomer.child(userId).setValue(customer);
                    tVMsg.setText("Email updated to: " + customer.getEmail());
                }
            });
        }
    }

}
