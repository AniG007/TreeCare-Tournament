package dal.mitacsgri.treecare.screens.leaderboard

import android.util.Log
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import dal.mitacsgri.treecare.model.Challenger
import dal.mitacsgri.treecare.screens.BaseViewHolder
import kotlinx.android.synthetic.main.item_leaderboard.view.*

class LeaderboardItemViewHolder(
    itemView: View,
    private val viewModel: LeaderboardItemViewModel
    ): BaseViewHolder<Challenger>(itemView) {

    override fun bind(item: Challenger) {
        itemView.apply {
            nameTV.text = item.name
            achievementTV.text = viewModel.getAchievementText(item)

            val user = FirebaseAuth.getInstance().currentUser
            user?.let {
                // Name, email address, and profile photo Url
                val name = user.displayName
                val email = user.email
                val photoUrl = user.photoUrl
                Log.d("photourl", photoUrl.toString())

                // Check if user's email is verified
                val emailVerified = user.isEmailVerified

                // The user's ID, unique to the Firebase project. Do NOT use this value to
                // authenticate with your backend server, if you have one. Use
                // FirebaseUser.getToken() instead.
                val uid = user.uid
            }
        }
    }
}