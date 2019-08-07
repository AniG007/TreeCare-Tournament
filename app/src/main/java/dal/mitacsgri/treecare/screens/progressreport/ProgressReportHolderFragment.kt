package dal.mitacsgri.treecare.screens.progressreport


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import dal.mitacsgri.treecare.R
import kotlinx.android.synthetic.main.fragment_progress_report_holder.view.*

class ProgressReportHolderFragment : Fragment() {

    companion object {
        const val REPORT_TYPE = "report_type"
        const val WEEK_DATA = 0L
        const val MONTH_DATA = 1L

        fun newInstance(dataType: Long): ProgressReportHolderFragment {
            val fragment = ProgressReportHolderFragment()
            val args = Bundle()
            args.putLong(REPORT_TYPE, dataType)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_progress_report_holder, container, false)

        view.apply {

            val adapter = ProgressReportHolderPagerAdapter(childFragmentManager, arguments?.getLong(REPORT_TYPE)!!)
            viewPager.adapter = adapter
            viewPager.currentItem = adapter.count - 1

            previousDataButton.setOnClickListener {
                viewPager.currentItem = viewPager.currentItem - 1
            }

            nextDataButton.setOnClickListener {
                viewPager.currentItem = viewPager.currentItem + 1
            }

            viewPager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {

                }

                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                }

                override fun onPageSelected(position: Int) {
                    when (position) {
                        0 -> previousDataButton.visibility = View.GONE
                        adapter.count - 1 -> nextDataButton.visibility = View.GONE
                        else -> {
                            previousDataButton.visibility = View.VISIBLE
                            nextDataButton.visibility = View.VISIBLE
                        }
                    }
                }
            })
        }

        return view
    }


}
