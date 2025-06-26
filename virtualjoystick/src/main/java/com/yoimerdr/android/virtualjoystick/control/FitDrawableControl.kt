package com.yoimerdr.android.virtualjoystick.control

import android.graphics.drawable.Drawable
import com.yoimerdr.android.virtualjoystick.control.drawer.ControlDrawer
import com.yoimerdr.android.virtualjoystick.control.drawer.FitDrawableControlDrawer
import com.yoimerdr.android.virtualjoystick.views.JoystickView

/**
 * 会使用半径比例自动调整控件大小的 [DrawableControl]
 *
 * @author 💎 Li Junchao
 */
class FitDrawableControl(
    drawable: Drawable,
    radiusRatio: Float,     // 使用半径比例来控制控件大小 radiusProportion
    invalidRadius: Float,
    directionType: JoystickView.DirectionType
) : Control(invalidRadius, directionType){
    override var drawer: ControlDrawer = FitDrawableControlDrawer(drawable, radiusRatio)
}