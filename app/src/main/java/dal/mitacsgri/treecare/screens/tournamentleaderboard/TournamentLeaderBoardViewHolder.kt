package dal.mitacsgri.treecare.screens.tournamentleaderboard

import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.model.TeamTournament
import dal.mitacsgri.treecare.screens.BaseViewHolder
import kotlinx.android.synthetic.main.item_tournament_leaderboard.view.*

class TournamentLeaderBoardViewHolder(
    itemView: View,
    private val viewModel: TournamentLeaderBoardViewModel
    ): BaseViewHolder<TeamTournament>(itemView){

    private var teamPosition = 0

    override fun bind(item: TeamTournament){
        itemView.apply {
            name.text = item.teamName
            rank.text = teamPosition.toString()
            stepsCount.text = viewModel.getTotalStepsText(item)
            leafCount.text = viewModel.getLeafCountText(item)

            if (viewModel.isCurrentTeam(item)) {
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.colorPrimaryLight))
            }

            cardView.setOnClickListener {
                val action = TournamentLeaderBoardFragmentDirections.actionTournamentLeaderBoardFragmentToTeamRankingFragment(item.teamName, item.name)
                findNavController().navigate(action)
            }
        }
    }

    fun bind(item: TeamTournament, position: Int){
        teamPosition = position + 1
        bind(item)
    }

}