package dal.mitacsgri.treecare.screens.progressreport

import com.github.mikephil.charting.formatter.ValueFormatter

class XAxisWeekLabelValueFormatter: ValueFormatter() {

    override fun getFormattedValue(value: Float): String {
        return when(value.toInt()) {
            0 -> "MON"
            1 -> "TUE"
            2 -> "WED"
            3 -> "THU"
            4 -> "FRI"
            5 -> "SAT"
            6 -> "SUN"
            else -> ""
        }
    }
}