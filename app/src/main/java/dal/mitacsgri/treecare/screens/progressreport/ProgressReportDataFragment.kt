package dal.mitacsgri.treecare.screens.progressreport


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dal.mitacsgri.treecare.R
import org.intellij.lang.annotations.MagicConstant

class ProgressReportDataFragment : Fragment() {

    companion object {
        const val DATA_TYPE = "data_type"
        const val WEEK_DATA = 0L
        const val MONTH_DATA = 1L

        fun newInstance(@MagicConstant(intValues = [WEEK_DATA, MONTH_DATA]) dataType: Long): ProgressReportDataFragment {

            val fragment = ProgressReportDataFragment()
            val args = Bundle()
            args.putLong(DATA_TYPE, dataType)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val dataType = arguments?.getLong(DATA_TYPE)!!

        val view = inflater.inflate(R.layout.fragment_progress_report_data, container, false)
        view.apply {

        }

        return view
    }


}
