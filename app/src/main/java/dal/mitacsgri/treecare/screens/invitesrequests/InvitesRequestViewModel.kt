package dal.mitacsgri.treecare.screens.invitesrequests

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import dal.mitacsgri.treecare.extensions.default
import dal.mitacsgri.treecare.extensions.notifyObserver
import dal.mitacsgri.treecare.model.*
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import org.joda.time.DateTime

class InvitesRequestViewModel(
    private val sharedPrefsRepository: SharedPreferencesRepository,
    private val firestoreRepository: FirestoreRepository
): ViewModel() {

    val messageLiveData = MutableLiveData<String>()
    val messageLiveData2 = MutableLiveData<String>()
    val messageLiveData3 = MutableLiveData<String>()
    val messageLiveData4 = MutableLiveData<String>()
    val status = MutableLiveData<Boolean>().default(false)
    val invitesList = MutableLiveData<ArrayList<InvitesRequest>>().default(arrayListOf())
    val requestList = MutableLiveData<ArrayList<InvitesRequest>>().default(arrayListOf())


    // val statusMessage = MutableLiveData<String>()
    val uId = ArrayList<String>()

    // val uIdc = MutableLiveData<ArrayList<String>>().default(arrayListOf())
    // val uUrl = ArrayList<String>()
    val teams = ArrayList<String>()
    val tInvites = ArrayList<String>()

    // var tCount = 0
    // var uc :Int=0
    // val uCount = MutableLiveData<Int>().default(0)
    val cTeam = MutableLiveData<ArrayList<String>>().default(arrayListOf())
    fun getAllRequests(): MutableLiveData<ArrayList<InvitesRequest>> {
        val uid = sharedPrefsRepository.user.uid
        firestoreRepository.getAllCaptainedTeams(uid)
            .addOnSuccessListener {
                val ct = it.toObjects<Team>()
                for (i in 0 until ct.count()) {
                    if (ct.get(i).joinRequests.isNotEmpty()) {
                        //  Log.d("TAG","TEST"+ct.get(i).joinRequests)
                        val v = ct.get(i).joinRequests.toString()
                        cTeam.value?.add(v)
                    }
                    //Log.d("Value","Cteam "+cTeam.value)
                    teams.add(ct.get(i).name)
                    //Log.d("Value","Teamnames "+teams)
                    //Log.d("TAG", "teamz" + ct.get(i).joinRequests)
                }
                // val count = cTeam.value?.count()
                var count = 0
                var c = 0 //used for counting the iteration while fetching user photos
                for (t in teams) {
                    firestoreRepository.getTeam(t)
                        .addOnSuccessListener {
                            val team = it.toObject<Team>()
                            //val capname = team?.captainName
                            val tName = team?.name.toString()
                            // Log.d("Requests","tnames"+tName)
                            team?.joinRequests?.let { it1 -> uId.addAll(it1) }
                            for (i in 0 until uId.count()) {
                                //  Log.d("TAG","indi"+uId.get(i))
                                //This loop is for counting all the elements in the array list
                                // since the DB returns an array list, which we add to another list
                                //So one whole list of user id's is counter as one. To avoid that we use this loop
                                count++
                            }
                            //uId.add(team?.joinRequests.toString())
                            //Log.d("Requests","uids "+uId)
                            //val uCount = uId.count()
                            //  Log.d("TAG","Counter "+count)
                            for (u in 0 until uId.count()) {
                                c += 1
                                //Log.d("Counter","c "+c)
                                //Log.d("Names","names"+u)
                                firestoreRepository.getUserData(uId.get(u))
                                    .addOnSuccessListener {
                                        //  Log.d("Requests","userid "+u)
                                        val user2 = it.toObject<User>()
                                        //uUrl.add(user2?.photoUrl.toString())
                                        // Log.d("Requests","unames "+ user2?.name)
                                        requestList.value?.add(
                                            InvitesRequest(
                                                user2?.name.toString(),
                                                " " + tName,
                                                user2?.photoUrl.toString(),
                                                user2?.uid.toString()
                                            )
                                        )
                                        if (c == count) {
                                            requestList.notifyObserver()
                                            //Log.d("TAG","values1 "+requestList.value)
                                        }
                                        // Log.d("TAG","values2 "+requestList.value)
                                    }
                                //Log.d("TAG","values3 "+requestList.value)
                            }
                            uId.clear() // clearing the Uid so that the next set from the next team can be loaded

                            //cTeam.notifyObserver()
                            //Log.d("TAG","ids"+cTeam.value?.count())

                        }
                }
                /*
        firestoreRepository.getUserData(uid)
            .addOnSuccessListener {
                val user = it.toObject<User>()
                val teams: ArrayList<String> = user!!.captainedTeams
                val count = getCount(uid)
                Log.d ("count", "user Count "+ count.value?.count())
                for (t in teams) {
                    Log.d("TAG", "TeamName In loop " + t)
                    firestoreRepository.getTeam(t)
                        .addOnSuccessListener {
                            val team = it.toObject<Team>()
                            val capname = team?.captainName
                            val tName = team?.name.toString()
                            uId.add(team?.joinRequests.toString())
                            //val uCount = uId.count()
                            for (u in uId) {
                                val user1 = sharedPrefsRepository.user.uid
                                firestoreRepository.getUserData(user1)
                                    .addOnSuccessListener {
                                        val user2 = it.toObject<User>()
                                        //uUrl.add(user2?.photoUrl.toString())
                                        requestList.value?.add(
                                            InvitesRequest(
                                                user2?.name.toString(),
                                                tName,
                                                user2?.photoUrl.toString()
                                            )
                                        )
                                    }

                            }
                            //arr.add(team?.captain to InvitesRequest)

                            //InvitesRequest(, tName,

                            Log.d("TAG", "JoinRequest " + requestList.value)
                        }
                }


            }*/


                //To retrieve all invites that the user has received from captains


                /*fun getCount(uid: String, teams: ArrayList<String>): Int? {
        firestoreRepository.getUserData(uid)
            .addOnSuccessListener {
                tCount = teams.count()
                for (t in teams) {
                    firestoreRepository.getTeam(t)
                        .addOnSuccessListener {
                            val team = it.toObject<Team>()
                            Log.d("Tag","nullcheck "+team!!.joinRequests.toString())
                            uIdc.value?.add(team.joinRequests.toString())
                            uIdc.notifyObserver()
                            Log.d("TAG","uIdc "+uIdc.value)
                            val count = uIdc.value!!.count()
                            //uCount.value = uIdc.value!!.count()
                            uCount.value = count
                            //uc = uIdc.value!!.count()
                            uCount.notifyObserver()
                            uc = uCount.value!!.toInt()
                            Log.d("TAG","ucount "+ uCount.value)
                            Log.d("TAG","ucount "+ uc)
                        }
                    Log.d("TAG","uIdc before uc1 "+uIdc.value)
                    Log.d("TAG","ucount1 "+ uCount.value)
                    Log.d("TAG","ucount1.1 "+ uc)
                }
                Log.d("TAG","uIdc before uc2  "+uIdc.value)
                Log.d("TAG","ucount2 "+ uCount.value)
                Log.d("TAG","ucount2.1 "+ uc)

            }
        return (uCount.value) //Counting the number of data fetches that will happen for each userId under each team
    }*/

                /*fun getCount(uid:String): MutableLiveData<ArrayList<String>> {

        firestoreRepository.getAllCaptainedTeams(uid)
            .addOnSuccessListener {
                val ct = it.toObjects<Team>()
                for (i in 0 until ct.count()){
                cTeam.value?.add(ct.get(i).joinRequests.toString())
                //Log.d("TAG", "teamz" + ct.get(i).joinRequests)
            }
                cTeam.notifyObserver()
                //Log.d("TAG","ids"+cTeam.value?.count())

            }
        Log.d("TAG","ids"+cTeam.value?.count())
        return cTeam

    }*/

            }
        return requestList
    }

    fun getAllInvites(): MutableLiveData<ArrayList<InvitesRequest>> {

        val uid = sharedPrefsRepository.user.uid
        var count2 = 0
        firestoreRepository.getUserData(uid)
            .addOnSuccessListener {
                val user = it.toObject<User>()
                tInvites.add(user?.teamInvites.toString())
                //Log.d("Inv","tnames"+tInvites[0])
                Log.d("Inv", "tCount" + user?.teamInvites?.count())
                //Log.d("Inv","tnames"+tInvites[1])
                for (t in user?.teamInvites!!) {
                    count2++ //Increments when each team is fetched
                    /*Log.d("Inv","TN"+t[0])
                    Log.d("Inv","TN"+t[1])*/
                    firestoreRepository.getTeam(t)
                        .addOnSuccessListener {
                            val team = it.toObject<Team>()
                            //val cap = team?.captainName
                            val cId = team?.captain
                            // Log.d("Inv","capId"+team?.captain)
                            firestoreRepository.getUserData(cId.toString())
                                .addOnSuccessListener {
                                    val captain = it.toObject<User>()
                                    //val pUrl = user?.photoUrl
                                    // Log.d("Inv","capname "+team?.captainName.toString())
                                    // Log.d("Inv","teamName "+team?.name)
                                    // Log.d("Inv","purl "+captain?.photoUrl)
                                    invitesList.value?.add(
                                        InvitesRequest(
                                            team?.captainName.toString(),
                                            team?.name.toString(),
                                            captain?.photoUrl.toString(),
                                            team?.captain.toString()
                                        )
                                    )
                                    if (count2 == user?.teamInvites?.count())
                                        invitesList.notifyObserver()
                                }
                            //Log.d("Inv","Inside"+invitesList.value)

                        }
                }
                // invitesList.notifyObserver()
                //Log.d("Inv","Outside"+invitesList.value)
            }

        return invitesList
    }

    fun acceptInvite(item: InvitesRequest): MutableLiveData<String> {
        val userid = sharedPrefsRepository.user.uid
        var count = 0
        //Log.d("Del","tn"+item.teamName+" "+item.photoUrl+" "+item.userName+" "+userid)
        //TODO:add tourney to user if the team has a tourney
        if(sharedPrefsRepository.user.currentTeams.isEmpty()){
        //TODO: These functions can be simplified using the updateTeam function instead of having so many functions
            firestoreRepository.deleteTeamInvitesFromUser(userid, item.teamName)
                .addOnSuccessListener {

                    count++

                    Log.d("del", "Deleted the invite Successfully")
                    status.value = true
                    status.notifyObserver()
                    /*invitesList.value?.remove(item)
                    invitesList.notifyObserver()*/
                }
                .addOnFailureListener {
                    Log.e("del", "Deletion of Invite Failed")
                }
            firestoreRepository.deleteTeamJoinRequestFromUser(userid, item.teamName)
                .addOnSuccessListener {

                    count++

                    Log.d("del", "Deleted the Request Successfully")
                    status.value = true
                    status.notifyObserver()
                    /*requestList.value?.remove(item)
                    requestList.notifyObserver()*/
                }
                .addOnFailureListener {
                    Log.e("del", "Deletion of Request Failed")
                }
            firestoreRepository.deleteJoinRequestFromTeam(userid, item.teamName)
                .addOnSuccessListener {

                    count++

                    Log.d("del", "Deletion of Join Req from team was successful")
                    status.value = true
                    status.notifyObserver()
                }

            firestoreRepository.deleteInvitedMemberFromTeam(userid, item.teamName)
                .addOnSuccessListener {

                    count++

                    Log.d("del", "Deletion of Invite from team was successful")
                    status.notifyObserver()

                }

            /*firestoreRepository.getTeam(item.teamName)
                .addOnSuccessListener {
                    val team = it.toObject<Team>()
                    val capId = team?.captain
                    firestoreRepository.deleteJoinRequestFromTeam(capId.toString(), item.teamName)
                        .addOnSuccessListener {
                            count++

                            Log.d("del", "Deletion of Invite from team was successful")
                            status.notifyObserver()
                        }
                }*/

            firestoreRepository.addCurrentTeams(userid, item.teamName)
                .addOnSuccessListener {

                    count++

                    Log.d("Add", "User was added to the team successfully")
                    status.value = true
                    status.notifyObserver()
                }

            firestoreRepository.addTeamMember(userid, item.teamName)
                .addOnSuccessListener {

                    count++
                    Log.d("count", "c1 " + count)

                    status.value = true
                    status.notifyObserver()
                    Log.d("Add", "User was added to the team successfully")

                    if (count == 6) {

                        firestoreRepository.getTeam(item.teamName)
                            .addOnSuccessListener {
                                val team = it.toObject<Team>()
                                val tourneys = team?.currentTournaments
                                for(tourney in tourneys!!){
                                    addTournament(item.teamName,tourney, "Invite", sharedPrefsRepository.user.uid)
                                }
                            }

                        invitesList.value?.remove(item)
                        invitesList.notifyObserver()

                        requestList.value?.remove(item)
                        requestList.notifyObserver()

                        messageLiveData.value = "You have been added to the team"
                        //messageLiveData.notifyObserver()

                    } else {
                        messageLiveData.value = "Unable to process request. Please try again later"
                        //messageLiveData.notifyObserver()
                    }
                }
            //Log.d("count","c2 "+count)
            return messageLiveData
        }
        else{
                messageLiveData.value = "You can only be a part of one team"
                //messageLiveData.notifyObserver()
                return messageLiveData
        }
    }

    fun acceptUser(item: InvitesRequest): MutableLiveData<String> {
        Log.d("Test","Inside Function")
        val userid2 = item.uId
        val tName = item.teamName.trim()
        Log.d("Del","tn"+item.teamName.trim()+" "+item.photoUrl+" "+item.userName+" "+userid2)
        Log.d("Test", "UserId "+userid2)
        var count2 = 0

        firestoreRepository.getUserData(userid2)
            .addOnSuccessListener {
                val user = it.toObject<User>()
                // checking if the user is already in a team before accepting
                if(user?.currentTeams?.isEmpty()!!) {

                    firestoreRepository.getTeam(tName)
                        .addOnSuccessListener {
                            Log.d("Test", "Inside getTeam")
                            val team = it.toObject<Team>()
                            val capId = team?.captain
                            Log.d("Test", "CaptainId" + capId)

                            firestoreRepository.deleteJoinRequestFromTeam(capId.toString(), tName)
                                .addOnSuccessListener {
                                    Log.d("Test", "Inside deleteUserJoinReq")

                                    Log.d("del2", "Deletion of Invite from team was successful")
                                    status.notifyObserver()
                                }
                            count2++
                            Log.d("Count", "Count1" + count2)
                        }

                    firestoreRepository.deleteTeamJoinRequestFromUser(userid2, tName)
                        .addOnSuccessListener {
                            Log.d("Test", "Inside deleteTeamJoinReqFromUser")
                            count2++
                            Log.d("Count", "Count2" + count2)

                            Log.d("del2", "Deleted the Request Successfully")
                            status.value = true
                            status.notifyObserver()
                        }

                    firestoreRepository.deleteJoinRequestFromTeam(userid2, tName)
                        .addOnSuccessListener {

                            Log.d("Test", "Inside deleteJoinReqFromTeam")

                            count2++
                            Log.d("Count", "Count3" + count2)

                            Log.d("del2", "Deletion of Join Req from team was successful")
                            status.value = true
                            status.notifyObserver()
                        }

                    firestoreRepository.addCurrentTeams(userid2, tName)
                        .addOnSuccessListener {
                            Log.d("Test", "Inside addCurrentTeams")
                            count2++
                            Log.d("Count", "Count4" + count2)

                            Log.d("Add", "User was added to the team successfully")
                            status.value = true
                            status.notifyObserver()
                        }

                    firestoreRepository.addTeamMember(userid2, tName)
                        .addOnSuccessListener {
                            Log.d("Test", "Inside addTeamMember")
                            count2++
                            Log.d("Count", "Count5" + count2)

                            Log.d("count", "c1 " + count2)

                            status.value = true
                            status.notifyObserver()
                            Log.d("Add", "User was added to the team successfully")

                            if (count2 == 5) {

                                firestoreRepository.getTeam(tName)
                                    .addOnSuccessListener {
                                        val team = it.toObject<Team>()
                                        val tourneys = team?.currentTournaments
                                        for(tourney in tourneys!!){
                                            addTournament(tName,tourney, "Request",item.uId)
                                        }
                                    }

                                invitesList.value?.remove(item)
                                invitesList.notifyObserver()

                                requestList.value?.remove(item)
                                requestList.notifyObserver()

                                messageLiveData2.value = "Player has been added to the team"
                                //messageLiveData2.notifyObserver()

                            } else {
                                messageLiveData2.value = "Unable to process request. Please try again later"
                                //messageLiveData2.notifyObserver()
                            }
                        }
                }

                else{
                    messageLiveData2.value = "User is already part of a team"
                    //messageLiveData2.notifyObserver()
                }
            }
        return messageLiveData2
    }

    fun declineUser(item: InvitesRequest) {

        firestoreRepository.updateUserData(item.uId, mapOf("teamJoinRequests" to FieldValue.arrayRemove(item.teamName.trim())))
            .addOnSuccessListener {
                Log.d("Test","uid"+item.uId +" "+item.teamName)
                Log.d("del3","teamJoinReq for user was deleted successfully")
            }

        firestoreRepository.updateTeamData(item.teamName.trim(), mapOf("joinRequests" to FieldValue.arrayRemove(item.uId)))
            .addOnSuccessListener {
                Log.d("del3","teamInvite for user was deleted successfully")
            }

        requestList.value?.remove(item)
        requestList.notifyObserver()
        messageLiveData4.value = "User Request was declined"
    }

    fun declineInvite(item: InvitesRequest){

        firestoreRepository.updateUserData(sharedPrefsRepository.user.uid, mapOf("teamInvites" to FieldValue.arrayRemove(item.teamName)))
            .addOnSuccessListener {
                Log.d("del3","teamJoinReq for user was deleted successfully")
            }

        firestoreRepository.updateTeamData(item.teamName.trim(), mapOf("invitedMembers" to FieldValue.arrayRemove(sharedPrefsRepository.user.uid)))
            .addOnSuccessListener {
                Log.d("del3","InvitedMembers from team was deleted successfully")
            }
        messageLiveData3.value = "Invite has been Declined"

        invitesList.value?.remove(item)
        invitesList.notifyObserver()

    }

    fun addTournament(team: String, tournamentName : String, type:String, uid:String) {
        //Adding tournament to currentTournaments in Users Collection
        firestoreRepository.getTournament(tournamentName)
            .addOnSuccessListener {
                val tournament = it.toObject<Tournament>()
                val userTournament =
                    tournament?.let { it1 -> getUserTournament(it1, team) }

                //Log.d("Test", "tourneyName2 ${tournament?.name}")

                            //val uid = sharedPrefsRepository.user.uid
                            //Log.d("Test","UID ${uid}")
                            userTournament?.let { it1 -> updateUserSharedPrefsData(it1) }
                            Log.d("Test", "tourneyName2 ${tournament?.name}")
                            mapOf("currentTournaments.${tournament?.name}" to userTournament)?.let { it1 ->
                                firestoreRepository.updateUserTournamentData(uid, it1)
                            }
                                .addOnSuccessListener {

                                    //TODO: These 3 lines which are below have
                                    // to be executed everytime for a user when captain accepts them into the team

                                    if (type == "Invite") {
                                        val user = sharedPrefsRepository.user
                                        user.currentTournaments[tournament!!.name] =
                                            userTournament!!
                                        sharedPrefsRepository.user = user

                                        Log.d("Test", "Being added to user")
                                    }
                                }
                                .addOnFailureListener {
                                    Log.d("Test", "Unable to add user")
                                }

                    }
            }

    private fun getUserTournament(tournament: Tournament, team:String) =
        UserTournament(
            name = tournament.name,
            dailyStepsMap = mutableMapOf(),
            totalSteps = sharedPrefsRepository.getDailyStepCount(),
            joinDate = DateTime().millis,
            goal = tournament.goal,
            endDate = tournament.finishTimestamp,
            teamName = team
        )

    private fun updateUserSharedPrefsData(userTournament: UserTournament){
        val user = sharedPrefsRepository.user
        userTournament.leafCount = sharedPrefsRepository.getDailyStepCount() / 1000
        userTournament.totalSteps = sharedPrefsRepository.getDailyStepCount()
        user.currentTournaments[userTournament.name] = userTournament
        sharedPrefsRepository.user = user
    }
}