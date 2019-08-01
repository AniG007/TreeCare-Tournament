package dal.mitacsgri.treecare.screens.progressreport

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import dal.mitacsgri.treecare.R
import kotlinx.android.synthetic.main.fragment_progress_report_data.*
import kotlinx.android.synthetic.main.fragment_progress_report_data.view.*
import org.intellij.lang.annotations.MagicConstant
import org.koin.androidx.viewmodel.ext.android.viewModel

class ProgressReportDataFragment : Fragment() {

    private val mViewModel: ProgressReportDataViewModel by viewModel()
    private var reportType: Long = 1

    companion object {
        const val REPORT_TYPE = "report_type"
        const val WEEK_DATA = 0L
        const val MONTH_DATA = 1L

        fun newInstance(@MagicConstant(intValues = [WEEK_DATA, MONTH_DATA]) dataType: Long): ProgressReportDataFragment {

            val fragment = ProgressReportDataFragment()
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
        reportType = arguments?.getLong(REPORT_TYPE)!!

        val view = inflater.inflate(R.layout.fragment_progress_report_data, container, false)
        view.apply {
            val barChartLiveData =
                when(reportType) {
                    WEEK_DATA -> mViewModel.getStepsDataForWeek()
                    MONTH_DATA -> mViewModel.getStepsDataForMonth()
                    else -> MutableLiveData()
                }
            barChartLiveData.observe(this@ProgressReportDataFragment, Observer {
                updateBarChart(barChart, it)
                totalStepCountTV.text = mViewModel.getAggregateStepCount()
            })

            progressReportDurationTV.text = mViewModel.getProgressReportDurationText(reportType)
        }
        return view
    }

    fun reanimateBarChart(durationMillis: Int) {
        if (view != null) {
            barChart.animateY(durationMillis)
        }
    }

    private fun updateBarChart(barChart: BarChart, barData: BarData) {
        barChart.apply {
            data = barData
            setFitBars(true)

            val newDescription = Description()
            newDescription.text = ""
            description = newDescription

            setTouchEnabled(false)

            xAxis.apply {
                if (reportType == WEEK_DATA) valueFormatter = XAxisWeekDataFormatter()
                position = XAxis.XAxisPosition.BOTTOM_INSIDE
                setDrawAxisLine(false)
                setDrawGridLines(false)
            }
            axisRight.isEnabled = false
            legend.isEnabled = false

            axisLeft.setDrawAxisLine(false)

            animateY(1000)
        }
    }


}
