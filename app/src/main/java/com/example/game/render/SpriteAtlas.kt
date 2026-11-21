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
import com.example.game.model.Faction
import com.example.game.model.ResourceNodeType
import com.example.game.model.UnitType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object SpriteAtlas {

  const val SUPERSAMPLE = 4f
  private const val MAX_CACHED_UNITS = 40
  private val resourceCache = HashMap<Pair<ResourceNodeType, Int>, ImageBitmap>()
  private val unitCache =
    object : LinkedHashMap<Int, ImageBitmap>(56, 0.75f, true) {
      override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, ImageBitmap>?): Boolean =
        size > MAX_CACHED_UNITS
    }

  fun getResourceSprite(type: ResourceNodeType, variant: Int): ImageBitmap {
    val key = type to (variant % 3)
    return resourceCache.getOrPut(key) { renderResourceSprite(type, variant % 3).asImageBitmap() }
  }

  fun getUnitSprite(
    type: UnitType,
    faction: Faction,
    facingRight: Boolean,
    walkFrame: Int = 0,
  ): ImageBitmap {
    val frame = walkFrame and 1
    val key =
      (type.ordinal * (Faction.entries.size * 4)) +
        (faction.ordinal * 4) +
        ((if (facingRight) 1 else 0) * 2) +
        frame
    return unitCache.getOrPut(key) { renderUnitSprite(type, faction, facingRight, frame).asImageBitmap() }
  }

  private fun renderResourceSprite(type: ResourceNodeType, variant: Int): Bitmap {
    val lw = 160f
    val lh = 180f
    val bmp = Bitmap.createBitmap((lw * SUPERSAMPLE).toInt(), (lh * SUPERSAMPLE).toInt(), Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    c.scale(SUPERSAMPLE, SUPERSAMPLE)

    val cx = lw * 0.5f
    val gy = lh * 0.80f
    val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val aoEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 0.55f
      color = 0x42100C08
    }
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = 0x72080705
      maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)
    }

    when (type) {
      ResourceNodeType.FOREST_OAK -> {
        val lean = (variant - 1) * 3.2f
        c.drawOval(cx - 36f, gy - 11f, cx + 54f, gy + 17f, shadowPaint)

        // Forest duff, moss & flared buttress roots
        p.style = Paint.Style.FILL
        p.color = 0x99344622.toInt()
        c.drawOval(RectF(cx - 27f, gy + 1f, cx + 27f, gy + 10f), p)
        val trunk = Path().apply {
          moveTo(cx - 20f, gy + 7f); quadTo(cx - 9f, gy - 2f, cx - 6.5f + lean * 0.3f, gy - 47f)
          lineTo(cx + 6.5f + lean * 0.3f, gy - 47f); quadTo(cx + 10f, gy - 2f, cx + 21f, gy + 7f)
          lineTo(cx + 7f, gy + 4f); lineTo(cx, gy + 8.5f); lineTo(cx - 7f, gy + 4f); close()
        }
        p.shader = LinearGradient(cx - 12f, gy, cx + 12f, gy, 0xFF75573D.toInt(), 0xFF281B10.toInt(), Shader.TileMode.CLAMP)
        c.drawPath(trunk, p)
        p.shader = null
        c.drawPath(trunk, aoEdge)

        // Gnarled limbs reaching into canopy + deeply furrowed bark grain
        p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = 3.8f; p.color = 0xFF4A3320.toInt()
        c.drawLine(cx + lean * 0.3f, gy - 36f, cx - 19f + lean * 0.6f, gy - 55f, p)
        c.drawLine(cx + lean * 0.3f, gy - 34f, cx + 18f + lean * 0.6f, gy - 53f, p)
        p.strokeWidth = 2.0f; p.color = 0xFF3A2818.toInt()
        c.drawLine(cx - 12f + lean * 0.5f, gy - 48f, cx - 25f + lean * 0.7f, gy - 61f, p)
        c.drawLine(cx + 11f + lean * 0.5f, gy - 46f, cx + 24f + lean * 0.7f, gy - 59f, p)
        p.strokeWidth = 0.85f; p.color = 0x991E130A.toInt()
        for (bx in listOf(-4.5f, -2f, 0.5f, 3f, 5f)) {
          c.drawLine(cx + bx, gy - 40f, cx + bx * 1.35f, gy + 3f, p)
        }
        p.style = Paint.Style.FILL; p.color = 0xAA291A0D.toInt()
        c.drawOval(RectF(cx - 2.4f, gy - 22f, cx + 1.8f, gy - 16f), p)
        p.color = 0x776B7846.toInt(); c.drawCircle(cx - 4.2f, gy - 12f, 1.8f, p)
        p.strokeCap = Paint.Cap.BUTT
        p.style = Paint.Style.FILL

        val rng = Random(100 + variant * 31)
        // Deep shadowed underside boughs
        for ((ox, oy, rad) in listOf(
          Triple(-24f, -40f, 22f), Triple(24f, -38f, 21f), Triple(0f, -45f, 26f), Triple(-10f, -52f, 22f), Triple(12f, -51f, 21f),
        )) {
          p.color = 0xEE182315.toInt()
          c.drawCircle(cx + ox + lean, gy + oy, rad, p)
        }
        val lobes = listOf(
          Triple(-28f, -54f, 18.5f), Triple(26f, -52f, 18.5f), Triple(-12f, -46f, 20.5f),
          Triple(13f, -46f, 19.5f), Triple(-16f, -66f, 21.5f), Triple(15f, -64f, 20.5f),
          Triple(0f, -76f, 21.5f), Triple(-4f, -57f, 22.5f), Triple(1f, -68f, 18.5f),
        )
        for ((ox, oy, rad) in lobes) {
          val lx = cx + ox + lean + (rng.nextFloat() - 0.5f) * 3.5f
          val ly = gy + oy + (rng.nextFloat() - 0.5f) * 3.5f
          val hiLeaf = if (variant == 2) 0xFF7B7B34.toInt() else 0xFF637C36.toInt()
          val midLeaf = if (variant == 2) 0xFF4F5724.toInt() else 0xFF3A5223.toInt()
          p.shader = RadialGradient(
            lx - rad * 0.38f, ly - rad * 0.38f, rad * 1.18f,
            intArrayOf(hiLeaf, midLeaf, 0xFF192613.toInt()),
            floatArrayOf(0f, 0.56f, 1f),
            Shader.TileMode.CLAMP,
          )
          c.drawCircle(lx, ly, rad, p)
        }
        p.shader = null
        // 620 fine botanical leaf clusters mottled with sunbeams and shade
        for (i in 0 until 620) {
          val ax = cx + lean + (rng.nextFloat() - 0.5f) * 72f
          val ay = gy - 59f + (rng.nextFloat() - 0.5f) * 46f
          val dist = kotlin.math.hypot((ax - (cx + lean)) / 38f, (ay - (gy - 59f)) / 26f)
          if (dist > 1.02f) continue
          p.color =
            when (i % 6) {
              0 -> if (variant == 2) 0xCCA88F3E.toInt() else 0xCC7B9446.toInt()
              1 -> 0xCC567031.toInt()
              2 -> 0xBB3B5424.toInt()
              3 -> if (variant == 2) 0xCC8E6B32.toInt() else 0xBB67823C.toInt()
              4 -> 0x9E253819.toInt()
              else -> 0x85162111.toInt()
            }
          val lr = 1.6f + (i % 3) * 0.48f
          c.drawOval(RectF(ax - lr * 1.25f, ay - lr * 0.68f, ax + lr * 1.25f, ay + lr * 0.68f), p)
        }
        // Sunlit golden-hour upper-left crown rim clusters & subtle fine crown twigs
        p.shader = RadialGradient(
          cx + lean - 14f, gy - 76f, 22f,
          if (variant == 2) 0x6ADDBB5E.toInt() else 0x62A8C764.toInt(),
          0x00000000,
          Shader.TileMode.CLAMP,
        )
        c.drawCircle(cx + lean - 14f, gy - 76f, 22f, p)
        p.shader = null
        // Fallen leaves, mossy root flares & natural forest floor ferns
        p.color = 0xFF5A3B22.toInt()
        c.drawOval(RectF(cx - 20f, gy + 4f, cx - 8f, gy + 8.5f), p)
        c.drawOval(RectF(cx + 8f, gy + 4f, cx + 20f, gy + 8.5f), p)
        p.color = 0xFF394F24.toInt()
        c.drawOval(RectF(cx - 26f, gy + 1.5f, cx - 16f, gy + 6.5f), p)
        p.color = 0xFF94773B.toInt()
        c.drawCircle(cx + 15f, gy + 8f, 1.3f, p); c.drawCircle(cx - 12f, gy + 9f, 1.1f, p)
      }

      ResourceNodeType.FOREST_PINE -> {
        val lean = (variant - 1) * 2.2f
        c.drawOval(cx - 29f, gy - 9f, cx + 44f, gy + 14f, shadowPaint)
        p.color = 0x9942311E.toInt(); c.drawOval(RectF(cx - 23f, gy + 1f, cx + 24f, gy + 9.5f), p)
        p.color = 0xFF4F3620.toInt()
        c.drawOval(RectF(cx - 14f, gy + 3f, cx - 9f, gy + 7.5f), p)
        c.drawOval(RectF(cx + 9f, gy + 4f, cx + 13.5f, gy + 7.5f), p)

        p.shader = LinearGradient(cx - 7f, gy, cx + 7f, gy, 0xFF6B4830.toInt(), 0xFF26170D.toInt(), Shader.TileMode.CLAMP)
        val pTrunk = Path().apply {
          moveTo(cx - 9f, gy + 6f); lineTo(cx - 5.5f, gy - 36f); lineTo(cx + 5.5f, gy - 36f); lineTo(cx + 9f, gy + 6f); close()
        }
        c.drawPath(pTrunk, p)
        p.shader = null
        c.drawPath(pTrunk, aoEdge)
        p.style = Paint.Style.STROKE; p.strokeWidth = 0.85f; p.color = 0x881A0E07.toInt()
        for (bx in listOf(-3.2f, 0f, 3.2f)) {
          c.drawLine(cx + bx, gy - 32f, cx + bx * 1.4f, gy + 4f, p)
        }
        p.style = Paint.Style.FILL

        val rng = Random(300 + variant * 23)
        for (tier in 0..7) {
          val baseY = gy - 13f - tier * 12.2f
          val halfW = 37f - tier * 3.9f
          val tipY = baseY - 25f + tier * 1.1f
          val tx = cx + lean * (tier / 7f)
          val left = Path().apply {
            moveTo(tx, tipY); lineTo(tx, baseY + 6f)
            lineTo(tx - halfW * 0.35f, baseY + 2f); lineTo(tx - halfW * 0.58f, baseY + 5f)
            lineTo(tx - halfW * 0.8f, baseY + 1.5f); lineTo(tx - halfW, baseY + 3f); close()
          }
          val right = Path().apply {
            moveTo(tx, tipY); lineTo(tx + halfW, baseY + 3f)
            lineTo(tx + halfW * 0.8f, baseY + 1.5f); lineTo(tx + halfW * 0.58f, baseY + 5f)
            lineTo(tx + halfW * 0.35f, baseY + 2f); lineTo(tx, baseY + 6f); close()
          }
          p.shader = LinearGradient(tx - halfW, tipY, tx, baseY, 0xFF4F6B40.toInt(), 0xFF1F3822.toInt(), Shader.TileMode.CLAMP)
          c.drawPath(left, p)
          p.shader = LinearGradient(tx, tipY, tx + halfW, baseY, 0xFF1B3321.toInt(), 0xFF0E1B14.toInt(), Shader.TileMode.CLAMP)
          c.drawPath(right, p)
          p.shader = null
          c.drawPath(left, aoEdge); c.drawPath(right, aoEdge)

          // Needle sprays & hanging spruce cones
          p.style = Paint.Style.STROKE; p.strokeWidth = 1.0f
          for (n in 0..14) {
            val f = n / 14f
            val nx = tx - halfW + halfW * 1.9f * f
            val ny = baseY + (rng.nextFloat() - 0.5f) * 6f - (1f - kotlin.math.abs(f - 0.5f) * 1.8f) * 8f
            p.color = if (nx < tx) 0xAA6A8752.toInt() else 0x8816291A.toInt()
            c.drawLine(nx, ny, nx + (rng.nextFloat() - 0.5f) * 4f, ny + 3.5f, p)
          }
          p.style = Paint.Style.FILL
          if (tier in 1..5 && tier % 2 == 1) {
            p.color = 0xFF5D3A1F.toInt()
            c.drawOval(RectF(tx - halfW * 0.4f - 1.3f, baseY + 2f, tx - halfW * 0.4f + 1.3f, baseY + 5.8f), p)
          }
        }
      }

      ResourceNodeType.BERRY_BUSH -> {
        c.drawOval(cx - 33f, gy - 8f, cx + 38f, gy + 12f, shadowPaint)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2.6f; p.color = 0xFF3E2816.toInt()
        c.drawLine(cx, gy + 3f, cx - 14f, gy - 14f, p)
        c.drawLine(cx, gy + 3f, cx + 12f, gy - 13f, p)
        c.drawLine(cx, gy + 1f, cx + 1f, gy - 18f, p)
        p.style = Paint.Style.FILL

        for ((ox, oy, r) in listOf(
          Triple(-17f, -12f, 18f), Triple(17f, -11f, 17f), Triple(-8f, -22f, 19f),
          Triple(9f, -21f, 18f), Triple(0f, -14f, 18f), Triple(-2f, -27f, 15.5f),
        )) {
          p.shader = RadialGradient(cx + ox - 5f, gy + oy - 6f, r * 1.15f, 0xFF597334.toInt(), 0xFF1A2B14.toInt(), Shader.TileMode.CLAMP)
          c.drawCircle(cx + ox, gy + oy, r, p)
        }
        p.shader = null
        val rng = Random(200 + variant * 17)
        for (i in 0 until 130) {
          val lx = cx + (rng.nextFloat() - 0.5f) * 48f
          val ly = gy - 18f + (rng.nextFloat() - 0.5f) * 26f
          p.color =
            when (i % 3) {
              0 -> 0xBB6E8740.toInt()
              1 -> 0x993E5724.toInt()
              else -> 0x881A2B14.toInt()
            }
          c.drawOval(RectF(lx - 2.6f, ly - 1.4f, lx + 2.6f, ly + 1.4f), p)
        }

        val berries = listOf(
          -18f to -14f, -14f to -9f, -8f to -19f, -3f to -13f, 4f to -10f, 14f to -15f,
          18f to -7f, -21f to -6f, 0f to -26f, 9f to -24f, -11f to -26f, 6f to -18f, -1f to -31f,
        )
        for ((bx, by) in berries) {
          p.color = 0x55000000; c.drawCircle(cx + bx + 0.7f, gy + by + 1.0f, 3.6f, p)
          p.shader = RadialGradient(cx + bx - 1.2f, gy + by - 1.2f, 4.4f, 0xFFD94C54.toInt(), 0xFF6B0C14.toInt(), Shader.TileMode.CLAMP)
          c.drawCircle(cx + bx, gy + by, 3.6f, p)
          p.shader = null
          p.color = 0xD8FFF5F5.toInt()
          c.drawCircle(cx + bx - 1.1f, gy + by - 1.1f, 0.9f, p)
        }
        for ((wx, wy) in listOf(-12f to -20f, 12f to -19f, 2f to -22f)) {
          p.color = 0xE6EFE8D6.toInt(); c.drawCircle(cx + wx, gy + wy, 1.9f, p)
          p.color = 0xFFCFA442.toInt(); c.drawCircle(cx + wx, gy + wy, 0.75f, p)
        }
        p.color = 0xFF941B22.toInt()
        c.drawCircle(cx - 10f, gy + 5f, 1.8f, p); c.drawCircle(cx + 14f, gy + 6f, 1.7f, p)
      }

      ResourceNodeType.GOLD_MINE -> {
        c.drawOval(cx - 44f, gy - 11f, cx + 52f, gy + 17f, shadowPaint)
        val mainL = Path().apply {
          moveTo(cx - 45f, gy + 6f); lineTo(cx - 33f, gy - 20f); lineTo(cx - 12f, gy - 38f)
          lineTo(cx + 7f, gy - 36f); lineTo(cx + 5f, gy + 13f); close()
        }
        val mainR = Path().apply {
          moveTo(cx + 7f, gy - 36f); lineTo(cx + 32f, gy - 17f); lineTo(cx + 43f, gy + 5f)
          lineTo(cx + 5f, gy + 13f); close()
        }
        p.shader = LinearGradient(cx - 42f, gy - 36f, cx + 7f, gy + 13f, 0xFFA38C6B.toInt(), 0xFF5A4935.toInt(), Shader.TileMode.CLAMP)
        c.drawPath(mainL, p)
        p.shader = LinearGradient(cx, gy - 36f, cx + 43f, gy + 13f, 0xFF574939.toInt(), 0xFF261F18.toInt(), Shader.TileMode.CLAMP)
        c.drawPath(mainR, p)
        p.shader = null
        c.drawPath(mainL, aoEdge); c.drawPath(mainR, aoEdge)

        // Rock cleavage & excavated mine adit
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.2f; p.color = 0x88241A10.toInt()
        c.drawLine(cx - 34f, gy - 10f, cx + 2f, gy - 18f, p)
        c.drawLine(cx - 24f, gy - 24f, cx + 22f, gy - 14f, p)
        p.style = Paint.Style.FILL; p.color = 0xFF140F0B.toInt()
        c.drawArc(RectF(cx - 28f, gy - 12f, cx - 10f, gy + 11f), 180f, 180f, true, p)

        // Timber mine shoring + hanging iron miner's lantern
        p.style = Paint.Style.STROKE; p.strokeWidth = 3.0f; p.color = 0xFF54351C.toInt()
        c.drawLine(cx - 29f, gy + 9f, cx - 26f, gy - 13f, p)
        c.drawLine(cx - 10f, gy + 9f, cx - 12f, gy - 13f, p)
        c.drawLine(cx - 29f, gy - 12f, cx - 9f, gy - 14f, p)
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(cx - 13f, gy - 8f, 7f, 0xBBFFC852.toInt(), 0x00FF7800, Shader.TileMode.CLAMP)
        c.drawCircle(cx - 13f, gy - 8f, 7f, p); p.shader = null
        // Warm amber lantern light spilling onto dark cavern threshold
        p.shader = RadialGradient(cx - 19f, gy + 4f, 14f, 0x4DFFAA3E.toInt(), 0x00000000, Shader.TileMode.CLAMP)
        c.drawOval(RectF(cx - 32f, gy - 3f, cx - 6f, gy + 13f), p); p.shader = null
        p.color = 0xFFFFEA8A.toInt(); c.drawCircle(cx - 13f, gy - 8f, 1.5f, p)
        // Ore-cart wooden rail ties & iron rails emerging from mine adit
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.6f; p.color = 0xFF472C14.toInt()
        for (t in 0..2) {
          val tx = cx - 22f - t * 4.5f; val ty = gy + 9f + t * 2.4f
          c.drawLine(tx - 4f, ty - 2f, tx + 5f, ty + 2f, p)
        }
        p.strokeWidth = 1.2f; p.color = 0xFF565E66.toInt()
        c.drawLine(cx - 21f, gy + 6f, cx - 35f, gy + 13f, p)
        c.drawLine(cx - 15f, gy + 9f, cx - 29f, gy + 16f, p)
        p.style = Paint.Style.FILL
        // Crystalline white quartz spikes clustered around gold seams
        for ((qx, qy, qh) in listOf(Triple(-18f, -15f, 6.5f), Triple(8f, -7f, 5.8f))) {
          val qSpire = Path().apply {
            moveTo(cx + qx - 2f, gy + qy); lineTo(cx + qx, gy + qy - qh); lineTo(cx + qx + 2.5f, gy + qy); close()
          }
          p.color = 0xCCE8EEF2.toInt(); c.drawPath(qSpire, p); c.drawPath(qSpire, aoEdge)
        }

        // Natural quartz & raw metallic gold seams (no cartoon halo)
        for ((gx, gyOff, gr) in listOf(
          Triple(-12f, -19f, 9.6f), Triple(13f, -10f, 8.8f), Triple(2f, -26f, 8.0f),
          Triple(-24f, -3f, 6.2f), Triple(25f, 0f, 5.6f), Triple(16f, -22f, 5.2f),
        )) {
          val vein = Path().apply {
            moveTo(cx + gx, gy + gyOff - gr)
            lineTo(cx + gx + gr * 0.8f, gy + gyOff - gr * 0.1f)
            lineTo(cx + gx + gr * 0.3f, gy + gyOff + gr * 0.8f)
            lineTo(cx + gx - gr * 0.68f, gy + gyOff + gr * 0.55f)
            lineTo(cx + gx - gr * 0.85f, gy + gyOff - gr * 0.25f)
            close()
          }
          p.shader = LinearGradient(
            cx + gx - gr, gy + gyOff - gr,
            cx + gx + gr, gy + gyOff + gr,
            0xFFF2D479.toInt(), 0xFF8C6318.toInt(), Shader.TileMode.CLAMP,
          )
          c.drawPath(vein, p)
          p.shader = null
          val hiFacet = Path().apply {
            moveTo(cx + gx, gy + gyOff - gr)
            lineTo(cx + gx + gr * 0.22f, gy + gyOff)
            lineTo(cx + gx - gr * 0.8f, gy + gyOff - gr * 0.2f)
            close()
          }
          p.color = 0xAAFFF3CC.toInt(); c.drawPath(hiFacet, p)
          c.drawPath(vein, aoEdge)
          p.color = 0xEEFFFDF2.toInt()
          c.drawCircle(cx + gx - gr * 0.2f, gy + gyOff - gr * 0.35f, 1.3f, p)
        }
        // Fallen ore fragments & leaning iron pickaxe
        p.color = 0xFFC79B34.toInt()
        c.drawCircle(cx - 14f, gy + 12f, 2.8f, p); c.drawCircle(cx - 6f, gy + 14f, 2.1f, p)
        c.drawCircle(cx + 12f, gy + 11f, 2.5f, p); c.drawCircle(cx + 18f, gy + 12f, 1.8f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.9f; p.color = 0xFF57361C.toInt()
        c.drawLine(cx + 22f, gy + 12f, cx + 32f, gy - 6f, p)
        p.color = 0xFFB0BCC4.toInt(); c.drawArc(RectF(cx + 24f, gy - 10f, cx + 38f, gy - 2f), 200f, 140f, false, p)
        p.style = Paint.Style.FILL
      }

      ResourceNodeType.STONE_QUARRY -> {
        c.drawOval(cx - 42f, gy - 11f, cx + 48f, gy + 16f, shadowPaint)
        val slabL = Path().apply { moveTo(cx - 42f, gy + 6f); lineTo(cx - 28f, gy - 27f); lineTo(cx + 7f, gy - 34f); lineTo(cx + 5f, gy + 11f); close() }
        val slabR = Path().apply { moveTo(cx + 7f, gy - 34f); lineTo(cx + 38f, gy + 2f); lineTo(cx + 5f, gy + 11f); close() }
        p.shader = LinearGradient(cx - 40f, gy - 34f, cx + 7f, gy + 11f, 0xFFBFC4C7.toInt(), 0xFF6B7075.toInt(), Shader.TileMode.CLAMP)
        c.drawPath(slabL, p)
        p.shader = LinearGradient(cx, gy - 34f, cx + 38f, gy + 11f, 0xFF5A5F63.toInt(), 0xFF282B2E.toInt(), Shader.TileMode.CLAMP)
        c.drawPath(slabR, p)
        p.shader = null
        c.drawPath(slabL, aoEdge); c.drawPath(slabR, aoEdge)

        p.style = Paint.Style.STROKE; p.strokeWidth = 1.3f; p.color = 0x9924272B.toInt()
        c.drawLine(cx - 30f, gy - 15f, cx + 4f, gy - 19f, p)
        c.drawLine(cx - 35f, gy - 3f, cx + 21f, gy - 6f, p)
        c.drawLine(cx - 14f, gy - 30f, cx - 10f, gy + 6f, p)
        p.style = Paint.Style.FILL
        p.color = 0x993B4F24.toInt(); c.drawOval(RectF(cx - 33f, gy + 2f, cx - 10f, gy + 11f), p)
        p.color = 0xFFA2A8AD.toInt(); c.drawRect(cx - 10f, gy + 3f, cx + 6f, gy + 12f, p); c.drawRect(cx - 10f, gy + 3f, cx + 6f, gy + 12f, aoEdge)
        p.color = 0xFF838A90.toInt(); c.drawRect(cx - 7f, gy - 4f, cx + 4f, gy + 3f, p); c.drawRect(cx - 7f, gy - 4f, cx + 4f, gy + 3f, aoEdge)
        p.color = 0xFF5A6066.toInt(); c.drawCircle(cx + 24f, gy + 7f, 6.2f, p)
        p.color = 0xFFD9DFE3.toInt(); c.drawCircle(cx - 14f, gy - 25f, 1.9f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.9f; p.color = 0xFFC7CFD6.toInt()
        c.drawLine(cx - 4f, gy - 29f, cx - 8f, gy - 38f, p)
        c.drawLine(cx + 2f, gy - 28f, cx, gy - 36f, p)
        // Quarry mallet & wooden splitting wedge on foreground stone block
        p.strokeWidth = 1.6f; p.color = 0xFF6B4221.toInt()
        c.drawLine(cx - 8f, gy + 8f, cx + 2f, gy + 1f, p)
        p.style = Paint.Style.FILL; p.color = 0xFF474E56.toInt()
        c.drawRect(cx + 0.5f, gy - 1f, cx + 5.5f, gy + 3f, p)
        p.color = 0xFF9E6B38.toInt()
        c.drawRect(cx + 12f, gy + 4f, cx + 16f, gy + 9f, p)
        p.style = Paint.Style.FILL
      }

      ResourceNodeType.DEER -> {
        c.drawOval(cx - 20f, gy - 4f, cx + 24f, gy + 8f, shadowPaint)
        p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND; p.color = 0xFF472C17.toInt(); p.strokeWidth = 2.2f
        c.drawLine(cx - 11f, gy - 12f, cx - 13f, gy + 2f, p)
        c.drawLine(cx + 9f, gy - 12f, cx + 10f, gy + 2f, p)
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(cx - 17f, gy - 24f, cx + 17f, gy - 7f, 0xFFA36E40.toInt(), 0xFF5C351A.toInt(), Shader.TileMode.CLAMP)
        c.drawOval(RectF(cx - 17f, gy - 24f, cx + 17f, gy - 7.5f), p)
        p.shader = null
        c.drawOval(RectF(cx - 17f, gy - 24f, cx + 17f, gy - 7.5f), aoEdge)

        p.style = Paint.Style.STROKE; p.color = 0xFF6B4324.toInt(); p.strokeWidth = 2.4f
        c.drawLine(cx - 6f, gy - 11f, cx - 7f, gy + 3f, p)
        c.drawLine(cx + 12f, gy - 11f, cx + 14f, gy + 3f, p)
        p.style = Paint.Style.FILL; p.color = 0xFF1F160F.toInt()
        for (fx in listOf(cx - 13f, cx - 7f, cx + 10f, cx + 14f)) c.drawCircle(fx, gy + 3f, 1.3f, p)

        p.color = 0xFFD9C6AC.toInt(); c.drawOval(RectF(cx - 9f, gy - 12f, cx + 10f, gy - 7.8f), p)
        p.color = 0xFFE3D4BE.toInt(); c.drawCircle(cx - 16f, gy - 16f, 2.8f, p)
        c.drawCircle(cx - 7f, gy - 19f, 1.2f, p); c.drawCircle(cx + 1f, gy - 17f, 1.2f, p)
        c.drawCircle(cx - 2f, gy - 20f, 1.1f, p); c.drawCircle(cx + 6f, gy - 18f, 1.0f, p)

        p.color = 0xFF8C5B33.toInt()
        p.style = Paint.Style.STROKE; p.strokeWidth = 4.8f
        c.drawLine(cx + 10f, gy - 18f, cx + 17f, gy - 33f, p)
        p.style = Paint.Style.FILL
        c.drawOval(RectF(cx + 13f, gy - 38f, cx + 25.5f, gy - 29.5f), p)
        p.color = 0xFF7A4E2A.toInt(); c.drawOval(RectF(cx + 12f, gy - 41f, cx + 16.5f, gy - 36f), p)
        p.color = 0xFFD9B294.toInt(); c.drawOval(RectF(cx + 12.8f, gy - 40f, cx + 15.6f, gy - 37.2f), p)
        p.color = 0xFF1A120C.toInt()
        c.drawCircle(cx + 20.5f, gy - 34f, 1.0f, p)
        c.drawCircle(cx + 24.8f, gy - 32.8f, 1.1f, p)
        p.color = 0xB8FFFFFF.toInt(); c.drawCircle(cx + 20.8f, gy - 34.4f, 0.4f, p)
        p.style = Paint.Style.STROKE; p.color = 0xFFD8CCB4.toInt(); p.strokeWidth = 2.0f; p.strokeCap = Paint.Cap.ROUND
        c.drawLine(cx + 16f, gy - 37.5f, cx + 25f, gy - 50f, p)
        c.drawLine(cx + 19.5f, gy - 43.5f, cx + 14f, gy - 50f, p)
        c.drawLine(cx + 21.5f, gy - 46.5f, cx + 28f, gy - 46.5f, p)
        c.drawLine(cx + 18f, gy - 41f, cx + 23.5f, gy - 40f, p)
        p.strokeCap = Paint.Cap.BUTT
      }
    }
    return bmp
  }

  private fun renderUnitSprite(type: UnitType, faction: Faction, facingRight: Boolean, walkFrame: Int): Bitmap {
    val lw = 112f
    val lh = 112f
    val bmp = Bitmap.createBitmap((lw * SUPERSAMPLE).toInt(), (lh * SUPERSAMPLE).toInt(), Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    c.scale(SUPERSAMPLE, SUPERSAMPLE)

    val cx = lw * 0.5f
    val gy = lh * 0.85f
    val dir = if (facingRight) 1f else -1f
    val stride = if (walkFrame == 1) 1f else -1f

    // Desaturated authentic heraldic dye pigments across all 4 empires
    val (fMain, fDark, fLight) =
      when (faction) {
        Faction.PLAYER -> Triple(0xFF28558F.toInt(), 0xFF142B4A.toInt(), 0xFF5D87BC.toInt())
        Faction.ENEMY -> Triple(0xFF9C2926.toInt(), 0xFF4F1312.toInt(), 0xFFC75B58.toInt())
        Faction.ENEMY_2 -> Triple(0xFF246E3C.toInt(), 0xFF10361C.toInt(), 0xFF4CA86B.toInt())
        Faction.ENEMY_3 -> Triple(0xFF6B2D82.toInt(), 0xFF341442.toInt(), 0xFF9F60BA.toInt())
      }

    val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val aoEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 0.55f
      color = 0x4A120D08
    }
    val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = 0x70080705
      maskFilter = BlurMaskFilter(4.5f, BlurMaskFilter.Blur.NORMAL)
    }

    fun drawHumanFace(hx: Float, hy: Float, r: Float, bearded: Boolean, skinBase: Int = 0xFFE3B992.toInt()) {
      val headPath = Path().apply {
        moveTo(hx - r * 0.90f, hy - r * 0.25f)
        cubicTo(hx - r * 0.92f, hy - r * 1.12f, hx + r * 0.92f, hy - r * 1.12f, hx + r * 0.90f, hy - r * 0.2f)
        cubicTo(hx + r * 0.82f, hy + r * 0.65f, hx + dir * r * 0.32f, hy + r * 1.05f, hx + dir * r * 0.08f, hy + r * 1.02f)
        cubicTo(hx - dir * r * 0.48f, hy + r * 0.9f, hx - r * 0.86f, hy + r * 0.45f, hx - r * 0.90f, hy - r * 0.25f)
        close()
      }
      p.style = Paint.Style.FILL
      p.shader = RadialGradient(hx + dir * r * 0.22f, hy - r * 0.2f, r * 1.3f, skinBase, 0xFF9C6C47.toInt(), Shader.TileMode.CLAMP)
      c.drawPath(headPath, p)
      p.shader = null
      c.drawPath(headPath, aoEdge)

      p.color = 0xFF2F1E13.toInt()
      c.drawCircle(hx + dir * r * 0.38f, hy - r * 0.12f, 0.72f, p)
      c.drawCircle(hx - dir * r * 0.04f, hy - r * 0.12f, 0.65f, p)
      p.style = Paint.Style.STROKE; p.strokeWidth = 0.75f; p.color = 0xFF5D3D26.toInt()
      c.drawLine(hx - dir * r * 0.18f, hy - r * 0.35f, hx + dir * r * 0.56f, hy - r * 0.32f, p)
      c.drawLine(hx + dir * r * 0.2f, hy - r * 0.22f, hx + dir * r * 0.26f, hy + r * 0.14f, p)
      p.style = Paint.Style.FILL
      if (bearded) {
        val beard = Path().apply {
          moveTo(hx - r * 0.66f, hy + r * 0.24f)
          lineTo(hx + dir * r * 0.66f, hy + r * 0.28f)
          lineTo(hx + dir * r * 0.14f, hy + r * 1.12f)
          lineTo(hx - dir * r * 0.44f, hy + r * 0.90f)
          close()
        }
        p.color = 0xFF3E291B.toInt()
        c.drawPath(beard, p)
        p.color = 0xFF593D28.toInt()
        c.drawCircle(hx + dir * r * 0.16f, hy + r * 0.55f, r * 0.24f, p)
      }
    }

    fun drawArticulatedLeg(hipX: Float, hipY: Float, footX: Float, footY: Float, isSteel: Boolean, trouserCol: Int) {
      val kneeX = (hipX + footX) * 0.5f + dir * 2.1f
      val kneeY = (hipY + footY) * 0.5f - 1.0f
      p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND
      p.strokeWidth = 4.1f; p.color = trouserCol
      c.drawLine(hipX, hipY, kneeX, kneeY, p)
      p.strokeWidth = 3.6f
      p.color = if (isSteel) 0xFF7B858F.toInt() else 0xFF422A16.toInt()
      c.drawLine(kneeX, kneeY, footX, footY, p)
      p.style = Paint.Style.FILL
      if (isSteel) {
        p.color = 0xFFC5CFD8.toInt()
        c.drawCircle(kneeX + dir * 0.5f, kneeY, 2.1f, p)
      } else {
        p.color = 0xFF52351E.toInt()
        c.drawCircle(kneeX, kneeY + 0.8f, 1.9f, p)
      }
      p.color = if (isSteel) 0xFF495159.toInt() else 0xFF2B1A0D.toInt()
      c.drawOval(RectF(footX - 2.9f, footY - 1.8f, footX + dir * 4.5f + 1.8f, footY + 2.2f), p)
      p.strokeCap = Paint.Cap.BUTT
    }

    fun drawArm(shX: Float, shY: Float, handX: Float, handY: Float, sleeveCol: Int, isGauntlet: Boolean) {
      val elbowX = (shX + handX) * 0.5f - dir * 1.7f
      val elbowY = (shY + handY) * 0.5f + 2.1f
      p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND
      p.strokeWidth = 3.5f; p.color = sleeveCol
      c.drawLine(shX, shY, elbowX, elbowY, p)
      p.strokeWidth = 3.0f; p.color = if (isGauntlet) 0xFF8C96A1.toInt() else 0xFFD4AC86.toInt()
      c.drawLine(elbowX, elbowY, handX, handY, p)
      p.style = Paint.Style.FILL
      p.color = if (isGauntlet) 0xFFC0C9D2.toInt() else 0xFFE3B992.toInt()
      c.drawCircle(handX, handY, 1.9f, p)
      p.strokeCap = Paint.Cap.BUTT
    }

    when (type) {
      UnitType.BATTERING_RAM -> {
        c.drawOval(cx - 37f, gy - 9f, cx + 41f, gy + 13f, shadow)
        for (wx in listOf(-21f, 21f)) {
          p.style = Paint.Style.FILL
          p.color = 0xFF2C3136.toInt(); c.drawCircle(cx + wx, gy - 4f, 10.0f, p)
          p.color = 0xFF754F2D.toInt(); c.drawCircle(cx + wx, gy - 4f, 7.6f, p)
          p.color = 0xFF2E1B0C.toInt(); c.drawCircle(cx + wx, gy - 4f, 5.4f, p)
          p.style = Paint.Style.STROKE; p.strokeWidth = 1.4f; p.color = 0xFF94693E.toInt()
          for (sp in 0..2) {
            val ang = sp * Math.PI.toFloat() / 3f
            c.drawLine(cx + wx + cos(ang) * 7f, gy - 4f + sin(ang) * 7f, cx + wx - cos(ang) * 7f, gy - 4f - sin(ang) * 7f, p)
          }
          p.style = Paint.Style.FILL; p.color = 0xFFB0B8C0.toInt()
          c.drawCircle(cx + wx, gy - 4f, 2.2f, p)
          c.drawCircle(cx + wx, gy - 4f, 10.0f, aoEdge)
        }
        val shed = Path().apply {
          moveTo(cx - 34f, gy - 8f); lineTo(cx - 26f, gy - 39f); lineTo(cx, gy - 51f); lineTo(cx + 26f, gy - 39f); lineTo(cx + 34f, gy - 8f); close()
        }
        p.shader = ProceduralTextures.scaledShader(ProceduralTextures.timberShader, 0.20f)
        c.drawPath(shed, p); p.shader = null
        val shedR = Path().apply { moveTo(cx, gy - 51f); lineTo(cx + 26f, gy - 39f); lineTo(cx + 34f, gy - 8f); lineTo(cx, gy - 8f); close() }
        p.color = 0x3C000000; c.drawPath(shedR, p)
        c.drawPath(shed, aoEdge)
        p.color = fMain; c.drawRect(cx - 26f, gy - 40f, cx + 26f, gy - 34f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2.0f; p.color = 0xFF2E3338.toInt()
        c.drawLine(cx - 19f, gy - 36f, cx - 23f, gy - 8f, p)
        c.drawLine(cx + 19f, gy - 36f, cx + 23f, gy - 8f, p)
        c.drawLine(cx, gy - 49f, cx, gy - 34f, p)
        p.shader = LinearGradient(cx, gy - 22f, cx, gy - 11f, 0xFFC5CED6.toInt(), 0xFF454C54.toInt(), Shader.TileMode.CLAMP)
        p.strokeWidth = 8.4f
        c.drawLine(cx - dir * 10f, gy - 16f, cx + dir * 42f, gy - 16f, p)
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = 0xFFB8C0C8.toInt(); c.drawCircle(cx + dir * 42f, gy - 16f, 6.4f, p); c.drawCircle(cx + dir * 42f, gy - 16f, 6.4f, aoEdge)
        p.color = 0xFFC7A14A.toInt(); c.drawCircle(cx + dir * 35f, gy - 16f, 2.5f, p)
        p.color = 0xFFEFF4F8.toInt(); c.drawCircle(cx + dir * 43f, gy - 19f, 1.3f, p)
        p.color = 0xFFE5B454.toInt(); c.drawCircle(cx + dir * 24f, gy - 30f, 1.9f, p)
      }

      UnitType.SCOUT_CAVALRY, UnitType.KNIGHT -> {
        c.drawOval(cx - 29f, gy - 7f, cx + 31f, gy + 12f, shadow)
        val horseHi = if (type == UnitType.KNIGHT) 0xFF5D4D3E.toInt() else 0xFF8C5D35.toInt()
        val horseLo = if (type == UnitType.KNIGHT) 0xFF241D16.toInt() else 0xFF452A14.toInt()
        val hoofSwing = stride * 4.4f

        val tail = Path().apply {
          moveTo(cx - dir * 21f, gy - 24f)
          quadTo(cx - dir * 34f, gy - 20f, cx - dir * 31f, gy - 7f)
          quadTo(cx - dir * 24f, gy - 14f, cx - dir * 19f, gy - 19f)
          close()
        }
        p.color = 0xFF1F140B.toInt(); c.drawPath(tail, p)

        fun drawHorseLeg(hx: Float, hy: Float, kneeOffX: Float, fx: Float, fy: Float, col: Int) {
          val kx = hx + kneeOffX; val ky = (hy + fy) * 0.5f
          p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND; p.strokeWidth = 3.7f; p.color = col
          c.drawLine(hx, hy, kx, ky, p)
          p.strokeWidth = 2.8f; c.drawLine(kx, ky, fx, fy, p)
          p.style = Paint.Style.FILL; p.color = 0xFF1C1510.toInt()
          c.drawCircle(fx, fy, 2.1f, p); p.strokeCap = Paint.Cap.BUTT
        }
        drawHorseLeg(cx - dir * 14f, gy - 14f, -dir * 3f, cx - dir * 18f - hoofSwing, gy + 1f, horseLo)
        drawHorseLeg(cx + dir * 14f, gy - 14f, dir * 3f, cx + dir * 13f + hoofSwing, gy + 1f, horseLo)

        val horseBody = Path().apply {
          moveTo(cx - dir * 23f, gy - 22f)
          cubicTo(cx - dir * 22f, gy - 33f, cx + dir * 18f, gy - 34f, cx + dir * 23f, gy - 21f)
          cubicTo(cx + dir * 25f, gy - 11f, cx + dir * 10f, gy - 10f, cx, gy - 12f)
          cubicTo(cx - dir * 12f, gy - 10f, cx - dir * 24f, gy - 12f, cx - dir * 23f, gy - 22f)
          close()
        }
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(cx, gy - 33f, cx, gy - 10f, horseHi, horseLo, Shader.TileMode.CLAMP)
        c.drawPath(horseBody, p); p.shader = null; c.drawPath(horseBody, aoEdge)
        // Muscular shoulder & croup coat sheen
        p.color = 0x26FFE8C8; c.drawOval(RectF(cx + dir * 7f - 6f, gy - 29f, cx + dir * 7f + 6f, gy - 19f), p)

        val horseHead = Path().apply {
          moveTo(cx + dir * 12f, gy - 26f)
          quadTo(cx + dir * 20f, gy - 40f, cx + dir * 24f, gy - 44f)
          lineTo(cx + dir * 27f, gy - 48f); lineTo(cx + dir * 28f, gy - 42f)
          lineTo(cx + dir * 35f, gy - 32f); lineTo(cx + dir * 29f, gy - 29f)
          lineTo(cx + dir * 21f, gy - 18f); close()
        }
        p.shader = LinearGradient(cx + dir * 14f, gy - 45f, cx + dir * 29f, gy - 24f, horseHi, horseLo, Shader.TileMode.CLAMP)
        c.drawPath(horseHead, p); p.shader = null; c.drawPath(horseHead, aoEdge)

        p.style = Paint.Style.STROKE; p.strokeWidth = 2.8f; p.color = 0xFF1A1109.toInt()
        c.drawLine(cx + dir * 13f, gy - 27f, cx + dir * 23f, gy - 43f, p)
        p.strokeWidth = 1.2f; p.color = 0xFF6B4323.toInt()
        c.drawLine(cx + dir * 28f, gy - 33f, cx + dir * 14f, gy - 31f, p)
        // Leather bridle cheek & nose straps
        p.strokeWidth = 0.9f; p.color = 0xFF3E2410.toInt()
        c.drawLine(cx + dir * 25f, gy - 41f, cx + dir * 30f, gy - 32f, p)
        c.drawLine(cx + dir * 28f, gy - 36f, cx + dir * 33.5f, gy - 33f, p)
        p.style = Paint.Style.FILL
        p.color = 0xFFC7A048.toInt(); c.drawCircle(cx + dir * 29f, gy - 32f, 1.1f, p)
        p.color = 0xFF1A140F.toInt(); c.drawCircle(cx + dir * 26.8f, gy - 39f, 1.0f, p)

        drawHorseLeg(cx - dir * 11f, gy - 14f, -dir * 2f, cx - dir * 10f + hoofSwing, gy + 2f, horseHi)
        drawHorseLeg(cx + dir * 16f, gy - 14f, dir * 3f, cx + dir * 19f - hoofSwing, gy + 2f, horseHi)

        if (type == UnitType.KNIGHT) {
          val barding = Path().apply {
            moveTo(cx - dir * 22f, gy - 26f); lineTo(cx + dir * 21f, gy - 26f)
            lineTo(cx + dir * 19f, gy - 11f); lineTo(cx - dir * 19f, gy - 11f); close()
          }
          p.shader = LinearGradient(cx, gy - 26f, cx, gy - 11f, fMain, fDark, Shader.TileMode.CLAMP)
          c.drawPath(barding, p); p.shader = null
          p.style = Paint.Style.STROKE; p.color = 0xFFCFA94E.toInt(); p.strokeWidth = 1.4f
          c.drawPath(barding, p)
          p.style = Paint.Style.FILL
          p.color = 0xFFC5CFD8.toInt()
          c.drawRect(cx + dir * 22f - 2f, gy - 42f, cx + dir * 28f + 2f, gy - 33f, p)
          p.color = 0xFFEFF4F8.toInt()
          c.drawCircle(cx + dir * 25f, gy - 40f, 1.2f, p)
        } else {
          p.color = fMain; c.drawRect(cx - 10f, gy - 31f, cx + 10f, gy - 24f, p)
          p.color = 0xFF422814.toInt(); c.drawRect(cx - 6f, gy - 32f, cx + 6f, gy - 28f, p)
        }

        drawArticulatedLeg(cx, gy - 28f, cx + dir * 4f, gy - 14f, isSteel = type == UnitType.KNIGHT, trouserCol = 0xFF422E20.toInt())
        // Stirrup leather strap & iron stirrup loop
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.0f; p.color = 0xFF4A2B12.toInt()
        c.drawLine(cx + dir * 2f, gy - 26f, cx + dir * 4f, gy - 14.5f, p)
        p.color = 0xFFB8C2CC.toInt()
        c.drawRect(cx + dir * 4f - 1.6f, gy - 15.5f, cx + dir * 4f + 1.6f, gy - 13f, p)
        p.style = Paint.Style.FILL

        val riderTorso = Path().apply {
          moveTo(cx - 8.5f, gy - 46f); lineTo(cx + 8.5f, gy - 46f)
          lineTo(cx + 6.5f, gy - 27f); lineTo(cx - 6.5f, gy - 27f); close()
        }
        p.shader = LinearGradient(cx - 8f, gy - 46f, cx + 8f, gy - 27f, if (type == UnitType.KNIGHT) 0xFFC9D2DB.toInt() else fLight, fDark, Shader.TileMode.CLAMP)
        c.drawPath(riderTorso, p); p.shader = null; c.drawPath(riderTorso, aoEdge)
        p.color = fMain; c.drawRect(cx - 6.5f, gy - 45f, cx + 6.5f, gy - 37f, p)

        if (type == UnitType.KNIGHT) {
          p.color = 0xFFD4DDE6.toInt()
          c.drawCircle(cx - dir * 8.0f, gy - 44f, 3.0f, p)
          c.drawCircle(cx + dir * 8.0f, gy - 44f, 3.0f, p)
          p.shader = RadialGradient(cx - 1.5f, gy - 54f, 8f, 0xFFE8EFF5.toInt(), 0xFF525B63.toInt(), Shader.TileMode.CLAMP)
          c.drawOval(RectF(cx - 5.8f, gy - 59f, cx + 5.8f, gy - 46f), p); p.shader = null
          c.drawOval(RectF(cx - 5.8f, gy - 59f, cx + 5.8f, gy - 46f), aoEdge)
          p.color = 0xFF181B1F.toInt(); c.drawLine(cx - 2.5f, gy - 52f, cx + dir * 5f, gy - 52f, p)
          val plume = Path().apply {
            moveTo(cx - dir * 1f, gy - 59f); quadTo(cx - dir * 12f, gy - 67f, cx - dir * 14f, gy - 54f)
            quadTo(cx - dir * 6f, gy - 57f, cx - dir * 1f, gy - 57f); close()
          }
          p.color = fLight; c.drawPath(plume, p); c.drawPath(plume, aoEdge)
          drawArm(cx + dir * 5f, gy - 42f, cx + dir * 11f, gy - 35f, 0xFFB8C2CC.toInt(), true)
          p.style = Paint.Style.STROKE; p.color = 0xFF6B4524.toInt(); p.strokeWidth = 2.6f
          c.drawLine(cx - dir * 2f, gy - 33f, cx + dir * 33f, gy - 59f, p)
          p.color = 0xFFD4DDE6.toInt(); p.strokeWidth = 2.0f
          c.drawLine(cx + dir * 29f, gy - 56f, cx + dir * 36f, gy - 62f, p)
          p.style = Paint.Style.FILL; p.color = fMain
          val pennant = Path().apply { moveTo(cx + dir * 29f, gy - 56f); lineTo(cx + dir * 16f, gy - 51f); lineTo(cx + dir * 24f, gy - 47f); close() }
          c.drawPath(pennant, p); c.drawPath(pennant, aoEdge)
        } else {
          drawHumanFace(cx, gy - 52f, 4.9f, bearded = true)
          p.color = 0xFF7D8690.toInt(); c.drawArc(RectF(cx - 6.2f, gy - 58.5f, cx + 6.2f, gy - 49f), 180f, 180f, true, p)
          c.drawArc(RectF(cx - 6.2f, gy - 58.5f, cx + 6.2f, gy - 49f), 180f, 180f, true, aoEdge)
          drawArm(cx + dir * 4f, gy - 42f, cx + dir * 12f, gy - 34f, fMain, false)
          p.style = Paint.Style.STROKE; p.color = 0xFFCFD8E0.toInt(); p.strokeWidth = 2.2f
          c.drawLine(cx + dir * 12f, gy - 34f, cx + dir * 24f, gy - 46f, p)
          p.style = Paint.Style.FILL
        }
      }

      UnitType.ARTILLERY -> {
        c.drawOval(cx - 35f, gy - 8f, cx + 38f, gy + 12f, shadow)
        // Towed split-trail stabilizer legs + strapped wooden ramrod
        p.style = Paint.Style.STROKE; p.strokeWidth = 4.2f; p.color = 0xFF3E4638.toInt()
        c.drawLine(cx - dir * 8f, gy - 12f, cx - dir * 32f, gy - 2f, p)
        c.drawLine(cx - dir * 8f, gy - 14f, cx - dir * 28f, gy - 9f, p)
        p.strokeWidth = 1.7f; p.color = 0xFF8C5B32.toInt()
        c.drawLine(cx - dir * 10f, gy - 15f, cx - dir * 29f, gy - 7.5f, p)
        p.style = Paint.Style.FILL; p.color = 0xFF2B3126.toInt()
        c.drawRect(cx - dir * 34f - 2f, gy - 4f, cx - dir * 34f + 2f, gy + 1f, p)
        c.drawCircle(cx - dir * 10f, gy - 15f, 1.5f, p)
        // Armored gun shield & hydraulic recoil cradle
        p.shader = LinearGradient(cx - 8f, gy - 34f, cx + 10f, gy - 12f, 0xFF6B775C.toInt(), 0xFF2B3325.toInt(), Shader.TileMode.CLAMP)
        val shield = Path().apply {
          moveTo(cx + dir * 2f, gy - 36f); lineTo(cx + dir * 13f, gy - 32f)
          lineTo(cx + dir * 11f, gy - 10f); lineTo(cx - dir * 2f, gy - 12f); close()
        }
        c.drawPath(shield, p); p.shader = null; c.drawPath(shield, aoEdge)
        // Heavy rifled howitzer cannon barrel + muzzle brake
        p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND; p.strokeWidth = 6.2f
        p.shader = LinearGradient(cx, gy - 28f, cx + dir * 42f, gy - 44f, 0xFF8C969E.toInt(), 0xFF2F363B.toInt(), Shader.TileMode.CLAMP)
        c.drawLine(cx - dir * 5f, gy - 23f, cx + dir * 40f, gy - 40f, p)
        p.shader = null
        p.strokeWidth = 8.2f; p.color = 0xFF424A52.toInt()
        c.drawLine(cx - dir * 6f, gy - 23f, cx + dir * 10f, gy - 29f, p)
        p.style = Paint.Style.FILL; p.color = 0xFF252A2E.toInt()
        c.drawCircle(cx + dir * 41f, gy - 40.5f, 3.8f, p)
        // Heavy spoked rubber-rimmed artillery wheel with radial spokes & hub rivets
        val wx = cx + dir * 2f; val wy = gy - 10f
        p.color = 0xFF1E2226.toInt(); c.drawCircle(wx, wy, 12.2f, p)
        p.color = 0xFF56614A.toInt(); c.drawCircle(wx, wy, 8.6f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.5f; p.color = 0xFF283022.toInt()
        for (sp in 0..2) {
          val ang = sp * Math.PI.toFloat() / 3f
          c.drawLine(wx + cos(ang) * 7.8f, wy + sin(ang) * 7.8f, wx - cos(ang) * 7.8f, wy - sin(ang) * 7.8f, p)
        }
        p.style = Paint.Style.FILL
        p.color = 0xFF2C3325.toInt(); c.drawCircle(wx, wy, 4.4f, p)
        p.color = 0xFFC5CFD8.toInt(); c.drawCircle(wx, wy, 2.2f, p)
        c.drawCircle(wx, wy, 12.2f, aoEdge)
        // Brass shell & gunner beside breech
        p.color = 0xFFE0B446.toInt()
        c.drawRect(cx - dir * 20f, gy - 11f, cx - dir * 14f, gy - 3f, p)
        drawArticulatedLeg(cx - dir * 16f, gy - 16f, cx - dir * 18f, gy, false, 0xFF3E4A36.toInt())
        p.color = fMain; c.drawRect(cx - dir * 21f, gy - 32f, cx - dir * 11f, gy - 17f, p)
        drawHumanFace(cx - dir * 16f, gy - 38f, 3.8f, bearded = false)
        p.color = 0xFF4A5442.toInt(); c.drawArc(RectF(cx - dir * 16f - 5f, gy - 44f, cx - dir * 16f + 5f, gy - 36f), 180f, 180f, true, p)
      }

      UnitType.TANK, UnitType.HEAVY_TANK -> {
        val isHeavy = type == UnitType.HEAVY_TANK
        c.drawOval(cx - 42f, gy - 8f, cx + 44f, gy + 14f, shadow)
        // Continuous caterpillar track assembly, cleat teeth + road wheels
        val trackTop = gy - 17f
        val trackBot = gy + 3f
        p.style = Paint.Style.FILL
        p.color = 0xFF1D2024.toInt()
        c.drawRoundRect(RectF(cx - 35f, trackTop, cx + 35f, trackBot), 8f, 8f, p)
        p.color = 0xFF42484F.toInt()
        c.drawRoundRect(RectF(cx - 33f, trackTop + 2f, cx + 33f, trackBot - 2f), 6f, 6f, p)
        p.color = 0xFF28241E.toInt()
        for (ti in -5..5) {
          c.drawRect(cx + ti * 5.8f - 1.2f, trackBot - 2.2f, cx + ti * 5.8f + 1.2f, trackBot + 0.5f, p)
        }
        for (wi in -2..2) {
          val wx = cx + wi * 12.5f
          p.color = 0xFF262B30.toInt(); c.drawCircle(wx, (trackTop + trackBot) * 0.5f, 5.2f, p)
          p.color = if (isHeavy) 0xFF5D6652.toInt() else 0xFF6B785A.toInt()
          c.drawCircle(wx, (trackTop + trackBot) * 0.5f, 3.2f, p)
          p.color = 0xFFB8C2CC.toInt(); c.drawCircle(wx, (trackTop + trackBot) * 0.5f, 1.1f, p)
        }
        c.drawRoundRect(RectF(cx - 35f, trackTop, cx + 35f, trackBot), 8f, 8f, aoEdge)

        // Armored steel track skirts & sloped composite hull
        val hullHi = if (isHeavy) 0xFF5A6352.toInt() else 0xFF70805E.toInt()
        val hullLo = if (isHeavy) 0xFF262B22.toInt() else 0xFF333B2A.toInt()
        val hull = Path().apply {
          moveTo(cx - dir * 36f, gy - 12f)
          lineTo(cx - dir * 28f, gy - 28f)
          lineTo(cx + dir * 28f, gy - 27f)
          lineTo(cx + dir * 37f, gy - 13f)
          lineTo(cx + dir * 32f, gy - 9f)
          lineTo(cx - dir * 32f, gy - 9f)
          close()
        }
        p.shader = LinearGradient(cx, gy - 28f, cx, gy - 9f, hullHi, hullLo, Shader.TileMode.CLAMP)
        c.drawPath(hull, p); p.shader = null; c.drawPath(hull, aoEdge)

        // Faction insignia armor stripe, panel weld highlight & ERA reactive bricks
        p.color = fMain
        c.drawRect(cx - dir * 20f - 6f, gy - 23f, cx - dir * 20f + 6f, gy - 14f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.1f; p.color = 0x62E6ECD6.toInt()
        c.drawLine(cx - dir * 27f, gy - 27f, cx + dir * 27f, gy - 26f, p)
        p.style = Paint.Style.FILL
        if (isHeavy) {
          for (b in 0..3) {
            val bx = cx - 14f + b * 8.5f
            p.color = 0xFF475040.toInt(); c.drawRect(bx, gy - 19f, bx + 6.5f, gy - 12f, p)
            c.drawRect(bx, gy - 19f, bx + 6.5f, gy - 12f, aoEdge)
          }
        }

        // Low-profile sloped turret + smoke grenade launchers + commander cupola
        val turret = Path().apply {
          moveTo(cx - dir * 20f, gy - 26f)
          lineTo(cx - dir * 13f, gy - 42f)
          lineTo(cx + dir * 14f, gy - 41f)
          lineTo(cx + dir * 22f, gy - 27f)
          close()
        }
        p.shader = LinearGradient(cx, gy - 42f, cx, gy - 26f, 0xFF889674.toInt(), hullLo, Shader.TileMode.CLAMP)
        c.drawPath(turret, p); p.shader = null; c.drawPath(turret, aoEdge)
        // Turret specular top bevel & optical rangefinder lens glint
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.6f; p.color = 0x8CE8F0DA.toInt()
        c.drawLine(cx - dir * 12f, gy - 41f, cx + dir * 13f, gy - 40f, p)
        p.style = Paint.Style.FILL; p.color = 0xFF7BE8FF.toInt()
        c.drawCircle(cx + dir * 10f, gy - 36f, 1.5f, p)
        // Smoke discharger pods
        p.color = 0xFF2B3136.toInt()
        c.drawCircle(cx + dir * 2f, gy - 33f, 1.8f, p)
        c.drawCircle(cx - dir * 2f, gy - 32f, 1.8f, p)

        // Long tank cannon with bore evacuator & steel specular highlight
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = if (isHeavy) 5.8f else 4.6f
        p.shader = LinearGradient(cx + dir * 16f, gy - 36f, cx + dir * 48f, gy - 32f, 0xFFB0BABF.toInt(), 0xFF3E454A.toInt(), Shader.TileMode.CLAMP)
        c.drawLine(cx + dir * 14f, gy - 35f, cx + dir * (if (isHeavy) 49f else 43f), gy - 37f, p)
        p.shader = null
        p.strokeWidth = 1.0f; p.color = 0x7CF4F8FA
        c.drawLine(cx + dir * 15f, gy - 36.6f, cx + dir * (if (isHeavy) 47f else 41f), gy - 38.4f, p)
        p.strokeWidth = if (isHeavy) 7.4f else 6.0f; p.color = 0xFF32393F.toInt()
        c.drawLine(cx + dir * 27f, gy - 36f, cx + dir * 34f, gy - 36.4f, p)
        p.style = Paint.Style.FILL; p.color = 0xFF1F2327.toInt()
        c.drawCircle(cx + dir * (if (isHeavy) 49f else 43f), gy - 37f, if (isHeavy) 3.4f else 2.8f, p)

        // Hatch cupola, coaxial MG & whip radio antenna
        p.color = fDark; c.drawOval(RectF(cx - dir * 8f - 6f, gy - 46f, cx - dir * 8f + 6f, gy - 40f), p)
        p.color = 0xFFC5CFD8.toInt(); c.drawOval(RectF(cx - dir * 8f - 6f, gy - 46f, cx - dir * 8f + 6f, gy - 40f), aoEdge)
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.3f; p.color = 0xFFD0D8E0.toInt()
        c.drawLine(cx - dir * 17f, gy - 40f, cx - dir * 23f, gy - 63f, p)
        p.style = Paint.Style.FILL; p.color = fLight
        c.drawCircle(cx - dir * 23f, gy - 63f, 2.0f, p)
      }

      UnitType.ROCKET_LAUNCHER -> {
        c.drawOval(cx - 42f, gy - 8f, cx + 44f, gy + 14f, shadow)
        val trackTop = gy - 16f
        val trackBot = gy + 3f
        p.style = Paint.Style.FILL
        p.color = 0xFF1D2024.toInt()
        c.drawRoundRect(RectF(cx - 34f, trackTop, cx + 34f, trackBot), 7f, 7f, p)
        p.color = 0xFF3E454C.toInt()
        c.drawRoundRect(RectF(cx - 32f, trackTop + 2f, cx + 32f, trackBot - 2f), 5f, 5f, p)
        for (wi in -2..2) {
          val wx = cx + wi * 12f
          p.color = 0xFF23282D.toInt(); c.drawCircle(wx, (trackTop + trackBot) * 0.5f, 4.8f, p)
          p.color = 0xFF6B775C.toInt(); c.drawCircle(wx, (trackTop + trackBot) * 0.5f, 2.8f, p)
        }
        c.drawRoundRect(RectF(cx - 34f, trackTop, cx + 34f, trackBot), 7f, 7f, aoEdge)

        // Armored carrier cab + chassis
        val hull = Path().apply {
          moveTo(cx - dir * 35f, gy - 12f)
          lineTo(cx - dir * 30f, gy - 25f)
          lineTo(cx + dir * 26f, gy - 26f)
          lineTo(cx + dir * 34f, gy - 13f)
          close()
        }
        p.shader = LinearGradient(cx, gy - 26f, cx, gy - 12f, 0xFF6F7E5D.toInt(), 0xFF2D3525.toInt(), Shader.TileMode.CLAMP)
        c.drawPath(hull, p); p.shader = null; c.drawPath(hull, aoEdge)
        // Armored cab windshield + faction stripe
        p.color = 0xFF83D0F2.toInt()
        c.drawRect(cx + dir * 22f - 4f, gy - 24f, cx + dir * 22f + 4f, gy - 18f, p)
        p.color = fMain; c.drawRect(cx + dir * 10f - 5f, gy - 22f, cx + dir * 10f + 5f, gy - 15f, p)

        // Hydraulic lift arm & elevated 12-tube MLRS rocket pod
        p.style = Paint.Style.STROKE; p.strokeWidth = 4.2f; p.color = 0xFF3E454C.toInt()
        c.drawLine(cx - dir * 12f, gy - 24f, cx - dir * 4f, gy - 37f, p)
        p.style = Paint.Style.FILL
        val pod = Path().apply {
          moveTo(cx - dir * 26f, gy - 28f)
          lineTo(cx + dir * 16f, gy - 50f)
          lineTo(cx + dir * 25f, gy - 37f)
          lineTo(cx - dir * 17f, gy - 16f)
          close()
        }
        p.shader = LinearGradient(cx, gy - 50f, cx, gy - 16f, 0xFF7D8B68.toInt(), 0xFF343D2C.toInt(), Shader.TileMode.CLAMP)
        c.drawPath(pod, p); p.shader = null; c.drawPath(pod, aoEdge)
        // Visible red missile warhead noses in pod face
        for (row in 0..1) {
          for (col in 0..2) {
            val mx = cx + dir * (13f + col * 3.0f - row * 1.8f)
            val my = gy - 44.5f + col * 2.8f + row * 4.5f
            p.color = 0xFF1F2327.toInt(); c.drawCircle(mx, my, 2.3f, p)
            p.color = 0xFFE64B3C.toInt(); c.drawCircle(mx + dir * 0.5f, my - 0.3f, 1.3f, p)
          }
        }
        p.color = fLight; c.drawCircle(cx - dir * 18f, gy - 25f, 2.6f, p)
      }

      UnitType.FIGHTER_PLANE -> {
        val cyAir = lh * 0.50f
        c.drawOval(cx - 22f, gy - 4f, cx + 22f, gy + 5f, shadow)
        // Twin swept main delta wings & vertical tail stabilizers
        p.style = Paint.Style.FILL
        val wingFar = Path().apply {
          moveTo(cx + dir * 4f, cyAir - 4f)
          lineTo(cx - dir * 20f, cyAir - 27f)
          lineTo(cx - dir * 28f, cyAir - 25f)
          lineTo(cx - dir * 16f, cyAir - 3f)
          close()
        }
        p.shader = LinearGradient(cx, cyAir - 27f, cx, cyAir, 0xFF9EAAB5.toInt(), 0xFF475059.toInt(), Shader.TileMode.CLAMP)
        c.drawPath(wingFar, p); p.shader = null; c.drawPath(wingFar, aoEdge)

        val tailFin = Path().apply {
          moveTo(cx - dir * 18f, cyAir - 4f)
          lineTo(cx - dir * 32f, cyAir - 22f)
          lineTo(cx - dir * 37f, cyAir - 20f)
          lineTo(cx - dir * 31f, cyAir - 3f)
          close()
        }
        p.color = fMain; c.drawPath(tailFin, p); c.drawPath(tailFin, aoEdge)

        // Supersonic fuselage body + dorsal spine highlight + panel seams
        val fuselage = Path().apply {
          moveTo(cx + dir * 42f, cyAir - 1f)
          cubicTo(cx + dir * 22f, cyAir - 9f, cx - dir * 14f, cyAir - 8f, cx - dir * 36f, cyAir - 4f)
          lineTo(cx - dir * 36f, cyAir + 4f)
          cubicTo(cx - dir * 12f, cyAir + 7f, cx + dir * 22f, cyAir + 6f, cx + dir * 42f, cyAir - 1f)
          close()
        }
        p.shader = LinearGradient(cx, cyAir - 9f, cx, cyAir + 7f, 0xFFE3ECF2.toInt(), 0xFF4E5761.toInt(), Shader.TileMode.CLAMP)
        c.drawPath(fuselage, p); p.shader = null; c.drawPath(fuselage, aoEdge)
        p.style = Paint.Style.STROKE; p.strokeWidth = 0.85f; p.color = 0x55FFFFFF
        c.drawLine(cx + dir * 34f, cyAir - 3.5f, cx - dir * 28f, cyAir - 5f, p)
        p.style = Paint.Style.FILL

        // Tinted glass bubble cockpit canopy
        p.shader = LinearGradient(cx + dir * 6f, cyAir - 10f, cx + dir * 22f, cyAir - 2f, 0xFF98E2FF.toInt(), 0xFF1D4D6B.toInt(), Shader.TileMode.CLAMP)
        c.drawOval(RectF(cx + dir * 14f - 10f, cyAir - 9.5f, cx + dir * 14f + 10f, cyAir - 1.5f), p)
        p.shader = null; p.color = 0xCCFFFFFF.toInt(); c.drawCircle(cx + dir * 16f, cyAir - 7f, 1.5f, p)

        // Near swept delta wing + underwing missile pylons + faction stripe
        val wingNear = Path().apply {
          moveTo(cx + dir * 8f, cyAir + 2f)
          lineTo(cx - dir * 16f, cyAir + 26f)
          lineTo(cx - dir * 25f, cyAir + 24f)
          lineTo(cx - dir * 12f, cyAir + 3f)
          close()
        }
        p.shader = LinearGradient(cx, cyAir, cx, cyAir + 26f, 0xFFC5CFD8.toInt(), 0xFF5D6770.toInt(), Shader.TileMode.CLAMP)
        c.drawPath(wingNear, p); p.shader = null; c.drawPath(wingNear, aoEdge)
        p.color = fMain; c.drawCircle(cx - dir * 12f, cyAir + 14f, 3.2f, p)
        p.color = 0xFFE5EDF5.toInt(); c.drawCircle(cx - dir * 12f, cyAir + 14f, 1.4f, p)
        // Wingtip missile & green/red nav strobe
        p.style = Paint.Style.STROKE; p.strokeWidth = 2.0f; p.color = 0xFFECEFF2.toInt()
        c.drawLine(cx - dir * 25f, cyAir + 24f, cx - dir * 13f, cyAir + 26f, p)
        p.style = Paint.Style.FILL; p.color = 0xFF5CE67A.toInt()
        c.drawCircle(cx - dir * 23f, cyAir + 24f, 1.3f, p)
        p.color = 0xFFE65345.toInt()
        c.drawCircle(cx - dir * 24f, cyAir - 25f, 1.3f, p)
        // Nozzle cone
        p.color = 0xFF282D33.toInt()
        c.drawRect(cx - dir * 38f - 2f, cyAir - 3.5f, cx - dir * 38f + 2f, cyAir + 3.5f, p)
      }

      UnitType.BOMBER_PLANE -> {
        val cyAir = lh * 0.50f
        c.drawOval(cx - 28f, gy - 4f, cx + 28f, gy + 6f, shadow)
        // Wide heavy strategic swept wings & dual engine nacelles
        p.style = Paint.Style.FILL
        val farWing = Path().apply {
          moveTo(cx + dir * 10f, cyAir - 5f)
          lineTo(cx - dir * 18f, cyAir - 35f)
          lineTo(cx - dir * 28f, cyAir - 33f)
          lineTo(cx - dir * 12f, cyAir - 4f)
          close()
        }
        p.shader = LinearGradient(cx, cyAir - 35f, cx, cyAir, 0xFF6B737B.toInt(), 0xFF2B3136.toInt(), Shader.TileMode.CLAMP)
        c.drawPath(farWing, p); p.shader = null; c.drawPath(farWing, aoEdge)

        // Tail fin
        val tail = Path().apply {
          moveTo(cx - dir * 22f, cyAir - 4f)
          lineTo(cx - dir * 38f, cyAir - 24f)
          lineTo(cx - dir * 44f, cyAir - 22f)
          lineTo(cx - dir * 36f, cyAir - 2f)
          close()
        }
        p.color = 0xFF3E454C.toInt(); c.drawPath(tail, p); c.drawPath(tail, aoEdge)
        p.color = fMain; c.drawRect(cx - dir * 39f - 3f, cyAir - 17f, cx - dir * 39f + 3f, cyAir - 11f, p)

        // Broad heavy stealth-angled fuselage
        val hull = Path().apply {
          moveTo(cx + dir * 44f, cyAir + 1f)
          cubicTo(cx + dir * 26f, cyAir - 11f, cx - dir * 18f, cyAir - 11f, cx - dir * 40f, cyAir - 5f)
          lineTo(cx - dir * 40f, cyAir + 6f)
          cubicTo(cx - dir * 16f, cyAir + 10f, cx + dir * 24f, cyAir + 9f, cx + dir * 44f, cyAir + 1f)
          close()
        }
        p.shader = LinearGradient(cx, cyAir - 11f, cx, cyAir + 10f, 0xFF7B858E.toInt(), 0xFF2B3036.toInt(), Shader.TileMode.CLAMP)
        c.drawPath(hull, p); p.shader = null; c.drawPath(hull, aoEdge)
        // Subtle fuselage panel seam ribs
        p.style = Paint.Style.STROKE; p.strokeWidth = 0.8f; p.color = 0x4514181C
        c.drawLine(cx + dir * 8f, cyAir - 9f, cx + dir * 8f, cyAir + 7f, p)
        c.drawLine(cx - dir * 12f, cyAir - 9f, cx - dir * 12f, cyAir + 7f, p)
        p.style = Paint.Style.FILL

        // Armored cockpit windshields + divider bars
        p.color = 0xFF75BFE0.toInt()
        c.drawOval(RectF(cx + dir * 24f - 6f, cyAir - 7f, cx + dir * 24f + 6f, cyAir - 1f), p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.0f; p.color = 0xFF1F2429.toInt()
        c.drawLine(cx + dir * 24f, cyAir - 7f, cx + dir * 24f, cyAir - 1f, p)
        p.style = Paint.Style.FILL

        // Near heavy wing + twin turbine pods + ordnance & wingtip nav strobes
        val nearWing = Path().apply {
          moveTo(cx + dir * 12f, cyAir + 3f)
          lineTo(cx - dir * 16f, cyAir + 35f)
          lineTo(cx - dir * 27f, cyAir + 33f)
          lineTo(cx - dir * 10f, cyAir + 5f)
          close()
        }
        p.shader = LinearGradient(cx, cyAir, cx, cyAir + 35f, 0xFF6B747D.toInt(), 0xFF262B30.toInt(), Shader.TileMode.CLAMP)
        c.drawPath(nearWing, p); p.shader = null; c.drawPath(nearWing, aoEdge)
        // Engine nacelles with front intake rim & spinner glint
        for ((exOff, eyOff) in listOf(-dir * 4f to 14f, -dir * 12f to 24f)) {
          p.color = 0xFF1E2226.toInt()
          c.drawRoundRect(RectF(cx + exOff - 7f, cyAir + eyOff - 3f, cx + exOff + 7f, cyAir + eyOff + 3f), 2f, 2f, p)
          p.color = 0xFFC5CFD8.toInt(); c.drawCircle(cx + exOff + dir * 6f, cyAir + eyOff, 1.8f, p)
          p.color = fMain; c.drawRect(cx + exOff - 2f, cyAir + eyOff - 3f, cx + exOff + 2f, cyAir + eyOff + 3f, p)
        }
        p.color = fMain; c.drawCircle(cx - dir * 14f, cyAir + 20f, 3.6f, p)
        p.color = 0xFFE5EDF5.toInt(); c.drawCircle(cx - dir * 14f, cyAir + 20f, 1.6f, p)
        p.color = 0xFF5CE67A.toInt(); c.drawCircle(cx - dir * 24f, cyAir + 33f, 1.4f, p)
        p.color = 0xFFE65345.toInt(); c.drawCircle(cx - dir * 24f, cyAir - 33f, 1.4f, p)
      }

      UnitType.GUNSHIP -> {
        val cyAir = lh * 0.52f
        c.drawOval(cx - 24f, gy - 4f, cx + 24f, gy + 5f, shadow)
        // Landing skids under helicopter belly
        p.style = Paint.Style.STROKE; p.strokeWidth = 2.0f; p.color = 0xFF282E33.toInt()
        c.drawLine(cx - dir * 16f, cyAir + 13f, cx + dir * 20f, cyAir + 13f, p)
        c.drawLine(cx - dir * 8f, cyAir + 6f, cx - dir * 10f, cyAir + 13f, p)
        c.drawLine(cx + dir * 10f, cyAir + 6f, cx + dir * 12f, cyAir + 13f, p)

        // Tail boom + swept vertical stabilizer + tail rotor
        p.style = Paint.Style.FILL
        val boom = Path().apply {
          moveTo(cx - dir * 10f, cyAir - 4f)
          lineTo(cx - dir * 40f, cyAir - 7f)
          lineTo(cx - dir * 42f, cyAir - 2f)
          lineTo(cx - dir * 10f, cyAir + 3f)
          close()
        }
        p.shader = LinearGradient(cx, cyAir - 7f, cx, cyAir + 3f, 0xFF5D6A52.toInt(), 0xFF262D20.toInt(), Shader.TileMode.CLAMP)
        c.drawPath(boom, p); p.shader = null; c.drawPath(boom, aoEdge)
        val tailFin = Path().apply {
          moveTo(cx - dir * 35f, cyAir - 6f)
          lineTo(cx - dir * 43f, cyAir - 22f)
          lineTo(cx - dir * 47f, cyAir - 20f)
          lineTo(cx - dir * 41f, cyAir - 3f)
          close()
        }
        p.color = fMain; c.drawPath(tailFin, p); c.drawPath(tailFin, aoEdge)
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.8f; p.color = 0xAACFD8E0.toInt()
        c.drawCircle(cx - dir * 44f, cyAir - 16f, 6.5f, p)

        // Armored tandem fuselage + turbine cowling
        p.style = Paint.Style.FILL
        val body = Path().apply {
          moveTo(cx + dir * 32f, cyAir + 2f)
          lineTo(cx + dir * 18f, cyAir - 9f)
          lineTo(cx - dir * 16f, cyAir - 10f)
          lineTo(cx - dir * 20f, cyAir + 4f)
          lineTo(cx + dir * 16f, cyAir + 8f)
          close()
        }
        p.shader = LinearGradient(cx, cyAir - 10f, cx, cyAir + 8f, 0xFF6B7B5C.toInt(), 0xFF252B1E.toInt(), Shader.TileMode.CLAMP)
        c.drawPath(body, p); p.shader = null; c.drawPath(body, aoEdge)
        // Tandem pilot & gunner glass canopy
        p.shader = LinearGradient(cx + dir * 2f, cyAir - 10f, cx + dir * 26f, cyAir, 0xFF9DE6FF.toInt(), 0xFF1A4761.toInt(), Shader.TileMode.CLAMP)
        c.drawOval(RectF(cx + dir * 14f - 12f, cyAir - 8f, cx + dir * 14f + 12f, cyAir + 1f), p)
        p.shader = null; c.drawOval(RectF(cx + dir * 14f - 12f, cyAir - 8f, cx + dir * 14f + 12f, cyAir + 1f), aoEdge)

        // Stub weapon wing + 3-tube rocket pod + chin FLIR optic turret + nose autocannon + exhaust cowl
        p.color = 0xFF282E24.toInt(); c.drawOval(RectF(cx - dir * 12f - 5f, cyAir - 12f, cx - dir * 12f + 5f, cyAir - 7f), p)
        p.color = 0xFF343B2E.toInt()
        c.drawRect(cx - dir * 4f - 7f, cyAir - 2f, cx - dir * 4f + 7f, cyAir + 6f, p)
        p.color = fMain; c.drawRect(cx - dir * 4f - 4f, cyAir + 2f, cx - dir * 4f + 4f, cyAir + 7f, p)
        for (ryOff in listOf(3.2f, 5.2f, 7.0f)) {
          p.color = 0xFF1A1D20.toInt(); c.drawCircle(cx - dir * 4f + dir * 4.2f, cyAir + ryOff, 1.4f, p)
          p.color = 0xFFE04838.toInt(); c.drawCircle(cx - dir * 4f + dir * 4.5f, cyAir + ryOff, 0.75f, p)
        }
        // Nose chin FLIR targeting ball + lens glint + 30mm autocannon
        p.color = 0xFF282D33.toInt(); c.drawCircle(cx + dir * 28f, cyAir + 3.5f, 3.1f, p)
        p.color = 0xFF7BE8FF.toInt(); c.drawCircle(cx + dir * 29.2f, cyAir + 3.2f, 1.2f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2.0f; p.color = 0xFF1C2024.toInt()
        c.drawLine(cx + dir * 24f, cyAir + 6f, cx + dir * 36f, cyAir + 10f, p)
        // Rotor mast hub & pitch control links
        p.style = Paint.Style.FILL; p.color = 0xFF2C3238.toInt()
        c.drawRect(cx - 2.5f, cyAir - 18f, cx + 2.5f, cyAir - 9f, p)
        p.color = 0xFF8C969E.toInt(); c.drawRect(cx - 5f, cyAir - 19f, cx + 5f, cyAir - 16.5f, p)
      }

      else -> {
        c.drawOval(cx - 17f, gy - 5f, cx + 19f, gy + 8f, shadow)
        val isArmoredBody =
          type == UnitType.MAN_AT_ARMS ||
            type == UnitType.PIKEMAN ||
            type == UnitType.MARINE ||
            type == UnitType.ROCKET_SOLDIER
        val trouserCol =
          when (type) {
            UnitType.VILLAGER -> 0xFF4F3B27.toInt()
            UnitType.ARCHER -> 0xFF283B1F.toInt()
            UnitType.RIFLEMAN -> 0xFF524B36.toInt()
            UnitType.MACHINE_GUNNER, UnitType.ROCKET_SOLDIER -> 0xFF343D2D.toInt()
            UnitType.SNIPER -> 0xFF253320.toInt()
            UnitType.MARINE -> 0xFF2E3829.toInt()
            else -> 0xFF362E27.toInt()
          }

        // Grounded realistic anatomical legs & stride
        val legSwing = stride * 4.4f
        drawArticulatedLeg(cx - dir * 3.1f, gy - 17f, cx - dir * 4.2f - legSwing, gy + 1f, isArmoredBody, trouserCol)
        drawArticulatedLeg(cx + dir * 3.1f, gy - 17f, cx + dir * 4.2f + legSwing, gy + 1f, isArmoredBody, trouserCol)

        if (type == UnitType.ARCHER || type == UnitType.CROSSBOWMAN || type == UnitType.MAN_AT_ARMS || type == UnitType.SNIPER) {
          val cloakCol = if (type == UnitType.SNIPER) 0xFF2C3F23.toInt() else fDark
          val cloak = Path().apply {
            moveTo(cx - dir * 4f, gy - 39f); lineTo(cx - dir * 15f, gy - 9f)
            lineTo(cx - dir * 6f, gy - 8f); lineTo(cx + dir * 2f, gy - 37f); close()
          }
          p.color = cloakCol; c.drawPath(cloak, p); c.drawPath(cloak, aoEdge)
        }

        val topCol = when (type) {
          UnitType.MAN_AT_ARMS, UnitType.PIKEMAN -> 0xFFC5CFD8.toInt()
          UnitType.VILLAGER -> 0xFFB08D62.toInt()
          UnitType.ARCHER -> 0xFF3E6134.toInt()
          UnitType.RIFLEMAN -> 0xFF7D704C.toInt()
          UnitType.MACHINE_GUNNER -> 0xFF5A634A.toInt()
          UnitType.SNIPER -> 0xFF374C2A.toInt()
          UnitType.ROCKET_SOLDIER -> 0xFF4F5C44.toInt()
          UnitType.MARINE -> 0xFF44543D.toInt()
          else -> fLight
        }
        val botCol = when (type) {
          UnitType.MAN_AT_ARMS, UnitType.PIKEMAN -> 0xFF515962.toInt()
          UnitType.VILLAGER -> 0xFF5D442B.toInt()
          UnitType.ARCHER -> 0xFF1F3619.toInt()
          UnitType.RIFLEMAN -> 0xFF423B26.toInt()
          UnitType.MACHINE_GUNNER -> 0xFF2B3123.toInt()
          UnitType.SNIPER -> 0xFF1A2815.toInt()
          UnitType.ROCKET_SOLDIER -> 0xFF262D20.toInt()
          UnitType.MARINE -> 0xFF212B1E.toInt()
          else -> fDark
        }
        // Tapered natural shoulders, ribcage, waist & tunic skirt
        val torso = Path().apply {
          moveTo(cx - 9.2f, gy - 39f); lineTo(cx + 9.2f, gy - 39f)
          lineTo(cx + 6.4f, gy - 24f); lineTo(cx + 8.4f, gy - 13f)
          lineTo(cx - 8.4f, gy - 13f); lineTo(cx - 6.4f, gy - 24f); close()
        }
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(cx - 9f, gy - 39f, cx + 9f, gy - 13f, topCol, botCol, Shader.TileMode.CLAMP)
        c.drawPath(torso, p); p.shader = null; c.drawPath(torso, aoEdge)

        p.color = 0x30000000
        c.drawLine(cx - 3f, gy - 23f, cx - 3.5f, gy - 13f, p)
        c.drawLine(cx + 3f, gy - 23f, cx + 3.5f, gy - 13f, p)
        p.color = fMain; c.drawRect(cx - 8.5f, gy - 39f, cx + 8.5f, gy - 32f, p)
        p.color = 0xFF362110.toInt(); c.drawRect(cx - 6.8f, gy - 25.5f, cx + 6.8f, gy - 22.8f, p)
        p.color = 0xFFCFA74C.toInt(); c.drawRect(cx - 1.8f, gy - 26f, cx + 1.8f, gy - 22.5f, p)

        // Realistic anatomical head-to-body ratio (~1:7 instead of 1:4 chibi)
        drawHumanFace(cx, gy - 46.5f, 4.6f, bearded = type != UnitType.ARCHER)

        when (type) {
          UnitType.VILLAGER -> {
            p.color = 0xFFD4C2A1.toInt(); c.drawRect(cx - 5.2f, gy - 22.5f, cx + 5.2f, gy - 13.5f, p)
            p.style = Paint.Style.STROKE; p.color = 0xFF4D311A.toInt(); p.strokeWidth = 1.8f
            c.drawLine(cx - dir * 6f, gy - 37f, cx + dir * 5.5f, gy - 24f, p)
            p.style = Paint.Style.FILL; p.color = 0xFF6B4524.toInt()
            c.drawRect(cx + dir * 3.5f - 2.2f, gy - 24.5f, cx + dir * 3.5f + 2.2f, gy - 18f, p)
            p.color = 0xFFA3824A.toInt(); c.drawOval(RectF(cx - 10.2f, gy - 50.5f, cx + 10.2f, gy - 45f), p)
            p.color = 0xFFC29E5E.toInt(); c.drawArc(RectF(cx - 5.5f, gy - 55f, cx + 5.5f, gy - 46f), 180f, 180f, true, p)
            c.drawOval(RectF(cx - 10.2f, gy - 50.5f, cx + 10.2f, gy - 45f), aoEdge)
            p.color = 0xFF3E2612.toInt(); c.drawRect(cx - 5.2f, gy - 49.5f, cx + 5.2f, gy - 48f, p)
            drawArm(cx - dir * 6.5f, gy - 36f, cx + dir * 6.5f, gy - 27f, 0xFF8A6943.toInt(), false)
            drawArm(cx + dir * 6.5f, gy - 36f, cx + dir * 12f, gy - 31f, 0xFFA37E54.toInt(), false)
            p.style = Paint.Style.STROKE; p.color = 0xFF5A391E.toInt(); p.strokeWidth = 2.4f
            c.drawLine(cx + dir * 4f, gy - 23f, cx + dir * 18f, gy - 39f, p)
            p.style = Paint.Style.FILL; p.color = 0xFFB8C2CC.toInt()
            val axeHead = Path().apply { moveTo(cx + dir * 16f, gy - 37f); lineTo(cx + dir * 22.5f, gy - 42f); lineTo(cx + dir * 21.5f, gy - 33f); close() }
            c.drawPath(axeHead, p); c.drawPath(axeHead, aoEdge)
            p.style = Paint.Style.STROKE; p.color = 0xFFEFF4F8.toInt(); p.strokeWidth = 0.85f
            c.drawLine(cx + dir * 21.5f, gy - 41f, cx + dir * 21f, gy - 34f, p)
          }

          UnitType.MILITIA, UnitType.MAN_AT_ARMS -> {
            p.shader = LinearGradient(cx - 5.5f, gy - 53.5f, cx + 5.5f, gy - 44.5f, 0xFFE5EDF4.toInt(), 0xFF576069.toInt(), Shader.TileMode.CLAMP)
            c.drawArc(RectF(cx - 5.8f, gy - 53.5f, cx + 5.8f, gy - 43.5f), 180f, 180f, true, p); p.shader = null
            c.drawArc(RectF(cx - 5.8f, gy - 53.5f, cx + 5.8f, gy - 43.5f), 180f, 180f, true, aoEdge)
            p.style = Paint.Style.STROKE; p.strokeWidth = 0.85f; p.color = 0x99FFFFFF.toInt()
            c.drawArc(RectF(cx - 4.4f, gy - 52.3f, cx + 2.2f, gy - 46f), 195f, 95f, false, p)
            p.style = Paint.Style.FILL
            p.color = 0xFFAEBAC4.toInt(); c.drawRect(cx + dir * 1.5f - 0.9f, gy - 48f, cx + dir * 1.5f + 0.9f, gy - 44f, p)
            if (type == UnitType.MAN_AT_ARMS) {
              p.color = 0xFFA2AEB8.toInt(); c.drawRect(cx - 5.8f, gy - 48.5f, cx + 5.8f, gy - 46f, p)
              p.color = 0xFFD4DDE6.toInt()
              c.drawCircle(cx - dir * 8.0f, gy - 37f, 2.7f, p); c.drawCircle(cx + dir * 8.0f, gy - 37f, 2.7f, p)
              p.color = 0x88FFFFFF.toInt()
              c.drawCircle(cx - 2.6f, gy - 30f, 0.85f, p); c.drawCircle(cx + 2.6f, gy - 30f, 0.85f, p); c.drawCircle(cx, gy - 28f, 0.85f, p)
            } else {
              p.style = Paint.Style.STROKE; p.strokeWidth = 0.7f; p.color = 0x552E1D10
              c.drawLine(cx - 4.5f, gy - 31f, cx + 4.5f, gy - 25f, p)
              c.drawLine(cx + 4.5f, gy - 31f, cx - 4.5f, gy - 25f, p)
            }
            drawArm(cx + dir * 7.2f, gy - 36f, cx + dir * 12.5f, gy - 29f, topCol, type == UnitType.MAN_AT_ARMS)
            p.style = Paint.Style.STROKE; p.color = 0xFFD9E1E8.toInt(); p.strokeWidth = 3.0f
            c.drawLine(cx + dir * 12.5f, gy - 30f, cx + dir * 26f, gy - 49f, p)
            p.color = 0xFF666F78.toInt(); p.strokeWidth = 0.85f
            c.drawLine(cx + dir * 13.5f, gy - 31f, cx + dir * 24f, gy - 46f, p)
            p.color = 0xFFC79F44.toInt(); p.strokeWidth = 2.5f
            c.drawLine(cx + dir * 9.5f, gy - 31f, cx + dir * 15.5f, gy - 27f, p)
            p.style = Paint.Style.FILL
            val sh = Path().apply {
              moveTo(cx - dir * 15f, gy - 38f); lineTo(cx - dir * 4f, gy - 38f)
              lineTo(cx - dir * 4f, gy - 25f); lineTo(cx - dir * 9.5f, gy - 16.5f)
              lineTo(cx - dir * 15f, gy - 25f); close()
            }
            p.shader = LinearGradient(cx - dir * 14.5f, gy - 37.5f, cx - dir * 4.5f, gy - 17f, fLight, fDark, Shader.TileMode.CLAMP)
            c.drawPath(sh, p); p.shader = null; c.drawPath(sh, aoEdge)
            p.style = Paint.Style.STROKE; p.color = 0xAAEFE6D0.toInt(); p.strokeWidth = 1.0f
            c.drawLine(cx - dir * 9.5f, gy - 36.5f, cx - dir * 9.5f, gy - 18.5f, p)
            c.drawLine(cx - dir * 13.5f, gy - 30f, cx - dir * 5.5f, gy - 30f, p)
            p.style = Paint.Style.FILL
            p.color = 0xFFC7A048.toInt(); c.drawCircle(cx - dir * 9.5f, gy - 30f, 1.9f, p)
          }

          UnitType.SPEARMAN, UnitType.PIKEMAN -> {
            p.shader = LinearGradient(cx - 5.5f, gy - 53.5f, cx + 5.5f, gy - 44f, 0xFFE5EDF4.toInt(), 0xFF576069.toInt(), Shader.TileMode.CLAMP)
            c.drawArc(RectF(cx - 5.8f, gy - 53.5f, cx + 5.8f, gy - 44f), 180f, 180f, true, p); p.shader = null
            if (type == UnitType.PIKEMAN) {
              p.color = 0xFFC8D2DC.toInt(); c.drawOval(RectF(cx - 9.2f, gy - 48.5f, cx + 9.2f, gy - 44.5f), p)
              c.drawOval(RectF(cx - 9.2f, gy - 48.5f, cx + 9.2f, gy - 44.5f), aoEdge)
            }
            drawArm(cx + dir * 7.2f, gy - 36f, cx + dir * 9.5f, gy - 29f, topCol, type == UnitType.PIKEMAN)
            p.style = Paint.Style.STROKE; p.color = 0xFF6B4829.toInt(); p.strokeWidth = 2.6f
            c.drawLine(cx - dir * 7.5f, gy - 16f, cx + dir * 32f, gy - 59f, p)
            p.style = Paint.Style.FILL; p.color = 0xFFD9E1E8.toInt()
            val spearHead = Path().apply {
              moveTo(cx + dir * 36f, gy - 64f); lineTo(cx + dir * 28f, gy - 55f); lineTo(cx + dir * 32.5f, gy - 52f); close()
            }
            c.drawPath(spearHead, p); c.drawPath(spearHead, aoEdge)
            p.color = fMain; c.drawCircle(cx + dir * 28f, gy - 54f, 1.8f, p)
            p.color = 0xFF8C96A0.toInt(); c.drawCircle(cx + dir * 30f, gy - 56f, 1.6f, p)
            p.color = fMain; c.drawCircle(cx - dir * 9.0f, gy - 28f, 6.2f, p); c.drawCircle(cx - dir * 9.0f, gy - 28f, 6.2f, aoEdge)
            p.color = 0xFFC7A048.toInt(); c.drawCircle(cx - dir * 9.0f, gy - 28f, 1.9f, p)
          }

          UnitType.ARCHER, UnitType.CROSSBOWMAN, UnitType.SKIRMISHER -> {
            if (type == UnitType.CROSSBOWMAN) {
              p.shader = LinearGradient(cx - 5.5f, gy - 53.5f, cx + 5.5f, gy - 44f, 0xFFE5EDF4.toInt(), 0xFF576069.toInt(), Shader.TileMode.CLAMP)
              c.drawArc(RectF(cx - 5.8f, gy - 53.5f, cx + 5.8f, gy - 44f), 180f, 180f, true, p); p.shader = null
              c.drawArc(RectF(cx - 5.8f, gy - 53.5f, cx + 5.8f, gy - 44f), 180f, 180f, true, aoEdge)
            } else {
              p.color = if (type == UnitType.ARCHER) 0xFF2C4A22.toInt() else fDark
              c.drawArc(RectF(cx - 6.2f, gy - 53.5f, cx + 6.2f, gy - 43.5f), 165f, 210f, true, p)
              c.drawArc(RectF(cx - 6.2f, gy - 53.5f, cx + 6.2f, gy - 43.5f), 165f, 210f, true, aoEdge)
            }
            p.style = Paint.Style.STROKE; p.color = 0xFF5A391E.toInt(); p.strokeWidth = 3.2f
            c.drawLine(cx - dir * 5.5f, gy - 33f, cx - dir * 10.5f, gy - 46f, p)
            p.color = 0xFFEBE4D4.toInt(); p.strokeWidth = 1.1f
            c.drawLine(cx - dir * 9.5f, gy - 46f, cx - dir * 12f, gy - 49f, p)
            c.drawLine(cx - dir * 11f, gy - 44.5f, cx - dir * 13.5f, gy - 47.5f, p)
            val bowX = cx + dir * 13.0f
            drawArm(cx + dir * 6.5f, gy - 36f, bowX, gy - 31f, topCol, false)
            drawArm(cx - dir * 4.5f, gy - 36f, cx + dir * 3.8f, gy - 31.5f, topCol, false)
            p.style = Paint.Style.STROKE; p.strokeWidth = 2.8f
            p.color = if (type == UnitType.CROSSBOWMAN) 0xFFC5CFD8.toInt() else 0xFF845731.toInt()
            c.drawArc(RectF(bowX - 7f, gy - 43.5f, bowX + 7f, gy - 18.5f), if (facingRight) -75f else 105f, 150f, false, p)
            p.strokeWidth = 0.8f; p.color = 0xFFD9CCB4.toInt()
            c.drawLine(bowX + dir * 1.5f, gy - 42f, bowX + dir * 1.5f, gy - 20f, p)
            p.strokeWidth = 1.3f; p.color = 0xFF52321A.toInt()
            c.drawLine(cx + dir * 3.8f, gy - 30.5f, bowX + dir * 11.5f, gy - 30.5f, p)
            p.style = Paint.Style.FILL; p.color = 0xFFD4DDE6.toInt()
            c.drawCircle(bowX + dir * 11.5f, gy - 30.5f, 1.3f, p)
          }
          UnitType.RIFLEMAN -> {
            // Industrial steel Brodie / campaign helmet
            p.shader = LinearGradient(cx - 7f, gy - 53f, cx + 7f, gy - 44f, 0xFF9EA6AE.toInt(), 0xFF42494F.toInt(), Shader.TileMode.CLAMP)
            c.drawArc(RectF(cx - 6.2f, gy - 53.5f, cx + 6.2f, gy - 44f), 180f, 180f, true, p)
            p.shader = null
            p.color = 0xFF70777E.toInt(); c.drawOval(RectF(cx - 9.4f, gy - 48.5f, cx + 9.4f, gy - 44.5f), p)
            c.drawOval(RectF(cx - 9.4f, gy - 48.5f, cx + 9.4f, gy - 44.5f), aoEdge)
            // Khaki web shoulder straps + ammo pouches
            p.color = 0xFF4B3D24.toInt(); c.drawRect(cx - 5.5f, gy - 25f, cx - 1.5f, gy - 20f, p); c.drawRect(cx + 1.5f, gy - 25f, cx + 5.5f, gy - 20f, p)
            drawArm(cx - dir * 5.0f, gy - 36f, cx + dir * 6.5f, gy - 29f, topCol, false)
            drawArm(cx + dir * 6.5f, gy - 36f, cx + dir * 13.5f, gy - 31f, topCol, false)
            // Wooden rifle stock + steel barrel & bayonet
            p.style = Paint.Style.STROKE; p.strokeWidth = 3.0f; p.color = 0xFF5D3A1C.toInt()
            c.drawLine(cx - dir * 2f, gy - 27f, cx + dir * 14f, gy - 33f, p)
            p.strokeWidth = 2.1f; p.color = 0xFF282E33.toInt()
            c.drawLine(cx + dir * 12f, gy - 32.5f, cx + dir * 30f, gy - 39f, p)
            p.strokeWidth = 1.2f; p.color = 0xFFD9E2EA.toInt()
            c.drawLine(cx + dir * 26f, gy - 37.5f, cx + dir * 35f, gy - 41f, p)
            p.style = Paint.Style.FILL
          }

          UnitType.MARINE -> {
            // Modern Kevlar composite helmet + tactical goggles & rucksack
            p.color = 0xFF2C3526.toInt(); c.drawRect(cx - dir * 10.5f, gy - 37f, cx - dir * 6f, gy - 25f, p)
            p.shader = LinearGradient(cx - 6f, gy - 54f, cx + 6f, gy - 44f, 0xFF56664C.toInt(), 0xFF222B1E.toInt(), Shader.TileMode.CLAMP)
            c.drawArc(RectF(cx - 6.6f, gy - 54f, cx + 6.6f, gy - 44f), 175f, 190f, true, p)
            p.shader = null; c.drawArc(RectF(cx - 6.6f, gy - 54f, cx + 6.6f, gy - 44f), 175f, 190f, true, aoEdge)
            p.color = 0xFF1F2429.toInt(); c.drawRect(cx - 3.5f, gy - 49.5f, cx + 4.8f, gy - 46.5f, p)
            p.color = 0xFF6BB5D8.toInt(); c.drawRect(cx + dir * 1f - 2f, gy - 49f, cx + dir * 1f + 2f, gy - 47f, p)
            // Tactical plate carrier vest + faction shoulder patch & knee pads
            p.color = 0xFF2E382B.toInt(); c.drawRect(cx - 6.2f, gy - 36f, cx + 6.2f, gy - 24f, p); c.drawRect(cx - 6.2f, gy - 36f, cx + 6.2f, gy - 24f, aoEdge)
            p.color = 0xFF1D221B.toInt(); c.drawCircle(cx - dir * 3.5f, gy - 8f, 1.8f, p); c.drawCircle(cx + dir * 3.5f, gy - 8f, 1.8f, p)
            p.color = fMain; c.drawRect(cx - dir * 8.5f, gy - 37f, cx - dir * 5.2f, gy - 31f, p)
            drawArm(cx - dir * 4.5f, gy - 35f, cx + dir * 7.5f, gy - 29f, topCol, true)
            drawArm(cx + dir * 6.5f, gy - 35f, cx + dir * 14f, gy - 30.5f, topCol, true)
            // Modern matte-black assault carbine & curved mag
            p.style = Paint.Style.STROKE; p.strokeWidth = 3.2f; p.color = 0xFF1B1E22.toInt()
            c.drawLine(cx - dir * 1f, gy - 29f, cx + dir * 18f, gy - 33.5f, p)
            p.strokeWidth = 2.0f; p.color = 0xFF3E454D.toInt()
            c.drawLine(cx + dir * 17f, gy - 33.5f, cx + dir * 28f, gy - 35.5f, p)
            p.strokeWidth = 2.4f; p.color = 0xFF262A30.toInt()
            c.drawLine(cx + dir * 11f, gy - 32f, cx + dir * 12.5f, gy - 25.5f, p)
            p.style = Paint.Style.FILL; p.color = 0xFFD94A38.toInt()
            c.drawCircle(cx + dir * 9f, gy - 33.5f, 1.1f, p)
          }

          UnitType.MACHINE_GUNNER -> {
            // Steel helmet + bandolier + heavy bipod-mounted LMG & brass ammo belt
            p.shader = LinearGradient(cx - 6f, gy - 53.5f, cx + 6f, gy - 44f, 0xFF7B856E.toInt(), 0xFF31392B.toInt(), Shader.TileMode.CLAMP)
            c.drawArc(RectF(cx - 6.6f, gy - 53.5f, cx + 6.6f, gy - 44f), 175f, 190f, true, p)
            p.shader = null; c.drawArc(RectF(cx - 6.6f, gy - 53.5f, cx + 6.6f, gy - 44f), 175f, 190f, true, aoEdge)
            // Ammo drum box on hip
            p.color = 0xFF3B472E.toInt(); c.drawRect(cx + dir * 6f, gy - 29f, cx + dir * 14f, gy - 22f, p)
            drawArm(cx - dir * 4.5f, gy - 35f, cx + dir * 7.5f, gy - 28f, topCol, false)
            drawArm(cx + dir * 6.5f, gy - 35f, cx + dir * 14f, gy - 29f, topCol, false)
            // Heavy LMG receiver, perforated barrel shroud, bipod legs, and golden brass belt
            p.style = Paint.Style.STROKE; p.strokeWidth = 3.8f; p.color = 0xFF1F2327.toInt()
            c.drawLine(cx + dir * 1f, gy - 29f, cx + dir * 21f, gy - 32f, p)
            p.strokeWidth = 2.4f; p.color = 0xFF474E56.toInt()
            c.drawLine(cx + dir * 20f, gy - 32f, cx + dir * 32f, gy - 33f, p)
            p.strokeWidth = 1.6f; p.color = 0xFF282D33.toInt()
            c.drawLine(cx + dir * 22f, gy - 31f, cx + dir * 26f, gy - 21f, p)
            p.strokeWidth = 2.0f; p.color = 0xFFD9AE46.toInt()
            c.drawLine(cx + dir * 8f, gy - 29f, cx + dir * 11f, gy - 23f, p)
            p.style = Paint.Style.FILL; p.color = 0xFFB0BAC4.toInt()
            c.drawCircle(cx + dir * 23f, gy - 32.3f, 0.8f, p)
            c.drawCircle(cx + dir * 26f, gy - 32.6f, 0.8f, p)
          }

          UnitType.SNIPER -> {
            // Camouflage ghillie hood & foliage scrim
            p.shader = LinearGradient(cx - 7f, gy - 55f, cx + 7f, gy - 42f, 0xFF4F6B3B.toInt(), 0xFF203018.toInt(), Shader.TileMode.CLAMP)
            c.drawArc(RectF(cx - 7.5f, gy - 54.5f, cx + 7.5f, gy - 42.5f), 160f, 220f, true, p)
            p.shader = null; c.drawArc(RectF(cx - 7.5f, gy - 54.5f, cx + 7.5f, gy - 42.5f), 160f, 220f, true, aoEdge)
            p.color = 0xFF5D7A42.toInt()
            c.drawCircle(cx - dir * 5f, gy - 49f, 2.0f, p); c.drawCircle(cx - dir * 8f, gy - 32f, 2.2f, p)
            p.color = fMain; c.drawRect(cx - dir * 8f, gy - 37f, cx - dir * 5f, gy - 31f, p)
            drawArm(cx - dir * 4.5f, gy - 35f, cx + dir * 7.5f, gy - 30f, topCol, false)
            drawArm(cx + dir * 6.5f, gy - 35f, cx + dir * 15f, gy - 31f, topCol, false)
            // Long precision barrel + suppressor + magnified optic scope
            p.style = Paint.Style.STROKE; p.strokeWidth = 3.0f; p.color = 0xFF2B3126.toInt()
            c.drawLine(cx - dir * 1f, gy - 30f, cx + dir * 16f, gy - 33f, p)
            p.strokeWidth = 1.9f; p.color = 0xFF1B1E22.toInt()
            c.drawLine(cx + dir * 16f, gy - 33f, cx + dir * 37f, gy - 36f, p)
            p.strokeWidth = 2.6f; p.color = 0xFF3E454C.toInt()
            c.drawLine(cx + dir * 7f, gy - 34.5f, cx + dir * 17f, gy - 36f, p)
            p.style = Paint.Style.FILL; p.color = 0xFF7BE8FF.toInt()
            c.drawCircle(cx + dir * 17f, gy - 36f, 1.2f, p)
          }

          UnitType.ROCKET_SOLDIER -> {
            // Tactical helmet with visor + armored chest rig + shoulder-fired AT rocket launcher
            p.shader = LinearGradient(cx - 6f, gy - 54f, cx + 6f, gy - 44f, 0xFF5D6B50.toInt(), 0xFF242C1F.toInt(), Shader.TileMode.CLAMP)
            c.drawArc(RectF(cx - 6.6f, gy - 54f, cx + 6.6f, gy - 44f), 175f, 190f, true, p)
            p.shader = null; c.drawArc(RectF(cx - 6.6f, gy - 54f, cx + 6.6f, gy - 44f), 175f, 190f, true, aoEdge)
            p.color = 0xFF343E2D.toInt(); c.drawRect(cx - 6.2f, gy - 36f, cx + 6.2f, gy - 24f, p)
            p.color = fMain; c.drawRect(cx - dir * 8.5f, gy - 37f, cx - dir * 5.2f, gy - 31f, p)
            drawArm(cx - dir * 4.5f, gy - 35f, cx + dir * 6f, gy - 39f, topCol, true)
            drawArm(cx + dir * 6.5f, gy - 35f, cx + dir * 13f, gy - 41f, topCol, true)
            // Thick olive-drab shoulder-mounted launcher tube + protruding red/silver warhead
            p.style = Paint.Style.STROKE; p.strokeWidth = 6.2f; p.color = 0xFF425235.toInt()
            c.drawLine(cx - dir * 16f, gy - 38f, cx + dir * 24f, gy - 46f, p)
            p.strokeWidth = 7.4f; p.color = 0xFF252D1E.toInt()
            c.drawLine(cx - dir * 18f, gy - 37.5f, cx - dir * 14f, gy - 38.5f, p)
            c.drawLine(cx + dir * 21f, gy - 45.4f, cx + dir * 25f, gy - 46.2f, p)
            p.style = Paint.Style.FILL; p.color = 0xFFD94A38.toInt()
            c.drawCircle(cx + dir * 28f, gy - 46.8f, 3.0f, p)
            p.color = 0xFFE8EEF2.toInt()
            c.drawCircle(cx + dir * 30f, gy - 47.2f, 1.4f, p)
          }
          else -> Unit
        }
      }
    }
    return bmp
  }
}
