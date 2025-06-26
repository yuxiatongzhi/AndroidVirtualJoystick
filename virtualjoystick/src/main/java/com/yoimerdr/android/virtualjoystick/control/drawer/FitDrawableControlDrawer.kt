package com.yoimerdr.android.virtualjoystick.control.drawer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.yoimerdr.android.virtualjoystick.control.Control
import com.yoimerdr.android.virtualjoystick.geometry.Circle
import com.yoimerdr.android.virtualjoystick.geometry.ImmutablePosition
import com.yoimerdr.android.virtualjoystick.geometry.Size
import com.yoimerdr.android.virtualjoystick.geometry.factory.RectFFactory
import com.yoimerdr.android.virtualjoystick.utils.log.Logger

/**
 * <TODO>
 *
 * @author 💎 Li Junchao
 */
open class FitDrawableControlDrawer(
    private val properties: FitDrawableProperties
): ControlDrawer {

    /**
     * @param drawable 要绘制的图像资源.
     * @param scale 缩放倍率 [drawable].
     * @param paint 绘制 [drawable] 的 [Paint].
     */
    constructor(drawable: Drawable, ratio: Float, paint: Paint?) : this(
        FitDrawableProperties(
            drawable,
            ratio,
            paint
        )
    )

    /**
     * @param drawable 要绘制的图像资源
     * @param ratio scale 缩放倍率 [drawable].
     */
    constructor(drawable: Drawable, ratio: Float) : this(drawable, ratio, null)

    /**
     * @param drawable 要绘制的图像资源
     * @param paint 绘制 [drawable] 的 [Paint].
     */
    constructor(drawable: Drawable, paint: Paint?) : this(drawable, 1f, paint)

    /**
     * @param drawable 要绘制的图像资源
     */
    constructor(drawable: Drawable) : this(drawable, null)

    open var drawable: Drawable
        get() = properties.drawable
        set(drawable) {
            properties.drawable = drawable
            checkDrawableSize()
            convertDrawableToBitmap()
        }

    open var ratio: Float
        /** 对 [drawable] 绘制的缩放 */
        get() = properties.ratio

        /**
         * Sets the ratio for the dimensions of the size of the drawable resource.
         * @throws ratio The new ratio value.
         * @throws IllegalArgumentException If the ratio is negative or zero.
         */
        @Throws(IllegalArgumentException::class)
        set(ratio) {
            if (ratio <= 0)
                throw IllegalArgumentException("The ratio value must be a value greater than zero.")
            if (this.ratio != ratio) {
                properties.ratio = ratio
                convertDrawableToBitmap()
            }
        }

    protected open val paint: Paint? get() = properties.paint

    protected open var circleSize: Size = Size(drawable.intrinsicWidth, drawable.intrinsicHeight)

    override fun onSizeChanged(size: Size) {
        super.onSizeChanged(size)
        circleSize = size
        convertDrawableToBitmap()
    }

    /**
     * 要绘制的 [Bitmap]
     */
    protected open val bitmap: Bitmap get() {
        if(mBitmap == null)
            convertDrawableToBitmap()
        return mBitmap!!
    }

    /**
     * The drawable bitmap.
     */
    private var mBitmap: Bitmap? = null

    init {
        checkDrawableSize()
        convertDrawableToBitmap()
    }

    open class FitDrawableProperties(var drawable: Drawable, var ratio: Float, val paint: Paint?)

    companion object {

        /**
         * 根据[DrawableRes] id 获取 [Drawable].
         *
         * @param context The current activity or view context.
         * @param resourceId The drawable resource id.
         * @throws IllegalArgumentException if doesn't exist a drawable for the given resourceId.
         */
        @JvmStatic
        @Throws(IllegalArgumentException::class)
        fun getDrawable(context: Context, @DrawableRes resourceId: Int): Drawable {
            val drawable = ContextCompat.getDrawable(context, resourceId)
            if (drawable != null)
                return drawable
            throw IllegalArgumentException("Don't exists a valid drawable for given resource id")
        }

        /**
         * Instance a [DrawableControlDrawer] from context and a [DrawableRes] id.
         * @param context The current activity or view context.
         * @param id The drawable resource id.
         * @param scale The scale ratio to scale the drawable. Must be a value greater than zero.
         * @param paint The [DrawableControlDrawer] paint.
         * @throws IllegalArgumentException If doesn't exist a drawable for the given id or the [scale] is not positive.
         */
        @JvmStatic
        @JvmOverloads
        @Throws(IllegalArgumentException::class)
        fun fromDrawableRes(
            context: Context,
            @DrawableRes id: Int,
            scale: Float = 1f,
            paint: Paint? = null
        ): FitDrawableControlDrawer {
            return FitDrawableControlDrawer(getDrawable(context, id), scale, paint)
        }

    }

    /**
     * Gets the scaled width dimension of the drawable.
     */
    protected val width: Float get() = circleSize.width * ratio

    /**
     * Gets for the scaled height dimension of the drawable.
     */
    protected val height: Float get() = circleSize.height * ratio

    /**
     * Gets half the value of [width].
     */
    protected val halfWidth: Float get() = width / 2f

    /**
     * Gets half the value of [height].
     */
    protected val halfHeight: Float get() = height / 2f


    /**
     * Check whether the dimensions of the drawable are similar,
     * if not log an error message.
     * @see [Logger.errorFromClass]
     */
    protected fun checkDrawableSize() {
        drawable.apply {
            if (intrinsicHeight != intrinsicWidth)
                Logger.errorFromClass(
                    this@FitDrawableControlDrawer,
                    "To avoid unexpected behavior, the width and height of the drawable should be the same or should not differ too much."
                )
        }
    }

    /**
     * Convert the drawable to a bitmap according the [width] and [height] dimensions.
     *
     * @see [bitmap]
     */
    @Throws(IllegalArgumentException::class)
    protected fun convertDrawableToBitmap() {
        mBitmap?.recycle()
        mBitmap = drawable.toBitmap(width.toInt(), height.toInt())
    }

    /**
     * Gets the current position where the control is located
     * and that the drawer will take as center to draw the drawable.
     * @param control The [Control] from where the drawer is used.
     */
    protected open fun getPosition(control: Control): ImmutablePosition {
        val max = control.viewRadius - maxOf(halfWidth, halfHeight)
        return if (max <= 0) {
            Logger.errorFromClass(
                this,
                "The size of the scaled drawable (width and height) is too large with respect to the view where the control is used. Try scaling it using the scale property of this drawer using a smaller value."
            )
            control.center
        } else if (control.distanceFromCenter > max)
            Circle.fromImmutableCenter(max, control.center)
                .parametricPositionOf(control.anglePosition)
        else control.position
    }

    /**
     * Gets the rectangle that the drawable will be scaled/translated to fit into.
     * @param control The [Control] from where the drawer is used.
     */
    protected open fun getDestination(control: Control): RectF {
        return RectFFactory.withCenterAt(getPosition(control), halfWidth, halfHeight)
    }

    override fun draw(canvas: Canvas, control: Control) {
        canvas.drawBitmap(bitmap, null, getDestination(control), paint)
    }
}