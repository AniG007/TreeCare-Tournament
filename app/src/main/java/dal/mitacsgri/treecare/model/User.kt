package dal.mitacsgri.treecare.model

import dal.mitacsgri.treecare.extensions.getMapFormattedDate
import org.joda.time.DateTime

/**
 * Created by Devansh on 22-06-2019
 */

data class User (
    val uid: String = "",
    val isFirstRun: Boolean = false,
    val firstLoginTime: Long = 0,
    var name: String = "",
    val email: String = "",
    var dailyGoalMap: MutableMap<String, Int> = mutableMapOf(
        DateTime(firstLoginTime).getMapFormattedDate() to 5000,
        DateTime(firstLoginTime).plusDays(1).getMapFormattedDate() to 5000
    ),
    var lastGoalChangeTime: Long = DateTime(firstLoginTime).withTimeAtStartOfDay().millis,
    val currentChallenges: MutableMap<String, UserChallenge> = mutableMapOf(),
    val currentTeams: ArrayList<String> = arrayListOf(),
    val teamJoinRequests: ArrayList<String> = arrayListOf(),
    val photoUrl: String = "",
    val dailyGoalStreakCount: Int = 0
)