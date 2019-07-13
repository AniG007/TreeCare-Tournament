package dal.mitacsgri.treecare.model

import com.google.firebase.Timestamp

data class Tournament (
    val name: String = "",
    val dailyGoal: Int = 0,
    val teams: ArrayList<Team> = arrayListOf(),
    val creationTimestamp: Timestamp = Timestamp.now(),
    val finishTimestamp: Timestamp = Timestamp.now(),
    val isActive: Boolean = true,
    val teamSize: Int = 0
)