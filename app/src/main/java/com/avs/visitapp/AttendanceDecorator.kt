package com.avs.visitapp

import android.content.Context
import android.graphics.drawable.Drawable
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade

class AttendanceDecorator(
    context: Context,
    private val dates: Collection<CalendarDay>,
    private val drawableRes: Int
) : DayViewDecorator {

    private val drawable: Drawable? = context.getDrawable(drawableRes)

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return dates.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        drawable?.let {
            view.setBackgroundDrawable(it)
        }
    }

}
