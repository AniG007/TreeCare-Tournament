package dal.mitacsgri.treecare.screens.progressreport

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import dal.mitacsgri.treecare.extensions.i
import dal.mitacsgri.treecare.repository.StepCountRepository
import dal.mitacsgri.treecare.screens.progressreport.ProgressReportDataFragment.Companion.MONTH_DATA
import dal.mitacsgri.treecare.screens.progressreport.ProgressReportDataFragment.Companion.WEEK_DATA
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants.*
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import java.text.NumberFormat
import java.util.*

class ProgressReportDataViewModel(
    private val stepCountRepository: StepCountRepository
): ViewModel() {

    private var aggregateStepCount = 0

    fun getProgressReportDurationText(progressReportType: Long): String {
        return when(progressReportType) {
            WEEK_DATA -> {
                val weekStartDate = DateTime(getStartOfWeek())
                val weekEndDate = weekStartDate.plusDays(6)
                val formatter = DateTimeFormat.forPattern("MMMM, d")

                formatter.print(weekStartDate) + " - " + formatter.print(weekEndDate)
            }

            MONTH_DATA -> {
                val formatter = DateTimeFormat.forPattern("MMMM")
                formatter.print(DateTime())
            }

            else -> ""
        }
    }

    fun getStepsDataForWeek(): MutableLiveData<BarData> {

        val barLiveData =  MutableLiveData<BarData>()

        val entries = arrayListOf<BarEntry>()
        for (i in 0..6) {
            entries.add(BarEntry(i.toFloat(), 0f))
        }

        stepCountRepository.getStepCountDataOverARange(
            getStartOfWeek(), DateTime().millis) {

            it.forEach { (date, steps) ->
                val dayOfWeek = DateTime(date).dayOfWeek
                entries[dayOfWeek-1] = BarEntry(dayOfWeek-1.toFloat(), steps.toFloat())

                aggregateStepCount += steps
            }
            val set = BarDataSet(entries, "Weekly Step Count")
            barLiveData.value = createBarData(set, WEEK_DATA)
        }

        return barLiveData
    }

    fun getStepsDataForMonth(): MutableLiveData<BarData> {
        val barLiveData = MutableLiveData<BarData>()
        val entries = arrayListOf<BarEntry>()
        addBarEntriesBasedOnDaysInMonth(entries)

        stepCountRepository.getStepCountDataOverARange(
            getStartOfMonth(), DateTime.now().minusDays(3).millis
        ) {map ->
            val keys = map.keys.sorted()

            var i = 0
            keys.forEach {
                entries.add(BarEntry((++i).toFloat(), map[it]?.toFloat() ?: 0f))
                aggregateStepCount += map[it] ?: 0
            }

            val set = BarDataSet(entries, "Daily Step Count")
            barLiveData.value = createBarData(set, MONTH_DATA)
        }

        return barLiveData
    }

    //Must be called from inside the observer for BarData live data
    fun getAggregateStepCount(): String =
        NumberFormat.getNumberInstance(Locale.getDefault()).format(aggregateStepCount)

    private fun createBarData(set: BarDataSet, progressReportType: Long): BarData {
        set.setGradientColor(0xff53c710.i, 0xFF6CFF13.i)
        val data = BarData(set)
        data.barWidth = 0.4f
        if (progressReportType == MONTH_DATA)
            data.setValueFormatter(XAxisDataLabelFormatter())
        return data
    }

    private fun getStartOfWeek(): Long {
        val now = LocalDate.now()
        val weekStartDate = now.withDayOfWeek(MONDAY)
        return weekStartDate.toDateTimeAtCurrentTime().withTimeAtStartOfDay().millis
    }

    private fun getStartOfMonth(): Long {
        val now = LocalDate.now().minusDays(2)
        val monthStartDate = now.withDayOfMonth(1)
        return monthStartDate.toDateTimeAtCurrentTime().withTimeAtStartOfDay().millis
    }

    private fun addBarEntriesBasedOnDaysInMonth(entries: ArrayList<BarEntry>) {
        when(LocalDate.now().monthOfYear) {
            FEBRUARY -> {
                if (DateTime().withYear(DateTime().year).year().isLeap) {
                    for (i in 0..29) {
                        entries.add(BarEntry(i.toFloat(), 0f))
                    }
                } else {
                    for (i in 0..28) {
                        entries.add(BarEntry(i.toFloat(), 0f))
                    }
                }
            }
            JANUARY, MARCH, MAY, JULY, AUGUST, OCTOBER, DECEMBER ->
                for (i in 0..31) {
                    entries.add(BarEntry(i.toFloat(), 0f))
                }
            else -> for (i in 0..30) {
                    entries.add(BarEntry(i.toFloat(), 0f))
                }
        }
    }
}