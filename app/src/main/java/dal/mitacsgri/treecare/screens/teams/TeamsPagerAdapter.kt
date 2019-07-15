package dal.mitacsgri.treecare.screens.teams

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import dal.mitacsgri.treecare.screens.teams.yourcaptainedteams.YourCaptainedTeamsFragment
import dal.mitacsgri.treecare.screens.teams.yourteams.YourTeamsFragment

class TeamsPagerAdapter(fm: FragmentManager)
    : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int) =
        when(position) {
            0 -> YourTeamsFragment()
            1 -> YourCaptainedTeamsFragment()
            else -> YourTeamsFragment()
        }

    override fun getCount() = 2

    override fun getPageTitle(position: Int) =
            when(position) {
                0 -> "Your teams"
                1 -> "Your captained teams"
                else -> ""
            }
}