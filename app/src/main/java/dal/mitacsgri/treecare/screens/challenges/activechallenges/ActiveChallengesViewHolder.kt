package dal.mitacsgri.treecare.screens.challenges.activechallenges

import android.view.View
import dal.mitacsgri.treecare.data.Challenge
import dal.mitacsgri.treecare.screens.BaseViewHolder
import kotlinx.android.synthetic.main.item_active_challenge.view.*

/**
 * Created by Devansh on 25-06-2019
 */

class ActiveChallengesViewHolder(itemView: View, private val viewModel: ActiveChallengesViewModel)
    : BaseViewHolder<Challenge>(itemView) {

    override fun bind(item: Challenge) {
        itemView.apply {
            nameTV.text = item.name
            descriptionTV.text = item.description
            durationTV.text = viewModel.getChallengeDurationText(item)
            participantsTV.text = viewModel.getParticipantsCountString(item)

            buttonJoin.setOnClickListener {
                viewModel.joinChallenge(item)
            }
        }
    }
}