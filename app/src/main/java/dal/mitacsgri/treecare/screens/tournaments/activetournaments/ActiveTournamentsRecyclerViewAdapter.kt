package dal.mitacsgri.treecare.screens.tournaments.activetournaments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.model.Tournament

class ActiveTournamentsRecyclerViewAdapter(
    private val tournaments: List<Tournament>,
    private val viewModel: ActiveTournamentsViewModel
): RecyclerView.Adapter<ActiveTournamentsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ActiveTournamentsViewHolder(viewModel,
            LayoutInflater.from(parent.context).inflate(R.layout.item_active_tournament, parent, false))
            //LayoutInflater.from(parent.context).inflate(R.layout.item_current_tournament, parent, false))

    override fun getItemCount() = tournaments.size

    override fun onBindViewHolder(holder: ActiveTournamentsViewHolder, position: Int) {
        holder.bind(tournaments[position])
    }
}