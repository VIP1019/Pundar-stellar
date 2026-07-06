package com.example.pundarapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.pundarapp.ui.theme.*

@Composable
fun PundarLineChart(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = PundarChartLine,
    gradientStartColor: Color = PundarChartGradientStart,
    gradientEndColor: Color = PundarChartGradientEnd,
    lineWidth: Float = 3f
) {
    if (dataPoints.size < 2) return

    Canvas(modifier = modifier.fillMaxWidth().height(160.dp)) {
        val width = size.width
        val height = size.height
        val padding = 8f

        val minValue = dataPoints.min()
        val maxValue = dataPoints.max()
        val valueRange = if (maxValue - minValue > 0) maxValue - minValue else 1f

        val stepX = (width - padding * 2) / (dataPoints.size - 1)

        // Build the line path
        val linePath = Path()
        val fillPath = Path()

        dataPoints.forEachIndexed { index, value ->
            val x = padding + index * stepX
            val y = height - padding - ((value - minValue) / valueRange) * (height - padding * 2)

            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, y)
            } else {
                // Smooth curve using cubic bezier
                val prevX = padding + (index - 1) * stepX
                val prevValue = dataPoints[index - 1]
                val prevY = height - padding - ((prevValue - minValue) / valueRange) * (height - padding * 2)

                val controlX1 = prevX + stepX * 0.4f
                val controlX2 = x - stepX * 0.4f

                linePath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                fillPath.cubicTo(controlX1, prevY, controlX2, y, x, y)
            }
        }

        // Close fill path
        fillPath.lineTo(padding + (dataPoints.size - 1) * stepX, height)
        fillPath.lineTo(padding, height)
        fillPath.close()

        // Draw gradient fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(gradientStartColor, gradientEndColor),
                startY = 0f,
                endY = height
            )
        )

        // Draw line
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(
                width = lineWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Draw end dot
        val lastX = padding + (dataPoints.size - 1) * stepX
        val lastY = height - padding - ((dataPoints.last() - minValue) / valueRange) * (height - padding * 2)
        drawCircle(
            color = lineColor,
            radius = 5f,
            center = Offset(lastX, lastY)
        )
        drawCircle(
            color = Color.White,
            radius = 3f,
            center = Offset(lastX, lastY)
        )
    }
}

@Composable
fun PundarMiniChart(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = PundarChartLine
) {
    if (dataPoints.size < 2) return

    Canvas(modifier = modifier.fillMaxWidth().height(40.dp)) {
        val width = size.width
        val height = size.height

        val minValue = dataPoints.min()
        val maxValue = dataPoints.max()
        val valueRange = if (maxValue - minValue > 0) maxValue - minValue else 1f

        val stepX = width / (dataPoints.size - 1)

        val path = Path()
        dataPoints.forEachIndexed { index, value ->
            val x = index * stepX
            val y = height - ((value - minValue) / valueRange) * height

            if (index == 0) path.moveTo(x, y)
            else {
                val prevX = (index - 1) * stepX
                val prevValue = dataPoints[index - 1]
                val prevY = height - ((prevValue - minValue) / valueRange) * height
                val cx1 = prevX + stepX * 0.4f
                val cx2 = x - stepX * 0.4f
                path.cubicTo(cx1, prevY, cx2, y, x, y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun PundarDonutChart(
    percentage: Float,
    modifier: Modifier = Modifier,
    color: Color = PundarBlue,
    backgroundColor: Color = PundarSurfaceVariant,
    strokeWidth: Float = 12f
) {
    Canvas(modifier = modifier.size(80.dp)) {
        val sweepAngle = 360f * (percentage / 100f)

        // Background arc
        drawArc(
            color = backgroundColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Foreground arc
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}
