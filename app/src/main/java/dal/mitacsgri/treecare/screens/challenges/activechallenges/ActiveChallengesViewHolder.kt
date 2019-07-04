package dal.mitacsgri.treecare.screens.challenges.activechallenges

import android.content.DialogInterface
import android.view.View
import androidx.navigation.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dal.mitacsgri.treecare.model.Challenge
import dal.mitacsgri.treecare.screens.BaseViewHolder
import dal.mitacsgri.treecare.screens.challenges.ChallengesFragmentDirections
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
                MaterialAlertDialogBuilder(context)
                    .setTitle("Join the challenge")
                    .setMessage("Do you really want to join the challenge '${item.name}'")
                    .setPositiveButton("Yes") { dialogInterface: DialogInterface, i: Int ->
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