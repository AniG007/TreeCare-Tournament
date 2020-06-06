package dal.mitacsgri.treecare.screens.tournaments.currenttournaments


import android.content.DialogInterface
import android.content.Intent
import android.view.View
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dal.mitacsgri.treecare.extensions.toast
import dal.mitacsgri.treecare.model.Challenge
import dal.mitacsgri.treecare.model.Tournament
import dal.mitacsgri.treecare.screens.BaseViewHolder
import dal.mitacsgri.treecare.screens.tournaments.TournamentsFragmentDirections
import dal.mitacsgri.treecare.screens.tournaments.TournamentsViewModel
import dal.mitacsgri.treecare.screens.treecareunityactivity.TreeCareUnityActivity
import kotlinx.android.synthetic.main.item_active_tournament.view.*
import kotlinx.android.synthetic.main.item_current_tournament.view.*
import kotlinx.android.synthetic.main.item_current_tournament.view.buttonLeaderBoard
import kotlinx.android.synthetic.main.item_current_tournament.view.durationTV
import kotlinx.android.synthetic.main.item_current_tournament.view.goalTV
import kotlinx.android.synthetic.main.item_current_tournament.view.nameTV
import kotlinx.android.synthetic.main.item_current_tournament.view.teamCountTV

/**
 * Created by Anirudh on 25-04-2020
 */
class CurrentTournamentsViewHolder(
    itemView: View,
    private val viewModel: TournamentsViewModel
): BaseViewHolder<Tournament>(itemView) {

    override fun bind(item: Tournament) {
        itemView.apply {
            nameTV.text = item.name
            goalTV.text = viewModel.getGoalText(item)
            durationTV.text = viewModel.getTournamentDurationText(item)
            //team1CountTV.text = viewModel.getTeamsCountText(item)
            teamCountTV.text = viewModel.getTeamsCountText(item)

            buttonTree.setOnClickListener {
                viewModel.startUnityActivityForTournament(item) {
                    context.startActivity(Intent(context, TreeCareUnityActivity::class.java))
                }
            }

            buttonLeaderBoard.setOnClickListener {
                val action = TournamentsFragmentDirections
                    .actionTournamentsFragmentToLeaderboardFragment(item.name) //connect from tournaments to leaderboard
                findNavController().navigate(action)
            }

            buttonExit.visibility = if (item.active) View.VISIBLE else View.INVISIBLE

            buttonExit.setOnClickListener {
                MaterialAlertDialogBuilder(context)
                    .setTitle("Leave tournament")
                    .setMessage("Do you really want to leave the tournament '${item.name}' ?")
                    .setPositiveButton("No") { dialogInterface: DialogInterface, i: Int ->
                        dialogInterface.dismiss()
                    }
                    .setNegativeButton("Yes") { _: DialogInterface, _: Int ->
                        viewModel.leaveTournament(item)
                        //findNavController().navigateUp()
                    }
                    .show()
            }
        }
    }
}