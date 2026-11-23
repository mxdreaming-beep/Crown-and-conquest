package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.model.Faction
import com.example.game.model.MatchResult
import com.example.ui.theme.AgedParchment
import com.example.ui.theme.AgedTimber
import com.example.ui.theme.BronzeBorder
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.CarvedWood
import com.example.ui.theme.DarkOak
import com.example.ui.theme.EmberRed
import com.example.ui.theme.EnemyRed
import com.example.ui.theme.ParchmentCream
import com.example.ui.theme.PlayerBlue

@Composable
fun PostGameScreen(
  result: MatchResult,
  onPlayAgain: () -> Unit,
  onReturnToMenu: () -> Unit,
) {
  BackHandler { onReturnToMenu() }

  val s = LocalAppStrings.current
  val isVictory = result.winner == Faction.PLAYER
  val mins = (result.durationSec / 60).toInt()
  val secs = (result.durationSec % 60).toInt()
  val timeFormatted = "%02d:%02d".format(mins, secs)

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .background(DarkOak)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .navigationBarsPadding()
        .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    BoxWithConstraints(
      modifier = Modifier.weight(1f).widthIn(max = 1040.dp).fillMaxWidth(),
    ) {
      val isLandscape = maxWidth >= 640.dp && maxWidth > maxHeight
      if (isLandscape) {
        Row(
          modifier = Modifier.fillMaxSize(),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Column(
            modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            PostGameBanner(result = result, isVictory = isVictory, timeFormatted = timeFormatted, s = s, compact = true)
            if (result.aiSummaries.size > 1) {
              RivalsChronicleCard(result = result, s = s)
            }
          }
          Column(
            modifier = Modifier.weight(1.1f).fillMaxHeight().verticalScroll(rememberScrollState()),
          ) {
            PostGameStatsCard(result = result, s = s)
          }
        }
      } else {
        Column(
          modifier =
            Modifier
              .fillMaxSize()
              .widthIn(max = 640.dp)
              .verticalScroll(rememberScrollState()),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          PostGameBanner(result = result, isVictory = isVictory, timeFormatted = timeFormatted, s = s, compact = false)
          Spacer(modifier = Modifier.height(14.dp))
          if (result.aiSummaries.size > 1) {
            RivalsChronicleCard(result = result, s = s)
            Spacer(modifier = Modifier.height(12.dp))
          }
          PostGameStatsCard(result = result, s = s)
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
      modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      OutlinedButton(
        onClick = onReturnToMenu,
        border = BorderStroke(1.dp, BronzeBorder),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ParchmentCream),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.weight(1f).height(50.dp).testTag("postgame_menu_button"),
      ) {
        Text(s.mainMenu, style = MaterialTheme.typography.labelLarge)
      }
      Button(
        onClick = onPlayAgain,
        colors = ButtonDefaults.buttonColors(containerColor = BurnishedGold, contentColor = DarkOak),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.weight(1f).height(50.dp).testTag("postgame_play_again_button"),
      ) {
        Text(s.playAgain, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
      }
    }
  }
}

@Composable
private fun PostGameBanner(
  result: MatchResult,
  isVictory: Boolean,
  timeFormatted: String,
  s: AppStrings,
  compact: Boolean,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.8.dp, if (isVictory) BurnishedGold else EmberRed),
    modifier = Modifier.fillMaxWidth().height(if (compact) 136.dp else 164.dp),
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      Image(
        painter = painterResource(id = R.drawable.img_victory_banner),
        contentDescription = if (isVictory) s.victory else s.defeat,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                if (isVictory) {
                  listOf(Color(0x441B1209), Color(0xDE16100A))
                } else {
                  listOf(Color(0x88240B0B), Color(0xEC160A0A))
                }
              )
            )
      )
      Column(
        modifier = Modifier.align(Alignment.Center).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Icon(
          imageVector = if (isVictory) Icons.Default.EmojiEvents else Icons.Default.Shield,
          contentDescription = null,
          tint = if (isVictory) BurnishedGold else EmberRed,
          modifier = Modifier.size(if (compact) 36.dp else 46.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = if (isVictory) s.victory else s.defeat,
          style = if (compact) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displayLarge,
          color = if (isVictory) BurnishedGold else EmberRed,
        )
        Text(
          text = "${s.mapName(result.mapType)} • ${s.durationPrefix} $timeFormatted",
          style = MaterialTheme.typography.bodyLarge,
          color = AgedParchment,
        )
      }
    }
  }
}

@Composable
private fun RivalsChronicleCard(result: MatchResult, s: AppStrings) {
  Surface(
    color = AgedTimber,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.5.dp, BronzeBorder),
    modifier = Modifier.fillMaxWidth().testTag("postgame_rivals_card"),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(s.rivalChronicle, style = MaterialTheme.typography.titleMedium, color = BurnishedGold)
      HorizontalDivider(color = BronzeBorder.copy(alpha = 0.45f))
      result.aiSummaries.forEach { summary ->
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .background(CarvedWood.copy(alpha = 0.42f), RoundedCornerShape(6.dp))
              .padding(horizontal = 10.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1.3f)) {
            Text(
              text = "${s.factionName(summary.faction)} (${s.civName(summary.civilization)})",
              style = MaterialTheme.typography.labelLarge,
              color = Color(summary.faction.bannerHex),
            )
            Text(
              text =
                "${s.ageName(summary.age)} • " +
                  if (summary.survivingBuildings == 0) s.defeatedLabel else "${summary.survivingBuildings} ${s.bldgsLeftSuffix}",
              style = MaterialTheme.typography.labelSmall,
              color = AgedParchment,
            )
          }
          Text(
            text = "${summary.stats.enemiesKilled} ${s.killsLabel} • ${summary.stats.buildingsRazed} ${s.razedLabel}",
            style = MaterialTheme.typography.labelMedium,
            color = ParchmentCream,
          )
        }
      }
    }
  }
}

@Composable
private fun PostGameStatsCard(result: MatchResult, s: AppStrings) {
  Surface(
    color = AgedTimber,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.5.dp, BronzeBorder),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
      Row(modifier = Modifier.fillMaxWidth()) {
        Text(s.categoryLabel, style = MaterialTheme.typography.labelLarge, color = AgedParchment, modifier = Modifier.weight(1.4f))
        Text(s.civName(result.playerCiv), style = MaterialTheme.typography.labelLarge, color = PlayerBlue, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Text(s.civName(result.enemyCiv), style = MaterialTheme.typography.labelLarge, color = EnemyRed, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
      }
      HorizontalDivider(color = BronzeBorder.copy(alpha = 0.45f))

      StatRow(s.ageReached, s.ageName(result.playerAge), s.ageName(result.enemyAge))
      StatRow(s.woodGathered, result.playerStats.totalWoodGathered.toString(), result.enemyStats.totalWoodGathered.toString())
      StatRow(s.foodGathered, result.playerStats.totalFoodGathered.toString(), result.enemyStats.totalFoodGathered.toString())
      StatRow(s.goldGathered, result.playerStats.totalGoldGathered.toString(), result.enemyStats.totalGoldGathered.toString())
      StatRow(s.stoneGathered, result.playerStats.totalStoneGathered.toString(), result.enemyStats.totalStoneGathered.toString())
      HorizontalDivider(color = BronzeBorder.copy(alpha = 0.45f))
      StatRow(s.unitsTrained, result.playerStats.unitsTrained.toString(), result.enemyStats.unitsTrained.toString())
      StatRow(s.enemiesDefeated, result.playerStats.enemiesKilled.toString(), result.enemyStats.enemiesKilled.toString())
      StatRow(s.unitsLost, result.playerStats.unitsLost.toString(), result.enemyStats.unitsLost.toString())
      StatRow(s.buildingsBuilt, result.playerStats.buildingsConstructed.toString(), result.enemyStats.buildingsConstructed.toString())
      StatRow(s.buildingsRazed, result.playerStats.buildingsRazed.toString(), result.enemyStats.buildingsRazed.toString())
    }
  }
}

@Composable
private fun StatRow(label: String, playerVal: String, enemyVal: String) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .background(CarvedWood.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
        .padding(horizontal = 8.dp, vertical = 5.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, style = MaterialTheme.typography.bodyMedium, color = ParchmentCream, modifier = Modifier.weight(1.4f))
    Text(playerVal, style = MaterialTheme.typography.labelMedium, color = Color(0xFF7CB4FF), textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    Text(enemyVal, style = MaterialTheme.typography.labelMedium, color = Color(0xFFFF8282), textAlign = TextAlign.End, modifier = Modifier.weight(1f))
  }
}
