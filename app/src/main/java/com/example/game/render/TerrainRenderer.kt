package com.example.game.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.game.model.CameraState
import com.example.game.model.GameWorld
import com.example.game.model.MapType
import com.example.game.model.TileTerrain
import com.example.game.render.AtmosphereRenderUtils.drawSoftEllipticalShadow
import com.example.game.render.AtmosphereRenderUtils.drawSoftRadialOval
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object TerrainRenderer {

  private const val TEX_SCALE = 0.36f
  private const val HALF_W_TEX = (IsometricMath.BASE_TILE_W * 0.5f) / TEX_SCALE
  private const val HALF_H_TEX = (IsometricMath.BASE_TILE_H * 0.5f) / TEX_SCALE

  private val UnexploredColor = Color(0xFF08070B)
  private val ExploredFogColor = Color(0x34100F18)
  private val FogEdgeOuter = Color(0xA80A090F)
  private val FogEdgeInner = Color(0x5212101A)
  private val MistCloudOuter = Color(0x4E16141F)
  private val MistCloudInner = Color(0x780C0B12)

  private fun groundFallbackColor(mapType: MapType, alt: Boolean): Color =
    when (mapType) {
      MapType.ARABIA -> if (alt) Color(0xFFA18C52) else Color(0xFF938248)
      MapType.BLACK_FOREST -> if (alt) Color(0xFF334925) else Color(0xFF2A3E20)
      MapType.COASTAL -> if (alt) Color(0xFF475F2E) else Color(0xFF3E5428)
    }

  private fun dirtFallbackColor(mapType: MapType): Color =
    when (mapType) {
      MapType.ARABIA -> Color(0xFFB58C56)
      MapType.BLACK_FOREST -> Color(0xFF5D4228)
      MapType.COASTAL -> Color(0xFF7D603D)
    }

  private fun cliffFallbackColor(mapType: MapType): Color =
    if (mapType == MapType.ARABIA) Color(0xFF9B6D46) else Color(0xFF696B6F)

  private val WaterFallbackColor = Color(0xFF1A465C)
  private val TimberFallbackColor = Color(0xFF6A4322)

  private val reusableDiamondPath = Path()
  private val reusableFacetPath = Path()
  private val reusableRockPath = Path()

  private fun tileHash(tx: Int, ty: Int): Int {
    var h = tx * 374761393 + ty * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    return h xor (h ushr 16)
  }

  private fun isBridgeTile(world: GameWorld, tx: Int, ty: Int): Boolean {
    return world.tileAt(tx - 1, ty) == TileTerrain.WATER ||
      world.tileAt(tx + 1, ty) == TileTerrain.WATER ||
      world.tileAt(tx, ty - 1) == TileTerrain.WATER ||
      world.tileAt(tx, ty + 1) == TileTerrain.WATER
  }

  private fun setTexDiamondPath(path: Path, tx: Int, ty: Int, shiftX: Float = 0f, shiftY: Float = 0f) {
    val topX = (tx - ty) * HALF_W_TEX + shiftX
    val topY = (tx + ty) * HALF_H_TEX + shiftY
    val rightX = (tx + 1 - ty) * HALF_W_TEX + shiftX
    val rightY = (tx + 1 + ty) * HALF_H_TEX + shiftY
    val botX = (tx - ty) * HALF_W_TEX + shiftX
    val botY = (tx + ty + 2) * HALF_H_TEX + shiftY
    val leftX = (tx - 1 - ty) * HALF_W_TEX + shiftX
    val leftY = (tx + 1 + ty) * HALF_H_TEX + shiftY
    path.reset()
    path.moveTo(topX, topY - 0.35f)
    path.lineTo(rightX + 0.7f, rightY)
    path.lineTo(botX, botY + 0.35f)
    path.lineTo(leftX - 0.7f, leftY)
    path.close()
  }

  fun DrawScope.drawIsometricTerrain(
    world: GameWorld,
    camera: CameraState,
    animClock: Float,
  ) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return
    val z = camera.zoom

    val corners =
      listOf(
        IsometricMath.screenToWorld(0f, 0f, camera, w, h),
        IsometricMath.screenToWorld(w, 0f, camera, w, h),
        IsometricMath.screenToWorld(0f, h, camera, w, h),
        IsometricMath.screenToWorld(w, h, camera, w, h),
      )
    val minX = max(0, corners.minOf { it.x }.toInt() - 2)
    val maxX = min(world.mapSize - 1, corners.maxOf { it.x }.toInt() + 2)
    val minY = max(0, corners.minOf { it.y }.toInt() - 2)
    val maxY = min(world.mapSize - 1, corners.maxOf { it.y }.toInt() + 2)
    if (minX > maxX || minY > maxY) return

    val worldOrigin = IsometricMath.worldToScreen(0f, 0f, camera, w, h)
    val texToScreen = z * TEX_SCALE
    val invTexToScreen = 1f / texToScreen
    var hasVisibleWater = false

    // Pass 1A: Textured ground & bridge tiles drawn with immutable ShaderBrush + single Canvas transform
    withTransform({
      translate(left = worldOrigin.x, top = worldOrigin.y)
      scale(scaleX = texToScreen, scaleY = texToScreen, pivot = Offset.Zero)
    }) {
      for (ty in minY..maxY) {
        for (tx in minX..maxX) {
          if (world.fogAt(tx, ty) == 0) continue
          val terrain = world.tileAt(tx, ty)
          if (terrain == TileTerrain.WATER) {
            hasVisibleWater = true
            continue
          }
          if (terrain == TileTerrain.CLIFF) continue
          val fallbackColor: Color
          val brush =
            when (terrain) {
              TileTerrain.GRASS -> {
                fallbackColor = groundFallbackColor(world.mapType, alt = false)
                ProceduralTextures.groundBrushFor(world.mapType, alt = false)
              }
              TileTerrain.GRASS_ALT -> {
                fallbackColor = groundFallbackColor(world.mapType, alt = true)
                ProceduralTextures.groundBrushFor(world.mapType, alt = true)
              }
              TileTerrain.DIRT_ROAD ->
                if (isBridgeTile(world, tx, ty)) {
                  fallbackColor = TimberFallbackColor
                  ProceduralTextures.timberBrush
                } else {
                  fallbackColor = dirtFallbackColor(world.mapType)
                  ProceduralTextures.dirtBrushFor(world.mapType)
                }
              else -> continue
            }
          setTexDiamondPath(reusableDiamondPath, tx, ty)
          drawPath(reusableDiamondPath, fallbackColor)
          drawPath(reusableDiamondPath, brush)
        }
      }
    }

    // Pass 1B: Gently drifting marine water tiles with immutable waterBrush
    if (hasVisibleWater) {
      val animOx = animClock * 13f * z
      val animOy = sin(animClock * 1.1f) * 2.6f * z
      val shiftX = -animOx * invTexToScreen
      val shiftY = -animOy * invTexToScreen
      withTransform({
        translate(left = worldOrigin.x + animOx, top = worldOrigin.y + animOy)
        scale(scaleX = texToScreen, scaleY = texToScreen, pivot = Offset.Zero)
      }) {
        for (ty in minY..maxY) {
          for (tx in minX..maxX) {
            if (world.fogAt(tx, ty) == 0 || world.tileAt(tx, ty) != TileTerrain.WATER) continue
            setTexDiamondPath(reusableDiamondPath, tx, ty, shiftX, shiftY)
            drawPath(reusableDiamondPath, WaterFallbackColor)
            drawPath(reusableDiamondPath, ProceduralTextures.waterBrush)
          }
        }
      }
    }

    // Pass 2: Painter's-order cliff geometry, shore banks, vegetation tufts, bridges & fog of war
    for (ty in minY..maxY) {
      for (tx in minX..maxX) {
        val fog = world.fogAt(tx, ty)
        val pTop = IsometricMath.worldToScreen(tx.toFloat(), ty.toFloat(), camera, w, h)
        val pRight = IsometricMath.worldToScreen(tx + 1f, ty.toFloat(), camera, w, h)
        val pBottom = IsometricMath.worldToScreen(tx + 1f, ty + 1f, camera, w, h)
        val pLeft = IsometricMath.worldToScreen(tx.toFloat(), ty + 1f, camera, w, h)
        val cx = (pTop.x + pBottom.x) * 0.5f
        val cy = (pTop.y + pBottom.y) * 0.5f

        reusableDiamondPath.reset()
        reusableDiamondPath.moveTo(pTop.x, pTop.y)
        reusableDiamondPath.lineTo(pRight.x, pRight.y)
        reusableDiamondPath.lineTo(pBottom.x, pBottom.y)
        reusableDiamondPath.lineTo(pLeft.x, pLeft.y)
        reusableDiamondPath.close()

        if (fog == 0) {
          drawPath(reusableDiamondPath, UnexploredColor)
          continue
        }

        val hash = tileHash(tx, ty)
        val terrain = world.tileAt(tx, ty)

        if (terrain == TileTerrain.CLIFF) {
          val hJitter = ((hash and 7) - 3) * 2.4f * z
          val xJitter = (((hash ushr 3) and 7) - 3) * 1.6f * z
          val mainPeak = Offset(cx - 4f * z + xJitter, pTop.y - 43f * z + hJitter)
          val upperLedge = Offset(cx - 19f * z, pTop.y - 24f * z + hJitter * 0.35f)
          val eastSpur = Offset(cx + 15f * z + xJitter * 0.4f, pTop.y - 31f * z + hJitter * 0.5f)
          val midCrag = Offset(cx - 1f * z, pTop.y - 16f * z)
          val cliffBrush = ProceduralTextures.cliffBrushFor(world.mapType)
          val cliffBase = cliffFallbackColor(world.mapType)

          // Soft feathered directional shadow & ambient occlusion under cliff apron
          drawSoftEllipticalShadow(
            cx = cx + 6f * z,
            cy = cy + 3f * z,
            rx = (pRight.x - pLeft.x) * 0.64f,
            ry = (pBottom.y - pTop.y) * 0.48f,
            coreAlpha = 0.56f,
          )

          fun drawFacet(pts: List<Offset>, tint: Color) {
            reusableFacetPath.reset()
            pts.forEachIndexed { idx, pt ->
              if (idx == 0) reusableFacetPath.moveTo(pt.x, pt.y)
              else reusableFacetPath.lineTo(pt.x, pt.y)
            }
            reusableFacetPath.close()
            drawPath(reusableFacetPath, cliffBase)
            withTransform({
              translate(left = worldOrigin.x, top = worldOrigin.y)
              scale(scaleX = texToScreen, scaleY = texToScreen, pivot = Offset.Zero)
            }) {
              reusableFacetPath.reset()
              pts.forEachIndexed { idx, pt ->
                val lx = (pt.x - worldOrigin.x) * invTexToScreen
                val ly = (pt.y - worldOrigin.y) * invTexToScreen
                if (idx == 0) reusableFacetPath.moveTo(lx, ly)
                else reusableFacetPath.lineTo(lx, ly)
              }
              reusableFacetPath.close()
              drawPath(reusableFacetPath, cliffBrush)
            }
            reusableFacetPath.reset()
            pts.forEachIndexed { idx, pt ->
              if (idx == 0) reusableFacetPath.moveTo(pt.x, pt.y)
              else reusableFacetPath.lineTo(pt.x, pt.y)
            }
            reusableFacetPath.close()
            drawPath(reusableFacetPath, tint)
          }

          // Multi-plane fractured crag lighting: sunlit upper plateau, weathered west face, deep shadowed east face
          drawFacet(listOf(pTop, pRight, eastSpur, mainPeak, upperLedge, pLeft), Color(0x3AF7E5B8))
          drawFacet(listOf(pLeft, upperLedge, midCrag, pBottom), Color(0x2A1F1811))
          drawFacet(listOf(upperLedge, mainPeak, midCrag), Color(0x28FCEECA))
          drawFacet(listOf(pBottom, midCrag, mainPeak, eastSpur, pRight), Color(0x8E0C0B12))

          // Natural sunlit rock ridge edges & weathered fracture crevices
          drawLine(Color(0xDEF5ECE0), mainPeak, eastSpur, strokeWidth = 2.7f * z)
          drawLine(Color(0xC8EEE4D0), upperLedge, mainPeak, strokeWidth = 2.4f * z)
          drawLine(Color(0x88F6F0E4), pTop, mainPeak, strokeWidth = 1.6f * z)
          drawLine(Color(0xC2161311), pBottom, midCrag, strokeWidth = 2.4f * z)
          drawLine(Color(0xCE161311), midCrag, mainPeak, strokeWidth = 2.2f * z)
          drawLine(
            Color(0x771E1914),
            Offset(pLeft.x + 11f * z, pLeft.y - 3f * z),
            Offset(mainPeak.x - 4f * z, mainPeak.y + 15f * z),
            strokeWidth = 1.3f * z,
          )
          drawLine(
            Color(0x77161317),
            Offset(pBottom.x + 12f * z, pBottom.y - 9f * z),
            Offset(eastSpur.x - 2f * z, eastSpur.y + 10f * z),
            strokeWidth = 1.2f * z,
          )
          drawLine(
            Color(0x5A2A221B),
            Offset(upperLedge.x + 3f * z, upperLedge.y + 4f * z),
            Offset(eastSpur.x - 6f * z, eastSpur.y + 5f * z),
            strokeWidth = 1.1f * z,
          )

          // Feathered highland turf cap / moss / arid scrub clinging to summit clefts & ridge overhang
          val scrubDark = if (world.mapType == MapType.ARABIA) Color(0x8E736536) else Color(0xCC2C401B)
          val scrubLight = if (world.mapType == MapType.ARABIA) Color(0x88998950) else Color(0xAA4D6630)
          drawSoftRadialOval(mainPeak.x - 1f * z, mainPeak.y + 0.5f * z, 15f * z, 5.8f * z, scrubDark)
          drawSoftRadialOval(mainPeak.x - 2f * z, mainPeak.y - 0.8f * z, 9f * z, 3.6f * z, scrubLight)
          drawSoftRadialOval(upperLedge.x + 1f * z, upperLedge.y + 0.4f * z, 7.2f * z, 3.0f * z, scrubDark)
          if (world.mapType != MapType.ARABIA) {
            for (g in -1..1) {
              val gx = mainPeak.x + g * 5.2f * z
              val gy = mainPeak.y + kotlin.math.abs(g) * 1.4f * z
              drawLine(Color(0xFF425E28), Offset(gx, gy), Offset(gx + g * 1.4f * z, gy - 5.2f * z), strokeWidth = 1.2f * z)
            }
          }

          // Angular fractured talus boulders & scree along cliff base with soft shadows
          fun drawScreeBoulder(bx: Float, by: Float, r: Float, shade: Color, hi: Color) {
            drawSoftEllipticalShadow(bx + r * 0.22f, by + r * 0.34f, r * 1.24f, r * 0.48f, coreAlpha = 0.48f)
            reusableRockPath.reset()
            reusableRockPath.moveTo(bx - r, by + r * 0.1f)
            reusableRockPath.lineTo(bx - r * 0.55f, by - r * 0.72f)
            reusableRockPath.lineTo(bx + r * 0.35f, by - r * 0.80f)
            reusableRockPath.lineTo(bx + r * 0.95f, by - r * 0.15f)
            reusableRockPath.lineTo(bx + r * 0.45f, by + r * 0.38f)
            reusableRockPath.lineTo(bx - r * 0.55f, by + r * 0.35f)
            reusableRockPath.close()
            drawPath(reusableRockPath, shade)
            drawPath(reusableRockPath, Color(0x45120E0A), style = Stroke(width = 0.8f * z))
            drawLine(hi, Offset(bx - r * 0.5f, by - r * 0.65f), Offset(bx + r * 0.35f, by - r * 0.70f), strokeWidth = 1.5f * z)
            drawLine(Color(0x52120E0A), Offset(bx + r * 0.1f, by - r * 0.35f), Offset(bx + r * 0.5f, by + r * 0.25f), strokeWidth = 0.9f * z)
          }
          drawScreeBoulder(pBottom.x - 15f * z, pBottom.y - 4f * z, 5.4f * z, Color(0xFF635B50), Color(0xFFA89C8C))
          drawScreeBoulder(pBottom.x + 13f * z, pBottom.y - 6f * z, 4.2f * z, Color(0xFF524B42), Color(0xFF8F8577))
          drawScreeBoulder(pLeft.x + 14f * z, pLeft.y + 1.5f * z, 3.2f * z, Color(0xFF6E655A), Color(0xFFABA090))
        } else {
          // Continuous rolling elevation daylight modulation & organic soil/moisture variation
          if (terrain != TileTerrain.WATER) {
            val hillWave = sin(tx * 0.27f + ty * 0.19f) * cos(tx * 0.14f - ty * 0.25f)
            if (hillWave > 0.18f) {
              drawPath(reusableDiamondPath, Color(0xFFF6E8BD).copy(alpha = (hillWave - 0.18f) * 0.11f))
            } else if (hillWave < -0.18f) {
              drawPath(reusableDiamondPath, Color(0xFF0A0D12).copy(alpha = (-hillWave - 0.18f) * 0.12f))
            }
            val toneMod = (hash and 7)
            if (toneMod == 0) {
              drawPath(reusableDiamondPath, Color(0x0D060504))
            } else if (toneMod == 5) {
              drawPath(reusableDiamondPath, Color(0x0CF2E6C4))
            }
          }

          // Soft feathered organic biome blending across tile boundaries to eliminate grid seams
          if (terrain == TileTerrain.GRASS || terrain == TileTerrain.GRASS_ALT) {
            val splatColor =
              when (world.mapType) {
                MapType.ARABIA -> if (terrain == TileTerrain.GRASS_ALT) Color(0x4CA99258) else Color(0x4086743D)
                MapType.BLACK_FOREST -> if (terrain == TileTerrain.GRASS_ALT) Color(0x523A542B) else Color(0x46223519)
                MapType.COASTAL -> if (terrain == TileTerrain.GRASS_ALT) Color(0x52567238) else Color(0x46344B21)
              }
            if ((hash and 3) != 0) {
              val sx = cx + (((hash ushr 2) and 7) - 3.5f) * 5f * z
              val sy = cy + (((hash ushr 5) and 7) - 3.5f) * 2.5f * z
              drawSoftRadialOval(sx, sy, 28f * z, 14f * z, splatColor)
            }
            // Cross-border organic overhang onto differing adjacent tiles
            if (world.tileAt(tx + 1, ty) != terrain && world.tileAt(tx + 1, ty) != TileTerrain.WATER && (hash and 1) == 0) {
              drawSoftRadialOval((pRight.x + pBottom.x) * 0.5f, (pRight.y + pBottom.y) * 0.5f, 20f * z, 10f * z, splatColor)
            }
            if (world.tileAt(tx, ty + 1) != terrain && world.tileAt(tx, ty + 1) != TileTerrain.WATER && (hash and 2) == 0) {
              drawSoftRadialOval((pLeft.x + pBottom.x) * 0.5f, (pLeft.y + pBottom.y) * 0.5f, 20f * z, 10f * z, splatColor)
            }

            val featherOuter =
              when (world.mapType) {
                MapType.ARABIA -> Color(0x309B7A4A)
                MapType.BLACK_FOREST -> Color(0x304D3B26)
                MapType.COASTAL -> Color(0x307A623F)
              }
            val featherInner =
              when (world.mapType) {
                MapType.ARABIA -> Color(0x5CA88552)
                MapType.BLACK_FOREST -> Color(0x5C57422B)
                MapType.COASTAL -> Color(0x5C856B45)
              }
            val fringeBlade =
              when (world.mapType) {
                MapType.ARABIA -> Color(0xB88C7C3B)
                MapType.BLACK_FOREST -> Color(0xC82C4520)
                MapType.COASTAL -> Color(0xC83E5D28)
              }
            fun drawRoadBorderTufts(a: Offset, b: Offset) {
              for (t in 1..2) {
                val f = t * 0.34f + ((hash and 3) * 0.05f)
                val bx = a.x + (b.x - a.x) * f
                val by = a.y + (b.y - a.y) * f
                drawLine(fringeBlade, Offset(bx - 1.5f * z, by), Offset(bx - 3.5f * z, by - 5.5f * z), 1.3f * z)
                drawLine(fringeBlade, Offset(bx + 1.5f * z, by), Offset(bx + 3.2f * z, by - 4.8f * z), 1.2f * z)
              }
            }
            if (world.tileAt(tx, ty - 1) == TileTerrain.DIRT_ROAD) {
              drawLine(featherOuter, pTop, pRight, strokeWidth = 22f * z)
              drawLine(featherInner, pTop, pRight, strokeWidth = 9f * z)
              drawRoadBorderTufts(pTop, pRight)
            }
            if (world.tileAt(tx, ty + 1) == TileTerrain.DIRT_ROAD) {
              drawLine(featherOuter, pLeft, pBottom, strokeWidth = 22f * z)
              drawLine(featherInner, pLeft, pBottom, strokeWidth = 9f * z)
              drawRoadBorderTufts(pLeft, pBottom)
            }
            if (world.tileAt(tx - 1, ty) == TileTerrain.DIRT_ROAD) {
              drawLine(featherOuter, pTop, pLeft, strokeWidth = 22f * z)
              drawLine(featherInner, pTop, pLeft, strokeWidth = 9f * z)
              drawRoadBorderTufts(pTop, pLeft)
            }
            if (world.tileAt(tx + 1, ty) == TileTerrain.DIRT_ROAD) {
              drawLine(featherOuter, pRight, pBottom, strokeWidth = 22f * z)
              drawLine(featherInner, pRight, pBottom, strokeWidth = 9f * z)
              drawRoadBorderTufts(pRight, pBottom)
            }

            if (world.tileAt(tx - 1, ty) == TileTerrain.CLIFF) {
              drawLine(Color(0x2E080604), pTop, pLeft, strokeWidth = 28f * z)
              drawLine(Color(0x58080604), pTop, pLeft, strokeWidth = 12f * z)
            }
            if (world.tileAt(tx, ty - 1) == TileTerrain.CLIFF) {
              drawLine(Color(0x2E080604), pTop, pRight, strokeWidth = 28f * z)
              drawLine(Color(0x58080604), pTop, pRight, strokeWidth = 12f * z)
            }
          }

          if (terrain == TileTerrain.WATER) {
            val landN = world.tileAt(tx, ty - 1) != TileTerrain.WATER
            val landS = world.tileAt(tx, ty + 1) != TileTerrain.WATER
            val landW = world.tileAt(tx - 1, ty) != TileTerrain.WATER
            val landE = world.tileAt(tx + 1, ty) != TileTerrain.WATER
            if (landN || landS || landW || landE) {
              // Translucent shallow coastal mineral turquoise + sandy shoal gradient
              drawPath(reusableDiamondPath, Color(0x3E2F6B6F))
              drawSoftRadialOval(cx, cy, 34f * z, 17f * z, Color(0x2EA69162))
            } else {
              // Deeper bathymetric oceanic shading off-shore
              drawPath(reusableDiamondPath, Color(0x28061420))
            }
            val wetBank = Color(0x88473927)
            val sandShore = Color(0xCC918061)
            val surfFoam = Color(0x8CD5E2E6)
            val wPulse = sin(animClock * 1.9f + tx * 0.7f + ty * 0.4f) * 2.0f * z
            if (landN) {
              if (world.tileAt(tx, ty - 1) == TileTerrain.DIRT_ROAD) {
                drawLine(Color(0x55060D14), Offset(pTop.x + 2f * z, pTop.y + 4f * z), Offset(pRight.x + 2f * z, pRight.y + 4f * z), strokeWidth = 10f * z)
              }
              drawLine(wetBank, pTop, pRight, strokeWidth = 14f * z)
              drawLine(sandShore, pTop, pRight, strokeWidth = 6.2f * z)
              drawLine(
                surfFoam,
                Offset(pTop.x + 6f * z, pTop.y + 5.5f * z + wPulse),
                Offset(pRight.x - 6f * z, pRight.y + 5.5f * z + wPulse),
                strokeWidth = 2.1f * z,
              )
              drawLine(
                Color(0x58E0EAED),
                Offset(pTop.x + 12f * z, pTop.y + 9.5f * z + wPulse * 0.5f),
                Offset(pRight.x - 12f * z, pRight.y + 9.5f * z + wPulse * 0.5f),
                strokeWidth = 1.4f * z,
              )
            }
            if (landS) {
              drawLine(wetBank, pLeft, pBottom, strokeWidth = 11f * z)
              drawLine(sandShore, pLeft, pBottom, strokeWidth = 5.0f * z)
              drawLine(
                Color(0x78CEE0E4),
                Offset(pLeft.x + 7f * z, pLeft.y - 4.5f * z - wPulse * 0.4f),
                Offset(pBottom.x - 7f * z, pBottom.y - 4.5f * z - wPulse * 0.4f),
                strokeWidth = 1.8f * z,
              )
            }
            if (landW) {
              if (world.tileAt(tx - 1, ty) == TileTerrain.DIRT_ROAD) {
                drawLine(Color(0x55060D14), Offset(pTop.x + 2f * z, pTop.y + 4f * z), Offset(pLeft.x + 2f * z, pLeft.y + 4f * z), strokeWidth = 10f * z)
              }
              drawLine(wetBank, pTop, pLeft, strokeWidth = 14f * z)
              drawLine(sandShore, pTop, pLeft, strokeWidth = 6.2f * z)
              drawLine(
                surfFoam,
                Offset(pTop.x - 6f * z, pTop.y + 5.5f * z + wPulse),
                Offset(pLeft.x + 6f * z, pLeft.y + 5.5f * z + wPulse),
                strokeWidth = 2.1f * z,
              )
            }
            if (landE) {
              drawLine(wetBank, pRight, pBottom, strokeWidth = 11f * z)
              drawLine(sandShore, pRight, pBottom, strokeWidth = 5.0f * z)
              drawLine(
                Color(0x78CEE0E4),
                Offset(pRight.x - 7f * z, pRight.y - 4.5f * z - wPulse * 0.4f),
                Offset(pBottom.x + 7f * z, pBottom.y - 4.5f * z - wPulse * 0.4f),
                strokeWidth = 1.8f * z,
              )
            }

            // Soft shimmering subsurface light caustics & sunlit wave specular glints
            val causticShiftX = 7f * z * sin(animClock * 1.4f + tx * 0.8f + ty * 0.6f)
            val causticShiftY = 3.5f * z * cos(animClock * 1.2f - tx * 0.5f + ty * 0.9f)
            drawSoftRadialOval(
              cx = cx - 4f * z + causticShiftX,
              cy = cy - 1.5f * z + causticShiftY,
              rx = 16f * z,
              ry = 7f * z,
              centerColor = Color(0x2E74C0D0),
            )
            drawSoftRadialOval(
              cx = cx + 4f * z - causticShiftX * 0.6f,
              cy = cy + 1.5f * z - causticShiftY * 0.5f,
              rx = 13f * z,
              ry = 5.5f * z,
              centerColor = Color(0x26A8DAE4),
            )

            val shimmer = 12f * z * sin(animClock * 1.9f + tx * 0.9f + ty * 0.5f)
            val shimmer2 = 8f * z * cos(animClock * 1.5f + tx * 0.6f - ty * 0.8f)
            drawLine(
              color = Color(0x52D8ECF2),
              start = Offset(cx - 20f * z + shimmer, cy - 3.5f * z),
              end = Offset(cx + 17f * z + shimmer, cy - 3.5f * z),
              strokeWidth = 2.0f * z,
            )
            drawLine(
              color = Color(0x42F0F8FA),
              start = Offset(cx - 11f * z - shimmer * 0.5f, cy + 5.5f * z),
              end = Offset(cx + 13f * z - shimmer * 0.5f, cy + 5.5f * z),
              strokeWidth = 1.5f * z,
            )
            drawCircle(Color(0x78F4FBFC), 1.6f * z, Offset(cx + shimmer2, cy + 1f * z))
            if (!landN && !landS && !landW && !landE && (hash and 7) == 1) {
              val capX = cx + shimmer2 * 0.8f
              val capY = cy - 4f * z + sin(animClock * 1.4f + tx) * 2.5f * z
              drawSoftRadialOval(capX, capY, 8.5f * z, 2.4f * z, Color(0x46E8F6FA))
            }
            if ((landN || landW) && (hash and 3) == 1) {
              drawCircle(Color(0xAA736756), 1.8f * z, Offset(cx - 10f * z, cy - 6f * z))
              drawCircle(Color(0xAA968872), 1.4f * z, Offset(cx + 8f * z, cy - 7f * z))
            }

            // Natural cattail reeds & muted lily pads along banks
            if ((landN || landW) && (hash and 3) == 0) {
              val sway = sin(animClock * 1.8f + tx) * 1.4f * z
              drawLine(Color(0xFF34471E), Offset(cx - 12f * z, cy - 4f * z), Offset(cx - 16f * z + sway, cy - 17f * z), 1.8f * z)
              drawLine(Color(0xFF455D28), Offset(cx - 8f * z, cy - 3f * z), Offset(cx - 8f * z + sway, cy - 19f * z), 1.7f * z)
              drawLine(Color(0xFF547032), Offset(cx - 4f * z, cy - 2f * z), Offset(cx - 1f * z + sway, cy - 15f * z), 1.5f * z)
              drawOval(Color(0xFF4D2E18), Offset(cx - 9.5f * z + sway, cy - 19f * z), Size(3.2f * z, 5.2f * z))
              drawOval(Color(0xFF4D2E18), Offset(cx - 17f * z + sway, cy - 17f * z), Size(2.8f * z, 4.6f * z))
            } else if ((landS || landE) && (hash and 7) == 2 && world.mapType != MapType.ARABIA) {
              drawOval(Color(0xFF335724), Offset(cx + 2f * z, cy - 3f * z), Size(8.5f * z, 4.6f * z))
              drawCircle(Color(0xFFEBE4D0), 1.5f * z, Offset(cx + 6f * z, cy - 1f * z))
            }
          } else {
            when (terrain) {
              TileTerrain.GRASS, TileTerrain.GRASS_ALT -> {
                if ((hash and 3) < 2) {
                  val ox = (((hash ushr 4) and 15) - 7) * 3.1f * z
                  val oy = (((hash ushr 8) and 7) - 3) * 2.1f * z
                  val tx0 = cx + ox
                  val ty0 = cy + oy
                  val gust = sin(animClock * 1.9f + tx * 0.5f + ty * 0.3f) * 1.4f * z
                  val bladeA =
                    when (world.mapType) {
                      MapType.ARABIA -> Color(0xFF7D6F34)
                      MapType.BLACK_FOREST -> Color(0xFF213618)
                      MapType.COASTAL -> Color(0xFF2D481E)
                    }
                  val bladeB =
                    when (world.mapType) {
                      MapType.ARABIA -> Color(0xFFA89652)
                      MapType.BLACK_FOREST -> Color(0xFF3D5C2B)
                      MapType.COASTAL -> Color(0xFF547335)
                    }
                  drawSoftEllipticalShadow(tx0, ty0 + 0.5f * z, 6.8f * z, 2.3f * z, coreAlpha = 0.28f)
                  drawLine(bladeA, Offset(tx0 - 3.5f * z, ty0), Offset(tx0 - 7f * z + gust, ty0 - 9f * z), 1.6f * z)
                  drawLine(bladeB, Offset(tx0 - 0.5f * z, ty0), Offset(tx0 + 0.5f * z + gust, ty0 - 11.5f * z), 1.6f * z)
                  drawLine(bladeB, Offset(tx0 + 2.5f * z, ty0), Offset(tx0 + 6f * z + gust, ty0 - 8f * z), 1.4f * z)
                  drawLine(bladeA, Offset(tx0 + 4.5f * z, ty0 + 0.5f * z), Offset(tx0 + 8.5f * z + gust, ty0 - 6f * z), 1.2f * z)
                }
                if ((hash and 15) == 3) {
                  when (world.mapType) {
                    MapType.ARABIA -> {
                      // Dry desert scrub clump, wind-carved sand crest & sun-bleached stone
                      drawSoftEllipticalShadow(cx + 11.5f * z, cy + 1.2f * z, 6.8f * z, 2.8f * z, coreAlpha = 0.30f)
                      drawOval(Color(0xFFAD8F58), Offset(cx + 6f * z, cy - 3.5f * z), Size(11f * z, 5.5f * z))
                      drawCircle(Color(0xFF566B32), 3.4f * z, Offset(cx - 11f * z, cy + 3f * z))
                      drawCircle(Color(0xFF7B9048), 2.0f * z, Offset(cx - 11.5f * z, cy + 2.2f * z))
                      drawLine(Color(0x70694E2A), Offset(cx - 17f * z, cy - 4f * z), Offset(cx - 4f * z, cy - 1f * z), 1.2f * z)
                    }
                    MapType.BLACK_FOREST -> {
                      // Forest floor fern rosette, weathered mossy fallen limb & natural toadstools
                      drawSoftEllipticalShadow(cx + 10.5f * z, cy + 1.5f * z, 7.8f * z, 3.6f * z, coreAlpha = 0.34f)
                      drawOval(Color(0xFF223D1C), Offset(cx + 4f * z, cy - 4f * z), Size(13f * z, 6.5f * z))
                      drawOval(Color(0xFF3E612B), Offset(cx + 6f * z, cy - 5.5f * z), Size(9f * z, 4.5f * z))
                      drawLine(Color(0xFF4F331B), Offset(cx - 17f * z, cy + 5.5f * z), Offset(cx - 3f * z, cy + 1f * z), 3.1f * z)
                      drawLine(Color(0xFF6E4B2B), Offset(cx - 16f * z, cy + 4.5f * z), Offset(cx - 5f * z, cy + 0.8f * z), 1.1f * z)
                      drawCircle(Color(0xFF9C2D24), 2.1f * z, Offset(cx - 12f * z, cy + 0.8f * z))
                      drawCircle(Color(0xFF9C2D24), 1.6f * z, Offset(cx - 8f * z, cy + 2.5f * z))
                      drawCircle(Color(0xFFE8DEC8), 0.7f * z, Offset(cx - 12.5f * z, cy + 0.3f * z))
                    }
                    MapType.COASTAL -> {
                      // Subtle wildflower clump in natural meadow tones
                      drawSoftRadialOval(cx + 10f * z, cy - 2f * z, 6.5f * z, 4.5f * z, Color(0x88314E20))
                      drawCircle(Color(0xFFD6B94E), 2.0f * z, Offset(cx + 9f * z, cy - 3.2f * z))
                      drawCircle(Color(0xFFB33B30), 2.0f * z, Offset(cx + 13.5f * z, cy - 1f * z))
                      drawCircle(Color(0xFFEBE4D0), 1.7f * z, Offset(cx - 10f * z, cy + 2.5f * z))
                      drawCircle(Color(0xFFEBE4D0), 1.5f * z, Offset(cx - 7f * z, cy + 3.8f * z))
                    }
                  }
                }
                if ((hash and 31) == 11) {
                  drawSoftEllipticalShadow(cx - 17.5f * z, cy - 1.6f * z, 3.8f * z, 2.0f * z, coreAlpha = 0.34f)
                  drawOval(Color(0xFF635B4E), Offset(cx - 21f * z, cy - 6f * z), Size(6.5f * z, 4.5f * z))
                  drawOval(Color(0xFF82796A), Offset(cx - 16f * z, cy - 4.5f * z), Size(4.8f * z, 3.3f * z))
                  drawOval(Color(0xFF9E9484), Offset(cx - 20f * z, cy - 5.8f * z), Size(3.2f * z, 1.8f * z))
                }
              }
              TileTerrain.DIRT_ROAD -> {
                if (isBridgeTile(world, tx, ty)) {
                  // Submerged timber bridge pilings standing in water with animated wake foam rings
                  val hasWaterNorth = world.tileAt(tx, ty - 1) == TileTerrain.WATER
                  val hasWaterSouth = world.tileAt(tx, ty + 1) == TileTerrain.WATER
                  val hasWaterWest = world.tileAt(tx - 1, ty) == TileTerrain.WATER
                  val hasWaterEast = world.tileAt(tx + 1, ty) == TileTerrain.WATER
                  if (hasWaterSouth || hasWaterEast || hasWaterNorth || hasWaterWest) {
                    val wakePulse = sin(animClock * 2.2f + tx * 0.7f + ty * 0.5f) * 1.4f * z
                    for (piling in listOf(pLeft, pBottom, pRight)) {
                      drawSoftRadialOval(piling.x, piling.y + 11f * z, 11.5f * z + wakePulse, 4.6f * z, Color(0x46DCEEF4))
                      drawRect(Color(0x96141F26), Offset(piling.x - 2.6f * z, piling.y + 2f * z), Size(5.2f * z, 10f * z))
                      drawOval(
                        color = Color(0x5AEAF6FA),
                        topLeft = Offset(piling.x - 6.5f * z - wakePulse * 0.4f, piling.y + 8.5f * z),
                        size = Size(13f * z + wakePulse * 0.8f, 4.4f * z),
                        style = Stroke(width = 1.0f * z),
                      )
                    }
                  }
                  // Soft water reflection shadow & deck underside beam
                  drawLine(Color(0x58081018), Offset(pLeft.x, pLeft.y + 4.5f * z), Offset(pBottom.x, pBottom.y + 4.5f * z), 8.2f * z)
                  drawLine(Color(0x58081018), Offset(pBottom.x, pBottom.y + 4.5f * z), Offset(pRight.x, pRight.y + 4.5f * z), 8.2f * z)
                  drawPath(reusableDiamondPath, Color(0xB4291A0D), style = Stroke(width = 2.6f * z))
                  for (plank in -4..4) {
                    val ox = plank * 6.2f * z
                    val oy = plank * 3.1f * z
                    if ((plank and 1) == 0) {
                      drawLine(
                        Color(0x28261507),
                        Offset(cx - 21f * z + ox, cy + 10.5f * z - oy - 0.5f * z),
                        Offset(cx + 21f * z + ox, cy - 10.5f * z - oy - 0.5f * z),
                        3.4f * z,
                      )
                    }
                    drawLine(
                      Color(0xA41D1208),
                      Offset(cx - 22f * z + ox, cy + 11f * z - oy),
                      Offset(cx + 22f * z + ox, cy - 11f * z - oy),
                      1.8f * z,
                    )
                    drawLine(
                      Color(0x46E0BD8A),
                      Offset(cx - 20f * z + ox, cy + 10f * z - oy - 1.2f * z),
                      Offset(cx + 20f * z + ox, cy - 10f * z - oy - 1.2f * z),
                      0.95f * z,
                    )
                  }
                  // Faint muddy wagon wheel track polish down bridge centerline
                  drawLine(Color(0x28362412), Offset(cx - 16f * z, cy + 6f * z), Offset(cx + 16f * z, cy - 6f * z), 4.0f * z)
                  // Handrails + diagonal timber bracing struts + corner iron-capped posts
                  drawLine(Color(0xFF4F2F17), Offset(pTop.x, pTop.y - 10f * z), Offset(pRight.x, pRight.y - 10f * z), 2.8f * z)
                  drawLine(Color(0xFF94693E), Offset(pTop.x, pTop.y - 11.2f * z), Offset(pRight.x, pRight.y - 11.2f * z), 1.1f * z)
                  drawLine(Color(0xFF57361C), Offset(pLeft.x, pLeft.y - 10f * z), Offset(pBottom.x, pBottom.y - 10f * z), 2.8f * z)
                  drawLine(Color(0xFF94693E), Offset(pLeft.x, pLeft.y - 11.2f * z), Offset(pBottom.x, pBottom.y - 11.2f * z), 1.1f * z)
                  drawLine(Color(0xFF6B4323), Offset(pLeft.x, pLeft.y - 5f * z), Offset(pBottom.x, pBottom.y - 5f * z), 1.6f * z)
                  drawLine(Color(0xFF6B4323), Offset(pBottom.x, pBottom.y - 5f * z), Offset(pRight.x, pRight.y - 5f * z), 1.6f * z)
                  for (post in listOf(pTop, pRight, pBottom, pLeft)) {
                    drawRect(Color(0xFF2B1A0D), Offset(post.x - 2.6f * z, post.y), Size(5.2f * z, 5.5f * z))
                    drawRect(Color(0xFF3E2613), Offset(post.x - 3.0f * z, post.y - 14f * z), Size(6.0f * z, 14f * z))
                    drawRect(Color(0xFF855D37), Offset(post.x - 3.0f * z, post.y - 14f * z), Size(6.0f * z, 2.8f * z))
                    drawCircle(Color(0xFF282C30), 1.4f * z, Offset(post.x, post.y - 10f * z))
                  }
                } else {
                  if ((hash and 3) == 0) {
                    drawLine(Color(0x322E2012), Offset(cx - 18f * z, cy + 6f * z), Offset(cx + 16f * z, cy - 10f * z), 3.6f * z)
                    drawLine(Color(0x322E2012), Offset(cx - 12f * z, cy + 11f * z), Offset(cx + 21f * z, cy - 5f * z), 3.0f * z)
                    drawLine(Color(0x26ECD8B4), Offset(cx - 17f * z, cy + 5f * z), Offset(cx + 16f * z, cy - 11f * z), 1.1f * z)
                  }
                  if (world.mapType == MapType.ARABIA && (hash and 15) == 5) {
                    drawLine(Color(0x5A574124), Offset(cx - 15f * z, cy + 2f * z), Offset(cx + 7f * z, cy - 4f * z), 1.3f * z)
                  }
                  if ((hash and 7) < 2) {
                    drawSoftEllipticalShadow(cx - 5.5f * z, cy - 0.5f * z, 5.2f * z, 2.6f * z, coreAlpha = 0.30f)
                    drawOval(Color(0xFF756A57), Offset(cx - 10f * z, cy - 3f * z), Size(9.0f * z, 4.8f * z))
                    drawOval(Color(0xFF9E927B), Offset(cx - 9f * z, cy - 3f * z), Size(5.5f * z, 2.3f * z))
                    drawOval(Color(0xFF887C68), Offset(cx + 5f * z, cy + 2f * z), Size(6.8f * z, 3.8f * z))
                    drawCircle(Color(0xFFA89C84), 1.4f * z, Offset(cx + 7.2f * z, cy + 3.0f * z))
                  }
                }
              }
              else -> Unit
            }
          }
        }

        if (fog == 1) {
          drawPath(reusableDiamondPath, ExploredFogColor)
        }
        val fogN = world.fogAt(tx, ty - 1) == 0
        val fogS = world.fogAt(tx, ty + 1) == 0
        val fogW = world.fogAt(tx - 1, ty) == 0
        val fogE = world.fogAt(tx + 1, ty) == 0
        if (fogN || fogS || fogW || fogE) {
          drawPath(reusableDiamondPath, FogEdgeOuter, style = Stroke(width = 16f * z))
          drawPath(reusableDiamondPath, FogEdgeInner, style = Stroke(width = 28f * z))
          val drift = sin(animClock * 0.9f + tx * 0.5f + ty * 0.4f) * 3.5f * z
          if (fogN) {
            val mx = (pTop.x + pRight.x) * 0.5f + drift
            val my = (pTop.y + pRight.y) * 0.5f
            drawSoftRadialOval(mx, my, 30f * z, 16f * z, MistCloudInner, MistCloudOuter)
          }
          if (fogS) {
            val mx = (pLeft.x + pBottom.x) * 0.5f - drift
            val my = (pLeft.y + pBottom.y) * 0.5f
            drawSoftRadialOval(mx, my, 30f * z, 16f * z, MistCloudInner, MistCloudOuter)
          }
          if (fogW) {
            val mx = (pTop.x + pLeft.x) * 0.5f + drift * 0.7f
            val my = (pTop.y + pLeft.y) * 0.5f
            drawSoftRadialOval(mx, my, 30f * z, 16f * z, MistCloudInner, MistCloudOuter)
          }
          if (fogE) {
            val mx = (pRight.x + pBottom.x) * 0.5f - drift * 0.7f
            val my = (pRight.y + pBottom.y) * 0.5f
            drawSoftRadialOval(mx, my, 30f * z, 16f * z, MistCloudInner, MistCloudOuter)
          }
        }
      }
    }
  }
}
