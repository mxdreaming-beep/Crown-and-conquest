package com.example.game.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.game.model.CameraState
import com.example.game.model.ResourceNode
import com.example.game.model.ResourceNodeType
import com.example.game.render.AtmosphereRenderUtils.drawSoftEllipticalShadow
import com.example.game.render.AtmosphereRenderUtils.drawSoftRadialOval
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

object ResourceRenderer {

  private val ExploredSpriteTint =
    ColorFilter.colorMatrix(
      ColorMatrix().apply {
        setToScale(0.78f, 0.80f, 0.86f, 1.0f)
      }
    )

  fun DrawScope.drawResourceNode(
    node: ResourceNode,
    camera: CameraState,
    isDimmed: Boolean,
    animClock: Float,
  ) {
    val c = IsometricMath.worldToScreen(node.x, node.y, camera, size.width, size.height)
    val z = camera.zoom
    val sprite = SpriteAtlas.getResourceSprite(node.type, node.variant)
    val dstW = (136f * z).toInt().coerceAtLeast(1)
    val dstH = (152f * z).toInt().coerceAtLeast(1)
    val isTree = node.type == ResourceNodeType.FOREST_OAK || node.type == ResourceNodeType.FOREST_PINE
    val swayPx = if (isTree) (sin(animClock * 1.6f + node.id) * 1.6f * z).toInt() else 0

    // Feathered directional golden-hour cast shadow + crisp soft contact AO beneath node
    val shW = if (isTree) 68f * z else 48f * z
    if (isTree) {
      drawSoftRadialOval(
        cx = c.x,
        cy = c.y + 2f * z,
        rx = shW * 0.56f,
        ry = shW * 0.22f,
        centerColor = Color(0x32253115).copy(alpha = if (isDimmed) 0.14f else 0.24f),
      )
    }
    drawSoftEllipticalShadow(
      cx = c.x + shW * 0.26f + swayPx * 0.45f,
      cy = c.y + shW * 0.06f,
      rx = shW * 0.62f,
      ry = shW * 0.24f,
      coreAlpha = if (isDimmed) 0.34f else 0.42f,
    )
    drawSoftEllipticalShadow(
      cx = c.x + shW * 0.14f,
      cy = c.y + shW * 0.02f,
      rx = shW * 0.44f,
      ry = shW * 0.18f,
      coreAlpha = if (isDimmed) 0.44f else 0.54f,
    )

    drawImage(
      image = sprite,
      dstOffset = IntOffset((c.x - dstW * 0.5f).toInt() + swayPx, (c.y - dstH * 0.80f).toInt()),
      dstSize = IntSize(dstW, dstH),
      alpha = 1.0f,
      colorFilter = if (isDimmed) ExploredSpriteTint else null,
      filterQuality = FilterQuality.High,
    )

    val detailAlpha = if (isDimmed) 0.55f else 1.0f

    if (isTree) {
      // Feathered sunlit canopy dapple shimmer in breeze
      val dappleAlpha = (0.18f + 0.08f * sin(animClock * 1.9f + node.id * 0.7f)) * detailAlpha
      drawSoftRadialOval(
        cx = c.x - 11f * z + swayPx,
        cy = c.y - 77f * z,
        rx = 16f * z,
        ry = 9f * z,
        centerColor = Color(0xFFFFEBA8).copy(alpha = dappleAlpha),
      )
      drawSoftRadialOval(
        cx = c.x + 1f * z + swayPx,
        cy = c.y - 87f * z,
        rx = 10f * z,
        ry = 6f * z,
        centerColor = Color(0xFFFFF4C8).copy(alpha = dappleAlpha * 0.8f),
      )
      if (node.type == ResourceNodeType.FOREST_OAK && (node.id % 3 == 0)) {
        val ph = ((animClock * 0.34f + node.id * 0.29f) % 1f)
        val lx = c.x + swayPx + sin(animClock * 3.1f + node.id) * 11f * z + ph * 8f * z
        val ly = c.y - 68f * z + ph * 64f * z
        val lAlpha = (1f - kotlin.math.abs(ph * 2f - 1f)) * 0.78f * detailAlpha
        val leafCol = if (node.variant == 2) Color(0xFFC78B36) else Color(0xFF7B9442)
        drawOval(leafCol.copy(alpha = lAlpha), Offset(lx - 1.8f * z, ly - 1.0f * z), Size(3.6f * z, 2.0f * z))
      }
    }

    if (node.type == ResourceNodeType.BERRY_BUSH && (node.id % 2 == 0)) {
      // Tiny sunlit butterfly fluttering gently above ripe berry bush
      val bx = c.x + cos(animClock * 2.1f + node.id) * 14f * z
      val by = c.y - 34f * z + sin(animClock * 3.4f + node.id) * 6f * z
      val flap = 1.4f + kotlin.math.abs(sin(animClock * 14f + node.id)) * 2.1f
      val wingCol = (if (node.id % 4 == 0) Color(0xFFE8C458) else Color(0xFFF2ECE0)).copy(alpha = detailAlpha)
      drawOval(wingCol, Offset(bx - flap * z, by - 1.7f * z), Size(flap * z, 2.8f * z))
      drawOval(wingCol, Offset(bx, by - 1.7f * z), Size(flap * z, 2.8f * z))
      drawCircle(Color(0xFF281B10).copy(alpha = detailAlpha), 0.7f * z, Offset(bx, by - 0.3f * z))
    }

    if (node.type == ResourceNodeType.GOLD_MINE) {
      val glint = max(0f, sin(animClock * 2.6f + node.id * 1.7f) - 0.50f) / 0.50f
      if (glint > 0.01f) {
        drawSoftRadialOval(c.x - 10f * z, c.y - 24f * z, (4.2f + 2.2f * glint) * z, (4.2f + 2.2f * glint) * z, Color(0xFFFFF7D1).copy(alpha = 0.72f * glint * detailAlpha))
        drawSoftRadialOval(c.x + 11f * z, c.y - 16f * z, (3.4f + 1.8f * glint) * z, (3.4f + 1.8f * glint) * z, Color(0xFFFFF4C2).copy(alpha = 0.58f * glint * detailAlpha))
      }
    }
  }
}
