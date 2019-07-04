package dal.mitacsgri.treecare.screens.challenges.challengesbyyou

import android.content.DialogInterface
import android.view.View
import androidx.navigation.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dal.mitacsgri.treecare.model.Challenge
import dal.mitacsgri.treecare.screens.BaseViewHolder
import dal.mitacsgri.treecare.screens.challenges.ChallengesFragmentDirections
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
                MaterialAlertDialogBuilder(context)
                    .setTitle("Delete challenge")
                    .setMessage("Do you really want to delete the challenge '${item.name}' ?")
                    .setPositiveButton("No") { dialogInterface: DialogInterface, i: Int ->
                        dialogInterface.dismiss()
                    }
                    .setNegativeButton("Yes") { _: DialogInterface, _: Int ->
                        viewModel.deleteChallenge(item)
                    }
                    .show()
            }

            buttonJoin.setOnClickListener {
                MaterialAlertDialogBuilder(context)
                    .setTitle("Join the challenge")
                    .setMessage("Do you really want to join the challenge '${item.name}'")
                    .setPositiveButton("Yes") { _: DialogInterface, _: Int ->
                        viewModel.joinChallenge(item)
                    }
                    .setNegativeButton("No") { dialogInterface: DialogInterface, i: Int ->
                        dialogInterface.dismiss()
                    }
                    .show()
            }

            buttonLeaderBoard.setOnClickListener {
                val action = ChallengesFragmentDirections
                    .actionChallengesFragmentToLeaderboardFragment(item.name)
                findNavController().navigate(action)
            }
        }
    }
}