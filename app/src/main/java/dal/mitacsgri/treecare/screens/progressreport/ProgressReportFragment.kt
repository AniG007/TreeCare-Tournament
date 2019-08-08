package dal.mitacsgri.treecare.screens.progressreport

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import dal.mitacsgri.treecare.R
import kotlinx.android.synthetic.main.fragment_progress_report.view.*

class ProgressReportFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_progress_report, container, false)

        view.apply {
            viewPager.adapter = ProgressReportPagerAdapter(childFragmentManager)
            tabLayout.setupWithViewPager(viewPager)
            backButton.setOnClickListener {
                try {
                    findNavController().navigateUp()
                } catch (e: IllegalStateException) {
                    activity?.onBackPressed()
                }
            }
        }

        return view
    }


}
