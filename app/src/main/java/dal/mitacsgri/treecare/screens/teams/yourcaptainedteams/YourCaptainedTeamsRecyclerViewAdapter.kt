package dal.mitacsgri.treecare.screens.teams.yourcaptainedteams

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.model.Team

class YourCaptainedTeamsRecyclerViewAdapter(
    private val teams: List<Team>
): RecyclerView.Adapter<YourCaptainedTeamViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        YourCaptainedTeamViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_your_captained_team, parent, false))

    override fun getItemCount() = teams.size

    override fun onBindViewHolder(holder: YourCaptainedTeamViewHolder, position: Int) {
        holder.bind(teams[position])
    }
}