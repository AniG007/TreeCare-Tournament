package dal.mitacsgri.treecare.screens.progressreport

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

class ProgressReportPagerAdapter(fm: FragmentManager):
    FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment = ProgressReportHolderFragment.newInstance(
        when(position) {
            0 -> ProgressReportHolderFragment.WEEK_DATA
            1 -> ProgressReportHolderFragment.MONTH_DATA
            else -> 0
        }
    )

    override fun getCount(): Int = 2

    override fun getPageTitle(position: Int) =
        when(position) {
            0 -> "Week"
            1 -> "Month"
            else -> ""
        }

}