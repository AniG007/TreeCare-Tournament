package dal.mitacsgri.treecare.screens.tournaments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import dal.mitacsgri.treecare.R
import kotlinx.android.synthetic.main.fragment_tournaments.view.*

class TournamentsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tournaments, container, false)

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
