package com.example.routerproxy

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

class LiquidBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density = resources.displayMetrics.density

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()

        paint.shader = LinearGradient(
            0f,
            0f,
            width,
            height,
            Color.rgb(4, 4, 5),
            Color.rgb(18, 18, 19),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width, height, paint)

        paint.maskFilter = BlurMaskFilter(90f * density, BlurMaskFilter.Blur.NORMAL)
        drawGlow(canvas, width * 0.16f, height * 0.16f, 250f, Color.argb(42, 255, 255, 255))
        drawGlow(canvas, width * 0.88f, height * 0.42f, 280f, Color.argb(26, 255, 255, 255))
        drawGlow(canvas, width * 0.52f, height * 0.92f, 320f, Color.argb(24, 255, 255, 255))
        paint.maskFilter = null

        paint.shader = null
        paint.color = Color.argb(10, 255, 255, 255)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f * density
        canvas.drawLine(width * 0.08f, height * 0.08f, width * 0.92f, height * 0.08f, paint)
        canvas.drawLine(width * 0.08f, height * 0.92f, width * 0.92f, height * 0.92f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawGlow(canvas: Canvas, centerX: Float, centerY: Float, radiusDp: Float, color: Int) {
        val radius = radiusDp * density
        paint.shader = RadialGradient(
            centerX,
            centerY,
            radius,
            color,
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(centerX, centerY, radius, paint)
    }
}