package dal.mitacsgri.treecare.screens.currentchallenges

import android.view.View
import dal.mitacsgri.treecare.data.Challenge
import dal.mitacsgri.treecare.screens.BaseViewHolder
import kotlinx.android.synthetic.main.item_current_challenge.view.*

/**
 * Created by Devansh on 25-06-2019
 */
class CurrentChallengesViewHolder(
    itemView: View,
    private val viewModel: CurrentChallengesViewModel
    ): BaseViewHolder<Challenge>(itemView) {

    override fun bind(item: Challenge) {
        itemView.apply {
            nameTV.text = item.name
            descriptionTV.text = item.description
            durationTV.text = viewModel.getChallengeDurationText(item)
        }
    }
}