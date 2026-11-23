package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Shield
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
import com.example.game.model.TechDef
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

private enum class GuideTab(val icon: ImageVector) {
  OVERMAP(Icons.Default.AccountBalance),
  COSTS(Icons.Default.Build),
  CIVS(Icons.Default.Flag),
  FAQ(Icons.AutoMirrored.Filled.HelpOutline),
}

@Composable
fun GameGuideDialog(
  onDismiss: () -> Unit,
  initialPlayerCiv: Civilization? = null,
) {
  val s = LocalAppStrings.current
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
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight && maxWidth >= 540.dp
        Column(modifier = Modifier.fillMaxSize()) {
          // Hero Banner Header
          Box(modifier = Modifier.fillMaxWidth().height(if (isLandscape) 74.dp else 108.dp)) {
            Image(
              painter = painterResource(id = R.drawable.img_codex_banner),
              contentDescription = s.codexTitle,
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
              modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
              verticalAlignment = Alignment.Bottom,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  color = Color(0xCC24180F),
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(1.2.dp, BurnishedGold),
                  modifier = Modifier.size(36.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.MenuBook,
                      contentDescription = null,
                      tint = BurnishedGold,
                      modifier = Modifier.size(20.dp),
                    )
                  }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = s.codexTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = BurnishedGold,
                  )
                  if (!isLandscape) {
                    Text(
                      text = s.codexSubtitle,
                      style = MaterialTheme.typography.labelSmall,
                      color = ParchmentCream,
                    )
                  }
                }
              }
              IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(48.dp).testTag("close_guide_button"),
              ) {
                Icon(Icons.Default.Close, contentDescription = s.cancel, tint = BurnishedGold)
              }
            }
          }

          // Navigation Tab Bar
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .background(AgedTimber)
                .padding(horizontal = 10.dp, vertical = 7.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            GuideTab.entries.forEach { tab ->
              val selected = activeTab == tab
              val label =
                when (tab) {
                  GuideTab.OVERMAP -> s.tabOvermap
                  GuideTab.COSTS -> s.tabCosts
                  GuideTab.CIVS -> s.tabCivs
                  GuideTab.FAQ -> s.tabFaq
                }
              Surface(
                color = if (selected) BurnishedGold else CarvedWood,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, if (selected) ParchmentCream else BronzeBorder),
                modifier =
                  Modifier
                    .height(36.dp)
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
                    text = label,
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
              GuideTab.OVERMAP -> AgesOvermapTab(s)
              GuideTab.COSTS -> CostsAndUnitsTab(s)
              GuideTab.CIVS -> CivilizationsTab(initialPlayerCiv, s)
              GuideTab.FAQ -> FaqAndControlsTab(s)
            }
          }
        }
      }
    }
  }
}

private fun formatTechPrereqs(tDef: TechDef, s: AppStrings): String? {
  val parts = buildList {
    tDef.requiredBuildings.forEach { add(s.buildingName(it)) }
    tDef.requiredTechs.forEach { add(s.techName(it)) }
  }
  return if (parts.isEmpty()) null else "${s.requiresPrefix} ${parts.joinToString(", ")}"
}

@Composable
private fun AgesOvermapTab(s: AppStrings) {
  SectionHeader(s.overmapTitle, s.overmapSub)

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
              text = s.ageShort(age),
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = ParchmentCream,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
          Spacer(modifier = Modifier.height(3.dp))
          val costLabel =
            when (age) {
              Age.DARK_AGE -> s.startingEra
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
            Text(s.ageName(age), style = MaterialTheme.typography.titleMedium, color = BurnishedGold)
          }
          if (advanceTech != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              ResourceCostBadges(cost = advanceTech.cost, affordable = true, iconSize = 11.dp)
              Text("${advanceTech.researchTimeSec.toInt()}s", style = MaterialTheme.typography.labelSmall, color = AgedParchment)
            }
          } else {
            Text(s.startingEra, style = MaterialTheme.typography.labelSmall, color = AgedParchment)
          }
        }

        Text(s.ageSummary(age), style = MaterialTheme.typography.bodyMedium, color = ParchmentCream)

        if (advanceTech != null) {
          formatTechPrereqs(advanceTech, s)?.let { reqLine ->
            Text(
              text = reqLine,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = BurnishedGold,
            )
          }
        }

        if (unlockedBuildings.isNotEmpty()) {
          OvermapDetailLine(s.buildingsHeader, unlockedBuildings.joinToString(", ") { "${s.buildingName(it.type)} (${it.cost.summary()})" })
        }
        if (unlockedUnits.isNotEmpty()) {
          OvermapDetailLine(s.unitsHeader, unlockedUnits.joinToString(", ") { "${s.unitName(it.type)} (${it.cost.summary()})" })
        }
        if (unlockedTechs.isNotEmpty()) {
          OvermapDetailLine(s.upgradesHeader, unlockedTechs.joinToString(", ") { "${s.techName(it.type)} (${it.cost.summary()})" })
        }
      }
    }
  }

  SectionHeader(s.battlegroundsTitle, s.battlegroundsSub)
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    MapType.entries.forEach { map ->
      Surface(
        color = CarvedWood,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BronzeBorder.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          Text(s.mapName(map), style = MaterialTheme.typography.titleSmall, color = BurnishedGold)
          Text(s.mapDesc(map), style = MaterialTheme.typography.bodySmall, color = AgedParchment)
        }
      }
    }
  }
}

@Composable
private fun CostsAndUnitsTab(s: AppStrings) {
  SectionHeader(s.ecoBuildingsTitle, s.ecoBuildingsSub)
  GameDefinitions.buildingDefs.values
    .filter { it.category == BuildCategory.ECONOMY }
    .forEach { b -> BuildingGuideRow(b, s) }

  Spacer(modifier = Modifier.height(4.dp))
  SectionHeader(s.milBuildingsTitle, s.milBuildingsSub)
  GameDefinitions.buildingDefs.values
    .filter { it.category == BuildCategory.MILITARY }
    .forEach { b -> BuildingGuideRow(b, s) }

  Spacer(modifier = Modifier.height(4.dp))
  SectionHeader(s.unitRosterTitle, s.unitRosterSub)
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
            Text(s.unitName(u.type), style = MaterialTheme.typography.titleSmall, color = BurnishedGold)
            Text(
              "${s.ageName(u.requiredAge)} • ${s.buildingName(u.trainedAt)} • ${u.trainTimeSec.toInt()}s",
              style = MaterialTheme.typography.labelSmall,
              color = AgedParchment,
            )
          }
          ResourceCostBadges(cost = u.cost, affordable = true, iconSize = 12.dp)
        }
        val bonuses = buildList {
          if (u.bonusVsCavalry > 0) add("+${u.bonusVsCavalry} Cavalry")
          if (u.bonusVsArchers > 0) add("+${u.bonusVsArchers} Archers")
          if (u.bonusVsArmored > 0) add("+${u.bonusVsArmored} Armored")
          if (u.bonusVsBuildings > 0) add("+${u.bonusVsBuildings} Buildings")
          if (u.requiredTech != null) add("${s.reqPrefix} ${s.techName(u.requiredTech)}")
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
  SectionHeader(s.techCentersTitle, s.techCentersSub)
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
            Text(s.techName(t.type), style = MaterialTheme.typography.titleSmall, color = BurnishedGold)
            ResourceCostBadges(cost = t.cost, affordable = true, iconSize = 11.dp)
          }
          Text(
            "${s.ageName(t.requiredAge)} • ${s.buildingName(t.researchedAt)} (${t.researchTimeSec.toInt()}s) — ${s.techDesc(t.type)}",
            style = MaterialTheme.typography.bodySmall,
            color = ParchmentCream,
          )
          formatTechPrereqs(t, s)?.let { reqLine ->
            Text(
              text = reqLine,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = Color(0xFFFFD875),
            )
          }
        }
      }
    }
}

@Composable
private fun BuildingGuideRow(b: com.example.game.model.BuildingDef, s: AppStrings) {
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
          Text(s.buildingName(b.type), style = MaterialTheme.typography.titleSmall, color = BurnishedGold)
          Text(
            "${s.ageName(b.requiredAge)} • HP ${b.maxHp} • ${b.buildTimeSec.toInt()}s" +
              (if (b.popBonus > 0) " • +${b.popBonus} Pop" else "") +
              (if (b.attackDamage > 0) " • ATK ${b.attackDamage}" else ""),
            style = MaterialTheme.typography.labelSmall,
            color = AgedParchment,
          )
        }
        ResourceCostBadges(cost = b.cost, affordable = true, iconSize = 12.dp)
      }
      Text(s.buildingDesc(b.type), style = MaterialTheme.typography.bodySmall, color = ParchmentCream)
    }
  }
}

@Composable
private fun CivilizationsTab(activeCiv: Civilization?, s: AppStrings) {
  SectionHeader(s.civsTitle, s.civsSub)
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
            Text(s.civName(civ), style = MaterialTheme.typography.titleMedium, color = BurnishedGold)
            if (isCurrent) {
              Surface(color = BurnishedGold, shape = RoundedCornerShape(4.dp)) {
                Text(s.activeLabel, style = MaterialTheme.typography.labelSmall, color = DarkOak, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
              }
            }
          }
          Text(s.civSpecialty(civ), style = MaterialTheme.typography.labelMedium, color = Color(civ.accentHex))
        }
        Text(s.civBonus(civ), style = MaterialTheme.typography.bodyMedium, color = ParchmentCream)
        Text(s.civStrategy(civ), style = MaterialTheme.typography.bodySmall, color = AgedParchment)
      }
    }
  }
}

@Composable
private fun FaqAndControlsTab(s: AppStrings) {
  SectionHeader(s.faqTitle, s.faqSub)

  s.faqEntries.forEach { (q, a) ->
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
