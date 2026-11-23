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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Language
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AgedParchment
import com.example.ui.theme.BronzeBorder
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.CarvedWood
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
  selectedLanguage: AppLanguage = AppLanguage.EN,
  onSelectLanguage: (AppLanguage) -> Unit = {},
) {
  val s = LocalAppStrings.current
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
      contentDescription = s.appTitle,
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

    BoxWithConstraints(
      modifier =
        Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .navigationBarsPadding()
          .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
      val isLandscape = maxWidth > maxHeight && maxWidth >= 540.dp
      val compactPortrait = maxHeight < 660.dp

      if (isLandscape) {
        Column(modifier = Modifier.fillMaxSize()) {
          AudioMedallionBar(
            musicEnabled = musicEnabled,
            sfxEnabled = sfxEnabled,
            onToggleMusic = onToggleMusic,
            onToggleSfx = onToggleSfx,
            s = s,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(
              modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
            ) {
              TitleCrestAndHeader(s = s, compact = true)
              Spacer(modifier = Modifier.height(10.dp))
              LanguageSelectorCard(
                selectedLanguage = selectedLanguage,
                onSelectLanguage = onSelectLanguage,
                s = s,
                compact = false,
              )
            }
            Box(
              modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
              contentAlignment = Alignment.Center,
            ) {
              TitleCommandCard(
                s = s,
                onPlaySkirmish = onPlaySkirmish,
                onOpenManual = { showManual = true },
                compact = true,
              )
            }
          }
        }
      } else {
        Column(
          modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          AudioMedallionBar(
            musicEnabled = musicEnabled,
            sfxEnabled = sfxEnabled,
            onToggleMusic = onToggleMusic,
            onToggleSfx = onToggleSfx,
            s = s,
          )

          Spacer(modifier = Modifier.height(if (compactPortrait) 4.dp else 8.dp))
          TitleCrestAndHeader(s = s, compact = compactPortrait)
          Spacer(modifier = Modifier.height(if (compactPortrait) 6.dp else 12.dp))

          Column(
            modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compactPortrait) 6.dp else 10.dp),
          ) {
            LanguageSelectorCard(
              selectedLanguage = selectedLanguage,
              onSelectLanguage = onSelectLanguage,
              s = s,
              compact = compactPortrait,
            )
            TitleCommandCard(
              s = s,
              onPlaySkirmish = onPlaySkirmish,
              onOpenManual = { showManual = true },
              compact = compactPortrait,
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

@Composable
private fun AudioMedallionBar(
  musicEnabled: Boolean,
  sfxEnabled: Boolean,
  onToggleMusic: () -> Unit,
  onToggleSfx: () -> Unit,
  s: AppStrings,
) {
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
          contentDescription = if (musicEnabled) s.muteMusic else s.unmuteMusic,
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
          contentDescription = if (sfxEnabled) s.muteSfx else s.unmuteSfx,
          tint = if (sfxEnabled) BurnishedGold else AgedParchment.copy(alpha = 0.55f),
        )
      }
    }
  }
}

@Composable
private fun TitleCrestAndHeader(s: AppStrings, compact: Boolean) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.padding(top = 2.dp),
  ) {
    Box(
      modifier =
        Modifier
          .size(if (compact) 42.dp else 68.dp)
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
        contentDescription = s.appTitle,
        tint = BurnishedGold,
        modifier = Modifier.size(if (compact) 24.dp else 38.dp),
      )
    }
    Spacer(modifier = Modifier.height(if (compact) 4.dp else 12.dp))
    Text(
      text = s.appTitle,
      style = if (compact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displayLarge,
      color = BurnishedGold,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(if (compact) 2.dp else 4.dp))
    Text(
      text = s.gameSubtitle,
      style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
      color = AgedParchment,
      textAlign = TextAlign.Center,
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguageSelectorCard(
  selectedLanguage: AppLanguage,
  onSelectLanguage: (AppLanguage) -> Unit,
  s: AppStrings,
  compact: Boolean,
) {
  Surface(
    modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth().testTag("language_selector_card"),
    color = Color(0xDD1E150E),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.2.dp, BronzeBorder.copy(alpha = 0.78f)),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = if (compact) 6.dp else 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Default.Language,
          contentDescription = s.languageLabel,
          tint = BurnishedGold,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = s.languageLabel,
          style = MaterialTheme.typography.labelLarge,
          color = BurnishedGold,
        )
      }
      if (compact) {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          AppLanguage.entries.forEach { lang ->
            LanguagePill(
              lang = lang,
              selected = lang == selectedLanguage,
              onSelectLanguage = onSelectLanguage,
            )
          }
        }
      } else {
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          AppLanguage.entries.forEach { lang ->
            LanguagePill(
              lang = lang,
              selected = lang == selectedLanguage,
              onSelectLanguage = onSelectLanguage,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun LanguagePill(
  lang: AppLanguage,
  selected: Boolean,
  onSelectLanguage: (AppLanguage) -> Unit,
) {
  Surface(
    color = if (selected) BurnishedGold else CarvedWood,
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(1.dp, if (selected) ParchmentCream else BronzeBorder.copy(alpha = 0.65f)),
    modifier =
      Modifier
        .heightIn(min = 36.dp)
        .clip(RoundedCornerShape(8.dp))
        .clickable { onSelectLanguage(lang) }
        .testTag("lang_${lang.code}"),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
      Text(
        text = lang.nativeName,
        style = MaterialTheme.typography.labelMedium.copy(
          fontSize = 12.sp,
          fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        ),
        color = if (selected) DarkOak else ParchmentCream,
      )
    }
  }
}

@Composable
private fun TitleCommandCard(
  s: AppStrings,
  onPlaySkirmish: () -> Unit,
  onOpenManual: () -> Unit,
  compact: Boolean = false,
) {
  Surface(
    modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth(),
    color = Color(0xE6221810),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.8.dp, BurnishedGold.copy(alpha = 0.8f)),
    tonalElevation = 8.dp,
  ) {
    Column(
      modifier =
        Modifier
          .padding(3.dp)
          .border(1.dp, BronzeBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
          .padding(if (compact) 11.dp else 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = s.splashDescription,
        style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
        color = ParchmentCream,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(if (compact) 8.dp else 14.dp))

      Button(
        onClick = onPlaySkirmish,
        colors = ButtonDefaults.buttonColors(containerColor = BurnishedGold, contentColor = DarkOak),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().height(if (compact) 48.dp else 52.dp).testTag("play_skirmish_button"),
      ) {
        Text(
          text = s.playSkirmish,
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        )
      }

      Spacer(modifier = Modifier.height(if (compact) 6.dp else 10.dp))

      OutlinedButton(
        onClick = onOpenManual,
        border = BorderStroke(1.2.dp, BronzeBorder),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ParchmentCream),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().height(if (compact) 44.dp else 48.dp).testTag("field_manual_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.MenuBook,
          contentDescription = null,
          tint = BurnishedGold,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = s.overmapAndFaq,
          style = MaterialTheme.typography.labelLarge,
        )
      }
    }
  }
}
