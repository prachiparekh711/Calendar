package com.daily.events.calender.helpers

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.daily.events.calender.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.loper7.date_time_picker.DateTimeConfig
import com.loper7.date_time_picker.DateTimePicker
import kotlinx.android.synthetic.main.activity_event.*
import org.jetbrains.annotations.NotNull
import java.text.SimpleDateFormat
import java.util.*

class CardDatePickerDialog(context: Context) : BottomSheetDialog(context), View.OnClickListener {
    companion object {
        const val CARD = 0 //卡片
        const val CUBE = 1 //方形
        const val STACK = 2 //顶部圆角

        fun builder(context: Context): Builder {
            return lazy { Builder(context) }.value
        }
    }

    private var builder: Builder? = null

    private var tv_cancel: TextView? = null
    private var tv_submit: TextView? = null
    private var tv_title: TextView? = null
    private var tv_choose_date: TextView? = null
    private var btn_today: TextView? = null
    private var datePicker: DateTimePicker? = null
    private var tv_go_back: TextView? = null
    private var linear_now: LinearLayout? = null
    private var linear_bg: LinearLayout? = null
    private var mBehavior: BottomSheetBehavior<FrameLayout>? = null

    private var millisecond: Long = 0


    constructor(context: Context, builder: Builder) : this(context) {
        this.builder = builder
    }

    init {
        builder = builder(context)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.bottom_calender_layout)
        super.onCreate(savedInstanceState)

        val bottomSheet = delegate.findViewById<FrameLayout>(R.id.design_bottom_sheet)
        bottomSheet!!.setBackgroundColor(Color.TRANSPARENT)


        tv_cancel = findViewById(R.id.dialog_cancel)
        tv_submit = findViewById(R.id.dialog_submit)
        datePicker = findViewById(R.id.dateTimePicker)
        tv_title = findViewById(R.id.tv_title)
        btn_today = findViewById(R.id.btn_today)
        tv_choose_date = findViewById(R.id.tv_choose_date)
        tv_go_back = findViewById(R.id.tv_go_back)
        linear_now = findViewById(R.id.linear_now)
        linear_bg = findViewById(R.id.linear_bg)

        mBehavior = BottomSheetBehavior.from(bottomSheet)


        //背景模式
        if (builder!!.model != 0) {
            val parmas = LinearLayout.LayoutParams(linear_bg!!.layoutParams)
            when (builder!!.model) {
                CARD -> {
                    parmas.setMargins(dip2px(12f), dip2px(12f), dip2px(12f), dip2px(12f))
                    linear_bg!!.layoutParams = parmas
                    linear_bg!!.setBackgroundResource(R.drawable.shape_bg_round_white_5)
                }
                CUBE -> {
                    parmas.setMargins(0, 0, 0, 0)
                    linear_bg!!.layoutParams = parmas
                    linear_bg!!.setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.colorTextWhite
                        )
                    )
                }
                STACK -> {
                    parmas.setMargins(0, 0, 0, 0)
                    linear_bg!!.layoutParams = parmas
                    linear_bg!!.setBackgroundResource(R.drawable.shape_bg_top_round_white_15)
                }
                else -> {
                    parmas.setMargins(0, 0, 0, 0)
                    linear_bg!!.layoutParams = parmas
                    linear_bg!!.setBackgroundResource(builder!!.model)
                }
            }
        }

        //标题
        if (builder!!.titleValue.isNullOrEmpty()) {
            tv_title!!.visibility = View.GONE
        } else {
            tv_title?.text = builder!!.titleValue
            tv_title?.visibility = View.VISIBLE
        }

        //按钮
        tv_cancel?.text = builder!!.cancelText
        tv_submit?.text = builder!!.chooseText

        //设置自定义layout
        datePicker!!.setLayout(R.layout.layout_date_picker_globalization1)
        //显示标签
        datePicker!!.showLabel(builder!!.dateLabel)
        //设置标签文字
        datePicker!!.setLabelText(
            builder!!.yearLabel,
            builder!!.monthLabel,
            builder!!.dayLabel,
            builder!!.hourLabel,
            builder!!.minLabel,
            builder!!.secondLabel
        )

        //显示模式
        if (builder!!.displayTypes == null) {
            builder!!.displayTypes = intArrayOf(
                DateTimeConfig.YEAR,
                DateTimeConfig.MONTH,
                DateTimeConfig.DAY,
                DateTimeConfig.HOUR,
                DateTimeConfig.MIN,
                DateTimeConfig.SECOND
            )
        }

        datePicker!!.setDisplayType(builder!!.displayTypes)
        //回到当前时间展示
        if (builder!!.displayTypes != null) {
            var year_month_day_hour = 0
            for (i in builder!!.displayTypes!!) {
                if (i == DateTimeConfig.YEAR && year_month_day_hour <= 0) {
                    year_month_day_hour = 0
                    tv_go_back!!.text = "回到今年"
                    btn_today!!.text = "今"
                }
                if (i == DateTimeConfig.MONTH && year_month_day_hour <= 1) {
                    year_month_day_hour = 1
                    tv_go_back!!.text = "回到本月"
                    btn_today!!.text = "本"
                }
                if (i == DateTimeConfig.DAY && year_month_day_hour <= 2) {
                    year_month_day_hour = 2
                    tv_go_back!!.text = "回到今日"
                    btn_today!!.text = "今"
                }
                if ((i == DateTimeConfig.HOUR || i == DateTimeConfig.MIN) && year_month_day_hour <= 3) {
                    year_month_day_hour = 3
                    tv_go_back!!.text = "回到此刻"
                    btn_today!!.text = "此"
                }
            }

        }
        linear_now!!.visibility = if (builder!!.backNow) View.VISIBLE else View.GONE
        tv_choose_date!!.visibility = if (builder!!.focusDateInfo) View.VISIBLE else View.GONE

        //强制关闭国际化（不受系统语言影响）
        datePicker!!.setGlobal(DateTimeConfig.GLOBAL_LOCAL)
        //设置最小时间
        datePicker!!.setMinMillisecond(builder!!.minTime)
        //设置最大时间
        datePicker!!.setMaxMillisecond(builder!!.maxTime)
        //设置默认时间
        datePicker!!.setDefaultMillisecond(builder!!.defaultMillisecond)
        //设置是否循环滚动
        datePicker!!.setWrapSelectorWheel(
            builder!!.wrapSelectorWheelTypes,
            builder!!.wrapSelectorWheel
        )

        datePicker!!.setTextSize(14)
        if (builder!!.themeColor != 0) {
            datePicker!!.setThemeColor(builder!!.themeColor)
            tv_submit!!.setTextColor(builder!!.themeColor)

            val gd = GradientDrawable()
            gd.setColor(builder!!.themeColor)
            gd.cornerRadius = dip2px(60f).toFloat()
            btn_today!!.background = gd
        }

        tv_cancel!!.setOnClickListener(this)
        tv_submit!!.setOnClickListener(this)
        btn_today!!.setOnClickListener(this)

        datePicker!!.setOnDateTimeChangedListener { millisecond ->
            this@CardDatePickerDialog.millisecond = millisecond

            val date = Date(millisecond)
            val format = SimpleDateFormat("E, dd MMM hh:mm a")

            tv_choose_date!!.text =
                format.format(date)
        }
    }

    override fun onStart() {
        super.onStart()
        mBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onClick(v: View) {
        this.dismiss()
        when (v.id) {

            R.id.btn_today -> {
                builder?.onChooseListener?.invoke(Calendar.getInstance().timeInMillis)
            }
            R.id.dialog_submit -> {
                builder?.onChooseListener?.invoke(millisecond)
            }
            R.id.dialog_cancel -> {
                builder?.onCancelListener?.invoke()
            }
        }
        this.dismiss()
    }


    class Builder(private var context: Context) {
        @JvmField
        var backNow: Boolean = true

        @JvmField
        var focusDateInfo: Boolean = true

        @JvmField
        var dateLabel: Boolean = true

        @JvmField
        var cancelText: String = "取消"

        @JvmField
        var chooseText: String = "确定"

        @JvmField
        var titleValue: String? = null

        @JvmField
        var defaultMillisecond: Long = 0

        @JvmField
        var minTime: Long = 0

        @JvmField
        var maxTime: Long = 0

        @JvmField
        var displayTypes: IntArray? = null

        @JvmField
        var model: Int = CARD

        @JvmField
        var themeColor: Int = 0

        @JvmField
        var pickerLayoutResId: Int = 0

        @JvmField
        var wrapSelectorWheel: Boolean = true

        @JvmField
        var wrapSelectorWheelTypes: MutableList<Int>? = mutableListOf()

        @JvmField
        var onChooseListener: ((Long) -> Unit)? = null

        @JvmField
        var onCancelListener: (() -> Unit)? = null

        var yearLabel = "年"
        var monthLabel = "月"
        var dayLabel = "日"
        var hourLabel = "时"
        var minLabel = "分"
        var secondLabel = "秒"

        /**
         * 设置标题
         */
        fun setTitle(value: String): Builder {
            this.titleValue = value
            return this
        }

        /**
         * 设置显示值
         */
        fun setDisplayType(vararg types: Int): Builder {
            this.displayTypes = types
            return this
        }

        /**
         * 设置显示值
         */
        fun setDisplayType(types: MutableList<Int>?): Builder {
            this.displayTypes = types?.toIntArray()
            return this
        }

        /**
         * 设置默认时间
         */
        fun setDefaultTime(millisecond: Long): Builder {
            this.defaultMillisecond = millisecond
            return this
        }

        /**
         * 设置范围最小值
         */
        fun setMinTime(millisecond: Long): Builder {
            this.minTime = millisecond
            return this
        }

        /**
         * 设置范围最大值
         */
        fun setMaxTime(millisecond: Long): Builder {
            this.maxTime = millisecond
            return this
        }

        /**
         * 是否显示回到当前
         */
        fun showBackNow(b: Boolean): Builder {
            this.backNow = b
            return this
        }

        /**
         * 是否显示选中日期信息
         */
        fun showFocusDateInfo(b: Boolean): Builder {
            this.focusDateInfo = b
            return this
        }

        /**
         * 是否显示单位标签
         */
        fun showDateLabel(b: Boolean): Builder {
            this.dateLabel = b
            return this
        }

        /**
         * 显示模式
         */
        fun setBackGroundModel(model: Int): Builder {
            this.model = model
            return this
        }

        /**
         * 设置主题颜色
         */
        fun setThemeColor(@ColorInt themeColor: Int): Builder {
            this.themeColor = themeColor
            return this
        }

        /**
         * 设置标签文字
         * @param year 年标签
         * @param month 月标签
         * @param day 日标签
         * @param hour 时标签
         * @param min 分标签
         * @param second 秒标签
         */
        fun setLabelText(
            year: String = yearLabel,
            month: String = monthLabel,
            day: String = dayLabel,
            hour: String = hourLabel,
            min: String = minLabel,
            second: String = secondLabel
        ): Builder {
            this.yearLabel = year
            this.monthLabel = month
            this.dayLabel = day
            this.hourLabel = hour
            this.minLabel = min
            this.secondLabel = second
            return this
        }

        /**
         *设置是否循环滚动
         *setWrapSelectorWheel()
         *setLabelText("年","月","日","时")
         *setLabelText(month="月",hour="时")
         */
        fun setWrapSelectorWheel(vararg types: Int, wrapSelector: Boolean): Builder {
            return setWrapSelectorWheel(types.toMutableList(), wrapSelector)
        }

        /**
         * 设置是否循环滚动
         */
        fun setWrapSelectorWheel(wrapSelector: Boolean): Builder {
            return setWrapSelectorWheel(null, wrapSelector)
        }

        /**
         * 设置是否循环滚动
         */
        fun setWrapSelectorWheel(types: MutableList<Int>?, wrapSelector: Boolean): Builder {
            this.wrapSelectorWheelTypes = types
            this.wrapSelectorWheel = wrapSelector
            return this
        }


        /**
         * 绑定选择监听
         */
        fun setOnChoose(text: String = "确定", listener: ((Long) -> Unit)? = null): Builder {
            this.onChooseListener = listener
            this.chooseText = text
            return this
        }

        /**
         * 绑定取消监听
         */
        fun setOnCancel(text: String = "取消", listener: (() -> Unit)? = null): Builder {
            this.onCancelListener = listener
            this.cancelText = text
            return this
        }

        /**
         * 设置自定义选择器layout
         *
         */
        fun setPickerLayout(@NotNull layoutResId: Int): Builder {
            this.pickerLayoutResId = layoutResId
            return this
        }

        fun build(): CardDatePickerDialog {
            return CardDatePickerDialog(context, this)
        }
    }


    /**
     * 根据手机的分辨率dp 转成px(像素)
     */
    private fun dip2px(dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * 根据手机的分辨率px(像素) 转成dp
     */
    private fun px2dip(pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

}