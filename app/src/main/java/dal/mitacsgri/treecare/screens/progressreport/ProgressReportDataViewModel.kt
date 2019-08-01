package dal.mitacsgri.treecare.screens.progressreport

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import dal.mitacsgri.treecare.extensions.i
import dal.mitacsgri.treecare.repository.StepCountRepository
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants.*
import org.joda.time.LocalDate

class ProgressReportDataViewModel(
    private val stepCountRepository: StepCountRepository
): ViewModel() {

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
            }
            val set = BarDataSet(entries, "Weekly Step Count")
            barLiveData.value = createBarData(set)
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
            }

            val set = BarDataSet(entries, "Daily Step Count")
            barLiveData.value = createBarData(set)
        }

        return barLiveData
    }

    private fun createBarData(set: BarDataSet): BarData {
        set.setGradientColor(0xff53c710.i, 0xFF6CFF13.i)
        val data = BarData(set)
        data.barWidth = 0.4f
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