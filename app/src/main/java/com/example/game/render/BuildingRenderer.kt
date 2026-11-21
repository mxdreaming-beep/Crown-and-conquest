package com.example.game.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.game.model.Building
import com.example.game.model.BuildingType
import com.example.game.model.CameraState
import com.example.game.model.Faction
import com.example.game.render.AtmosphereRenderUtils.drawSoftEllipticalShadow
import com.example.game.render.AtmosphereRenderUtils.drawSoftRadialOval
import com.example.game.render.AtmosphereRenderUtils.drawSoftVolumetricPuff
import kotlin.math.cos
import kotlin.math.sin

object BuildingRenderer {

  private val TimberLight = Color(0xFF966236)
  private val TimberDark = Color(0xFF4B2B13)
  private val ExploredBuildingTint =
    ColorFilter.colorMatrix(
      ColorMatrix().apply {
        setToScale(0.78f, 0.80f, 0.86f, 1.0f)
      }
    )

  fun DrawScope.drawBuilding(
    b: Building,
    camera: CameraState,
    isSelected: Boolean,
    isDimmed: Boolean,
    animClock: Float,
  ) {
    val w = size.width
    val h = size.height
    val z = camera.zoom
    val factionColor = Color(b.faction.bannerHex)

    val pTop = IsometricMath.worldToScreen(b.tileX.toFloat(), b.tileY.toFloat(), camera, w, h)
    val pRight = IsometricMath.worldToScreen((b.tileX + b.footprint).toFloat(), b.tileY.toFloat(), camera, w, h)
    val pBottom = IsometricMath.worldToScreen((b.tileX + b.footprint).toFloat(), (b.tileY + b.footprint).toFloat(), camera, w, h)
    val pLeft = IsometricMath.worldToScreen(b.tileX.toFloat(), (b.tileY + b.footprint).toFloat(), camera, w, h)
    val center = Offset((pTop.x + pBottom.x) * 0.5f, (pTop.y + pBottom.y) * 0.5f)

    if (isSelected) {
      val pulse = 0.78f + 0.18f * sin(animClock * 4.5f)
      val selPath = Path().apply {
        moveTo(pTop.x, pTop.y); lineTo(pRight.x, pRight.y); lineTo(pBottom.x, pBottom.y); lineTo(pLeft.x, pLeft.y); close()
      }
      drawPath(selPath, Color(0x287CB852))
      drawPath(selPath, Color(0xFF95CF63).copy(alpha = pulse), style = Stroke(width = 2.6f * z))
      for (pt in listOf(pTop, pRight, pBottom, pLeft)) {
        drawCircle(Color(0xFFE0C26E), 2.8f * z, pt)
      }
    }

    if (b.type == BuildingType.FARM) {
      drawFarm(b, pTop, pRight, pBottom, pLeft, center, z, animClock, isDimmed)
      if (isSelected || b.hp < b.maxHp) {
        drawHpBar(center.x, pTop.y - 14f * z, 68f * z, b.hp, b.maxHp, z)
      }
      if (isDimmed) drawDiamondOverlay(pTop, pRight, pBottom, pLeft, Color(0x34100F18))
      return
    }

    if (!b.isComplete) {
      val rise = (24f + 58f * b.buildProgress) * z
      drawConstructionScaffold(pTop, pRight, pBottom, pLeft, rise, b.buildProgress, z)
      drawHpBar(center.x, pTop.y - rise - 12f * z, 70f * z, (b.buildProgress * 100).toInt(), 100, z, Color(0xFFF2C964))
      if (isDimmed) drawDiamondOverlay(pTop, pRight, pBottom, pLeft, Color(0x34100F18))
      return
    }

    val sprite = BuildingSpriteAtlas.getBuildingSprite(b.type, b.faction)
    val scaleInv = 1f / BuildingSpriteAtlas.SUPERSAMPLE
    val dstW = (sprite.width * scaleInv * z).toInt().coerceAtLeast(1)
    val dstH = (sprite.height * scaleInv * z).toInt().coerceAtLeast(1)
    val anchorFromBottom = (b.footprint * 37f + 18f) * z
    val dstLeft = (center.x - dstW * 0.5f).toInt()
    val dstTop = (center.y - (dstH - anchorFromBottom)).toInt()

    // Feathered earthen courtyard apron + golden-hour Gaussian cast shadow & contact AO
    val shadowSpanW = (b.footprint * 54f + 26f) * z
    val shadowSpanH = (b.footprint * 27f + 14f) * z
    drawSoftRadialOval(
      cx = center.x,
      cy = center.y + 3f * z,
      rx = shadowSpanW * 0.64f,
      ry = shadowSpanH * 0.62f,
      centerColor = Color(0x30291D12),
    )
    drawSoftEllipticalShadow(
      cx = center.x + 12f * z,
      cy = center.y + 5f * z,
      rx = shadowSpanW * 0.60f,
      ry = shadowSpanH * 0.56f,
      coreAlpha = 0.44f,
    )
    drawSoftEllipticalShadow(
      cx = center.x + 3f * z,
      cy = center.y + 2f * z,
      rx = shadowSpanW * 0.45f,
      ry = shadowSpanH * 0.42f,
      coreAlpha = 0.56f,
    )

    drawImage(
      image = sprite,
      dstOffset = IntOffset(dstLeft, dstTop),
      dstSize = IntSize(dstW, dstH),
      alpha = 1.0f,
      colorFilter = if (isDimmed) ExploredBuildingTint else null,
      filterQuality = FilterQuality.High,
    )

    when (b.type) {
      BuildingType.MILL -> {
        val hub = Offset(center.x, center.y - 68f * z)
        val sailLen = 49f * z
        for (k in 0..3) {
          val ang = b.animPhase + k * (Math.PI.toFloat() * 0.5f)
          val ex = hub.x + cos(ang) * sailLen
          val ey = hub.y + sin(ang) * sailLen
          drawLine(TimberDark, hub, Offset(ex, ey), strokeWidth = 4.6f * z)
          val perpX = -sin(ang) * 10f * z
          val perpY = cos(ang) * 10f * z
          val sStart = Offset(hub.x + cos(ang) * 11f * z, hub.y + sin(ang) * 11f * z)
          val sail = Path().apply {
            moveTo(sStart.x, sStart.y); lineTo(ex, ey); lineTo(ex + perpX, ey + perpY); lineTo(sStart.x + perpX, sStart.y + perpY); close()
          }
          drawPath(sail, Color(0xFFDED4BE))
          drawPath(sail, TimberDark, style = Stroke(width = 1.2f * z))
          for (step in 1..4) {
            val t = step / 5f
            val rx = sStart.x + (ex - sStart.x) * t
            val ry = sStart.y + (ey - sStart.y) * t
            drawLine(TimberLight, Offset(rx, ry), Offset(rx + perpX, ry + perpY), strokeWidth = 1.0f * z)
          }
        }
        drawCircle(TimberDark, 5.6f * z, hub)
        drawCircle(Color(0xFFC29E48), 2.8f * z, hub)
      }
      BuildingType.TOWN_CENTER -> {
        drawChimneySmoke(center.x + 36f * z, center.y - 101f * z, z, animClock)
        drawTorchPool(center.x - 16f * z, center.y - 14f * z, 18f * z, animClock + b.id)
        drawTorchPool(center.x + 22f * z, center.y - 12f * z, 15f * z, animClock + b.id + 1.5f)
        drawBannerFlag(center.x, center.y - 178f * z, z, factionColor, animClock)
      }
      BuildingType.HOUSE -> {
        drawChimneySmoke(center.x - 50f * z, center.y - 94f * z, z, animClock + b.id)
        drawTorchPool(center.x + 12f * z, center.y - 16f * z, 12f * z, animClock + b.id)
      }
      BuildingType.BARRACKS -> {
        drawTorchPool(center.x - 20f * z, center.y - 12f * z, 14f * z, animClock + b.id)
        drawBannerFlag(center.x, center.y - 118f * z, z, factionColor, animClock)
      }
      BuildingType.BLACKSMITH -> {
        drawChimneySmoke(center.x + 16f * z, center.y - 127f * z, z, animClock)
        val forgePulse = 0.55f + 0.35f * sin(animClock * 9f + b.id)
        drawSoftVolumetricPuff(center.x - 28f * z, center.y - 12f * z, 22f * z, Color(0xFFD96414), forgePulse * 0.48f)
        drawSoftVolumetricPuff(center.x - 28f * z, center.y - 12f * z, 9f * z, Color(0xFFF5CA5E), forgePulse * 0.78f)
        for (sp in 0..2) {
          val ph = ((animClock * 1.4f + sp * 0.33f + b.id) % 1f)
          drawCircle(
            Color(0xFFFFB240).copy(alpha = (1f - ph) * 0.85f),
            1.6f * z,
            Offset(center.x + 16f * z + sin(animClock * 5f + sp) * 6f * z, center.y - 130f * z - ph * 24f * z),
          )
        }
      }
      BuildingType.WATCH_TOWER -> {
        drawTorchPool(center.x, center.y - 18f * z, 13f * z, animClock + b.id)
        drawBannerFlag(center.x, center.y - 154f * z, z, factionColor, animClock)
      }
      BuildingType.CASTLE -> {
        drawTorchPool(center.x - 28f * z, center.y - 16f * z, 20f * z, animClock + b.id)
        drawTorchPool(center.x + 28f * z, center.y - 16f * z, 20f * z, animClock + b.id + 2.1f)
        drawBannerFlag(center.x, center.y - 214f * z, z, factionColor, animClock)
        drawBannerFlag(center.x - 42f * z, center.y - 170f * z, z * 0.78f, factionColor, animClock + 1.2f)
      }
      BuildingType.WAR_FACTORY -> {
        drawChimneySmoke(center.x - 34f * z, center.y - 134f * z, z * 1.15f, animClock)
        drawChimneySmoke(center.x + 22f * z, center.y - 130f * z, z * 1.1f, animClock + 0.7f)
        if (b.queue.isNotEmpty()) {
          val weldPulse = sin(animClock * 21f + b.id)
          if (weldPulse > 0.35f) {
            drawSoftVolumetricPuff(center.x - 34f * z, center.y - 24f * z, 15f * z, Color(0xFF8CE3FF), 0.78f)
            drawCircle(Color(0xEEFFFFFF), 3.2f * z, Offset(center.x - 34f * z, center.y - 24f * z))
          }
        }
      }
      BuildingType.MINING_CAMP -> {
        drawTorchPool(center.x - 16f * z, center.y - 14f * z, 13f * z, animClock + b.id)
        drawTorchPool(center.x + 18f * z, center.y - 10f * z, 11f * z, animClock + b.id + 1.3f)
      }
      BuildingType.UNIVERSITY -> {
        drawTorchPool(center.x - 24f * z, center.y - 18f * z, 16f * z, animClock + b.id)
        drawTorchPool(center.x + 2f * z, center.y - 14f * z, 14f * z, animClock + b.id + 1.1f)
        drawBannerFlag(center.x - 42f * z, center.y - 142f * z, z * 0.82f, factionColor, animClock + 0.5f)
        drawBannerFlag(center.x + 10f * z, center.y - 166f * z, z * 0.88f, factionColor, animClock + 1.4f)
        val glint = 0.45f + 0.45f * sin(animClock * 2.8f + b.id)
        drawCircle(
          Color(0xFFFFF0A8).copy(alpha = glint),
          3.0f * z,
          Offset(center.x + 37f * z, center.y - 134f * z),
        )
      }
      BuildingType.AIRFIELD -> {
        // Rotating ATC radar dish sweep & soft glowing runway approach beacons
        val dishCenter = Offset(center.x + 48f * z, center.y - 146f * z)
        val sweep = sin(animClock * 3.4f + b.id)
        drawLine(
          color = Color(0xFFD9E2EA),
          start = Offset(dishCenter.x - 10f * z * sweep, dishCenter.y),
          end = Offset(dishCenter.x + 10f * z * sweep, dishCenter.y),
          strokeWidth = 3.2f * z,
        )
        drawCircle(Color(0xFF9EA8B0), 2.8f * z, dishCenter)
        drawSoftRadialOval(dishCenter.x + 6f * z * sweep, dishCenter.y - 2f * z, 6f * z, 4f * z, Color(0x366CE894))
        val blink = sin(animClock * 5f + b.id) > 0f
        if (blink) {
          drawSoftRadialOval(pLeft.x + 26f * z, pLeft.y + 4f * z, 9f * z, 5f * z, Color(0x525CE67A))
          drawCircle(Color(0xEE80FA9A), 2.6f * z, Offset(pLeft.x + 26f * z, pLeft.y + 4f * z))
          drawSoftRadialOval(pRight.x - 24f * z, pRight.y + 4f * z, 9f * z, 5f * z, Color(0x52FFAE38))
          drawCircle(Color(0xEEFFD070), 2.6f * z, Offset(pRight.x - 24f * z, pRight.y + 4f * z))
        }
      }
      else -> Unit
    }

    if (b.hp < b.maxHp * 0.55f) {
      val flicker = sin(animClock * 11f + b.id) * 5f * z
      drawSoftVolumetricPuff(center.x + flicker * 0.4f, center.y - 54f * z, 18f * z, Color(0xFF2B241E), 0.52f)
      drawSoftVolumetricPuff(center.x + flicker, center.y - 36f * z, 16f * z, Color(0xFFFF6A1A), 0.78f)
      drawSoftVolumetricPuff(center.x - flicker * 0.5f, center.y - 42f * z, 9f * z, Color(0xFFFFD045), 0.85f)
    }

    if (isSelected || b.hp < b.maxHp) {
      val topOffset =
        when (b.type) {
          BuildingType.CASTLE, BuildingType.WATCH_TOWER -> 136f * z
          BuildingType.UNIVERSITY -> 128f * z
          BuildingType.WAR_FACTORY, BuildingType.AIRFIELD -> 114f * z
          else -> 84f * z
        }
      drawHpBar(center.x, pTop.y - topOffset, 76f * z, b.hp, b.maxHp, z)
    }
  }

  private fun DrawScope.drawConstructionScaffold(
    pTop: Offset,
    pRight: Offset,
    pBottom: Offset,
    pLeft: Offset,
    rise: Float,
    progress: Float,
    z: Float,
  ) {
    // Earthen foundation footprint + rising stone interior core
    val basePath = Path().apply { moveTo(pTop.x, pTop.y); lineTo(pRight.x, pRight.y); lineTo(pBottom.x, pBottom.y); lineTo(pLeft.x, pLeft.y); close() }
    drawPath(basePath, Color(0xFF5D462E))
    drawPath(basePath, Color(0xFF3A2715), style = Stroke(width = 2.5f * z))

    val stoneRise = rise * 0.72f * progress
    if (stoneRise > 3f * z) {
      val sL = Path().apply { moveTo(pLeft.x, pLeft.y); lineTo(pBottom.x, pBottom.y); lineTo(pBottom.x, pBottom.y - stoneRise); lineTo(pLeft.x, pLeft.y - stoneRise); close() }
      val sR = Path().apply { moveTo(pBottom.x, pBottom.y); lineTo(pRight.x, pRight.y); lineTo(pRight.x, pRight.y - stoneRise); lineTo(pBottom.x, pBottom.y - stoneRise); close() }
      drawPath(sL, Color(0xFF9C9182))
      drawPath(sR, Color(0xFF6B6256))
    }

    val tTop = Offset(pTop.x, pTop.y - rise)
    val tRight = Offset(pRight.x, pRight.y - rise)
    val tBottom = Offset(pBottom.x, pBottom.y - rise)
    val tLeft = Offset(pLeft.x, pLeft.y - rise)
    val topDeck = Path().apply { moveTo(tTop.x, tTop.y); lineTo(tRight.x, tRight.y); lineTo(tBottom.x, tBottom.y); lineTo(tLeft.x, tLeft.y); close() }
    drawPath(topDeck, Color(0xCC9C7248))
    drawPath(topDeck, TimberDark, style = Stroke(width = 2.0f * z))

    // Vertical scaffolding posts & diagonal cross-braces
    for ((bot, top) in listOf(pTop to tTop, pRight to tRight, pBottom to tBottom, pLeft to tLeft)) {
      drawLine(TimberDark, bot, top, 3.4f * z)
    }
    drawLine(TimberLight, pLeft, tBottom, 2.2f * z)
    drawLine(TimberLight, pBottom, tLeft, 2.2f * z)
    drawLine(TimberLight, pBottom, tRight, 2.2f * z)
    drawLine(TimberLight, pRight, tBottom, 2.2f * z)
    // Stacked timber planks by worksite edge
    drawLine(Color(0xFF7D5029), Offset(pBottom.x - 18f * z, pBottom.y - 3f * z), Offset(pBottom.x + 10f * z, pBottom.y - 16f * z), 3.4f * z)
  }

  private fun DrawScope.drawFarm(
    b: Building,
    pTop: Offset,
    pRight: Offset,
    pBottom: Offset,
    pLeft: Offset,
    center: Offset,
    z: Float,
    animClock: Float,
    isDimmed: Boolean,
  ) {
    if (!b.isComplete) {
      val soil = Path().apply { moveTo(pTop.x, pTop.y); lineTo(pRight.x, pRight.y); lineTo(pBottom.x, pBottom.y); lineTo(pLeft.x, pLeft.y); close() }
      drawPath(soil, Color(0xFF5D3F20))
      drawPath(soil, Color(0xFF3E2712), style = Stroke(width = 3.0f * z))
      for (i in -3..3) {
        val ox = i * 9f * z
        val oy = i * 4.5f * z
        val sx = center.x - 32f * z + ox
        val sy = center.y + 16f * z - oy
        val ex = center.x + 32f * z + ox
        val ey = center.y - 16f * z - oy
        drawLine(Color(0xFF3D2713), Offset(sx, sy), Offset(ex, ey), strokeWidth = 3.0f * z)
        if (b.buildProgress > 0.35f) {
          for (k in 0..4) {
            val f = 0.12f + k * 0.19f
            val bx = sx + (ex - sx) * f
            val by = sy + (ey - sy) * f
            drawLine(Color(0xFF7CA33B), Offset(bx, by), Offset(bx + 1f * z, by - (6.5f * b.buildProgress) * z), strokeWidth = 1.9f * z)
          }
        }
      }
      for (pt in listOf(pTop, pRight, pBottom, pLeft)) {
        drawRect(TimberDark, Offset(pt.x - 2.8f * z, pt.y - 11f * z), Size(5.6f * z, 11f * z))
      }
      drawHpBar(center.x, pTop.y - 10f * z, 68f * z, (b.buildProgress * 100).toInt(), 100, z, Color(0xFFF2C964))
      return
    }

    val sprite = BuildingSpriteAtlas.getBuildingSprite(BuildingType.FARM, b.faction)
    val scaleInv = 1f / BuildingSpriteAtlas.SUPERSAMPLE
    val dstW = (sprite.width * scaleInv * z).toInt().coerceAtLeast(1)
    val dstH = (sprite.height * scaleInv * z).toInt().coerceAtLeast(1)
    val anchorFromBottom = (b.footprint * 37f + 18f) * z
    val dstLeft = (center.x - dstW * 0.5f).toInt()
    val dstTop = (center.y - (dstH - anchorFromBottom)).toInt()
    drawImage(
      image = sprite,
      dstOffset = IntOffset(dstLeft, dstTop),
      dstSize = IntSize(dstW, dstH),
      colorFilter = if (isDimmed) ExploredBuildingTint else null,
      filterQuality = FilterQuality.High,
    )

    val windPhase = animClock * 2.4f + b.id * 0.9f
    for (row in -5..5) {
      val ox = row * 8.2f * z
      val oy = row * 4.1f * z
      val sx = center.x - 38f * z + ox
      val sy = center.y + 19f * z - oy
      val ex = center.x + 36f * z + ox
      val ey = center.y - 18f * z - oy

      for (k in 0..6) {
        val f = 0.07f + k * 0.142f
        val bx = sx + (ex - sx) * f
        val by = sy + (ey - sy) * f
        if (row <= -2 && k >= 5) continue
        if (kotlin.math.abs(bx - (center.x - 22f * z)) < 8f * z && kotlin.math.abs(by - (center.y - 8f * z)) < 7f * z) continue

        val wave = sin(windPhase + row * 0.55f + k * 0.45f)
        val gust = wave * 3.4f * z
        val stalkH = (11.5f + ((row * 3 + k) and 3) * 1.4f) * z
        val tipX = bx + gust + 1.8f * z
        val tipY = by - stalkH
        val sunHit = ((wave + 1f) * 0.5f).coerceIn(0f, 1f)

        drawLine(Color(0xFF7D6624), Offset(bx, by), Offset(bx - 3.3f * z + gust * 0.4f, by - 5.2f * z), strokeWidth = 1.3f * z)
        drawLine(Color(0xFFAE8730), Offset(bx, by), Offset(tipX, tipY), strokeWidth = 1.8f * z)
        drawOval(
          color = if (((row + k) and 1) == 0) Color(0xFFD4AC48) else Color(0xFFC49736),
          topLeft = Offset(tipX - 2.2f * z, tipY - 4.6f * z),
          size = Size(4.4f * z, 6.2f * z),
        )
        drawOval(
          color = Color(0xFFF2DA8A).copy(alpha = 0.65f + sunHit * 0.35f),
          topLeft = Offset(tipX - 1.1f * z, tipY - 3.6f * z),
          size = Size(2.2f * z, 3.2f * z),
        )
      }
    }
  }

  private fun DrawScope.drawTorchPool(x: Float, y: Float, radius: Float, phase: Float) {
    val pulse = 0.70f + 0.30f * sin(phase * 7.8f)
    drawSoftRadialOval(
      cx = x,
      cy = y,
      rx = radius * 1.45f,
      ry = radius * 0.62f,
      centerColor = Color(0xFFFF9B2A).copy(alpha = 0.34f * pulse),
      midColor = Color(0xFFFF7A12).copy(alpha = 0.14f * pulse),
    )
    drawSoftVolumetricPuff(
      cx = x,
      cy = y - radius * 0.32f,
      radius = radius * 0.36f,
      color = Color(0xFFFFD875),
      alpha = 0.58f * pulse,
    )
    drawCircle(
      color = Color(0xFFFFF8DE).copy(alpha = 0.82f * pulse),
      radius = radius * 0.085f,
      center = Offset(x, y - radius * 0.33f),
    )
  }

  private fun DrawScope.drawChimneySmoke(x: Float, y: Float, z: Float, animClock: Float) {
    for (i in 0..5) {
      val phase = ((animClock * 0.48f + i * 0.166f) % 1f)
      val sx = x + sin(animClock * 2.0f + i * 1.2f) * (5.2f + phase * 6.0f) * z + phase * 16f * z
      val sy = y - phase * 44f * z
      val alpha = (1f - phase) * (0.46f - phase * 0.12f).coerceAtLeast(0.05f)
      val col =
        when {
          phase < 0.20f -> Color(0xFFB8A38B)
          phase < 0.52f -> Color(0xFFDCD3C6)
          else -> Color(0xFFF0EAE0)
        }
      drawSoftVolumetricPuff(sx, sy, (4.8f + phase * 10.2f) * z, col, alpha)
    }
  }

  private fun DrawScope.drawBannerFlag(x: Float, y: Float, z: Float, color: Color, animClock: Float) {
    drawLine(Color(0xFF422914), Offset(x + 1.1f * z, y + 22f * z), Offset(x + 1.1f * z, y - 11f * z), strokeWidth = 3.4f * z)
    drawLine(Color(0xFFD8C59B), Offset(x, y + 22f * z), Offset(x, y - 11f * z), strokeWidth = 3.0f * z)
    drawCircle(Color(0xFFFFD866), 2.5f * z, Offset(x, y - 12f * z))
    val wave = sin(animClock * 5f) * 4.2f * z
    val flag = Path().apply {
      moveTo(x, y - 10f * z)
      lineTo(x + 24f * z, y - 3.5f * z + wave)
      lineTo(x + 17.5f * z, y + 1f * z + wave * 0.5f)
      lineTo(x + 24f * z, y + 5.5f * z + wave)
      lineTo(x, y + 5.5f * z)
      close()
    }
    drawPath(flag, color)
    // Folded cloth highlight & underside depth crease
    drawLine(
      color = Color(0x55FFF5D6),
      start = Offset(x + 1f * z, y - 8f * z),
      end = Offset(x + 19f * z, y - 3.5f * z + wave * 0.8f),
      strokeWidth = 2.0f * z,
    )
    drawLine(
      color = Color(0x45080604),
      start = Offset(x + 1f * z, y + 3.5f * z),
      end = Offset(x + 18f * z, y + 4.5f * z + wave * 0.8f),
      strokeWidth = 1.6f * z,
    )
    drawPath(flag, Color(0x99FFE082), style = Stroke(width = 1.1f * z))
  }

  private fun DrawScope.drawDiamondOverlay(pTop: Offset, pRight: Offset, pBottom: Offset, pLeft: Offset, color: Color) {
    val path = Path().apply { moveTo(pTop.x, pTop.y); lineTo(pRight.x, pRight.y); lineTo(pBottom.x, pBottom.y); lineTo(pLeft.x, pLeft.y); close() }
    drawPath(path, color)
  }

  fun DrawScope.drawHpBar(cx: Float, y: Float, width: Float, hp: Int, maxHp: Int, z: Float, overrideColor: Color? = null) {
    val h = 6.8f * z
    val left = cx - width * 0.5f
    drawRect(Color(0xDD110D09), Offset(left - 1.8f, y - 1.8f), Size(width + 3.6f, h + 3.6f))
    val ratio = (hp.toFloat() / maxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
    val barColor = overrideColor ?: if (ratio > 0.45f) Color(0xFF4AD158) else if (ratio > 0.22f) Color(0xFFE8B738) else Color(0xFFE04034)
    drawRect(barColor, Offset(left, y), Size(width * ratio, h))
    drawRect(Color(0x48FFFFFF), Offset(left, y), Size(width * ratio, h * 0.35f))
  }
}
