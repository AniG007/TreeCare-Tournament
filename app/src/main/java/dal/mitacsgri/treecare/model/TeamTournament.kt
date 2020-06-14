package dal.mitacsgri.treecare.model

import com.google.firebase.Timestamp

data class TeamTournament(
    val name: String = "",
    val dailyStepsMap: MutableMap<String, Int> = mutableMapOf(),
    var totalSteps: Int = 0,
    var tournamentGoalStreak: Int = 0,
    val joinDate: Long = 0,
    var isCurrentTournament: Boolean = true,
    val type: Int = 0,
    var leafCount: Int = 0,
    var fruitCount: Int = 0,
    var currentDayOfWeek: Int = 0,
    val goal: Int = 0,
    var isActive: Boolean = true,
    val startDate: Timestamp = Timestamp.now(),
    val endDate: Timestamp = Timestamp.now(),
    var lastUpdateTime: Timestamp = Timestamp.now(),
    var steps: Int = 0
)
