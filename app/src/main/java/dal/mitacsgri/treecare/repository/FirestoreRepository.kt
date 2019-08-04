package dal.mitacsgri.treecare.repository

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dal.mitacsgri.treecare.consts.*
import dal.mitacsgri.treecare.model.*

/**
 * Created by Devansh on 22-06-2019
 */
class FirestoreRepository {

    private val db: FirebaseFirestore = Firebase.firestore

    fun getUserData(uid: String): Task<DocumentSnapshot> {
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        return docRef.get()
    }

    fun storeUser(user: User) = db.collection(COLLECTION_USERS).document(user.uid)
            .set(user, SetOptions.merge())

    fun updateUserData(userId: String, values: Map<String, Any>) =
        db.collection(COLLECTION_USERS).document(userId).update(values)

    fun getChallenge(id: String) = db.collection(COLLECTION_CHALLENGES).document(id).get()

    fun updateChallengeData(challengeName: String, values: Map<String, Any>) =
        db.collection(COLLECTION_CHALLENGES).document(challengeName).update(values)

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

    fun deleteUserFromChallengeDB(challenge: Challenge, userId: String) =
        db.collection(COLLECTION_CHALLENGES).document(challenge.name)
            .update("players", FieldValue.arrayRemove(userId))

    fun deleteChallengeFromUserDB(userId: String, userChallenge: UserChallenge, userChallengeJson: String) =
        db.collection(COLLECTION_USERS).document(userId)
            .update(mapOf("currentChallenges.${userChallenge.name}" to userChallengeJson))

    fun getAllChallengesCreatedByUser(userId: String) =
        db.collection(COLLECTION_CHALLENGES)
            .whereEqualTo("creatorUId", userId).get()

    fun setChallengeAsNonExist(challengeName: String) =
        db.collection(COLLECTION_CHALLENGES).document(challengeName)
            .update("exist", false)

    fun getAllActiveTournaments() = db.collection(COLLECTION_TOURNAMENTS).get()

    fun getAllTeams() = db.collection(COLLECTION_TEAMS).get()

    fun getAllCaptainedTeams(userId: String) = db.collection(COLLECTION_TEAMS)
        .whereEqualTo("captain", userId).get()

    fun getAllTeamsForUserAsMember(userId: String) = db.collection(COLLECTION_TEAMS)
        .whereArrayContains("members", userId).get()

    fun getTeam(teamName: String)  = db.collection(COLLECTION_TEAMS).document(teamName).get()

    fun storeTeam(team: Team, action: (status: Boolean) -> Unit) {
        db.collection(COLLECTION_TEAMS).document(team.name)
            .set(team)
            .addOnSuccessListener {
                action(true)
                Log.d("Team store success", "Team: $team")
            }
            .addOnFailureListener {
                action(false)
                Log.d("Team store failed", it.toString() + "Challenge: $team")
            }
    }

    fun updateTeamData(teamName: String, values: Map<String, Any>) =
        db.collection(COLLECTION_TEAMS).document(teamName).update(values)

    fun getTrophiesData(userId: String) =
            db.collection(COLLECTION_TROPHIES).document(userId).get()

    fun storeTrophiesData(userId: String, trophies: UserChallengeTrophies) =
            db.collection(COLLECTION_TROPHIES).document(userId).set(trophies)
}