package data

/**
 * Created by Devansh on 22-06-2019
 */

data class User (
    val uid: String = "",
    val isFirstRun: Boolean = false,
    val firstLoginTime: Long = 0,
    val name: String = "",
    val email: String = "",
    val dailyGoalMap: MutableMap<String, Int> = mutableMapOf()
)