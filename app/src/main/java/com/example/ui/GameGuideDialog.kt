package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.game.model.Age
import com.example.game.model.BuildCategory
import com.example.game.model.Civilization
import com.example.game.model.Difficulty
import com.example.game.model.GameDefinitions
import com.example.game.model.MapType
import com.example.game.model.TechType
import com.example.ui.theme.AgedParchment
import com.example.ui.theme.AgedTimber
import com.example.ui.theme.BronzeBorder
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.CarvedWood
import com.example.ui.theme.DarkOak
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.ParchmentCream
import com.example.ui.theme.PlayerBlue

private enum class GuideTab(val label: String, val icon: ImageVector) {
  OVERMAP("Ages Overmap", Icons.Default.AccountBalance),
  COSTS("Costs & Units", Icons.Default.Build),
  CIVS("Civilizations", Icons.Default.Flag),
  FAQ("FAQ & Controls", Icons.AutoMirrored.Filled.HelpOutline),
}

@Composable
fun GameGuideDialog(
  onDismiss: () -> Unit,
  initialPlayerCiv: Civilization? = null,
) {
  var activeTab by remember { mutableStateOf(GuideTab.OVERMAP) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      color = DarkOak,
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(2.dp, BurnishedGold),
      modifier =
        Modifier
          .fillMaxWidth(0.95f)
          .fillMaxHeight(0.88f)
          .testTag("guide_dialog"),
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Hero Banner Header
        Box(modifier = Modifier.fillMaxWidth().height(112.dp)) {
          Image(
            painter = painterResource(id = R.drawable.img_codex_banner),
            contentDescription = "Crown & Conquest Kingdom Overmap",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier =
              Modifier
                .fillMaxSize()
                .background(
                  Brush.verticalGradient(
                    listOf(Color(0xAA160E08), Color(0x66160E08), Color(0xF21A120C))
                  )
                )
          )
          Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                color = Color(0xCC24180F),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.2.dp, BurnishedGold),
                modifier = Modifier.size(38.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = BurnishedGold,
                    modifier = Modifier.size(22.dp),
                  )
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Royal Codex & Overmap",
                  style = MaterialTheme.typography.titleLarge,
                  color = BurnishedGold,
                )
                Text(
                  text = "Age Unlocks • Building & Unit Costs • Counters • FAQ",
                  style = MaterialTheme.typography.labelSmall,
                  color = ParchmentCream,
                )
              }
            }
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.size(48.dp).testTag("close_guide_button"),
            ) {
              Icon(Icons.Default.Close, contentDescription = "Close Codex", tint = BurnishedGold)
            }
          }
        }

        // Navigation Tab Bar
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .background(AgedTimber)
              .padding(horizontal = 10.dp, vertical = 8.dp)
              .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          GuideTab.entries.forEach { tab ->
            val selected = activeTab == tab
            Surface(
              color = if (selected) BurnishedGold else CarvedWood,
              shape = RoundedCornerShape(20.dp),
              border = BorderStroke(1.dp, if (selected) ParchmentCream else BronzeBorder),
              modifier =
                Modifier
                  .height(38.dp)
                  .clip(RoundedCornerShape(20.dp))
                  .clickable { activeTab = tab }
                  .testTag("guide_tab_${tab.name.lowercase()}"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Icon(
                  imageVector = tab.icon,
                  contentDescription = null,
                  tint = if (selected) DarkOak else BurnishedGold,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = tab.label,
                  style = MaterialTheme.typography.labelLarge,
                  color = if (selected) DarkOak else ParchmentCream,
                )
              }
            }
          }
        }

        HorizontalDivider(color = BronzeBorder, thickness = 1.dp)

        // Scrollable Tab Content
        Column(
          modifier =
            Modifier
              .weight(1f)
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          when (activeTab) {
            GuideTab.OVERMAP -> AgesOvermapTab()
            GuideTab.COSTS -> CostsAndUnitsTab()
            GuideTab.CIVS -> CivilizationsTab(initialPlayerCiv)
            GuideTab.FAQ -> FaqAndControlsTab()
          }
        }
      }
    }
  }
}

@Composable
private fun AgesOvermapTab() {
  SectionHeader("1. Gameplay Overmap", "Advance through 6 Ages at your Town Center to evolve from medieval steel to tanks and jet aircraft.")

  // Progression Ribbon
  Surface(
    color = CarvedWood,
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, BronzeBorder),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Age.entries.forEachIndexed { idx, age ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Surface(
            color = when (age) {
              Age.DARK_AGE -> Color(0xFF4D3520)
              Age.FEUDAL_AGE -> Color(0xFF2D5933)
              Age.CASTLE_AGE -> Color(0xFF234D82)
              Age.IMPERIAL_AGE -> Color(0xFF6B236E)
              Age.INDUSTRIAL_AGE -> Color(0xFF6F4821)
              Age.MODERN_AGE -> Color(0xFF1F5D6B)
            },
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.2.dp, BurnishedGold),
          ) {
            Text(
              text = age.displayName.replace(" Age", ""),
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = ParchmentCream,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
          Spacer(modifier = Modifier.height(3.dp))
          val costLabel =
            when (age) {
              Age.DARK_AGE -> "Start"
              Age.FEUDAL_AGE -> "500F • 26s"
              Age.CASTLE_AGE -> "800F 200G • 32s"
              Age.IMPERIAL_AGE -> "1000F 800G • 36s"
              Age.INDUSTRIAL_AGE -> "1100F 900G • 38s"
              Age.MODERN_AGE -> "1300F 1150G • 42s"
            }
          Text(costLabel, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = AgedParchment)
        }
        if (idx < Age.entries.lastIndex) {
          Text("→", color = BurnishedGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
      }
    }
  }

  // Detailed Age Cards derived directly from GameDefinitions
  Age.entries.forEach { age ->
    val advanceTech =
      when (age) {
        Age.DARK_AGE -> null
        Age.FEUDAL_AGE -> GameDefinitions.techDefs.getValue(TechType.ADVANCE_FEUDAL)
        Age.CASTLE_AGE -> GameDefinitions.techDefs.getValue(TechType.ADVANCE_CASTLE)
        Age.IMPERIAL_AGE -> GameDefinitions.techDefs.getValue(TechType.ADVANCE_IMPERIAL)
        Age.INDUSTRIAL_AGE -> GameDefinitions.techDefs.getValue(TechType.ADVANCE_INDUSTRIAL)
        Age.MODERN_AGE -> GameDefinitions.techDefs.getValue(TechType.ADVANCE_MODERN)
      }
    val unlockedBuildings = GameDefinitions.buildingDefs.values.filter { it.requiredAge == age }
    val unlockedUnits = GameDefinitions.unitDefs.values.filter { it.requiredAge == age }
    val unlockedTechs = GameDefinitions.techDefs.values.filter { it.requiredAge == age && !it.type.name.startsWith("ADVANCE_") }

    val badgeColor =
      when (age) {
        Age.DARK_AGE -> Color(0xFF7D532B)
        Age.FEUDAL_AGE -> ForestEmerald
        Age.CASTLE_AGE -> PlayerBlue
        Age.IMPERIAL_AGE -> Color(0xFF9C3AA8)
        Age.INDUSTRIAL_AGE -> Color(0xFFB56E2C)
        Age.MODERN_AGE -> Color(0xFF28899C)
      }

    Surface(
      color = AgedTimber,
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.2.dp, badgeColor.copy(alpha = 0.85f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(color = badgeColor, shape = RoundedCornerShape(6.dp)) {
              Text(
                text = "AGE ${age.index + 1}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = ParchmentCream,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
            Text(age.displayName, style = MaterialTheme.typography.titleMedium, color = BurnishedGold)
          }
          if (advanceTech != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              ResourceCostBadges(cost = advanceTech.cost, affordable = true, iconSize = 11.dp)
              Text("${advanceTech.researchTimeSec.toInt()}s", style = MaterialTheme.typography.labelSmall, color = AgedParchment)
            }
          } else {
            Text("Starting Era", style = MaterialTheme.typography.labelSmall, color = AgedParchment)
          }
        }

        val summary =
          when (age) {
            Age.DARK_AGE -> "Establish lumber, gold/stone mining, permanent farms, houses (+5 Pop), and early Barracks."
            Age.FEUDAL_AGE -> "Unlock Archery Range, Stable, Blacksmith upgrades, Watch Towers, Spearmen, Skirmishers & Archers."
            Age.CASTLE_AGE -> "Build Stone Castles (+20 Pop & arrow volleys), extra Town Centers, Knights, Crossbowmen & Siege Rams."
            Age.IMPERIAL_AGE -> "Empire Power Spike: ALL existing and newly trained units immediately gain +20% HP & +2 Attack!"
            Age.INDUSTRIAL_AGE -> "Enter mechanized warfare: War Factory trains Battle Tanks & Field Artillery alongside Riflemen & MG Troopers (+2 Attack)."
            Age.MODERN_AGE -> "Command the skies & heavy armor: Airfield deploys Fighter Jets, Attack Gunships & Heavy Bombers beside Heavy Tanks, MLRS Batteries, Assault Marines, Snipers & Rocket Troopers (+3 Attack)."
          }
        Text(summary, style = MaterialTheme.typography.bodyMedium, color = ParchmentCream)

        if (unlockedBuildings.isNotEmpty()) {
          OvermapDetailLine("Buildings", unlockedBuildings.joinToString(", ") { "${it.type.displayName} (${it.cost.summary()})" })
        }
        if (unlockedUnits.isNotEmpty()) {
          OvermapDetailLine("Units", unlockedUnits.joinToString(", ") { "${it.type.displayName} (${it.cost.summary()})" })
        }
        if (unlockedTechs.isNotEmpty()) {
          OvermapDetailLine("Upgrades", unlockedTechs.joinToString(", ") { "${it.type.displayName} (${it.cost.summary()})" })
        }
      }
    }
  }

  SectionHeader(
    "2. Battlegrounds, Multi-AI War & Victory",
    "Configure 1–3 AI rival empires on Standard (60×60), Large (76×76), or Huge (92×92) maps. Win by razing every rival's buildings!",
  )
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    MapType.entries.forEach { map ->
      Surface(
        color = CarvedWood,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BronzeBorder.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          Text(map.displayName, style = MaterialTheme.typography.titleSmall, color = BurnishedGold)
          Text(map.description, style = MaterialTheme.typography.bodySmall, color = AgedParchment)
        }
      }
    }
  }
}

@Composable
private fun CostsAndUnitsTab() {
  SectionHeader("Economy Buildings", "Built by Villagers under the Economy category.")
  GameDefinitions.buildingDefs.values
    .filter { it.category == BuildCategory.ECONOMY }
    .forEach { b -> BuildingGuideRow(b) }

  Spacer(modifier = Modifier.height(4.dp))
  SectionHeader("Military & Defense Buildings", "Built by Villagers under the Military category.")
  GameDefinitions.buildingDefs.values
    .filter { it.category == BuildCategory.MILITARY }
    .forEach { b -> BuildingGuideRow(b) }

  Spacer(modifier = Modifier.height(4.dp))
  SectionHeader("Unit Roster & Counters", "Complete training costs, stats, and bonus damage counters.")
  GameDefinitions.unitDefs.values.forEach { u ->
    Surface(
      color = AgedTimber,
      shape = RoundedCornerShape(10.dp),
      border = BorderStroke(1.dp, BronzeBorder.copy(alpha = 0.55f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(u.type.displayName, style = MaterialTheme.typography.titleSmall, color = BurnishedGold)
            Text(
              "${u.requiredAge.displayName} • ${u.trainedAt.displayName} • ${u.trainTimeSec.toInt()}s",
              style = MaterialTheme.typography.labelSmall,
              color = AgedParchment,
            )
          }
          ResourceCostBadges(cost = u.cost, affordable = true, iconSize = 12.dp)
        }
        val bonuses = buildList {
          if (u.bonusVsCavalry > 0) add("+${u.bonusVsCavalry} vs Cavalry")
          if (u.bonusVsArchers > 0) add("+${u.bonusVsArchers} vs Archers")
          if (u.bonusVsArmored > 0) add("+${u.bonusVsArmored} vs Armored")
          if (u.bonusVsBuildings > 0) add("+${u.bonusVsBuildings} vs Buildings")
          if (u.requiredTech != null) add("Req: ${u.requiredTech.displayName}")
        }
        Text(
          text = "HP ${u.maxHp} • ATK ${u.attack} • ARM ${u.meleeArmor}/${u.pierceArmor} • RNG ${u.range} • SPD ${u.speed}",
          style = MaterialTheme.typography.labelSmall,
          color = ParchmentCream,
        )
        if (bonuses.isNotEmpty()) {
          Text(
            text = bonuses.joinToString(" • "),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF7BE87E),
          )
        }
      }
    }
  }

  Spacer(modifier = Modifier.height(4.dp))
  SectionHeader(
    "Mill, Mining Camp, University & Blacksmith",
    "Permanent buffs researched at Mill, Mining Camp, University, Blacksmith, Barracks & Archery Range.",
  )
  GameDefinitions.techDefs.values
    .filter { !it.type.name.startsWith("ADVANCE_") }
    .forEach { t ->
      Surface(
        color = CarvedWood,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BronzeBorder.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(t.type.displayName, style = MaterialTheme.typography.titleSmall, color = BurnishedGold)
            ResourceCostBadges(cost = t.cost, affordable = true, iconSize = 11.dp)
          }
          Text(
            "${t.requiredAge.displayName} • ${t.researchedAt.displayName} (${t.researchTimeSec.toInt()}s) — ${t.type.shortDesc}",
            style = MaterialTheme.typography.bodySmall,
            color = ParchmentCream,
          )
        }
      }
    }
}

@Composable
private fun BuildingGuideRow(b: com.example.game.model.BuildingDef) {
  Surface(
    color = AgedTimber,
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, BronzeBorder.copy(alpha = 0.55f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(b.type.displayName, style = MaterialTheme.typography.titleSmall, color = BurnishedGold)
          Text(
            "${b.requiredAge.displayName} • HP ${b.maxHp} • Build ${b.buildTimeSec.toInt()}s" +
              (if (b.popBonus > 0) " • +${b.popBonus} Pop" else "") +
              (if (b.attackDamage > 0) " • ATK ${b.attackDamage}" else ""),
            style = MaterialTheme.typography.labelSmall,
            color = AgedParchment,
          )
        }
        ResourceCostBadges(cost = b.cost, affordable = true, iconSize = 12.dp)
      }
      Text(b.description, style = MaterialTheme.typography.bodySmall, color = ParchmentCream)
    }
  }
}

@Composable
private fun CivilizationsTab(activeCiv: Civilization?) {
  SectionHeader("Four Great Kingdoms", "Each civilization brings unique passive bonuses and cost discounts.")
  Civilization.entries.forEach { civ ->
    val isCurrent = civ == activeCiv
    Surface(
      color = if (isCurrent) CarvedWood else AgedTimber,
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(if (isCurrent) 2.dp else 1.dp, Color(civ.accentHex)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(civ.accentHex), modifier = Modifier.size(18.dp))
            Text(civ.displayName, style = MaterialTheme.typography.titleMedium, color = BurnishedGold)
            if (isCurrent) {
              Surface(color = BurnishedGold, shape = RoundedCornerShape(4.dp)) {
                Text("Active", style = MaterialTheme.typography.labelSmall, color = DarkOak, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
              }
            }
          }
          Text(civ.specialty, style = MaterialTheme.typography.labelMedium, color = Color(civ.accentHex))
        }
        Text(civ.bonusText, style = MaterialTheme.typography.bodyMedium, color = ParchmentCream)
        val strat =
          when (civ) {
            Civilization.BRITONS -> "Playstyle: Push early into Feudal Archers & Crossbowmen protected by Spearmen."
            Civilization.FRANKS -> "Playstyle: Boom cheaply with 45W Permanent Farms and dominate Castle Age with +20% HP Knights."
            Civilization.TEUTONS -> "Playstyle: Anchor territory with resilient Watch Towers/Castles and armored Infantry."
            Civilization.BYZANTINES -> "Playstyle: Out-trade opponents with 20% cheaper Pikemen & Skirmishers plus tough walls/buildings."
          }
        Text(strat, style = MaterialTheme.typography.bodySmall, color = AgedParchment)
      }
    }
  }
}

@Composable
private fun FaqAndControlsTab() {
  SectionHeader("Frequently Asked Questions", "Everything you need to master economy, combat, and difficulty.")

  val faqs = listOf(
    "How is the Villager Build Menu organized?" to
      "Select any Villager to see Auto Gather plus two condensed build categories: Economy (House, Mill, Farm, Lumber Camp, Mining Camp, University, Town Center) and Military (Barracks, Archery Range, Stable, Blacksmith, Watch Tower, Castle, War Factory, Airfield).",
    "How do Production Queues & Cancellation work?" to
      "Every building supports a queue of up to 15 units or researches! Tap any queued chip below the progress bar to cancel that item and receive a full resource refund.",
    "What do Mill, Mining Camp, University & Blacksmith Researches unlock?" to
      "• Mill (8 Techs): Millstones, Horse Collar, Wheelbarrow, Heavy Plow, Granary Silos, Crop Rotation, Mechanized Farming & Supply Logistics boost gather speed, Farm yields, carry capacity, Villager speed/HP, and training speed.\n• Mining Camp (5 Techs): Gold/Stone Mining, Gold/Stone Shaft Mining & Steam Excavation accelerate mineral extraction, boost carry capacity, and reinforce building masonry (+10% HP).\n• University (8 Techs): Masonry, Treadmill Crane, Ballistics, Architecture, Siege Engineers, Chemistry, Military Academy & Aerodynamics strengthen all buildings (+15%/+20% HP), speed up construction & training, empower siege, gunpowder & aircraft!\n• Blacksmith (10 Techs): Forging through Blast Furnace, Ballistic Alloys & Reactive Armor scale melee/ranged/tank/jet firepower, range, armor, and +30 military HP.",
    "Do Farms run out of Food?" to
      "No! Once built, Farms are permanent wheat fields that supply endless Food for 1 Villager per Farm without needing to reseed.",
    "What happens when a Villager is Idle?" to
      "An amber halo marks idle Villagers and the top-left Idle Villager button counts them. After ~1.4 seconds idle, Villagers automatically find and gather whichever resource your empire needs most—or tap Auto Gather immediately.",
    "How do the 4 Difficulty Levels differ?" to
      buildString {
        Difficulty.entries.forEachIndexed { idx, d ->
          if (idx > 0) append("\n")
          append("• ${d.displayName}: ${d.summary}")
        }
      },
    "What are the best Combat Counters & Modern Units?" to
      "• Spearmen & Pikemen counter Cavalry (+14 / +22); Skirmishers counter Archers.\n• Industrial Age unlocks Riflemen, MG Troopers, Field Artillery, and Battle Tanks.\n• Modern Age unlocks Assault Marines, Snipers, Rocket Troopers (huge bonus vs Armored), Heavy Tanks, MLRS Batteries, Attack Gunships, Fighter Jets & Heavy Bombers!",
    "How do Mobile RTS Touch Controls & Moving Buildings work?" to
      "• Pan: Drag 1 finger across the map • Zoom: Pinch or tap +/- pills.\n• Select: Tap a unit/building (double-tap a unit to select all of that type).\n• Multi-Select: Tap Box Select and drag a rectangle over units.\n• Relocate Building: Misplaced a structure? Tap any of your buildings, tap Move in the bottom header, then tap the new map spot.\n• Minimap: Tap or drag on the bottom-left minimap to jump anywhere instantly.",
  )

  faqs.forEach { (q, a) ->
    Surface(
      color = AgedTimber,
      shape = RoundedCornerShape(10.dp),
      border = BorderStroke(1.dp, BronzeBorder.copy(alpha = 0.65f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(q, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = BurnishedGold)
        Text(a, style = MaterialTheme.typography.bodySmall, color = ParchmentCream)
      }
    }
  }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
  Column(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = BurnishedGold)
    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AgedParchment)
  }
}

@Composable
private fun OvermapDetailLine(label: String, content: String) {
  Row(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = "$label: ",
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
      color = BurnishedGold,
    )
    Text(
      text = content,
      style = MaterialTheme.typography.labelSmall,
      color = AgedParchment,
      maxLines = 3,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )
  }
}
