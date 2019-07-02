package dal.mitacsgri.treecare.screens.challenges.currentchallenges

import android.view.View
import dal.mitacsgri.treecare.model.Challenge
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
            challengeTypeTV.text = viewModel.getChallengeTypeText(item)
            goalTV.text = viewModel.getGoalText(item)
            durationTV.text = viewModel.getChallengeDurationText(item)
            playersTV.text = viewModel.getPlayersCountText(item)

            buttonTree.setOnClickListener {

            }
            buttonLeaderBoard.setOnClickListener {

            }

            buttonExit.setOnClickListener {
                viewModel.leaveChallenge(item)
            }
        }
    }
}