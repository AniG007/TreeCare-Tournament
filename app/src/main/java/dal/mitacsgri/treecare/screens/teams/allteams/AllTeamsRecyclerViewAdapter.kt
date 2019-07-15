package dal.mitacsgri.treecare.screens.teams.allteams

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.model.Team

class AllTeamsRecyclerViewAdapter(
    private val teams: List<Team>,
    private val viewModel: AllTeamsViewModel
): RecyclerView.Adapter<AllTeamsItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        AllTeamsItemViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_all_teams_team, parent, false), viewModel)

    override fun getItemCount() = teams.size

    override fun onBindViewHolder(holder: AllTeamsItemViewHolder, position: Int) {
        holder.bind(teams[position])
    }
}