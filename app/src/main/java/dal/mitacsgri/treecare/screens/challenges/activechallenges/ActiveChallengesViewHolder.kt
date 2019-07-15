package dal.mitacsgri.treecare.screens.challenges.activechallenges

import android.content.DialogInterface
import android.view.View
import androidx.navigation.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dal.mitacsgri.treecare.extensions.disable
import dal.mitacsgri.treecare.model.Challenge
import dal.mitacsgri.treecare.screens.BaseViewHolder
import dal.mitacsgri.treecare.screens.challenges.ChallengesFragmentDirections
import kotlinx.android.synthetic.main.item_active_challenge.view.*

/**
 * Created by Devansh on 25-06-2019
 */

class ActiveChallengesViewHolder(
    itemView: View,
    private val viewModel: ActiveChallengesViewModel
    ): BaseViewHolder<Challenge>(itemView) {

    override fun bind(item: Challenge) {
        itemView.apply {
            nameTV.text = item.name
            descriptionTV.text = item.description
            durationTV.text = viewModel.getChallengeDurationText(item)
            teamsCountTV.text = viewModel.getParticipantsCountString(item)
            challengeTypeTV.text = viewModel.getChallengeTypeText(item)
            goalTV.text = viewModel.getGoalText(item)

            if (item.active) {
                buttonJoin.setOnClickListener {
                    MaterialAlertDialogBuilder(context)
                        .setTitle("Join the challenge")
                        .setMessage("Do you really want to join the challenge '${item.name}'")
                        .setPositiveButton("Yes") { _: DialogInterface, _: Int ->
                            viewModel.joinChallenge(item)
                        }
                        .setNegativeButton("No") { dialogInterface: DialogInterface, _: Int ->
                            dialogInterface.dismiss()
                        }
                        .show()
                }
            } else {
                buttonJoin.disable()
            }

            buttonLeaderBoard.setOnClickListener {
                val action = ChallengesFragmentDirections
                    .actionChallengesFragmentToLeaderboardFragment(item.name)
                findNavController().navigate(action)
            }
        }
    }
}