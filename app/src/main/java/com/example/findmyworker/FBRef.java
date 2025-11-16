package com.example.findmyworker;


import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.TaskState;


public class FBRef {
    public static FirebaseAuth refAuth = FirebaseAuth.getInstance();
    public static FirebaseDatabase FBDB = FirebaseDatabase.getInstance();
    public static FirebaseStorage storage = FirebaseStorage.getInstance();
    public static DatabaseReference refCustomer = FBDB.getReference("Customer");
    public static DatabaseReference refAvailability = FBDB.getReference("Availability");
    public static DatabaseReference refChatList = FBDB.getReference("ChatList");
    public static DatabaseReference refChatMessagesLinkedList = FBDB.getReference("ChatMessagesLinkedList");


    public static DatabaseReference getRefCustomer(String userId) {
        return refCustomer.child(userId);
    }

    public Task<DataSnapshot> getCustomerData(String userId) { return refCustomer.child(userId).get(); }

    public Customer convertSnapshotToCustomer(DataSnapshot dataSnapshot) { return dataSnapshot.getValue(Customer.class); }

}

