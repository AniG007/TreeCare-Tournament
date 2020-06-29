package dal.mitacsgri.treecare.screens.tournaments.activetournaments

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.util.Log
import android.view.View
import androidx.navigation.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dal.mitacsgri.treecare.di.sharedPreferencesRepositoryModule
import dal.mitacsgri.treecare.extensions.disable
import dal.mitacsgri.treecare.extensions.enable
import dal.mitacsgri.treecare.model.Tournament
import dal.mitacsgri.treecare.repository.SharedPreferencesRepository
import dal.mitacsgri.treecare.screens.BaseViewHolder
import dal.mitacsgri.treecare.screens.tournaments.TournamentsViewModel
import kotlinx.android.synthetic.main.item_active_tournament.view.*
import kotlinx.android.synthetic.main.item_active_tournament.view.durationTV
import kotlinx.android.synthetic.main.item_active_tournament.view.goalTV
import kotlinx.android.synthetic.main.item_active_tournament.view.nameTV
import kotlinx.android.synthetic.main.item_active_tournament.view.teamCountTV
import kotlinx.android.synthetic.main.item_current_challenge.*
import kotlinx.android.synthetic.main.item_current_tournament.view.*

//import kotlinx.android.synthetic.main.item_current_tournament.view.*

class ActiveTournamentsViewHolder(
    private val viewModel: TournamentsViewModel,
    itemView: View): BaseViewHolder<Tournament>(itemView) {

    @SuppressLint("SetTextI18n")
    override fun bind(item: Tournament) {
        val sharedPref:SharedPreferencesRepository
        itemView.apply {

            nameTV.text = item.name
            goalTV.text = viewModel.getGoalText(item)
            descriptionTV.text = item.description
            durationTV.text = viewModel.getTournamentDurationText(item)
            //membersCountTV.text = viewModel.getTeamsCountText(item)
            teamCountTV.text = viewModel.getTeamsCountText(item).toString()
            //team2CountTV.text = viewModel.getTeamsCountText(item).toString()

            //if (item.active && !viewModel.hasTeamJoinedTournament(item)) {


            if (item.active) {
                buttonJoin.enable()
                buttonJoin.setOnClickListener {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(viewModel.getJoinTournamentDialogTitleText(item))
                        .setMessage(viewModel.getJoinTournamentMessageText())
                        .setPositiveButton("Yes") { _: DialogInterface, _: Int ->
                           // val action = TournamentsFragmentDirections.actionTournamentsFragmentToEnrollTeamsFragment(item.name)
                            viewModel.getExistingTeams(item.name)
                            viewModel.enrollTeams(item.name)
                            //buttonJoin.setEnabled(false)

                            //findNavController().navigateUp()
                            /*viewModel.joinTournament(item) {
                                viewModel.startUnityActivityForTournament(item) {
                                    context.startActivity(Intent(context, TreeCareUnityActivity::class.java))
                                }
                            }*/
                        }
                        .setNegativeButton("No") { dialogInterface: DialogInterface, _: Int ->
                            dialogInterface.dismiss()
                        }
                        .show()
                }
            } else {
                buttonJoin.disable()
            }
            buttonActiveLeaderBoard.setOnClickListener {
                viewModel.display()
            }

        }
    }
}