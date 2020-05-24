package dal.mitacsgri.treecare.screens.enrollteams

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.model.Team

class EnrollTeamsRecyclerViewAdapter(
    private val membersList : ArrayList<Team>,
    private val viewModel: EnrollTeamsViewModel
): RecyclerView.Adapter<EnrollTeamsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EnrollTeamsViewHolder =
        EnrollTeamsViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_enroll_teams, parent, false),
            viewModel
        )

    override fun getItemCount() = membersList.size

    override fun onBindViewHolder(holder: EnrollTeamsViewHolder, position: Int) {
        holder.bind(membersList[position])
    }
}