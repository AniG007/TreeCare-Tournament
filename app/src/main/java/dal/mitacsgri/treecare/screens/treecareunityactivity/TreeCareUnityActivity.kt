package dal.mitacsgri.treecare.screens.treecareunityactivity

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ktx.toObject
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.consts.*
import dal.mitacsgri.treecare.extensions.getMapFormattedDate
import dal.mitacsgri.treecare.model.*
import dal.mitacsgri.treecare.repository.FirestoreRepository
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import dal.mitacsgri.treecare.screens.gamesettings.SettingsActivity
import dal.mitacsgri.treecare.screens.leaderboard.LeaderboardActivity
import dal.mitacsgri.treecare.screens.progressreport.ProgressReportActivity
import dal.mitacsgri.treecare.screens.tournamentleaderboard.TournamentLeaderBoardActivity
import dal.mitacsgri.treecare.services.StepDetectorService
import dal.mitacsgri.treecare.unity.UnityPlayerActivity
import org.joda.time.DateTime
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.util.*

/*
 * Created by Devansh on 24-06-2019*
 * Updated by Anirudh for Tournament Mode
 */


class TreeCareUnityActivity : UnityPlayerActivity(), KoinComponent {

    private val sharedPrefsRepository:  SharedPreferencesRepository by inject()
    private val firestoreRepository: FirestoreRepository by inject()

    private val TAG: String = "SensorAPI"
    private var volume = 0f
    private var isSoundFadingIn = true

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var audioManager: AudioManager

    //Called from Unity
    fun OpenSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    fun OpenHelp() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getHelpTitle())
            .setMessage(getHelpText())
            .setNegativeButton("Close") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            .show()
    }

    fun OpenProgressReport() {
        startActivity(Intent(this, ProgressReportActivity::class.java))
    }

    fun OpenLeaderboard() {
        startActivity(Intent(this, LeaderboardActivity::class.java))
    }

    fun OpenTournamentLeaderboard(){
        startActivity(Intent(this, TournamentLeaderBoardActivity::class.java))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService()

        sharedPrefsRepository.apply {
            if (gameMode == STARTER_MODE) {
                sharedPrefsRepository.storeDailyStepsGoal(
                    sharedPrefsRepository.user.dailyGoalMap[DateTime().getMapFormattedDate()] ?: 5000)
            }
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val res = audioManager.requestAudioFocus(audioFocusChangedListener, AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN)

        if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mediaPlayer = MediaPlayer.create(this, R.raw.tree_background_sound)
            mediaPlayer.isLooping = true
            startFadeIn()
        }

        if (sharedPrefsRepository.gameMode == CHALLENGER_MODE) {
            getChallengersListAndCurrentPosition(sharedPrefsRepository.challengeName!!)
        }

        if(sharedPrefsRepository.gameMode == TOURNAMENT_MODE){
            getTeamsListAndCurrentPosition(sharedPrefsRepository.tournamentName!!)
        }

    }

    override fun onStart() {
        super.onStart()
        if (!isSoundFadingIn) {
            val volume = sharedPrefsRepository.volume
            mediaPlayer.setVolume(volume, volume)
        }
        mediaPlayer.start()
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer.pause()
        isSoundFadingIn = false
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService()
        mediaPlayer.stop()
        audioManager.abandonAudioFocus(audioFocusChangedListener)
    }

    private fun startService() {
        val serviceIntent = Intent(applicationContext, StepDetectorService::class.java)
        serviceIntent.putExtra("inputExtra", "Foreground Service Example in Android")
        ContextCompat.startForegroundService(applicationContext, serviceIntent)
    }

    private fun stopService() {
        val serviceIntent = Intent(applicationContext, StepDetectorService::class.java)
        stopService(serviceIntent)
    }

    private fun startFadeIn() {
        val FADE_DURATION = 6000
        val FADE_INTERVAL = 100L
        val MAX_VOLUME = sharedPrefsRepository.volume
        val numberOfSteps = FADE_DURATION / FADE_INTERVAL
        val deltaVolume = MAX_VOLUME / numberOfSteps.toFloat()

        isSoundFadingIn = true

        val timer = Timer(true)
        val timerTask = object : TimerTask() {
            override fun run() {
                fadeInStep(deltaVolume)
                if (volume >= MAX_VOLUME) {
                    timer.cancel()
                    timer.purge()
                    isSoundFadingIn = false
                }
            }
        }

        timer.schedule(timerTask, FADE_INTERVAL, FADE_INTERVAL)
    }

    private fun fadeInStep(deltaVolume: Float) {
        mediaPlayer.setVolume(volume, volume)
        volume += deltaVolume
    }

    private fun getHelpText() =
        when(sharedPrefsRepository.gameMode) {
            STARTER_MODE -> getString(R.string.starter_mode_instructions)
            CHALLENGER_MODE -> getString(R.string.challenger_mode_instructions)
            TOURNAMENT_MODE -> getString(R.string.tournament_mode_instructions)
            else -> ""
        }

    private fun getHelpTitle() =
        buildSpannedString {
            bold {
                append("Help: ")
                append(when (sharedPrefsRepository.gameMode) {
                    STARTER_MODE -> getString(R.string.starter_mode)
                    CHALLENGER_MODE -> getString(R.string.challenger_mode)
                    else -> ""
                })
            }
        }

    private val audioFocusChangedListener = AudioManager.OnAudioFocusChangeListener {
        when(it) {
            AudioManager.AUDIOFOCUS_LOSS -> mediaPlayer.pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> mediaPlayer.pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                val volume = sharedPrefsRepository.volume
                if (volume < 0.2) {
                    mediaPlayer.setVolume(volume, volume)
                } else
                    mediaPlayer.setVolume(0.2f, 0.2f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> mediaPlayer.start()
        }
    }

    fun getCurrentChallengerPosition(challengers: ArrayList<Challenger>): Int {

        val currentUserUid = sharedPrefsRepository.user.uid
        for (i in 0 until challengers.size) {
            if (challengers[i].uid == currentUserUid)
                return i+1
        }
        return -1
    }

    fun getCurrentTeamPosition(teams: ArrayList<TeamTournament>): Int {

        val currentTeam = sharedPrefsRepository.team.name
        for (i in 0 until teams.size) {
            if (teams[i].teamName == currentTeam)
                return i+1
        }
        return -1
    }

    private fun getChallengersListAndCurrentPosition(challengeName: String) {
        val challengersList = arrayListOf<Challenger>()

        firestoreRepository.getChallenge(challengeName)
            .addOnSuccessListener {
                val challenge = it.toObject<Challenge>() ?: Challenge()
                val challengers = challenge.players
                val challengersCount = challengers.size
                val limit = if (challengersCount > 10) 10 else challengersCount

                for (i in 0 until limit) {
                    firestoreRepository.getUserData(challengers[i])
                        .addOnSuccessListener {
                            val user = it.toObject<User>()
                            val challenger = user?.let { makeChallengerFromUser(user, challenge) }
                            challengersList.add(challenger!!)

                            if (challengersList.size == limit) {
                                challengersList.sortChallengersList(challenge.type)

                                sharedPrefsRepository.challengeLeaderboardPosition =
                                    getCurrentChallengerPosition(challengersList)
                            }
                        }
                }
            }
    }

    private fun makeChallengerFromUser(user: User, challenge: Challenge): Challenger {
        val userChallengeData = user.currentChallenges[challenge.name] ?: UserChallenge()

        return Challenger(
            name = user.name,
            uid = user.uid,
            photoUrl = user.photoUrl,
            challengeGoalStreak = userChallengeData.challengeGoalStreak,
            totalSteps = userChallengeData.totalSteps,
            totalLeaves = userChallengeData.leafCount)
    }

    private fun getTeamsListAndCurrentPosition(tournamentName: String) {
        val teamsList = arrayListOf<TeamTournament>()

        firestoreRepository.getTournament(tournamentName)
            .addOnSuccessListener {
                val tournament = it.toObject<Tournament>() ?: Tournament()
                val teams = tournament.teams
                val teamsCount = teams.size
                val limit = teamsCount

                for (i in 0 until teamsCount) {
                    firestoreRepository.getTeam(teams[i])
                        .addOnSuccessListener {
                            val team = it.toObject<Team>()
                            val totalSteps = team?.currentTournaments!![tournamentName]?.totalSteps
                            val teamTournament = team.let { getTeamTournament(team, tournament, team.name, totalSteps) }
                            teamsList.add(teamTournament)

                            if (teamsList.size == limit) {
                                teamsList.sortTeamsList()
                                sharedPrefsRepository.tournamentLeaderboardPosition =
                                    getCurrentTeamPosition(teamsList)
                            }
                        }
                }
            }
    }

    private fun getTeamTournament(team:Team, tournament: Tournament, teamName: String, steps: Int?) : TeamTournament {

        val teamTournamentData = team.currentTournaments[tournament.name] ?: TeamTournament()

        return TeamTournament(
            name = tournament.name,
            dailyStepsMap    = mutableMapOf(),
            totalSteps = steps!!,
            leafCount = teamTournamentData.leafCount,
            joinDate = DateTime().millis,
            goal = tournament.dailyGoal,
            startDate = tournament.startTimestamp,
            endDate = tournament.finishTimestamp,
            teamName = teamName
        )
    }

    private fun ArrayList<Challenger>.sortChallengersList(challengeType: Int) {
        sortByDescending {
            when (challengeType) {
                CHALLENGE_TYPE_DAILY_GOAL_BASED -> it.totalSteps
                CHALLENGE_TYPE_AGGREGATE_BASED -> it.totalSteps
                else -> it.totalSteps
            }
        }
    }

    private fun ArrayList<TeamTournament>.sortTeamsList() {
        sortByDescending {
            it.totalSteps
        }
    }
}
