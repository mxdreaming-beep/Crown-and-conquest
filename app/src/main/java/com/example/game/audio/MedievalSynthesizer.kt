package com.example.game.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

enum class GameSfx {
  UI_CLICK,
  SELECT_UNIT,
  SELECT_BUILDING,
  ORDER_MOVE,
  ORDER_GATHER,
  ATTACK_HORN,
  WOOD_CHOP,
  MINE_PICK,
  BUILD_HAMMER,
  BUILD_COMPLETE,
  UNIT_READY,
  AGE_ADVANCE,
  SWORD_CLASH,
  ARROW_TWANG,
  GUNSHOT,
  MG_BURST,
  SNIPER_SHOT,
  CANNON_FIRE,
  ROCKET_LAUNCH,
  EXPLOSION,
  MOVE_BUILDING,
  ERROR_THUD,
  VICTORY_FANFARE,
  DEFEAT_HORN,
}

/**
 * Pure Kotlin procedural medieval music & battlefield SFX synthesizer.
 * Generates 16-bit PCM audio in memory with zero binary asset bloat or external dependencies.
 */
object MedievalSynthesizer {

  const val MUSIC_SAMPLE_RATE = 22050
  const val MUSIC_DURATION_SEC = 20
  const val SFX_SAMPLE_RATE = 44100

  private data class NoteEvent(
    val startBeat: Float,
    val durBeats: Float,
    val freqHz: Float,
    val gain: Float,
  )

  private fun midiToHz(midi: Int): Float = 440f * 2f.pow((midi - 69) / 12f)

  /**
   * Synthesizes a seamless 20-second stereo D-Dorian medieval theme (8 bars @ 96 BPM)
   * combining Karplus-Strong lute/harp arpeggios, bowed viol/cello fifth drone,
   * breathy wooden recorder melody, tabor frame drum, and cathedral stereo reverb.
   */
  fun generateMedievalThemePcm(
    sampleRate: Int = MUSIC_SAMPLE_RATE,
    durationSec: Int = MUSIC_DURATION_SEC,
  ): ShortArray {
    val totalFrames = sampleRate * durationSec
    val left = FloatArray(totalFrames)
    val right = FloatArray(totalFrames)
    val rng = Random(1453)

    val beatSec = durationSec.toFloat() / 32f // 8 bars * 4 beats = 32 beats (96 BPM)
    val twoPi = (2.0 * PI).toFloat()

    // 1. Warm Bowed Viol / Hurdy-Gurdy Fifth Drone (D2 = 38, A2 = 45, D3 = 50) with seamless phase
    val d2Cycles = kotlin.math.round(73.42f * durationSec).toInt().coerceAtLeast(1)
    val a2Cycles = kotlin.math.round(110.00f * durationSec).toInt().coerceAtLeast(1)
    val fD2 = d2Cycles.toFloat() / durationSec
    val fA2 = a2Cycles.toFloat() / durationSec

    for (i in 0 until totalFrames) {
      val t = i.toFloat() / sampleRate
      val swell = 0.85f + 0.15f * sin(twoPi * t / 5.0f)
      val phD = twoPi * fD2 * t
      val phA = twoPi * fA2 * t
      val bowD = sin(phD) * 0.65f + sin(phD * 2f) * 0.25f + sin(phD * 3f) * 0.10f
      val bowA = sin(phA) * 0.60f + sin(phA * 2f) * 0.22f
      val drone = (bowD * 0.15f + bowA * 0.10f) * swell
      left[i] += drone * 0.92f
      right[i] += drone * 1.04f
    }

    // 2. Karplus-Strong Plucked Lute / Celtic Harp Accompaniment (wrapping overhang for seamless loop)
    fun pluckString(startSec: Float, freqHz: Float, durSec: Float, amp: Float, panL: Float, panR: Float) {
      val period = (sampleRate / freqHz).toInt().coerceIn(8, 2000)
      val exc = FloatArray(period)
      var prevNoise = 0f
      for (k in 0 until period) {
        val raw = rng.nextFloat() * 2f - 1f
        prevNoise = prevNoise * 0.45f + raw * 0.55f
        exc[k] = prevNoise
      }
      val len = (durSec * sampleRate).toInt()
      val startIdx = ((startSec * sampleRate).toInt()).mod(totalFrames)
      var idx = 0
      for (s in 0 until len) {
        val next = (idx + 1) % period
        val y = exc[idx]
        exc[idx] = (y + exc[next]) * 0.4955f
        idx = next
        val env = if (s < 48) s / 48f else (1f - s.toFloat() / len).pow(0.45f)
        val sample = y * amp * env
        val dst = (startIdx + s) % totalFrames
        left[dst] += sample * panL
        right[dst] += sample * panR
      }
    }

    // D-Dorian 8-bar progression: Dm - C - Bb/F - C | Dm - Am - C - Dm
    val chords = listOf(
      intArrayOf(50, 57, 62, 65), // Dm
      intArrayOf(48, 55, 60, 64), // C
      intArrayOf(46, 53, 58, 62), // Bb
      intArrayOf(48, 55, 60, 64), // C
      intArrayOf(50, 57, 62, 65), // Dm
      intArrayOf(45, 52, 57, 60), // Am
      intArrayOf(48, 55, 60, 64), // C
      intArrayOf(50, 57, 62, 65), // Dm
    )

    for (bar in chords.indices) {
      val c = chords[bar]
      val b0 = bar * 4f
      val pattern = intArrayOf(0, 1, 2, 3, 2, 1, 2, 3)
      for (step in pattern.indices) {
        val beat = b0 + step * 0.5f
        val midi = c[pattern[step]]
        val freq = midiToHz(midi)
        val isDownbeat = step == 0 || step == 4
        val gain = if (isDownbeat) 0.25f else 0.17f
        val panL = if (step % 2 == 0) 0.85f else 0.62f
        val panR = if (step % 2 == 0) 0.62f else 0.85f
        pluckString(beat * beatSec, freq, durSec = if (isDownbeat) 1.55f else 1.05f, amp = gain, panL = panL, panR = panR)
      }
    }

    // 3. Wooden Recorder / Medieval Flute Melody (D-Dorian)
    val melody = listOf(
      NoteEvent(0.0f, 1.5f, midiToHz(62), 0.23f), // D4
      NoteEvent(1.5f, 0.5f, midiToHz(64), 0.20f), // E4
      NoteEvent(2.0f, 1.5f, midiToHz(65), 0.24f), // F4
      NoteEvent(3.5f, 0.5f, midiToHz(67), 0.21f), // G4

      NoteEvent(4.0f, 2.0f, midiToHz(69), 0.26f), // A4
      NoteEvent(6.0f, 1.0f, midiToHz(67), 0.22f), // G4
      NoteEvent(7.0f, 1.0f, midiToHz(64), 0.20f), // E4

      NoteEvent(8.0f, 1.5f, midiToHz(65), 0.24f), // F4
      NoteEvent(9.5f, 0.5f, midiToHz(64), 0.20f), // E4
      NoteEvent(10.0f, 2.0f, midiToHz(62), 0.24f), // D4

      NoteEvent(12.0f, 1.5f, midiToHz(60), 0.22f), // C4
      NoteEvent(13.5f, 0.5f, midiToHz(62), 0.20f), // D4
      NoteEvent(14.0f, 2.0f, midiToHz(64), 0.23f), // E4

      NoteEvent(16.0f, 1.5f, midiToHz(69), 0.25f), // A4
      NoteEvent(17.5f, 0.5f, midiToHz(71), 0.24f), // B4 (Dorian 6th!)
      NoteEvent(18.0f, 1.5f, midiToHz(72), 0.26f), // C5
      NoteEvent(19.5f, 0.5f, midiToHz(71), 0.22f), // B4

      NoteEvent(20.0f, 1.5f, midiToHz(69), 0.25f), // A4
      NoteEvent(21.5f, 0.5f, midiToHz(67), 0.22f), // G4
      NoteEvent(22.0f, 2.0f, midiToHz(65), 0.23f), // F4

      NoteEvent(24.0f, 1.0f, midiToHz(67), 0.23f), // G4
      NoteEvent(25.0f, 1.0f, midiToHz(65), 0.21f), // F4
      NoteEvent(26.0f, 1.0f, midiToHz(64), 0.21f), // E4
      NoteEvent(27.0f, 1.0f, midiToHz(60), 0.20f), // C4

      NoteEvent(28.0f, 3.5f, midiToHz(62), 0.25f), // D4 sustained cadence
    )

    for (note in melody) {
      val startIdx = (note.startBeat * beatSec * sampleRate).toInt()
      val len = (note.durBeats * beatSec * sampleRate).toInt().coerceAtLeast(1)
      var phase = 0f
      for (s in 0 until len) {
        val u = s.toFloat() / len
        val tNote = s.toFloat() / sampleRate
        val vib = if (tNote > 0.14f) sin(twoPi * 5.2f * tNote) * 0.0055f else 0f
        phase += twoPi * note.freqHz * (1f + vib) / sampleRate
        val env =
          when {
            tNote < 0.055f -> tNote / 0.055f
            u > 0.80f -> ((1f - u) / 0.20f).coerceAtLeast(0f)
            else -> 1f - 0.12f * u
          }
        val breath = (rng.nextFloat() * 2f - 1f) * 0.032f * env
        val tone = sin(phase) * 0.78f + sin(phase * 2f) * 0.19f + sin(phase * 3f) * 0.05f + breath
        val out = tone * note.gain * env
        val dst = (startIdx + s) % totalFrames
        left[dst] += out * 0.72f
        right[dst] += out * 0.88f
      }
    }

    // 4. Tactile Tabor Frame Drum + Subtle Brass Tambourine Shimmer
    for (bar in 0 until 8) {
      for (beatOffset in floatArrayOf(0f, 2.0f, 3.5f)) {
        val accent = if (beatOffset == 0f) 1.0f else if (beatOffset == 2.0f) 0.74f else 0.46f
        val startIdx = ((bar * 4f + beatOffset) * beatSec * sampleRate).toInt()
        val drumLen = (0.26f * sampleRate).toInt()
        var ph = 0f
        for (s in 0 until drumLen) {
          val t = s.toFloat() / sampleRate
          val freq = 56f + 68f * exp(-t * 26f)
          ph += twoPi * freq / sampleRate
          val env = (if (s < 42) s / 42f else exp(-t * 13.5f))
          val skinOver = sin(ph * 1.58f) * 0.16f * exp(-t * 24f)
          val snare = (rng.nextFloat() * 2f - 1f) * 0.14f * exp(-t * 48f)
          val jingle = if (beatOffset == 2.0f) {
            (sin(twoPi * 6450f * t) + sin(twoPi * 7820f * t)) * 0.032f * exp(-t * 30f)
          } else 0f
          val thump = (sin(ph) * 0.84f + skinOver + snare) * 0.18f * accent * env + jingle
          val dst = (startIdx + s) % totalFrames
          left[dst] += thump * (if (beatOffset == 2.0f) 0.88f else 1.0f)
          right[dst] += thump * (if (beatOffset == 2.0f) 1.08f else 1.0f)
        }
      }
    }

    // 5. Seamless Stereo Cathedral Hall Delay Reverb
    val d1 = (0.073f * sampleRate).toInt()
    val d2 = (0.119f * sampleRate).toInt()
    val revL = FloatArray(totalFrames)
    val revR = FloatArray(totalFrames)
    for (i in 0 until totalFrames) {
      val src1 = (i - d1 + totalFrames) % totalFrames
      val src2 = (i - d2 + totalFrames) % totalFrames
      revL[i] = left[i] + right[src1] * 0.24f + left[src2] * 0.14f
      revR[i] = right[i] + left[src1] * 0.24f + right[src2] * 0.14f
    }

    // Peak normalization with warm soft-saturation to ShortArray interleaved stereo
    var peak = 0.001f
    for (i in 0 until totalFrames) {
      val aL = abs(revL[i])
      val aR = abs(revR[i])
      if (aL > peak) peak = aL
      if (aR > peak) peak = aR
    }
    val norm = 0.84f / peak
    val pcm = ShortArray(totalFrames * 2)
    for (i in 0 until totalFrames) {
      val l = tanh((revL[i] * norm).toDouble()).toFloat()
      val r = tanh((revR[i] * norm).toDouble()).toFloat()
      pcm[i * 2] = (l * 32000f).toInt().coerceIn(-32767, 32767).toShort()
      pcm[i * 2 + 1] = (r * 32000f).toInt().coerceIn(-32767, 32767).toShort()
    }
    return pcm
  }

  /**
   * Generates a complete RIFF WAVE (16-bit PCM mono) byte payload for a game SFX event.
   */
  fun generateSfxWav(sfx: GameSfx, sampleRate: Int = SFX_SAMPLE_RATE): ByteArray {
    val samples = generateSfxSamples(sfx, sampleRate)
    return encodeWav16Mono(samples, sampleRate)
  }

  fun generateSfxSamples(sfx: GameSfx, sampleRate: Int = SFX_SAMPLE_RATE): ShortArray {
    val twoPi = (2.0 * PI).toFloat()
    val rng = Random(sfx.ordinal * 997 + 17)

    fun makeFloatBuffer(durSec: Float, fn: (i: Int, t: Float, u: Float) -> Float): FloatArray {
      val n = (durSec * sampleRate).toInt().coerceAtLeast(1)
      val buf = FloatArray(n)
      for (i in 0 until n) {
        val t = i.toFloat() / sampleRate
        val u = i.toFloat() / n
        buf[i] = fn(i, t, u)
      }
      return buf
    }

    fun biquadLowPassInPlace(buf: FloatArray, cutoffHz: Float, q: Float = 0.707f) {
      val w0 = twoPi * cutoffHz.coerceIn(40f, sampleRate * 0.45f) / sampleRate
      val cosW0 = kotlin.math.cos(w0.toDouble()).toFloat()
      val sinW0 = kotlin.math.sin(w0.toDouble()).toFloat()
      val alpha = sinW0 / (2f * q.coerceAtLeast(0.3f))
      val b0 = (1f - cosW0) * 0.5f
      val b1 = 1f - cosW0
      val b2 = (1f - cosW0) * 0.5f
      val a0 = 1f + alpha
      val a1 = -2f * cosW0
      val a2 = 1f - alpha
      var x1 = 0f
      var x2 = 0f
      var y1 = 0f
      var y2 = 0f
      for (i in buf.indices) {
        val x0 = buf[i]
        val y0 = (b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2) / a0
        x2 = x1
        x1 = x0
        y2 = y1
        y1 = y0
        buf[i] = y0
      }
    }

    fun biquadHighPassInPlace(buf: FloatArray, cutoffHz: Float, q: Float = 0.707f) {
      val w0 = twoPi * cutoffHz.coerceIn(20f, sampleRate * 0.42f) / sampleRate
      val cosW0 = kotlin.math.cos(w0.toDouble()).toFloat()
      val sinW0 = kotlin.math.sin(w0.toDouble()).toFloat()
      val alpha = sinW0 / (2f * q.coerceAtLeast(0.3f))
      val b0 = (1f + cosW0) * 0.5f
      val b1 = -(1f + cosW0)
      val b2 = (1f + cosW0) * 0.5f
      val a0 = 1f + alpha
      val a1 = -2f * cosW0
      val a2 = 1f - alpha
      var x1 = 0f
      var x2 = 0f
      var y1 = 0f
      var y2 = 0f
      for (i in buf.indices) {
        val x0 = buf[i]
        val y0 = (b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2) / a0
        x2 = x1
        x1 = x0
        y2 = y1
        y1 = y0
        buf[i] = y0
      }
    }

    fun bandPassInPlace(buf: FloatArray, lowCutHz: Float, highCutHz: Float, q: Float = 0.85f) {
      biquadLowPassInPlace(buf, highCutHz, q)
      biquadHighPassInPlace(buf, lowCutHz, 0.707f)
    }

    fun applyOutdoorEcho(buf: FloatArray, tapsSec: FloatArray, gains: FloatArray): FloatArray {
      val copy = buf.copyOf()
      for (k in tapsSec.indices) {
        val delaySamples = (tapsSec[k] * sampleRate).toInt().coerceAtLeast(1)
        val g = gains[k]
        var damp = 0f
        for (i in delaySamples until buf.size) {
          // Slightly air-absorbed reflection for outdoor realism
          damp = damp * 0.25f + copy[i - delaySamples] * 0.75f
          buf[i] += damp * g
        }
      }
      return buf
    }

    fun finalizePcm(buf: FloatArray, gain: Float = 0.94f): ShortArray {
      var peak = 0.001f
      for (v in buf) {
        val a = abs(v)
        if (a > peak) peak = a
      }
      val scale = if (peak > 1.25f) 1.25f / peak else 1.0f
      val out = ShortArray(buf.size)
      for (i in buf.indices) {
        val shaped = tanh((buf[i] * scale * gain).toDouble()).toFloat()
        out[i] = (shaped * 31200f).toInt().coerceIn(-32767, 32767).toShort()
      }
      return out
    }

    fun makeFilteredNoise(
      durSec: Float,
      lowHz: Float,
      highHz: Float,
      q: Float = 0.85f,
      decayFn: (t: Float) -> Float,
    ): FloatArray {
      val buf = makeFloatBuffer(durSec) { _, t, _ -> (rng.nextFloat() * 2f - 1f) * decayFn(t) }
      bandPassInPlace(buf, lowHz, highHz, q)
      return buf
    }

    return when (sfx) {
      GameSfx.UI_CLICK -> {
        val buf = makeFloatBuffer(0.045f) { _, t, u ->
          val wood = sin(twoPi * (910f + 460f * exp(-t * 150f)) * t) * exp(-t * 95f) * 0.52f
          val latch = sin(twoPi * 2280f * t) * exp(-t * 165f) * 0.34f
          val click = (rng.nextFloat() * 2f - 1f) * exp(-t * 185f) * 0.22f
          (wood + latch + click) * (1f - u * 0.55f)
        }
        finalizePcm(buf)
      }

      GameSfx.SELECT_UNIT -> {
        val brush = makeFilteredNoise(0.14f, 1200f, 5200f) { t -> exp(-t * 68f) * 0.34f }
        val buf = makeFloatBuffer(0.14f) { i, t, _ ->
          val ring = (sin(twoPi * 587.33f * t) * 0.48f + sin(twoPi * 1174.66f * t) * 0.24f) * exp(-t * 30f)
          val leather = sin(twoPi * 172f * t) * exp(-t * 48f) * 0.36f
          brush[i] + ring + leather
        }
        finalizePcm(buf)
      }

      GameSfx.SELECT_BUILDING -> {
        val stoneNoise = makeFilteredNoise(0.17f, 120f, 1400f) { t -> exp(-t * 40f) * 0.32f }
        val buf = makeFloatBuffer(0.17f) { i, t, _ ->
          val stone = sin(twoPi * (142f * exp(-t * 18f) + 88f) * t) * exp(-t * 22f) * 0.56f
          val fifth = (sin(twoPi * 293.66f * t) * 0.40f + sin(twoPi * 440f * t) * 0.32f) * exp(-t * 22f)
          stoneNoise[i] + stone + fifth
        }
        applyOutdoorEcho(buf, floatArrayOf(0.036f), floatArrayOf(0.24f))
        finalizePcm(buf)
      }

      GameSfx.ORDER_MOVE -> {
        val turf = makeFilteredNoise(0.16f, 220f, 2600f) { t ->
          val g1 = exp(-t * 72f)
          val g2 = if (t > 0.058f) exp(-(t - 0.058f) * 72f) * 0.85f else 0f
          g1 + g2
        }
        val buf = makeFloatBuffer(0.16f) { i, t, _ ->
          val boot1 = sin(twoPi * (138f * exp(-t * 38f) + 60f) * t) * exp(-t * 58f) * 0.56f
          val t2 = t - 0.058f
          val boot2 = if (t2 > 0f) sin(twoPi * (160f * exp(-t2 * 38f) + 68f) * t2) * exp(-t2 * 64f) * 0.48f else 0f
          boot1 + boot2 + turf[i] * 0.45f
        }
        biquadLowPassInPlace(buf, 3100f)
        finalizePcm(buf)
      }

      GameSfx.ORDER_GATHER -> {
        val satchel = makeFilteredNoise(0.16f, 900f, 4400f) { t -> exp(-t * 48f) * 0.25f }
        val buf = makeFloatBuffer(0.16f) { i, t, _ ->
          val n1 = sin(twoPi * 523.25f * t) * exp(-t * 32f) * 0.48f
          val n2 = if (t > 0.046f) sin(twoPi * 659.25f * (t - 0.046f)) * exp(-(t - 0.046f) * 26f) * 0.58f else 0f
          n1 + n2 + satchel[i]
        }
        finalizePcm(buf)
      }

      GameSfx.ATTACK_HORN -> {
        val buf = makeFloatBuffer(0.48f) { _, t, u ->
          val freq = if (t < 0.14f) 220f else 293.66f
          val attack = (t / 0.032f).coerceAtMost(1f)
          val env = attack * (1f - u).pow(0.52f)
          val vib = 1f + 0.0085f * sin(twoPi * 5.6f * t)
          val ph = twoPi * freq * vib * t
          val brass =
            sin(ph) * 0.50f +
              sin(ph * 2f) * 0.35f +
              sin(ph * 3f) * 0.23f +
              sin(ph * 4f) * 0.14f +
              sin(ph * 5f) * 0.08f
          val air = (rng.nextFloat() * 2f - 1f) * 0.055f * attack
          (brass + air) * env * 0.88f
        }
        biquadLowPassInPlace(buf, 3600f)
        applyOutdoorEcho(buf, floatArrayOf(0.088f, 0.178f), floatArrayOf(0.32f, 0.16f))
        finalizePcm(buf)
      }

      GameSfx.WOOD_CHOP -> {
        val chipNoise = makeFilteredNoise(0.18f, 420f, 4400f, q = 1.1f) { t -> exp(-t * 68f) * 0.90f }
        val buf = makeFloatBuffer(0.18f) { i, t, _ ->
          val axeThud = sin(twoPi * (175f * exp(-t * 32f) + 56f) * t) * exp(-t * 34f) * 0.82f
          val trunkModal =
            (sin(twoPi * 295f * t) * 0.42f +
              sin(twoPi * 472f * t) * 0.26f +
              sin(twoPi * 760f * t) * 0.14f) * exp(-t * 48f)
          val steelBit = sin(twoPi * 2450f * t) * exp(-t * 95f) * 0.18f
          axeThud + trunkModal + steelBit + chipNoise[i]
        }
        applyOutdoorEcho(buf, floatArrayOf(0.044f, 0.095f), floatArrayOf(0.24f, 0.10f))
        finalizePcm(buf)
      }

      GameSfx.MINE_PICK -> {
        val shatter = makeFilteredNoise(0.22f, 1100f, 7200f, q = 1.0f) { t -> exp(-t * 36f) * 0.62f }
        val buf = makeFloatBuffer(0.22f) { i, t, _ ->
          val modes =
            (sin(twoPi * 1745f * t) * 0.44f +
              sin(twoPi * 2730f * t) * 0.34f +
              sin(twoPi * 3915f * t) * 0.28f +
              sin(twoPi * 5480f * t) * 0.18f +
              sin(twoPi * 7120f * t) * 0.10f) * exp(-t * 18f)
          val rockImpact = sin(twoPi * (230f * exp(-t * 28f) + 95f) * t) * exp(-t * 44f) * 0.44f
          modes + rockImpact + shatter[i]
        }
        applyOutdoorEcho(buf, floatArrayOf(0.052f, 0.112f), floatArrayOf(0.25f, 0.11f))
        finalizePcm(buf)
      }

      GameSfx.BUILD_HAMMER -> {
        val nailNoise = makeFilteredNoise(0.21f, 620f, 4500f) { t ->
          val h1 = exp(-t * 78f)
          val h2 = if (t > 0.084f) exp(-(t - 0.084f) * 80f) * 0.85f else 0f
          h1 + h2
        }
        val buf = makeFloatBuffer(0.21f) { i, t, _ ->
          val w1 =
            (sin(twoPi * (290f * exp(-t * 26f) + 92f) * t) * 0.72f +
              sin(twoPi * 640f * t) * 0.24f) * exp(-t * 46f)
          val t2 = t - 0.084f
          val w2 =
            if (t2 > 0f) {
              (sin(twoPi * (340f * exp(-t2 * 26f) + 110f) * t2) * 0.64f +
                sin(twoPi * 720f * t2) * 0.22f) * exp(-t2 * 50f)
            } else 0f
          w1 + w2 + nailNoise[i] * 0.74f
        }
        applyOutdoorEcho(buf, floatArrayOf(0.040f), floatArrayOf(0.18f))
        finalizePcm(buf)
      }

      GameSfx.BUILD_COMPLETE -> {
        val buf = makeFloatBuffer(0.50f) { _, t, _ ->
          val f1 = (sin(twoPi * 587.33f * t) * 0.65f + sin(twoPi * 1174.66f * t) * 0.22f) * exp(-t * 6.2f)
          val f2 = if (t > 0.085f) (sin(twoPi * 739.99f * (t - 0.085f)) * 0.65f + sin(twoPi * 1479.98f * (t - 0.085f)) * 0.20f) * exp(-(t - 0.085f) * 5.8f) else 0f
          val f3 = if (t > 0.17f) (sin(twoPi * 880f * (t - 0.17f)) * 0.75f + sin(twoPi * 1760f * (t - 0.17f)) * 0.22f) * exp(-(t - 0.17f) * 5.0f) else 0f
          (f1 + f2 + f3) * 0.56f
        }
        applyOutdoorEcho(buf, floatArrayOf(0.068f), floatArrayOf(0.28f))
        finalizePcm(buf)
      }

      GameSfx.UNIT_READY -> {
        val buf = makeFloatBuffer(0.32f) { _, t, _ ->
          val drum = sin(twoPi * (140f * exp(-t * 26f) + 75f) * t) * exp(-t * 24f) * 0.55f
          val n1 = sin(twoPi * 440f * t) * exp(-t * 10f) * 0.45f
          val n2 = if (t > 0.09f) sin(twoPi * 587.33f * (t - 0.09f)) * exp(-(t - 0.09f) * 7.5f) * 0.58f else 0f
          drum + n1 + n2
        }
        finalizePcm(buf)
      }

      GameSfx.AGE_ADVANCE -> {
        val buf = makeFloatBuffer(0.88f) { _, t, u ->
          val notes = floatArrayOf(293.66f, 440.00f, 587.33f, 739.99f)
          var sum = 0f
          for (idx in notes.indices) {
            val start = idx * 0.11f
            if (t >= start) {
              val tt = t - start
              val env = (if (tt < 0.02f) tt / 0.02f else exp(-tt * 3.1f)) * (1f - u * 0.28f)
              val ph = twoPi * notes[idx] * tt
              sum += (sin(ph) * 0.58f + sin(ph * 2f) * 0.28f + sin(ph * 3f) * 0.12f) * env
            }
          }
          val timpani = sin(twoPi * (112f * exp(-t * 14f) + 56f) * t) * exp(-t * 6.8f) * 0.52f
          sum * 0.45f + timpani
        }
        applyOutdoorEcho(buf, floatArrayOf(0.072f, 0.155f), floatArrayOf(0.30f, 0.16f))
        finalizePcm(buf)
      }

      GameSfx.SWORD_CLASH -> {
        val scrape = makeFilteredNoise(0.26f, 1600f, 8800f, q = 1.2f) { t -> exp(-t * 40f) * 0.72f }
        val buf = makeFloatBuffer(0.26f) { i, t, _ ->
          val ring =
            (sin(twoPi * 1420f * t) * 0.36f +
              sin(twoPi * 2290f * t) * 0.32f +
              sin(twoPi * 2348f * t) * 0.28f +
              sin(twoPi * 3690f * t) * 0.24f +
              sin(twoPi * 5140f * t) * 0.18f +
              sin(twoPi * 6820f * t) * 0.11f) * exp(-t * 15.5f)
          val impact = sin(twoPi * (195f * exp(-t * 32f) + 84f) * t) * exp(-t * 54f) * 0.44f
          ring + impact + scrape[i]
        }
        applyOutdoorEcho(buf, floatArrayOf(0.048f), floatArrayOf(0.25f))
        finalizePcm(buf)
      }

      GameSfx.ARROW_TWANG -> {
        val airWhoosh = makeFilteredNoise(0.20f, 1100f, 5400f, q = 1.35f) { t -> exp(-abs(t - 0.042f) * 56f) * 0.78f }
        val buf = makeFloatBuffer(0.20f) { i, t, _ ->
          val bowFreq = 152f + 122f * exp(-t * 36f)
          val stringTwang = (sin(twoPi * bowFreq * t) * 0.54f + sin(twoPi * bowFreq * 2.02f * t) * 0.22f) * exp(-t * 24f)
          val bowLimb = sin(twoPi * 94f * t) * exp(-t * 48f) * 0.34f
          stringTwang + bowLimb + airWhoosh[i]
        }
        finalizePcm(buf)
      }

      GameSfx.GUNSHOT -> {
        // Ballistic N-wave crack + dense chamber blast + brass receiver cycle + open field rolloff
        val muzzleCrack = makeFilteredNoise(0.26f, 450f, 8200f, q = 0.95f) { t -> exp(-t * 32f) * 1.25f }
        val tail = makeFilteredNoise(0.26f, 120f, 2200f, q = 0.75f) { t -> exp(-t * 11.5f) * 0.58f }
        val buf = makeFloatBuffer(0.26f) { i, t, _ ->
          // Supersonic shockwave front
          val shock = if (t < 0.0045f) (1f - t / 0.0045f) * 0.85f else 0f
          val chestPunch = sin(twoPi * (235f * exp(-t * 52f) + 52f) * t) * exp(-t * 30f) * 0.88f
          val boltCycle =
            if (t in 0.048f..0.090f) {
              sin(twoPi * 2650f * t) * exp(-(t - 0.048f) * 95f) * 0.22f
            } else 0f
          shock + muzzleCrack[i] + tail[i] + chestPunch + boltCycle
        }
        applyOutdoorEcho(buf, floatArrayOf(0.062f, 0.130f), floatArrayOf(0.28f, 0.14f))
        finalizePcm(buf)
      }

      GameSfx.MG_BURST -> {
        val bursts = floatArrayOf(0f, 0.066f, 0.132f)
        val raw = FloatArray((0.31f * sampleRate).toInt())
        val crackNoise = makeFilteredNoise(0.31f, 520f, 8400f, q = 0.9f) { _ -> 1.0f }
        for (i in raw.indices) {
          val t = i.toFloat() / sampleRate
          var sum = 0f
          for (bStart in bursts) {
            if (t >= bStart) {
              val tt = t - bStart
              val shock = if (tt < 0.0032f) (1f - tt / 0.0032f) * 0.65f else 0f
              val env = exp(-tt * 46f)
              val snap = crackNoise[i] * env * 1.15f
              val body = sin(twoPi * (245f * exp(-tt * 58f) + 64f) * tt) * env * 0.76f
              val casing = sin(twoPi * 4120f * tt) * exp(-tt * 78f) * 0.20f
              sum += shock + snap + body + casing
            }
          }
          raw[i] = sum
        }
        applyOutdoorEcho(raw, floatArrayOf(0.052f, 0.105f), floatArrayOf(0.24f, 0.10f))
        finalizePcm(raw)
      }

      GameSfx.SNIPER_SHOT -> {
        // Razor-sharp .50-cal supersonic whip-crack + heavy chamber recoil + wide valley slapback
        val crack = makeFilteredNoise(0.42f, 950f, 10500f, q = 1.0f) { t -> exp(-t * 36f) * 1.45f }
        val boom = makeFilteredNoise(0.42f, 65f, 1450f, q = 0.75f) { t -> exp(-t * 10f) * 0.75f }
        val buf = makeFloatBuffer(0.42f) { i, t, _ ->
          val shock = if (t < 0.004f) (1f - t / 0.004f) * 1.05f else 0f
          val kick = sin(twoPi * (280f * exp(-t * 46f) + 48f) * t) * exp(-t * 22f) * 0.96f
          val muzzleRing = sin(twoPi * 2180f * t) * exp(-t * 42f) * 0.24f
          shock + crack[i] + boom[i] + kick + muzzleRing
        }
        applyOutdoorEcho(buf, floatArrayOf(0.082f, 0.172f, 0.265f), floatArrayOf(0.34f, 0.18f, 0.09f))
        finalizePcm(buf)
      }

      GameSfx.CANNON_FIRE -> {
        // Heavy field cannon & MBT turret blast: seismic low-end thump + dense pressure roar + steel breech ring
        val blast = makeFilteredNoise(0.52f, 42f, 2400f, q = 0.8f) { t -> exp(-t * 9.8f) * 1.35f }
        val airTear = makeFilteredNoise(0.52f, 1100f, 5200f) { t -> exp(-t * 28f) * 0.55f }
        val buf = makeFloatBuffer(0.52f) { i, t, _ ->
          val shock = if (t < 0.006f) (1f - t / 0.006f) * 0.90f else 0f
          val subBoom = sin(twoPi * (145f * exp(-t * 20f) + 34f) * t) * exp(-t * 8.5f) * 1.12f
          val breech = (sin(twoPi * 485f * t) * 0.26f + sin(twoPi * 930f * t) * 0.14f) * exp(-t * 28f)
          shock + blast[i] + airTear[i] + subBoom + breech
        }
        applyOutdoorEcho(buf, floatArrayOf(0.095f, 0.205f, 0.32f), floatArrayOf(0.32f, 0.18f, 0.08f))
        finalizePcm(buf)
      }

      GameSfx.ROCKET_LAUNCH -> {
        // Booster ignition thump + turbulent supersonic thruster roar + fin flutter
        val exhaust = makeFilteredNoise(0.40f, 520f, 6400f, q = 1.15f) { t ->
          val attack = (t / 0.020f).coerceAtMost(1f)
          attack * exp(-t * 6.8f) * 1.22f
        }
        val rumble = makeFilteredNoise(0.40f, 70f, 650f) { t -> exp(-t * 9f) * 0.72f }
        val buf = makeFloatBuffer(0.40f) { i, t, _ ->
          val pop = sin(twoPi * (210f * exp(-t * 36f) + 72f) * t) * exp(-t * 26f) * 0.74f
          val hissMod = sin(twoPi * (340f + t * 960f) * t) * exp(-t * 8.5f) * 0.24f
          exhaust[i] + rumble[i] + pop + hissMod
        }
        applyOutdoorEcho(buf, floatArrayOf(0.065f, 0.140f), floatArrayOf(0.24f, 0.11f))
        finalizePcm(buf)
      }

      GameSfx.EXPLOSION -> {
        val shockwave = makeFilteredNoise(0.64f, 38f, 2600f, q = 0.75f) { t -> exp(-t * 6.0f) * 1.40f }
        val debris = makeFilteredNoise(0.64f, 800f, 7200f) { t ->
          val burst = exp(-t * 13f)
          val secondaryCrackle = if (t in 0.14f..0.44f && (t * 42f).toInt() % 3 == 0) 0.38f else 0f
          (burst + secondaryCrackle) * 0.56f
        }
        val buf = makeFloatBuffer(0.64f) { i, t, _ ->
          val transient = if (t < 0.005f) (1f - t / 0.005f) * 0.95f else 0f
          val subBoom = sin(twoPi * (112f * exp(-t * 12f) + 28f) * t) * exp(-t * 4.8f) * 1.10f
          transient + shockwave[i] + debris[i] + subBoom
        }
        applyOutdoorEcho(buf, floatArrayOf(0.105f, 0.220f, 0.340f), floatArrayOf(0.34f, 0.19f, 0.09f))
        finalizePcm(buf)
      }

      GameSfx.MOVE_BUILDING -> {
        val dirt = makeFilteredNoise(0.24f, 160f, 2400f) { t -> exp(-t * 28f) * 0.65f }
        val buf = makeFloatBuffer(0.24f) { i, t, _ ->
          val hoist = sin(twoPi * (135f * exp(-t * 20f) + 60f) * t) * exp(-t * 22f) * 0.74f
          val chime = if (t > 0.06f) sin(twoPi * 659.25f * (t - 0.06f)) * exp(-(t - 0.06f) * 24f) * 0.42f else 0f
          dirt[i] + hoist + chime
        }
        finalizePcm(buf)
      }

      GameSfx.ERROR_THUD -> {
        val buf = makeFloatBuffer(0.15f) { _, t, _ ->
          val env = exp(-t * 28f)
          (sin(twoPi * 125f * t) * 0.65f + sin(twoPi * 188f * t) * 0.30f) * env * 0.75f
        }
        finalizePcm(buf)
      }

      GameSfx.VICTORY_FANFARE -> {
        val buf = makeFloatBuffer(0.95f) { _, t, u ->
          val chord = floatArrayOf(293.66f, 369.99f, 440.00f, 587.33f)
          var sum = 0f
          for (idx in chord.indices) {
            val start = idx * 0.13f
            if (t >= start) {
              val tt = t - start
              val env = (if (tt < 0.025f) tt / 0.025f else exp(-tt * 2.5f)) * (1f - u * 0.25f)
              val ph = twoPi * chord[idx] * tt
              sum += (sin(ph) * 0.6f + sin(ph * 2f) * 0.26f + sin(ph * 3f) * 0.12f) * env
            }
          }
          sum * 0.44f
        }
        applyOutdoorEcho(buf, floatArrayOf(0.08f, 0.16f), floatArrayOf(0.28f, 0.14f))
        finalizePcm(buf)
      }

      GameSfx.DEFEAT_HORN -> {
        val buf = makeFloatBuffer(0.85f) { _, t, u ->
          val notes = floatArrayOf(293.66f, 261.63f, 233.08f, 220.00f)
          val idx = (t / 0.21f).toInt().coerceIn(0, notes.lastIndex)
          val tt = t - idx * 0.21f
          val env = (if (tt < 0.03f) tt / 0.03f else exp(-tt * 3.0f)) * (1f - u * 0.35f)
          val ph = twoPi * notes[idx] * t
          (sin(ph) * 0.65f + sin(ph * 2f) * 0.24f) * env * 0.65f
        }
        applyOutdoorEcho(buf, floatArrayOf(0.09f), floatArrayOf(0.25f))
        finalizePcm(buf)
      }
    }
  }

  fun encodeWav16Mono(samples: ShortArray, sampleRate: Int = SFX_SAMPLE_RATE): ByteArray {
    val dataSize = samples.size * 2
    val totalSize = 44 + dataSize
    val buf = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
    buf.put("RIFF".toByteArray(Charsets.US_ASCII))
    buf.putInt(36 + dataSize)
    buf.put("WAVE".toByteArray(Charsets.US_ASCII))
    buf.put("fmt ".toByteArray(Charsets.US_ASCII))
    buf.putInt(16) // PCM chunk size
    buf.putShort(1) // PCM format
    buf.putShort(1) // mono
    buf.putInt(sampleRate)
    buf.putInt(sampleRate * 2) // byte rate
    buf.putShort(2) // block align
    buf.putShort(16) // bits per sample
    buf.put("data".toByteArray(Charsets.US_ASCII))
    buf.putInt(dataSize)
    for (s in samples) {
      buf.putShort(s)
    }
    return buf.array()
  }
}
