package com.wheelsspin.namepicker.ui

import android.graphics.Paint
import android.graphics.Rect
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.wheelsspin.namepicker.Entry
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Slice colours, chosen to stay legible against white label text in both themes. */
private val SLICE_COLORS = listOf(
    Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFF4511E),
    Color(0xFF8E24AA), Color(0xFF00897B), Color(0xFFD81B60), Color(0xFF3949AB),
    Color(0xFF7CB342), Color(0xFFF9A825), Color(0xFF00ACC1), Color(0xFF5E35B1),
)

@Composable
fun Wheel(
    entries: List<Entry>,
    rotationDegrees: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            if (entries.isEmpty()) {
                drawEmptyWheel()
            } else {
                drawWheel(entries, rotationDegrees)
            }
            drawPointer()
        }
    }
}

private fun DrawScope.drawEmptyWheel() {
    val radius = min(size.width, size.height) / 2f - 8f
    drawCircle(
        color = Color(0xFFBDBDBD),
        radius = radius,
        style = Stroke(width = 6f),
    )
}

private fun DrawScope.drawWheel(entries: List<Entry>, rotationDegrees: Float) {
    val radius = min(size.width, size.height) / 2f - 8f
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val sweep = 360f / entries.size

    val arcTopLeft = Offset(centerX - radius, centerY - radius)
    val arcSize = Size(radius * 2f, radius * 2f)

    // Canvas angles start at 3 o'clock; -90 puts slice 0 at the pointer when rotation is 0.
    val baseAngle = -90f + rotationDegrees

    entries.forEachIndexed { index, _ ->
        drawArc(
            color = SLICE_COLORS[index % SLICE_COLORS.size],
            startAngle = baseAngle + index * sweep,
            sweepAngle = sweep,
            useCenter = true,
            topLeft = arcTopLeft,
            size = arcSize,
        )
    }

    // Slice separators, drawn after the fills so they sit cleanly on top.
    entries.indices.forEach { index ->
        val angleRad = Math.toRadians((baseAngle + index * sweep).toDouble())
        drawLine(
            color = Color(0x33000000),
            start = Offset(centerX, centerY),
            end = Offset(
                centerX + (radius * cos(angleRad)).toFloat(),
                centerY + (radius * sin(angleRad)).toFloat(),
            ),
            strokeWidth = 2f,
        )
    }

    drawLabels(entries, baseAngle, sweep, radius, centerX, centerY)

    drawCircle(color = Color(0x22000000), radius = radius, style = Stroke(width = 6f))
    drawCircle(color = Color.White, radius = radius * 0.11f, center = Offset(centerX, centerY))
    drawCircle(
        color = Color(0xFF37474F),
        radius = radius * 0.11f,
        center = Offset(centerX, centerY),
        style = Stroke(width = 5f),
    )
}

private fun DrawScope.drawLabels(
    entries: List<Entry>,
    baseAngle: Float,
    sweep: Float,
    radius: Float,
    centerX: Float,
    centerY: Float,
) {
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.White.toArgb()
        textAlign = Paint.Align.RIGHT
        textSize = (radius * 0.11f).coerceIn(18f, 46f)
        isFakeBoldText = true
        setShadowLayer(4f, 0f, 1f, Color(0x66000000).toArgb())
    }

    // Longer lists mean thinner slices, so labels get proportionally less room.
    val available = radius * 0.72f
    val bounds = Rect()

    drawContext.canvas.nativeCanvas.apply {
        entries.forEachIndexed { index, entry ->
            val midAngle = baseAngle + (index + 0.5f) * sweep
            val label = TextUtils.ellipsize(
                entry.name, paint, available, TextUtils.TruncateAt.END
            ).toString()

            paint.getTextBounds(label, 0, label.length, bounds)

            save()
            rotate(midAngle, centerX, centerY)
            drawText(
                label,
                centerX + radius * 0.9f,
                centerY + bounds.height() / 2f,
                paint,
            )
            restore()
        }
    }
}

/** Fixed marker at 12 o'clock. The wheel spins beneath it. */
private fun DrawScope.drawPointer() {
    val centerX = size.width / 2f
    val radius = min(size.width, size.height) / 2f - 8f
    val top = size.height / 2f - radius
    val width = radius * 0.09f
    val height = radius * 0.16f

    val path = Path().apply {
        moveTo(centerX, top + height)
        lineTo(centerX - width, top - height * 0.35f)
        lineTo(centerX + width, top - height * 0.35f)
        close()
    }

    drawPath(path, color = Color(0xFF263238))
    drawPath(path, color = Color.White, style = Stroke(width = 4f))
}
