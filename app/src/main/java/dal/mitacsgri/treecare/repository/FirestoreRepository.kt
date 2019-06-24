package dal.mitacsgri.treecare.repository

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import consts.COLLECTION_USERS
import data.User

/**
 * Created by Devansh on 22-06-2019
 */
class FirestoreRepository {

    private val db: FirebaseFirestore = Firebase.firestore

    fun getUserData(uid: String): Task<DocumentSnapshot> {
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        return docRef.get()
    }

    fun storeUser(user: User) {
        db.collection(COLLECTION_USERS).document(user.uid)
            .set(user, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("USER stored", user.toString())
            }
            .addOnFailureListener {
                Log.d("USER store failed", it.toString())
            }
    }
}