package dal.mitacsgri.treecare.screens.tournaments.currenttournaments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.model.Tournament
import dal.mitacsgri.treecare.screens.tournaments.TournamentsViewModel
import dal.mitacsgri.treecare.screens.tournaments.currenttournaments.CurrentTournamentsViewHolder

class CurrentTournamentsRecyclerViewAdapter (
    private val tournamentsList: List<Tournament>,
    private val viewModel: TournamentsViewModel
) : RecyclerView.Adapter<CurrentTournamentsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrentTournamentsViewHolder =
        CurrentTournamentsViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_current_tournament, parent, false),
            viewModel)

    override fun getItemCount() = tournamentsList.size

    override fun onBindViewHolder(holder: CurrentTournamentsViewHolder, position: Int) {
        holder.bind(tournamentsList[position])
    }
}