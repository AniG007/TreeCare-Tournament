import org.joda.time.DateTime
import org.joda.time.DateTimeConstants
import org.joda.time.Days
import org.joda.time.LocalDate

/**
 * Created by Devansh on 03-08-2019
 */

fun calculateLeafCountFromStepCount(stepCount: Int, dailyGoal: Int): Int {
    var leafCount = stepCount / 1000
    if (stepCount < dailyGoal) {
        leafCount -= Math.ceil((dailyGoal - stepCount) / 1000.0).toInt()
        if (leafCount < 0) leafCount = 0
    }
    return leafCount
}

fun expandDailyGoalMapIfNeeded(dailyGoalMap: MutableMap<String, Int>)
        : Map<String, Int> {

    var keysList = mutableListOf<Long>()
    dailyGoalMap.keys.forEach {
        keysList.add(it.toLong())
    }
    keysList = keysList.sorted().toMutableList()

    val lastTime = keysList[keysList.size-1]
    val days = Days.daysBetween(DateTime(lastTime), DateTime()).days

    val oldGoal = dailyGoalMap[lastTime.toString()]

    for (i in 1..days) {
        val key = DateTime(lastTime).plusDays(i).withTimeAtStartOfDay().millis.toString()
        dailyGoalMap[key] = oldGoal!!
    }
    return dailyGoalMap
}

fun getStartOfWeek(dateMillis: Long): Long {
    val startDate = LocalDate(dateMillis)
    val weekStartDate = startDate.withDayOfWeek(DateTimeConstants.MONDAY)
    return weekStartDate.toDateTimeAtCurrentTime().withTimeAtStartOfDay().millis
}

fun getStartOfMonth(dateMillis: Long): Long {
    val startDate = LocalDate(dateMillis)
    val monthStartDate = startDate.withDayOfMonth(1)
    return monthStartDate.toDateTimeAtCurrentTime().withTimeAtStartOfDay().millis
}