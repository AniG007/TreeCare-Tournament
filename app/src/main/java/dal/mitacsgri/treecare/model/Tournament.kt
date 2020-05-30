package dal.mitacsgri.treecare.model

import com.google.firebase.Timestamp

data class Tournament (
    val name: String = "",
    val dailyGoal: Int = 0,
    val description: String = "",
   // val type: Int = 0,
    val active: Boolean = true,
    val goal: Int = 5000,
//    val team1: ArrayList<Team> = arrayListOf(),
//    val team2: ArrayList<Team> = arrayListOf(),
    val teams: ArrayList<String> = arrayListOf(),
    val creationTimestamp: Timestamp = Timestamp.now(),
    //var players: ArrayList<String> = arrayListOf(),
    //val startTimestamp: Timestamp = Timestamp.now(),
    val finishTimestamp: Timestamp = Timestamp.now(),
    //val isActive: Boolean = true,
    val exist: Boolean = true,
    val creatorName: String = "",
    val creatorUId: String = "",
    val teamLimit: Int = 0
){

    override fun equals(other: Any?): Boolean {
    other as Tournament
    return (name == other.name)
}

override fun hashCode(): Int {
    return name.length
}
}