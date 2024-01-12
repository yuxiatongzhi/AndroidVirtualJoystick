package com.yoimerdr.android.virtualjoystick.control.drawer

import android.graphics.Canvas
import com.yoimerdr.android.virtualjoystick.control.Control

open class ComposedControlDrawer(
    protected open val drawers: List<ControlDrawer>,
    protected val beforeDraw: BeforeDraw? = null
) : ControlDrawer {

    fun interface BeforeDraw {
        fun before(drawer: ControlDrawer, control: Control)
    }

    override fun draw(canvas: Canvas, control: Control) {
        drawers.forEach { drawer ->
            beforeDraw?.before(drawer, control)
            drawer.draw(canvas, control)
        }
    }
}