package dal.mitacsgri.treecare.model

data class UserChallenge(
    val name: String,
    val dailyStepsMap: Map<String, Int>,
    val totalSteps: Int,
    val joinDate: Long,
    var isCurrentChallenge: Boolean = true
)