package dal.mitacsgri.treecare.screens.tournaments

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import dal.mitacsgri.treecare.screens.tournaments.activetournaments.ActiveTournamentsFragment
import dal.mitacsgri.treecare.screens.tournaments.currenttournaments.CurrentTournamentsFragment

class TournamentsPagerAdapter(fm: FragmentManager)
    : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int) =
        when(position) {
            0 -> CurrentTournamentsFragment()
            1 -> ActiveTournamentsFragment()
            else -> ActiveTournamentsFragment()
        }

    override fun getCount() = 2

    override fun getPageTitle(position: Int) =
            when(position) {
                0 -> "Current Tournaments"
                1 -> "Active Tournaments"
                else -> ""
            }
}