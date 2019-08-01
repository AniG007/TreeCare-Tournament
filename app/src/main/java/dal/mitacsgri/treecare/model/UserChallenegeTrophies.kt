package dal.mitacsgri.treecare.model

data class UserChallenegeTrophies(
    val gold: ArrayList<String> = arrayListOf(),
    val silver: ArrayList<String> = arrayListOf(),
    val bronze: ArrayList<String> = arrayListOf()
)