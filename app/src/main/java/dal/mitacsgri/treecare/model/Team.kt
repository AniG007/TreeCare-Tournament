package dal.mitacsgri.treecare.model

import com.google.firebase.Timestamp

data class Team (
    val name: String = "",
    val description: String = "",
    val members: ArrayList<String> = arrayListOf(),
    val invitedMembers: ArrayList<String> = arrayListOf(),
    val joinRequests: ArrayList<String> = arrayListOf(),
    val currentTournaments: ArrayList<String> = arrayListOf(),
    val captain: String = "",
    val captainName: String = "",
    val creationTimestamp: Timestamp = Timestamp.now(),
    val newCaptain: String = ""
)