package dal.mitacsgri.treecare.data

import com.google.firebase.Timestamp

/**
 * Created by Devansh on 25-06-2019
 */

data class Challenge(
    val name: String = "",
    val description: String = "",
    val creationTimestamp: Timestamp = Timestamp.now(),
    val finishTimestamp: Timestamp = Timestamp.now(),
    val type: Int = 0,
    val goal: Int = 5000,
    val players: List<String> = arrayListOf(),
    val isExist: Boolean = true,
    val isActive: Boolean = true,
    val creatorName: String = "",
    val creatorUId: String = ""
)