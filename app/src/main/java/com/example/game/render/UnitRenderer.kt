package com.example.game.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.game.model.CameraState
import com.example.game.model.Faction
import com.example.game.model.GameDefinitions
import com.example.game.model.ResourceType
import com.example.game.model.Unit
import com.example.game.model.UnitOrderType
import com.example.game.model.UnitType
import com.example.game.render.AtmosphereRenderUtils.drawSoftEllipticalShadow
import com.example.game.render.AtmosphereRenderUtils.drawSoftRadialOval
import com.example.game.render.AtmosphereRenderUtils.drawSoftVolumetricPuff
import com.example.game.render.BuildingRenderer.drawHpBar
import kotlin.math.abs
import kotlin.math.sin

object UnitRenderer {

  fun DrawScope.drawUnit(
    u: Unit,
    camera: CameraState,
    isSelected: Boolean,
  ) {
    val pos = IsometricMath.worldToScreen(u.x, u.y, camera, size.width, size.height)
    val z = camera.zoom
    val uDef = GameDefinitions.unitDefs.getValue(u.type)
    val dir = if (u.facingRight) 1f else -1f
    val bob =
      when {
        uDef.isFlying -> sin(u.animTime * 1.8f) * 3.8f * z
        uDef.isArmored -> if (u.isMoving) abs(sin(u.animTime * 2f)) * 1.4f * z else 0f
        u.isMoving -> abs(sin(u.animTime)) * 4.5f * z
        else -> 0f
      }
    val elevation = if (uDef.isFlying) 44f * z else 0f
    val chopLunge =
      if (u.isAttackingAnim && !uDef.isFlying && !uDef.isArmored) {
        sin(u.animTime * 1.4f) * 2.8f * dir * z
      } else {
        0f
      }

    if (u.isMoving && !uDef.isFlying) {
      val trailLen = if (uDef.isArmored) 2 else 1
      for (tIdx in 0..trailLen) {
        val ph = ((u.animTime * 2.2f + tIdx * 0.35f) % 1f)
        val dustR =
          if (uDef.isArmored) (4.2f + ph * 6.5f) * z
          else (2.8f + ph * 3.8f) * z
        drawSoftVolumetricPuff(
          cx = pos.x - dir * ((if (uDef.isArmored) 16f else 7f) + ph * 13f) * z,
          cy = pos.y + (1.0f - ph * 4.0f) * z,
          radius = dustR,
          color = if (uDef.isArmored) Color(0xFF8C7B63) else Color(0xFFA29177),
          alpha = (1f - ph) * 0.44f,
        )
      }
    }

    if (uDef.isFlying) {
      // Feathered golden-hour projected shadow beneath airborne aircraft
      val shadowW = if (u.type == UnitType.BOMBER_PLANE) 72f * z else 56f * z
      drawSoftEllipticalShadow(
        cx = pos.x + 8f * z,
        cy = pos.y + 3f * z,
        rx = shadowW * 0.60f,
        ry = shadowW * 0.25f,
        coreAlpha = 0.42f,
      )
      if (u.type == UnitType.GUNSHIP) {
        // Soft radial rotor downwash dust bloom on ground beneath gunship
        val washR = (22f + 5f * sin(u.animTime * 6f)) * z
        drawSoftRadialOval(
          cx = pos.x,
          cy = pos.y,
          rx = washR * 1.35f,
          ry = washR * 0.62f,
          centerColor = Color(0x32B8A688),
          midColor = Color(0x1AC8B99D),
        )
        drawOval(
          color = Color(0x2EA8967A),
          topLeft = Offset(pos.x - washR, pos.y - washR * 0.45f),
          size = Size(washR * 2f, washR * 0.9f),
          style = Stroke(width = 1.4f * z),
        )
      } else {
        // Wingtip vapor contrails & soft feathered jet afterburner exhaust bloom
        val tailX = pos.x - dir * 32f * z
        val airY = pos.y - elevation - bob - 38f * z
        drawLine(
          color = Color(0x48EAF4F8),
          start = Offset(tailX, airY - 12f * z),
          end = Offset(tailX - dir * 40f * z, airY - 12f * z),
          strokeWidth = 1.7f * z,
        )
        drawLine(
          color = Color(0x48EAF4F8),
          start = Offset(tailX, airY + 12f * z),
          end = Offset(tailX - dir * 40f * z, airY + 12f * z),
          strokeWidth = 1.7f * z,
        )
        val flamePulse = 1.0f + 0.38f * sin(u.animTime * 19f)
        drawSoftRadialOval(
          cx = tailX - dir * (6f * z * flamePulse),
          cy = airY,
          rx = 8.5f * z * flamePulse,
          ry = 4.5f * z,
          centerColor = Color(0xFFFFB23E).copy(alpha = 0.85f),
          midColor = Color(0xFFFF5C14).copy(alpha = 0.42f),
        )
        drawCircle(Color(0xF6FFF8D4), 2.5f * z, Offset(tailX - dir * 3.5f * z, airY))
      }
    }

    if (!uDef.isFlying) {
      val footW =
        when {
          uDef.isArmored -> 56f * z
          u.type == UnitType.KNIGHT || u.type == UnitType.SCOUT_CAVALRY || u.type == UnitType.BATTERING_RAM -> 46f * z
          else -> 29f * z
        }
      drawSoftEllipticalShadow(
        cx = pos.x + footW * 0.24f,
        cy = pos.y + footW * 0.05f,
        rx = footW * 0.64f,
        ry = footW * 0.24f,
        coreAlpha = 0.34f,
      )
      drawSoftEllipticalShadow(
        cx = pos.x + footW * 0.06f,
        cy = pos.y + footW * 0.02f,
        rx = footW * 0.42f,
        ry = footW * 0.17f,
        coreAlpha = 0.58f,
      )
    }

    if (isSelected) {
      val ringW =
        when {
          uDef.isFlying || uDef.isArmored -> 56f * z
          u.type == UnitType.KNIGHT || u.type == UnitType.SCOUT_CAVALRY || u.type == UnitType.BATTERING_RAM -> 48f * z
          else -> 34f * z
        }
      drawOval(
        color = Color(0x287CB852),
        topLeft = Offset(pos.x - ringW * 0.5f, pos.y - ringW * 0.25f),
        size = Size(ringW, ringW * 0.5f),
      )
      drawOval(
        color = Color(0xD89CE068),
        topLeft = Offset(pos.x - ringW * 0.5f, pos.y - ringW * 0.25f),
        size = Size(ringW, ringW * 0.5f),
        style = Stroke(width = 2.3f * z),
      )
    } else if (u.faction == Faction.PLAYER && u.type == UnitType.VILLAGER && u.order == UnitOrderType.IDLE) {
      drawOval(
        color = Color(0xA6D9AC4A),
        topLeft = Offset(pos.x - 16f * z, pos.y - 8f * z),
        size = Size(32f * z, 16f * z),
        style = Stroke(width = 1.8f * z),
      )
    } else if (u.faction.isAi) {
      // Subtle faction allegiance ground ring so multi-AI skirmishes are effortless to read in melee piles
      val ringW = if (uDef.isArmored || uDef.isFlying) 44f * z else 28f * z
      drawOval(
        color = Color(u.faction.bannerHex).copy(alpha = 0.45f),
        topLeft = Offset(pos.x - ringW * 0.5f, pos.y - ringW * 0.24f),
        size = Size(ringW, ringW * 0.48f),
        style = Stroke(width = 1.4f * z),
      )
    }

    val walkFrame = if (u.isMoving) ((u.animTime * 2.4f).toInt() and 1) else 0
    val sprite = SpriteAtlas.getUnitSprite(u.type, u.faction, u.facingRight, walkFrame)
    val baseLogical =
      when {
        uDef.isFlying || u.type == UnitType.HEAVY_TANK || u.type == UnitType.ROCKET_LAUNCHER -> 108f
        uDef.isArmored -> 102f
        else -> 90f
      }
    val dstSize = (baseLogical * z).toInt().coerceAtLeast(1)
    val spriteTop = (pos.y - dstSize * 0.85f - elevation - bob).toInt()
    drawImage(
      image = sprite,
      dstOffset = IntOffset((pos.x - dstSize * 0.5f + chopLunge).toInt(), spriteTop),
      dstSize = IntSize(dstSize, dstSize),
      filterQuality = FilterQuality.High,
    )

    if (u.type == UnitType.GUNSHIP) {
      val hubX = pos.x
      val hubY = spriteTop + dstSize * 0.34f
      val discRx = 39f * z
      val discRy = 11.5f * z
      drawOval(
        color = Color(0x32CFD8E0),
        topLeft = Offset(hubX - discRx, hubY - discRy),
        size = Size(discRx * 2f, discRy * 2f),
      )
      drawOval(
        color = Color(0x52E6EDF5),
        topLeft = Offset(hubX - discRx, hubY - discRy),
        size = Size(discRx * 2f, discRy * 2f),
        style = Stroke(width = 1.2f * z),
      )
      val ang = u.animTime * 14f
      val bx1 = kotlin.math.cos(ang) * discRx
      val by1 = sin(ang) * discRy
      drawLine(Color(0xC223282E), Offset(hubX - bx1, hubY - by1), Offset(hubX + bx1, hubY + by1), 2.3f * z)
      val bx2 = kotlin.math.cos(ang + 1.57f) * discRx
      val by2 = sin(ang + 1.57f) * discRy
      drawLine(Color(0xC223282E), Offset(hubX - bx2, hubY - by2), Offset(hubX + bx2, hubY + by2), 2.3f * z)
      drawCircle(Color(0xFFC5CFD8), 2.2f * z, Offset(hubX, hubY))
    }

    if (u.type == UnitType.VILLAGER && u.isAttackingAnim && (u.order == UnitOrderType.GATHER || u.order == UnitOrderType.BUILD)) {
      val burst = ((u.animTime * 2.2f) % 1f)
      val sx = pos.x + dir * (16f + burst * 9f) * z
      val sy = pos.y - (16f + (1f - burst) * 10f) * z
      val chipColor = if (u.order == UnitOrderType.BUILD) Color(0xFFFFD878) else Color(0xFFCFA266)
      drawCircle(chipColor.copy(alpha = (1f - burst) * 0.9f), 2.1f * z, Offset(sx, sy))
      drawCircle(chipColor.copy(alpha = (1f - burst) * 0.75f), 1.6f * z, Offset(sx + dir * 4f * z, sy + burst * 6f * z))
    }

    if (u.type == UnitType.VILLAGER && u.carryingAmount > 0 && u.carryingType != null) {
      val bx = pos.x + dir * 10f * z + chopLunge
      val by = pos.y - 20f * z - bob
      when (u.carryingType) {
        ResourceType.WOOD -> {
          drawRoundRect(
            color = Color(0xFF5D3A1F),
            topLeft = Offset(bx - 5.2f * z, by - 3.5f * z),
            size = Size(10.5f * z, 4.2f * z),
            cornerRadius = CornerRadius(1.8f * z, 1.8f * z),
          )
          drawRoundRect(
            color = Color(0xFF784E2C),
            topLeft = Offset(bx - 4.2f * z, by - 6.2f * z),
            size = Size(9.5f * z, 3.8f * z),
            cornerRadius = CornerRadius(1.8f * z, 1.8f * z),
          )
          drawCircle(Color(0xFFCFA972), 1.6f * z, Offset(bx + dir * 4.2f * z, by - 4.3f * z))
        }
        ResourceType.FOOD -> {
          drawOval(Color(0xFF6E4B28), Offset(bx - 5f * z, by - 2.5f * z), Size(10f * z, 6.2f * z))
          drawOval(Color(0xFF3E2610), Offset(bx - 5f * z, by - 2.5f * z), Size(10f * z, 6.2f * z), style = Stroke(width = 0.9f * z))
          drawLine(Color(0xFF9E7442), Offset(bx - 3.5f * z, by + 0.5f * z), Offset(bx + 3.5f * z, by + 0.5f * z), 0.9f * z)
          drawCircle(Color(0xFFA82C26), 2.2f * z, Offset(bx - 2f * z, by - 3.5f * z))
          drawCircle(Color(0xFFD4AC4E), 2.0f * z, Offset(bx + 2f * z, by - 3.8f * z))
          drawCircle(Color(0xFFFFF4C8), 0.7f * z, Offset(bx + 1.5f * z, by - 4.4f * z))
        }
        ResourceType.GOLD -> {
          drawCircle(Color(0xFF9C6D18), 4.2f * z, Offset(bx, by))
          drawCircle(Color(0xFFE0BC52), 2.9f * z, Offset(bx - 1.1f * z, by - 1.1f * z))
          drawCircle(Color(0xFFFFF5D4), 1.0f * z, Offset(bx - 1.8f * z, by - 1.9f * z))
        }
        ResourceType.STONE -> {
          drawRoundRect(
            color = Color(0xFF8C949B),
            topLeft = Offset(bx - 4.5f * z, by - 4.0f * z),
            size = Size(9.0f * z, 7.6f * z),
            cornerRadius = CornerRadius(1.2f * z, 1.2f * z),
          )
          drawRoundRect(
            color = Color(0x5524282C),
            topLeft = Offset(bx - 4.5f * z, by - 4.0f * z),
            size = Size(9.0f * z, 7.6f * z),
            cornerRadius = CornerRadius(1.2f * z, 1.2f * z),
            style = Stroke(width = 0.8f * z),
          )
        }
      }
    }

    if (isSelected || u.hp < u.maxHp) {
      val hpOffsetY = if (uDef.isFlying) 88f * z + bob else 54f * z
      drawHpBar(pos.x, pos.y - hpOffsetY, if (uDef.isArmored || uDef.isFlying) 44f * z else 36f * z, u.hp, u.maxHp, z)
    }
  }
}

