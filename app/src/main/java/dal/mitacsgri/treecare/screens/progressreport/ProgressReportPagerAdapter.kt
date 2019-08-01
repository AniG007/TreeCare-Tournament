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
    private var position1ShownOnce = false
    private var position0ShownOnce = false

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
            when(position) {
                0 -> if (!position0ShownOnce) {
                    position0ShownOnce = true
                    reanimateFragmentChart()
                }

                1 -> if (!position1ShownOnce) {
                    position1ShownOnce = true
                    reanimateFragmentChart()
                }
            }
        }
        super.setPrimaryItem(container, position, any)
    }

    private fun reanimateFragmentChart() {
        mCurrentFragment.reanimateBarChart(1000)
    }
}