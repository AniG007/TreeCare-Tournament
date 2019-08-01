package dal.mitacsgri.treecare.screens.progressreport

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import dal.mitacsgri.treecare.screens.progressreport.ProgressReportDataFragment.Companion.MONTH_DATA
import dal.mitacsgri.treecare.screens.progressreport.ProgressReportDataFragment.Companion.WEEK_DATA

class ProgressReportPagerAdapter(fm: FragmentManager)
    : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private var mCurrentFragment = ProgressReportDataFragment()

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

    override fun setPrimaryItem(container: ViewGroup, position: Int, any: Any) {
        if (mCurrentFragment != any) {
            mCurrentFragment = any as ProgressReportDataFragment
            mCurrentFragment.reanimateBarChart(1000)
        }
        super.setPrimaryItem(container, position, any)
    }
}