package dal.mitacsgri.treecare.data

import java.util.*

/**
 * Created by Devansh on 25-06-2019
 */

data class Challenge(
    val name: String = "",
    val description: String = "",
    val creationTimestamp: Date = Date(),
    val finishTimestamp: Date = Date(),
    val players: List<String> = arrayListOf()
)