package dal.mitacsgri.treecare.model

import com.google.firebase.Timestamp

data class Team (
    val name: String = "",
    val players: ArrayList<String> = arrayListOf(),
    val currentTournaments: ArrayList<String> = arrayListOf(),
    val captain: String = "",
    val creationTimestamp: Timestamp = Timestamp.now(),
    val newCaptain: String = ""
)