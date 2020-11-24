package dal.mitacsgri.treecare.screens.tournamentleaderboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.model.TeamTournament

class TournamentLeaderBoardRecyclerViewAdapter(
    private val teamList: List<TeamTournament>,
    private val viewModel: TournamentLeaderBoardViewModel,
    private val viewLifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<TournamentLeaderBoardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TournamentLeaderBoardViewHolder=
        TournamentLeaderBoardViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_tournament_leaderboard, parent, false),
        viewModel,viewLifecycleOwner)

    override fun getItemCount() = teamList.size

    override fun onBindViewHolder(holder: TournamentLeaderBoardViewHolder, position: Int) {
        holder.bind(teamList[position], position)
    }

}