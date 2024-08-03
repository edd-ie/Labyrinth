package edd_ie.com.github.labyrinth.utils;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseUtil {

    public static CollectionReference allLocations(){
        return FirebaseFirestore.getInstance().collection("location");
    }

    public static CollectionReference allCorners(){
        return FirebaseFirestore.getInstance().collection("corners");
    }

//    public static DocumentReference getChatroomReference(String chatroomId){
//        return FirebaseFirestore.getInstance().collection("chatrooms").document(chatroomId);
//    }
//
//    public static CollectionReference getChatroomMessageReference(String chatroomId){
//        return getChatroomReference(chatroomId).collection("chats");
//    }
//
//    public static CollectionReference allChatroomCollectionReference(){
//        return FirebaseFirestore.getInstance().collection("chatrooms");
//    }


//    public static void logout(){
//        FirebaseAuth.getInstance().signOut();
//    }

//    public static StorageReference  getCurrentProfilePicStorageRef(){
//        return FirebaseStorage.getInstance().getReference().child("profile_pic")
//                .child(FirebaseUtil.currentUserId());
//    }

//    public static StorageReference  getOtherProfilePicStorageRef(String otherUserId){
//        return FirebaseStorage.getInstance().getReference().child("profile_pic")
//                .child(otherUserId);
//    }

    //    public static String currentUserId(){
//        return FirebaseAuth.getInstance().getUid();
//    }

//    public static boolean isLoggedIn(){
//        if(currentUserId()!=null){
//            return true;
//        }
//        return false;
//    }

//    public static DocumentReference currentUserDetails(){
//        return FirebaseFirestore.getInstance().collection("users").document(currentUserId());
//    }


}