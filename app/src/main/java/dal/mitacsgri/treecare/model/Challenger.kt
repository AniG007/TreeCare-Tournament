package dal.mitacsgri.treecare.model

data class Challenger(
    val name: String,
    val uid: String,
    val photoUrl: String,
    val challengeGoalStreak: Int,
    val totalSteps: Int
)