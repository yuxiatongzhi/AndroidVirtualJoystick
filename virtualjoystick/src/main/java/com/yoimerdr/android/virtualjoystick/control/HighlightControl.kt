package com.yoimerdr.android.virtualjoystick.control

import com.yoimerdr.android.virtualjoystick.control.drawer.ControlDrawer
import com.yoimerdr.android.virtualjoystick.control.drawer.HighlightControlDrawer
import com.yoimerdr.android.virtualjoystick.theme.ColorsScheme
import com.yoimerdr.android.virtualjoystick.views.JoystickView

/**
 * <TODO>
 *
 * @author 💎 Li Junchao
 */
class HighlightControl(
    colors: ColorsScheme,
    strictColor: Boolean = false,
    radiusRatio: Float,
    invalidRadius: Float,
    directionType: JoystickView.DirectionType,
) : Control(invalidRadius, directionType) {
    override var drawer: ControlDrawer = HighlightControlDrawer(colors.primary, radiusRatio)
}