package com.example.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import com.example.game.AppScreen
import java.io.File
import java.util.EnumMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages looping medieval start-screen theme music via static AudioTrack and
 * low-latency polyphonic battlefield SFX via SoundPool.
 * All native Android audio calls are safely guarded for headless JVM / Robolectric environments.
 */
class SoundEngine(context: Context) {

  private val appContext = context.applicationContext
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val _musicEnabled = MutableStateFlow(true)
  val musicEnabled: StateFlow<Boolean> = _musicEnabled.asStateFlow()

  private val _sfxEnabled = MutableStateFlow(true)
  val sfxEnabled: StateFlow<Boolean> = _sfxEnabled.asStateFlow()

  private var soundPool: SoundPool? = null
  private val loadedSfxIds = EnumMap<GameSfx, Int>(GameSfx::class.java)
  private var musicTrack: AudioTrack? = null
  private var musicPrepared = false
  private var currentScreen: AppScreen = AppScreen.Title
  private var hostPaused = false
  private var released = false

  private val lastPlayMs = EnumMap<GameSfx, Long>(GameSfx::class.java)

  init {
    scope.launch {
      prepareSfxPool()
      prepareMenuMusic()
      syncMusicPlayback()
    }
  }

  private fun prepareSfxPool() {
    runCatching {
      val attrs =
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_GAME)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()
      val pool =
        SoundPool.Builder()
          .setMaxStreams(12)
          .setAudioAttributes(attrs)
          .build()
      soundPool = pool

      val sfxDir = File(appContext.cacheDir, "crown_sfx_v3").apply { mkdirs() }
      for (sfx in GameSfx.entries) {
        if (released) break
        val wavFile = File(sfxDir, "${sfx.name.lowercase()}.wav")
        if (!wavFile.exists() || wavFile.length() < 44L) {
          wavFile.writeBytes(MedievalSynthesizer.generateSfxWav(sfx))
        }
        val soundId = pool.load(wavFile.absolutePath, 1)
        if (soundId > 0) {
          loadedSfxIds[sfx] = soundId
        }
      }
    }
  }

  private fun prepareMenuMusic() {
    runCatching {
      val pcm = MedievalSynthesizer.generateMedievalThemePcm()
      val byteCount = pcm.size * 2
      val frameCount = pcm.size / 2
      val attrs =
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_GAME)
          .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
          .build()
      val format =
        AudioFormat.Builder()
          .setSampleRate(MedievalSynthesizer.MUSIC_SAMPLE_RATE)
          .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
          .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
          .build()
      val track =
        AudioTrack.Builder()
          .setAudioAttributes(attrs)
          .setAudioFormat(format)
          .setBufferSizeInBytes(byteCount)
          .setTransferMode(AudioTrack.MODE_STATIC)
          .build()
      val written = track.write(pcm, 0, pcm.size)
      if (written > 0) {
        track.setLoopPoints(0, frameCount, -1)
        track.setVolume(0.55f)
        musicTrack = track
        musicPrepared = true
      } else {
        track.release()
      }
    }
  }

  fun setScreen(screen: AppScreen) {
    currentScreen = screen
    syncMusicPlayback()
  }

  fun toggleMusic() {
    _musicEnabled.value = !_musicEnabled.value
    syncMusicPlayback()
  }

  fun toggleSfx() {
    _sfxEnabled.value = !_sfxEnabled.value
  }

  fun onHostPause() {
    hostPaused = true
    runCatching {
      musicTrack?.let { if (it.playState == AudioTrack.PLAYSTATE_PLAYING) it.pause() }
    }
  }

  fun onHostResume() {
    hostPaused = false
    syncMusicPlayback()
  }

  private fun shouldPlayStartMusic(): Boolean =
    !released &&
      !hostPaused &&
      _musicEnabled.value &&
      (currentScreen is AppScreen.Title || currentScreen is AppScreen.SkirmishSetup)

  private fun syncMusicPlayback() {
    if (!musicPrepared) return
    val track = musicTrack ?: return
    runCatching {
      if (shouldPlayStartMusic()) {
        if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
          track.play()
        }
      } else {
        if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
          track.pause()
        }
      }
    }
  }

  fun playSfx(sfx: GameSfx, volume: Float = 0.85f, pitch: Float = 1.0f, pan: Float = 0f) {
    if (released || hostPaused || !_sfxEnabled.value) return
    val soundId = loadedSfxIds[sfx] ?: return
    val now = System.currentTimeMillis()
    val minGapMs =
      when (sfx) {
        GameSfx.WOOD_CHOP, GameSfx.MINE_PICK, GameSfx.BUILD_HAMMER -> 380L
        GameSfx.ARROW_TWANG, GameSfx.SWORD_CLASH, GameSfx.GUNSHOT, GameSfx.MG_BURST -> 95L
        GameSfx.SNIPER_SHOT, GameSfx.CANNON_FIRE, GameSfx.ROCKET_LAUNCH -> 120L
        GameSfx.EXPLOSION -> 140L
        else -> 40L
      }
    val prev = lastPlayMs[sfx] ?: 0L
    if (now - prev < minGapMs) return
    lastPlayMs[sfx] = now

    val effectivePitch =
      if (pitch == 1.0f &&
        sfx in setOf(
          GameSfx.WOOD_CHOP,
          GameSfx.MINE_PICK,
          GameSfx.BUILD_HAMMER,
          GameSfx.ARROW_TWANG,
          GameSfx.SWORD_CLASH,
          GameSfx.GUNSHOT,
          GameSfx.MG_BURST,
          GameSfx.SNIPER_SHOT,
          GameSfx.CANNON_FIRE,
          GameSfx.ROCKET_LAUNCH,
          GameSfx.EXPLOSION,
        )
      ) {
        0.94f + kotlin.random.Random.nextFloat() * 0.12f
      } else {
        pitch
      }

    runCatching {
      val v = volume.coerceIn(0.05f, 1.0f)
      val p = pan.coerceIn(-1f, 1f)
      val leftVol = (v * (1f - (p.coerceAtLeast(0f) * 0.72f))).coerceIn(0.03f, 1.0f)
      val rightVol = (v * (1f - ((-p).coerceAtLeast(0f) * 0.72f))).coerceIn(0.03f, 1.0f)
      soundPool?.play(soundId, leftVol, rightVol, 1, 0, effectivePitch.coerceIn(0.85f, 1.18f))
    }
  }

  fun release() {
    if (released) return
    released = true
    scope.cancel()
    runCatching {
      musicTrack?.stop()
      musicTrack?.release()
      musicTrack = null
      soundPool?.release()
      soundPool = null
    }
  }
}
