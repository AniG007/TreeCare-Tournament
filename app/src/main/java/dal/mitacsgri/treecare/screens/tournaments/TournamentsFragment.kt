package dal.mitacsgri.treecare.screens.tournaments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import dal.mitacsgri.treecare.R
import dal.mitacsgri.treecare.extensions.createFragmentViewWithStyle
import kotlinx.android.synthetic.main.fragment_tournaments.view.*

class TournamentsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.createFragmentViewWithStyle(
            activity, R.layout.fragment_tournaments, R.style.tournament_mode_theme)

        view.apply {
            viewPager.adapter = TournamentsPagerAdapter(childFragmentManager)

            tabLayout.setupWithViewPager(viewPager)
            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }

        return view
    }


}
