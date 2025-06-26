package com.yoimerdr.android.virtualjoystick.control

import android.graphics.drawable.Drawable
import com.yoimerdr.android.virtualjoystick.control.drawer.ControlDrawer
import com.yoimerdr.android.virtualjoystick.control.drawer.DrawableControlDrawer
import com.yoimerdr.android.virtualjoystick.views.JoystickView

/**
 * 使用指定的 [Drawable] 作为摇杆 control, 但需单独指定一个 `scale` 缩放倍数来调整 control 大小。
 * 建议使用 [FitDrawableControl]
 *
 * @author 💎 Li Junchao
 */
@Deprecated(message = "Use FitDrawableControl instead", replaceWith = ReplaceWith("FitDrawableControl"))
open class DrawableControl (
    drawable: Drawable,
    scale: Float = 1f,      // 使用缩放倍数 scale 来控制大小
    invalidRadius: Float,
    directionType: JoystickView.DirectionType
) : Control(invalidRadius, directionType){
    override var drawer: ControlDrawer = DrawableControlDrawer(drawable, scale)
}