package dal.mitacsgri.treecare.screens.teams.allteams


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.extensions.createFragmentViewWithStyle
import kotlinx.android.synthetic.main.fragment_all_teams.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class AllTeamsFragment : Fragment() {

    private val mViewModel: AllTeamsViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.createFragmentViewWithStyle(activity,
            R.layout.fragment_all_teams, R.style.tournament_mode_theme)

        mViewModel.getAllTeams().observe(this, Observer {
            view.recyclerView.adapter = AllTeamsRecyclerViewAdapter(it, mViewModel)
        })

        return view
    }


}
