package dal.mitacsgri.treecare.repository

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dal.mitacsgri.treecare.consts.COLLECTION_CHALLENGES
import dal.mitacsgri.treecare.consts.COLLECTION_USERS
import dal.mitacsgri.treecare.data.Challenge
import dal.mitacsgri.treecare.data.User

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

    fun getChallenge(id: String) = db.collection(COLLECTION_CHALLENGES).document(id).get()

    fun getAllActiveChallenges() = db.collection(COLLECTION_CHALLENGES).get()

    fun storeChallenge(challenge: Challenge, action: (status: Boolean) -> Unit) {
        db.collection(COLLECTION_CHALLENGES).document(challenge.name)
            .set(challenge)
            .addOnSuccessListener {
                action(true)
                Log.d("Challenge stored", challenge.toString())
            }
            .addOnFailureListener {
                action(false)
                Log.d("Challenge store failed ", it.toString() + "Challenge: $challenge")
            }
    }

    fun getAllChallengesCreatedByUser(userId: String) =
        db.collection(COLLECTION_CHALLENGES)
            .whereEqualTo("creatorUId", userId).get()
}