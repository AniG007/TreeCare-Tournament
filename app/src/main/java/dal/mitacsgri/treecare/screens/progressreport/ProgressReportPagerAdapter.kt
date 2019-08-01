package dal.mitacsgri.treecare.screens.progressreport

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import dal.mitacsgri.treecare.screens.progressreport.ProgressReportDataFragment.Companion.MONTH_DATA
import dal.mitacsgri.treecare.screens.progressreport.ProgressReportDataFragment.Companion.WEEK_DATA

class ProgressReportPagerAdapter(fm: FragmentManager)
    : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment =
        when(position) {
            0 -> ProgressReportDataFragment.newInstance(WEEK_DATA)
            1 -> ProgressReportDataFragment.newInstance(MONTH_DATA)
            else -> ProgressReportDataFragment()
        }

    override fun getCount() = 2

    override fun getPageTitle(position: Int) =
            when(position) {
                0 -> "Week"
                1 -> "Month"
                else -> ""
            }
}