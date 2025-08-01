package com.yoimerdr.android.virtualjoystick.control.drawer

import android.graphics.Canvas
import com.yoimerdr.android.virtualjoystick.control.Control
import com.yoimerdr.android.virtualjoystick.geometry.Size

/**
 * Interface for draw the control representation.
 */
fun interface ControlDrawer {
    /**
     * Draw the control representation.
     *
     * @param canvas The view canvas.
     * @param control The [Control] from where the drawer is used.
     */
    fun draw(canvas: Canvas, control: Control)

    fun onSizeChanged(size: Size) {}
}