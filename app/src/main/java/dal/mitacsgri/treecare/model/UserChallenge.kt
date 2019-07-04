package dal.mitacsgri.treecare.model

data class UserChallenge(
    val name: String,
    val dailyStepsMap: MutableMap<String, Int>,
    var totalSteps: Int,
    val challengeGoalStreak: Int = 0,
    val joinDate: Long,
    var isCurrentChallenge: Boolean = true,
    val type: Int
)