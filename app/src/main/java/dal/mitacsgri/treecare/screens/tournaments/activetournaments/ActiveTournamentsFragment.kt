package dal.mitacsgri.treecare.screens.tournaments.activetournaments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.extensions.createFragmentViewWithStyle
import kotlinx.android.synthetic.main.fragment_active_tournaments.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class ActiveTournamentsFragment : Fragment() {

    private val mViewModel: ActiveTournamentsViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.createFragmentViewWithStyle(
            activity, R.layout.fragment_active_challenges, R.style.tournament_mode_theme)

        view.apply {
            recyclerView.apply {
                layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                adapter = ActiveTournamentsRecyclerViewAdapter(mViewModel.tournaments.value!!, mViewModel)
            }

            mViewModel.tournaments.observe(this@ActiveTournamentsFragment, Observer {
                recyclerView.adapter = ActiveTournamentsRecyclerViewAdapter(it, mViewModel)
            })
        }

        mViewModel.getActiveTournaments()

        return view
    }


}
