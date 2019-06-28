package dal.mitacsgri.treecare.screens.challenges.challengesbyyou

import android.view.View
import dal.mitacsgri.treecare.data.Challenge
import dal.mitacsgri.treecare.screens.BaseViewHolder
import kotlinx.android.synthetic.main.item_challenge_by_you.view.*

/**
 * Created by Devansh on 28-06-2019
 */
class ChallengesByYouViewHolder(
    itemView: View,
    private val viewModel: ChallengesByYouViewModel
    ): BaseViewHolder<Challenge>(itemView) {

    override fun bind(item: Challenge) {
        itemView.apply {
            nameTV.text = item.name
            descriptionTV.text = item.description
            durationTV.text = viewModel.getChallengeDurationText(item)
            participantsTV.text = viewModel.getParticipantsCountString(item)

            buttonDelete.setOnClickListener {
                viewModel.deleteChallenge(item)
            }
        }
    }
}