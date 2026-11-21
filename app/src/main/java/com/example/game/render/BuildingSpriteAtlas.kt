package com.example.game.render

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.game.model.BuildingType
import com.example.game.model.Faction
import com.example.game.model.GameDefinitions

object BuildingSpriteAtlas {

  const val SUPERSAMPLE = 4f
  private const val MAX_CACHED_BUILDINGS = 16
  private val cache =
    object : LinkedHashMap<Pair<BuildingType, Faction>, ImageBitmap>(24, 0.75f, true) {
      override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<BuildingType, Faction>, ImageBitmap>?): Boolean =
        size > MAX_CACHED_BUILDINGS
    }

  fun getBuildingSprite(type: BuildingType, faction: Faction): ImageBitmap {
    return cache.getOrPut(type to faction) { renderBuilding(type, faction).asImageBitmap() }
  }

  private fun renderBuilding(type: BuildingType, faction: Faction): Bitmap {
    val fp = GameDefinitions.buildingDefs.getValue(type).footprintTiles
    val lw = if (fp >= 3) 420f else if (fp == 2) 296f else 196f
    val lh = if (fp >= 3) 380f else if (fp == 2) 270f else 232f
    val bmp = Bitmap.createBitmap((lw * SUPERSAMPLE).toInt(), (lh * SUPERSAMPLE).toInt(), Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    c.scale(SUPERSAMPLE, SUPERSAMPLE)

    val halfW = fp * 74f
    val halfH = fp * 37f
    val cx = lw * 0.5f
    val baseCenterY = lh - halfH - 18f

    val pTopX = cx; val pTopY = baseCenterY - halfH
    val pRightX = cx + halfW; val pRightY = baseCenterY
    val pBotX = cx; val pBotY = baseCenterY + halfH
    val pLeftX = cx - halfW; val pLeftY = baseCenterY

    val (fMain, fDark) =
      when (faction) {
        Faction.PLAYER -> 0xFF28558F.toInt() to 0xFF142B4A.toInt()
        Faction.ENEMY -> 0xFF9C2926.toInt() to 0xFF4F1312.toInt()
        Faction.ENEMY_2 -> 0xFF246E3C.toInt() to 0xFF10361C.toInt()
        Faction.ENEMY_3 -> 0xFF6B2D82.toInt() to 0xFF341442.toInt()
      }

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = 0x7A080705
      maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
    }
    val shadowPath = Path().apply {
      moveTo(pTopX + 20f, pTopY + 10f)
      lineTo(pRightX + 24f, pRightY + 10f)
      lineTo(pBotX + 22f, pBotY + 11f)
      lineTo(pLeftX + 12f, pLeftY + 9f)
      close()
    }
    c.drawPath(shadowPath, shadowPaint)

    val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val aoEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 0.55f
      color = 0x4E120E09
    }

    val stoneS by lazy { ProceduralTextures.newScaledShader(ProceduralTextures.stoneBitmap, 0.19f) }
    val plasterS by lazy { ProceduralTextures.newScaledShader(ProceduralTextures.plasterBitmap, 0.22f) }
    val thatchS by lazy { ProceduralTextures.newScaledShader(ProceduralTextures.thatchBitmap, 0.20f) }
    val slateS by lazy { ProceduralTextures.newScaledShader(ProceduralTextures.slateBitmap, 0.18f) }
    val timberS by lazy { ProceduralTextures.newScaledShader(ProceduralTextures.timberBitmap, 0.20f) }
    val dirtS by lazy { ProceduralTextures.newScaledShader(ProceduralTextures.dirtBitmap, 0.22f) }
    val plowedS by lazy { ProceduralTextures.newScaledShader(ProceduralTextures.plowedSoilBitmap, 0.20f) }
    val concreteS by lazy { ProceduralTextures.newScaledShader(ProceduralTextures.concreteBitmap, 0.22f) }
    val brickS by lazy { ProceduralTextures.newScaledShader(ProceduralTextures.brickBitmap, 0.20f) }

    fun drawCourtyardApron(shader: Shader, insetRatio: Float = 0.0f) {
      val apron = Path().apply {
        moveTo(cx + (pTopX - cx) * (1f - insetRatio), baseCenterY + (pTopY - baseCenterY) * (1f - insetRatio))
        lineTo(cx + (pRightX - cx) * (1f - insetRatio), baseCenterY + (pRightY - baseCenterY) * (1f - insetRatio))
        lineTo(cx + (pBotX - cx) * (1f - insetRatio), baseCenterY + (pBotY - baseCenterY) * (1f - insetRatio))
        lineTo(cx + (pLeftX - cx) * (1f - insetRatio), baseCenterY + (pLeftY - baseCenterY) * (1f - insetRatio))
        close()
      }
      p.style = Paint.Style.FILL
      p.shader = shader
      c.drawPath(apron, p)
      p.shader = null
      if (insetRatio > 0.05f) {
        c.drawPath(apron, aoEdge)
      } else {
        // Organic edge border stones & weathered soil fringe instead of rigid outer diamond outline
        for ((bx, by) in listOf(
          (pLeftX + pBotX) * 0.5f to (pLeftY + pBotY) * 0.5f,
          (pBotX + pRightX) * 0.5f to (pBotY + pRightY) * 0.5f,
          pLeftX + 12f to pLeftY + 4f,
          pBotX - 10f to pBotY - 3f,
        )) {
          p.color = 0x552E2418; c.drawOval(RectF(bx - 5f, by - 2.2f, bx + 5f, by + 2.2f), p)
          p.color = 0x99736A5C.toInt(); c.drawOval(RectF(bx - 3.5f, by - 2.0f, bx + 3.5f, by + 1.5f), p)
        }
      }
    }

    fun drawStoneQuoins(x: Float, yBottom: Float, wallH: Float) {
      p.style = Paint.Style.FILL
      var qy = yBottom - 4f
      var idx = 0
      while (qy > yBottom - wallH + 6f) {
        val qw = if (idx % 2 == 0) 6.6f else 4.4f
        p.color = if (idx % 2 == 0) 0xFFA69B8A.toInt() else 0xFF7B7264.toInt()
        c.drawRect(x - qw, qy - 5.2f, x + qw * 0.72f, qy, p)
        c.drawRect(x - qw, qy - 5.2f, x + qw * 0.72f, qy, aoEdge)
        qy -= 7.4f
        idx++
      }
    }

    fun drawIsoBox(
      tx: Float, ty: Float, rx: Float, ry: Float, bx: Float, by: Float, lx: Float, ly: Float,
      wallH: Float,
      wallShader: Shader,
      stonePlinthH: Float = 0f,
      quoins: Boolean = false,
    ) {
      val leftWall = Path().apply { moveTo(lx, ly); lineTo(bx, by); lineTo(bx, by - wallH); lineTo(lx, ly - wallH); close() }
      val rightWall = Path().apply { moveTo(bx, by); lineTo(rx, ry); lineTo(rx, ry - wallH); lineTo(bx, by - wallH); close() }

      p.style = Paint.Style.FILL
      p.shader = wallShader
      c.drawPath(leftWall, p)
      c.drawPath(rightWall, p)
      p.shader = null

      if (stonePlinthH > 0f) {
        val plL = Path().apply { moveTo(lx, ly); lineTo(bx, by); lineTo(bx, by - stonePlinthH); lineTo(lx, ly - stonePlinthH); close() }
        val plR = Path().apply { moveTo(bx, by); lineTo(rx, ry); lineTo(rx, ry - stonePlinthH); lineTo(bx, by - stonePlinthH); close() }
        p.shader = stoneS
        c.drawPath(plL, p); c.drawPath(plR, p)
        p.shader = null
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.6f; p.color = 0x99261E16.toInt()
        c.drawLine(lx, ly - stonePlinthH, bx, by - stonePlinthH, p)
        c.drawLine(bx, by - stonePlinthH, rx, ry - stonePlinthH, p)
        p.strokeWidth = 0.9f; p.color = 0x48ECE2CE
        c.drawLine(lx, ly - stonePlinthH - 1.3f, bx, by - stonePlinthH - 1.3f, p)
        p.style = Paint.Style.FILL
      }

      // Natural warm daylight key on left wall, cool shadow face on right wall
      p.color = 0x22F2DCA8
      c.drawPath(leftWall, p)
      p.color = 0x6A0F0E16
      c.drawPath(rightWall, p)

      // Rising damp & ground contact ambient occlusion + deep eave shadow
      p.shader = LinearGradient(bx, by, bx, by - wallH * 0.38f, 0x6216180E, 0x00000000, Shader.TileMode.CLAMP)
      c.drawPath(leftWall, p); c.drawPath(rightWall, p)
      p.shader = LinearGradient(bx, by - wallH, bx, by - wallH + 13f, 0x6E0A0806, 0x00000000, Shader.TileMode.CLAMP)
      c.drawPath(leftWall, p); c.drawPath(rightWall, p)
      p.shader = null

      if (quoins) drawStoneQuoins(bx, by, wallH)

      p.style = Paint.Style.STROKE; p.strokeWidth = 1.3f; p.color = 0x4CEFE5D0
      c.drawLine(bx, by - 1f, bx, by - wallH + 1f, p)
      p.strokeWidth = 0.9f; p.color = 0x2DF7ECD0
      c.drawLine(lx + 1.5f, ly - wallH + 1f, bx, by - wallH + 1f, p)
      p.style = Paint.Style.FILL
      c.drawPath(leftWall, aoEdge)
      c.drawPath(rightWall, aoEdge)
    }

    fun drawTimberFraming(lx: Float, ly: Float, bx: Float, by: Float, rx: Float, ry: Float, wallH: Float, addXBraces: Boolean = true) {
      p.style = Paint.Style.STROKE
      val lmx = (lx + bx) * 0.5f; val lmy = (ly + by) * 0.5f
      val rmx = (bx + rx) * 0.5f; val rmy = (by + ry) * 0.5f

      // Subtle dark shadow lip beneath beams
      p.color = 0x4E120A04; p.strokeWidth = 4.4f
      c.drawLine(lx, ly - wallH * 0.52f + 1.4f, bx, by - wallH * 0.52f + 1.4f, p)
      c.drawLine(bx, by - wallH * 0.52f + 1.4f, rx, ry - wallH * 0.52f + 1.4f, p)

      p.color = 0xFF42230C.toInt()
      p.strokeWidth = 3.3f
      c.drawLine(lx, ly, lx, ly - wallH, p)
      c.drawLine(bx, by, bx, by - wallH, p)
      c.drawLine(rx, ry, rx, ry - wallH, p)
      c.drawLine(lmx, lmy, lmx, lmy - wallH, p)
      c.drawLine(rmx, rmy, rmx, rmy - wallH, p)
      c.drawLine(lx, ly - wallH * 0.52f, bx, by - wallH * 0.52f, p)
      c.drawLine(bx, by - wallH * 0.52f, rx, ry - wallH * 0.52f, p)
      c.drawLine(lx, ly - wallH, bx, by - wallH, p)
      c.drawLine(bx, by - wallH, rx, ry - wallH, p)

      if (addXBraces) {
        p.strokeWidth = 2.0f
        c.drawLine(lx, ly - wallH * 0.52f, lmx, lmy - wallH, p)
        c.drawLine(lmx, lmy - wallH * 0.52f, lx, ly - wallH, p)
        c.drawLine(bx, by - wallH * 0.52f, rmx, rmy - wallH, p)
        c.drawLine(rmx, rmy - wallH * 0.52f, bx, by - wallH, p)
      }
      // Peg rivets at timber beam junctions
      p.style = Paint.Style.FILL; p.color = 0xFF211206.toInt()
      for ((px, py) in listOf(
        lx to ly - wallH * 0.52f, lmx to lmy - wallH * 0.52f, bx to by - wallH * 0.52f,
        rmx to rmy - wallH * 0.52f, rx to ry - wallH * 0.52f,
      )) {
        c.drawCircle(px, py, 1.4f, p)
      }
    }

    fun drawGableRoof(
      tx: Float, ty: Float, rx: Float, ry: Float, bx: Float, by: Float, lx: Float, ly: Float,
      wallH: Float, ridgeH: Float,
      roofShader: Shader,
      gableShader: Shader,
      tintColor: Int? = null,
      overhang: Float = 6f,
    ) {
      val tlx = lx - overhang; val tly = ly - wallH + overhang * 0.3f
      val tbx = bx; val tby = by - wallH + overhang * 0.5f
      val trx = rx + overhang; val try_ = ry - wallH + overhang * 0.3f
      val ttx = tx; val tty = ty - wallH - overhang * 0.3f

      val rStartX = (ttx + tlx) * 0.5f; val rStartY = (tty + tly) * 0.5f - ridgeH
      val rEndX = (tbx + trx) * 0.5f; val rEndY = (tby + try_) * 0.5f - ridgeH

      val gableWall = Path().apply {
        moveTo(bx, by - wallH); lineTo(rx, ry - wallH); lineTo(rEndX, rEndY); close()
      }
      p.style = Paint.Style.FILL
      p.shader = gableShader
      c.drawPath(gableWall, p); p.shader = null
      p.color = 0x4E120C06; c.drawPath(gableWall, p)
      c.drawPath(gableWall, aoEdge)
      // Vertical king-post & bargeboard timber trim under gable overhang
      p.style = Paint.Style.STROKE; p.strokeWidth = 2.0f; p.color = 0xFF472811.toInt()
      c.drawLine((bx + rx) * 0.5f, (by + ry) * 0.5f - wallH, rEndX, rEndY, p)
      p.strokeWidth = 3.2f; p.color = 0xFF3E220D.toInt()
      c.drawLine(tbx, tby, rEndX, rEndY, p)
      c.drawLine(rEndX, rEndY, trx, try_, p)

      val backSlope = Path().apply { moveTo(ttx, tty); lineTo(rStartX, rStartY); lineTo(rEndX, rEndY); lineTo(trx, try_); close() }
      val frontSlope = Path().apply { moveTo(tlx, tly); lineTo(tbx, tby); lineTo(rEndX, rEndY); lineTo(rStartX, rStartY); close() }

      p.style = Paint.Style.FILL
      p.shader = roofShader
      c.drawPath(backSlope, p); c.drawPath(frontSlope, p)
      p.shader = null
      if (tintColor != null) {
        p.color = (tintColor and 0x00FFFFFF) or 0x8A000000.toInt()
        c.drawPath(backSlope, p); c.drawPath(frontSlope, p)
      }
      // Sunlit lower-left front slope fading toward ridge, deep cool shadow on rear slope
      p.shader = LinearGradient(tlx, tly, rStartX, rStartY, 0x42FFF3CA, 0x10FFE2A8, Shader.TileMode.CLAMP)
      c.drawPath(frontSlope, p)
      p.shader = LinearGradient(rEndX, rEndY, trx, try_, 0x4C110D22, 0x72090714, Shader.TileMode.CLAMP)
      c.drawPath(backSlope, p)
      p.shader = null

      // Subtle overlapping shingle / tile course lines along both slopes
      p.style = Paint.Style.STROKE; p.strokeWidth = 0.85f
      for (course in 1..4) {
        val cf = course / 5f
        val flx = tlx + (rStartX - tlx) * cf; val fly = tly + (rStartY - tly) * cf
        val frx = tbx + (rEndX - tbx) * cf; val fry = tby + (rEndY - tby) * cf
        p.color = 0x361C1208
        c.drawLine(flx, fly, frx, fry, p)
        p.color = 0x26FFF2CE
        c.drawLine(flx, fly - 0.9f, frx, fry - 0.9f, p)

        val blx = trx + (rEndX - trx) * cf; val bly = try_ + (rEndY - try_) * cf
        val brx = ttx + (rStartX - ttx) * cf; val bry = tty + (rStartY - tty) * cf
        p.color = 0x440B0912
        c.drawLine(blx, bly, brx, bry, p)
      }

      // Eave underside shadow & rafter tails
      p.strokeWidth = 2.6f; p.color = 0x52120B05
      c.drawLine(tlx, tly + 1.8f, tbx, tby + 1.8f, p)
      p.strokeWidth = 1.7f; p.color = 0xFF3E2613.toInt()
      for (i in 1..5) {
        val f = i / 6f
        val ex = tlx + (tbx - tlx) * f; val ey = tly + (tby - tly) * f
        c.drawLine(ex, ey, ex - 2f, ey + 3.5f, p)
      }
      p.strokeWidth = 3.2f; p.color = 0xFF4F341D.toInt()
      c.drawLine(rStartX, rStartY, rEndX, rEndY, p)
      p.strokeWidth = 1.1f; p.color = 0x80F5E4B8.toInt()
      c.drawLine(rStartX, rStartY - 1.2f, rEndX, rEndY - 1.2f, p)
      p.style = Paint.Style.FILL; p.color = 0xFFB89648.toInt()
      c.drawCircle(rStartX, rStartY - 2f, 2.0f, p)
      c.drawCircle(rEndX, rEndY - 2f, 2.2f, p)
      c.drawPath(frontSlope, aoEdge); c.drawPath(backSlope, aoEdge)
    }

    fun drawIsoRoof(
      tx: Float, ty: Float, rx: Float, ry: Float, bx: Float, by: Float, lx: Float, ly: Float,
      wallH: Float, peakH: Float,
      roofShader: Shader,
      tintColor: Int? = null,
      overhang: Float = 5f,
    ) {
      val rcx = (tx + bx) * 0.5f
      val rcy = (ty + by) * 0.5f - wallH
      fun expand(px: Float, py: Float): Pair<Float, Float> {
        val dx = px - rcx; val dy = (py - wallH) - rcy
        val len = kotlin.math.hypot(dx, dy).coerceAtLeast(1f)
        return (px + dx / len * overhang) to (py - wallH + dy / len * (overhang * 0.55f))
      }
      val (tlx, tly) = expand(lx, ly)
      val (tbx, tby) = expand(bx, by)
      val (trx, try_) = expand(rx, ry)
      val (ttx, tty) = expand(tx, ty)
      val ax = (ttx + tbx) * 0.5f
      val ay = (tty + tby) * 0.5f - peakH

      val backL = Path().apply { moveTo(ttx, tty); lineTo(tlx, tly); lineTo(ax, ay); close() }
      val backR = Path().apply { moveTo(ttx, tty); lineTo(trx, try_); lineTo(ax, ay); close() }
      val frontL = Path().apply { moveTo(tlx, tly); lineTo(tbx, tby); lineTo(ax, ay); close() }
      val frontR = Path().apply { moveTo(tbx, tby); lineTo(trx, try_); lineTo(ax, ay); close() }

      p.style = Paint.Style.FILL
      p.shader = roofShader
      c.drawPath(backL, p); c.drawPath(backR, p); c.drawPath(frontL, p); c.drawPath(frontR, p)
      p.shader = null
      if (tintColor != null) {
        p.color = (tintColor and 0x00FFFFFF) or 0x7A000000.toInt()
        c.drawPath(backL, p); c.drawPath(backR, p); c.drawPath(frontL, p); c.drawPath(frontR, p)
      }
      p.shader = LinearGradient(tlx, tly, ax, ay, 0x38F6E3B0, 0x14FFF4D4, Shader.TileMode.CLAMP)
      c.drawPath(frontL, p); c.drawPath(backL, p)
      p.shader = LinearGradient(ax, ay, trx, try_, 0x4A0C0B12, 0x74080710, Shader.TileMode.CLAMP)
      c.drawPath(frontR, p); c.drawPath(backR, p)
      p.shader = null

      // Concentric hip-roof tile courses around pyramid faces
      p.style = Paint.Style.STROKE; p.strokeWidth = 0.8f; p.color = 0x3A1A120A
      for (step in 1..3) {
        val f = step / 4f
        val p1x = ax + (tlx - ax) * f; val p1y = ay + (tly - ay) * f
        val p2x = ax + (tbx - ax) * f; val p2y = ay + (tby - ay) * f
        val p3x = ax + (trx - ax) * f; val p3y = ay + (try_ - ay) * f
        c.drawLine(p1x, p1y, p2x, p2y, p)
        c.drawLine(p2x, p2y, p3x, p3y, p)
      }

      p.style = Paint.Style.STROKE; p.strokeWidth = 2.4f; p.color = 0xFF3E2613.toInt()
      c.drawLine(ax, ay, tlx, tly, p); c.drawLine(ax, ay, tbx, tby, p); c.drawLine(ax, ay, trx, try_, p)
      p.strokeWidth = 0.9f; p.color = 0x6EF5E6BE
      c.drawLine(ax - 0.8f, ay, tbx - 0.8f, tby, p)
      p.style = Paint.Style.FILL; p.color = 0xFFC29E4E.toInt(); c.drawCircle(ax, ay - 2.5f, 2.8f, p)
      c.drawCircle(ax, ay - 2.5f, 2.8f, aoEdge)
      c.drawPath(frontL, aoEdge); c.drawPath(frontR, aoEdge)
    }

    fun drawChimneyStack(x: Float, yTop: Float, yBot: Float, w: Float = 12f) {
      p.style = Paint.Style.FILL
      p.shader = stoneS
      c.drawRect(x - w * 0.5f, yTop, x + w * 0.5f, yBot, p)
      p.shader = null
      p.color = 0x3A000000
      c.drawRect(x + w * 0.08f, yTop, x + w * 0.5f, yBot, p)
      p.color = 0x22EFE2C8
      c.drawRect(x - w * 0.5f, yTop, x - w * 0.15f, yBot, p)
      c.drawRect(x - w * 0.5f, yTop, x + w * 0.5f, yBot, aoEdge)
      p.color = 0xFF6B3F2E.toInt()
      c.drawRect(x - w * 0.65f, yTop - 4f, x + w * 0.65f, yTop + 1f, p)
      c.drawRect(x - w * 0.65f, yTop - 4f, x + w * 0.65f, yTop + 1f, aoEdge)
      p.color = 0xFF844D36.toInt()
      c.drawRect(x - w * 0.35f, yTop - 9.5f, x - w * 0.05f, yTop - 4f, p)
      c.drawRect(x + w * 0.08f, yTop - 8.5f, x + w * 0.38f, yTop - 4f, p)
      p.color = 0xFF1A1411.toInt()
      c.drawRect(x - w * 0.35f, yTop - 10.8f, x - w * 0.05f, yTop - 9f, p)
      c.drawRect(x + w * 0.08f, yTop - 9.8f, x + w * 0.38f, yTop - 8.2f, p)
    }

    fun drawBattlements(lx: Float, ly: Float, bx: Float, by: Float, rx: Float, ry: Float, wallH: Float, countPerSide: Int = 4) {
      p.style = Paint.Style.FILL
      for (i in 0 until countPerSide) {
        val t = (i + 0.5f) / countPerSide
        val mxL = lx + (bx - lx) * t; val myL = ly + (by - ly) * t - wallH
        p.shader = stoneS; c.drawRect(mxL - 4.8f, myL - 8f, mxL + 4.8f, myL + 1f, p); p.shader = null
        p.color = 0x22EDE2CE; c.drawRect(mxL - 4.8f, myL - 8f, mxL + 4.8f, myL - 5.5f, p)
        c.drawRect(mxL - 4.8f, myL - 8f, mxL + 4.8f, myL + 1f, aoEdge)

        val mxR = bx + (rx - bx) * t; val myR = by + (ry - by) * t - wallH
        p.shader = stoneS; c.drawRect(mxR - 4.8f, myR - 8f, mxR + 4.8f, myR + 1f, p); p.shader = null
        p.color = 0x56000000; c.drawRect(mxR - 4.8f, myR - 8f, mxR + 4.8f, myR + 1f, p)
        c.drawRect(mxR - 4.8f, myR - 8f, mxR + 4.8f, myR + 1f, aoEdge)
      }
    }

    fun drawArchDoor(gx: Float, gy: Float, gw: Float, gh: Float, portcullis: Boolean = false) {
      p.style = Paint.Style.FILL; p.color = 0xFF756D62.toInt()
      c.drawRect(gx - gw * 0.76f, gy - 2f, gx + gw * 0.76f, gy + 3.5f, p)
      c.drawRect(gx - gw * 0.76f, gy - 2f, gx + gw * 0.76f, gy + 3.5f, aoEdge)

      p.color = 0xFF524A42.toInt()
      c.drawRect(gx - gw * 0.64f, gy - gh * 0.72f, gx + gw * 0.64f, gy, p)
      c.drawCircle(gx, gy - gh * 0.72f, gw * 0.64f, p)
      p.style = Paint.Style.STROKE; p.strokeWidth = 1.0f; p.color = 0xAA241F1A.toInt()
      c.drawArc(RectF(gx - gw * 0.62f, gy - gh * 0.72f - gw * 0.62f, gx + gw * 0.62f, gy - gh * 0.72f + gw * 0.62f), 180f, 180f, false, p)
      p.style = Paint.Style.FILL

      p.shader = timberS
      c.drawRect(gx - gw * 0.45f, gy - gh * 0.65f, gx + gw * 0.45f, gy, p)
      c.drawCircle(gx, gy - gh * 0.65f, gw * 0.45f, p)
      p.shader = null

      p.style = Paint.Style.STROKE; p.strokeWidth = 1.2f; p.color = 0xFF211309.toInt()
      c.drawLine(gx, gy - gh * 0.95f, gx, gy, p)
      p.color = 0xFF2B3036.toInt(); p.strokeWidth = 1.6f
      c.drawLine(gx - gw * 0.42f, gy - gh * 0.5f, gx - gw * 0.1f, gy - gh * 0.5f, p)
      c.drawLine(gx + gw * 0.1f, gy - gh * 0.28f, gx + gw * 0.42f, gy - gh * 0.28f, p)
      p.color = 0xFFC29E48.toInt(); p.strokeWidth = 1.0f
      c.drawCircle(gx - gw * 0.14f, gy - gh * 0.34f, 1.4f, p)
      c.drawCircle(gx + gw * 0.14f, gy - gh * 0.34f, 1.4f, p)

      if (portcullis) {
        p.color = 0xFF14171A.toInt(); p.style = Paint.Style.FILL
        c.drawRect(gx - gw * 0.42f, gy - gh * 0.62f, gx + gw * 0.42f, gy, p)
        p.color = 0xFF6B747D.toInt(); p.style = Paint.Style.STROKE; p.strokeWidth = 1.9f
        for (bar in -2..2) {
          val bx = gx + bar * gw * 0.14f
          c.drawLine(bx, gy - gh * 0.9f, bx, gy, p)
        }
        c.drawLine(gx - gw * 0.38f, gy - gh * 0.45f, gx + gw * 0.38f, gy - gh * 0.45f, p)
        c.drawLine(gx - gw * 0.38f, gy - gh * 0.22f, gx + gw * 0.38f, gy - gh * 0.22f, p)
      }
      p.style = Paint.Style.FILL
    }

    fun drawShutteredWindow(wx: Float, wy: Float, ww: Float, wh: Float, glow: Boolean = true) {
      if (glow) {
        p.shader = RadialGradient(wx + ww * 0.5f, wy + wh * 0.5f, ww * 1.6f, 0x3CE09B3E.toInt(), 0x00FF9100, Shader.TileMode.CLAMP)
        c.drawCircle(wx + ww * 0.5f, wy + wh * 0.5f, ww * 1.6f, p)
        p.shader = null
      }
      p.style = Paint.Style.FILL
      p.color = 0xFF4B321C.toInt()
      c.drawRect(wx - 4.0f, wy, wx - 1.0f, wy + wh, p); c.drawRect(wx - 4.0f, wy, wx - 1.0f, wy + wh, aoEdge)
      c.drawRect(wx + ww + 1.0f, wy, wx + ww + 4.0f, wy + wh, p); c.drawRect(wx + ww + 1.0f, wy, wx + ww + 4.0f, wy + wh, aoEdge)
      p.color = 0xFF2B1C10.toInt()
      c.drawRect(wx - 1.5f, wy - 1.5f, wx + ww + 1.5f, wy + wh + 1.5f, p)
      if (glow) {
        p.shader = LinearGradient(wx, wy, wx, wy + wh, 0xFFD49746.toInt(), 0xFF5C3818.toInt(), Shader.TileMode.CLAMP)
      } else {
        p.color = 0xFF22272B.toInt()
      }
      c.drawRect(wx, wy, wx + ww, wy + wh, p)
      p.shader = null
      p.color = 0xFF7D7468.toInt()
      c.drawRect(wx - 2.4f, wy + wh + 1f, wx + ww + 2.4f, wy + wh + 3.4f, p)
      p.color = 0x55221B15; c.drawRect(wx - 2.0f, wy + wh + 3.4f, wx + ww + 2.0f, wy + wh + 4.8f, p)
      p.style = Paint.Style.STROKE; p.strokeWidth = 0.85f; p.color = 0xFF322114.toInt()
      c.drawLine(wx + ww * 0.5f, wy, wx + ww * 0.5f, wy + wh, p)
      c.drawLine(wx, wy + wh * 0.5f, wx + ww, wy + wh * 0.5f, p)
      c.drawLine(wx, wy, wx + ww, wy + wh, p)
      c.drawLine(wx + ww, wy, wx, wy + wh, p)
      p.strokeWidth = if (glow) 0.8f else 1.0f
      p.color = if (glow) 0x52FFF4CE else 0x5CE4F2FA
      c.drawLine(wx + 1.5f, wy + 2f, wx + ww * 0.65f, wy + wh * 0.45f, p)
      p.style = Paint.Style.FILL
    }

    fun drawShieldEmblem(sx: Float, sy: Float, r: Float) {
      p.style = Paint.Style.FILL
      p.color = 0xFF21170E.toInt(); c.drawCircle(sx, sy, r + 1.0f, p)
      p.color = fMain; c.drawCircle(sx, sy, r, p)
      p.style = Paint.Style.STROKE; p.strokeWidth = 1.0f; p.color = 0xFFCFA84C.toInt()
      c.drawCircle(sx, sy, r * 0.85f, p)
      p.style = Paint.Style.FILL
      p.color = 0xFFC9A148.toInt(); c.drawCircle(sx, sy, r * 0.40f, p)
    }

    fun drawCrateAndBarrel(bx: Float, by: Float) {
      p.style = Paint.Style.FILL; p.shader = timberS
      c.drawRect(bx - 7.5f, by - 9.5f, bx + 4.5f, by + 1f, p); p.shader = null
      c.drawRect(bx - 7.5f, by - 9.5f, bx + 4.5f, by + 1f, aoEdge)
      p.style = Paint.Style.STROKE; p.strokeWidth = 1.0f; p.color = 0xFF362010.toInt()
      c.drawLine(bx - 7.5f, by - 9.5f, bx + 4.5f, by + 1f, p)
      p.style = Paint.Style.FILL
      p.color = 0xFF704523.toInt(); c.drawOval(RectF(bx + 5.5f, by - 8.5f, bx + 15f, by + 3f), p)
      c.drawOval(RectF(bx + 5.5f, by - 8.5f, bx + 15f, by + 3f), aoEdge)
      p.color = 0xFF34383D.toInt(); p.style = Paint.Style.STROKE; p.strokeWidth = 1.2f
      c.drawLine(bx + 6f, by - 4.8f, bx + 14.5f, by - 4.8f, p)
      c.drawLine(bx + 6f, by - 0.5f, bx + 14.5f, by - 0.5f, p)
      p.style = Paint.Style.FILL
    }

    fun drawWallTorch(tx: Float, ty: Float) {
      p.shader = RadialGradient(tx, ty - 5f, 15f, 0x5EE08D26.toInt(), 0x00FF5E00, Shader.TileMode.CLAMP)
      c.drawCircle(tx, ty - 5f, 15f, p); p.shader = null
      p.color = 0xFF2D2218.toInt(); c.drawRect(tx - 1.4f, ty - 4f, tx + 1.4f, ty + 3.5f, p)
      p.color = 0xFFE68A1E.toInt(); c.drawCircle(tx, ty - 5.2f, 2.5f, p)
      p.color = 0xFFFCEBA8.toInt(); c.drawCircle(tx, ty - 5.8f, 1.2f, p)
    }

    fun drawIvyCluster(vx: Float, vy: Float, spread: Float) {
      p.style = Paint.Style.FILL
      val pts = listOf(
        -0.45f to -0.65f, 0.22f to -0.35f, 0.58f to 0.1f, -0.18f to 0.25f, 0.35f to 0.58f, -0.52f to 0.05f,
      )
      for ((idx, pt) in pts.withIndex()) {
        p.color = if (idx % 2 == 0) 0xDC223819.toInt() else 0xDC405B28.toInt()
        c.drawCircle(vx + pt.first * spread, vy + pt.second * spread, 3.1f, p)
      }
      p.color = 0xFF9C3036.toInt(); c.drawCircle(vx + 0.2f * spread, vy - 0.3f * spread, 1.1f, p)
      p.color = 0xFFD9C788.toInt(); c.drawCircle(vx - 0.4f * spread, vy + 0.1f * spread, 1.0f, p)
    }

    when (type) {
      BuildingType.TOWN_CENTER -> {
        drawCourtyardApron(dirtS)
        drawCourtyardApron(stoneS, 0.20f)

        val mTopX = cx + 26f; val mTopY = baseCenterY - 26f
        val mRightX = pRightX - 18f; val mRightY = pRightY - 4f
        val mBotX = cx + 38f; val mBotY = baseCenterY + 26f
        val mLeftX = cx - 4f; val mLeftY = baseCenterY + 4f
        drawIsoBox(mTopX, mTopY, mRightX, mRightY, mBotX, mBotY, mLeftX, mLeftY, 34f, timberS, stonePlinthH = 8f)
        drawIsoRoof(mTopX, mTopY, mRightX, mRightY, mBotX, mBotY, mLeftX, mLeftY, 34f, 18f, thatchS, fMain, overhang = 5f)
        for (s in 0..4) {
          p.color = if (s % 2 == 0) 0xFFF5E9CC.toInt() else fMain
          val ax = mLeftX + 5f + s * 8.2f
          val ay = mBotY - 12f - s * 3.9f
          c.drawRect(ax, ay, ax + 6.5f, ay + 6.5f, p)
        }

        val wingH = 52f
        val hTopX = cx - 18f; val hTopY = pTopY + 28f
        val hRightX = cx + 24f; val hRightY = baseCenterY - 6f
        val hBotX = cx - 20f; val hBotY = pBotY - 18f
        val hLeftX = pLeftX + 28f; val hLeftY = pLeftY + 4f
        drawIsoBox(hTopX, hTopY, hRightX, hRightY, hBotX, hBotY, hLeftX, hLeftY, wingH, plasterS, stonePlinthH = 14f, quoins = true)
        drawTimberFraming(hLeftX, hLeftY, hBotX, hBotY, hRightX, hRightY, wingH)
        drawGableRoof(hTopX, hTopY, hRightX, hRightY, hBotX, hBotY, hLeftX, hLeftY, wingH, 28f, thatchS, plasterS, overhang = 6f)

        p.style = Paint.Style.STROKE; p.color = 0xFF3A2311.toInt(); p.strokeWidth = 2.0f
        c.drawLine(hBotX - 10f, hBotY - 34f, hBotX - 18f, hBotY - 30f, p)
        p.style = Paint.Style.FILL; p.color = 0xFFE3BD5E.toInt()
        c.drawRect(hBotX - 21f, hBotY - 30f, hBotX - 12f, hBotY - 22f, p)
        c.drawRect(hBotX - 21f, hBotY - 30f, hBotX - 12f, hBotY - 22f, aoEdge)

        val kTopX = cx; val kTopY = baseCenterY - 48f
        val kRightX = cx + 44f; val kRightY = baseCenterY - 26f
        val kBotX = cx; val kBotY = baseCenterY - 4f
        val kLeftX = cx - 44f; val kLeftY = baseCenterY - 26f
        drawIsoBox(kTopX, kTopY, kRightX, kRightY, kBotX, kBotY, kLeftX, kLeftY, 96f, stoneS, stonePlinthH = 16f, quoins = true)
        drawBattlements(kLeftX, kLeftY, kBotX, kBotY, kRightX, kRightY, 96f, countPerSide = 4)
        drawIsoRoof(kTopX, kTopY, kRightX, kRightY, kBotX, kBotY, kLeftX, kLeftY, 98f, 38f, slateS, fMain, overhang = 4f)

        drawChimneyStack(cx + 36f, baseCenterY - 92f, baseCenterY - 56f, 11f)
        drawArchDoor((hLeftX + hBotX) * 0.5f, (hLeftY + hBotY) * 0.5f, 20f, 30f)
        drawShutteredWindow(cx - 22f, baseCenterY - 74f, 8f, 14f)
        drawShutteredWindow(cx + 12f, baseCenterY - 74f, 8f, 14f)
        drawShutteredWindow(hBotX - 38f, hBotY - 38f, 9f, 10f)
        drawShieldEmblem(cx, baseCenterY - 46f, 7.8f)
        drawWallTorch((hLeftX + hBotX) * 0.5f - 14f, (hLeftY + hBotY) * 0.5f - 14f)
        drawWallTorch((hLeftX + hBotX) * 0.5f + 14f, (hLeftY + hBotY) * 0.5f - 14f)
        drawIvyCluster(hLeftX + 10f, hLeftY - 10f, 8f)
        drawIvyCluster(cx + 34f, baseCenterY - 12f, 7f)

        // Courtyard supply cart & carved stone village well
        p.shader = timberS; c.drawRect(pBotX - 38f, pBotY - 18f, pBotX - 18f, pBotY - 8f, p); p.shader = null
        c.drawRect(pBotX - 38f, pBotY - 18f, pBotX - 18f, pBotY - 8f, aoEdge)
        p.color = 0xFF4F2E14.toInt()
        c.drawCircle(pBotX - 34f, pBotY - 7f, 4.6f, p); c.drawCircle(pBotX - 22f, pBotY - 7f, 4.6f, p)
        p.color = 0xFFD9AE54.toInt(); c.drawCircle(pBotX - 28f, pBotY - 18f, 4.3f, p)
        drawCrateAndBarrel(pBotX + 26f, pBotY - 4f)
        // Village well with stone rim, water reflection & pitched shingle hood
        val wx = pBotX - 2f; val wy = pBotY - 11f
        p.shader = stoneS; c.drawOval(RectF(wx - 10f, wy - 5f, wx + 10f, wy + 5f), p); p.shader = null
        c.drawOval(RectF(wx - 10f, wy - 5f, wx + 10f, wy + 5f), aoEdge)
        p.color = 0xFF1F4E66.toInt(); c.drawOval(RectF(wx - 6.8f, wy - 3.2f, wx + 6.8f, wy + 1.8f), p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.9f; p.color = 0xFF4A2B12.toInt()
        c.drawLine(wx - 7f, wy - 2f, wx - 7f, wy - 16f, p)
        c.drawLine(wx + 7f, wy - 2f, wx + 7f, wy - 16f, p)
        p.style = Paint.Style.FILL; p.shader = thatchS
        val wRoof = Path().apply { moveTo(wx - 11f, wy - 14f); lineTo(wx, wy - 22f); lineTo(wx + 11f, wy - 14f); close() }
        c.drawPath(wRoof, p); p.shader = null; c.drawPath(wRoof, aoEdge)
      }

      BuildingType.HOUSE -> {
        drawCourtyardApron(dirtS, 0.08f)
        val sTopX = cx + 10f; val sTopY = pTopY + 14f
        val sRightX = pRightX - 10f; val sRightY = pRightY - 4f
        val sBotX = cx + 24f; val sBotY = pBotY - 22f
        val sLeftX = cx - 2f; val sLeftY = baseCenterY - 4f
        drawIsoBox(sTopX, sTopY, sRightX, sRightY, sBotX, sBotY, sLeftX, sLeftY, 24f, timberS, stonePlinthH = 6f)
        drawIsoRoof(sTopX, sTopY, sRightX, sRightY, sBotX, sBotY, sLeftX, sLeftY, 24f, 14f, slateS, overhang = 4f)

        val wallH = 42f
        val ixT = cx - 8f; val iyT = pTopY + 12f
        val ixR = cx + 28f; val iyR = baseCenterY + 2f
        val ixB = cx - 8f; val iyB = pBotY - 8f
        val ixL = pLeftX + 16f; val iyL = pLeftY + 2f
        drawChimneyStack(ixL + 8f, pTopY - 48f, iyL - 8f, 12f)
        drawIsoBox(ixT, iyT, ixR, iyR, ixB, iyB, ixL, iyL, wallH, plasterS, stonePlinthH = 11f, quoins = true)
        drawTimberFraming(ixL, iyL, ixB, iyB, ixR, iyR, wallH)
        drawGableRoof(ixT, iyT, ixR, iyR, ixB, iyB, ixL, iyL, wallH, 32f, thatchS, plasterS, overhang = 7f)

        p.color = 0x5A477829; c.drawOval(RectF(cx - 26f, iyB - 58f, cx - 10f, iyB - 50f), p)
        drawArchDoor((ixB + ixR) * 0.5f, (iyB + iyR) * 0.5f, 13f, 22f)
        drawWallTorch((ixB + ixR) * 0.5f - 10f, (iyB + iyR) * 0.5f - 12f)
        drawShutteredWindow(cx - 32f, iyB - 32f, 9f, 9f)
        drawIvyCluster(ixL + 10f, iyL - 16f, 9f)
        p.color = 0xFF4D2D16.toInt(); c.drawRect(cx - 34f, iyB - 19f, cx - 20f, iyB - 14f, p)
        p.color = 0xFFE35252.toInt(); c.drawCircle(cx - 30f, iyB - 20f, 2.2f, p); c.drawCircle(cx - 24f, iyB - 20f, 2.2f, p)
        p.color = 0xFFF2D254.toInt(); c.drawCircle(cx - 27f, iyB - 20.5f, 1.8f, p)
        for (log in 0..2) {
          p.color = 0xFF7D4E26.toInt(); c.drawCircle(ixL + 8f + log * 5f, iyL + 5f, 3.2f, p)
          p.color = 0xFFD9AE6E.toInt(); c.drawCircle(ixL + 8f + log * 5f, iyL + 5f, 1.8f, p)
        }
        drawCrateAndBarrel(ixB + 16f, iyB + 2f)
      }

      BuildingType.LUMBER_CAMP -> {
        drawCourtyardApron(dirtS)
        val wallH = 36f
        val ixT = cx; val iyT = pTopY + 12f
        val ixR = pRightX - 20f; val iyR = pRightY
        val ixB = cx; val iyB = pBotY - 12f
        val ixL = pLeftX + 20f; val iyL = pLeftY
        drawIsoBox(ixT, iyT, ixR, iyR, ixB, iyB, ixL, iyL, wallH, timberS, stonePlinthH = 7f)
        drawGableRoof(ixT, iyT, ixR, iyR, ixB, iyB, ixL, iyL, wallH, 26f, thatchS, timberS, overhang = 7f)
        for (row in 0..2) {
          for (i in 0..(2 - row)) {
            val lx = pLeftX + 24f + i * 11f + row * 5.5f
            val ly = pLeftY + 10f - row * 8.5f
            p.color = 0xFF5D391A.toInt(); c.drawCircle(lx, ly, 6.2f, p); c.drawCircle(lx, ly, 6.2f, aoEdge)
            p.color = 0xFFDAB173.toInt(); c.drawCircle(lx, ly, 4.3f, p)
            p.color = 0xFF9C7240.toInt(); c.drawCircle(lx, ly, 2.0f, p)
          }
        }
        p.color = 0xFF7A4D28.toInt(); c.drawRect(pBotX + 18f, pBotY - 15f, pBotX + 29f, pBotY - 3f, p)
        p.color = 0xFFD9AE6E.toInt(); c.drawOval(RectF(pBotX + 18f, pBotY - 17f, pBotX + 29f, pBotY - 12f), p)
        p.color = 0xFFC8D0D8.toInt(); c.drawRect(pBotX + 23f, pBotY - 22f, pBotX + 28f, pBotY - 15f, p)
        p.color = 0xFFE6C88A.toInt(); c.drawOval(RectF(pBotX - 18f, pBotY - 10f, pBotX + 2f, pBotY - 2f), p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2.4f; p.color = 0xFF4F2F16.toInt()
        c.drawLine(pBotX - 24f, pBotY - 6f, pBotX - 16f, pBotY - 18f, p)
        c.drawLine(pBotX - 8f, pBotY - 6f, pBotX - 16f, pBotY - 18f, p)
        p.style = Paint.Style.FILL
      }

      BuildingType.MINING_CAMP -> {
        drawCourtyardApron(stoneS, 0.1f)
        val wallH = 40f
        val ixT = cx; val iyT = pTopY + 12f
        val ixR = pRightX - 20f; val iyR = pRightY
        val ixB = cx; val iyB = pBotY - 12f
        val ixL = pLeftX + 20f; val iyL = pLeftY
        drawIsoBox(ixT, iyT, ixR, iyR, ixB, iyB, ixL, iyL, wallH, stoneS, stonePlinthH = 9f, quoins = true)
        drawTimberFraming(ixL, iyL, ixB, iyB, ixR, iyR, wallH, addXBraces = true)
        drawIsoRoof(ixT, iyT, ixR, iyR, ixB, iyB, ixL, iyL, wallH, 28f, slateS, fMain, overhang = 6f)
        drawArchDoor((ixB + ixR) * 0.5f, (iyB + iyR) * 0.5f, 18f, 24f)
        drawWallTorch((ixB + ixR) * 0.5f - 12f, (iyB + iyR) * 0.5f - 15f)
        // Heavy timber A-frame hoist crane & suspended iron ore bucket
        p.style = Paint.Style.STROKE; p.strokeWidth = 2.8f; p.color = 0xFF472811.toInt()
        c.drawLine(ixT - 6f, iyT - 30f, ixL + 10f, iyL - 62f, p)
        c.drawLine(ixL + 22f, iyL - 36f, ixL + 10f, iyL - 62f, p)
        p.strokeWidth = 1.6f; p.color = 0xFF282D33.toInt()
        c.drawLine(ixL + 10f, iyL - 62f, ixL + 10f, iyL - 36f, p)
        p.style = Paint.Style.FILL; p.color = 0xFF3E444D.toInt()
        c.drawRect(ixL + 5f, iyL - 36f, ixL + 15f, iyL - 28f, p)
        c.drawRect(ixL + 5f, iyL - 36f, ixL + 15f, iyL - 28f, aoEdge)
        // Inclined ore sluice chute & iron rails
        p.style = Paint.Style.FILL; p.shader = timberS
        val chute = Path().apply {
          moveTo(ixL + 6f, iyL - 26f); lineTo(ixL + 18f, iyL - 22f)
          lineTo(pLeftX + 34f, pLeftY - 6f); lineTo(pLeftX + 22f, pLeftY - 10f); close()
        }
        c.drawPath(chute, p); p.shader = null; c.drawPath(chute, aoEdge)
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.6f; p.color = 0xFF454B52.toInt()
        c.drawLine(pLeftX + 12f, pLeftY + 2f, pLeftX + 52f, pLeftY - 10f, p)
        c.drawLine(pLeftX + 16f, pLeftY + 7f, pLeftX + 56f, pLeftY - 5f, p)
        // Filled ore mine cart with glittering gold nuggets & cut granite
        p.style = Paint.Style.FILL; p.shader = timberS
        c.drawRect(pLeftX + 16f, pLeftY - 10f, pLeftX + 42f, pLeftY + 5f, p); p.shader = null
        c.drawRect(pLeftX + 16f, pLeftY - 10f, pLeftX + 42f, pLeftY + 5f, aoEdge)
        for ((gx, gy, gr) in listOf(Triple(23f, -11f, 4.6f), Triple(31f, -13f, 5.4f), Triple(37f, -10f, 4.2f))) {
          p.color = 0xFF8E6418.toInt(); c.drawCircle(pLeftX + gx, pLeftY + gy, gr + 0.8f, p)
          p.color = 0xFFFFDC5E.toInt(); c.drawCircle(pLeftX + gx - 0.5f, pLeftY + gy - 0.5f, gr * 0.82f, p)
          p.color = 0xFFFFF6CE.toInt(); c.drawCircle(pLeftX + gx - 1.4f, pLeftY + gy - 1.4f, gr * 0.35f, p)
        }
        p.color = 0xFF2B2E33.toInt()
        c.drawCircle(pLeftX + 21f, pLeftY + 7f, 4.5f, p); c.drawCircle(pLeftX + 37f, pLeftY + 7f, 4.5f, p)
        // Stacked hewn granite blocks + iron lantern post + leaning pickaxe
        p.color = 0xFF8F9499.toInt()
        c.drawRect(pBotX + 12f, pBotY - 14f, pBotX + 26f, pBotY - 4f, p)
        c.drawRect(pBotX + 12f, pBotY - 14f, pBotX + 26f, pBotY - 4f, aoEdge)
        p.color = 0xFFAAB0B5.toInt()
        c.drawRect(pBotX + 16f, pBotY - 21f, pBotX + 28f, pBotY - 13f, p)
        c.drawRect(pBotX + 16f, pBotY - 21f, pBotX + 28f, pBotY - 13f, aoEdge)
        drawWallTorch(pBotX - 24f, pBotY - 16f)
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.8f; p.color = 0xFF5A371A.toInt()
        c.drawLine(pBotX - 18f, pBotY - 2f, pBotX - 10f, pBotY - 16f, p)
        p.color = 0xFFD0D8E0.toInt(); c.drawLine(pBotX - 16f, pBotY - 18f, pBotX - 5f, pBotY - 14f, p)
        p.style = Paint.Style.FILL
      }

      BuildingType.MILL -> {
        drawCourtyardApron(dirtS)
        val wallH = 62f
        val ixT = cx; val iyT = pTopY + 18f
        val ixR = pRightX - 28f; val iyR = pRightY
        val ixB = cx; val iyB = pBotY - 18f
        val ixL = pLeftX + 28f; val iyL = pLeftY
        drawIsoBox(ixT, iyT, ixR, iyR, ixB, iyB, ixL, iyL, wallH, plasterS, stonePlinthH = 22f, quoins = true)
        drawTimberFraming(ixL, iyL, ixB, iyB, ixR, iyR, wallH)
        p.style = Paint.Style.STROKE; p.strokeWidth = 3.2f; p.color = 0xFF472911.toInt()
        c.drawLine(ixL - 4f, iyL - 48f, ixB, iyB - 48f, p)
        c.drawLine(ixB, iyB - 48f, ixR + 4f, iyR - 48f, p)
        p.style = Paint.Style.FILL
        drawIsoRoof(ixT, iyT, ixR, iyR, ixB, iyB, ixL, iyL, wallH, 32f, slateS, fMain, overhang = 7f)
        drawArchDoor((ixL + ixB) * 0.5f, (iyL + iyB) * 0.5f, 15f, 24f)
        drawShutteredWindow(cx + 14f, iyB - 48f, 8f, 10f)
        drawIvyCluster(ixL + 8f, iyL - 10f, 8f)
        // Timber grain hopper chute & stitched flour sacks beside millstone
        p.style = Paint.Style.FILL; p.shader = timberS
        val chute = Path().apply {
          moveTo(cx + 8f, iyB - 34f); lineTo(cx + 16f, iyB - 36f)
          lineTo(pBotX + 18f, pBotY - 12f); lineTo(pBotX + 10f, pBotY - 10f); close()
        }
        c.drawPath(chute, p); p.shader = null; c.drawPath(chute, aoEdge)
        p.color = 0xFF9C9486.toInt(); c.drawCircle(pBotX + 22f, pBotY - 9f, 9f, p); c.drawCircle(pBotX + 22f, pBotY - 9f, 9f, aoEdge)
        p.color = 0xFF473E34.toInt(); c.drawCircle(pBotX + 22f, pBotY - 9f, 2.8f, p)
        for (i in 0..2) {
          val sx = pBotX - 28f + i * 9f
          p.color = if (i % 2 == 0) 0xFFE3D2AD.toInt() else 0xFFCBB68B.toInt()
          c.drawOval(RectF(sx, pBotY - 14f, sx + 13f, pBotY - 3f), p)
          c.drawOval(RectF(sx, pBotY - 14f, sx + 13f, pBotY - 3f), aoEdge)
          p.style = Paint.Style.STROKE; p.strokeWidth = 1.0f; p.color = 0xFF7D5933.toInt()
          c.drawLine(sx + 3f, pBotY - 11.5f, sx + 10f, pBotY - 11.5f, p)
          p.style = Paint.Style.FILL
        }
      }

      BuildingType.BARRACKS -> {
        drawCourtyardApron(dirtS)
        drawCourtyardApron(stoneS, 0.22f)
        val wallH = 52f
        val ixT = cx; val iyT = pTopY + 14f
        val ixR = pRightX - 22f; val iyR = pRightY
        val ixB = cx; val iyB = pBotY - 14f
        val ixL = pLeftX + 22f; val iyL = pLeftY
        drawIsoBox(ixT, iyT, ixR, iyR, ixB, iyB, ixL, iyL, wallH, stoneS, stonePlinthH = 15f, quoins = true)
        drawTimberFraming(ixL, iyL, ixB, iyB, ixR, iyR, wallH, addXBraces = true)
        drawGableRoof(ixT, iyT, ixR, iyR, ixB, iyB, ixL, iyL, wallH, 34f, slateS, plasterS, fDark, overhang = 7f)

        drawArchDoor((ixL + ixB) * 0.5f, (iyL + iyB) * 0.5f, 19f, 28f)
        drawWallTorch((ixL + ixB) * 0.5f - 14f, (iyL + iyB) * 0.5f - 14f)
        drawShieldEmblem(cx + 28f, iyB - 28f, 8.5f)
        drawShieldEmblem(cx + 50f, iyB - 38f, 7.0f)
        drawShutteredWindow(cx + 26f, iyB - 48f, 9f, 10f)

        p.style = Paint.Style.STROKE; p.strokeWidth = 2.2f; p.color = 0xFF4B2D14.toInt()
        c.drawLine(pBotX - 14f, pBotY - 6f, pBotX + 6f, pBotY - 16f, p)
        for (s in 0..2) {
          val sx = pBotX - 10f + s * 6f; val sy = pBotY - 8f - s * 3f
          c.drawLine(sx, sy + 6f, sx, sy - 16f, p)
          p.color = 0xFFE6EDF2.toInt(); c.drawLine(sx, sy - 16f, sx, sy - 21f, p)
          p.color = 0xFF4B2D14.toInt()
        }
        p.style = Paint.Style.FILL; p.color = 0xFFD4AC59.toInt()
        c.drawCircle(pLeftX + 24f, pLeftY - 14f, 5f, p)
        c.drawRect(pLeftX + 20f, pLeftY - 10f, pLeftX + 28f, pLeftY + 2f, p)
        // Courtyard padded sword-training dummy post
        val dx = pBotX + 28f; val dy = pBotY - 10f
        p.style = Paint.Style.STROKE; p.strokeWidth = 2.6f; p.color = 0xFF523116.toInt()
        c.drawLine(dx, dy + 4f, dx, dy - 20f, p)
        c.drawLine(dx - 8f, dy - 13f, dx + 8f, dy - 13f, p)
        p.style = Paint.Style.FILL; p.color = 0xFFD4B572.toInt()
        c.drawOval(RectF(dx - 3.6f, dy - 17f, dx + 3.6f, dy - 7f), p)
        c.drawOval(RectF(dx - 3.6f, dy - 17f, dx + 3.6f, dy - 7f), aoEdge)
        p.color = 0xFFA68646.toInt(); c.drawCircle(dx, dy - 20f, 2.8f, p)
        p.color = fMain; c.drawCircle(dx - 7f, dy - 13f, 3.2f, p); c.drawCircle(dx - 7f, dy - 13f, 3.2f, aoEdge)
      }

      BuildingType.ARCHERY_RANGE -> {
        drawCourtyardApron(dirtS)
        val wallH = 44f
        val ixT = cx; val iyT = pTopY + 14f
        val ixR = pRightX - 24f; val iyR = pRightY
        val ixB = cx; val iyB = pBotY - 14f
        val ixL = pLeftX + 24f; val iyL = pLeftY
        drawIsoBox(ixT, iyT, ixR, iyR, ixB, iyB, ixL, iyL, wallH, timberS, stonePlinthH = 10f)
        drawGableRoof(ixT, iyT, ixR, iyR, ixB, iyB, ixL, iyL, wallH, 28f, thatchS, timberS, fMain, overhang = 7f)
        drawArchDoor((ixL + ixB) * 0.5f, (iyL + iyB) * 0.5f, 15f, 24f)
        drawShieldEmblem(cx - 26f, iyB - 32f, 6.5f)
        p.shader = timberS; c.drawRect(pLeftX + 18f, pLeftY - 8f, pLeftX + 36f, pLeftY + 1f, p); p.shader = null
        c.drawRect(pLeftX + 18f, pLeftY - 8f, pLeftX + 36f, pLeftY + 1f, aoEdge)
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.4f; p.color = 0xFFF2E6D0.toInt()
        c.drawLine(pLeftX + 21f, pLeftY - 10f, pLeftX + 31f, pLeftY - 13f, p)
        c.drawLine(pLeftX + 22f, pLeftY - 13f, pLeftX + 33f, pLeftY - 16f, p)
        p.style = Paint.Style.FILL
        for ((txOff, tyOff) in listOf(-12f to -14f, 18f to -24f)) {
          val tx = pBotX + txOff; val ty = pBotY + tyOff
          p.style = Paint.Style.STROKE; p.strokeWidth = 2.2f; p.color = 0xFF4F2F16.toInt()
          c.drawLine(tx - 6f, ty + 14f, tx, ty - 2f, p); c.drawLine(tx + 6f, ty + 14f, tx, ty - 2f, p)
          p.style = Paint.Style.FILL
          p.color = 0xFFC79B45.toInt(); c.drawCircle(tx, ty, 11.5f, p)
          p.color = 0xFFF5ECD4.toInt(); c.drawCircle(tx, ty, 9.2f, p)
          p.color = 0xFFC93232.toInt(); c.drawCircle(tx, ty, 6.0f, p)
          p.color = 0xFFF2C964.toInt(); c.drawCircle(tx, ty, 2.8f, p)
          p.style = Paint.Style.STROKE; p.strokeWidth = 1.4f; p.color = 0xFF3E220D.toInt()
          c.drawLine(tx + 1f, ty - 1f, tx + 9f, ty - 8f, p)
          p.style = Paint.Style.FILL
        }
      }

      BuildingType.STABLE -> {
        drawCourtyardApron(dirtS)
        val wallH = 46f
        val ixT = cx; val iyT = pTopY + 14f
        val ixR = pRightX - 20f; val iyR = pRightY
        val ixB = cx; val iyB = pBotY - 14f
        val ixL = pLeftX + 20f; val iyL = pLeftY
        drawIsoBox(ixT, iyT, ixR, iyR, ixB, iyB, ixL, iyL, wallH, timberS, stonePlinthH = 11f)
        drawTimberFraming(ixL, iyL, ixB, iyB, ixR, iyR, wallH)
        drawGableRoof(ixT, iyT, ixR, iyR, ixB, iyB, ixL, iyL, wallH, 30f, thatchS, timberS, overhang = 7f)
        drawArchDoor((ixL + ixB) * 0.5f, (iyL + iyB) * 0.5f, 18f, 26f)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2.4f; p.color = 0xFFE8BE54.toInt()
        c.drawArc(RectF(cx - 26f, iyB - 48f, cx - 14f, iyB - 34f), 160f, 220f, false, p)
        p.strokeWidth = 2.6f; p.color = 0xFF472811.toInt()
        c.drawLine(cx + 12f, iyB - 58f, cx + 22f, iyB - 53f, p)
        p.strokeWidth = 1.3f; p.color = 0xFFD9C38F.toInt()
        c.drawLine(cx + 21f, iyB - 53f, cx + 21f, iyB - 38f, p)
        p.style = Paint.Style.FILL
        p.color = 0xFFE3B74B.toInt()
        c.drawRect(pBotX + 12f, pBotY - 16f, pBotX + 32f, pBotY - 4f, p)
        c.drawRect(pBotX + 12f, pBotY - 16f, pBotX + 32f, pBotY - 4f, aoEdge)
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.1f; p.color = 0xFF8A5D20.toInt()
        c.drawLine(pBotX + 14f, pBotY - 10f, pBotX + 30f, pBotY - 10f, p)
        // Timber horse watering trough with sunlit water sheen
        p.style = Paint.Style.FILL; p.shader = timberS
        c.drawRect(pRightX - 38f, pRightY - 6f, pRightX - 16f, pRightY + 6f, p); p.shader = null
        c.drawRect(pRightX - 38f, pRightY - 6f, pRightX - 16f, pRightY + 6f, aoEdge)
        p.color = 0xFF2B6B8F.toInt(); c.drawRect(pRightX - 36f, pRightY - 4f, pRightX - 18f, pRightY + 3f, p)
        p.color = 0x99AEE8FA.toInt(); c.drawRect(pRightX - 33f, pRightY - 2.8f, pRightX - 24f, pRightY - 1.2f, p)
      }

      BuildingType.BLACKSMITH -> {
        drawCourtyardApron(stoneS, 0.14f)
        val wallH = 46f
        val ixT = cx; val iyT = pTopY + 14f
        val ixR = pRightX - 22f; val iyR = pRightY
        val ixB = cx; val iyB = pBotY - 14f
        val ixL = pLeftX + 22f; val iyL = pLeftY
        drawChimneyStack(cx + 16f, pTopY - 44f, iyT + 8f, 16f)
        drawIsoBox(ixT, iyT, ixR, iyR, ixB, iyB, ixL, iyL, wallH, stoneS, quoins = true)
        drawIsoRoof(ixT, iyT, ixR, iyR, ixB, iyB, ixL, iyL, wallH, 28f, slateS, overhang = 6f)
        p.shader = RadialGradient(ixL + 22f, iyL - 10f, 20f, 0x99E08D26.toInt(), 0x00FF4D00, Shader.TileMode.CLAMP)
        c.drawCircle(ixL + 22f, iyL - 10f, 20f, p); p.shader = null
        p.color = 0xFF211712.toInt(); c.drawArc(RectF(ixL + 13f, iyL - 20f, ixL + 31f, iyL - 2f), 180f, 180f, true, p)
        p.color = 0xFFD9641E.toInt(); c.drawOval(RectF(ixL + 15f, iyL - 12f, ixL + 29f, iyL - 4f), p)
        p.color = 0xFFFCE28B.toInt(); c.drawCircle(ixL + 22f, iyL - 8f, 2.5f, p)
        p.color = 0xFF282D33.toInt(); c.drawRect(pBotX - 25f, pBotY - 16f, pBotX - 6f, pBotY - 8f, p)
        p.color = 0xFFB8C4CE.toInt(); c.drawRect(pBotX - 24f, pBotY - 16f, pBotX - 7f, pBotY - 14f, p)
        // Red-hot forged blade resting atop anvil + hammer
        p.color = 0xFFFF7A24.toInt(); c.drawRect(pBotX - 21f, pBotY - 17.5f, pBotX - 10f, pBotY - 15.5f, p)
        p.color = 0xFFFFE078.toInt(); c.drawRect(pBotX - 18f, pBotY - 17.2f, pBotX - 13f, pBotY - 16.0f, p)
        p.color = 0xFF7D5029.toInt(); c.drawRect(pBotX - 21f, pBotY - 8f, pBotX - 10f, pBotY, p)
        p.color = 0xFF286085.toInt(); c.drawOval(RectF(pBotX - 4f, pBotY - 8f, pBotX + 12f, pBotY), p)
        p.color = 0xFF4E331A.toInt(); c.drawOval(RectF(pBotX - 5f, pBotY - 9f, pBotX + 13f, pBotY + 1f), aoEdge)
        // Charcoal heap beside quench trough & steel ingots
        p.color = 0xFF1D1B1A.toInt(); c.drawOval(RectF(pBotX - 38f, pBotY - 11f, pBotX - 26f, pBotY - 4f), p)
        p.color = 0xFF9EA9B2.toInt(); c.drawRect(pBotX - 33f, pBotY - 15f, pBotX - 27f, pBotY - 12f, p)
        drawCrateAndBarrel(pBotX + 18f, pBotY - 4f)
      }

      BuildingType.WATCH_TOWER -> {
        drawCourtyardApron(stoneS, 0.12f)
        val wallH = 108f
        val ixT = cx; val iyT = pTopY + 6f
        val ixR = pRightX - 10f; val iyR = pRightY
        val ixB = cx; val iyB = pBotY - 6f
        val ixL = pLeftX + 10f; val iyL = pLeftY
        drawIsoBox(ixT, iyT, ixR, iyR, ixB, iyB, ixL, iyL, wallH, stoneS, stonePlinthH = 18f, quoins = true)
        drawBattlements(ixL, iyL, ixB, iyB, ixR, iyR, wallH, countPerSide = 3)
        p.shader = timberS
        val hL = Path().apply { moveTo(ixL - 4f, iyL - wallH + 14f); lineTo(ixB, iyB - wallH + 14f); lineTo(ixB, iyB - wallH + 3f); lineTo(ixL - 4f, iyL - wallH + 3f); close() }
        val hR = Path().apply { moveTo(ixB, iyB - wallH + 14f); lineTo(ixR + 4f, iyR - wallH + 14f); lineTo(ixR + 4f, iyR - wallH + 3f); lineTo(ixB, iyB - wallH + 3f); close() }
        c.drawPath(hL, p); c.drawPath(hR, p); p.shader = null
        c.drawPath(hL, aoEdge); c.drawPath(hR, aoEdge)
        // Angled timber bracing struts beneath upper hoarding gallery
        p.style = Paint.Style.STROKE; p.strokeWidth = 2.0f; p.color = 0xFF472811.toInt()
        for (i in 1..3) {
          val f = i / 4f
          val sxL = ixL + (ixB - ixL) * f; val syL = iyL + (iyB - iyL) * f - wallH
          c.drawLine(sxL, syL + 22f, sxL - 2.5f, syL + 14f, p)
          val sxR = ixB + (ixR - ixB) * f; val syR = iyB + (iyR - iyB) * f - wallH
          c.drawLine(sxR, syR + 22f, sxR + 2.5f, syR + 14f, p)
        }
        p.style = Paint.Style.FILL
        drawIsoRoof(ixT, iyT, ixR, iyR, ixB, iyB, ixL, iyL, wallH + 6f, 24f, slateS, fMain, overhang = 6f)
        p.color = 0xFF1A140F.toInt()
        c.drawRect(cx - 14f, baseCenterY - 82f, cx - 10f, baseCenterY - 64f, p)
        c.drawRect(cx + 10f, baseCenterY - 82f, cx + 14f, baseCenterY - 64f, p)
        drawShutteredWindow(cx - 14f, baseCenterY - 44f, 6f, 12f)
        drawArchDoor((ixL + ixB) * 0.5f, (iyL + iyB) * 0.5f, 14f, 22f)
        drawIvyCluster(ixL + 6f, iyL - 8f, 7f)
      }

      BuildingType.CASTLE -> {
        drawCourtyardApron(stoneS)
        val wallH = 92f
        val ixT = cx; val iyT = pTopY + 12f
        val ixR = pRightX - 18f; val iyR = pRightY
        val ixB = cx; val iyB = pBotY - 12f
        val ixL = pLeftX + 18f; val iyL = pLeftY

        drawIsoBox(ixT, iyT, ixR, iyR, ixB, iyB, ixL, iyL, wallH, stoneS, stonePlinthH = 20f, quoins = true)
        drawBattlements(ixL, iyL, ixB, iyB, ixR, iyR, wallH, countPerSide = 6)
        // Carved stone machicolation corbels beneath curtain-wall battlements
        p.style = Paint.Style.FILL
        for (i in 1..5) {
          val t = i / 6f
          val mxL = ixL + (ixB - ixL) * t; val myL = iyL + (iyB - iyL) * t - wallH
          p.color = 0xFF5D564C.toInt(); c.drawRect(mxL - 2.2f, myL + 1f, mxL + 2.2f, myL + 6.5f, p)
          val mxR = ixB + (ixR - ixB) * t; val myR = iyB + (iyR - iyB) * t - wallH
          p.color = 0xFF3E3A34.toInt(); c.drawRect(mxR - 2.2f, myR + 1f, mxR + 2.2f, myR + 6.5f, p)
        }

        fun drawTurret(tx: Float, ty: Float, spireH: Float) {
          val th = wallH + 36f
          val leftRect = RectF(tx - 17f, ty - th, tx, ty - wallH + 14f)
          val rightRect = RectF(tx, ty - th, tx + 17f, ty - wallH + 14f)
          p.style = Paint.Style.FILL; p.shader = stoneS
          c.drawRect(leftRect, p); c.drawRect(rightRect, p); p.shader = null
          p.color = 0x22FFF2D4; c.drawRect(leftRect, p)
          p.color = 0x4E120D08; c.drawRect(rightRect, p)
          c.drawRect(tx - 17f, ty - th, tx + 17f, ty - wallH + 14f, aoEdge)

          val spireL = Path().apply { moveTo(tx, ty - th - spireH); lineTo(tx, ty - th); lineTo(tx - 21f, ty - th); close() }
          val spireR = Path().apply { moveTo(tx, ty - th - spireH); lineTo(tx + 21f, ty - th); lineTo(tx, ty - th); close() }
          p.shader = slateS; c.drawPath(spireL, p); c.drawPath(spireR, p); p.shader = null
          p.color = (fMain and 0x00FFFFFF) or 0x88000000.toInt(); c.drawPath(spireL, p); c.drawPath(spireR, p)
          p.color = 0x44000000; c.drawPath(spireR, p)
          c.drawPath(spireL, aoEdge); c.drawPath(spireR, aoEdge)
          p.color = 0xFFF2C964.toInt(); c.drawCircle(tx, ty - th - spireH - 2f, 3.2f, p)
        }

        drawTurret(ixT, iyT, 26f)
        drawTurret(ixL, iyL, 26f)
        drawTurret(ixR, iyR, 26f)

        val dTopX = cx; val dTopY = baseCenterY - 40f
        val dRightX = cx + 44f; val dRightY = baseCenterY - 18f
        val dBotX = cx; val dBotY = baseCenterY + 4f
        val dLeftX = cx - 44f; val dLeftY = baseCenterY - 18f
        drawIsoBox(dTopX, dTopY, dRightX, dRightY, dBotX, dBotY, dLeftX, dLeftY, wallH + 56f, stoneS, quoins = true)
        drawBattlements(dLeftX, dLeftY, dBotX, dBotY, dRightX, dRightY, wallH + 56f, countPerSide = 3)
        drawIsoRoof(dTopX, dTopY, dRightX, dRightY, dBotX, dBotY, dLeftX, dLeftY, wallH + 58f, 30f, slateS, fMain, overhang = 4f)

        // Keep arrow slits & shuttered royal windows
        p.color = 0xFF1A1511.toInt()
        c.drawRect(cx - 24f, baseCenterY - wallH - 12f, cx - 21f, baseCenterY - wallH + 2f, p)
        c.drawRect(cx + 21f, baseCenterY - wallH - 12f, cx + 24f, baseCenterY - wallH + 2f, p)
        drawShutteredWindow(cx - 18f, baseCenterY - wallH - 36f, 8f, 16f)
        drawShutteredWindow(cx + 10f, baseCenterY - wallH - 36f, 8f, 16f)
        drawShieldEmblem(cx - 26f, iyB - 58f, 9f)
        drawShieldEmblem(cx + 26f, iyB - 58f, 9f)

        p.color = fMain
        c.drawRect(cx - 10f, iyB - 98f, cx - 2f, iyB - 76f, p)
        c.drawRect(cx + 2f, iyB - 98f, cx + 10f, iyB - 76f, p)
        p.color = 0xFFE8BE54.toInt()
        c.drawRect(cx - 10f, iyB - 80f, cx - 2f, iyB - 76f, p)
        c.drawRect(cx + 2f, iyB - 80f, cx + 10f, iyB - 76f, p)

        drawTurret(ixB, iyB, 28f)
        val gX = (ixL + ixB) * 0.5f; val gY = (iyL + iyB) * 0.5f
        drawArchDoor(gX, gY, 24f, 36f, portcullis = true)
        // Lowered heavy timber drawbridge planks & iron suspension chains
        val db = Path().apply {
          moveTo(gX - 13f, gY); lineTo(gX + 13f, gY)
          lineTo(gX + 5f, gY + 14f); lineTo(gX - 21f, gY + 14f); close()
        }
        p.style = Paint.Style.FILL; p.shader = timberS; c.drawPath(db, p); p.shader = null; c.drawPath(db, aoEdge)
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.4f; p.color = 0xFF474E56.toInt()
        c.drawLine(gX - 11f, gY - 22f, gX - 18f, gY + 12f, p)
        c.drawLine(gX + 11f, gY - 22f, gX + 3f, gY + 12f, p)
        p.style = Paint.Style.FILL
        drawWallTorch(gX - 18f, gY - 16f)
        drawWallTorch(gX + 18f, gY - 16f)
        drawIvyCluster(ixL + 12f, iyL - 12f, 10f)
        drawIvyCluster(ixR - 14f, iyR - 10f, 9f)
      }

      BuildingType.FARM -> {
        drawCourtyardApron(ProceduralTextures.scaledShader(ProceduralTextures.grassShader, 0.24f))
        val gateX = (pBotX + pRightX) * 0.5f
        val gateY = (pBotY + pRightY) * 0.5f
        p.style = Paint.Style.FILL; p.shader = dirtS
        c.drawOval(RectF(gateX - 18f, gateY - 9f, gateX + 16f, gateY + 9f), p)
        p.shader = null

        fun insetPt(px: Float, py: Float, r: Float): Pair<Float, Float> =
          (cx + (px - cx) * r) to (baseCenterY + (py - baseCenterY) * r)
        val (sTopX, sTopY) = insetPt(pTopX, pTopY, 0.87f)
        val (sRightX, sRightY) = insetPt(pRightX, pRightY, 0.87f)
        val (sBotX, sBotY) = insetPt(pBotX, pBotY, 0.87f)
        val (sLeftX, sLeftY) = insetPt(pLeftX, pLeftY, 0.87f)

        val soilPlot = Path().apply {
          moveTo(sTopX, sTopY); lineTo(sRightX, sRightY); lineTo(sBotX, sBotY); lineTo(sLeftX, sLeftY); close()
        }
        p.shader = plowedS; c.drawPath(soilPlot, p); p.shader = null
        c.drawPath(soilPlot, aoEdge)

        p.style = Paint.Style.STROKE
        for (i in -5..5) {
          val u = (i + 5f) / 10f
          val sx = sLeftX + (sTopX - sLeftX) * u
          val sy = sLeftY + (sTopY - sLeftY) * u
          val ex = sBotX + (sRightX - sBotX) * u
          val ey = sBotY + (sRightY - sBotY) * u
          p.strokeWidth = 3.2f; p.color = 0xCC35200E.toInt()
          c.drawLine(sx, sy, ex, ey, p)
          p.strokeWidth = 1.6f; p.color = 0xAA8A5D30.toInt()
          c.drawLine(sx, sy - 1.5f, ex, ey - 1.5f, p)
        }
        p.style = Paint.Style.FILL

        val hTopX = sTopX; val hTopY = sTopY + 4f
        val hRightX = sTopX + 26f; val hRightY = sTopY + 17f
        val hBotX = sTopX; val hBotY = sTopY + 30f
        val hLeftX = sTopX - 26f; val hLeftY = sTopY + 17f
        drawIsoBox(hTopX, hTopY, hRightX, hRightY, hBotX, hBotY, hLeftX, hLeftY, 20f, timberS, stonePlinthH = 5f)
        drawIsoRoof(hTopX, hTopY, hRightX, hRightY, hBotX, hBotY, hLeftX, hLeftY, 20f, 15f, thatchS, overhang = 4f)
        drawArchDoor((hLeftX + hBotX) * 0.5f, (hLeftY + hBotY) * 0.5f, 9f, 13f)
        p.color = 0xFF6B4221.toInt(); c.drawOval(RectF(hRightX + 4f, hRightY - 12f, hRightX + 16f, hRightY + 1f), p)
        c.drawOval(RectF(hRightX + 4f, hRightY - 12f, hRightX + 16f, hRightY + 1f), aoEdge)
        p.color = 0xFF3484A8.toInt(); c.drawOval(RectF(hRightX + 5.5f, hRightY - 11.5f, hRightX + 14.5f, hRightY - 6.5f), p)

        // Field Scarecrow + perched black crow
        val scX = cx - 22f; val scY = baseCenterY - 8f
        p.color = 0x45000000; c.drawOval(RectF(scX - 8f, scY - 2f, scX + 10f, scY + 4f), p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2.4f; p.color = 0xFF422A16.toInt()
        c.drawLine(scX, scY + 1f, scX, scY - 25f, p)
        c.drawLine(scX - 11f, scY - 18f, scX + 11f, scY - 19f, p)
        p.style = Paint.Style.FILL
        p.color = 0xFF704D2E.toInt(); c.drawRect(scX - 4.5f, scY - 20f, scX + 4.5f, scY - 8f, p)
        p.color = fMain; c.drawRect(scX - 4f, scY - 21f, scX + 4f, scY - 18f, p)
        p.color = 0xFFC4A05C.toInt(); c.drawCircle(scX, scY - 23f, 3.1f, p)
        p.color = 0xFF96753B.toInt(); c.drawOval(RectF(scX - 7.5f, scY - 27f, scX + 7.5f, scY - 23f), p)
        c.drawArc(RectF(scX - 4f, scY - 30f, scX + 4f, scY - 24f), 180f, 180f, true, p)
        p.color = 0xFF1A1B1E.toInt(); c.drawCircle(scX + 6f, scY - 27f, 1.4f, p)

        for ((sx, sy) in listOf(pBotX - 24f to pBotY - 18f, pBotX - 12f to pBotY - 12f)) {
          p.color = 0xFFC79F44.toInt(); c.drawOval(RectF(sx - 6f, sy - 11f, sx + 6f, sy + 2f), p)
          c.drawOval(RectF(sx - 6f, sy - 11f, sx + 6f, sy + 2f), aoEdge)
          p.color = 0xFF6B4221.toInt(); c.drawRect(sx - 5f, sy - 5f, sx + 5f, sy - 3f, p)
        }
        p.color = 0xFF845B32.toInt(); c.drawOval(RectF(pBotX + 16f, pBotY - 18f, pBotX + 30f, pBotY - 6f), p)
        c.drawOval(RectF(pBotX + 16f, pBotY - 18f, pBotX + 30f, pBotY - 6f), aoEdge)
        p.color = 0xFFD6B96E.toInt(); c.drawOval(RectF(pBotX + 18f, pBotY - 18f, pBotX + 28f, pBotY - 12f), p)

        for ((fx, fy, fcol) in listOf(
          Triple(pLeftX + 18f, pLeftY + 12f, 0xFFA6362E.toInt()),
          Triple(pLeftX + 32f, pLeftY + 18f, 0xFFC9AF50.toInt()),
          Triple(pRightX - 22f, pRightY + 10f, 0xFFA6362E.toInt()),
          Triple(pBotX - 8f, pBotY - 3f, 0xFFC9AF50.toInt()),
          Triple(pTopX + 22f, pTopY + 12f, 0xFFE3DAC6.toInt()),
        )) {
          p.color = fcol; c.drawCircle(fx, fy, 1.8f, p)
        }

        fun drawFenceSegment(x1: Float, y1: Float, x2: Float, y2: Float) {
          p.style = Paint.Style.STROKE
          p.strokeWidth = 2.4f; p.color = 0xFF472911.toInt()
          c.drawLine(x1, y1 - 4f, x2, y2 - 4f, p)
          p.strokeWidth = 2.6f; p.color = 0xFF8F5D34.toInt()
          c.drawLine(x1, y1 - 9f, x2, y2 - 9f, p)
          p.style = Paint.Style.FILL
        }
        drawFenceSegment(pTopX, pTopY, pRightX, pRightY)
        drawFenceSegment(pTopX, pTopY, pLeftX, pLeftY)
        drawFenceSegment(pLeftX, pLeftY, pBotX, pBotY)
        val g1x = pBotX + (pRightX - pBotX) * 0.34f; val g1y = pBotY + (pRightY - pBotY) * 0.34f
        val g2x = pBotX + (pRightX - pBotX) * 0.66f; val g2y = pBotY + (pRightY - pBotY) * 0.66f
        drawFenceSegment(pBotX, pBotY, g1x, g1y)
        drawFenceSegment(g2x, g2y, pRightX, pRightY)
        drawFenceSegment(g1x, g1y, g1x + 11f, g1y + 11f)

        val posts = listOf(
          pTopX to pTopY,
          (pTopX + pRightX) * 0.5f to (pTopY + pRightY) * 0.5f,
          pRightX to pRightY,
          (pTopX + pLeftX) * 0.5f to (pTopY + pLeftY) * 0.5f,
          pLeftX to pLeftY,
          (pLeftX + pBotX) * 0.5f to (pLeftY + pBotY) * 0.5f,
          pBotX to pBotY,
          g1x to g1y,
          g2x to g2y,
        )
        for ((px, py) in posts) {
          p.color = 0xFF6B6257.toInt(); c.drawOval(RectF(px - 3.5f, py - 2f, px + 3.5f, py + 2.5f), p)
          p.color = 0xFF4A2B12.toInt(); c.drawRect(px - 2.6f, py - 13f, px + 2.6f, py + 1f, p)
          p.color = 0xFF9C6B3B.toInt(); c.drawRect(px - 2.6f, py - 13f, px + 2.6f, py - 9.5f, p)
          c.drawRect(px - 2.6f, py - 13f, px + 2.6f, py + 1f, aoEdge)
        }
      }

      BuildingType.WAR_FACTORY -> {
        drawCourtyardApron(concreteS)
        // Reinforced concrete assembly apron + hazard striping
        drawCourtyardApron(stoneS, 0.28f)
        val wallH = 62f
        val ixT = cx; val iyT = pTopY + 18f
        val ixR = pRightX - 24f; val iyR = pRightY - 4f
        val ixB = cx - 4f; val iyB = pBotY - 18f
        val ixL = pLeftX + 24f; val iyL = pLeftY - 4f

        // Twin tall riveted foundry smokestacks behind hall
        drawChimneyStack(cx - 34f, pTopY - 62f, iyT + 18f, 15f)
        drawChimneyStack(cx + 22f, pTopY - 58f, iyT + 22f, 14f)

        drawIsoBox(ixT, iyT, ixR, iyR, ixB, iyB, ixL, iyL, wallH, brickS, stonePlinthH = 14f)

        // Sawtooth industrial roof & corrugated skylights
        drawGableRoof(ixT, iyT, ixR, iyR, ixB, iyB, ixL, iyL, wallH, 28f, slateS, brickS, fDark, overhang = 5f)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2.0f; p.color = 0xFF7FAAC4.toInt()
        c.drawLine(cx - 48f, iyL - wallH - 6f, cx + 10f, iyT - wallH - 22f, p)
        c.drawLine(cx - 28f, iyL - wallH + 6f, cx + 30f, iyT - wallH - 10f, p)

        // Large armored vehicle assembly bay doors with caution chevron stripes & safety bollards
        val bayX = (ixL + ixB) * 0.5f; val bayY = (iyL + iyB) * 0.5f
        p.style = Paint.Style.FILL; p.color = 0xFF1A1D21.toInt()
        c.drawRect(bayX - 22f, bayY - 38f, bayX + 22f, bayY + 2f, p)
        p.color = 0xFF3E454D.toInt()
        c.drawRect(bayX - 20f, bayY - 36f, bayX + 20f, bayY - 26f, p)
        for (s in -3..3) {
          p.color = if (s % 2 == 0) 0xFFE3B738.toInt() else 0xFF1B1D20.toInt()
          c.drawRect(bayX + s * 6f - 3f, bayY - 41f, bayX + s * 6f + 3f, bayY - 36f, p)
        }
        p.color = 0xFFE3B738.toInt()
        c.drawRect(bayX - 25f, bayY - 7f, bayX - 22f, bayY + 3f, p)
        c.drawRect(bayX + 22f, bayY - 7f, bayX + 25f, bayY + 3f, p)
        // Tank tracks visible inside assembly bay
        p.color = 0xFF56614A.toInt(); c.drawRect(bayX - 11f, bayY - 14f, bayX + 11f, bayY - 2f, p)
        p.color = fMain; c.drawRect(bayX - 5f, bayY - 18f, bayX + 5f, bayY - 13f, p)

        // Heavy steel gantry crane jib over courtyard
        p.style = Paint.Style.STROKE; p.strokeWidth = 3.2f; p.color = 0xFF3E454D.toInt()
        c.drawLine(ixB + 14f, iyB - 4f, ixB + 14f, iyB - 58f, p)
        c.drawLine(ixB + 14f, iyB - 58f, ixB + 56f, iyB - 70f, p)
        p.strokeWidth = 1.4f; p.color = 0xFFC5CFD8.toInt()
        c.drawLine(ixB + 46f, iyB - 67f, ixB + 46f, iyB - 32f, p)
        p.style = Paint.Style.FILL; p.color = 0xFFE0B446.toInt()
        c.drawCircle(ixB + 46f, iyB - 30f, 3.5f, p)

        // Industrial steel wall buttresses, ventilation louvers & munition shell racks
        p.color = 0xFF424952.toInt()
        c.drawRect(cx + 18f, iyB - 68f, cx + 23f, iyB - 10f, p)
        c.drawRect(cx + 46f, iyB - 82f, cx + 51f, iyB - 24f, p)
        drawShutteredWindow(cx + 31f, iyB - 46f, 11f, 12f)
        drawShutteredWindow(cx + 58f, iyB - 58f, 11f, 12f)
        drawShieldEmblem(bayX, bayY - 54f, 8.5f)
        drawCrateAndBarrel(pBotX + 26f, pBotY - 10f)
        p.color = 0xFF3B4D33.toInt(); c.drawRect(pBotX - 44f, pBotY - 16f, pBotX - 24f, pBotY - 6f, p)
        c.drawRect(pBotX - 44f, pBotY - 16f, pBotX - 24f, pBotY - 6f, aoEdge)
        p.color = 0xFFD9AE46.toInt()
        for (sh in 0..2) c.drawCircle(pBotX - 40f + sh * 4.5f, pBotY - 18f, 1.9f, p)
      }

      BuildingType.AIRFIELD -> {
        drawCourtyardApron(concreteS)
        // Painted asphalt airstrip lane across lower courtyard
        val strip = Path().apply {
          moveTo(pLeftX + 12f, pLeftY - 6f)
          lineTo(pBotX - 22f, pBotY - 22f)
          lineTo(pRightX - 10f, pRightY + 4f)
          lineTo(pBotX + 16f, pBotY + 12f)
          close()
        }
        p.style = Paint.Style.FILL; p.color = 0xFF282C30.toInt(); c.drawPath(strip, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2.0f; p.color = 0xFFEBE4D0.toInt()
        for (dash in 0..5) {
          val f0 = 0.08f + dash * 0.15f
          val f1 = f0 + 0.08f
          val sx = pLeftX + 18f + (pRightX - pLeftX - 36f) * f0
          val sy = pLeftY + 2f + (pBotY - pLeftY) * 0.5f * f0
          val ex = pLeftX + 18f + (pRightX - pLeftX - 36f) * f1
          val ey = pLeftY + 2f + (pBotY - pLeftY) * 0.5f * f1
          c.drawLine(sx, sy, ex, ey, p)
        }
        p.style = Paint.Style.FILL

        // Arched corrugated steel aircraft hangar on upper-left quadrant
        val hTopX = cx - 22f; val hTopY = pTopY + 18f
        val hRightX = cx + 32f; val hRightY = baseCenterY - 18f
        val hBotX = cx - 26f; val hBotY = baseCenterY + 6f
        val hLeftX = pLeftX + 20f; val hLeftY = pLeftY - 14f
        drawIsoBox(hTopX, hTopY, hRightX, hRightY, hBotX, hBotY, hLeftX, hLeftY, 44f, slateS, stonePlinthH = 10f)
        drawGableRoof(hTopX, hTopY, hRightX, hRightY, hBotX, hBotY, hLeftX, hLeftY, 44f, 22f, slateS, concreteS, fMain, overhang = 4f)
        val doorX = (hLeftX + hBotX) * 0.5f; val doorY = (hLeftY + hBotY) * 0.5f
        p.color = 0xFF16191D.toInt(); c.drawRect(doorX - 24f, doorY - 34f, doorX + 24f, doorY + 1f, p)
        p.color = 0xFF707880.toInt(); c.drawRect(doorX - 24f, doorY - 36f, doorX + 24f, doorY - 31f, p)
        drawShieldEmblem((hTopX + hRightX) * 0.5f, hRightY - 46f, 8.0f)

        // Concrete & glass Air-Traffic Control Tower on upper-right edge
        val tcTopX = cx + 48f; val tcTopY = pTopY + 38f
        val tcRightX = cx + 82f; val tcRightY = pTopY + 55f
        val tcBotX = cx + 48f; val tcBotY = pTopY + 72f
        val tcLeftX = cx + 14f; val tcLeftY = pTopY + 55f
        drawIsoBox(tcTopX, tcTopY, tcRightX, tcRightY, tcBotX, tcBotY, tcLeftX, tcLeftY, 82f, concreteS, stonePlinthH = 12f)
        // Tinted ATC observation cab deck
        p.color = 0xFF28607D.toInt()
        c.drawRect(tcLeftX - 4f, tcBotY - 102f, tcRightX + 4f, tcBotY - 82f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2.0f; p.color = 0xFF1F2327.toInt()
        c.drawRect(tcLeftX - 4f, tcBotY - 102f, tcRightX + 4f, tcBotY - 82f, p)
        p.style = Paint.Style.FILL; p.color = 0xFF424952.toInt()
        c.drawRect(tcLeftX - 7f, tcBotY - 108f, tcRightX + 7f, tcBotY - 101f, p)
        // Radio mast
        p.style = Paint.Style.STROKE; p.strokeWidth = 2.2f; p.color = 0xFFD0D8E0.toInt()
        c.drawLine(tcBotX, tcBotY - 108f, tcBotX, tcBotY - 134f, p)
        p.style = Paint.Style.FILL

        // Orange-white airfield windsock + parked fuel bowser truck
        val wsX = pRightX - 38f; val wsY = pRightY - 10f
        p.style = Paint.Style.STROKE; p.strokeWidth = 2.0f; p.color = 0xFFD9E0E6.toInt()
        c.drawLine(wsX, wsY, wsX, wsY - 26f, p)
        p.style = Paint.Style.FILL; p.color = 0xFFE36B24.toInt()
        c.drawRect(wsX, wsY - 26f, wsX + 12f, wsY - 21f, p)
        p.color = 0xFFF2ECE0.toInt()
        c.drawRect(wsX + 4f, wsY - 26f, wsX + 8f, wsY - 21f, p)

        // Fuel bowser, wheel chocks & concrete jet blast deflector
        p.color = 0xFF565E66.toInt()
        c.drawRect(pLeftX + 34f, pLeftY - 18f, pLeftX + 54f, pLeftY - 8f, p)
        c.drawRect(pLeftX + 34f, pLeftY - 18f, pLeftX + 54f, pLeftY - 8f, aoEdge)
        p.color = 0xFF495440.toInt(); c.drawRoundRect(RectF(pBotX + 18f, pBotY - 26f, pBotX + 46f, pBotY - 14f), 4f, 4f, p)
        c.drawRoundRect(RectF(pBotX + 18f, pBotY - 26f, pBotX + 46f, pBotY - 14f), 4f, 4f, aoEdge)
        p.color = 0xFF1F2226.toInt()
        c.drawCircle(pBotX + 24f, pBotY - 12f, 3.2f, p); c.drawCircle(pBotX + 40f, pBotY - 12f, 3.2f, p)
      }

      BuildingType.UNIVERSITY -> {
        drawCourtyardApron(stoneS)
        drawCourtyardApron(dirtS, 0.24f)

        // Grand Neo-Gothic library & lecture hall on upper-left wing
        val uTopX = cx - 22f; val uTopY = pTopY + 18f
        val uRightX = cx + 44f; val uRightY = baseCenterY - 16f
        val uBotX = cx - 18f; val uBotY = baseCenterY + 18f
        val uLeftX = pLeftX + 24f; val uLeftY = pLeftY - 14f
        drawIsoBox(uTopX, uTopY, uRightX, uRightY, uBotX, uBotY, uLeftX, uLeftY, 64f, plasterS, stonePlinthH = 16f, quoins = true)
        drawTimberFraming(uLeftX, uLeftY, uBotX, uBotY, uRightX, uRightY, 64f, addXBraces = false)
        drawGableRoof(uTopX, uTopY, uRightX, uRightY, uBotX, uBotY, uLeftX, uLeftY, 64f, 34f, slateS, stoneS, fMain, overhang = 7f)

        // Tall gothic lancet windows along scholarly facade
        val winMidX = (uLeftX + uBotX) * 0.5f
        val winMidY = (uLeftY + uBotY) * 0.5f
        drawArchDoor(winMidX, winMidY, 20f, 32f)
        drawWallTorch(winMidX - 15f, winMidY - 14f)
        drawWallTorch(winMidX + 15f, winMidY - 14f)
        drawShutteredWindow(winMidX - 28f, winMidY - 52f, 9f, 18f)
        drawShutteredWindow(winMidX - 6f, winMidY - 54f, 9f, 18f)
        drawShutteredWindow(winMidX + 16f, winMidY - 52f, 9f, 18f)

        // East scholarly wing with stone buttresses & faction heraldry
        val eTopX = cx + 12f; val eTopY = baseCenterY - 18f
        val eRightX = pRightX - 22f; val eRightY = pRightY - 4f
        val eBotX = cx + 38f; val eBotY = pBotY - 36f
        val eLeftX = cx - 8f; val eLeftY = baseCenterY + 2f
        drawIsoBox(eTopX, eTopY, eRightX, eRightY, eBotX, eBotY, eLeftX, eLeftY, 44f, stoneS, stonePlinthH = 10f, quoins = true)
        drawIsoRoof(eTopX, eTopY, eRightX, eRightY, eBotX, eBotY, eLeftX, eLeftY, 44f, 22f, slateS, fMain, overhang = 5f)
        drawShutteredWindow((eBotX + eRightX) * 0.5f - 8f, (eBotY + eRightY) * 0.5f - 30f, 8f, 15f)
        drawShieldEmblem((eLeftX + eBotX) * 0.5f, (eLeftY + eBotY) * 0.5f - 28f, 7.6f)

        // Central astronomical rotunda tower with patinated copper dome & brass telescope
        val rTopX = cx + 10f; val rTopY = baseCenterY - 62f
        val rRightX = cx + 44f; val rRightY = baseCenterY - 44f
        val rBotX = cx + 10f; val rBotY = baseCenterY - 26f
        val rLeftX = cx - 24f; val rLeftY = baseCenterY - 44f
        drawIsoBox(rTopX, rTopY, rRightX, rRightY, rBotX, rBotY, rLeftX, rLeftY, 76f, stoneS, stonePlinthH = 12f, quoins = true)
        drawBattlements(rLeftX, rLeftY, rBotX, rBotY, rRightX, rRightY, 76f, countPerSide = 3)

        val domeCx = rBotX
        val domeBaseY = rBotY - 82f
        val domeR = 29f
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(domeCx - 8f, domeBaseY - 18f, domeR * 1.4f, 0xFF69B5A3.toInt(), 0xFF28574E.toInt(), Shader.TileMode.CLAMP)
        c.drawArc(RectF(domeCx - domeR, domeBaseY - domeR, domeCx + domeR, domeBaseY + domeR), 180f, 180f, true, p)
        p.shader = null
        c.drawArc(RectF(domeCx - domeR, domeBaseY - domeR, domeCx + domeR, domeBaseY + domeR), 180f, 180f, true, aoEdge)
        // Copper dome bronze collar & ribbed meridian seam
        p.style = Paint.Style.STROKE; p.strokeWidth = 2.4f; p.color = 0xFFB89242.toInt()
        c.drawLine(domeCx - domeR, domeBaseY, domeCx + domeR, domeBaseY, p)
        p.strokeWidth = 1.6f; p.color = 0xFF1F423B.toInt()
        c.drawArc(RectF(domeCx - 11f, domeBaseY - domeR, domeCx + 11f, domeBaseY + domeR), 180f, 180f, false, p)
        // Observatory viewing slit & brass telescope barrel
        p.style = Paint.Style.FILL; p.color = 0xFF14181C.toInt()
        c.drawRect(domeCx + 4f, domeBaseY - domeR * 0.78f, domeCx + 12f, domeBaseY - 4f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 3.8f; p.color = 0xFFD9B252.toInt()
        c.drawLine(domeCx + 7f, domeBaseY - 14f, domeCx + 27f, domeBaseY - 26f, p)
        p.strokeWidth = 1.4f; p.color = 0xFFFFF2B8.toInt()
        c.drawCircle(domeCx + 27f, domeBaseY - 26f, 2.0f, p)
        // Gold astrolabe finial atop dome
        p.style = Paint.Style.STROKE; p.strokeWidth = 2.2f; p.color = 0xFFE3BA58.toInt()
        c.drawLine(domeCx, domeBaseY - domeR, domeCx, domeBaseY - domeR - 14f, p)
        p.style = Paint.Style.FILL
        c.drawCircle(domeCx, domeBaseY - domeR - 15f, 2.8f, p)

        // Courtyard octagonal scholarly fountain, open reading lectern & manicured cypress topiaries
        val fCx = cx - 4f; val fCy = pBotY - 34f
        p.color = 0xFF7D7466.toInt(); c.drawOval(RectF(fCx - 20f, fCy - 10f, fCx + 20f, fCy + 10f), p)
        c.drawOval(RectF(fCx - 20f, fCy - 10f, fCx + 20f, fCy + 10f), aoEdge)
        p.color = 0xFF2E6E8A.toInt(); c.drawOval(RectF(fCx - 15f, fCy - 7f, fCx + 15f, fCy + 7f), p)
        p.color = 0xFF9BE0F0.toInt(); c.drawOval(RectF(fCx - 8f, fCy - 3.5f, fCx + 4f, fCy + 1.5f), p)
        p.color = 0xFFA69B8A.toInt(); c.drawRect(fCx - 3f, fCy - 14f, fCx + 3f, fCy, p)
        // Open illuminated parchment tome on wooden courtyard lectern
        val lx0 = fCx - 30f; val ly0 = fCy + 8f
        p.color = 0xFF543318.toInt(); c.drawRect(lx0 - 1.5f, ly0 - 9f, lx0 + 1.5f, ly0 + 1f, p)
        p.color = 0xFF7A4B24.toInt(); c.drawRect(lx0 - 6f, ly0 - 12f, lx0 + 6f, ly0 - 7f, p)
        p.color = 0xFFF2E6C4.toInt(); c.drawRect(lx0 - 4.8f, ly0 - 12.5f, lx0 + 4.8f, ly0 - 8f, p)

        for ((tx0, ty0) in listOf((uLeftX - 4f) to (uLeftY + 14f), (eRightX - 8f) to (eRightY + 8f))) {
          p.color = 0xFF4F2E14.toInt(); c.drawRect(tx0 - 2f, ty0 - 6f, tx0 + 2f, ty0 + 2f, p)
          p.color = 0xFF284D24.toInt(); c.drawOval(RectF(tx0 - 7f, ty0 - 26f, tx0 + 7f, ty0 - 4f), p)
          p.color = 0xFF437338.toInt(); c.drawOval(RectF(tx0 - 5f, ty0 - 24f, tx0 + 3f, ty0 - 8f), p)
        }
        drawCrateAndBarrel(pBotX + 24f, pBotY - 6f)
        drawIvyCluster(uLeftX + 12f, uLeftY - 12f, 9f)
      }
    }

    return bmp
  }
}
