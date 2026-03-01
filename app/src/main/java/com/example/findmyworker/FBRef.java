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

/**
 * This class provides static references to Firebase Authentication, Database, and Storage instances,
 * as well as specific DatabaseReference paths.
 *
 * NOTE: Using static references like this can lead to issues with testability and flexibility
 * in larger applications or when managing different Firebase environments.
 * For future refactoring, consider initializing these instances locally within the components
 * that need them, or using a Dependency Injection framework (e.g., Hilt, Koin) to manage their lifecycles.
 * This approach ensures that Firebase instances are managed more robustly and are easier to mock for testing.
 */
public class FBRef {
    public static FirebaseAuth refAuth = FirebaseAuth.getInstance();
    public static FirebaseDatabase FBDB = FirebaseDatabase.getInstance();
    public static FirebaseStorage storage = FirebaseStorage.getInstance();

    public static DatabaseReference refCustomer = FBDB.getReference("Customer");
    public static DatabaseReference refAvailability = FBDB.getReference("Availability");
    public static DatabaseReference refChatList = FBDB.getReference("ChatList");
    public static DatabaseReference refChatMessagesLinkedList = FBDB.getReference("ChatMessagesLinkedList");
}
