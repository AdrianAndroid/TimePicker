package com.timerpicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.alpha
import org.threeten.bp.LocalTime
import java.util.*
import kotlin.math.*

class SleepTimePicker @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    init {
        init(context, attrs)
    }

    private lateinit var progressPaint: Paint
    private lateinit var progressBackgroundPaint: Paint
    private var progressTopBlurPaint: Paint? = null
    private var progressBottomBlurPaint: Paint? = null
    private lateinit var divisionPaint: Paint
    private lateinit var progressDivisionPaint: Paint
    private lateinit var textPaint: Paint
    private var divisionOffset = 0 // 表的分隔条距离偏移角度
    private var labelOffset = 0 // 文字的偏移角度
    private var divisionLength = 0
    private var divisionWidth = 0
    private var progressDivisionWidth = 0
    private val hourLabels = listOf(0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22)

    private lateinit var circleBounds: RectF

    private var radius: Float = 0F
    private var center = Point(0, 0)
    private var progressBottomShadowSize = 0
    private var progressTopShadowSize = 0
    private var progressDivisionSpace = 0 // progress division 的安全距离
    private var strokeBottomShadowColor = Color.TRANSPARENT
    private var strokeTopShadowColor = Color.TRANSPARENT
    private var labelColor = Color.WHITE
    private lateinit var sleepLayout: View
    private lateinit var wakeLayout: View
    private var sleepAngle = 30.0 // 开始角度
    private var wakeAngle = 225.0 // 结束角度
    private var draggingSleep = false
    private var draggingWake = false
    private val stepMinutes = 15
    private val textRect = Rect()

    var listener: ((bedTime: LocalTime, wakeTime: LocalTime) -> Unit)? = null

//    var progressColor: Int
//        @ColorInt
//        get() = progressPaint.color
//        set(@ColorInt color) {
//            progressPaint.color = color
//            invalidate()
//        }
//
//    var progressBackgroundColor: Int
//        @ColorInt
//        get() = progressBackgroundPaint.color
//        set(@ColorInt color) {
//            progressBackgroundPaint.color = color
//            invalidate()
//        }

    /**
     * 弧度进度条的宽度
     */
    private val progressStrokeWidth: Float
        get() = progressPaint.strokeWidth

    /**
     * 弧度背景的宽度
     */
    private val progressBgStrokeWidth: Float
        get() = progressBackgroundPaint.strokeWidth

    fun getBedTime() = computeBedTime()

    fun getWakeTime() = computeWakeTime()

    fun setTime(bedTime: LocalTime, wakeTime: LocalTime) {
        sleepAngle = minutesToAngle(bedTime.hour * 60 + bedTime.minute)
        wakeAngle = minutesToAngle(wakeTime.hour * 60 + wakeTime.minute)
        invalidate()
        notifyChanges()
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        divisionOffset = dp2px(DEFAULT_DIVISION_OFFSET_DP)
        divisionLength = dp2px(DEFAULT_DIVISION_LENGTH_DP)
        divisionWidth = dp2px(DEFAULT_DIVISION_WIDTH_DP)
        progressDivisionWidth = dp2px(DEFAULT_DIVISION_WIDTH_DP)
        labelOffset = dp2px(DEFAULT_LABEL_OFFSET_DP)
        progressDivisionSpace = dp2px(DEFAULT_PROGRESS_DIVISION_SPACE_DP)
        var progressColor = Color.WHITE
        var progressBackgroundColor = Color.parseColor(DEFAULT_PROGRESS_BACKGROUND_COLOR)
        var divisionColor = Color.parseColor(DEFAULT_PROGRESS_BACKGROUND_COLOR)
        var progressDivisionColor = Color.parseColor(DEFAULT_PROGRESS_BACKGROUND_COLOR)
        var progressStrokeWidth = dp2px(DEFAULT_STROKE_WIDTH_DP)
        var progressBgStrokeWidth = dp2px(DEFAULT_STROKE_WIDTH_DP)
        var progressStrokeCap: Paint.Cap = Paint.Cap.ROUND
        var sleepLayoutId = 0
        var wakeLayoutId = 0
        var divisionAlpha = DEFAULT_DIVISION_ALPHA
        var progressDivisionAlpha = DEFAULT_DIVISION_ALPHA


        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.SleepTimePicker)

            sleepLayoutId = a.getResourceId(R.styleable.SleepTimePicker_sleepLayoutId, 0)
            wakeLayoutId = a.getResourceId(R.styleable.SleepTimePicker_wakeLayoutId, 0)

            progressColor = a.getColor(R.styleable.SleepTimePicker_progressColor, progressColor)
            progressBackgroundColor =
                a.getColor(R.styleable.SleepTimePicker_progressBackgroundColor, progressBackgroundColor)
            divisionColor = a.getColor(R.styleable.SleepTimePicker_divisionColor, divisionColor)
            divisionAlpha = a.getColor(R.styleable.SleepTimePicker_divisionAlpha, divisionAlpha)
            progressDivisionColor = a.getColor(R.styleable.SleepTimePicker_progressDivisionColor, progressDivisionColor)
            progressDivisionAlpha = a.getColor(R.styleable.SleepTimePicker_progressDivisionAlpha, progressDivisionAlpha)
            progressStrokeWidth =
                a.getDimensionPixelSize(R.styleable.SleepTimePicker_progressStrokeWidth, progressStrokeWidth)
            progressBottomShadowSize = a.getDimensionPixelSize(R.styleable.SleepTimePicker_strokeBottomShadowRadius, 0)
            progressTopShadowSize = a.getDimensionPixelSize(R.styleable.SleepTimePicker_strokeTopShadowRadius, 0)
            progressDivisionSpace = a.getDimensionPixelSize(R.styleable.SleepTimePicker_progressDivisionSpace, dp2px(
                DEFAULT_PROGRESS_DIVISION_SPACE_DP))
            progressBgStrokeWidth =
                a.getDimensionPixelSize(R.styleable.SleepTimePicker_progressBgStrokeWidth, progressStrokeWidth)
            strokeBottomShadowColor = a.getColor(R.styleable.SleepTimePicker_strokeBottomShadowColor, progressColor)
            strokeTopShadowColor = a.getColor(R.styleable.SleepTimePicker_strokeTopShadowColor, progressColor)
            labelColor = a.getColor(R.styleable.SleepTimePicker_labelColor, progressColor)

            progressStrokeCap = Paint.Cap.ROUND

            a.recycle()
        }

        progressPaint = Paint()
        progressPaint.strokeCap = progressStrokeCap
        progressPaint.strokeWidth = progressStrokeWidth.toFloat()
        progressPaint.style = Paint.Style.STROKE
        progressPaint.color = progressColor
        progressPaint.isAntiAlias = true

        progressBackgroundPaint = Paint()
        progressBackgroundPaint.style = Paint.Style.STROKE
        progressBackgroundPaint.strokeWidth = progressBgStrokeWidth.toFloat()
        progressBackgroundPaint.color = progressBackgroundColor
        progressBackgroundPaint.isAntiAlias = true

        if (progressTopShadowSize > 0) {
            progressTopBlurPaint = Paint()
            progressTopBlurPaint!!.strokeCap = Paint.Cap.ROUND
            progressTopBlurPaint!!.strokeWidth = BLUR_STROKE_RATIO * (progressTopShadowSize + progressStrokeWidth)
            progressTopBlurPaint!!.style = Paint.Style.STROKE
            progressTopBlurPaint!!.isAntiAlias = true
            val topBlurRadius = BLUR_RADIUS_RATIO * (progressTopShadowSize + progressBgStrokeWidth)
            progressTopBlurPaint!!.maskFilter = BlurMaskFilter(topBlurRadius, BlurMaskFilter.Blur.NORMAL)
            progressTopBlurPaint!!.color = strokeTopShadowColor
        }

        if (progressBottomShadowSize > 0) {
            progressBottomBlurPaint = Paint(0)
            progressBottomBlurPaint!!.strokeCap = Paint.Cap.ROUND
            progressBottomBlurPaint!!.strokeWidth = BLUR_STROKE_RATIO * (progressBottomShadowSize + progressStrokeWidth)
            progressBottomBlurPaint!!.style = Paint.Style.STROKE
            progressBottomBlurPaint!!.isAntiAlias = true
            val bottomBlurRadius = BLUR_RADIUS_RATIO * (progressBottomShadowSize + progressBgStrokeWidth)
            progressBottomBlurPaint!!.maskFilter = BlurMaskFilter(bottomBlurRadius, BlurMaskFilter.Blur.NORMAL)
            progressBottomBlurPaint!!.color = strokeBottomShadowColor
        }

        divisionPaint = Paint(0)
        divisionPaint.strokeCap = Paint.Cap.BUTT
        divisionPaint.strokeWidth = divisionWidth.toFloat()
        divisionPaint.color = divisionColor
        divisionPaint.style = Paint.Style.STROKE
        divisionPaint.isAntiAlias = true
        divisionPaint.alpha = divisionAlpha

        progressDivisionPaint = Paint(0)
        progressDivisionPaint.strokeCap = Paint.Cap.BUTT
        progressDivisionPaint.strokeWidth = progressDivisionWidth.toFloat()
        progressDivisionPaint.color = progressDivisionColor
        progressDivisionPaint.style = Paint.Style.STROKE
        progressDivisionPaint.isAntiAlias = true
        progressDivisionPaint.alpha = progressDivisionAlpha

        textPaint = Paint()
        textPaint.textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, SCALE_LABEL_TEXT_SIZE,
            resources.displayMetrics
        )
        textPaint.color = labelColor

        val inflater = LayoutInflater.from(context)
        sleepLayout = inflater.inflate(sleepLayoutId, this, false)
        wakeLayout = inflater.inflate(wakeLayoutId, this, false)
        addView(sleepLayout)
        addView(wakeLayout)
        circleBounds = RectF()

        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        setWillNotDraw(false)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val measuredHeight = MeasureSpec.getSize(heightMeasureSpec)
        measureChildren(widthMeasureSpec, heightMeasureSpec)

        val smallestSide = Math.min(measuredWidth, measuredHeight)
        setMeasuredDimension(smallestSide, smallestSide)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        calculateBounds(w, h) // 大小变化的时候测量
        requestLayout()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        layoutView(sleepLayout, sleepAngle)
        layoutView(wakeLayout, wakeAngle)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            if (isTouchOnView(sleepLayout, ev)) {
                draggingSleep = true
                return true
            }
            if (isTouchOnView(wakeLayout, ev)) {
                draggingWake = true
                return true
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val x = ev.x
        val y = ev.y

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val touchAngleRad = atan2(center.y - y, x - center.x).toDouble()
                if (draggingSleep) {
                    val sleepAngleRad = Math.toRadians(sleepAngle) // 角度转弧度
                    val diff = Math.toDegrees(angleBetweenVectors(sleepAngleRad, touchAngleRad))
                    sleepAngle = to_0_720(sleepAngle + diff)
                    requestLayout()
                    notifyChanges()
                    return true
                } else if (draggingWake) {
                    val wakeAngleRad = Math.toRadians(wakeAngle)
                    val diff = Math.toDegrees(angleBetweenVectors(wakeAngleRad, touchAngleRad))
                    wakeAngle = to_0_720(wakeAngle + diff)
                    requestLayout()
                    notifyChanges()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                draggingSleep = false
                draggingWake = false
            }
        }
        return super.onTouchEvent(ev)
    }

    override fun onDraw(canvas: Canvas) {
        drawProgressBackground(canvas) // progress 的背景颜色
        drawProgress(canvas) // progress 进度条
        drawProgressDivisions(canvas) // 进度条上的分隔条
        drawDivisions(canvas) // 画刻度
    }

    private fun notifyChanges() {
        val computeBedTime = computeBedTime()
        val computeWakeTime = computeWakeTime()
        listener?.invoke(computeBedTime, computeWakeTime)
    }

    private fun computeBedTime(): LocalTime {
        val bedMins: Int = snapMinutes(angleToMins(sleepAngle), stepMinutes)
        return LocalTime.of((bedMins / 60) % 24, bedMins % 60)
    }

    private fun computeWakeTime(): LocalTime {
        val wakeMins: Int = snapMinutes(angleToMins(wakeAngle), stepMinutes)
        return LocalTime.of((wakeMins / 60) % 24, wakeMins % 60)
    }

    private fun layoutView(view: View, angle: Double) {
        val measuredWidth = view.measuredWidth
        val measuredHeight = view.measuredHeight
        val halfWidth = measuredWidth / 2
        val halfHeight = measuredHeight / 2
        val parentCenterX = width / 2
        val parentCenterY = height / 2
        val centerX = (parentCenterX + radius * cos(Math.toRadians(angle))).toInt()
        val centerY = (parentCenterY - radius * sin(Math.toRadians(angle))).toInt()
        view.layout(
            (centerX - halfWidth),
            centerY - halfHeight,
            centerX + halfWidth,
            centerY + halfHeight
        )
    }

    private fun calculateBounds(mWidth: Int, mHeight: Int) {
        val maxChildWidth = 0 //max(sleepLayout.measuredWidth, wakeLayout.measuredWidth)
        val maxChildHeight = 0 //max(sleepLayout.measuredHeight, wakeLayout.measuredHeight)
        val maxChildSize = max(maxChildWidth, maxChildHeight) // 正方形
        val offset = progressBackgroundPaint.strokeWidth //progressBackgroundPaint.strokeWidth // abs(progressBackgroundPaint.strokeWidth / 2 - maxChildSize / 2)
        val width = mWidth - paddingStart - paddingEnd - maxChildSize - offset
        val height = mHeight - paddingTop - paddingBottom - maxChildSize - offset
        if (BuildConfig.DEBUG) {
            val sb = StringBuilder()
            sb.append("width=").append(width).append(", mWidth=").append(mWidth).append(", paddingStart=").append(paddingStart).append(", paddingEnd=").append(paddingEnd).append(", maxChildSize=").append(maxChildSize).append(", offset=").append(offset).append("\n")
            sb.append("height=").append(height).append(", mHeight=").append(mHeight).append(", paddingTop=").append(paddingTop).append(", paddingBottom=").append(paddingBottom).append(", maxChildSize=").append(maxChildSize).append(", offset=").append(offset)
            Log.i(TAG, sb.toString())
        }

        radius = min(width, height) / 2F
        center = Point(mWidth / 2, mHeight / 2)

        circleBounds.left = center.x - radius
        circleBounds.top = center.y - radius
        circleBounds.right = center.x + radius
        circleBounds.bottom = center.y + radius
    }

    private fun isTouchOnView(view: View, ev: MotionEvent): Boolean {
        return (ev.x > view.left && ev.x < view.right
                && ev.y > view.top && ev.y < view.bottom)
    }


    private fun drawProgressBackground(canvas: Canvas) {
        // oval, startAngle, sweepAngle, useCenter, paint
        canvas.drawArc(
            circleBounds, ANGLE_START_PROGRESS_BACKGROUND.toFloat(),
            ANGLE_END_PROGRESS_BACKGROUND.toFloat(),
            false, progressBackgroundPaint
        )
    }

    private fun drawProgress(canvas: Canvas) {
        val startAngle = -sleepAngle.toFloat()
        val sweep = to_0_360(sleepAngle - wakeAngle).toFloat() // 计算弧度
        progressBottomBlurPaint?.let {
            canvas.drawArc(circleBounds, startAngle, sweep, false, it)
        }
        progressTopBlurPaint?.let {
            canvas.drawArc(circleBounds, startAngle, sweep, false, it)
        }
        canvas.drawArc(circleBounds, startAngle, sweep, false, progressPaint)
    }

    private fun drawProgressDivisions(canvas: Canvas) {
        val startAngle = -sleepAngle + progressDivisionSpace
        val endAngle = -wakeAngle - progressDivisionSpace
        val betweenAngles = to_0_360(endAngle - startAngle).toFloat() // 计算弧度
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "drawProgressDivisions startAngle=$startAngle, endAngle=$endAngle betweenAngles=$betweenAngles")
        }
        val count: Int = (betweenAngles / 2).roundToInt()
        val subDivisionLength = progressStrokeWidth / 3
        val distance = radius + subDivisionLength / 2
        var currentAngel = startAngle
        for (index in 0..count) {
            val angle = currentAngel
            val radians = Math.toRadians(angle)
            val startX = center.x + distance * cos(radians)
            val endX = center.x + (distance - subDivisionLength) * cos(radians)
            val startY = center.y + distance * sin(radians)
            val endY = center.y + (distance - subDivisionLength) * sin(radians)
            canvas.drawLine(startX.toFloat(), startY.toFloat(), endX.toFloat(), endY.toFloat(), progressDivisionPaint)
            currentAngel += 2
        }
    }

    private fun drawDivisions(canvas: Canvas) {
        val divisionAngle: Int = 360 / hourLabels.size
        hourLabels.forEachIndexed { index, value ->
            val bgStrokeWidth = progressBackgroundPaint.strokeWidth
            val distance = radius - bgStrokeWidth / 2 - divisionOffset

            val angle = (divisionAngle * index) - 90 // 当时的角度
            val radians = Math.toRadians(angle.toDouble())
            val startX = center.x + distance * cos(radians)
            val endX = center.x + (distance - divisionLength) * cos(radians)
            val startY = center.y + distance * sin(radians)
            val endY = center.y + (distance - divisionLength) * sin(radians)
            canvas.drawLine(startX.toFloat(), startY.toFloat(), endX.toFloat(), endY.toFloat(), divisionPaint)

            val subDivisionLength = divisionLength / 2

            // 画剩下的两个刻度
            val angle1 = angle - 10
            val radians1 = Math.toRadians(angle1.toDouble())
            val startX1 = center.x + distance * cos(radians1)
            val endX1 = center.x + (distance - subDivisionLength) * cos(radians1)
            val startY1 = center.y + distance * sin(radians1)
            val endY1 = center.y + (distance - subDivisionLength) * sin(radians1)
            canvas.drawLine(startX1.toFloat(), startY1.toFloat(), endX1.toFloat(), endY1.toFloat(), divisionPaint)

            // 画剩下的两个刻度
            val angle2 = angle - 20
            val radians2 = Math.toRadians(angle2.toDouble())
            val startX2 = center.x + distance * cos(radians2)
            val endX2 = center.x + (distance - subDivisionLength) * cos(radians2)
            val startY2 = center.y + distance * sin(radians2)
            val endY2 = center.y + (distance - subDivisionLength) * sin(radians2)
            canvas.drawLine(startX2.toFloat(), startY2.toFloat(), endX2.toFloat(), endY2.toFloat(), divisionPaint)

            val tmp = value.toString()
            textPaint.getTextBounds(tmp, 0, tmp.length, textRect)
            val x = center.x + (radius - bgStrokeWidth / 2 - labelOffset) * cos(radians) - textRect.width() / 2
            val y = (center.y + (radius - bgStrokeWidth / 2 - labelOffset) * sin(radians) + textRect.height() / 2)
            canvas.drawText(tmp, x.toFloat(), y.toFloat(), textPaint)
        }
    }

    private fun dp2px(dp: Float): Int {
        val metrics = resources.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics).toInt()
    }

    fun to_0_360(angle: Double): Double {
        var result = angle % 360
        if (result < 0) result += 360
        return result
    }

    fun to_0_720(angle: Double): Double {
        var result = angle % 720
        if (result < 0) result += 720
        return result
    }

    fun minutesToAngle(mins: Int): Double {
        return to_0_720(90 - (mins / (12 * 60.0)) * 360.0)
    }

    fun angleToMins(angle: Double): Int {
        return (((to_0_720(90 - angle)) / 360) * 12 * 60).toInt()
    }

    /**
    @param angle1 - first angle in radians
    @param angle2 - second angle in radians
    @return angle between vectors in radians
     **/
    fun angleBetweenVectors(angle1: Double, angle2: Double): Double {
        val x1 = cos(angle1)
        val y1 = sin(angle1)
        val x2 = cos(angle2)
        val y2 = sin(angle2)
        return vectorsAngleRad(x1, y1, x2, y2)
    }

    private fun cross(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        return (x1 * y2 - y1 * x2)
    }

    private fun dot(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        return (x1 * x2 + y1 * y2)
    }

    fun vectorsAngleRad(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        return atan2(cross(x1, y1, x2, y2), dot(x1, y1, x2, y2))
    }

    fun snapMinutes(minutes: Int, step: Int): Int {
        return (minutes / step) * step + (2 * (minutes % step) / step) * step
    }

    companion object {
        private const val TAG = "SleepTimePicker"
        private const val ANGLE_START_PROGRESS_BACKGROUND = 0
        private const val ANGLE_END_PROGRESS_BACKGROUND = 360
        private const val DEFAULT_STROKE_WIDTH_DP = 8F
        private const val DEFAULT_DIVISION_LENGTH_DP = 8F
        private const val DEFAULT_DIVISION_OFFSET_DP = 2F
        private const val DEFAULT_LABEL_OFFSET_DP = 18F
        private const val DEFAULT_DIVISION_WIDTH_DP = 2F
        private const val DEFAULT_DIVISION_ALPHA = 255
        private const val DEFAULT_PROGRESS_DIVISION_SPACE_DP = 3F // progress division 到开头的安全距离
        private const val SCALE_LABEL_TEXT_SIZE = 8F
        private const val DEFAULT_PROGRESS_BACKGROUND_COLOR = "#e0e0e0"
        private const val BLUR_STROKE_RATIO = 3 / 8F
        private const val BLUR_RADIUS_RATIO = 1 / 4F
    }
}
