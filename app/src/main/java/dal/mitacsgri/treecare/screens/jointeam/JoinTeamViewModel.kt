package dal.mitacsgri.treecare.screens.jointeam

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.toObjects
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.extensions.notifyObserver
import dal.mitacsgri.treecare.model.User
import dal.mitacsgri.treecare.repository.FirestoreRepository

class JoinTeamViewModel(

    //private val sharedPrefsRepository: SharedPreferencesRepository,
    private val firestoreRepository: FirestoreRepository
): ViewModel() {

    val mailData = MutableLiveData<String>()
    val userID = MutableLiveData<String>()
    val valid = MutableLiveData<Boolean>()
    val list:ArrayList<Int> = ArrayList<Int>()
    val messageLiveData = MutableLiveData<String>()
    val messageLiveData2 = MutableLiveData<String>()
    fun getUserEmail(email:String, teamName:String) {
        firestoreRepository.getAllUserEmail(email)
            .addOnSuccessListener {
                val user = it.toObjects<User>()
                try {
                    mailData.value = user.get(0).email
                }
                catch (e:Exception){
                    Log.d("Exception",e.toString())
                    mailData.value=""
                }
                if(mailData.value=="" ){
                    messageLiveData.value = "PlayerID does not exist"
                    //messageLiveData.notifyObserver()
                }
                else{
                    getUserId(email,teamName)
                }
            }
    }

    fun getUserId(email:String, teamName:String) {
        firestoreRepository.getAllUserEmail(email)
            .addOnSuccessListener {
                Log.d("Test", "mailId1" + it.toString())
                val user = it.toObjects<User>()
                try {
                    userID.value = user.get(0).email

                    //Log.d("Test", "mailId2" + user.get(0).email)

                    if (userID.value == email) {
                        firestoreRepository.updateUserData(user.get(0).uid, mapOf("teamInvites" to FieldValue.arrayUnion(teamName)))
                            .addOnSuccessListener {
                                Log.d("Invite", "sent")

                                firestoreRepository.updateTeamData(teamName, mapOf("invitedMembers" to FieldValue.arrayUnion(user.get(0).uid)))
                                    .addOnSuccessListener {
                                        Log.d("UserId", "User Added to team invites")
                                        messageLiveData2.value = "An Invite has been sent to the Player"
                                        //messageLiveData2.notifyObserver()
                                    }
                                    .addOnFailureListener {
                                        Log.d("Invite", "Failed to add to team")
                                        firestoreRepository.updateTeamData(user[0].uid, mapOf("invitedMembers" to FieldValue.arrayRemove(teamName)))
                                    }
                            }
                            .addOnFailureListener {
                                Log.e("Invite", "failed")
                            }
                    }
                    else{
                        messageLiveData.value = "PlayerID does not exist"
                        messageLiveData.notifyObserver()
                    }
                }
                catch (e: Exception) {
                        Log.d("Exception", e.toString())
                    }
            }
    }
}

                /*try {
                    valid.value = sendInvite(userID.value.toString(), teamName)
                    Log.d("Toast","Back in try"+valid.value)
                        //messageLiveData.notifyObserver()
                }
                catch(e:Exception){
                    valid.value = false
                    if(valid.value == false) {
                        messageLiveData.value = "Unable to send out invites. Please try again later"
                        messageLiveData.notifyObserver()
                        Log.d("Toast","msgge"+messageLiveData.value)
                    }
                        //messageLiveData.notifyObserver()
                }
                if(valid.value == true) {
                    messageLiveData.value = "An invite has been sent to the user"
                    messageLiveData.notifyObserver()
                    Log.d("Toast","msgg"+messageLiveData.value)
                }

                valid.notifyObserver()

                Log.d("TAG","DBID "+userID.value)
                //Log.d("TAG","DBB "+userEmail)
            }
            .addOnFailureListener{
                Log.d("Toast","VM User Does not Exist")

            }
        // Log.d("TAG","DBID2 "+userEmail)
        //return userID
        //return valid\
        Log.d("Toast","msg"+messageLiveData.value)
        return valid
    }

    fun sendInvite(Uid: String, teamName: String): Boolean? {
        Log.d("TAG","TN "+teamName)
        Log.d("Invite","UID "+ Uid)

       *//* if(Uid.isNotEmpty()) {
            valid.value = true
            valid.notifyObserver()
        }*//*
        //val uid = sharedPrefsRepository.user.uid

        firestoreRepository.updateUserData(Uid,
            mapOf("teamInvites" to FieldValue.arrayUnion(teamName)))
            .addOnSuccessListener {
                Log.d("Invite", "sent")

                firestoreRepository.updateTeamData(teamName,
                    mapOf("invitedMembers" to FieldValue.arrayUnion(Uid)))
                    .addOnSuccessListener {
                        //action(true)
                        Log.d("UserId","User Added to team invites")
                        valid.value = true
                        valid.notifyObserver()
                        Log.d("Valid","Value1"+valid.value)
                    }

                    .addOnFailureListener {
                        //action(false)
                        Log.d("Invite","Failed to add to team")
                        firestoreRepository.updateTeamData(Uid,
                            mapOf("invitedMembers" to FieldValue.arrayRemove(teamName)))
                    }
                //valid.notifyObserver()
                Log.d("Valid","Value2"+valid.value)
            }
            .addOnFailureListener {
                Log.e("Invite", "failed")
                //action(false)
            }


        Log.d("Valid","Value3"+valid.value)
        return valid.value
    }
}*/

    //fun getRegexToMatchEmail() = Regex("\n*@*.com")
    //userEmail = userLiveData.value.toString()
    /*for (i in 0 until limit){
        //val user = it.toObject<User>()
        Log.d("TAG", "UserInfo "+limit.toString()+" " + user.toString())
        userLiveData.value = user?.email.toString()
        if(userLiveData.value.equals(email)) {
            Log.d("TAG","MatchingMailIds"+userLiveData)
            userEmail.add(userLiveData.value.toString())
            isValid=true
        }
    }*/

    //isValid = userLiveData.value?.matches(getRegexToMatchEmail())!!

    /*if (isValid) {
        Log.d("TAG", "MailId" + userLiveData.value.toString())
        return userLiveData.value.toString()
    }*/

