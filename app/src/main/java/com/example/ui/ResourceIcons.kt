package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.ResourceCost
import com.example.game.model.ResourceType
import com.example.ui.theme.EmberRed
import com.example.ui.theme.ParchmentCream

/**
 * Custom hand-painted medieval glyphs for Wood, Food, Gold, and Stone so resources
 * are immediately recognizable at a glance in both the top bar and build/train cards.
 */
@Composable
fun ResourceGlyph(
  type: ResourceType,
  modifier: Modifier = Modifier.size(16.dp),
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    when (type) {
      ResourceType.WOOD -> {
        // Stacked split timber logs with bark shading and pale cut end-grain rings
        fun drawLog(cx: Float, cy: Float, rx: Float, ry: Float, len: Float) {
          val logBody = Path().apply {
            moveTo(cx - len * 0.45f, cy + len * 0.25f)
            lineTo(cx + len * 0.35f, cy - len * 0.22f)
            lineTo(cx + len * 0.35f + rx, cy - len * 0.22f + ry * 1.4f)
            lineTo(cx - len * 0.45f + rx, cy + len * 0.25f + ry * 1.4f)
            close()
          }
          drawPath(
            logBody,
            brush = Brush.verticalGradient(listOf(Color(0xFF8C5D34), Color(0xFF472A14))),
          )
          drawPath(logBody, color = Color(0x661C0F06), style = Stroke(width = w * 0.035f))
          // Cut cross-section disc
          drawOval(
            color = Color(0xFFD6B57E),
            topLeft = Offset(cx - len * 0.48f, cy + len * 0.14f),
            size = Size(rx * 1.4f, ry * 1.55f),
          )
          drawOval(
            color = Color(0x995A3619),
            topLeft = Offset(cx - len * 0.48f, cy + len * 0.14f),
            size = Size(rx * 1.4f, ry * 1.55f),
            style = Stroke(width = w * 0.035f),
          )
          drawCircle(
            color = Color(0xFFA37644),
            radius = rx * 0.28f,
            center = Offset(cx - len * 0.48f + rx * 0.7f, cy + len * 0.14f + ry * 0.78f),
          )
        }
        drawLog(w * 0.56f, h * 0.50f, w * 0.20f, h * 0.18f, w * 0.52f)
        drawLog(w * 0.36f, h * 0.56f, w * 0.20f, h * 0.18f, w * 0.50f)
        drawLog(w * 0.46f, h * 0.33f, w * 0.20f, h * 0.18f, w * 0.50f)
      }

      ResourceType.FOOD -> {
        // Golden wheat sheaf behind a natural crimson orchard apple with leaf
        val stalkColor = Color(0xFFCFA240)
        for (ox in listOf(-0.26f, -0.14f, -0.02f)) {
          drawLine(
            color = stalkColor,
            start = Offset(w * (0.42f + ox), h * 0.85f),
            end = Offset(w * (0.28f + ox * 0.7f), h * 0.20f),
            strokeWidth = w * 0.075f,
            cap = StrokeCap.Round,
          )
          drawOval(
            color = Color(0xFFEBD07A),
            topLeft = Offset(w * (0.20f + ox * 0.7f), h * 0.14f),
            size = Size(w * 0.16f, h * 0.22f),
          )
        }
        // Ripe orchard apple
        drawOval(
          brush = Brush.radialGradient(
            colors = listOf(Color(0xFFDF5C52), Color(0xFFA8231E), Color(0xFF5C100D)),
            center = Offset(w * 0.56f, h * 0.48f),
            radius = w * 0.38f,
          ),
          topLeft = Offset(w * 0.36f, h * 0.36f),
          size = Size(w * 0.54f, h * 0.54f),
        )
        drawOval(
          color = Color(0x55360906),
          topLeft = Offset(w * 0.36f, h * 0.36f),
          size = Size(w * 0.54f, h * 0.54f),
          style = Stroke(width = w * 0.035f),
        )
        drawCircle(
          color = Color(0xBBFFF8F0),
          radius = w * 0.055f,
          center = Offset(w * 0.52f, h * 0.50f),
        )
        drawLine(
          color = Color(0xFF4F3119),
          start = Offset(w * 0.63f, h * 0.38f),
          end = Offset(w * 0.67f, h * 0.24f),
          strokeWidth = w * 0.07f,
          cap = StrokeCap.Round,
        )
        val leaf = Path().apply {
          moveTo(w * 0.65f, h * 0.31f)
          quadraticTo(w * 0.85f, h * 0.16f, w * 0.88f, h * 0.30f)
          quadraticTo(w * 0.76f, h * 0.38f, w * 0.65f, h * 0.31f)
          close()
        }
        drawPath(leaf, color = Color(0xFF4E7834))
      }

      ResourceType.GOLD -> {
        // Hammered stacked sovereign gold coins
        fun drawCoin(cx: Float, cy: Float, r: Float) {
          drawOval(
            color = Color(0xFF6E4A10),
            topLeft = Offset(cx - r, cy - r * 0.68f + h * 0.08f),
            size = Size(r * 2f, r * 1.36f),
          )
          drawOval(
            brush = Brush.linearGradient(
              colors = listOf(Color(0xFFF5DF88), Color(0xFFD4A22E), Color(0xFF8C5F12)),
              start = Offset(cx - r, cy - r),
              end = Offset(cx + r, cy + r),
            ),
            topLeft = Offset(cx - r, cy - r * 0.68f),
            size = Size(r * 2f, r * 1.36f),
          )
          drawOval(
            color = Color(0x773E2705),
            topLeft = Offset(cx - r, cy - r * 0.68f),
            size = Size(r * 2f, r * 1.36f),
            style = Stroke(width = w * 0.035f),
          )
          drawOval(
            color = Color(0x99FFF3BA),
            topLeft = Offset(cx - r * 0.56f, cy - r * 0.36f),
            size = Size(r * 1.12f, r * 0.72f),
            style = Stroke(width = w * 0.038f),
          )
        }
        drawCoin(w * 0.38f, h * 0.64f, w * 0.27f)
        drawCoin(w * 0.64f, h * 0.62f, w * 0.26f)
        drawCoin(w * 0.50f, h * 0.42f, w * 0.28f)
        drawCoin(w * 0.50f, h * 0.28f, w * 0.27f)
      }

      ResourceType.STONE -> {
        // Chiseled interlocking ashlar granite blocks with natural sunlit bevels
        fun drawStoneBlock(left: Float, top: Float, bw: Float, bh: Float, hi: Color, lo: Color) {
          drawRoundRect(
            brush = Brush.linearGradient(listOf(hi, lo), Offset(left, top), Offset(left + bw, top + bh)),
            topLeft = Offset(left, top),
            size = Size(bw, bh),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
          )
          drawLine(
            color = Color(0x66FFFFFF),
            start = Offset(left + w * 0.05f, top + h * 0.04f),
            end = Offset(left + bw - w * 0.05f, top + h * 0.04f),
            strokeWidth = w * 0.04f,
          )
          drawRoundRect(
            color = Color(0x66282D32),
            topLeft = Offset(left, top),
            size = Size(bw, bh),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
            style = Stroke(width = w * 0.035f),
          )
        }
        drawStoneBlock(w * 0.10f, h * 0.52f, w * 0.42f, h * 0.34f, Color(0xFFB3BAC0), Color(0xFF666E75))
        drawStoneBlock(w * 0.49f, h * 0.52f, w * 0.41f, h * 0.34f, Color(0xFF9CA3AA), Color(0xFF555C63))
        drawStoneBlock(w * 0.26f, h * 0.18f, w * 0.50f, h * 0.35f, Color(0xFFCED4D9), Color(0xFF7C848B))
      }
    }
  }
}

/**
 * Compact cost badge strip using real ResourceGlyph icons (e.g. [WoodIcon] 25  [StoneIcon] 100).
 */
@Composable
fun ResourceCostBadges(
  cost: ResourceCost,
  affordable: Boolean,
  iconSize: Dp = 11.dp,
  modifier: Modifier = Modifier,
) {
  val items = buildList {
    if (cost.wood > 0) add(ResourceType.WOOD to cost.wood)
    if (cost.food > 0) add(ResourceType.FOOD to cost.food)
    if (cost.gold > 0) add(ResourceType.GOLD to cost.gold)
    if (cost.stone > 0) add(ResourceType.STONE to cost.stone)
  }
  val textCol = if (affordable) ParchmentCream else EmberRed.copy(alpha = 0.92f)

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    items.forEach { (resType, amount) ->
      Row(verticalAlignment = Alignment.CenterVertically) {
        ResourceGlyph(type = resType, modifier = Modifier.size(iconSize))
        Spacer(modifier = Modifier.width(2.dp))
        Text(
          text = amount.toString(),
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Normal),
          color = textCol,
        )
      }
    }
  }
}
