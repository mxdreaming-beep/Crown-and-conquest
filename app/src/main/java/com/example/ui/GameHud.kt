package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Carpenter
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.game.GameUiState
import com.example.game.engine.RtsEngine
import com.example.game.model.Age
import com.example.game.model.BuildCategory
import com.example.game.model.Building
import com.example.game.model.BuildingType
import com.example.game.model.Faction
import com.example.game.model.GameDefinitions
import com.example.game.model.GameWorld
import com.example.game.model.ProductionItem
import com.example.game.model.ResourceCost
import com.example.game.model.ResourceType
import com.example.game.model.TechType
import com.example.game.model.UnitOrderType
import com.example.game.model.UnitType
import com.example.game.render.TacticalMinimap
import com.example.ui.theme.AgedParchment
import com.example.ui.theme.AgedTimber
import com.example.ui.theme.BronzeBorder
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.CarvedWood
import com.example.ui.theme.DarkOak
import com.example.ui.theme.EmberRed
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.ParchmentCream

@Composable
fun TopResourceBar(
  world: GameWorld,
  gameSpeed: Float,
  onCycleSpeed: () -> Unit,
  onPauseClick: () -> Unit,
  onOpenGuide: () -> Unit = {},
  sfxEnabled: Boolean = true,
  onToggleSfx: () -> Unit = {},
) {
  val s = LocalAppStrings.current
  val pState = world.playerState
  val res = pState.resources
  val popCurrent = world.units.count { it.faction == Faction.PLAYER }
  val popCap = RtsEngine.populationCap(world.buildings.filter { it.faction == Faction.PLAYER })
  val housed = popCurrent >= popCap

  Surface(
    color = Color(0xFF1B130D),
    border = BorderStroke(1.2.dp, BurnishedGold.copy(alpha = 0.55f)),
    modifier =
      Modifier
        .fillMaxWidth()
        .zIndex(2f)
        .background(
          Brush.verticalGradient(listOf(Color(0xFF332113), Color(0xFF17100A)))
        )
        .windowInsetsPadding(WindowInsets.statusBars),
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      val isCompactWidth = maxWidth < 620.dp
      if (isCompactWidth) {
        // Two-row bar on vertical phones: ALL 5 resource counters remain 100% visible at all times
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
          verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().testTag("top_resources_row"),
          ) {
            ResourceCounterCapsule(ResourceType.WOOD, res.wood, s.resourceName(ResourceType.WOOD), "res_wood", Modifier.weight(1f))
            ResourceCounterCapsule(ResourceType.FOOD, res.food, s.resourceName(ResourceType.FOOD), "res_food", Modifier.weight(1f))
            ResourceCounterCapsule(ResourceType.GOLD, res.gold, s.resourceName(ResourceType.GOLD), "res_gold", Modifier.weight(1f))
            ResourceCounterCapsule(ResourceType.STONE, res.stone, s.resourceName(ResourceType.STONE), "res_stone", Modifier.weight(1f))
            PopulationCapsule(popCurrent, popCap, housed, "res_pop", Modifier.weight(1.1f))
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              RivalStatusPips(world = world)
              AgeBadgePill(ageText = s.ageShort(pState.age))
            }
            TopBarControlsRow(
              gameSpeed = gameSpeed,
              sfxEnabled = sfxEnabled,
              s = s,
              onCycleSpeed = onCycleSpeed,
              onToggleSfx = onToggleSfx,
              onOpenGuide = onOpenGuide,
              onPauseClick = onPauseClick,
            )
          }
        }
      } else {
        // Single sleek row on landscape & tablets: resources + controls both fully visible without clipping
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.testTag("top_resources_row"),
          ) {
            ResourceCounterCapsule(ResourceType.WOOD, res.wood, s.resourceName(ResourceType.WOOD), "res_wood")
            ResourceCounterCapsule(ResourceType.FOOD, res.food, s.resourceName(ResourceType.FOOD), "res_food")
            ResourceCounterCapsule(ResourceType.GOLD, res.gold, s.resourceName(ResourceType.GOLD), "res_gold")
            ResourceCounterCapsule(ResourceType.STONE, res.stone, s.resourceName(ResourceType.STONE), "res_stone")
            PopulationCapsule(popCurrent, popCap, housed, "res_pop")
          }

          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            RivalStatusPips(world = world)
            AgeBadgePill(ageText = s.ageShort(pState.age))
            TopBarControlsRow(
              gameSpeed = gameSpeed,
              sfxEnabled = sfxEnabled,
              s = s,
              onCycleSpeed = onCycleSpeed,
              onToggleSfx = onToggleSfx,
              onOpenGuide = onOpenGuide,
              onPauseClick = onPauseClick,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun RivalStatusPips(world: GameWorld) {
  val activeAis = world.aiFactions
  if (activeAis.size > 1) {
    Surface(
      color = Color(0xFF130D08),
      shape = RoundedCornerShape(6.dp),
      border = BorderStroke(1.dp, BronzeBorder.copy(alpha = 0.52f)),
      modifier = Modifier.testTag("hud_rivals_status"),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        for (ai in activeAis) {
          val alive = world.buildings.any { it.faction == ai }
          Box(
            modifier =
              Modifier
                .size(9.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (alive) Color(ai.bannerHex) else Color(0xFF3A342E))
          )
        }
      }
    }
  }
}

@Composable
private fun AgeBadgePill(ageText: String) {
  Surface(
    color = Color(0xFF332213),
    shape = RoundedCornerShape(6.dp),
    border = BorderStroke(1.dp, BurnishedGold.copy(alpha = 0.82f)),
  ) {
    Text(
      text = ageText,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp),
      color = BurnishedGold,
      maxLines = 1,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
    )
  }
}

@Composable
private fun TopBarControlsRow(
  gameSpeed: Float,
  sfxEnabled: Boolean,
  s: AppStrings,
  onCycleSpeed: () -> Unit,
  onToggleSfx: () -> Unit,
  onOpenGuide: () -> Unit,
  onPauseClick: () -> Unit,
) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
    HudIconPill(
      icon = Icons.Default.Speed,
      label = if (gameSpeed == 1.0f) "1x" else "${gameSpeed}x",
      tint = ParchmentCream,
      desc = s.gameSpeedPrefix,
      tag = "speed_button",
      onClick = onCycleSpeed,
    )
    HudIconPill(
      icon = if (sfxEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
      label = null,
      tint = if (sfxEnabled) BurnishedGold else AgedParchment.copy(alpha = 0.5f),
      desc = if (sfxEnabled) s.muteSfx else s.unmuteSfx,
      tag = "hud_sfx_toggle",
      onClick = onToggleSfx,
    )
    HudIconPill(
      icon = Icons.AutoMirrored.Filled.MenuBook,
      label = null,
      tint = BurnishedGold,
      desc = s.overmapAndFaq,
      tag = "hud_guide_button",
      onClick = onOpenGuide,
    )
    HudIconPill(
      icon = Icons.Default.Pause,
      label = null,
      tint = ParchmentCream,
      desc = s.battlePaused,
      tag = "pause_button",
      onClick = onPauseClick,
    )
  }
}

@Composable
private fun ResourceCounterCapsule(
  type: ResourceType,
  amount: Int,
  label: String,
  tag: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = Color(0xFF130D08),
    shape = RoundedCornerShape(6.dp),
    border = BorderStroke(1.dp, BronzeBorder.copy(alpha = 0.42f)),
    modifier = modifier.testTag(tag),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      ResourceGlyph(type = type, modifier = Modifier.size(14.dp))
      Spacer(modifier = Modifier.width(3.dp))
      Text(
        text = amount.toString(),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
        color = ParchmentCream,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun PopulationCapsule(
  current: Int,
  cap: Int,
  isHoused: Boolean,
  tag: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = if (isHoused) Color(0xFF421814) else Color(0xFF130D08),
    shape = RoundedCornerShape(6.dp),
    border = BorderStroke(1.dp, if (isHoused) EmberRed else BronzeBorder.copy(alpha = 0.42f)),
    modifier = modifier.testTag(tag),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = Icons.Default.Group,
        contentDescription = LocalAppStrings.current.resourceName(ResourceType.FOOD),
        tint = if (isHoused) Color(0xFFFF8878) else AgedParchment,
        modifier = Modifier.size(14.dp),
      )
      Spacer(modifier = Modifier.width(3.dp))
      Text(
        text = "$current/$cap",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
        color = if (isHoused) Color(0xFFFFA59A) else ParchmentCream,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun HudIconPill(
  icon: ImageVector,
  label: String?,
  tint: Color,
  desc: String,
  tag: String,
  onClick: () -> Unit,
) {
  Surface(
    color = CarvedWood,
    shape = RoundedCornerShape(6.dp),
    border = BorderStroke(1.dp, BronzeBorder.copy(alpha = 0.72f)),
    modifier =
      Modifier
        .height(32.dp)
        .clip(RoundedCornerShape(6.dp))
        .clickable(onClick = onClick)
        .testTag(tag),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 7.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(15.dp))
      if (label != null) {
        Spacer(modifier = Modifier.width(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = tint)
      }
    }
  }
}

@Composable
fun BottomCommandPanel(
  uiState: GameUiState,
  onJumpCamera: (Float, Float) -> Unit,
  onStartBuild: (BuildingType) -> Unit,
  onTrainUnit: (UnitType) -> Unit,
  onResearchTech: (TechType) -> Unit,
  onToggleAttackMove: () -> Unit,
  onStopUnits: () -> Unit,
  onAutoGatherVillagers: () -> Unit,
  onSelectAllMilitary: () -> Unit,
  onJumpToTc: () -> Unit,
  onCycleIdleVillager: () -> Unit,
  onCancelQueueItem: (Int) -> Unit = {},
  onMoveSelectedBuilding: () -> Unit = {},
  isLandscape: Boolean = false,
) {
  val world = uiState.world ?: return
  val s = LocalAppStrings.current
  val selectedUnits = world.units.filter { it.id in uiState.selectedUnitIds }
  val selectedBuilding = world.buildings.firstOrNull { it.id == uiState.selectedBuildingId }
  val hasVillagersSelected = selectedUnits.any { it.type == UnitType.VILLAGER }
  val minimapSize = if (isLandscape) 46.dp else 60.dp
  val buttonHeight = if (isLandscape) 30.dp else 36.dp

  Surface(
    color = Color(0xFF1A120C),
    border = BorderStroke(1.dp, BronzeBorder),
    modifier =
      Modifier
        .fillMaxWidth()
        .zIndex(2f)
        .background(
          Brush.verticalGradient(listOf(Color(0xFF231810), Color(0xFF150E09)))
        )
        .navigationBarsPadding()
        .testTag("bottom_command_panel"),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(androidx.compose.foundation.layout.IntrinsicSize.Min)
          .heightIn(
            min = if (isLandscape) 48.dp else 62.dp,
            max = if (isLandscape) 72.dp else 104.dp,
          )
          .padding(horizontal = 4.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        color = Color(0xFF100B07),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, BurnishedGold.copy(alpha = 0.85f)),
      ) {
        Box(modifier = Modifier.size(minimapSize)) {
          TacticalMinimap(
            world = world,
            camera = uiState.camera,
            onJumpCamera = onJumpCamera,
            modifier = Modifier.fillMaxSize().padding(1.5.dp).clip(RoundedCornerShape(4.dp)).testTag("tactical_minimap"),
          )
          Canvas(modifier = Modifier.fillMaxSize()) {
            val inset = 3.dp.toPx()
            val studR = 1.2.dp.toPx()
            val corners =
              listOf(
                Offset(inset, inset),
                Offset(size.width - inset, inset),
                Offset(inset, size.height - inset),
                Offset(size.width - inset, size.height - inset),
              )
            for (pt in corners) {
              drawCircle(Color(0xFF1A1109), studR + 0.5.dp.toPx(), pt)
              drawCircle(Color(0xFFE2BE68), studR, pt)
            }
          }
        }
      }

      Spacer(modifier = Modifier.width(4.dp))

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
      ) {
        when {
          hasVillagersSelected -> {
            val selectedVils = selectedUnits.filter { it.type == UnitType.VILLAGER }
            val lead = selectedVils.first()
            val taskText =
              when (lead.order) {
                UnitOrderType.IDLE -> s.taskIdle
                UnitOrderType.GATHER ->
                  if (lead.carryingType != null) "${s.taskGathering} ${s.resourceName(lead.carryingType)}"
                  else s.taskGathering
                UnitOrderType.BUILD ->
                  if (lead.buildQueue.isNotEmpty()) "${s.taskConstructing} (+${lead.buildQueue.size})"
                  else s.taskConstructing
                UnitOrderType.MOVE -> s.taskMoving
                else -> s.taskActive
              }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text =
                  if (selectedVils.size == 1) "${s.villagerSingular} • $taskText"
                  else "${selectedVils.size} ${s.villagersPlural} • $taskText",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 11.sp, fontWeight = FontWeight.Bold),
                color = BurnishedGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              Text(
                text = s.tapNodeOrBuild,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 10.sp),
                color = AgedParchment.copy(alpha = 0.85f),
                maxLines = 1,
              )
            }
            VillagerBuildStrip(
              world = world,
              pendingBuildType = uiState.pendingBuildType,
              buttonHeight = buttonHeight,
              isLandscape = isLandscape,
              onStartBuild = onStartBuild,
              onAutoGatherVillagers = onAutoGatherVillagers,
            )
          }
          selectedUnits.isNotEmpty() -> {
            val lead = selectedUnits.first()
            val def = GameDefinitions.unitDefs.getValue(lead.type)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text =
                  if (selectedUnits.size == 1) "${s.unitName(lead.type)} (${lead.hp}/${lead.maxHp} HP)"
                  else "${selectedUnits.size} ${s.armyLabel} (${s.unitName(lead.type)})",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 11.sp, fontWeight = FontWeight.Bold),
                color = BurnishedGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              Surface(
                color = Color(0xFF130D08),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(0.8.dp, BronzeBorder.copy(alpha = 0.4f)),
              ) {
                Text(
                  text = "ATK ${def.attack} • ARM ${def.meleeArmor}/${def.pierceArmor}",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 10.sp),
                  color = AgedParchment,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                )
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              CommandButton(
                title = if (uiState.attackMoveMode) s.tapTarget else s.attackMove,
                subtitleText = s.engageFoes,
                icon = Icons.Default.GpsFixed,
                highlighted = uiState.attackMoveMode,
                enabled = true,
                tag = "attack_move_button",
                buttonHeight = buttonHeight,
                onClick = onToggleAttackMove,
              )
              CommandButton(
                title = s.holdLabel,
                subtitleText = s.stopUnits,
                icon = Icons.Default.Stop,
                highlighted = false,
                enabled = true,
                tag = "stop_units_button",
                buttonHeight = buttonHeight,
                onClick = onStopUnits,
              )
              CommandButton(
                title = s.allArmy,
                subtitleText = s.rallyTroops,
                icon = Icons.Default.Shield,
                highlighted = false,
                enabled = true,
                tag = "command_all_army_button",
                buttonHeight = buttonHeight,
                onClick = onSelectAllMilitary,
              )
            }
          }
          selectedBuilding != null -> {
            BuildingCommandView(
              world = world,
              building = selectedBuilding,
              onTrainUnit = onTrainUnit,
              onResearchTech = onResearchTech,
              onCancelQueueItem = onCancelQueueItem,
              isMoveMode = uiState.pendingMoveBuildingId == selectedBuilding.id,
              onMoveBuilding = onMoveSelectedBuilding,
              buttonHeight = buttonHeight,
              isLandscape = isLandscape,
            )
          }
          else -> {
            val workerCount = world.units.count { it.faction == Faction.PLAYER && it.type == UnitType.VILLAGER }
            val idleCount = world.units.count { it.faction == Faction.PLAYER && it.type == UnitType.VILLAGER && it.order == UnitOrderType.IDLE }
            val armyCount = world.units.count { it.faction == Faction.PLAYER && it.type != UnitType.VILLAGER }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "${s.civName(world.playerState.civilization)} • ${s.ageName(world.playerState.age)}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 11.sp, fontWeight = FontWeight.Bold),
                color = BurnishedGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "${s.workersLabel} $workerCount • ${s.armyLabel} $armyCount",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 10.sp),
                color = AgedParchment,
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              CommandButton(
                title = s.townCenterAction,
                subtitleText = s.trainAndAges,
                icon = Icons.Default.AccountBalance,
                highlighted = false,
                enabled = true,
                tag = "hud_jump_tc_button",
                buttonHeight = buttonHeight,
                onClick = onJumpToTc,
              )
              CommandButton(
                title = if (idleCount > 0) "${s.idleLabel} ($idleCount)" else s.workersLabel,
                subtitleText = if (idleCount > 0) s.assignJob else s.focusWorker,
                icon = Icons.Default.Engineering,
                highlighted = idleCount > 0,
                enabled = true,
                tag = "hud_idle_worker_button",
                buttonHeight = buttonHeight,
                onClick = onCycleIdleVillager,
              )
              CommandButton(
                title = s.selectArmy,
                subtitleText = "$armyCount ${s.readySuffix}",
                icon = Icons.Default.Security,
                highlighted = false,
                enabled = armyCount > 0,
                tag = "hud_select_army_button",
                buttonHeight = buttonHeight,
                onClick = onSelectAllMilitary,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun VillagerBuildStrip(
  world: GameWorld,
  pendingBuildType: BuildingType?,
  buttonHeight: androidx.compose.ui.unit.Dp,
  isLandscape: Boolean = false,
  onStartBuild: (BuildingType) -> Unit,
  onAutoGatherVillagers: () -> Unit,
) {
  val s = LocalAppStrings.current
  var activeCategory by remember { mutableStateOf<BuildCategory?>(null) }
  var inspectedType by remember(activeCategory) { mutableStateOf<BuildingType?>(null) }
  val shownDescType = pendingBuildType ?: inspectedType
  val pState = world.playerState
  val needed = RtsEngine.mostNeededResource(world, Faction.PLAYER)
  val economyBuildings =
    listOf(
      BuildingType.HOUSE,
      BuildingType.MILL,
      BuildingType.FARM,
      BuildingType.LUMBER_CAMP,
      BuildingType.MINING_CAMP,
      BuildingType.UNIVERSITY,
      BuildingType.TOWN_CENTER,
    )
  val militaryBuildings =
    listOf(
      BuildingType.BARRACKS,
      BuildingType.ARCHERY_RANGE,
      BuildingType.STABLE,
      BuildingType.BLACKSMITH,
      BuildingType.WATCH_TOWER,
      BuildingType.CASTLE,
      BuildingType.WAR_FACTORY,
      BuildingType.AIRFIELD,
    )

  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
    if (shownDescType != null || activeCategory != null) {
      val infoText =
        if (shownDescType != null) {
          val radiusHint = if (shownDescType == BuildingType.TOWN_CENTER) s.tcExpandHint else s.buildRadiusHint
          "${s.buildingName(shownDescType)}: ${s.buildingDesc(shownDescType)} ($radiusHint)"
        } else {
          s.buildRadiusHint
        }
      Surface(
        color = Color(0xFF140D08),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(0.8.dp, BronzeBorder.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth().testTag("build_description_banner"),
      ) {
        Text(
          text = infoText,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 10.5.sp),
          color = ParchmentCream,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      when (activeCategory) {
        null -> {
          CommandButton(
            title = s.autoGather,
            subtitleText = "${s.needPrefix} ${s.resourceName(needed)}",
            icon = Icons.Default.Handyman,
            highlighted = true,
            enabled = true,
            tag = "auto_gather_button",
            buttonHeight = buttonHeight,
            onClick = onAutoGatherVillagers,
          )
          CommandButton(
            title = s.economyTitle,
            subtitleText = s.economySub,
            icon = Icons.Default.Home,
            highlighted = false,
            enabled = true,
            tag = "build_category_economy",
            buttonHeight = buttonHeight,
            onClick = { activeCategory = BuildCategory.ECONOMY },
          )
          CommandButton(
            title = s.militaryTitle,
            subtitleText = s.militarySub,
            icon = Icons.Default.Security,
            highlighted = false,
            enabled = true,
            tag = "build_category_military",
            buttonHeight = buttonHeight,
            onClick = { activeCategory = BuildCategory.MILITARY },
          )
        }
        else -> {
          val currentCat = activeCategory!!
          CommandButton(
            title = s.back,
            subtitleText = s.categoryName(currentCat),
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            highlighted = false,
            enabled = true,
            tag = "build_category_back",
            buttonHeight = buttonHeight,
            onClick = {
              activeCategory = null
              inspectedType = null
            },
          )
          val list = if (currentCat == BuildCategory.ECONOMY) economyBuildings else militaryBuildings
          for (type in list) {
            val def = GameDefinitions.buildingDefs.getValue(type)
            val cost = GameDefinitions.adjustedBuildingCost(type, pState.civilization)
            val ageOk = pState.age.index >= def.requiredAge.index
            val affordable = ageOk && pState.resources.canAfford(cost)

            CommandButton(
              title = s.buildingName(type),
              cost = if (ageOk) cost else null,
              subtitleText = if (ageOk) null else s.ageShort(def.requiredAge),
              icon = buildingIconFor(type),
              highlighted = shownDescType == type,
              enabled = affordable,
              tag = "build_${type.name.lowercase()}",
              buttonHeight = buttonHeight,
              onClick = {
                inspectedType = type
                onStartBuild(type)
              },
            )
          }
        }
      }
    }
  }
}

private fun buildingIconFor(type: BuildingType): ImageVector =
  when (type) {
    BuildingType.HOUSE -> Icons.Default.Home
    BuildingType.MILL -> Icons.Default.Storefront
    BuildingType.FARM -> Icons.Default.Grass
    BuildingType.LUMBER_CAMP -> Icons.Default.Carpenter
    BuildingType.MINING_CAMP -> Icons.Default.Hardware
    BuildingType.BARRACKS -> Icons.Default.Security
    BuildingType.ARCHERY_RANGE -> Icons.Default.GpsFixed
    BuildingType.STABLE -> Icons.AutoMirrored.Filled.DirectionsRun
    BuildingType.BLACKSMITH -> Icons.Default.Construction
    BuildingType.UNIVERSITY -> Icons.Default.School
    BuildingType.WATCH_TOWER -> Icons.Default.Visibility
    BuildingType.TOWN_CENTER -> Icons.Default.AccountBalance
    BuildingType.CASTLE -> Icons.Default.Castle
    BuildingType.WAR_FACTORY -> Icons.Default.PrecisionManufacturing
    BuildingType.AIRFIELD -> Icons.Default.Flight
  }

@Composable
private fun BuildingCommandView(
  world: GameWorld,
  building: Building,
  onTrainUnit: (UnitType) -> Unit,
  onResearchTech: (TechType) -> Unit,
  onCancelQueueItem: (Int) -> Unit = {},
  isMoveMode: Boolean = false,
  onMoveBuilding: () -> Unit = {},
  buttonHeight: androidx.compose.ui.unit.Dp = 68.dp,
  isLandscape: Boolean = false,
) {
  val s = LocalAppStrings.current
  val pState = world.playerState
  val headItem = building.queue.firstOrNull()
  val bTitle = s.buildingName(building.type)

  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text =
          if (building.isComplete) "$bTitle (${building.hp}/${building.maxHp})"
          else "$bTitle (${s.buildingProgressPrefix} ${(building.buildProgress * 100).toInt()}%)",
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 11.sp, fontWeight = FontWeight.Bold),
        color = BurnishedGold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      if (building.type == BuildingType.FARM && building.isComplete) {
        Surface(
          color = Color(0xFF283416),
          shape = RoundedCornerShape(4.dp),
          border = BorderStroke(0.8.dp, BurnishedGold.copy(alpha = 0.65f)),
        ) {
          Text(
            text = s.permanentLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 10.sp),
            color = ParchmentCream,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
          )
        }
      }
      if (building.queue.isNotEmpty()) {
        Surface(
          color = Color(0xFF2D2116),
          shape = RoundedCornerShape(4.dp),
          border = BorderStroke(0.8.dp, BurnishedGold.copy(alpha = 0.6f)),
        ) {
          Text(
            text = "${s.queueLabel} ${building.queue.size}/${RtsEngine.MAX_PRODUCTION_QUEUE}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 10.sp),
            color = ParchmentCream,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
          )
        }
      }
      if (building.faction == Faction.PLAYER) {
        Spacer(modifier = Modifier.width(4.dp))
        Surface(
          color = if (isMoveMode) Color(0xFF314D24) else CarvedWood,
          shape = RoundedCornerShape(4.dp),
          border = BorderStroke(0.8.dp, if (isMoveMode) BurnishedGold else BronzeBorder),
          modifier =
            Modifier
              .height(if (isLandscape) 18.dp else 22.dp)
              .clip(RoundedCornerShape(4.dp))
              .clickable(onClick = onMoveBuilding)
              .testTag("move_building_button"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Icon(
              imageVector = Icons.Default.OpenWith,
              contentDescription = s.moveLabel,
              tint = BurnishedGold,
              modifier = Modifier.size(10.dp),
            )
            Text(
              text = if (isMoveMode) s.movingLabel else s.moveLabel,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 10.sp, fontWeight = FontWeight.Bold),
              color = ParchmentCream,
            )
          }
        }
      }
    }

    if (headItem != null) {
      val label =
        when (headItem) {
          is ProductionItem.TrainUnit -> "${s.trainingPrefix} ${s.unitName(headItem.unitType)}"
          is ProductionItem.ResearchTech -> "${s.researchingPrefix} ${s.techName(headItem.techType)}"
        }
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          label,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 10.sp),
          color = AgedParchment,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        LinearProgressIndicator(
          progress = { (headItem.progress / headItem.totalTime).coerceIn(0f, 1f) },
          color = BurnishedGold,
          trackColor = DarkOak,
          modifier = Modifier.width(if (isLandscape) 48.dp else 60.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
        )
        Row(
          modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          building.queue.forEachIndexed { idx, item ->
            val chipName =
              when (item) {
                is ProductionItem.TrainUnit -> s.unitName(item.unitType)
                is ProductionItem.ResearchTech -> s.techName(item.techType)
              }
            Surface(
              color = if (idx == 0) Color(0xFF3B2816) else Color(0xFF231810),
              shape = RoundedCornerShape(4.dp),
              border = BorderStroke(0.8.dp, if (idx == 0) BurnishedGold else BronzeBorder.copy(alpha = 0.65f)),
              modifier =
                Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .clickable { onCancelQueueItem(idx) }
                  .testTag("queue_chip_$idx"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
              ) {
                Text(
                  text = "#${idx + 1} $chipName",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, lineHeight = 9.5.sp),
                  color = ParchmentCream,
                  maxLines = 1,
                )
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = s.cancel,
                  tint = AgedParchment,
                  modifier = Modifier.size(9.dp),
                )
              }
            }
          }
        }
      }
    }

    if (!building.isComplete) {
      Text(
        s.tapSiteToBuild,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = AgedParchment,
        maxLines = 1,
      )
      return
    }

    val trainableUnits =
      GameDefinitions.unitDefs.values
        .filter { it.trainedAt == building.type }
        .filter { it.requiredTech == null || it.requiredTech in pState.researchedTechs }
    val researchableTechs =
      GameDefinitions.techDefs.values
        .filter { it.researchedAt == building.type }
        .filter { it.type !in pState.researchedTechs && it.type !in pState.researchingTechs }
        .filter { techDef ->
          when (techDef.type) {
            TechType.ADVANCE_FEUDAL -> pState.age == Age.DARK_AGE
            TechType.ADVANCE_CASTLE -> pState.age == Age.FEUDAL_AGE
            TechType.ADVANCE_IMPERIAL -> pState.age == Age.CASTLE_AGE
            TechType.ADVANCE_INDUSTRIAL -> pState.age == Age.IMPERIAL_AGE
            TechType.ADVANCE_MODERN -> pState.age == Age.INDUSTRIAL_AGE
            else -> true
          }
        }

    if (trainableUnits.isEmpty() && researchableTechs.isEmpty()) {
      Text(
        text = s.buildingDesc(building.type),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = AgedParchment,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      return
    }

    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      trainableUnits.forEach { uDef ->
        val cost = GameDefinitions.adjustedUnitCost(uDef.type, pState.civilization)
        val ageOk = pState.age.index >= uDef.requiredAge.index
        val canAfford = ageOk && pState.resources.canAfford(cost)
        CommandButton(
          title = s.unitName(uDef.type),
          cost = if (ageOk) cost else null,
          subtitleText = if (ageOk) null else s.ageShort(uDef.requiredAge),
          icon =
            when {
              uDef.type == UnitType.VILLAGER -> Icons.Default.Engineering
              uDef.isFlying -> Icons.Default.Flight
              uDef.isArmored -> Icons.Default.PrecisionManufacturing
              uDef.isRanged -> Icons.Default.GpsFixed
              uDef.isCavalry -> Icons.AutoMirrored.Filled.DirectionsRun
              else -> Icons.Default.Shield
            },
          highlighted = false,
          enabled = canAfford,
          tag = "train_${uDef.type.name.lowercase()}",
          buttonHeight = buttonHeight,
          onClick = { onTrainUnit(uDef.type) },
        )
      }

      researchableTechs.forEach { tDef ->
        val reqStatus = GameDefinitions.missingTechRequirements(world, Faction.PLAYER, tDef.type)
        val canAfford = reqStatus.isMet && pState.resources.canAfford(tDef.cost)
        val reqHint =
          when {
            reqStatus.isMet -> null
            reqStatus.missingAge != null -> s.ageShort(reqStatus.missingAge)
            reqStatus.missingBuildings.isNotEmpty() -> "${s.reqPrefix} ${s.buildingName(reqStatus.missingBuildings.first())}"
            reqStatus.missingTechs.isNotEmpty() -> "${s.reqPrefix} ${s.techName(reqStatus.missingTechs.first())}"
            else -> null
          }
        CommandButton(
          title = s.techName(tDef.type),
          cost = if (reqStatus.isMet) tDef.cost else null,
          subtitleText = reqHint,
          icon = if (tDef.type.name.startsWith("ADVANCE_")) Icons.Default.AccountBalance else Icons.Default.Build,
          highlighted = tDef.type.name.startsWith("ADVANCE_") && reqStatus.isMet,
          enabled = canAfford,
          tag = "tech_${tDef.type.name.lowercase()}",
          buttonHeight = buttonHeight,
          onClick = { onResearchTech(tDef.type) },
        )
      }
    }
  }
}

@Composable
private fun CommandButton(
  title: String,
  icon: ImageVector,
  highlighted: Boolean,
  enabled: Boolean,
  tag: String,
  onClick: () -> Unit,
  cost: ResourceCost? = null,
  subtitleText: String? = null,
  buttonHeight: androidx.compose.ui.unit.Dp = 68.dp,
) {
  val bg =
    when {
      highlighted -> Color(0xFF314D24)
      enabled -> CarvedWood
      else -> AgedTimber.copy(alpha = 0.72f)
    }
  val borderColor =
    when {
      highlighted -> ForestEmerald
      enabled -> BronzeBorder
      else -> BronzeBorder.copy(alpha = 0.35f)
    }

  val ultraCompact = buttonHeight <= 32.dp
  Surface(
    color = bg,
    shape = RoundedCornerShape(5.dp),
    border = BorderStroke(0.8.dp, borderColor),
    modifier =
      Modifier
        .widthIn(min = if (ultraCompact) 54.dp else 62.dp, max = if (ultraCompact) 80.dp else 88.dp)
        .height(buttonHeight)
        .clip(RoundedCornerShape(5.dp))
        .clickable(onClick = onClick)
        .testTag(tag),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
      verticalArrangement = Arrangement.Center,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (enabled) BurnishedGold else AgedParchment.copy(alpha = 0.45f),
          modifier = Modifier.size(if (ultraCompact) 10.dp else 11.dp),
        )
        Spacer(modifier = Modifier.width(2.5.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge.copy(
            fontSize = if (ultraCompact) 9.sp else 9.5.sp,
            lineHeight = if (ultraCompact) 10.sp else 11.sp,
          ),
          color = if (enabled) ParchmentCream else AgedParchment.copy(alpha = 0.5f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (cost != null) {
        ResourceCostBadges(
          cost = cost,
          affordable = enabled,
          iconSize = if (ultraCompact) 8.dp else 9.dp,
          fontSize = if (ultraCompact) 8.5.sp else 9.sp,
          itemSpacing = 3.dp,
        )
      } else if (subtitleText != null) {
        Text(
          text = subtitleText,
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = if (ultraCompact) 8.sp else 8.5.sp,
            lineHeight = if (ultraCompact) 9.sp else 10.sp,
          ),
          color = if (enabled) BurnishedGold else EmberRed.copy(alpha = 0.90f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
