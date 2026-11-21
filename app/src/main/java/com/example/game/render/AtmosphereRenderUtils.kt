package com.example.game.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Shared atmospheric, radial-gradient shadow, smoke, and soft-light helpers
 * that eliminate hard-edged geometric ovals and lend organic, lifelike depth.
 */
object AtmosphereRenderUtils {

  fun DrawScope.drawSoftEllipticalShadow(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    coreAlpha: Float = 0.50f,
    tint: Color = Color(0xFF060402),
  ) {
    if (rx <= 0.5f || ry <= 0.5f) return
    val scaleY = (ry / rx).coerceIn(0.15f, 2.5f)
    withTransform({
      scale(scaleX = 1f, scaleY = scaleY, pivot = Offset(cx, cy))
    }) {
      drawCircle(
        brush =
          Brush.radialGradient(
            0.0f to tint.copy(alpha = coreAlpha),
            0.42f to tint.copy(alpha = coreAlpha * 0.58f),
            0.74f to tint.copy(alpha = coreAlpha * 0.18f),
            1.0f to Color.Transparent,
            center = Offset(cx, cy),
            radius = rx,
          ),
        radius = rx,
        center = Offset(cx, cy),
      )
    }
  }

  fun DrawScope.drawSoftRadialOval(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    centerColor: Color,
    midColor: Color = centerColor.copy(alpha = centerColor.alpha * 0.42f),
    edgeColor: Color = Color.Transparent,
  ) {
    if (rx <= 0.5f || ry <= 0.5f) return
    val scaleY = (ry / rx).coerceIn(0.15f, 2.5f)
    withTransform({
      scale(scaleX = 1f, scaleY = scaleY, pivot = Offset(cx, cy))
    }) {
      drawCircle(
        brush =
          Brush.radialGradient(
            0.0f to centerColor,
            0.55f to midColor,
            1.0f to edgeColor,
            center = Offset(cx, cy),
            radius = rx,
          ),
        radius = rx,
        center = Offset(cx, cy),
      )
    }
  }

  fun DrawScope.drawSoftVolumetricPuff(
    cx: Float,
    cy: Float,
    radius: Float,
    color: Color,
    alpha: Float,
  ) {
    if (radius <= 0.5f || alpha <= 0.005f) return
    drawCircle(
      brush =
        Brush.radialGradient(
          0.0f to color.copy(alpha = alpha),
          0.48f to color.copy(alpha = alpha * 0.62f),
          0.82f to color.copy(alpha = alpha * 0.18f),
          1.0f to Color.Transparent,
          center = Offset(cx, cy),
          radius = radius,
        ),
      radius = radius,
      center = Offset(cx, cy),
    )
  }
}
