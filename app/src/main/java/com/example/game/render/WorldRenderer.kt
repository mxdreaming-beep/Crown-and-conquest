package com.example.game.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.game.engine.RtsEngine
import com.example.game.model.Building
import com.example.game.model.BuildingType
import com.example.game.model.CameraState
import com.example.game.model.Faction
import com.example.game.model.GameDefinitions
import com.example.game.model.GameWorld
import com.example.game.model.ProjectileKind
import com.example.game.model.ResourceNode
import com.example.game.model.Unit
import com.example.game.model.UnitOrderType
import com.example.game.model.VisualEffectType
import com.example.game.render.AtmosphereRenderUtils.drawSoftEllipticalShadow
import com.example.game.render.AtmosphereRenderUtils.drawSoftRadialOval
import com.example.game.render.AtmosphereRenderUtils.drawSoftVolumetricPuff
import com.example.game.render.BuildingRenderer.drawBuilding
import com.example.game.render.ResourceRenderer.drawResourceNode
import com.example.game.render.TerrainRenderer.drawIsometricTerrain
import com.example.game.render.UnitRenderer.drawUnit
import com.example.ui.theme.FrakturFontFamily
import kotlin.math.cos
import kotlin.math.sin

private sealed class DepthItem(val depth: Float) {
  class NodeItem(val node: ResourceNode, val isDimmed: Boolean) : DepthItem(node.x + node.y)
  class BuildingItem(val building: Building, val isDimmed: Boolean) : DepthItem(building.centerX + building.centerY)
  class UnitItem(val unit: Unit) : DepthItem(unit.x + unit.y)
}

object WorldRenderer {

  fun DrawScope.drawGameWorld(
    world: GameWorld,
    camera: CameraState,
    selectedUnitIds: Set<Int>,
    selectedBuildingId: Int?,
    pendingBuildType: BuildingType?,
    pendingBuildTile: Pair<Int, Int>?,
    selectionBox: Rect?,
    animClock: Float,
    textMeasurer: TextMeasurer,
    pendingMoveBuildingId: Int? = null,
  ) {
    val w = size.width
    val h = size.height
    val z = camera.zoom

    drawIsometricTerrain(world, camera, animClock)

    val items = ArrayList<DepthItem>(world.resources.size + world.buildings.size + world.units.size)
    for (r in world.resources) {
      val fog = world.fogAt(r.tileX, r.tileY)
      if (fog > 0) items += DepthItem.NodeItem(r, isDimmed = fog == 1)
    }
    for (b in world.buildings) {
      val fog = world.buildingMaxFog(b)
      if (b.faction == Faction.PLAYER || fog > 0) {
        items += DepthItem.BuildingItem(b, isDimmed = b.faction.isAi && fog == 1)
      }
    }
    for (u in world.units) {
      val fog = world.fogAt(u.x.toInt(), u.y.toInt())
      if (u.faction == Faction.PLAYER || fog == 2) items += DepthItem.UnitItem(u)
    }
    items.sortBy { it.depth }

    for (item in items) {
      when (item) {
        is DepthItem.NodeItem -> drawResourceNode(item.node, camera, item.isDimmed, animClock)
        is DepthItem.BuildingItem ->
          drawBuilding(
            b = item.building,
            camera = camera,
            isSelected = item.building.id == selectedBuildingId,
            isDimmed = item.isDimmed,
            animClock = animClock,
          )
        is DepthItem.UnitItem ->
          drawUnit(
            u = item.unit,
            camera = camera,
            isSelected = item.unit.id in selectedUnitIds,
          )
      }
    }

    // Rally point flag when a player building is selected
    val selB = selectedBuildingId?.let { id -> world.buildings.firstOrNull { it.id == id && it.faction == Faction.PLAYER } }
    if (selB != null && selB.type != BuildingType.FARM && selB.type != BuildingType.HOUSE) {
      val rp = IsometricMath.worldToScreen(selB.rallyX, selB.rallyY, camera, w, h)
      drawSoftEllipticalShadow(rp.x, rp.y, 7f * z, 3.2f * z, coreAlpha = 0.38f)
      drawLine(Color(0xFFDFD3BA), rp, Offset(rp.x, rp.y - 20f * z), 2.2f * z)
      drawCircle(Color(0xFFFFD866), 2.0f * z, Offset(rp.x, rp.y - 21f * z))
      val wave = sin(animClock * 5f) * 2.2f * z
      val flag = Path().apply {
        moveTo(rp.x, rp.y - 20f * z)
        lineTo(rp.x + 12f * z, rp.y - 15f * z + wave)
        lineTo(rp.x, rp.y - 10f * z)
        close()
      }
      drawPath(flag, Color(0xFFF2C964))
      drawPath(flag, Color(0x885C3A14), style = Stroke(width = 1.0f * z))
    }

    // Build queue path & step badges for selected villagers
    val buildingLookup = world.buildings.associateBy { it.id }
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f * z, 6f * z), -animClock * 18f * z)
    for (u in world.units) {
      if (u.id in selectedUnitIds && u.order == UnitOrderType.BUILD) {
        val routeIds = listOf(u.targetEntityId) + u.buildQueue
        val targets = routeIds.mapNotNull { buildingLookup[it] }
        if (targets.size > 1) {
          var prevPt = IsometricMath.worldToScreen(u.x, u.y, camera, w, h)
          for ((idx, bTarget) in targets.withIndex()) {
            val nextPt = IsometricMath.worldToScreen(bTarget.centerX, bTarget.centerY, camera, w, h)
            drawLine(
              color = Color(0xDDF2C964),
              start = prevPt,
              end = nextPt,
              strokeWidth = 2.0f * z,
              pathEffect = dashEffect,
            )
            if (idx > 0) {
              val badgeCenter = Offset(nextPt.x, nextPt.y - 26f * z)
              drawCircle(Color(0xDD1A1109), 9.5f * z, badgeCenter)
              drawCircle(Color(0xFFF2C964), 9.5f * z, badgeCenter, style = Stroke(width = 1.5f * z))
              val numLayout =
                textMeasurer.measure(
                  text = "${idx + 1}",
                  style = TextStyle(fontFamily = FrakturFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                )
              drawText(
                numLayout,
                color = Color(0xFFFFE49E),
                topLeft = Offset(badgeCenter.x - numLayout.size.width * 0.5f, badgeCenter.y - numLayout.size.height * 0.5f),
              )
            }
            prevPt = nextPt
          }
        }
      }
    }

    // Arrows, bolts, rifle tracers, heavy tank/artillery shells & air-to-ground missiles
    for (p in world.projectiles) {
      val fog = world.fogAt(p.targetX.toInt(), p.targetY.toInt())
      if (p.faction.isAi && fog < 2) continue
      val s0 = IsometricMath.worldToScreen(p.startX, p.startY, camera, w, h)
      val s1 = IsometricMath.worldToScreen(p.targetX, p.targetY, camera, w, h)
      val t = p.progress
      val maxArc =
        when (p.kind) {
          ProjectileKind.SNIPER_ROUND -> 3f
          ProjectileKind.BULLET -> 8f
          ProjectileKind.MISSILE -> 26f
          ProjectileKind.SHELL -> 62f
          ProjectileKind.ARROW, ProjectileKind.BOLT -> 50f
        }
      val liftOff =
        when (p.kind) {
          ProjectileKind.SNIPER_ROUND -> 28f
          ProjectileKind.BULLET -> 26f
          ProjectileKind.MISSILE -> 38f * (1f - t) + 12f
          ProjectileKind.SHELL -> 28f
          ProjectileKind.ARROW, ProjectileKind.BOLT -> 22f
        }
      val arcLift = sin(t * Math.PI.toFloat()) * maxArc * z
      val cx = s0.x + (s1.x - s0.x) * t
      val groundY = s0.y + (s1.y - s0.y) * t
      val cy = groundY - arcLift - liftOff * z
      val prevT = (t - 0.08f).coerceAtLeast(0f)
      val prevLift = sin(prevT * Math.PI.toFloat()) * maxArc * z
      val px = s0.x + (s1.x - s0.x) * prevT
      val py = s0.y + (s1.y - s0.y) * prevT - prevLift - liftOff * z
      val trailT = (t - 0.18f).coerceAtLeast(0f)
      val trailLift = sin(trailT * Math.PI.toFloat()) * maxArc * z
      val tx = s0.x + (s1.x - s0.x) * trailT
      val ty = s0.y + (s1.y - s0.y) * trailT - trailLift - liftOff * z

      when (p.kind) {
        ProjectileKind.SNIPER_ROUND -> {
          drawLine(Color(0x66D6F3FF), Offset(tx, ty), Offset(cx, cy), strokeWidth = 3.2f * z)
          drawLine(Color(0xFFE4FAFF), Offset(px, py), Offset(cx, cy), strokeWidth = 2.4f * z)
          drawCircle(Color(0xFFFFFFFF), 2.3f * z, Offset(cx, cy))
        }
        ProjectileKind.BULLET -> {
          drawLine(Color(0x88FFCC4D), Offset(tx, ty), Offset(cx, cy), strokeWidth = 2.0f * z)
          drawLine(Color(0xFFFFF8D6), Offset(px, py), Offset(cx, cy), strokeWidth = 2.6f * z)
          drawCircle(Color(0xFFFFFFFF), 1.9f * z, Offset(cx, cy))
        }
        ProjectileKind.SHELL -> {
          drawSoftEllipticalShadow(cx, groundY, 6.5f * z, 2.8f * z, coreAlpha = 0.36f)
          drawLine(Color(0x664A423A), Offset(tx, ty), Offset(px, py), strokeWidth = 4.2f * z)
          drawLine(Color(0xCCFF8F26), Offset(px, py), Offset(cx, cy), strokeWidth = 3.8f * z)
          drawCircle(Color(0xFF282D33), 4.0f * z, Offset(cx, cy))
          drawCircle(Color(0xFFFFEA8A), 2.1f * z, Offset(cx, cy))
        }
        ProjectileKind.MISSILE -> {
          drawSoftEllipticalShadow(cx, groundY, 7.0f * z, 3.0f * z, coreAlpha = 0.32f)
          drawLine(Color(0x88E8EEF2), Offset(tx, ty), Offset(px, py), strokeWidth = 4.8f * z)
          drawCircle(Color(0xEEFF8524), 4.2f * z, Offset(px, py))
          drawLine(Color(0xFFD9E2EA), Offset(px, py), Offset(cx, cy), strokeWidth = 3.4f * z)
          drawCircle(Color(0xFFE03E2E), 2.8f * z, Offset(cx, cy))
        }
        ProjectileKind.BOLT, ProjectileKind.ARROW -> {
          drawSoftEllipticalShadow(cx, groundY, 5.2f * z, 2.2f * z, coreAlpha = 0.32f)
          drawLine(Color(0x52FFF4CC), Offset(tx, ty), Offset(px, py), strokeWidth = 1.6f * z)
          val dx = cx - px
          val dy = cy - py
          val len = kotlin.math.hypot(dx, dy).coerceAtLeast(0.01f)
          val ux = dx / len
          val uy = dy / len
          val nx = -uy
          val ny = ux
          drawLine(
            color = if (p.isBolt) Color(0xFFFFD875) else Color(0xFF9C7240),
            start = Offset(px, py),
            end = Offset(cx, cy),
            strokeWidth = if (p.isBolt) 3.0f * z else 2.2f * z,
          )
          // Crisp feathered fletching at tail
          val fletchColor = if (p.isBolt) Color(0xFFFFEA8A) else Color(0xFFFAF3E3)
          drawLine(
            fletchColor,
            Offset(px, py),
            Offset(px - ux * 4.2f * z + nx * 2.5f * z, py - uy * 4.2f * z + ny * 2.5f * z),
            1.4f * z,
          )
          drawLine(
            fletchColor,
            Offset(px, py),
            Offset(px - ux * 4.2f * z - nx * 2.5f * z, py - uy * 4.2f * z - ny * 2.5f * z),
            1.4f * z,
          )
          // Directional broadhead tip
          val tip = Path().apply {
            moveTo(cx + ux * 3.8f * z, cy + uy * 3.8f * z)
            lineTo(cx - ux * 2.2f * z + nx * 2.4f * z, cy - uy * 2.2f * z + ny * 2.4f * z)
            lineTo(cx - ux * 2.2f * z - nx * 2.4f * z, cy - uy * 2.2f * z - ny * 2.4f * z)
            close()
          }
          drawPath(tip, Color(0xFFEAF2FA))
        }
      }
    }

    // Combat sparks, explosions, muzzle flashes, radial rays, billowing dust clouds & move pings
    for (fx in world.effects) {
      val s = IsometricMath.worldToScreen(fx.x, fx.y, camera, w, h)
      val p = (fx.ageSec / fx.durationSec).coerceIn(0f, 1f)
      val alpha = 1f - p
      when (fx.type) {
        VisualEffectType.MOVE_PING -> {
          val rad = (9f + p * 22f) * z
          drawOval(
            color = Color(0xFF95CF63).copy(alpha = alpha * 0.9f),
            topLeft = Offset(s.x - rad, s.y - rad * 0.5f),
            size = Size(rad * 2f, rad),
            style = Stroke(width = 2.3f * z),
          )
          val rad2 = (4f + p * 12f) * z
          drawOval(
            color = Color(0xFFD2EBB3).copy(alpha = alpha * 0.85f),
            topLeft = Offset(s.x - rad2, s.y - rad2 * 0.5f),
            size = Size(rad2 * 2f, rad2),
            style = Stroke(width = 1.5f * z),
          )
        }
        VisualEffectType.ATTACK_SPARK -> {
          val sy = s.y - 22f * z
          drawSoftVolumetricPuff(s.x, sy, (11f + p * 14f) * z, Color(0xFFE08C36), alpha * 0.52f)
          drawCircle(Color(0xFFFFFAF0).copy(alpha = alpha * 0.92f), (2.1f + p * 1.8f) * z, Offset(s.x, sy))
          for (ray in 0..5) {
            val ang = ray * 1.047f + 0.35f
            val inner = (3f + p * 7f) * z
            val outer = (9f + p * 16f) * z
            drawLine(
              color = Color(0xFFF5DF9C).copy(alpha = alpha * 0.88f),
              start = Offset(s.x + cos(ang) * inner, sy + sin(ang) * inner),
              end = Offset(s.x + cos(ang) * outer, sy + sin(ang) * outer),
              strokeWidth = 1.3f * z,
            )
          }
        }
        VisualEffectType.MUZZLE_FLASH -> {
          val sy = s.y - 28f * z
          drawSoftVolumetricPuff(s.x, sy - p * 11f * z, (14f + p * 18f) * z, Color(0xFF9E907C), alpha * 0.42f)
          drawSoftVolumetricPuff(s.x, sy, (12f + p * 10f) * z, Color(0xFFFFB238), alpha * 0.85f)
          drawCircle(Color(0xFFFFF8DC).copy(alpha = alpha * 0.96f), (4.4f + p * 2.8f) * z, Offset(s.x, sy))
          for (ray in 0..7) {
            val ang = ray * 0.785f
            drawLine(
              color = Color(0xFFFFE49E).copy(alpha = alpha * 0.88f),
              start = Offset(s.x, sy),
              end = Offset(s.x + cos(ang) * (14f + p * 10f) * z, sy + sin(ang) * (14f + p * 10f) * z),
              strokeWidth = 1.8f * z,
            )
          }
        }
        VisualEffectType.EXPLOSION -> {
          val sy = s.y - 16f * z
          val shockR = (12f + p * 46f) * z
          val outerShockR = (18f + p * 62f) * z
          // Feathered scorched earth crater + expanding blast pressure waves
          drawSoftEllipticalShadow(s.x, s.y, shockR * 0.72f, shockR * 0.34f, coreAlpha = alpha * 0.56f)
          drawOval(
            color = Color(0xFFE6C8A0).copy(alpha = alpha * 0.44f),
            topLeft = Offset(s.x - outerShockR, s.y - outerShockR * 0.5f),
            size = Size(outerShockR * 2f, outerShockR),
            style = Stroke(width = 1.8f * z),
          )
          drawOval(
            color = Color(0xFFFFB454).copy(alpha = alpha * 0.78f),
            topLeft = Offset(s.x - shockR, s.y - shockR * 0.5f),
            size = Size(shockR * 2f, shockR),
            style = Stroke(width = 3.0f * z),
          )
          drawSoftVolumetricPuff(s.x - 6f * z, sy - p * 19f * z, (22f + p * 36f) * z, Color(0xFF28221E), alpha * 0.72f)
          drawSoftVolumetricPuff(s.x + 8f * z, sy - p * 24f * z, (16f + p * 28f) * z, Color(0xFF473E36), alpha * 0.58f)
          drawSoftVolumetricPuff(s.x, sy, (18f + p * 22f) * z, Color(0xFFE8561A), alpha * 0.92f)
          drawSoftVolumetricPuff(s.x, sy, (10f + p * 12f) * z, Color(0xFFFFD863), alpha * 0.96f)
          drawCircle(Color(0xFFFFFAF0).copy(alpha = alpha * 0.99f), (4.4f + p * 3.0f) * z, Offset(s.x, sy))
          for (ember in 0..9) {
            val ang = ember * 0.628f + 0.25f
            val dist = (9f + p * 40f) * z
            drawCircle(
              Color(0xFFFFA836).copy(alpha = alpha),
              2.2f * z,
              Offset(s.x + cos(ang) * dist, sy + sin(ang) * dist * 0.7f - p * 14f * z),
            )
          }
        }
        VisualEffectType.DUST_PUFF -> {
          drawSoftVolumetricPuff(s.x - 8f * z, s.y - 16f * z - p * 18f * z, (15f + p * 30f) * z, Color(0xFF82725D), alpha * 0.52f)
          drawSoftVolumetricPuff(s.x + 6f * z, s.y - 20f * z - p * 24f * z, (11f + p * 22f) * z, Color(0xFFB3A28A), alpha * 0.62f)
          drawSoftVolumetricPuff(s.x - 1f * z, s.y - 24f * z - p * 28f * z, (7f + p * 15f) * z, Color(0xFFD4C6B2), alpha * 0.48f)
        }
      }
    }

    // Living wildlife: distant soaring hawks wheeling across clearings with faint ground shadows
    val zSky = z.coerceIn(0.65f, 1.45f)
    for (bIdx in 0..4) {
      val orbitT = animClock * (0.14f + (bIdx % 3) * 0.04f) + bIdx * 1.45f
      val wx = world.mapSize * (0.22f + 0.56f * ((bIdx * 0.37f) % 1f)) + cos(orbitT) * 7.5f
      val wy = world.mapSize * (0.24f + 0.52f * ((bIdx * 0.61f) % 1f)) + sin(orbitT * 0.8f) * 6.5f
      if (world.fogAt(wx.toInt().coerceIn(0, world.mapSize - 1), wy.toInt().coerceIn(0, world.mapSize - 1)) == 0) continue
      val gPos = IsometricMath.worldToScreen(wx, wy, camera, w, h)
      if (gPos.x < -60f || gPos.x > w + 60f || gPos.y < -60f || gPos.y > h + 60f) continue
      drawSoftEllipticalShadow(gPos.x + 18f * zSky, gPos.y + 12f * zSky, 7.5f * zSky, 3.2f * zSky, coreAlpha = 0.22f)
      val bx = gPos.x
      val by = gPos.y - 68f * zSky + sin(animClock * 1.6f + bIdx) * 4f * zSky
      val flap = sin(animClock * 7.5f + bIdx * 2.1f) * 3.8f * zSky
      val span = 6.5f * zSky
      val bird = Path().apply {
        moveTo(bx - span, by - flap)
        quadraticTo(bx - span * 0.45f, by - 2.6f * zSky, bx, by)
        quadraticTo(bx + span * 0.45f, by - 2.6f * zSky, bx + span, by - flap)
      }
      drawPath(bird, Color(0xD8241C16), style = Stroke(width = 1.6f * zSky))
    }

    // Feathered high-altitude cumulus cloud shadows drifting naturally across the landscape
    val wrapW = (w + 380f).coerceAtLeast(420f)
    val wrapH = (h + 280f).coerceAtLeast(320f)
    for (cIdx in 0..2) {
      val cxCloud = ((cIdx * 360f + animClock * (14f + cIdx * 3f)) % wrapW) - 180f
      val cyCloud = ((cIdx * 220f + animClock * 4.5f) % wrapH) - 110f
      drawSoftRadialOval(
        cx = cxCloud,
        cy = cyCloud,
        rx = (165f + cIdx * 30f) * z.coerceIn(0.75f, 1.35f),
        ry = (82f + cIdx * 16f) * z.coerceIn(0.75f, 1.35f),
        centerColor = Color(0x1E06070C),
      )
    }

    // Subtle slanting golden-hour atmospheric sunlight glow & feathered sun shafts
    drawRect(
      brush =
        Brush.linearGradient(
          colors = listOf(Color(0x1DF5DA9E), Color(0x0EECCB88), Color(0x00000000), Color(0x2808070B)),
          start = Offset.Zero,
          end = Offset(w, h),
        ),
      size = size,
    )
    val shaftAlpha = 0.055f + 0.016f * sin(animClock * 0.6f)
    drawRect(
      brush =
        Brush.radialGradient(
          0.0f to Color(0xFFFFE6AA).copy(alpha = shaftAlpha),
          0.55f to Color(0xFFFFE099).copy(alpha = shaftAlpha * 0.38f),
          1.0f to Color.Transparent,
          center = Offset(w * 0.18f, h * 0.08f),
          radius = (w.coerceAtLeast(h) * 0.72f).coerceAtLeast(100f),
        ),
      size = size,
    )

    // Subtle glowing pollen & airborne sunlight motes
    for (i in 0 until 18) {
      val mx = ((i * 197 + animClock * (11f + (i % 4) * 4f)) % w.coerceAtLeast(100f))
      val my = ((i * 131 + sin(animClock * 0.75f + i) * 24f) % h.coerceAtLeast(100f))
      val pulse = 0.18f + 0.18f * sin(animClock * 2.2f + i * 1.3f)
      val moteCol = if (i % 4 == 0) Color(0xFFEBD28E) else Color(0xFFF2EAD4)
      drawSoftVolumetricPuff(mx, my, (2.6f + (i % 3) * 0.7f) * z.coerceAtLeast(0.8f), moteCol, pulse)
    }

    for (ft in world.floatingTexts) {
      val s = IsometricMath.worldToScreen(ft.x, ft.y, camera, w, h)
      val rise = ft.ageSec * 36f * z
      val alpha = (1f - ft.ageSec / 1.3f).coerceIn(0f, 1f)
      val layout =
        textMeasurer.measure(
          text = ft.text,
          style = TextStyle(fontFamily = FrakturFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Normal),
        )
      val topLeft = Offset(s.x - layout.size.width * 0.5f, s.y - 64f * z - rise)
      drawText(layout, color = Color.Black.copy(alpha = alpha * 0.85f), topLeft = Offset(topLeft.x + 1.5f, topLeft.y + 1.5f))
      drawText(layout, color = Color(ft.colorHex).copy(alpha = alpha), topLeft = topLeft)
    }

    val movingB =
      pendingMoveBuildingId?.let { moveId ->
        world.buildings.firstOrNull { it.id == moveId && it.faction == Faction.PLAYER }
      }
    if (movingB != null && pendingBuildTile != null) {
      val fp = movingB.footprint
      val (tx, ty) = pendingBuildTile
      val valid = RtsEngine.canPlaceBuilding(world, movingB.type, tx, ty, ignoreBuildingId = movingB.id)
      val origCenter = IsometricMath.worldToScreen(movingB.centerX, movingB.centerY, camera, w, h)
      val newCenter = IsometricMath.worldToScreen(tx + fp * 0.5f, ty + fp * 0.5f, camera, w, h)
      drawLine(
        color = Color(0xEEF2C964),
        start = origCenter,
        end = newCenter,
        strokeWidth = 2.4f * z,
        pathEffect = dashEffect,
      )
      val pTop = IsometricMath.worldToScreen(tx.toFloat(), ty.toFloat(), camera, w, h)
      val pRight = IsometricMath.worldToScreen((tx + fp).toFloat(), ty.toFloat(), camera, w, h)
      val pBottom = IsometricMath.worldToScreen((tx + fp).toFloat(), (ty + fp).toFloat(), camera, w, h)
      val pLeft = IsometricMath.worldToScreen(tx.toFloat(), (ty + fp).toFloat(), camera, w, h)
      val ghostPath = Path().apply { moveTo(pTop.x, pTop.y); lineTo(pRight.x, pRight.y); lineTo(pBottom.x, pBottom.y); lineTo(pLeft.x, pLeft.y); close() }
      drawPath(ghostPath, if (valid) Color(0x526EB848) else Color(0x52C9443E))
      drawPath(ghostPath, if (valid) Color(0xFF95CF63) else Color(0xFFE05952), style = Stroke(width = 2.4f * z))
    } else if (pendingBuildType != null && pendingBuildTile != null) {
      val fp = GameDefinitions.buildingDefs.getValue(pendingBuildType).footprintTiles
      val (tx, ty) = pendingBuildTile
      val valid = RtsEngine.canPlaceBuilding(world, pendingBuildType, tx, ty)
      val pTop = IsometricMath.worldToScreen(tx.toFloat(), ty.toFloat(), camera, w, h)
      val pRight = IsometricMath.worldToScreen((tx + fp).toFloat(), ty.toFloat(), camera, w, h)
      val pBottom = IsometricMath.worldToScreen((tx + fp).toFloat(), (ty + fp).toFloat(), camera, w, h)
      val pLeft = IsometricMath.worldToScreen(tx.toFloat(), (ty + fp).toFloat(), camera, w, h)
      val ghostPath = Path().apply { moveTo(pTop.x, pTop.y); lineTo(pRight.x, pRight.y); lineTo(pBottom.x, pBottom.y); lineTo(pLeft.x, pLeft.y); close() }
      drawPath(ghostPath, if (valid) Color(0x526EB848) else Color(0x52C9443E))
      drawPath(ghostPath, if (valid) Color(0xFF95CF63) else Color(0xFFE05952), style = Stroke(width = 2.4f * z))
    }

    if (selectionBox != null) {
      drawRect(color = Color(0x227CB852), topLeft = selectionBox.topLeft, size = selectionBox.size)
      drawRect(color = Color(0xFF95CF63), topLeft = selectionBox.topLeft, size = selectionBox.size, style = Stroke(width = 1.8f))
    }
  }
}
