package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.model.Civilization
import com.example.game.model.Difficulty
import com.example.game.model.Faction
import com.example.game.model.MapSizeOption
import com.example.game.model.MapType
import com.example.game.model.SkirmishConfig
import com.example.ui.theme.AgedParchment
import com.example.ui.theme.AgedTimber
import com.example.ui.theme.BronzeBorder
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.CarvedWood
import com.example.ui.theme.DarkOak
import com.example.ui.theme.EnemyRed
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.ParchmentCream
import com.example.ui.theme.PlayerBlue

@Composable
fun SkirmishSetupScreen(
  config: SkirmishConfig,
  onUpdateConfig: (Civilization, Civilization, MapType, Difficulty) -> Unit,
  onLaunchBattle: () -> Unit,
  onBack: () -> Unit,
  musicEnabled: Boolean = true,
  onToggleMusic: () -> Unit = {},
  onUpdateFullConfig: ((SkirmishConfig) -> Unit)? = null,
) {
  BackHandler { onBack() }

  val s = LocalAppStrings.current

  fun emitFull(next: SkirmishConfig) {
    if (onUpdateFullConfig != null) {
      onUpdateFullConfig(next)
    } else {
      onUpdateConfig(next.playerCiv, next.enemyCiv, next.mapType, next.difficulty)
    }
  }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .background(DarkOak)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .navigationBarsPadding(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Top Header
    Row(
      modifier = Modifier.widthIn(max = 1080.dp).fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("setup_back_button")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back, tint = BurnishedGold)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(s.warRoomTitle, style = MaterialTheme.typography.headlineMedium, color = BurnishedGold)
      }
      IconButton(onClick = onToggleMusic, modifier = Modifier.size(48.dp).testTag("setup_music_toggle")) {
        Icon(
          imageVector = if (musicEnabled) Icons.Default.MusicNote else Icons.Default.MusicOff,
          contentDescription = if (musicEnabled) s.muteMusic else s.unmuteMusic,
          tint = if (musicEnabled) BurnishedGold else AgedParchment.copy(alpha = 0.55f),
        )
      }
    }

    BoxWithConstraints(
      modifier = Modifier.weight(1f).widthIn(max = 1080.dp).fillMaxWidth(),
    ) {
      val isLandscape = maxWidth >= 640.dp && maxWidth > maxHeight
      if (isLandscape) {
        Row(
          modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Column(
            modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            WarRoomBannerCard(s = s)
            PlayerCivSection(config = config, s = s, onSelect = { civ -> emitFull(config.copy(playerCiv = civ)) })
            Spacer(modifier = Modifier.height(6.dp))
          }
          Column(
            modifier = Modifier.weight(1.1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            BattleSettingsSection(config = config, s = s, emitFull = ::emitFull)
            Spacer(modifier = Modifier.height(6.dp))
          }
        }
      } else {
        Column(
          modifier =
            Modifier
              .fillMaxSize()
              .widthIn(max = 720.dp)
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          WarRoomBannerCard(s = s)
          PlayerCivSection(config = config, s = s, onSelect = { civ -> emitFull(config.copy(playerCiv = civ)) })
          BattleSettingsSection(config = config, s = s, emitFull = ::emitFull)
          Spacer(modifier = Modifier.height(8.dp))
        }
      }
    }

    // Bottom Launch Action
    Surface(
      modifier = Modifier.widthIn(max = 1080.dp).fillMaxWidth(),
      color = AgedTimber,
      border = BorderStroke(1.dp, BronzeBorder),
    ) {
      Button(
        onClick = onLaunchBattle,
        colors = ButtonDefaults.buttonColors(containerColor = BurnishedGold, contentColor = DarkOak),
        shape = RoundedCornerShape(8.dp),
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(50.dp)
            .testTag("launch_battle_button"),
      ) {
        Text(
          text = s.launchBattle,
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        )
      }
    }
  }
}

@Composable
private fun WarRoomBannerCard(s: AppStrings) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.5.dp, BronzeBorder),
    modifier = Modifier.fillMaxWidth().height(104.dp),
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      Image(
        painter = painterResource(id = R.drawable.img_war_room_banner),
        contentDescription = s.warRoomTitle,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(listOf(Color(0x22120C08), Color(0xCC160F0A)))
            )
      )
      Column(
        modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
      ) {
        Text(s.marshalRealm, style = MaterialTheme.typography.titleMedium, color = BurnishedGold)
        Text(
          s.marshalRealmSub,
          style = MaterialTheme.typography.labelSmall,
          color = ParchmentCream,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun PlayerCivSection(
  config: SkirmishConfig,
  s: AppStrings,
  onSelect: (Civilization) -> Unit,
) {
  SectionLabel(s.yourCivLabel, PlayerBlue)
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Civilization.entries.forEach { civ ->
      val selected = config.playerCiv == civ
      Surface(
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable { onSelect(civ) }
            .testTag("player_civ_${civ.name.lowercase()}"),
        color = if (selected) CarvedWood else AgedTimber,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) BurnishedGold else BronzeBorder.copy(alpha = 0.45f)),
      ) {
        Row(
          modifier = Modifier.padding(11.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          HeraldicCivShield(civ = civ, modifier = Modifier.size(42.dp))
          Column(modifier = Modifier.weight(1f)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(s.civName(civ), style = MaterialTheme.typography.titleLarge, color = if (selected) BurnishedGold else ParchmentCream)
              Text(s.civSpecialty(civ), style = MaterialTheme.typography.labelMedium, color = Color(civ.accentHex))
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(s.civBonus(civ), style = MaterialTheme.typography.bodyMedium, color = AgedParchment)
          }
        }
      }
    }
  }
}

@Composable
private fun BattleSettingsSection(
  config: SkirmishConfig,
  s: AppStrings,
  emitFull: (SkirmishConfig) -> Unit,
) {
  SectionLabel("${s.aiRivalsLabel} (${config.aiCount})", BurnishedGold)
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    listOf(1 to s.rivalDuel, 2 to s.rivalThree, 3 to s.rivalFour).forEach { (count, desc) ->
      val selected = config.aiCount == count
      Surface(
        modifier =
          Modifier
            .weight(1f)
            .height(54.dp)
            .clickable { emitFull(config.copy(aiCount = count)) }
            .testTag("ai_count_$count"),
        color = if (selected) CarvedWood else AgedTimber,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) BurnishedGold else BronzeBorder.copy(alpha = 0.45f)),
      ) {
        Column(
          modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Text(
            text = "$count AI",
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) BurnishedGold else ParchmentCream,
          )
          Text(desc, style = MaterialTheme.typography.labelSmall, color = AgedParchment, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }
    }
  }

  SectionLabel("${s.aiOneLabel} • ${s.factionName(Faction.ENEMY)}", Color(Faction.ENEMY.bannerHex))
  EnemyCivSelectorRow(
    selectedCiv = config.enemyCiv,
    highlightColor = EnemyRed,
    fillColor = Color(0xFF471A1A),
    tagPrefix = "enemy_civ",
    s = s,
    onSelect = { civ -> emitFull(config.copy(enemyCiv = civ)) },
  )

  if (config.aiCount >= 2) {
    SectionLabel("${s.aiTwoLabel} • ${s.factionName(Faction.ENEMY_2)}", Color(Faction.ENEMY_2.bannerHex))
    EnemyCivSelectorRow(
      selectedCiv = config.enemy2Civ,
      highlightColor = ForestEmerald,
      fillColor = Color(0xFF173D25),
      tagPrefix = "enemy2_civ",
      s = s,
      onSelect = { civ -> emitFull(config.copy(enemy2Civ = civ)) },
    )
  }

  if (config.aiCount >= 3) {
    SectionLabel("${s.aiThreeLabel} • ${s.factionName(Faction.ENEMY_3)}", Color(0xFFAF6BE0))
    EnemyCivSelectorRow(
      selectedCiv = config.enemy3Civ,
      highlightColor = Color(0xFFAF6BE0),
      fillColor = Color(0xFF341845),
      tagPrefix = "enemy3_civ",
      s = s,
      onSelect = { civ -> emitFull(config.copy(enemy3Civ = civ)) },
    )
  }

  SectionLabel(s.battlefieldSizeLabel, BurnishedGold)
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    MapSizeOption.entries.forEach { sizeOpt ->
      val selected = config.mapSize == sizeOpt
      Surface(
        modifier =
          Modifier
            .weight(1f)
            .height(58.dp)
            .clickable { emitFull(config.copy(mapSize = sizeOpt)) }
            .testTag("map_size_${sizeOpt.name.lowercase()}"),
        color = if (selected) CarvedWood else AgedTimber,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) BurnishedGold else BronzeBorder.copy(alpha = 0.45f)),
      ) {
        Column(
          modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Text(
            text = s.mapSizeName(sizeOpt),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) BurnishedGold else ParchmentCream,
            maxLines = 1,
          )
          Text(
            text = "${sizeOpt.tiles}×${sizeOpt.tiles} ${s.tilesUnit}",
            style = MaterialTheme.typography.labelSmall,
            color = AgedParchment,
            maxLines = 1,
          )
        }
      }
    }
  }

  SectionLabel(s.mapTypeLabel, BurnishedGold)
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    MapType.entries.forEach { map ->
      val selected = config.mapType == map
      Surface(
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable { emitFull(config.copy(mapType = map)) }
            .testTag("map_${map.name.lowercase()}"),
        color = if (selected) CarvedWood else AgedTimber,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) BurnishedGold else BronzeBorder.copy(alpha = 0.45f)),
      ) {
        Row(
          modifier = Modifier.padding(11.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          MapBiomePreview(mapType = map, modifier = Modifier.size(56.dp, 44.dp).clip(RoundedCornerShape(6.dp)))
          Column(modifier = Modifier.weight(1f)) {
            Text(s.mapName(map), style = MaterialTheme.typography.titleMedium, color = if (selected) BurnishedGold else ParchmentCream)
            Text(s.mapDesc(map), style = MaterialTheme.typography.bodyMedium, color = AgedParchment)
          }
        }
      }
    }
  }

  SectionLabel(s.difficultyLabel, BurnishedGold)
  val diffRows = Difficulty.entries.chunked(2)
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    diffRows.forEach { rowItems ->
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        rowItems.forEach { diff ->
          val selected = config.difficulty == diff
          Surface(
            modifier =
              Modifier
                .weight(1f)
                .height(58.dp)
                .clickable { emitFull(config.copy(difficulty = diff)) }
                .testTag("diff_${diff.name.lowercase()}"),
            color = if (selected) CarvedWood else AgedTimber,
            shape = RoundedCornerShape(10.dp),
            border =
              BorderStroke(
                if (selected) 2.dp else 1.dp,
                if (selected) (if (diff == Difficulty.EXTREME) EnemyRed else BurnishedGold) else BronzeBorder.copy(alpha = 0.45f),
              ),
          ) {
            Column(
              modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
            ) {
              Text(
                text = s.diffName(diff),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) (if (diff == Difficulty.EXTREME) Color(0xFFFF7A70) else BurnishedGold) else ParchmentCream,
                maxLines = 1,
              )
              Text(
                text = s.diffSummary(diff),
                style = MaterialTheme.typography.labelSmall,
                color = AgedParchment,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun HeraldicCivShield(civ: Civilization, modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val shield = Path().apply {
      moveTo(w * 0.12f, h * 0.10f)
      lineTo(w * 0.88f, h * 0.10f)
      lineTo(w * 0.88f, h * 0.56f)
      lineTo(w * 0.50f, h * 0.92f)
      lineTo(w * 0.12f, h * 0.56f)
      close()
    }
    val accent = Color(civ.accentHex)
    drawPath(shield, Color(0xFF1F160E))
    drawPath(shield, accent.copy(alpha = 0.28f))
    drawPath(shield, Color(0xFFD6A84A), style = Stroke(width = 2.2f))

    when (civ) {
      Civilization.BRITONS -> {
        drawArc(
          color = Color(0xFFEBD075),
          startAngle = -65f,
          sweepAngle = 130f,
          useCenter = false,
          topLeft = Offset(w * 0.24f, h * 0.22f),
          size = Size(w * 0.34f, h * 0.50f),
          style = Stroke(width = 2.4f),
        )
        drawLine(Color(0xFFFAF2D6), Offset(w * 0.26f, h * 0.47f), Offset(w * 0.74f, h * 0.47f), strokeWidth = 2.2f)
      }
      Civilization.FRANKS -> {
        drawLine(Color(0xFF63BA6B), Offset(w * 0.24f, h * 0.56f), Offset(w * 0.50f, h * 0.30f), strokeWidth = 3.2f)
        drawLine(Color(0xFF63BA6B), Offset(w * 0.50f, h * 0.30f), Offset(w * 0.76f, h * 0.56f), strokeWidth = 3.2f)
        drawCircle(Color(0xFFEBD075), w * 0.08f, Offset(w * 0.50f, h * 0.56f))
      }
      Civilization.TEUTONS -> {
        drawLine(Color(0xFFEBE4D0), Offset(w * 0.50f, h * 0.24f), Offset(w * 0.50f, h * 0.72f), strokeWidth = 5.0f)
        drawLine(Color(0xFFEBE4D0), Offset(w * 0.26f, h * 0.44f), Offset(w * 0.74f, h * 0.44f), strokeWidth = 5.0f)
        drawLine(Color(0xFF282018), Offset(w * 0.50f, h * 0.28f), Offset(w * 0.50f, h * 0.68f), strokeWidth = 2.4f)
        drawLine(Color(0xFF282018), Offset(w * 0.30f, h * 0.44f), Offset(w * 0.70f, h * 0.44f), strokeWidth = 2.4f)
      }
      Civilization.BYZANTINES -> {
        drawCircle(Color(0xFFC47EE0), w * 0.22f, Offset(w * 0.50f, h * 0.47f), style = Stroke(width = 2.4f))
        drawCircle(Color(0xFFEBD075), w * 0.11f, Offset(w * 0.50f, h * 0.47f))
      }
    }
  }
}

@Composable
private fun MapBiomePreview(mapType: MapType, modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    when (mapType) {
      MapType.ARABIA -> {
        drawRect(Color(0xFFA1884E), Offset.Zero, size)
        drawOval(Color(0xFF7D6234), Offset(w * 0.08f, h * 0.42f), Size(w * 0.55f, h * 0.45f))
        drawOval(Color(0xFFB89652), Offset(w * 0.36f, h * 0.14f), Size(w * 0.54f, h * 0.48f))
        drawCircle(Color(0xFFFFD15C), 4.2f, Offset(w * 0.52f, h * 0.50f))
        drawCircle(Color(0xFF566E30), 4.5f, Offset(w * 0.22f, h * 0.28f))
      }
      MapType.BLACK_FOREST -> {
        drawRect(Color(0xFF2B4220), Offset.Zero, size)
        drawLine(Color(0xFF6B4D2B), Offset(w * 0.1f, h * 0.85f), Offset(w * 0.9f, h * 0.15f), strokeWidth = 7f)
        for ((tx, ty) in listOf(0.20f to 0.25f, 0.38f to 0.22f, 0.72f to 0.65f, 0.84f to 0.52f, 0.28f to 0.72f)) {
          drawCircle(Color(0xFF1A2D13), 6.5f, Offset(w * tx, h * ty))
          drawCircle(Color(0xFF3E632B), 4.0f, Offset(w * tx - 1f, h * ty - 1.5f))
        }
      }
      MapType.COASTAL -> {
        drawRect(Color(0xFF425C2B), Offset.Zero, size)
        val river = Path().apply {
          moveTo(w * 0.35f, 0f)
          lineTo(w * 0.62f, 0f)
          lineTo(w * 0.74f, h)
          lineTo(w * 0.44f, h)
          close()
        }
        drawPath(river, Color(0xFF215E78))
        drawLine(Color(0xFFD6E8ED), Offset(w * 0.42f, h * 0.25f), Offset(w * 0.58f, h * 0.25f), strokeWidth = 1.6f)
        drawRect(Color(0xFF7D5029), Offset(w * 0.36f, h * 0.46f), Size(w * 0.32f, h * 0.14f))
      }
    }
    drawRect(Color(0x99C79B46), Offset.Zero, size, style = Stroke(width = 2.0f))
  }
}

@Composable
private fun EnemyCivSelectorRow(
  selectedCiv: Civilization,
  highlightColor: Color,
  fillColor: Color,
  tagPrefix: String,
  s: AppStrings,
  onSelect: (Civilization) -> Unit,
) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Civilization.entries.forEach { civ ->
      val selected = selectedCiv == civ
      Surface(
        modifier =
          Modifier
            .weight(1f)
            .height(64.dp)
            .clickable { onSelect(civ) }
            .testTag("${tagPrefix}_${civ.name.lowercase()}"),
        color = if (selected) fillColor else AgedTimber,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) highlightColor else BronzeBorder.copy(alpha = 0.45f)),
      ) {
        Column(
          modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          HeraldicCivShield(civ = civ, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = s.civName(civ),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) BurnishedGold else ParchmentCream,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
private fun SectionLabel(text: String, color: Color) {
  Text(
    text = text,
    style = MaterialTheme.typography.titleMedium,
    color = color,
    modifier = Modifier.padding(top = 4.dp),
  )
}
