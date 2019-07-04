package dal.mitacsgri.treecare.screens.leaderboard

import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
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
            Glide.with(this).load(item.photoUrl)
                .apply(RequestOptions.circleCropTransform())
                .into(imageView)
        }
    }
}