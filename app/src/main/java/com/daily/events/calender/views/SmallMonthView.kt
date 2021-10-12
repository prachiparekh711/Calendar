package com.daily.events.calender.views

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.daily.events.calender.Extensions.config
import com.daily.events.calender.Model.DayYearly
import com.daily.events.calender.R
import com.daily.events.calender.helpers.isWeekend
import com.simplemobiletools.commons.extensions.adjustAlpha
import com.simplemobiletools.commons.helpers.MEDIUM_ALPHA
import java.util.*


// used for displaying months at Yearly view
class SmallMonthView(context: Context, attrs: AttributeSet, defStyle: Int) :
    View(context, attrs, defStyle) {
    private var paint: Paint
    private var todayCirclePaint: Paint
    private var dayWidth = 0f
    private var textColor = 0
    private var redTextColor = 0
    private var whiteTextColor = 0
    private var days = 31
    private var isLandscape = false
    private var highlightWeekends = false
    private var isSundayFirst = false
    private var isPrintVersion = false
    private var mEvents: ArrayList<DayYearly>? = null

    var firstDay = 0
    var todaysId = 0

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    fun setDays(days: Int) {
        this.days = days
        invalidate()
    }

    fun setEvents(events: ArrayList<DayYearly>?) {
        mEvents = events
        post { invalidate() }
    }

    init {
        val attributes = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SmallMonthView,
            0, 0
        )

        try {
            days = attributes.getInt(R.styleable.SmallMonthView_days, 31)
        } finally {
            attributes.recycle()
        }

        textColor = context.resources.getColor(R.color.black)
        redTextColor = context.resources.getColor(R.color.red)
        whiteTextColor = context.resources.getColor(R.color.white)
        highlightWeekends = true
        isSundayFirst = false

        paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = resources.getDimensionPixelSize(R.dimen.year_view_day_text_size).toFloat()
            textAlign = Paint.Align.RIGHT
        }

        todayCirclePaint = Paint(paint)
        paint.textSize = 25f
        todayCirclePaint.color = context.resources.getColor(R.color.white)
        todayCirclePaint.color = context.resources.getColor(R.color.theme_color)
        isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dayWidth == 0f) {
            dayWidth = if (isLandscape) {
                width / 9f
            } else {
                width / 7f
            }
        }

        var curId = 1 - firstDay
        for (y in 1..6) {
            for (x in 1..7) {
                if (curId in 1..days) {

                    if (curId == todaysId && !isPrintVersion) {
//                        val bgLeft = x * dayWidth - dayWidth
//                        val bgTop = y * dayWidth - dayWidth
//                        canvas.drawRoundRect(
//                            RectF(
//                                bgLeft, bgTop + 15f, bgLeft + 65f, bgTop + 90f
//                            ), 5F, 5F, todayCirclePaint
//                        )
                        val dividerConstant = if (isLandscape) 6 else 4
                        canvas.drawCircle(
                            x * dayWidth - dayWidth / 2,
                            y * dayWidth - dayWidth / 6,
                            dayWidth * 0.41f,
                            todayCirclePaint
                        )
                    }

                    val paint = getPaint(curId, x, highlightWeekends)

                    canvas.drawText(
                        curId.toString(),
                        x * dayWidth - (dayWidth / 4),
                        y * dayWidth,
                        paint
                    )
                    paint.textSize = 25f
                    val customTypeface = ResourcesCompat.getFont(context, R.font.roboto)
                    paint.typeface = customTypeface
                }
                curId++
            }
        }
    }

    private fun getPaint(curId: Int, weekDay: Int, highlightWeekends: Boolean): Paint {
//        Log.e("highlightWeekends", highlightWeekends.toString())
        val colors = mEvents?.get(curId)?.eventColors ?: HashSet()
        if (colors.isNotEmpty()) {
            if (curId == todaysId) {
                val curPaint = Paint(paint)
                curPaint.color = whiteTextColor
                return curPaint
            } else {
                val curPaint = Paint(paint)
                curPaint.color = colors.first()
                return curPaint
            }
        } else if (highlightWeekends && isWeekend(weekDay - 1, isSundayFirst)) {
            val curPaint = Paint(paint)
            curPaint.color = redTextColor
            return curPaint
        }
        if (curId == todaysId) {
            val curPaint = Paint(paint)
            curPaint.color = whiteTextColor
            return curPaint
        }
        return paint
    }

    fun togglePrintMode() {
        isPrintVersion = !isPrintVersion
        textColor = if (isPrintVersion) {
            resources.getColor(R.color.text_color)
        } else {
            context.config.textColor.adjustAlpha(MEDIUM_ALPHA)
        }

        paint.color = textColor
        invalidate()
    }
}
