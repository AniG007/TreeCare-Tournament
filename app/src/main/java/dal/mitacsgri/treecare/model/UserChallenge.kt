package dal.mitacsgri.treecare.model

data class UserChallenge(
    val name: String = "",
    val dailyStepsMap: MutableMap<String, Int> = mutableMapOf(),
    var totalSteps: Int = 0,
    var challengeGoalStreak: Int = 0,
    val joinDate: Long = 0,
    var isCurrentChallenge: Boolean = true,
    val type: Int = 0,
    var leafCount: Int = 0,
    val goal: Int = 0,
    var isActive: Boolean = true
)