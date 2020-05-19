package dal.mitacsgri.treecare.screens.tournaments


import android.text.SpannedString
import android.util.Log
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.common.util.concurrent.MoreExecutors
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import dal.mitacsgri.treecare.backgroundtasks.workers.UpdateUserTournamentDataWorker
import dal.mitacsgri.treecare.consts.TOURNAMENT_MODE
import dal.mitacsgri.treecare.consts.TOURNAMENT_TYPE_DAILY_GOAL_BASED
import dal.mitacsgri.treecare.extensions.*
import dal.mitacsgri.treecare.model.Tournament
import dal.mitacsgri.treecare.model.UserTournament
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import org.joda.time.DateTime

class TournamentsViewModel(
    private val sharedPrefsRepository: SharedPreferencesRepository,
    private val firestoreRepository: FirestoreRepository
): ViewModel() {

    companion object Types {
        const val ACTIVE_TOURNAMENTS = 0
        const val TOURNAMENTS_BY_YOU = 1
    }

    val activeTournamentsList = MutableLiveData<ArrayList<Tournament>>().default(arrayListOf())
    val currentTournamentsList = MutableLiveData<ArrayList<Tournament>>().default(arrayListOf())
    val tournamentsByYouList = MutableLiveData<ArrayList<Tournament>>().default(arrayListOf())

    //The error status message must contain 'error' in string because it is used to check whether to
    //disable or enable join button
    val statusMessage = MutableLiveData<String>()
    var messageDisplayed = true

    fun getAllActiveTournaments() {
        firestoreRepository.getAllActiveTournaments()
            .addOnSuccessListener {
                activeTournamentsList.value = it.toObjects<Tournament>().filter { it.exist && it.active }.toArrayList()
                activeTournamentsList.notifyObserver()
            }
            .addOnFailureListener {
                Log.e("Active tournaments", "Fetch failed: $it")
            }
    }

    fun getCurrentTournamentsForUser() {
        val tournamentReferences = sharedPrefsRepository.user.currentTournaments

        tournamentReferences.forEach { (_, userTournament) ->
            //Getting challenges from the Challenges DB after getting reference
            // from the challenges list obtained from the user
            firestoreRepository.getTournament(userTournament.name)
                .addOnSuccessListener {
                    val tournament = it.toObject<Tournament>() ?: Tournament(exist = false)
                    synchronized(currentTournamentsList.value!!) {
                        if (tournament.exist) {
                            currentTournamentsList.value?.sortAndAddToList(tournament)
                            currentTournamentsList.notifyObserver()
                        }
                    }
                }
                .addOnFailureListener {
                    Log.d("Tournament not found", it.toString())
                }
        }
    }

    fun getAllCreatedTournamentsTournaments(userId: String) {
        firestoreRepository.getAllTournamentsCreatedByUser(userId)
            .addOnSuccessListener {
                tournamentsByYouList.value = it.toObjects<Tournament>().filter { it.exist }.toArrayList()
                tournamentsByYouList.notifyObserver()
            }
            .addOnFailureListener {
                Log.e("Active tournaments", "Fetch failed: $it")
            }
    }

    fun joinTournament(tournament: Tournament, successAction: () -> Unit) {
        val userTournament = getUserTournament(tournament)
        val uid = sharedPrefsRepository.user.uid

        updateUserSharedPrefsData(userTournament)

        firestoreRepository.updateUserData(uid,
            mapOf("currentTournaments.${tournament.name}" to userTournament))
            .addOnSuccessListener {
                //updateUserSharedPrefsData(userTournament)
                messageDisplayed = false
                statusMessage.value = "You are now a part of ${tournament.name}"

                var index = activeTournamentsList.value?.indexOf(tournament)!!
                activeTournamentsList.value?.get(index)?.players?.add(uid)
                activeTournamentsList.notifyObserver()

                index = tournamentsByYouList.value?.indexOf(tournament)!!
                if (index != -1) tournamentsByYouList.value?.get(index)?.players?.add(uid)
                tournamentsByYouList.notifyObserver()

                //Do this to display the leaf count as soon as the user joins the challenge
                /*if (tournament.type == TOURNAMENT_TYPE_DAILY_GOAL_BASED) {*/
                    userTournament.leafCount = sharedPrefsRepository.getDailyStepCount() / 1000
             //   }
                val user = sharedPrefsRepository.user
                user.currentTournaments[tournament.name] = userTournament
                sharedPrefsRepository.user = user

                successAction()
            }
            .addOnFailureListener {
                messageDisplayed = false
                statusMessage.value = "Error joining tournament"
                Log.e("Error joiningtournament", it.toString())
            }

        firestoreRepository.updateTournamentData(tournament.name,
            mapOf("players" to FieldValue.arrayUnion(sharedPrefsRepository.user.uid)))

        currentTournamentsList.value?.add(tournament)
        currentTournamentsList.notifyObserver()

        //Update data as soon as user joins a tournament
        val updateUserTournamentDataRequest =
            OneTimeWorkRequestBuilder<UpdateUserTournamentDataWorker>().build()
        WorkManager.getInstance().enqueue(updateUserTournamentDataRequest).result.addListener(
            Runnable {
                Log.d("Tournament data", "updated by work manager")
            }, MoreExecutors.directExecutor())
    }

    fun leaveTournament(tournament: Tournament) {
        val userId = sharedPrefsRepository.user.uid
        var counter = 0

        firestoreRepository.deleteUserFromTournamentDB(tournament, userId)
            .addOnSuccessListener {
                synchronized(counter) {
                    counter++
                    if (counter == 2) {
                        removeTournamentFromCurrentTournamentsLists(tournament)
                    }
                }
                Log.d("Tournament deleted", "from DB")
            }
            .addOnFailureListener {
                Log.e("Tournament deletefailed", it.toString())
            }

        val userTournament = getUserTournament(tournament).let {
            it.isCurrentTournament = false
            it
        }

        //TODO: Maybe later on we can think of only disabling the Tournament instead of actually deleting from the database
        firestoreRepository.deleteTournamentFromUserDB(userId, userTournament, userTournament.toJson<UserTournament>())
            .addOnSuccessListener {
                synchronized(counter) {
                    counter++
                    if (counter == 2) {
                        removeTournamentFromCurrentTournamentsLists(tournament)
                    }
                }
                statusMessage.value = "Success"
            }
            .addOnFailureListener {
                statusMessage.value = "Failed"
            }

        var index = activeTournamentsList.value?.indexOf(tournament)!!
        activeTournamentsList.value?.get(index)?.players?.remove(sharedPrefsRepository.user.uid)
        activeTournamentsList.notifyObserver()

        index = currentTournamentsList.value?.indexOf(tournament)!!
        currentTournamentsList.value?.get(index)?.players?.remove(sharedPrefsRepository.user.uid)
        currentTournamentsList.notifyObserver()
    }

    fun deleteTournament(tournament: Tournament) {
        firestoreRepository.setTournamentAsNonExist(tournament.name)
            .addOnSuccessListener {
                activeTournamentsList.value?.remove(tournament)
                activeTournamentsList.notifyObserver()

                currentTournamentsList.value?.remove(tournament)
                currentTournamentsList.notifyObserver()

                tournamentsByYouList.value?.remove(tournament)
                tournamentsByYouList.notifyObserver()
            }
            .addOnFailureListener {
                Log.e("Deletion failed", it.toString())
            }
    }

    fun startUnityActivityForTournament(tournament: Tournament, action: () -> Unit) {
        sharedPrefsRepository.apply {

            val userTournament = user.currentTournaments[tournament.name]!!
            gameMode = TOURNAMENT_MODE
            tournamentType = userTournament.type
            tournamentGoal = userTournament.goal
            tournamentLeafCount = userTournament.leafCount
            tournamentFruitCount = userTournament.fruitCount
            tournamentStreak = userTournament.tournamentGoalStreak
            tournamentName = userTournament.name
            isTournamentActive = userTournament.endDate.toDateTime().millis > DateTime().millis
            tournamentTotalStepsCount = if (tournament.active) getDailyStepCount() else userTournament.totalSteps

            action()
        }
    }

    fun getTournamentDurationText(tournament: Tournament): SpannedString {
        val finishDate = tournament.finishTimestamp.toDateTime().millis
        val finishDateString = tournament.finishTimestamp.toDateTime().getStringRepresentation()

        val tournamentEnded = finishDate < DateTime().millis

        return buildSpannedString {
            bold {
                append(if (tournamentEnded) "Ended: " else "Ends: ")
            }
            append(finishDateString)
        }
    }

    fun hasUserJoinedTournament(tournament: Tournament): Boolean {
        return sharedPrefsRepository.user.currentTournaments[tournament.name] != null
    }

 /*   fun getTournamentTypeText(tournament: Tournament) =
        buildSpannedString {
            bold {
                append("Type: ")
            }
            append(if (tournament.type == TOURNAMENT_TYPE_DAILY_GOAL_BASED) "Daily Goal Based"
            else "Aggregate based")
        }
*/
    fun getGoalText(tournament: Tournament) =
        buildSpannedString {
            bold {
                append("Minimum Daily Goal: ")
                /*append(if(tournament.type == TOURNAMENT_TYPE_DAILY_GOAL_BASED) "Minimum Daily Goal: "
                else "Total steps goal: ")*/
            }
            append(tournament.goal.toString())
        }

    //fun getPlayersCountText(tournament: Tournament) = tournament.players.size.toString()
    fun getTeam1CountText(tournament: Tournament) = tournament.team1.size.toString()
    fun getTeam2CountText(tournament: Tournament) = tournament.team2.size.toString()

    fun getCurrentUserId() = sharedPrefsRepository.user.uid

    fun getJoinTournamentDialogTitleText(tournament: Tournament) =
        buildSpannedString {
            append("Join tournament")
            bold {
                append("'${tournament.name}'")
            }
        }

    fun getJoinTournamemtMessageText() = "Are you ready to join now?"

    fun storeTournamentLeaderboardPosition(position: Int) {
        sharedPrefsRepository.tournamentLeaderboardPosition = position
    }

    private fun updateUserSharedPrefsData(userTournament: UserTournament) {
        val user = sharedPrefsRepository.user
        userTournament.leafCount = sharedPrefsRepository.getDailyStepCount() / 1000
        userTournament.totalSteps = sharedPrefsRepository.getDailyStepCount()
        user.currentTournaments[userTournament.name] = userTournament
        sharedPrefsRepository.user = user
    }

    private fun getUserTournament(tournament: Tournament) =
        UserTournament(
            name = tournament.name,
            dailyStepsMap = mutableMapOf(),
            totalSteps = sharedPrefsRepository.getDailyStepCount(),
            joinDate = DateTime().millis,
            //type = tournament.type,
            goal = tournament.goal,
            endDate = tournament.finishTimestamp
        )

    private fun removeTournamentFromCurrentTournamentsLists(tournament: Tournament) {
        currentTournamentsList.value?.remove(tournament)
        currentTournamentsList.notifyObserver()

        sharedPrefsRepository.user = sharedPrefsRepository.user.let {
            it.currentTournaments.remove(tournament.name)
            it
        }
    }

    private fun ArrayList<Tournament>.sortAndAddToList(tournament: Tournament) {
        val finishTimestampMillis = tournament.finishTimestamp.toDateTime().millis
        if (size == 0) {
            add(tournament)
            return
        }

        for(i in 0 until size) {
            if (this[i].finishTimestamp.toDateTime().millis < finishTimestampMillis) {
                add(i, tournament)
                return
            }
        }
        this.add(tournament)
    }
}