package dal.mitacsgri.treecare.data

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
    val currentChallenges: ArrayList<String> = arrayListOf()
)