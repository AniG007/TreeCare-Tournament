package dal.mitacsgri.treecare.screens.tournaments.activetournaments

import android.annotation.SuppressLint
import android.view.View
import dal.mitacsgri.treecare.model.Tournament
import dal.mitacsgri.treecare.screens.BaseViewHolder
import kotlinx.android.synthetic.main.item_active_tournament.view.*
import kotlinx.android.synthetic.main.item_active_tournament.view.durationTV
import kotlinx.android.synthetic.main.item_active_tournament.view.goalTV
import kotlinx.android.synthetic.main.item_active_tournament.view.nameTV
//import kotlinx.android.synthetic.main.item_current_tournament.view.*

class ActiveTournamentsViewHolder(
    private val viewModel: ActiveTournamentsViewModel,
    itemView: View): BaseViewHolder<Tournament>(itemView) {

    @SuppressLint("SetTextI18n")
    override fun bind(item: Tournament) {
        itemView.apply {

            nameTV.text = item.name
            goalTV.text = viewModel.getGoalText(item)
            durationTV.text = viewModel.getTournamentDurationText(item)
            //membersCountTV.text = viewModel.getTeamsCountText(item)
            team1CountTV.text = viewModel.getTeam1CountText(item).toString()
            team2CountTV.text = viewModel.getTeam2CountText(item).toString()
        }
    }
}