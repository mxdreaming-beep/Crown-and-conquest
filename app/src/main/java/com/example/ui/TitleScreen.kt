package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AgedParchment
import com.example.ui.theme.BronzeBorder
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.DarkOak
import com.example.ui.theme.ParchmentCream
import kotlin.math.sin

@Composable
fun TitleScreen(
  onPlaySkirmish: () -> Unit,
  musicEnabled: Boolean = true,
  sfxEnabled: Boolean = true,
  onToggleMusic: () -> Unit = {},
  onToggleSfx: () -> Unit = {},
) {
  var showManual by remember { mutableStateOf(false) }
  val infinite = rememberInfiniteTransition(label = "title_embers")
  val emberClock by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
    label = "ember_phase",
  )

  Box(modifier = Modifier.fillMaxSize().background(DarkOak)) {
    Image(
      painter = painterResource(id = R.drawable.img_title_backdrop),
      contentDescription = "Medieval castle valley battlefield artwork",
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )

    // Dramatic dark oak gradient scrim for crisp readability
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors =
                listOf(
                  Color(0xB8140E09),
                  Color(0x52140E09),
                  Color(0xE217100A),
                  Color(0xFA140D08),
                )
            )
          )
    )

    // Drifting golden siege embers over the painted castle valley
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      if (w > 0f && h > 0f) {
        for (i in 0 until 26) {
          val seed = i * 0.371f
          val progress = (emberClock + seed) % 1f
          val baseX = ((i * 193) % 100) / 100f * w
          val x = baseX + sin(progress * 6.283f + i) * 26f
          val y = h * (1.02f - progress * 0.92f)
          val alpha = sin(progress * Math.PI.toFloat()).coerceIn(0f, 1f) * 0.72f
          val rad = 1.6f + (i % 3) * 1.1f
          drawCircle(Color(0xFFFFB240).copy(alpha = alpha * 0.45f), rad * 2.3f, Offset(x, y))
          drawCircle(Color(0xFFFFE598).copy(alpha = alpha), rad, Offset(x, y))
        }
      }
    }

    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .navigationBarsPadding()
          .padding(horizontal = 24.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top Audio Medallion Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = Color(0xCC22170F),
          shape = CircleShape,
          border = BorderStroke(1.2.dp, if (musicEnabled) BurnishedGold else BronzeBorder.copy(alpha = 0.45f)),
        ) {
          IconButton(
            onClick = onToggleMusic,
            modifier = Modifier.size(48.dp).testTag("title_music_toggle"),
          ) {
            Icon(
              imageVector = if (musicEnabled) Icons.Default.MusicNote else Icons.Default.MusicOff,
              contentDescription = if (musicEnabled) "Mute Music" else "Unmute Music",
              tint = if (musicEnabled) BurnishedGold else AgedParchment.copy(alpha = 0.55f),
            )
          }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
          color = Color(0xCC22170F),
          shape = CircleShape,
          border = BorderStroke(1.2.dp, if (sfxEnabled) BurnishedGold else BronzeBorder.copy(alpha = 0.45f)),
        ) {
          IconButton(
            onClick = onToggleSfx,
            modifier = Modifier.size(48.dp).testTag("title_sfx_toggle"),
          ) {
            Icon(
              imageVector = if (sfxEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
              contentDescription = if (sfxEnabled) "Mute SFX" else "Unmute SFX",
              tint = if (sfxEnabled) BurnishedGold else AgedParchment.copy(alpha = 0.55f),
            )
          }
        }
      }

      // Center-Top Crest Emblem
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 4.dp),
      ) {
        Box(
          modifier =
            Modifier
              .size(72.dp)
              .border(2.dp, BurnishedGold, RoundedCornerShape(14.dp))
              .padding(3.dp)
              .border(1.dp, BronzeBorder.copy(alpha = 0.65f), RoundedCornerShape(11.dp))
              .background(
                Brush.radialGradient(listOf(Color(0xEE3C2715), Color(0xDD1A110A))),
                RoundedCornerShape(11.dp),
              ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = "Royal crest",
            tint = BurnishedGold,
            modifier = Modifier.size(40.dp),
          )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "Crown & Conquest",
          style = MaterialTheme.typography.displayLarge,
          color = BurnishedGold,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = stringResource(R.string.game_subtitle),
          style = MaterialTheme.typography.titleMedium,
          color = AgedParchment,
          textAlign = TextAlign.Center,
        )
      }

      // Bottom Command Card with Double Gilded Bezel
      Surface(
        modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
        color = Color(0xE6221810),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.8.dp, BurnishedGold.copy(alpha = 0.8f)),
        tonalElevation = 8.dp,
      ) {
        Column(
          modifier =
            Modifier
              .padding(4.dp)
              .border(1.dp, BronzeBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
              .padding(18.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = "Build your empire from Dark Age huts to Modern fortresses. Gather Wood, Food, Gold, and Stone—command armies—and conquer your rival.",
            style = MaterialTheme.typography.bodyMedium,
            color = ParchmentCream,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(16.dp))

          Button(
            onClick = onPlaySkirmish,
            colors = ButtonDefaults.buttonColors(containerColor = BurnishedGold, contentColor = DarkOak),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("play_skirmish_button"),
          ) {
            Text(
              text = stringResource(R.string.action_play_skirmish),
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          OutlinedButton(
            onClick = { showManual = true },
            border = BorderStroke(1.2.dp, BronzeBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ParchmentCream),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("field_manual_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.MenuBook,
              contentDescription = null,
              tint = BurnishedGold,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.action_how_to_play),
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }
      }
    }
  }

  if (showManual) {
    GameGuideDialog(onDismiss = { showManual = false })
  }
}

