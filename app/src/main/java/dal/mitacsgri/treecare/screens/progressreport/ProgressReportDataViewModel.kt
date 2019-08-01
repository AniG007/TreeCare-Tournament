package dal.mitacsgri.treecare.screens.progressreport

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import dal.mitacsgri.treecare.repository.StepCountRepository
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants
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
            val data = BarData(set)
            data.barWidth = 0.9f
            barLiveData.value = data
        }

        return barLiveData
    }

    private fun getStartOfWeek(): Long {
        val now = LocalDate.now()
        val weekStartDate = now.withDayOfWeek(DateTimeConstants.MONDAY)
        return weekStartDate.toDateTimeAtCurrentTime().withTimeAtStartOfDay().millis
    }

}