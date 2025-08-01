package com.yoimerdr.android.virtualjoystick.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.yoimerdr.android.virtualjoystick.control.Control
import com.yoimerdr.android.virtualjoystick.control.drawer.ColorfulControlDrawer
import com.yoimerdr.android.virtualjoystick.control.drawer.ControlDrawer
import com.yoimerdr.android.virtualjoystick.exceptions.InvalidControlPositionException
import com.yoimerdr.android.virtualjoystick.geometry.FixedPosition
import com.yoimerdr.android.virtualjoystick.geometry.ImmutablePosition
import com.yoimerdr.android.virtualjoystick.geometry.Plane
import com.yoimerdr.android.virtualjoystick.geometry.Size
import com.yoimerdr.android.virtualjoystick.theme.ColorsScheme
import com.yoimerdr.android.virtualjoystick.utils.log.Logger
import com.yoimerdr.android.virtualjoystick.views.handler.TouchHoldEventHandler
import com.yuxiatongzhi.virtualjoystick.R
import androidx.core.content.withStyledAttributes
import com.yoimerdr.android.virtualjoystick.control.drawer.FitDrawableControlDrawer

/**
 * A view representing a virtual joystick.
 */
class JoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defaultStyle: Int = 0
) : View(context, attrs, defaultStyle) {

    /**
     * The [Size] representation of the view size.
     */
    private val viewSize: Size get() = Size(width, height)

    private val viewRadius: Double get() = viewSize.width / 2.0

    /**
     * The control movement listener.
     */
    private var listener: MoveListener? = null

    /**
     * The interval for the joystick listener call when it is hold.
     */
    private var interval: Long = HOLD_INTERVAL
        set(value) {
            field = getHoldInterval(value)
            touchHandler.apply {
                holdInterval = field
                activeHoldInterval = field
            }
        }

    /**
     * The control holds handler.
     */
    private val touchHandler: JoystickTouchHandler = JoystickTouchHandler(this, interval)

    /**
     * The builder to build the controls defined in the package.
     */
    private var controlBuilder: Control.Builder = Control.Builder()

    /**
     * The joystick [Control].
     */
    private var control: Control

    /**
     * Gets the current [control] position.
     */
    val position: ImmutablePosition get() = control.position

    /**
     * Gets the current [control] center.
     */
    val center: ImmutablePosition get() = control.center

    /**
     * Gets distance between current [position] and [center].
     */
    val distance: Float get() = control.distanceFromCenter

    /**
     * Gets the angle (clockwise) formed from the current [position] and the [center].
     *
     * @return A value in the range from 0 to 2PI radians.
     */
    val angle: Double get() = control.anglePosition

    /**
     * 归一化标准设备坐标 x轴(-1, 1),y轴(-1, 1)
     */
    val ndcAxis: PointF get() = control.ndcAxis

    init {
        var primaryColor = ContextCompat.getColor(context, R.color.drawer_primary)
        var accentColor = ContextCompat.getColor(context, R.color.drawer_accent)

        // 无效半径——此半径范围内，方向无效
        var invalidRadius: Float = resources.getDimensionPixelSize(R.dimen.invalidRadiusDim).toFloat()
        var backgroundRes: Int = -1
        var controlType = Control.DefaultType.CIRCLE
        var directionType = DirectionType.COMPLETE

        var arcSweepAngle: Float = ResourcesCompat.getFloat(resources, R.dimen.arc_sweepAngle)
        var arcStrokeWidth: Float = ResourcesCompat.getFloat(resources, R.dimen.arc_strokeWidth)

        var circleRadiusProportion: Float = ResourcesCompat.getFloat(resources, R.dimen.circle_radiusRatio)

        @DrawableRes var drawableControlRes: Int = -1
        var drawableControlDrawerScale: Float = 1.0f

        if(attrs != null) {
            context.withStyledAttributes(attrs, R.styleable.JoystickView) {
                interval =
                    getInteger(R.styleable.JoystickView_moveInterval, interval.toInt())
                        .toLong()

                // all types
                invalidRadius = getDimensionPixelSize(
                    R.styleable.JoystickView_invalidRadius,
                    invalidRadius.toInt()
                ).toFloat()
                primaryColor = getColor(
                    R.styleable.JoystickView_controlDrawer_primaryColor,
                    primaryColor
                )
                accentColor =
                    getColor(R.styleable.JoystickView_controlDrawer_accentColor, accentColor)
                controlType = Control.DefaultType.fromId(
                    getInt(
                        R.styleable.JoystickView_controlType,
                        controlType.id
                    )
                )
                directionType = DirectionType.fromId(
                    getInt(
                        R.styleable.JoystickView_directionType,
                        directionType.id
                    )
                )
                backgroundRes = getBackgroundResOf(controlType).let {
                    getResourceId(R.styleable.JoystickView_background, it)
                }

                // arc types
                arcStrokeWidth = getFloat(
                    R.styleable.JoystickView_arcControlDrawer_strokeWidth,
                    arcStrokeWidth
                )
                arcSweepAngle = getFloat(
                    R.styleable.JoystickView_arcControlDrawer_sweepAngle,
                    arcSweepAngle
                )

                // circle types
                circleRadiusProportion = getFloat(
                    R.styleable.JoystickView_circleControlDrawer_radiusProportion,
                    circleRadiusProportion
                )

                if (controlType == Control.DefaultType.DRAWABLE) {
                    drawableControlRes = getResourceId(R.styleable.JoystickView_drawableControlDrawer_vectorDrawable, -1)
                    drawableControlDrawerScale =
                        getFloat(R.styleable.JoystickView_drawableControlDrawer_scale, 1f)

                }
            }
        } else {
            backgroundRes = getBackgroundResOf(controlType)
        }

        val controlDrawer = try {
            if (drawableControlRes != -1) {
                getCompatDrawable(drawableControlRes)
            } else null
        } catch (_: Exception) { null }

        control = controlBuilder
            .primaryColor(primaryColor)
            .accentColor(accentColor)
            .invalidRadius(invalidRadius)
            .arcStrokeWidth(arcStrokeWidth)
            .arcSweepAngle(arcSweepAngle)
            .circleRadiusRatio(circleRadiusProportion)
            .type(controlType)
            .directionType(directionType)
            .controlDrawer(controlDrawer, drawableControlDrawerScale)
            .build()

        background = try {
            getCompatDrawable(backgroundRes)
        } catch (_: Exception) {
            getCompatDrawable(JoystickView.getBackgroundResOf(controlType))
        }

    }

    /**
     * Gets a drawable resource from this resources.
     */
    private fun getCompatDrawable(@DrawableRes id: Int): Drawable? = ResourcesCompat.getDrawable(resources, id, context.theme)

    companion object {
        /**
         * The default interval in ms for the joystick [listener] call.
         */
        const val HOLD_INTERVAL: Long = 150

        /**
         * Checks if the value of [interval] is not less than zero.
         * @param interval A interval value in ms.
         * @return [HOLD_INTERVAL] if interval is less than zero; otherwise, the interval value.
         */
        @JvmStatic
        fun getHoldInterval(interval: Long): Long {
            return interval.coerceAtLeast(HOLD_INTERVAL)
        }

        /**
         * Gets the id of the drawable associated with the control type.
         * @param type The control type.
         */
        @JvmStatic
        @DrawableRes
        fun getBackgroundResOf(type: Control.DefaultType): Int {
            return when(type) {
                Control.DefaultType.ARC -> R.drawable.arcfor_bg
                else -> R.drawable.circlefor_bg
            }
        }
    }

    /**
     * 摇杆的 **方向**
     */
    enum class Direction {
        UP,
        LEFT,
        RIGHT,
        DOWN,
        UP_RIGHT,
        UP_LEFT,
        DOWN_RIGHT,
        DOWN_LEFT,
        NONE
    }

    /**
     * The type that will determine how many directions the joystick will be able to return.
     */
    enum class DirectionType(val id: Int) {
        /**
         * Determines that the joystick will be able to return all the entries of [Direction] enum.
         */
        COMPLETE(1),

        /**
         * Determines that the joystick will only be able to return 5 directions.
         *
         * [Direction.RIGHT], [Direction.DOWN], [Direction.LEFT], [Direction.UP] and [Direction.NONE]
         */
        SIMPLE(2);

        companion object {
            /**
             * @param id The id for the enum value
             * @return The enum value for the given id. If not found, returns the value [COMPLETE].
             */
            @JvmStatic
            fun fromId(id: Int): DirectionType {
                for(type in DirectionType.entries)
                    if(type.id == id)
                        return type

                return COMPLETE
            }
        }
    }

    /**
     * Joystick control movement listener.
     */
    fun interface MoveListener {
        /**
         * **摇杆** 移动时的 监听
         *
         * @param view      [View] 本身
         * @param direction [Direction] 摇杆方向
         * @param ndcAxis   物体坐标系归一化坐标 (x, y)
         */
        fun onMove(view: View, direction: Direction, ndcAxis: PointF)
    }

    /**
     * A handler for control holds.
     */
    private class JoystickTouchHandler(
        joystick: JoystickView,
        interval: Long,
    ) : TouchHoldEventHandler<JoystickView>(joystick, interval) {
        override fun touchHold() {
            view.move()
        }

        override fun touchDown(): Boolean {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }

            view.move()
            return true
        }

        override fun touchUp(): Boolean {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY_RELEASE)
            }

            view.control.toCenter()
            view.move(Direction.NONE)
            return true
        }

        override fun touchMove(): Boolean {
            view.move()
            return true
        }

        override fun notHandledTouch(event: MotionEvent): Boolean {
            return false
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        control.onSizeChanged(viewSize)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        var side = resources.getDimensionPixelSize(R.dimen.width)

        if(arrayOf(widthMode, heightMode).any { it == MeasureSpec.EXACTLY }) {
            val size = MeasureSpec.getSize(widthMeasureSpec)
                .coerceAtMost(MeasureSpec.getSize(heightMeasureSpec))
            if(size > 0)
                side = size
        }

        setMeasuredDimension(side, side)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        control.onDraw(canvas)
    }


    private fun move(direction: Direction, ndcAxis: PointF = PointF(0f, 0f)) {
        listener?.onMove(this, direction, ndcAxis)
    }

    private fun move() = move(control.direction, control.ndcAxis)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if(event == null)
            return false

        val touchPosition = FixedPosition(event.x, event.y)

        if(event.action != MotionEvent.ACTION_MOVE
            && event.action != MotionEvent.ACTION_UP
            && Plane.distanceBetween(touchPosition, center) > viewRadius) {
            if(!control.isInCenter()) {
                control.toCenter()
                invalidate()
            }
            return false
        }

        try {
            control.setPosition(touchPosition)
        } catch (e: InvalidControlPositionException) {
            Logger.errorFromClass(this@JoystickView, e)
            return false
        }

        return touchHandler.onTouchEvent(event).let {
            invalidate()
            it
        }

    }

    /**
     * Changes the current joystick move listener.
     * @param listener The new listener.
     */
    fun setMoveListener(listener: MoveListener) {
        this.listener = listener
    }

    /**
     * Changes the current interval for the joystick listener call when the control is hold.
     * @param interval An interval value in ms.
     */
    fun setHoldInterval(interval: Long) {
        this.interval = interval
    }

    /**
     * Changes the current interval for the joystick listener call when the control is hold.
     * @param interval An interval value in ms.
     */
    fun setHoldInterval(interval: Int) = setHoldInterval(interval.toLong())

    /**
     * Changes the current joystick control.
     * @param control The new [Control]
     */
    @Throws(InvalidControlPositionException::class)
    fun setControl(control: Control) {
        this.control = control.apply {
            val size = viewSize
            if(!size.isEmpty()) {
                onSizeChanged(size)
                setPosition(this@JoystickView.position)
                invalidate()
            }
        }
    }

    @Throws(InvalidControlPositionException::class)
    private fun buildControl() {
        setControl(controlBuilder.build())
    }

    /**
     * Changes the current control to one defined in the package.
     *
     * If you also want to change the background for the [type], use [setTypeAndBackground] instead.
     * @param type The new control type.
     */
    @Throws(InvalidControlPositionException::class)
    fun setType(type: Control.DefaultType) {
        controlBuilder.type(type)
        buildControl()
        invalidate()
    }

    /**
     * Changes the current control to one defined in the package,
     * and also changes the background to the one associated with the [type] according to [getBackgroundResOf].
     *
     * If you only want to change the the [type], use [setType]
     * @param type The new control type.
     */
    @Throws(InvalidControlPositionException::class)
    fun setTypeAndBackground(type: Control.DefaultType) {
        setType(type)
        val drawable = getCompatDrawable(getBackgroundResOf(type))
        if(drawable != null)
            background = drawable
    }

    fun setTypeAndBackground(type: Control.DefaultType, @DrawableRes backgroundRes: Int) {
        setType(type)
        val drawable = getCompatDrawable(backgroundRes)
        if(drawable != null)
            background = drawable
    }

    private val colorfulDrawer: ColorfulControlDrawer? get() {
        val drawer = control.drawer
        if(drawer is ColorfulControlDrawer)
            return drawer
        return null
    }

    /**
     * Changes the primary colour of the current [control]'s drawer.
     *
     * If the current control's drawer is custom (not defined in the package)
     * that does not inherit from [ColorfulControlDrawer], nothing will be changed.
     */
    fun setPrimaryColor(@ColorInt color: Int) {
        colorfulDrawer?.primaryColor = color
        controlBuilder.primaryColor(color)
        invalidate()
    }

    /**
     * Changes the accent colour of the current [control]'s drawer.
     *
     * If the current control's drawer is custom (not defined in the package)
     * that does not inherit from [ColorfulControlDrawer], nothing will be changed.
     */
    fun setAccentColor(@ColorInt color: Int) {
        colorfulDrawer?.accentColor = color
        controlBuilder.accentColor(color)
        invalidate()
    }

    /**
     * Changes the colors of the current [control]'s drawer.
     *
     * If the current control's drawer is custom (not defined in the package)
     * that does not inherit from [ColorfulControlDrawer], nothing will be changed.
     */
    fun setColors(colors: ColorsScheme) {
        colorfulDrawer?.setColors(colors)
        controlBuilder.colors(colors)
        invalidate()
    }

    /**
     * Changes the [Control.invalidRadius] of the current control.
     */
    fun setInvalidRadius(radius: Float) {
        control.invalidRadius = radius
        controlBuilder.invalidRadius(radius)
    }

    /**
     * Changes the [Control.directionType] property of the current control.
     */
    fun setDirectionType(type: DirectionType) {
        control.directionType = type
        controlBuilder.directionType(type)
    }

    /**
     * Changes the drawer of the current [control].
     */
    fun setControlDrawer(drawer: ControlDrawer) {
        control.drawer = drawer
        invalidate()
    }

    /**
     * 设置 绘制 [ControlDrawer] 所占的半径比例
     *
     * @param ratio 半径比率 (0.1 ~ 0.8)
     */
    fun setDrawerRadiusRatio(@FloatRange(from = 0.1, to = 0.8) ratio: Float) {
        controlBuilder.circleRadiusRatio(ratio)
        buildControl()
        invalidate()
    }

    /**
     * 设置 弧形 绘制 的 扫过的角度
     *
     * @param angle (30.0 ~ 180.0)
     */
    fun setArcSweepAngle(@FloatRange(from = 30.0, to = 180.0) angle: Float) {
        controlBuilder.arcSweepAngle(angle)
        buildControl()
        invalidate()
    }

    /**
     * 设置 弧形 绘制 的 线宽
     *
     * @param width 默认 13.0
     */
    fun setArcStrokeWidth(@FloatRange(from = 5.0) width: Float) {
        controlBuilder.arcStrokeWidth(width)
        buildControl()
        invalidate()
    }

    /**
     * 控制器类型为 [Control.DefaultType.DRAWABLE] 时，设置一个 [Drawable] 作为 [ControlDrawer]
     *
     * @param drawable      drawable resource id
     * @param radiusRatio   绘制此 Drawer 所使用的半径比例, 如果要基于自size控制大小，请采用 [setControlDrawer]
     */
    fun setControlDrawerDrawable(
        @DrawableRes drawable: Int,
        @FloatRange(from = 0.1, to = 0.8) radiusRatio: Float
    ) {
        getCompatDrawable(drawable)?.let {
            setControlDrawerDrawable(it, radiusRatio)
        }
    }

    /**
     * 控制器类型为 [Control.DefaultType.DRAWABLE] 时，设置一个 [Drawable] 作为 [ControlDrawer]
     *
     * @param drawable      [Drawable]
     * @param radiusRatio   绘制此 Drawer 所使用的半径比例
     */
    fun setControlDrawerDrawable(
        drawable: Drawable,
        @FloatRange(from = 0.1, to = 0.8) radiusRatio: Float
    ) {
        controlBuilder.controlDrawer(drawable)
        controlBuilder.circleRadiusRatio(radiusRatio)

        if (this.control.drawer is FitDrawableControlDrawer) {
            buildControl()
            invalidate()
        }
    }
}