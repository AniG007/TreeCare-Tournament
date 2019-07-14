package dal.mitacsgri.treecare.screens.tournaments.activetournaments

import android.annotation.SuppressLint
import android.view.View
import dal.mitacsgri.treecare.model.Tournament
import dal.mitacsgri.treecare.screens.BaseViewHolder
import kotlinx.android.synthetic.main.item_active_tournament.view.*

class ActiveTournamentsViewHolder(
    private val viewModel: ActiveTournamentsViewModel,
    itemView: View): BaseViewHolder<Tournament>(itemView) {

    @SuppressLint("SetTextI18n")
    override fun bind(item: Tournament) {
        itemView.apply {

            nameTV.text = item.name
            goalTV.text = viewModel.getGoalText(item)
            durationTV.text = viewModel.getTournamentDurationText(item)
            teamsCountTV.text = viewModel.getTeamsCountText(item)
        }
    }
}