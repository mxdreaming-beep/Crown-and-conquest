package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
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
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      // Intuitive Resource Capsules
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
      ) {
        ResourceCounterCapsule(ResourceType.WOOD, res.wood, "Wood")
        ResourceCounterCapsule(ResourceType.FOOD, res.food, "Food")
        ResourceCounterCapsule(ResourceType.GOLD, res.gold, "Gold")
        ResourceCounterCapsule(ResourceType.STONE, res.stone, "Stone")
        PopulationCapsule(popCurrent, popCap, housed)
      }

      Spacer(modifier = Modifier.width(6.dp))

      // Age Crest + Multi-AI Alive Pips + Speed + SFX + Codex/FAQ + Pause
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
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
                      .background(
                        if (alive) Color(ai.bannerHex) else Color(0xFF3A342E)
                      )
                )
              }
            }
          }
        }
        Surface(
          color = Color(0xFF332213),
          shape = RoundedCornerShape(6.dp),
          border = BorderStroke(1.dp, BurnishedGold.copy(alpha = 0.82f)),
        ) {
          Text(
            text = pState.age.displayName.replace(" Age", ""),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
            color = BurnishedGold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
          )
        }

        HudIconPill(
          icon = Icons.Default.Speed,
          label = if (gameSpeed == 1.0f) "1x" else "${gameSpeed}x",
          tint = ParchmentCream,
          desc = "Game Speed",
          tag = "speed_button",
          onClick = onCycleSpeed,
        )

        HudIconPill(
          icon = if (sfxEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
          label = null,
          tint = if (sfxEnabled) BurnishedGold else AgedParchment.copy(alpha = 0.5f),
          desc = if (sfxEnabled) "Mute SFX" else "Unmute SFX",
          tag = "hud_sfx_toggle",
          onClick = onToggleSfx,
        )

        HudIconPill(
          icon = Icons.AutoMirrored.Filled.MenuBook,
          label = null,
          tint = BurnishedGold,
          desc = "Overmap & FAQ",
          tag = "hud_guide_button",
          onClick = onOpenGuide,
        )

        HudIconPill(
          icon = Icons.Default.Pause,
          label = null,
          tint = ParchmentCream,
          desc = "Pause Menu",
          tag = "pause_button",
          onClick = onPauseClick,
        )
      }
    }
  }
}

@Composable
private fun ResourceCounterCapsule(type: ResourceType, amount: Int, label: String) {
  Surface(
    color = Color(0xFF130D08),
    shape = RoundedCornerShape(6.dp),
    border = BorderStroke(1.dp, BronzeBorder.copy(alpha = 0.42f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ResourceGlyph(type = type, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = amount.toString(),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
        color = ParchmentCream,
      )
    }
  }
}

@Composable
private fun PopulationCapsule(current: Int, cap: Int, isHoused: Boolean) {
  Surface(
    color = if (isHoused) Color(0xFF421814) else Color(0xFF130D08),
    shape = RoundedCornerShape(6.dp),
    border = BorderStroke(1.dp, if (isHoused) EmberRed else BronzeBorder.copy(alpha = 0.42f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Default.Group,
        contentDescription = "Population",
        tint = if (isHoused) Color(0xFFFF8878) else AgedParchment,
        modifier = Modifier.size(15.dp),
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = "$current/$cap",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
        color = if (isHoused) Color(0xFFFFA59A) else ParchmentCream,
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
) {
  val world = uiState.world ?: return
  val selectedUnits = world.units.filter { it.id in uiState.selectedUnitIds }
  val selectedBuilding = world.buildings.firstOrNull { it.id == uiState.selectedBuildingId }
  val hasVillagersSelected = selectedUnits.any { it.type == UnitType.VILLAGER }

  Surface(
    color = Color(0xFF1A120C),
    border = BorderStroke(1.5.dp, BronzeBorder),
    modifier =
      Modifier
        .fillMaxWidth()
        .zIndex(2f)
        .background(
          Brush.verticalGradient(listOf(Color(0xFF231810), Color(0xFF150E09)))
        )
        .navigationBarsPadding(),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(androidx.compose.foundation.layout.IntrinsicSize.Min)
          .heightIn(min = 138.dp, max = 186.dp)
          .padding(horizontal = 8.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        color = Color(0xFF100B07),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(2.dp, BurnishedGold.copy(alpha = 0.85f)),
      ) {
        Box(modifier = Modifier.size(122.dp)) {
          TacticalMinimap(
            world = world,
            camera = uiState.camera,
            onJumpCamera = onJumpCamera,
            modifier = Modifier.fillMaxSize().padding(3.dp).clip(RoundedCornerShape(5.dp)).testTag("tactical_minimap"),
          )
          // Ornate riveted bronze corner studs on minimap frame
          Canvas(modifier = Modifier.fillMaxSize()) {
            val inset = 5.dp.toPx()
            val studR = 2.1.dp.toPx()
            val corners =
              listOf(
                Offset(inset, inset),
                Offset(size.width - inset, inset),
                Offset(inset, size.height - inset),
                Offset(size.width - inset, size.height - inset),
              )
            for (pt in corners) {
              drawCircle(Color(0xFF1A1109), studR + 0.8.dp.toPx(), pt)
              drawCircle(Color(0xFFE2BE68), studR, pt)
            }
          }
        }
      }

      Spacer(modifier = Modifier.width(10.dp))

      // Command Inspector & Action Grid
      Column(
        modifier = Modifier.weight(1f).heightIn(min = 122.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        when {
          hasVillagersSelected -> {
            val selectedVils = selectedUnits.filter { it.type == UnitType.VILLAGER }
            val lead = selectedVils.first()
            val taskText =
              when (lead.order) {
                UnitOrderType.IDLE -> "Idle"
                UnitOrderType.GATHER -> if (lead.carryingType != null) "Gathering ${lead.carryingType.displayName}" else "Gathering"
                UnitOrderType.BUILD ->
                  if (lead.buildQueue.isNotEmpty()) "Constructing (+${lead.buildQueue.size} queued)" else "Constructing"
                UnitOrderType.MOVE -> "Moving"
                else -> "Active"
              }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = if (selectedVils.size == 1) "Villager • $taskText" else "${selectedVils.size} Villagers • $taskText",
                style = MaterialTheme.typography.labelLarge,
                color = BurnishedGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "Tap node or build",
                style = MaterialTheme.typography.labelSmall,
                color = AgedParchment.copy(alpha = 0.85f),
              )
            }
            VillagerBuildStrip(
              world = world,
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
                text = if (selectedUnits.size == 1) "${lead.type.displayName} (${lead.hp}/${lead.maxHp} HP)" else "${selectedUnits.size} Units (${lead.type.displayName})",
                style = MaterialTheme.typography.labelLarge,
                color = BurnishedGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Surface(
                color = Color(0xFF130D08),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, BronzeBorder.copy(alpha = 0.4f)),
              ) {
                Text(
                  text = "Atk ${def.attack} • Arm ${def.meleeArmor}/${def.pierceArmor}",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                  color = AgedParchment,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              CommandButton(
                title = if (uiState.attackMoveMode) "Tap Target..." else "Attack Move",
                subtitleText = "Engage foes",
                icon = Icons.Default.GpsFixed,
                highlighted = uiState.attackMoveMode,
                enabled = true,
                tag = "attack_move_button",
                onClick = onToggleAttackMove,
              )
              CommandButton(
                title = "Hold",
                subtitleText = "Stop units",
                icon = Icons.Default.Stop,
                highlighted = false,
                enabled = true,
                tag = "stop_units_button",
                onClick = onStopUnits,
              )
              CommandButton(
                title = "All Army",
                subtitleText = "Rally troops",
                icon = Icons.Default.Shield,
                highlighted = false,
                enabled = true,
                tag = "command_all_army_button",
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
                text = "${world.playerState.civilization.displayName} • ${world.playerState.age.displayName}",
                style = MaterialTheme.typography.labelLarge,
                color = BurnishedGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "Vils $workerCount • Army $armyCount",
                style = MaterialTheme.typography.labelSmall,
                color = AgedParchment,
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              CommandButton(
                title = "Town Center",
                subtitleText = "Train & Ages",
                icon = Icons.Default.AccountBalance,
                highlighted = false,
                enabled = true,
                tag = "hud_jump_tc_button",
                onClick = onJumpToTc,
              )
              CommandButton(
                title = if (idleCount > 0) "Idle ($idleCount)" else "Workers",
                subtitleText = if (idleCount > 0) "Assign job" else "Focus worker",
                icon = Icons.Default.Engineering,
                highlighted = idleCount > 0,
                enabled = true,
                tag = "hud_idle_worker_button",
                onClick = onCycleIdleVillager,
              )
              CommandButton(
                title = "Select Army",
                subtitleText = "$armyCount ready",
                icon = Icons.Default.Security,
                highlighted = false,
                enabled = armyCount > 0,
                tag = "hud_select_army_button",
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
  onStartBuild: (BuildingType) -> Unit,
  onAutoGatherVillagers: () -> Unit,
) {
  var activeCategory by remember { mutableStateOf<BuildCategory?>(null) }
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

  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    when (activeCategory) {
      null -> {
        CommandButton(
          title = "Auto Gather",
          subtitleText = "Need: ${needed.displayName}",
          icon = Icons.Default.Handyman,
          highlighted = true,
          enabled = true,
          tag = "auto_gather_button",
          onClick = onAutoGatherVillagers,
        )
        CommandButton(
          title = "Economy",
          subtitleText = "House, Farm, TC+",
          icon = Icons.Default.Home,
          highlighted = false,
          enabled = true,
          tag = "build_category_economy",
          onClick = { activeCategory = BuildCategory.ECONOMY },
        )
        CommandButton(
          title = "Military",
          subtitleText = "Barracks, Factory+",
          icon = Icons.Default.Security,
          highlighted = false,
          enabled = true,
          tag = "build_category_military",
          onClick = { activeCategory = BuildCategory.MILITARY },
        )
      }
      else -> {
        val currentCat = activeCategory!!
        CommandButton(
          title = "Back",
          subtitleText = currentCat.displayName,
          icon = Icons.AutoMirrored.Filled.ArrowBack,
          highlighted = false,
          enabled = true,
          tag = "build_category_back",
          onClick = { activeCategory = null },
        )
        val list = if (currentCat == BuildCategory.ECONOMY) economyBuildings else militaryBuildings
        for (type in list) {
          val def = GameDefinitions.buildingDefs.getValue(type)
          val cost = GameDefinitions.adjustedBuildingCost(type, pState.civilization)
          val ageOk = pState.age.index >= def.requiredAge.index
          val affordable = ageOk && pState.resources.canAfford(cost)

          CommandButton(
            title = type.displayName,
            cost = if (ageOk) cost else null,
            subtitleText = if (ageOk) null else def.requiredAge.displayName.replace(" Age", ""),
            icon = buildingIconFor(type),
            highlighted = false,
            enabled = affordable,
            tag = "build_${type.name.lowercase()}",
            onClick = { onStartBuild(type) },
          )
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
) {
  val pState = world.playerState
  val headItem = building.queue.firstOrNull()

  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = if (building.isComplete) "${building.type.displayName} (${building.hp}/${building.maxHp})" else "${building.type.displayName} (Building ${(building.buildProgress * 100).toInt()}%)",
        style = MaterialTheme.typography.labelLarge,
        color = BurnishedGold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      if (building.type == BuildingType.FARM && building.isComplete) {
        Surface(
          color = Color(0xFF283416),
          shape = RoundedCornerShape(4.dp),
          border = BorderStroke(1.dp, BurnishedGold.copy(alpha = 0.65f)),
        ) {
          Text(
            text = "Permanent",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = ParchmentCream,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
          )
        }
      }
      if (building.queue.isNotEmpty()) {
        Surface(
          color = Color(0xFF2D2116),
          shape = RoundedCornerShape(4.dp),
          border = BorderStroke(1.dp, BurnishedGold.copy(alpha = 0.6f)),
        ) {
          Text(
            text = "Queue ${building.queue.size}/${RtsEngine.MAX_PRODUCTION_QUEUE}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = ParchmentCream,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
          )
        }
      }
      if (building.faction == Faction.PLAYER) {
        Spacer(modifier = Modifier.width(6.dp))
        Surface(
          color = if (isMoveMode) Color(0xFF314D24) else CarvedWood,
          shape = RoundedCornerShape(6.dp),
          border = BorderStroke(1.dp, if (isMoveMode) BurnishedGold else BronzeBorder),
          modifier =
            Modifier
              .height(28.dp)
              .clip(RoundedCornerShape(6.dp))
              .clickable(onClick = onMoveBuilding)
              .testTag("move_building_button"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Default.OpenWith,
              contentDescription = "Move Building",
              tint = BurnishedGold,
              modifier = Modifier.size(13.dp),
            )
            Text(
              text = if (isMoveMode) "Moving..." else "Move",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
              color = ParchmentCream,
            )
          }
        }
      }
    }

    if (headItem != null) {
      val label =
        when (headItem) {
          is ProductionItem.TrainUnit -> "Training ${headItem.unitType.displayName}"
          is ProductionItem.ResearchTech -> "Researching ${headItem.techType.displayName}"
        }
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AgedParchment, maxLines = 1, overflow = TextOverflow.Ellipsis)
        LinearProgressIndicator(
          progress = { (headItem.progress / headItem.totalTime).coerceIn(0f, 1f) },
          color = BurnishedGold,
          trackColor = DarkOak,
          modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        building.queue.forEachIndexed { idx, item ->
          val chipName =
            when (item) {
              is ProductionItem.TrainUnit -> item.unitType.displayName
              is ProductionItem.ResearchTech -> item.techType.displayName
            }
          Surface(
            color = if (idx == 0) Color(0xFF3B2816) else Color(0xFF231810),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, if (idx == 0) BurnishedGold else BronzeBorder.copy(alpha = 0.65f)),
            modifier =
              Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { onCancelQueueItem(idx) }
                .testTag("queue_chip_$idx"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              Text(
                text = "#${idx + 1} $chipName",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = ParchmentCream,
                maxLines = 1,
              )
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancel queued item",
                tint = AgedParchment,
                modifier = Modifier.size(11.dp),
              )
            }
          }
        }
      }
    }

    if (!building.isComplete) {
      Text("Select a Villager and tap this site to construct.", style = MaterialTheme.typography.bodyMedium, color = AgedParchment)
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
        text = GameDefinitions.buildingDefs.getValue(building.type).description,
        style = MaterialTheme.typography.bodyMedium,
        color = AgedParchment,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      return
    }

    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      trainableUnits.forEach { uDef ->
        val cost = GameDefinitions.adjustedUnitCost(uDef.type, pState.civilization)
        val ageOk = pState.age.index >= uDef.requiredAge.index
        val canAfford = ageOk && pState.resources.canAfford(cost)
        CommandButton(
          title = uDef.type.displayName,
          cost = if (ageOk) cost else null,
          subtitleText = if (ageOk) null else uDef.requiredAge.displayName.replace(" Age", ""),
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
          onClick = { onTrainUnit(uDef.type) },
        )
      }

      researchableTechs.forEach { tDef ->
        val ageOk = pState.age.index >= tDef.requiredAge.index
        val canAfford = ageOk && pState.resources.canAfford(tDef.cost)
        CommandButton(
          title = tDef.type.displayName,
          cost = if (ageOk) tDef.cost else null,
          subtitleText = if (ageOk) null else tDef.requiredAge.displayName.replace(" Age", ""),
          icon = if (tDef.type.name.startsWith("ADVANCE_")) Icons.Default.AccountBalance else Icons.Default.Build,
          highlighted = tDef.type.name.startsWith("ADVANCE_"),
          enabled = canAfford,
          tag = "tech_${tDef.type.name.lowercase()}",
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

  Surface(
    color = bg,
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(1.dp, borderColor),
    modifier =
      Modifier
        .width(108.dp)
        .height(70.dp)
        .clip(RoundedCornerShape(8.dp))
        .clickable(onClick = onClick)
        .testTag(tag),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (enabled) BurnishedGold else AgedParchment.copy(alpha = 0.45f),
          modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
          color = if (enabled) ParchmentCream else AgedParchment.copy(alpha = 0.5f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (cost != null) {
        ResourceCostBadges(cost = cost, affordable = enabled, iconSize = 11.dp)
      } else if (subtitleText != null) {
        Text(
          text = subtitleText,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
          color = if (enabled) BurnishedGold else EmberRed.copy(alpha = 0.85f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
