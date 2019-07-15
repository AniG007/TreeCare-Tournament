package dal.mitacsgri.treecare.screens.teams.yourteams


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.extensions.createFragmentViewWithStyle
import kotlinx.android.synthetic.main.fragment_your_teams.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class YourTeamsFragment : Fragment() {

    private val mViewModel: YourTeamsViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.createFragmentViewWithStyle(activity,
            R.layout.fragment_your_teams, R.style.tournament_mode_theme)

        view.apply {
            mViewModel.getAllMyTeams().observe(this@YourTeamsFragment, Observer {
                recyclerView.adapter = YourTeamsRecyclerViewAdapter(it)
            })
        }

        return view
    }


}
