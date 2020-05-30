package dal.mitacsgri.treecare.screens.tournaments.currenttournaments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.extensions.toast
import dal.mitacsgri.treecare.model.Tournament
import dal.mitacsgri.treecare.screens.tournaments.TournamentsViewModel
import kotlinx.android.synthetic.main.fragment_current_tournaments.view.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class CurrentTournamentsFragment : Fragment() {

    private val mViewModel: TournamentsViewModel by sharedViewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        val view = inflater.createFragmentViewWithStyle(
//            activity, R.layout.fragment_current_tournaments, R.style.tournament_mode_theme)
        val view = inflater.inflate(R.layout.fragment_current_tournaments, container, false)

        //This clear needs to be done here otherwise whenever this fragment is created as a result of coming
        //back from a fragment up in the navigation stack, the elements are added to the list, as a result of which,
        //the elements are duplicated
        mViewModel.currentTournamentsList.value?.clear()

        view.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = CurrentTournamentsRecyclerViewAdapter(mViewModel.currentTournamentsList.value!!, mViewModel)
        }

        mViewModel.currentTournamentsList.observe(viewLifecycleOwner, Observer {
            view.recyclerView.adapter?.notifyDataSetChanged()
        })

        mViewModel.getCurrentTournamentsForUser()

        return view
    }

    private fun setUpRecyclerView(recyclerView: RecyclerView, tournamentsList: List<Tournament>) {
        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = CurrentTournamentsRecyclerViewAdapter(tournamentsList, mViewModel)
        }
    }
}
