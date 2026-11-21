package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
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
        .padding(18.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      modifier =
        Modifier
          .weight(1f)
          .widthIn(max = 640.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.8.dp, if (isVictory) BurnishedGold else EmberRed),
        modifier = Modifier.fillMaxWidth().height(168.dp),
      ) {
        Box(modifier = Modifier.fillMaxSize()) {
          Image(
            painter = painterResource(id = R.drawable.img_victory_banner),
            contentDescription = "Medieval castle battlement banner",
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
              modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = if (isVictory) stringResource(R.string.label_victory) else stringResource(R.string.label_defeat),
              style = MaterialTheme.typography.displayLarge,
              color = if (isVictory) BurnishedGold else EmberRed,
            )
            Text(
              text = "${result.mapType.displayName} • Duration $timeFormatted",
              style = MaterialTheme.typography.bodyLarge,
              color = AgedParchment,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      if (result.aiSummaries.size > 1) {
        Surface(
          color = AgedTimber,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.5.dp, BronzeBorder),
          modifier = Modifier.fillMaxWidth().testTag("postgame_rivals_card"),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Rival Empires Chronicle", style = MaterialTheme.typography.titleMedium, color = BurnishedGold)
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
                    text = "${summary.faction.displayName} (${summary.civilization.displayName})",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(summary.faction.bannerHex),
                  )
                  Text(
                    text = "${summary.age.displayName} • ${if (summary.survivingBuildings == 0) "Defeated" else "${summary.survivingBuildings} bldgs left"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AgedParchment,
                  )
                }
                Text(
                  text = "${summary.stats.enemiesKilled} Kills • ${summary.stats.buildingsRazed} Razed",
                  style = MaterialTheme.typography.labelMedium,
                  color = ParchmentCream,
                )
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
      }

      Surface(
        color = AgedTimber,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, BronzeBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(modifier = Modifier.fillMaxWidth()) {
            Text("Category", style = MaterialTheme.typography.labelLarge, color = AgedParchment, modifier = Modifier.weight(1.4f))
            Text(result.playerCiv.displayName, style = MaterialTheme.typography.labelLarge, color = PlayerBlue, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            Text(result.enemyCiv.displayName, style = MaterialTheme.typography.labelLarge, color = EnemyRed, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
          }
          HorizontalDivider(color = BronzeBorder.copy(alpha = 0.45f))

          StatRow("Age Reached", result.playerAge.displayName, result.enemyAge.displayName)
          StatRow("Wood Gathered", result.playerStats.totalWoodGathered.toString(), result.enemyStats.totalWoodGathered.toString())
          StatRow("Food Gathered", result.playerStats.totalFoodGathered.toString(), result.enemyStats.totalFoodGathered.toString())
          StatRow("Gold Gathered", result.playerStats.totalGoldGathered.toString(), result.enemyStats.totalGoldGathered.toString())
          StatRow("Stone Gathered", result.playerStats.totalStoneGathered.toString(), result.enemyStats.totalStoneGathered.toString())
          HorizontalDivider(color = BronzeBorder.copy(alpha = 0.45f))
          StatRow("Units Trained", result.playerStats.unitsTrained.toString(), result.enemyStats.unitsTrained.toString())
          StatRow("Enemies Defeated", result.playerStats.enemiesKilled.toString(), result.enemyStats.enemiesKilled.toString())
          StatRow("Units Lost", result.playerStats.unitsLost.toString(), result.enemyStats.unitsLost.toString())
          StatRow("Buildings Built", result.playerStats.buildingsConstructed.toString(), result.enemyStats.buildingsConstructed.toString())
          StatRow("Buildings Razed", result.playerStats.buildingsRazed.toString(), result.enemyStats.buildingsRazed.toString())
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

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
        Text("Main Menu", style = MaterialTheme.typography.labelLarge)
      }
      Button(
        onClick = onPlayAgain,
        colors = ButtonDefaults.buttonColors(containerColor = BurnishedGold, contentColor = DarkOak),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.weight(1f).height(50.dp).testTag("postgame_play_again_button"),
      ) {
        Text("Play Again", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
      }
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
