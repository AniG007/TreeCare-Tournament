package dal.mitacsgri.treecare.model

data class UserChallenge(
    val name: String,
    val dailyStepsMap: MutableMap<String, Int>,
    var totalSteps: Int,
    var challengeGoalStreak: Int = 0,
    val joinDate: Long,
    var isCurrentChallenge: Boolean = true,
    val type: Int,
    var leafCount: Int = 0,
    val goal: Int = 5000,
    var isActive: Boolean = true
)