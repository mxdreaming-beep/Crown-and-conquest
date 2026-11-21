package com.example.game.render

import android.graphics.Bitmap
import android.graphics.Color as NativeColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.game.model.BuildingType
import com.example.game.model.CameraState
import com.example.game.model.Faction
import com.example.game.model.GameDefinitions
import com.example.game.model.GameWorld
import com.example.game.model.MapType
import com.example.game.model.ResourceNodeType
import com.example.game.model.TileTerrain

private const val SUB_TILE = 3

@Composable
fun TacticalMinimap(
  world: GameWorld,
  camera: CameraState,
  onJumpCamera: (Float, Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  val mapSz = world.mapSize
  val texDim = (mapSz * SUB_TILE).coerceAtLeast(3)
  val bufferState = remember(texDim) { MinimapPixelBuffer(texDim) }

  Canvas(
    modifier =
      modifier
        .pointerInput(mapSz) {
          detectTapGestures { offset ->
            val wx = (offset.x / size.width.coerceAtLeast(1)) * mapSz
            val wy = (offset.y / size.height.coerceAtLeast(1)) * mapSz
            onJumpCamera(wx.coerceIn(0f, mapSz.toFloat()), wy.coerceIn(0f, mapSz.toFloat()))
          }
        }
        .pointerInput(mapSz) {
          detectDragGestures { change, _ ->
            change.consume()
            val wx = (change.position.x / size.width.coerceAtLeast(1)) * mapSz
            val wy = (change.position.y / size.height.coerceAtLeast(1)) * mapSz
            onJumpCamera(wx.coerceIn(0f, mapSz.toFloat()), wy.coerceIn(0f, mapSz.toFloat()))
          }
        }
  ) {
    val cw = size.width / mapSz
    val ch = size.height / mapSz

    // 1. Sub-tile Hill-Shaded Cartographic Relief Bitmap (3x3 px per map tile)
    bufferState.renderTerrain(world)
    drawImage(
      image = bufferState.imageBitmap,
      dstOffset = IntOffset.Zero,
      dstSize = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1)),
      filterQuality = FilterQuality.Low,
    )

    // 2. Crisp Resources (Layered Tree Canopy Clusters, Faceted Gold/Stone Diamonds, Berry/Hunt Pips)
    for (res in world.resources) {
      if (world.fogAt(res.tileX, res.tileY) == 0) continue
      val rx = res.x * cw
      val ry = res.y * ch
      when (res.type) {
        ResourceNodeType.FOREST_OAK, ResourceNodeType.FOREST_PINE -> {
          val radius = (cw * 0.92f).coerceAtLeast(1.9f)
          drawCircle(Color(0xBB0A1608), radius + 0.7f, Offset(rx + 0.5f, ry + 0.5f))
          drawCircle(
            if (res.type == ResourceNodeType.FOREST_PINE) Color(0xFF1A3A1E) else Color(0xFF284D1F),
            radius,
            Offset(rx, ry),
          )
          drawCircle(
            if (res.type == ResourceNodeType.FOREST_PINE) Color(0xFF315E2E) else Color(0xFF477233),
            radius * 0.48f,
            Offset(rx - radius * 0.22f, ry - radius * 0.22f),
          )
        }
        ResourceNodeType.GOLD_MINE -> {
          drawDiamondMarker(rx, ry, (cw * 1.25f).coerceAtLeast(3.1f), Color(0xFFE0B43E), Color(0xFF221606))
          drawCircle(Color(0xFFEFF0E4), (cw * 0.28f).coerceAtLeast(0.85f), Offset(rx - 0.6f, ry - 0.6f))
        }
        ResourceNodeType.STONE_QUARRY -> {
          drawDiamondMarker(rx, ry, (cw * 1.12f).coerceAtLeast(2.8f), Color(0xFFC5CCD0), Color(0xFF1A1D20))
          drawCircle(Color(0xAAEAEFF2), (cw * 0.24f).coerceAtLeast(0.75f), Offset(rx - 0.5f, ry - 0.5f))
        }
        ResourceNodeType.BERRY_BUSH -> {
          drawCircle(Color(0xFF1A0D0A), 2.4f, Offset(rx, ry))
          drawCircle(Color(0xFFB8322E), 1.65f, Offset(rx, ry))
        }
        ResourceNodeType.DEER -> {
          drawCircle(Color(0xFFBA7C42), 1.45f, Offset(rx, ry))
        }
      }
    }

    // 3. Buildings & Furrowed Permanent Farms
    for (b in world.buildings) {
      if (b.faction != Faction.PLAYER && world.buildingMaxFog(b) == 0) continue
      val col =
        when (b.faction) {
          Faction.PLAYER -> Color(0xFF3370BF)
          Faction.ENEMY -> Color(0xFFBF3633)
          Faction.ENEMY_2 -> Color(0xFF288C4B)
          Faction.ENEMY_3 -> Color(0xFF8540A8)
        }
      val bw = (b.footprint * cw).coerceAtLeast(3.8f)
      val bh = (b.footprint * ch).coerceAtLeast(3.8f)
      val bx = b.tileX * cw
      val by = b.tileY * ch

      if (b.type == BuildingType.FARM) {
        drawRect(Color(0xFF332111), Offset(bx - 0.6f, by - 0.6f), Size(bw + 1.2f, bh + 1.2f))
        drawRect(
          if (b.isComplete) Color(0xFFB58A38) else Color(0xFF6B4C26),
          Offset(bx, by),
          Size(bw, bh),
        )
        drawLine(Color(0xFF593A18), Offset(bx + 0.5f, by + bh * 0.35f), Offset(bx + bw - 0.5f, by + bh * 0.35f), strokeWidth = 0.8f)
        drawLine(Color(0xFF593A18), Offset(bx + 0.5f, by + bh * 0.70f), Offset(bx + bw - 0.5f, by + bh * 0.70f), strokeWidth = 0.8f)
        drawRect(col, Offset(bx, by), Size((bw * 0.38f).coerceAtLeast(1.8f), (bh * 0.38f).coerceAtLeast(1.8f)))
      } else {
        drawRect(Color(0xE00A0806), Offset(bx - 1.1f, by - 1.1f), Size(bw + 2.2f, bh + 2.2f))
        drawRect(col, Offset(bx, by), Size(bw, bh))
        drawLine(Color(0x66FFFFFF), Offset(bx, by), Offset(bx + bw, by), strokeWidth = 0.9f)
        drawLine(Color(0x66FFFFFF), Offset(bx, by), Offset(bx, by + bh), strokeWidth = 0.9f)
        if (b.type == BuildingType.TOWN_CENTER || b.type == BuildingType.CASTLE) {
          val pipW = (bw * 0.46f).coerceAtLeast(2.4f)
          val pipH = (bh * 0.46f).coerceAtLeast(2.4f)
          drawRect(Color(0xFFE5C466), Offset(bx + (bw - pipW) * 0.5f, by + (bh - pipH) * 0.5f), Size(pipW, pipH))
        } else if (b.type == BuildingType.AIRFIELD) {
          drawLine(Color(0xFFEBE4D0), Offset(bx + 1f, by + bh - 1.2f), Offset(bx + bw - 1f, by + 1.2f), strokeWidth = 1.2f)
        } else if (b.type == BuildingType.WAR_FACTORY) {
          drawRect(Color(0xFFE0B446), Offset(bx + bw * 0.25f, by + bh * 0.25f), Size((bw * 0.5f).coerceAtLeast(2.0f), (bh * 0.5f).coerceAtLeast(2.0f)))
        }
      }
    }

    // 4. Units (dark outline + clear 4-faction blip / aircraft chevron)
    for (u in world.units) {
      if (u.faction != Faction.PLAYER && world.fogAt(u.x.toInt(), u.y.toInt()) != 2) continue
      val ux = u.x * cw
      val uy = u.y * ch
      val uDef = GameDefinitions.unitDefs.getValue(u.type)
      val unitColor =
        when (u.faction) {
          Faction.PLAYER -> Color(0xFF5D9CE8)
          Faction.ENEMY -> Color(0xFFE0524F)
          Faction.ENEMY_2 -> Color(0xFF4DD47D)
          Faction.ENEMY_3 -> Color(0xFFC37AF0)
        }
      if (uDef.isFlying) {
        val dirSign = if (u.facingRight) 1f else -1f
        val r = 3.1f
        val jet = Path().apply {
          moveTo(ux + dirSign * r, uy)
          lineTo(ux - dirSign * r * 0.8f, uy - r * 0.75f)
          lineTo(ux - dirSign * r * 0.35f, uy)
          lineTo(ux - dirSign * r * 0.8f, uy + r * 0.75f)
          close()
        }
        drawPath(jet, Color(0xEE080604), style = Stroke(width = 1.6f))
        val jetColor =
          when (u.faction) {
            Faction.PLAYER -> Color(0xFF9CD4FF)
            Faction.ENEMY -> Color(0xFFFF8F8A)
            Faction.ENEMY_2 -> Color(0xFF8CFFAE)
            Faction.ENEMY_3 -> Color(0xFFE1B2FF)
          }
        drawPath(jet, jetColor)
      } else {
        val rad =
          when {
            uDef.isArmored -> 2.6f
            uDef.isCavalry || u.type.name == "BATTERING_RAM" -> 2.4f
            else -> 1.85f
          }
        drawCircle(Color(0xEE080604), rad + 0.9f, Offset(ux, uy))
        drawCircle(unitColor, rad, Offset(ux, uy))
      }
    }

    // 5. Compass Rose Edge Pips + Gold-Cornered Camera Viewport Reticle
    val midX = size.width * 0.5f
    val midY = size.height * 0.5f
    val tickCol = Color(0xAAD4AC54)
    drawLine(tickCol, Offset(midX, 0f), Offset(midX, 5f), 1.5f)
    drawLine(tickCol, Offset(midX, size.height - 5f), Offset(midX, size.height), 1.5f)
    drawLine(tickCol, Offset(0f, midY), Offset(5f, midY), 1.5f)
    drawLine(tickCol, Offset(size.width - 5f, midY), Offset(size.width, midY), 1.5f)

    val viewSpanW = (13.5f / camera.zoom) * cw
    val viewSpanH = (11.5f / camera.zoom) * ch
    val vx = (camera.centerWorldX * cw - viewSpanW * 0.5f).coerceIn(1f, (size.width - viewSpanW).coerceAtLeast(1f))
    val vy = (camera.centerWorldY * ch - viewSpanH * 0.5f).coerceIn(1f, (size.height - viewSpanH).coerceAtLeast(1f))
    val vw = viewSpanW.coerceAtMost(size.width - 2f)
    val vh = viewSpanH.coerceAtMost(size.height - 2f)

    drawRect(Color(0x16FFE794), Offset(vx, vy), Size(vw, vh))
    drawRect(Color(0xBBE0BC67), Offset(vx, vy), Size(vw, vh), style = Stroke(width = 1.0f))

    val bracket = 6.0f.coerceAtMost(vw * 0.35f).coerceAtMost(vh * 0.35f)
    val bColor = Color(0xFFF5E9C8)
    val sw = 1.9f
    drawLine(bColor, Offset(vx, vy), Offset(vx + bracket, vy), sw)
    drawLine(bColor, Offset(vx, vy), Offset(vx, vy + bracket), sw)
    drawLine(bColor, Offset(vx + vw - bracket, vy), Offset(vx + vw, vy), sw)
    drawLine(bColor, Offset(vx + vw, vy), Offset(vx + vw, vy + bracket), sw)
    drawLine(bColor, Offset(vx, vy + vh - bracket), Offset(vx, vy + vh), sw)
    drawLine(bColor, Offset(vx, vy + vh), Offset(vx + bracket, vy + vh), sw)
    drawLine(bColor, Offset(vx + vw - bracket, vy + vh), Offset(vx + vw, vy + vh), sw)
    drawLine(bColor, Offset(vx + vw, vy + vh - bracket), Offset(vx + vw, vy + vh), sw)
  }
}

private class MinimapPixelBuffer(private val dim: Int) {
  private val bitmap: Bitmap = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888)
  var imageBitmap = bitmap.asImageBitmap()
    private set
  private val pixels = IntArray(dim * dim)
  private var lastSignature: Long = Long.MIN_VALUE

  fun renderTerrain(world: GameWorld) {
    var sig = world.mapType.ordinal.toLong() * 1315423911L + world.mapSize
    val fogArr = world.fogGrid
    for (i in fogArr.indices) {
      sig = sig * 31L + fogArr[i]
    }
    if (sig == lastSignature) return
    lastSignature = sig

    val mapSz = world.mapSize
    for (ty in 0 until mapSz) {
      for (tx in 0 until mapSz) {
        val fog = world.fogAt(tx, ty)
        val terrain = world.tileAt(tx, ty)
        val adjWater =
          terrain == TileTerrain.DIRT_ROAD &&
            (world.tileAt(tx - 1, ty) == TileTerrain.WATER ||
              world.tileAt(tx + 1, ty) == TileTerrain.WATER ||
              world.tileAt(tx, ty - 1) == TileTerrain.WATER ||
              world.tileAt(tx, ty + 1) == TileTerrain.WATER)
        val nearLand =
          terrain == TileTerrain.WATER &&
            (world.tileAt(tx - 1, ty) != TileTerrain.WATER ||
              world.tileAt(tx + 1, ty) != TileTerrain.WATER ||
              world.tileAt(tx, ty - 1) != TileTerrain.WATER ||
              world.tileAt(tx, ty + 1) != TileTerrain.WATER)
        val inCliffShadow =
          terrain != TileTerrain.CLIFF &&
            (world.tileAt(tx - 1, ty) == TileTerrain.CLIFF || world.tileAt(tx, ty - 1) == TileTerrain.CLIFF)

        val baseInt =
          when {
            fog == 0 -> 0xFF08070B.toInt()
            terrain == TileTerrain.GRASS ->
              when (world.mapType) {
                MapType.ARABIA -> 0xFF8C7B45.toInt()
                MapType.BLACK_FOREST -> 0xFF283D1F.toInt()
                MapType.COASTAL -> 0xFF3E5428.toInt()
              }
            terrain == TileTerrain.GRASS_ALT ->
              when (world.mapType) {
                MapType.ARABIA -> 0xFF9C884F.toInt()
                MapType.BLACK_FOREST -> 0xFF324824.toInt()
                MapType.COASTAL -> 0xFF49622F.toInt()
              }
            adjWater -> 0xFF8F6C42.toInt()
            terrain == TileTerrain.DIRT_ROAD ->
              when (world.mapType) {
                MapType.ARABIA -> 0xFFB08956.toInt()
                MapType.BLACK_FOREST -> 0xFF63482E.toInt()
                MapType.COASTAL -> 0xFF876B45.toInt()
              }
            terrain == TileTerrain.WATER ->
              if (nearLand) 0xFF285C66.toInt() else 0xFF15374B.toInt()
            else -> if (world.mapType == MapType.ARABIA) 0xFF8E7454.toInt() else 0xFF63605A.toInt()
          }

        for (sy in 0 until SUB_TILE) {
          val py = ty * SUB_TILE + sy
          if (py >= dim) continue
          for (sx in 0 until SUB_TILE) {
            val px = tx * SUB_TILE + sx
            if (px >= dim) continue
            var col = baseInt
            if (fog != 0) {
              val grain = (((px * 73856093) xor (py * 19349663)) and 15) - 7
              var mul = 1f + grain * 0.011f
              if (terrain == TileTerrain.CLIFF) {
                mul *= if (sx + sy <= 1) 1.26f else if (sx + sy >= 3) 0.72f else 1.0f
              } else if (inCliffShadow && (sx == 0 || sy == 0)) {
                mul *= 0.68f
              } else if (terrain == TileTerrain.WATER && !nearLand && ((px + py) and 3) == 0) {
                mul *= 1.10f
              }
              if (fog == 1) mul *= 0.78f
              val r = (NativeColor.red(col) * mul).toInt().coerceIn(0, 255)
              val g = (NativeColor.green(col) * mul).toInt().coerceIn(0, 255)
              val b = (NativeColor.blue(col) * mul).toInt().coerceIn(0, 255)
              col = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
            pixels[py * dim + px] = col
          }
        }
      }
    }
    bitmap.setPixels(pixels, 0, dim, 0, 0, dim, dim)
    bitmap.prepareToDraw()
    imageBitmap = bitmap.asImageBitmap()
  }
}

private fun DrawScope.drawDiamondMarker(
  cx: Float,
  cy: Float,
  radius: Float,
  fill: Color,
  outline: Color,
) {
  val path =
    Path().apply {
      moveTo(cx, cy - radius)
      lineTo(cx + radius, cy)
      lineTo(cx, cy + radius)
      lineTo(cx - radius, cy)
      close()
    }
  drawPath(path, fill)
  drawPath(path, outline, style = Stroke(width = 1.0f))
}
