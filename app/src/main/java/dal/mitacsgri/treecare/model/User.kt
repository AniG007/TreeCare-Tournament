package dal.mitacsgri.treecare.model

import org.joda.time.DateTime

/**
 * Created by Devansh on 22-06-2019
 */

data class User (
    val uid: String = "",
    val isFirstRun: Boolean = false,
    val firstLoginTime: Long = 0,
    val name: String = "",
    val email: String = "",
    val dailyGoalMap: MutableMap<String, Int> = mutableMapOf(
        DateTime(firstLoginTime).withTimeAtStartOfDay().millis.toString() to 5000,
        DateTime(firstLoginTime).plusDays(1).withTimeAtStartOfDay().millis.toString() to 5000
    ),
    var lastGoalChangeTime: Long = DateTime(firstLoginTime).withTimeAtStartOfDay().millis,
    val currentChallenges: MutableMap<String, String> = mutableMapOf(),
    val photoUrl: String = "https://lh6.googleusercontent.com/-q4mJL3wLwdI/AAAAAAAAAAI/AAAAAAAAPBY/MObBN5tWYQE/s96-c/photo.jpg"
)