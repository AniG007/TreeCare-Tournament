package dal.mitacsgri.treecare.screens.challenges

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import dal.mitacsgri.treecare.screens.challenges.activechallenges.ActiveChallengesFragment
import dal.mitacsgri.treecare.screens.challenges.currentchallenges.CurrentChallengesFragment

/**
 * Created by Devansh on 25-06-2019
 */

class ChallengesPagerAdapter(private val fm: FragmentManager)
    : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int) =
        when(position) {
            0 -> CurrentChallengesFragment()
            1 -> {
                ActiveChallengesFragment()
            }
            else -> ActiveChallengesFragment()
        }

    override fun getCount() = 2

    override fun getPageTitle(position: Int) =
            when(position) {
                0 -> "My challenges"
                1 -> "New challenges"
                else -> "Challenge"
            }
}