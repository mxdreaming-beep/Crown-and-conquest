package com.example.game.render

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import com.example.game.model.MapType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

object ProceduralTextures {

  private const val TEX_SIZE = 512

  // Photographic-grade natural biome terrain bitmaps (shared safely across immutable brushes & sprites)
  val grassBitmap: Bitmap by lazy {
    buildHeightLitGround(
      baseCol = 0xFF3E5428.toInt(),
      highCol = 0xFF5D763A.toInt(),
      lowCol = 0xFF223115.toInt(),
      earthCol = 0xFF59452D.toInt(),
      seed = 11,
      bladeCount = 6200,
      pebbleCount = 260,
      cloverCount = 135,
    )
  }
  val grassAltBitmap: Bitmap by lazy {
    buildHeightLitGround(
      baseCol = 0xFF475F2E.toInt(),
      highCol = 0xFF6B8542.toInt(),
      lowCol = 0xFF283A19.toInt(),
      earthCol = 0xFF624D33.toInt(),
      seed = 29,
      bladeCount = 5800,
      pebbleCount = 240,
      cloverCount = 155,
    )
  }

  val arabiaGrassBitmap: Bitmap by lazy {
    buildHeightLitGround(
      baseCol = 0xFF938248.toInt(),
      highCol = 0xFFB8A360.toInt(),
      lowCol = 0xFF5E5228.toInt(),
      earthCol = 0xFFAC8450.toInt(),
      seed = 41,
      bladeCount = 3100,
      pebbleCount = 440,
      cloverCount = 35,
      crackCount = 40,
    )
  }
  val arabiaGrassAltBitmap: Bitmap by lazy {
    buildHeightLitGround(
      baseCol = 0xFFA18C52.toInt(),
      highCol = 0xFFC5AE6D.toInt(),
      lowCol = 0xFF69592E.toInt(),
      earthCol = 0xFFB88F59.toInt(),
      seed = 43,
      bladeCount = 2400,
      pebbleCount = 500,
      cloverCount = 25,
      crackCount = 55,
    )
  }
  val arabiaDirtBitmap: Bitmap by lazy {
    buildHeightLitGround(
      baseCol = 0xFFB58C56.toInt(),
      highCol = 0xFFD4AC74.toInt(),
      lowCol = 0xFF7D5A31.toInt(),
      earthCol = 0xFFDFBC86.toInt(),
      seed = 47,
      bladeCount = 90,
      pebbleCount = 1500,
      cloverCount = 0,
      crackCount = 120,
    )
  }

  val forestGrassBitmap: Bitmap by lazy {
    buildHeightLitGround(
      baseCol = 0xFF2A3E20.toInt(),
      highCol = 0xFF425C2E.toInt(),
      lowCol = 0xFF152210.toInt(),
      earthCol = 0xFF4A3520.toInt(),
      seed = 53,
      bladeCount = 6200,
      pebbleCount = 290,
      cloverCount = 140,
    )
  }
  val forestGrassAltBitmap: Bitmap by lazy {
    buildHeightLitGround(
      baseCol = 0xFF334925.toInt(),
      highCol = 0xFF4D6835.toInt(),
      lowCol = 0xFF1A2B14.toInt(),
      earthCol = 0xFF543C25.toInt(),
      seed = 59,
      bladeCount = 5600,
      pebbleCount = 310,
      cloverCount = 125,
    )
  }
  val forestDirtBitmap: Bitmap by lazy {
    buildHeightLitGround(
      baseCol = 0xFF5D4228.toInt(),
      highCol = 0xFF7C5A39.toInt(),
      lowCol = 0xFF382515.toInt(),
      earthCol = 0xFF3D4B24.toInt(),
      seed = 61,
      bladeCount = 620,
      pebbleCount = 1150,
      cloverCount = 40,
      crackCount = 45,
    )
  }

  val dirtBitmap: Bitmap by lazy {
    buildHeightLitGround(
      baseCol = 0xFF7D603D.toInt(),
      highCol = 0xFF9E7D54.toInt(),
      lowCol = 0xFF4F3A21.toInt(),
      earthCol = 0xFFB08D61.toInt(),
      seed = 73,
      bladeCount = 150,
      pebbleCount = 1650,
      cloverCount = 0,
      crackCount = 75,
    )
  }

  val waterBitmap: Bitmap by lazy { buildWaterBitmap() }
  val arabiaCliffBitmap: Bitmap by lazy {
    buildRockStrataBitmap(0xFF9B6D46.toInt(), 0xFFC79B70.toInt(), 0xFF4B2E1A.toInt(), seed = 83)
  }
  val stoneBitmap: Bitmap by lazy { buildStoneMasonryBitmap() }
  val cliffStoneBitmap: Bitmap by lazy {
    buildRockStrataBitmap(0xFF696B6F.toInt(), 0xFF9C9FA4.toInt(), 0xFF2B2D31.toInt(), seed = 89)
  }
  val plasterBitmap: Bitmap by lazy { buildPlasterBitmap() }
  val thatchBitmap: Bitmap by lazy { buildThatchBitmap() }
  val slateBitmap: Bitmap by lazy { buildSlateBitmap() }
  val timberBitmap: Bitmap by lazy { buildTimberBitmap() }
  val plowedSoilBitmap: Bitmap by lazy { buildPlowedSoilBitmap() }
  val concreteBitmap: Bitmap by lazy { buildConcreteBitmap() }
  val brickBitmap: Bitmap by lazy { buildBrickBitmap() }

  val grassShader: BitmapShader by lazy { makeTiledShader(grassBitmap) }
  val grassAltShader: BitmapShader by lazy { makeTiledShader(grassAltBitmap) }
  val arabiaGrassShader: BitmapShader by lazy { makeTiledShader(arabiaGrassBitmap) }
  val arabiaGrassAltShader: BitmapShader by lazy { makeTiledShader(arabiaGrassAltBitmap) }
  val arabiaDirtShader: BitmapShader by lazy { makeTiledShader(arabiaDirtBitmap) }
  val forestGrassShader: BitmapShader by lazy { makeTiledShader(forestGrassBitmap) }
  val forestGrassAltShader: BitmapShader by lazy { makeTiledShader(forestGrassAltBitmap) }
  val forestDirtShader: BitmapShader by lazy { makeTiledShader(forestDirtBitmap) }
  val dirtShader: BitmapShader by lazy { makeTiledShader(dirtBitmap) }
  val waterShader: BitmapShader by lazy { makeTiledShader(waterBitmap) }
  val arabiaCliffShader: BitmapShader by lazy { makeTiledShader(arabiaCliffBitmap) }
  val stoneShader: BitmapShader by lazy { makeTiledShader(stoneBitmap) }
  val cliffStoneShader: BitmapShader by lazy { makeTiledShader(cliffStoneBitmap) }
  val plasterShader: BitmapShader by lazy { makeTiledShader(plasterBitmap) }
  val thatchShader: BitmapShader by lazy { makeTiledShader(thatchBitmap) }
  val slateShader: BitmapShader by lazy { makeTiledShader(slateBitmap) }
  val timberShader: BitmapShader by lazy { makeTiledShader(timberBitmap) }
  val plowedSoilShader: BitmapShader by lazy { makeTiledShader(plowedSoilBitmap) }
  val concreteShader: BitmapShader by lazy { makeTiledShader(concreteBitmap) }
  val brickShader: BitmapShader by lazy { makeTiledShader(brickBitmap) }

  // Immutable ShaderBrush singletons (never mutated with setLocalMatrix — safe for Android HWUI RecordingCanvas)
  val grassBrush: ShaderBrush by lazy { ShaderBrush(makeTiledShader(grassBitmap)) }
  val grassAltBrush: ShaderBrush by lazy { ShaderBrush(makeTiledShader(grassAltBitmap)) }
  val arabiaGrassBrush: ShaderBrush by lazy { ShaderBrush(makeTiledShader(arabiaGrassBitmap)) }
  val arabiaGrassAltBrush: ShaderBrush by lazy { ShaderBrush(makeTiledShader(arabiaGrassAltBitmap)) }
  val arabiaDirtBrush: ShaderBrush by lazy { ShaderBrush(makeTiledShader(arabiaDirtBitmap)) }
  val forestGrassBrush: ShaderBrush by lazy { ShaderBrush(makeTiledShader(forestGrassBitmap)) }
  val forestGrassAltBrush: ShaderBrush by lazy { ShaderBrush(makeTiledShader(forestGrassAltBitmap)) }
  val forestDirtBrush: ShaderBrush by lazy { ShaderBrush(makeTiledShader(forestDirtBitmap)) }
  val dirtBrush: ShaderBrush by lazy { ShaderBrush(makeTiledShader(dirtBitmap)) }
  val waterBrush: ShaderBrush by lazy { ShaderBrush(makeTiledShader(waterBitmap)) }
  val arabiaCliffBrush: ShaderBrush by lazy { ShaderBrush(makeTiledShader(arabiaCliffBitmap)) }
  val cliffStoneBrush: ShaderBrush by lazy { ShaderBrush(makeTiledShader(cliffStoneBitmap)) }
  val timberBrush: ShaderBrush by lazy { ShaderBrush(makeTiledShader(timberBitmap)) }

  fun groundShaderFor(mapType: MapType, alt: Boolean): BitmapShader =
    when (mapType) {
      MapType.ARABIA -> if (alt) arabiaGrassAltShader else arabiaGrassShader
      MapType.BLACK_FOREST -> if (alt) forestGrassAltShader else forestGrassShader
      MapType.COASTAL -> if (alt) grassAltShader else grassShader
    }

  fun dirtShaderFor(mapType: MapType): BitmapShader =
    when (mapType) {
      MapType.ARABIA -> arabiaDirtShader
      MapType.BLACK_FOREST -> forestDirtShader
      MapType.COASTAL -> dirtShader
    }

  fun cliffShaderFor(mapType: MapType): BitmapShader =
    if (mapType == MapType.ARABIA) arabiaCliffShader else cliffStoneShader

  fun groundBrushFor(mapType: MapType, alt: Boolean): ShaderBrush =
    when (mapType) {
      MapType.ARABIA -> if (alt) arabiaGrassAltBrush else arabiaGrassBrush
      MapType.BLACK_FOREST -> if (alt) forestGrassAltBrush else forestGrassBrush
      MapType.COASTAL -> if (alt) grassAltBrush else grassBrush
    }

  fun dirtBrushFor(mapType: MapType): ShaderBrush =
    when (mapType) {
      MapType.ARABIA -> arabiaDirtBrush
      MapType.BLACK_FOREST -> forestDirtBrush
      MapType.COASTAL -> dirtBrush
    }

  fun cliffBrushFor(mapType: MapType): ShaderBrush =
    if (mapType == MapType.ARABIA) arabiaCliffBrush else cliffStoneBrush

  fun warmUpForMap(mapType: MapType) {
    groundBrushFor(mapType, alt = false)
    groundBrushFor(mapType, alt = true)
    dirtBrushFor(mapType)
    if (mapType == MapType.ARABIA) {
      cliffBrushFor(mapType)
    } else {
      waterBrush
      timberBrush
    }
    stoneBitmap
    plasterBitmap
    slateBitmap
  }

  private fun makeTiledShader(bmp: Bitmap): BitmapShader =
    BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)

  fun newScaledShader(bmp: Bitmap, scale: Float, ox: Float = 0f, oy: Float = 0f): BitmapShader {
    val shader = makeTiledShader(bmp)
    val m = Matrix()
    m.setScale(scale, scale)
    m.postTranslate(ox, oy)
    shader.setLocalMatrix(m)
    return shader
  }

  fun scaledShader(shader: BitmapShader, scale: Float, ox: Float = 0f, oy: Float = 0f): BitmapShader {
    val m = Matrix()
    m.setScale(scale, scale)
    m.postTranslate(ox, oy)
    shader.setLocalMatrix(m)
    return shader
  }

  private fun hash2(x: Int, y: Int, period: Int, seed: Int): Float {
    val px =
      if ((period and (period - 1)) == 0) (x and (period - 1))
      else ((x % period) + period) % period
    val py =
      if ((period and (period - 1)) == 0) (y and (period - 1))
      else ((y % period) + period) % period
    var h = px * 374761393 + py * 668265263 + seed * 1442695041
    h = (h xor (h ushr 13)) * 1274126177
    h = h xor (h ushr 16)
    return (h and 0xFFFF) * (1f / 65535f)
  }

  private fun tileableNoise(u: Float, v: Float, period: Int, seed: Int): Float {
    val x = u * period
    val y = v * period
    val xi = if (x >= 0f) x.toInt() else x.toInt() - 1
    val yi = if (y >= 0f) y.toInt() else y.toInt() - 1
    val xf = x - xi
    val yf = y - yi
    val sx = xf * xf * (3f - 2f * xf)
    val sy = yf * yf * (3f - 2f * yf)
    val n00 = hash2(xi, yi, period, seed)
    val n10 = hash2(xi + 1, yi, period, seed)
    val n01 = hash2(xi, yi + 1, period, seed)
    val n11 = hash2(xi + 1, yi + 1, period, seed)
    val nx0 = n00 + (n10 - n00) * sx
    val nx1 = n01 + (n11 - n01) * sx
    return nx0 + (nx1 - nx0) * sy
  }

  private fun tileableFbm5(u: Float, v: Float, seed: Int): Float {
    return tileableNoise(u, v, 8, seed) * 0.42f +
      tileableNoise(u, v, 16, seed + 17) * 0.25f +
      tileableNoise(u, v, 32, seed + 37) * 0.16f +
      tileableNoise(u, v, 64, seed + 71) * 0.11f +
      tileableNoise(u, v, 128, seed + 113) * 0.06f
  }

  private fun lerpColor(c0: Int, c1: Int, tRaw: Float): Int {
    val t = tRaw.coerceIn(0f, 1f)
    val r = (Color.red(c0) + (Color.red(c1) - Color.red(c0)) * t).toInt().coerceIn(0, 255)
    val g = (Color.green(c0) + (Color.green(c1) - Color.green(c0)) * t).toInt().coerceIn(0, 255)
    val b = (Color.blue(c0) + (Color.blue(c1) - Color.blue(c0)) * t).toInt().coerceIn(0, 255)
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
  }

  private fun shadeRgb(col: Int, mul: Float): Int {
    val r = (Color.red(col) * mul).toInt().coerceIn(0, 255)
    val g = (Color.green(col) * mul).toInt().coerceIn(0, 255)
    val b = (Color.blue(col) * mul).toInt().coerceIn(0, 255)
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
  }

  private fun buildHeightLitGround(
    baseCol: Int,
    highCol: Int,
    lowCol: Int,
    earthCol: Int,
    seed: Int,
    bladeCount: Int,
    pebbleCount: Int = 220,
    cloverCount: Int = 80,
    crackCount: Int = 0,
  ): Bitmap {
    val size = TEX_SIZE
    val pixels = IntArray(size * size)
    val heights = FloatArray(size * size)
    val inv = 1f / size

    for (y in 0 until size) {
      val v = y * inv
      for (x in 0 until size) {
        val u = x * inv
        val macro = tileableFbm5(u, v, seed)
        val fine = tileableNoise(u, v, 160, seed + 251) * 0.08f
        heights[y * size + x] = (macro * 0.92f + fine).coerceIn(0f, 1f)
      }
    }

    for (y in 0 until size) {
      val ym = (y - 1 + size) % size
      val yp = (y + 1) % size
      for (x in 0 until size) {
        val xm = (x - 1 + size) % size
        val xp = (x + 1) % size
        val h = heights[y * size + x]
        val dx = heights[y * size + xp] - heights[y * size + xm]
        val dy = heights[yp * size + x] - heights[ym * size + x]
        // Photorealistic directional sunlight & micro-crevice ambient occlusion
        val slopeShade = (1.0f - dx * 5.8f - dy * 5.0f).coerceIn(0.56f, 1.38f)
        val ao = (0.81f + h * 0.34f).coerceIn(0.76f, 1.12f)

        val soilMix = tileableNoise(x * inv, y * inv, 6, seed + 101)
        val dryMix = tileableNoise(x * inv, y * inv, 12, seed + 163)
        val moisture = tileableNoise(x * inv, y * inv, 9, seed + 227)
        val microGrain = tileableNoise(x * inv, y * inv, 160, seed + 197)
        val c =
          when {
            h < 0.44f -> lerpColor(lowCol, baseCol, h / 0.44f)
            else -> lerpColor(baseCol, highCol, (h - 0.44f) / 0.56f)
          }
        val earthy = if (soilMix < 0.38f) lerpColor(c, earthCol, (0.38f - soilMix) * 1.48f) else c
        val sunDry = if (dryMix > 0.66f) lerpColor(earthy, lerpColor(earthCol, highCol, 0.48f), (dryMix - 0.66f) * 1.18f) else earthy
        val dampRich = if (moisture < 0.25f) shadeRgb(sunDry, 0.90f) else sunDry
        val grainShaded = shadeRgb(dampRich, slopeShade * ao * (0.89f + microGrain * 0.22f))
        pixels[y * size + x] = grainShaded
      }
    }

    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    bmp.setPixels(pixels, 0, size, 0, 0, size, size)

    val c = Canvas(bmp)
    val rng = Random(seed * 13 + 7)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)

    if (crackCount > 0) {
      p.style = Paint.Style.STROKE
      for (i in 0 until crackCount) {
        var cx0 = rng.nextFloat() * size
        var cy0 = rng.nextFloat() * size
        p.strokeWidth = rng.nextFloat() * 0.85f + 0.45f
        p.color = shadeRgb(lowCol, 0.60f)
        p.alpha = rng.nextInt(65, 130)
        for (seg in 0..4) {
          val cx1 = cx0 + rng.nextFloat() * 12f - 6f
          val cy1 = cy0 + rng.nextFloat() * 10f - 5f
          c.drawLine(cx0, cy0, cx1, cy1, p)
          cx0 = cx1; cy0 = cy1
        }
      }
    }

    // Photorealistic weathered stone gravel & pebbles with directional rim & contact shadow
    for (i in 0 until pebbleCount) {
      val px = rng.nextFloat() * size
      val py = rng.nextFloat() * size
      val r = rng.nextFloat() * 2.4f + 0.55f
      p.style = Paint.Style.FILL
      p.color = 0x8218120B.toInt()
      c.drawOval(px - r * 0.8f + 0.8f, py - r * 0.5f + 0.9f, px + r * 1.05f + 0.8f, py + r * 0.78f + 0.9f, p)
      p.color = lerpColor(earthCol, 0xFFBDB3A1.toInt(), rng.nextFloat() * 0.62f)
      p.alpha = rng.nextInt(145, 230)
      c.drawOval(px - r, py - r * 0.70f, px + r, py + r * 0.70f, p)
      p.color = 0x52FFF8EA
      c.drawCircle(px - r * 0.28f, py - r * 0.24f, r * 0.34f, p)
    }

    // Subtle organic moss & clover micro-rosettes + tiny wild blossoms
    for (i in 0 until cloverCount) {
      val cx0 = rng.nextFloat() * size
      val cy0 = rng.nextFloat() * size
      val rad = rng.nextFloat() * 1.7f + 1.1f
      p.style = Paint.Style.FILL
      p.color = 0x4C101A0B
      c.drawCircle(cx0 + 0.6f, cy0 + 0.8f, rad * 1.35f, p)
      p.color = lerpColor(baseCol, highCol, 0.62f)
      p.alpha = rng.nextInt(125, 200)
      c.drawCircle(cx0 - rad * 0.45f, cy0, rad * 0.60f, p)
      c.drawCircle(cx0 + rad * 0.45f, cy0, rad * 0.60f, p)
      c.drawCircle(cx0, cy0 - rad * 0.48f, rad * 0.60f, p)
      if (i % 9 == 0) {
        p.color = if (i % 18 == 0) 0xD8F2E7BE.toInt() else 0xC8E0BC5E.toInt()
        c.drawCircle(cx0, cy0 - rad * 0.2f, 0.85f, p)
      }
    }

    // Ultra-fine naturalistic grass stalks / dry chaff / conifer needles with root shadow
    p.style = Paint.Style.STROKE
    p.strokeCap = Paint.Cap.ROUND
    for (i in 0 until bladeCount) {
      val bx = rng.nextFloat() * size
      val by = rng.nextFloat() * size
      val lean = rng.nextFloat() * 5.0f - 2.5f
      val bh = rng.nextFloat() * 6.8f + 2.4f
      p.strokeWidth = rng.nextFloat() * 0.65f + 0.35f
      p.color =
        when (i % 6) {
          0 -> highCol
          1 -> lowCol
          2 -> baseCol
          3 -> lerpColor(highCol, 0xFFA8995E.toInt(), 0.42f)
          4 -> lerpColor(lowCol, earthCol, 0.45f)
          else -> shadeRgb(baseCol, 0.82f)
        }
      p.alpha = rng.nextInt(95, 192)
      c.drawLine(bx, by, bx + lean, by - bh, p)
    }
    p.strokeCap = Paint.Cap.BUTT
    return bmp
  }

  private fun buildRockStrataBitmap(baseCol: Int, highCol: Int, darkCol: Int, seed: Int): Bitmap {
    val size = TEX_SIZE
    val pixels = IntArray(size * size)
    val heights = FloatArray(size * size)
    val inv = 1f / size
    for (y in 0 until size) {
      val v = y * inv
      for (x in 0 until size) {
        val u = x * inv
        val n = tileableFbm5(u, v, seed)
        val strata = sin(v * 2f * PI.toFloat() * 8f + tileableNoise(u, v, 10, seed + 5) * 4.5f) * 0.16f
        val cleavage = abs(sin((u * 1.7f + v * 0.6f) * 2f * PI.toFloat() * 5f + n * 2.8f)) * 0.10f
        val grit = tileableNoise(u, v, 160, seed + 83) * 0.06f
        heights[y * size + x] = (n + strata - cleavage + grit).coerceIn(0f, 1f)
      }
    }
    for (y in 0 until size) {
      val ym = (y - 1 + size) % size
      val yp = (y + 1) % size
      for (x in 0 until size) {
        val xm = (x - 1 + size) % size
        val xp = (x + 1) % size
        val h = heights[y * size + x]
        val dx = heights[y * size + xp] - heights[y * size + xm]
        val dy = heights[yp * size + x] - heights[ym * size + x]
        val shade = (1.0f - dx * 6.8f - dy * 5.8f).coerceIn(0.42f, 1.44f)
        val col = if (h < 0.46f) lerpColor(darkCol, baseCol, h / 0.46f) else lerpColor(baseCol, highCol, (h - 0.46f) / 0.54f)
        val lichen = tileableNoise(x * inv, y * inv, 14, seed + 199)
        val mica = tileableNoise(x * inv, y * inv, 192, seed + 257)
        var weathered = if (lichen > 0.76f) lerpColor(col, 0xFF697450.toInt(), (lichen - 0.76f) * 1.7f) else col
        if (mica > 0.88f) weathered = lerpColor(weathered, 0xFFF2ECE0.toInt(), (mica - 0.88f) * 2.5f)
        pixels[y * size + x] = shadeRgb(weathered, shade)
      }
    }
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    bmp.setPixels(pixels, 0, size, 0, 0, size, size)
    val c = Canvas(bmp)
    val rng = Random(seed * 29)
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    for (i in 0 until 115) {
      val x0 = rng.nextFloat() * size
      val y0 = rng.nextFloat() * size
      p.strokeWidth = rng.nextFloat() * 1.05f + 0.45f
      p.color = if (i % 4 == 0) 0x42F4EDE0 else 0x8A14100C.toInt()
      c.drawLine(x0, y0, x0 + rng.nextFloat() * 32f - 16f, y0 + rng.nextFloat() * 18f - 9f, p)
    }
    return bmp
  }

  private fun buildWaterBitmap(): Bitmap {
    val size = TEX_SIZE
    val heights = FloatArray(size * size)
    val pixels = IntArray(size * size)
    val inv = 1f / size
    for (y in 0 until size) {
      val v = y * inv
      for (x in 0 until size) {
        val u = x * inv
        val n1 = tileableFbm5(u, v, 99)
        val n2 = tileableNoise(u, v, 32, 141)
        val n3 = tileableNoise(u, v, 96, 173)
        val swellA = sin((u * 5f + v * 3f) * 2f * PI.toFloat() + n1 * 4.0f) * 0.32f
        val swellB = cos((u * 4f - v * 6f) * 2f * PI.toFloat() + n2 * 3.4f) * 0.26f
        val ripple = sin((u * 16f + v * 12f) * 2f * PI.toFloat() + n2 * 2.8f) * 0.14f
        val microCapillary = sin((u * 32f - v * 26f) * 2f * PI.toFloat() + n3 * 2.2f) * 0.06f
        val fine = n3 * 0.08f
        heights[y * size + x] = n1 * 0.42f + (swellA + swellB + ripple + microCapillary) * 0.34f + fine
      }
    }
    for (y in 0 until size) {
      val ym = (y - 1 + size) % size
      val yp = (y + 1) % size
      for (x in 0 until size) {
        val xm = (x - 1 + size) % size
        val xp = (x + 1) % size
        val h = heights[y * size + x]
        val dx = (heights[y * size + xp] - heights[y * size + xm]) * 7.8f
        val dy = (heights[yp * size + x] - heights[ym * size + x]) * 7.8f
        val sunDot = (-dx * 0.66f - dy * 0.62f + 0.52f).coerceIn(0f, 1.55f)
        val spec = (sunDot - 0.54f).coerceAtLeast(0f).pow(2.5f) * 3.1f
        val fresnel = (1f - (sunDot * 0.48f).coerceIn(0f, 0.8f)).pow(2f) * 0.25f
        val siltShift = tileableNoise(x * inv, y * inv, 6, 199)
        val deepCol = if (siltShift < 0.36f) 0xFF0A2230.toInt() else 0xFF0C263B.toInt()
        val shallowCol = if (siltShift > 0.66f) 0xFF246175.toInt() else 0xFF1E536E.toInt()
        val depthBase = lerpColor(deepCol, shallowCol, (h + 0.30f).coerceIn(0f, 1f))
        val skyBounce = lerpColor(depthBase, 0xFF4F8CA3.toInt(), (sunDot * 0.42f + fresnel).coerceIn(0f, 0.65f))
        val reflected = if (spec > 0.018f) lerpColor(skyBounce, 0xFFF2FAFC.toInt(), spec.coerceIn(0f, 0.88f)) else skyBounce
        pixels[y * size + x] = reflected
      }
    }
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    bmp.setPixels(pixels, 0, size, 0, 0, size, size)
    return bmp
  }

  private fun buildStoneMasonryBitmap(): Bitmap {
    val size = TEX_SIZE
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    // Recessed sandy mortar background
    c.drawColor(0xFF494239.toInt())
    val rng = Random(137)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.style = Paint.Style.FILL
    for (m in 0 until 1400) {
      p.color = if (m % 2 == 0) 0x3E6B6254 else 0x3E241E17
      c.drawCircle(rng.nextFloat() * size, rng.nextFloat() * size, rng.nextFloat() * 1.1f + 0.3f, p)
    }

    val rowH = 20
    val rows = size / rowH
    for (row in 0 until rows + 1) {
      val y = row * rowH
      val offset = if (row % 2 == 0) 0 else 20
      var x = -offset
      while (x < size) {
        val bw = if (x + 48 >= size && offset == 0) size - x else rng.nextInt(28, 50)
        val shade = rng.nextInt(-18, 18)
        val warm = rng.nextInt(-8, 12)
        val r = (140 + shade + warm).coerceIn(0, 255)
        val g = (134 + shade + warm / 2).coerceIn(0, 255)
        val b = (122 + shade).coerceIn(0, 255)

        p.style = Paint.Style.FILL
        p.color = Color.rgb(r, g, b)
        c.drawRoundRect(x + 1.3f, y + 1.3f, x + bw - 1.3f, y + rowH - 1.3f, 1.8f, 1.8f, p)

        // Subtle sunlit limestone chamfer top/left & crevice shadow bottom/right
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.4f
        p.color = Color.rgb((r + 26).coerceAtMost(255), (g + 24).coerceAtMost(255), (b + 20).coerceAtMost(255))
        c.drawLine(x + 2.0f, y + 2.0f, x + bw - 2.0f, y + 2.0f, p)
        c.drawLine(x + 2.0f, y + 2.0f, x + 2.0f, y + rowH - 2.0f, p)
        p.color = Color.rgb((r - 38).coerceAtLeast(0), (g - 38).coerceAtLeast(0), (b - 34).coerceAtLeast(0))
        c.drawLine(x + 2.0f, y + rowH - 1.8f, x + bw - 1.8f, y + rowH - 1.8f, p)
        c.drawLine(x + bw - 1.8f, y + 2.0f, x + bw - 1.8f, y + rowH - 1.8f, p)

        // Mineral pitting & dressed chisel striations
        p.style = Paint.Style.FILL
        for (dot in 0 until 15) {
          p.color = if (dot % 3 == 0) 0x24FFFFFF else 0x2A000000
          c.drawCircle(
            x + 3f + rng.nextFloat() * (bw - 6f).coerceAtLeast(1f),
            y + 3f + rng.nextFloat() * (rowH - 6f).coerceAtLeast(1f),
            rng.nextFloat() * 1.2f + 0.35f,
            p,
          )
        }
        x += bw
      }
    }

    // Organic weathering stains & subtle crevice moss
    for (i in 0 until 180) {
      val row = rng.nextInt(rows.coerceAtLeast(1))
      p.color = if (i % 2 == 0) 0x4A3B5228 else 0x3A2E281F
      c.drawCircle(rng.nextFloat() * size, row * rowH.toFloat(), rng.nextFloat() * 3.8f + 1.0f, p)
    }
    return bmp
  }

  private fun buildPlasterBitmap(): Bitmap {
    val size = TEX_SIZE
    val pixels = IntArray(size * size)
    val inv = 1f / size
    for (y in 0 until size) {
      val v = y * inv
      for (x in 0 until size) {
        val u = x * inv
        val n = tileableFbm5(u, v, 211)
        val grit = tileableNoise(u, v, 128, 223)
        val stain = tileableNoise(u, v, 8, 241)
        val col = lerpColor(0xFFB5A78C.toInt(), 0xFFDDD2BA.toInt(), n)
        val damp = if (stain < 0.28f) lerpColor(col, 0xFF8C7E66.toInt(), (0.28f - stain) * 1.1f) else col
        pixels[y * size + x] = shadeRgb(damp, 0.92f + grit * 0.16f)
      }
    }
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    bmp.setPixels(pixels, 0, size, 0, 0, size, size)
    val c = Canvas(bmp)
    val rng = Random(211)
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    for (i in 0 until 180) {
      val sx = rng.nextFloat() * size
      val sy = rng.nextFloat() * size
      p.strokeWidth = if (i % 3 == 0) 1.2f else 0.7f
      p.color = if (i % 3 == 0) 0x16FFFFFF else 0x3052412C
      c.drawLine(sx, sy, sx + rng.nextFloat() * 16f - 8f, sy + rng.nextFloat() * 14f - 5f, p)
    }
    return bmp
  }

  private fun buildThatchBitmap(): Bitmap {
    val size = TEX_SIZE
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    c.drawColor(0xFF7D6134.toInt())
    val rng = Random(307)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    for (tier in 0 until size step 24) {
      p.style = Paint.Style.FILL
      p.color = 0x66221608
      c.drawRect(0f, tier + 18f, size.toFloat(), tier + 24f, p)
      p.color = 0x26EED49B
      c.drawRect(0f, tier.toFloat(), size.toFloat(), tier + 2.5f, p)
    }
    p.style = Paint.Style.STROKE
    for (i in 0 until 6800) {
      val x = rng.nextFloat() * size
      val y = rng.nextFloat() * size
      p.strokeWidth = rng.nextFloat() * 0.95f + 0.45f
      p.color =
        when (i % 5) {
          0 -> 0xFFB8985B.toInt()
          1 -> 0xFF4F3B1B.toInt()
          2 -> 0xFF947640.toInt()
          3 -> 0xFFC7A96D.toInt()
          else -> 0xFF6B5024.toInt()
        }
      p.alpha = rng.nextInt(135, 225)
      c.drawLine(x, y, x + rng.nextFloat() * 3.6f - 1.8f, y + rng.nextFloat() * 12f + 4f, p)
    }
    p.style = Paint.Style.FILL
    for (i in 0 until 110) {
      p.color = if (i % 2 == 0) 0x4E3F5428 else 0x3E2F421D
      c.drawCircle(rng.nextFloat() * size, rng.nextFloat() * size, rng.nextFloat() * 4.2f + 1.2f, p)
    }
    return bmp
  }

  private fun buildSlateBitmap(): Bitmap {
    val size = TEX_SIZE
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    c.drawColor(0xFF1B212A.toInt())
    val rng = Random(409)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    val rowH = 18
    val colW = 18
    for (row in 0 until size / rowH + 1) {
      val y = row * rowH
      val offset = if (row % 2 == 0) 0 else colW / 2
      var x = -offset
      while (x < size) {
        val s = rng.nextInt(-16, 16)
        val moss = if (rng.nextInt(9) == 0) 12 else 0
        val r = (58 + s).coerceIn(0, 255)
        val g = (66 + s + moss).coerceIn(0, 255)
        val b = (78 + s).coerceIn(0, 255)
        p.style = Paint.Style.FILL
        p.color = Color.rgb(r, g, b)
        c.drawRoundRect(x + 0.9f, y + 0.9f, x + colW - 0.9f, y + rowH - 0.9f, 1.5f, 1.5f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.1f
        p.color = Color.rgb((r + 28).coerceAtMost(255), (g + 28).coerceAtMost(255), (b + 30).coerceAtMost(255))
        c.drawLine(x + 1.8f, y + 1.8f, x + colW - 1.8f, y + 1.8f, p)
        p.color = 0xAA0A0E15.toInt()
        c.drawLine(x + 1.5f, y + rowH - 1.1f, x + colW - 1.5f, y + rowH - 1.1f, p)
        p.strokeWidth = 0.65f; p.color = 0x24FFFFFF
        c.drawLine(x + 4f, y + 4f, x + colW - 5f, y + rowH - 5f, p)
        x += colW
      }
    }
    return bmp
  }

  private fun buildTimberBitmap(): Bitmap {
    val size = TEX_SIZE
    val pixels = IntArray(size * size)
    val inv = 1f / size
    val plankCount = 18
    val plankPx = size.toFloat() / plankCount
    for (y in 0 until size) {
      val v = y * inv
      val plankIdx = (y / plankPx).toInt()
      val plankY = (y % plankPx) / plankPx
      val plankTone = ((plankIdx * 37) % 19 - 9) * 0.012f
      for (x in 0 until size) {
        val u = x * inv
        val warp = tileableFbm5(u, v, 521 + plankIdx * 7) * 0.28f
        val ring = sin((plankY * 3.8f + warp * 9.0f) * PI.toFloat()) * 0.5f + 0.5f
        val fineFiber = tileableNoise(u * 2f, v * 0.5f, 128, 541) * 0.14f
        var col = lerpColor(0xFF452A15.toInt(), 0xFF7D5330.toInt(), ring * 0.72f + fineFiber)
        // Plank joint bevel & ambient shadow
        if (plankY < 0.08f) {
          col = shadeRgb(col, 0.52f)
        } else if (plankY < 0.16f) {
          col = shadeRgb(col, 1.14f)
        }
        pixels[y * size + x] = shadeRgb(col, 1f + plankTone)
      }
    }
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    bmp.setPixels(pixels, 0, size, 0, 0, size, size)
    val c = Canvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    for (row in 0 until plankCount) {
      val py = row * plankPx
      val jointX = ((row * 97) % (size - 32)) + 16f
      p.style = Paint.Style.STROKE; p.strokeWidth = 1.3f; p.color = 0xAA241206.toInt()
      c.drawLine(jointX, py + 1.5f, jointX, py + plankPx - 1.5f, p)
      p.style = Paint.Style.FILL; p.color = 0xFF1E2126.toInt()
      c.drawCircle(jointX - 4f, py + plankPx * 0.30f, 1.4f, p)
      c.drawCircle(jointX - 4f, py + plankPx * 0.72f, 1.4f, p)
      c.drawCircle(jointX + 4f, py + plankPx * 0.30f, 1.4f, p)
      c.drawCircle(jointX + 4f, py + plankPx * 0.72f, 1.4f, p)
    }
    return bmp
  }

  private fun buildPlowedSoilBitmap(): Bitmap {
    val size = TEX_SIZE
    val pixels = IntArray(size * size)
    val inv = 1f / size
    for (y in 0 until size) {
      val v = y * inv
      for (x in 0 until size) {
        val u = x * inv
        val n = tileableFbm5(u, v, 613)
        val clod = tileableNoise(u, v, 96, 631)
        val ridge = sin((u * 0.5f + v) * 2f * PI.toFloat() * 16f + n * 1.8f) * 0.5f + 0.5f
        val base = lerpColor(0xFF2B1A0D.toInt(), 0xFF6B4A2A.toInt(), ridge * 0.68f + n * 0.32f)
        val highlight = if (ridge > 0.68f) lerpColor(base, 0xFF8C663F.toInt(), (ridge - 0.68f) * 1.4f) else base
        pixels[y * size + x] = shadeRgb(highlight, 0.90f + clod * 0.20f)
      }
    }
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    bmp.setPixels(pixels, 0, size, 0, 0, size, size)
    val c = Canvas(bmp)
    val rng = Random(613)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    for (i in 0 until 520) {
      val px = rng.nextFloat() * size
      val py = rng.nextFloat() * size
      p.style = Paint.Style.FILL
      p.color =
        when (i % 4) {
          0 -> 0x801E1107.toInt()
          1 -> 0x7084623C
          2 -> 0x4CB09454
          else -> 0x5547351E
        }
      c.drawCircle(px, py, rng.nextFloat() * 1.9f + 0.5f, p)
    }
    p.style = Paint.Style.STROKE
    for (i in 0 until 260) {
      val sx = rng.nextFloat() * size
      val sy = rng.nextFloat() * size
      p.strokeWidth = 0.8f
      p.color = 0x62C9AA5E
      c.drawLine(sx, sy, sx + rng.nextFloat() * 7f - 3.5f, sy + rng.nextFloat() * 4f - 2f, p)
    }
    return bmp
  }

  private fun buildConcreteBitmap(): Bitmap {
    val size = TEX_SIZE
    val pixels = IntArray(size * size)
    val inv = 1f / size
    for (y in 0 until size) {
      val v = y * inv
      for (x in 0 until size) {
        val u = x * inv
        val n = tileableFbm5(u, v, 709)
        val grit = tileableNoise(u, v, 144, 719)
        val oilStain = tileableNoise(u, v, 10, 733)
        var col = lerpColor(0xFF596066.toInt(), 0xFF838A91.toInt(), n * 0.72f + grit * 0.28f)
        if (oilStain < 0.24f) {
          col = lerpColor(col, 0xFF33383D.toInt(), (0.24f - oilStain) * 1.4f)
        }
        pixels[y * size + x] = shadeRgb(col, 0.92f + grit * 0.16f)
      }
    }
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    bmp.setPixels(pixels, 0, size, 0, 0, size, size)
    val c = Canvas(bmp)
    val rng = Random(709)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    // Concrete slab expansion joints & chamfer edges
    val slab = 64
    for (i in 0 until size step slab) {
      p.style = Paint.Style.STROKE
      p.strokeWidth = 2.0f
      p.color = 0xAA252A2E.toInt()
      c.drawLine(i.toFloat(), 0f, i.toFloat(), size.toFloat(), p)
      c.drawLine(0f, i.toFloat(), size.toFloat(), i.toFloat(), p)
      p.strokeWidth = 1.0f
      p.color = 0x38DCE2E8
      c.drawLine(i + 2.2f, 0f, i + 2.2f, size.toFloat(), p)
      c.drawLine(0f, i + 2.2f, size.toFloat(), i + 2.2f, p)
    }
    // Fine tire skid streaks & expansion tar patches
    for (i in 0 until 70) {
      val sx = rng.nextFloat() * size
      val sy = rng.nextFloat() * size
      p.style = Paint.Style.STROKE
      p.strokeWidth = rng.nextFloat() * 1.6f + 0.6f
      p.color = if (i % 3 == 0) 0x361C1E21 else 0x24C4CBD1
      c.drawLine(sx, sy, sx + rng.nextFloat() * 28f - 14f, sy + rng.nextFloat() * 16f - 8f, p)
    }
    return bmp
  }

  private fun buildBrickBitmap(): Bitmap {
    val size = TEX_SIZE
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    c.drawColor(0xFF423C37.toInt())
    val rng = Random(811)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    val rowH = 14
    val brickW = 32
    for (row in 0 until size / rowH + 1) {
      val y = row * rowH
      val offset = if (row % 2 == 0) 0 else brickW / 2
      var x = -offset
      while (x < size) {
        val tone = rng.nextInt(-18, 18)
        val soot = if (rng.nextInt(7) == 0) -22 else 0
        val r = (128 + tone + soot).coerceIn(28, 255)
        val g = (60 + tone / 2 + soot).coerceIn(20, 255)
        val b = (48 + tone / 2 + soot).coerceIn(20, 255)
        p.style = Paint.Style.FILL
        p.color = Color.rgb(r, g, b)
        c.drawRoundRect(x + 1.1f, y + 1.1f, x + brickW - 1.1f, y + rowH - 1.1f, 1.2f, 1.2f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.0f
        p.color = Color.rgb((r + 24).coerceAtMost(255), (g + 16).coerceAtMost(255), (b + 14).coerceAtMost(255))
        c.drawLine(x + 1.8f, y + 1.8f, x + brickW - 1.8f, y + 1.8f, p)
        p.color = 0x7716100E
        c.drawLine(x + 1.5f, y + rowH - 1.4f, x + brickW - 1.5f, y + rowH - 1.4f, p)
        x += brickW
      }
    }
    p.style = Paint.Style.FILL
    for (i in 0 until 240) {
      p.color = if (i % 2 == 0) 0x26141210 else 0x1CCAD0D6
      c.drawCircle(rng.nextFloat() * size, rng.nextFloat() * size, rng.nextFloat() * 3.0f + 0.8f, p)
    }
    return bmp
  }
}
