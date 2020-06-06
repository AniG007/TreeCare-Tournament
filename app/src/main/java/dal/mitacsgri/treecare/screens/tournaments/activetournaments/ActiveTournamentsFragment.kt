package dal.mitacsgri.treecare.screens.tournaments.activetournaments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.extensions.createFragmentViewWithStyle
import dal.mitacsgri.treecare.extensions.toast
import dal.mitacsgri.treecare.screens.tournaments.TournamentsViewModel
import kotlinx.android.synthetic.main.fragment_active_tournaments.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class ActiveTournamentsFragment : Fragment() {

    private val mViewModel: TournamentsViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.createFragmentViewWithStyle(
            activity, R.layout.fragment_active_challenges, R.style.tournament_mode_theme)

        view.apply {
            recyclerView.apply {
                layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                adapter = ActiveTournamentsRecyclerViewAdapter(mViewModel.activeTournamentsList.value!!, mViewModel)
            }

            mViewModel.activeTournamentsList.observe(viewLifecycleOwner, Observer {
                recyclerView.adapter = ActiveTournamentsRecyclerViewAdapter(it, mViewModel)
            })

            if(!mViewModel.messageDisplayed2) {
                mViewModel.MessageStatus.observe(viewLifecycleOwner, Observer {
                    it.toast(context)
                    mViewModel.messageDisplayed2 = true
                    //findNavController().navigateUp()
                })
            }
        }

        mViewModel.getAllActiveTournaments()

        return view
    }


}
