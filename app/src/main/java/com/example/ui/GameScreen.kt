package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.HighlightAlt
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameUiState
import com.example.game.model.BuildingType
import com.example.game.model.Faction
import com.example.game.model.TechType
import com.example.game.model.UnitOrderType
import com.example.game.model.UnitType
import com.example.game.render.WorldRenderer.drawGameWorld
import com.example.ui.theme.AgedTimber
import com.example.ui.theme.BronzeBorder
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.DarkOak
import com.example.ui.theme.EmberRed
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.ParchmentCream
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

@Composable
fun GameScreen(
  uiState: GameUiState,
  onPanCamera: (Float, Float) -> Unit,
  onZoomCamera: (Float) -> Unit,
  onJumpCamera: (Float, Float) -> Unit,
  onMapTap: (Float, Float, Float, Float) -> Unit,
  onBoxSelect: (Rect, Float, Float) -> Unit,
  onToggleBoxSelect: () -> Unit,
  onToggleAttackMove: () -> Unit,
  onStopUnits: () -> Unit,
  onStartBuild: (BuildingType) -> Unit,
  onCancelBuild: () -> Unit,
  onTrainUnit: (UnitType) -> Unit,
  onResearchTech: (TechType) -> Unit,
  onCycleIdleVillager: () -> Unit,
  onAutoGatherVillagers: () -> Unit,
  onSelectAllMilitary: () -> Unit,
  onJumpToTc: () -> Unit,
  onCycleSpeed: () -> Unit,
  onTogglePause: () -> Unit,
  onRestart: () -> Unit,
  onQuitToMenu: () -> Unit,
  onCancelQueueItem: (Int) -> Unit = {},
  onToggleMusic: () -> Unit = {},
  onToggleSfx: () -> Unit = {},
  onMoveSelectedBuilding: () -> Unit = {},
) {
  BackHandler { onTogglePause() }

  val s = LocalAppStrings.current
  val world = uiState.world ?: return
  val textMeasurer = rememberTextMeasurer()
  var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }
  var selectionBox by remember { mutableStateOf<Rect?>(null) }
  var showGuide by remember { mutableStateOf(false) }
  val localizedBuildingNames = remember(s) { BuildingType.entries.associateWith { s.buildingName(it) } }

  val idleVillagerCount =
    world.units.count {
      it.faction == Faction.PLAYER &&
        it.type == UnitType.VILLAGER &&
        it.order == UnitOrderType.IDLE
    }
  val totalVillagerCount =
    world.units.count {
      it.faction == Faction.PLAYER &&
        it.type == UnitType.VILLAGER
    }
  val armyCount =
    world.units.count {
      it.faction == Faction.PLAYER &&
        it.type != UnitType.VILLAGER
    }
  val mins = (world.elapsedSec.toInt() / 60).toString().padStart(2, '0')
  val secs = (world.elapsedSec.toInt() % 60).toString().padStart(2, '0')

  BoxWithConstraints(modifier = Modifier.fillMaxSize().background(DarkOak)) {
    val isLandscape = maxWidth > maxHeight && maxWidth >= 540.dp

    Column(modifier = Modifier.fillMaxSize()) {
      TopResourceBar(
        world = world,
        gameSpeed = uiState.gameSpeed,
        onCycleSpeed = onCycleSpeed,
        onPauseClick = onTogglePause,
        onOpenGuide = { showGuide = true },
        sfxEnabled = uiState.sfxEnabled,
        onToggleSfx = onToggleSfx,
      )

      // Interactive Isometric Battlefield Canvas
      Box(modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
        Canvas(
          modifier =
            Modifier
              .fillMaxSize()
              .clipToBounds()
              .testTag("battlefield_canvas")
              .onSizeChanged { canvasSize = it }
              .pointerInput(uiState.boxSelectMode) {
                awaitEachGesture {
                  val down = awaitFirstDown(requireUnconsumed = false)
                  val startPos = down.position
                  var totalMoved = 0f
                  var lastDragPos = startPos
                  var multiTouchUsed = false

                  while (true) {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.filter { it.pressed }
                    if (pressed.isEmpty()) break

                    if (pressed.size >= 2) {
                      multiTouchUsed = true
                      val zoom = event.calculateZoom()
                      val pan = event.calculatePan()
                      if (zoom != 1f) onZoomCamera(zoom)
                      if (pan != Offset.Zero) onPanCamera(pan.x, pan.y)
                      event.changes.forEach { it.consume() }
                    } else if (!multiTouchUsed) {
                      val cur = pressed.first().position
                      val delta = cur - lastDragPos
                      totalMoved += hypot(delta.x, delta.y)
                      lastDragPos = cur

                      if (uiState.boxSelectMode && totalMoved > 8f) {
                        selectionBox =
                          Rect(
                            left = min(startPos.x, cur.x),
                            top = min(startPos.y, cur.y),
                            right = max(startPos.x, cur.x),
                            bottom = max(startPos.y, cur.y),
                          )
                      } else if (!uiState.boxSelectMode && totalMoved > 8f) {
                        onPanCamera(delta.x, delta.y)
                      }
                      pressed.first().consume()
                    }
                  }

                  val box = selectionBox
                  if (box != null) {
                    selectionBox = null
                    onBoxSelect(box, canvasSize.width.toFloat(), canvasSize.height.toFloat())
                  } else if (!multiTouchUsed && totalMoved <= 14f) {
                    onMapTap(startPos.x, startPos.y, canvasSize.width.toFloat(), canvasSize.height.toFloat())
                  }
                }
              }
        ) {
          drawGameWorld(
            world = world,
            camera = uiState.camera,
            selectedUnitIds = uiState.selectedUnitIds,
            selectedBuildingId = uiState.selectedBuildingId,
            pendingBuildType = uiState.pendingBuildType,
            pendingBuildTile = uiState.pendingBuildTile,
            selectionBox = selectionBox,
            animClock = uiState.animClock,
            textMeasurer = textMeasurer,
            pendingMoveBuildingId = uiState.pendingMoveBuildingId,
            buildingNames = localizedBuildingNames,
          )
        }

        // Dedicated Top-Left Idle Villager Button
        Surface(
          color = if (idleVillagerCount > 0) Color(0xEC462E0D) else Color(0xCC1E150E),
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(if (idleVillagerCount > 0) 1.6.dp else 1.dp, if (idleVillagerCount > 0) BurnishedGold else BronzeBorder),
          modifier =
            Modifier
              .align(Alignment.TopStart)
              .padding(start = 10.dp, top = if (isLandscape) 6.dp else 10.dp)
              .height(if (isLandscape) 36.dp else 42.dp)
              .clip(RoundedCornerShape(10.dp))
              .clickable(onClick = onCycleIdleVillager)
              .testTag("idle_villager_button"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Engineering,
              contentDescription = s.idleVillager,
              tint = if (idleVillagerCount > 0) BurnishedGold else ParchmentCream.copy(alpha = 0.85f),
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = if (idleVillagerCount > 0) s.idleVillager else "${s.workersLabel} ($totalVillagerCount)",
              style = MaterialTheme.typography.labelLarge,
              color = if (idleVillagerCount > 0) BurnishedGold else ParchmentCream,
            )
            if (idleVillagerCount > 0) {
              Surface(
                color = BurnishedGold,
                shape = RoundedCornerShape(10.dp),
              ) {
                Text(
                  text = idleVillagerCount.toString(),
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                  color = DarkOak,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                )
              }
            }
          }
        }

        // Top-Right Match Clock & Map Crest
        Surface(
          color = Color(0xCC1B130D),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, BronzeBorder.copy(alpha = 0.7f)),
          modifier = Modifier.align(Alignment.TopEnd).padding(end = 10.dp, top = if (isLandscape) 6.dp else 10.dp),
        ) {
          Text(
            text = "${s.mapName(world.mapType)} • $mins:$secs",
            style = MaterialTheme.typography.labelSmall,
            color = ParchmentCream,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
          )
        }

        // Status Toast Banner
        androidx.compose.animation.AnimatedVisibility(
          visible = uiState.statusBanner != null,
          enter = fadeIn(),
          exit = fadeOut(),
          modifier = Modifier.align(Alignment.TopCenter).padding(top = if (isLandscape) 46.dp else 58.dp, start = 16.dp, end = 16.dp),
        ) {
          Surface(
            color = Color(0xE01B130D),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.2.dp, BurnishedGold),
          ) {
            Text(
              text = uiState.statusBanner.orEmpty(),
              style = MaterialTheme.typography.labelLarge,
              color = ParchmentCream,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            )
          }
        }

        // Bottom Tactical Overlay Column: Building Description Card during placement + Quick-Action Pills
        Column(
          modifier =
            Modifier
              .align(Alignment.BottomCenter)
              .fillMaxWidth()
              .padding(horizontal = 6.dp, vertical = 2.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          val placingType = uiState.pendingBuildType
          if (placingType != null) {
            val radiusHint = if (placingType == BuildingType.TOWN_CENTER) s.tcExpandHint else s.buildRadiusHint
            Surface(
              color = Color(0xE618100A),
              shape = RoundedCornerShape(6.dp),
              border = BorderStroke(1.dp, BurnishedGold.copy(alpha = 0.85f)),
              modifier = Modifier.widthIn(max = 520.dp).testTag("placement_description_card"),
            ) {
              Text(
                text = "${s.buildingName(placingType)}: ${s.buildingDesc(placingType)} • $radiusHint",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 11.5.sp),
                color = ParchmentCream,
                maxLines = if (isLandscape) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              TacticalPill(
                icon = Icons.Default.HighlightAlt,
                label = if (uiState.boxSelectMode) s.dragBox else s.boxSelect,
                active = uiState.boxSelectMode,
                tag = "box_select_toggle",
                compact = isLandscape,
                onClick = onToggleBoxSelect,
              )
              TacticalPill(
                icon = Icons.Default.AccountBalance,
                label = s.tcShort,
                active = false,
                tag = "quick_tc_button",
                compact = isLandscape,
                onClick = onJumpToTc,
              )
              TacticalPill(
                icon = Icons.Default.Shield,
                label = if (armyCount > 0) "${s.armyLabel} ($armyCount)" else s.armyLabel,
                active = false,
                tag = "quick_army_button",
                compact = isLandscape,
                onClick = onSelectAllMilitary,
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              if (uiState.pendingBuildType != null || uiState.pendingMoveBuildingId != null) {
                TacticalPill(
                  icon = Icons.Default.Close,
                  label = s.cancel,
                  active = true,
                  activeColor = EmberRed,
                  tag = "cancel_build_button",
                  compact = isLandscape,
                  onClick = onCancelBuild,
                )
              } else {
                TacticalPill(
                  icon = Icons.Default.Remove,
                  label = null,
                  active = false,
                  tag = "zoom_out_button",
                  compact = isLandscape,
                  onClick = { onZoomCamera(0.84f) },
                )
                TacticalPill(
                  icon = Icons.Default.Add,
                  label = null,
                  active = false,
                  tag = "zoom_in_button",
                  compact = isLandscape,
                  onClick = { onZoomCamera(1.18f) },
                )
              }
            }
          }
        }
      }

      BottomCommandPanel(
        uiState = uiState,
        onJumpCamera = onJumpCamera,
        onStartBuild = onStartBuild,
        onTrainUnit = onTrainUnit,
        onResearchTech = onResearchTech,
        onToggleAttackMove = onToggleAttackMove,
        onStopUnits = onStopUnits,
        onAutoGatherVillagers = onAutoGatherVillagers,
        onSelectAllMilitary = onSelectAllMilitary,
        onJumpToTc = onJumpToTc,
        onCycleIdleVillager = onCycleIdleVillager,
        onCancelQueueItem = onCancelQueueItem,
        onMoveSelectedBuilding = onMoveSelectedBuilding,
        isLandscape = isLandscape,
      )
    }
  }

  if (uiState.isPaused) {
    AlertDialog(
      onDismissRequest = onTogglePause,
      containerColor = AgedTimber,
      titleContentColor = BurnishedGold,
      textContentColor = ParchmentCream,
      title = { Text(s.battlePaused, style = MaterialTheme.typography.headlineMedium) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Button(
            onClick = onTogglePause,
            colors = ButtonDefaults.buttonColors(containerColor = BurnishedGold, contentColor = DarkOak),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("resume_match_button"),
          ) {
            Text(s.resumeMatch, style = MaterialTheme.typography.labelLarge)
          }
          OutlinedButton(
            onClick = { showGuide = true },
            border = BorderStroke(1.dp, BurnishedGold),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = BurnishedGold),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_guide_button"),
          ) {
            Text(s.overmapAndFaq, style = MaterialTheme.typography.labelLarge)
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = onToggleSfx,
              border = BorderStroke(1.dp, if (uiState.sfxEnabled) BurnishedGold else BronzeBorder),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = if (uiState.sfxEnabled) BurnishedGold else ParchmentCream),
              modifier = Modifier.weight(1f).height(48.dp).testTag("pause_sfx_toggle"),
            ) {
              Icon(
                imageVector = if (uiState.sfxEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (uiState.sfxEnabled) s.sfxOn else s.sfxOff, style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
              onClick = onToggleMusic,
              border = BorderStroke(1.dp, if (uiState.musicEnabled) BurnishedGold else BronzeBorder),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = if (uiState.musicEnabled) BurnishedGold else ParchmentCream),
              modifier = Modifier.weight(1f).height(48.dp).testTag("pause_music_toggle"),
            ) {
              Icon(
                imageVector = if (uiState.musicEnabled) Icons.Default.MusicNote else Icons.Default.MusicOff,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (uiState.musicEnabled) s.musicOn else s.musicOff, style = MaterialTheme.typography.labelMedium)
            }
          }
          OutlinedButton(
            onClick = onRestart,
            border = BorderStroke(1.dp, BronzeBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ParchmentCream),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("restart_match_button"),
          ) {
            Text(s.restartSkirmish, style = MaterialTheme.typography.labelLarge)
          }
          OutlinedButton(
            onClick = onQuitToMenu,
            border = BorderStroke(1.dp, EmberRed),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmberRed),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("quit_match_button"),
          ) {
            Text(s.returnToMenu, style = MaterialTheme.typography.labelLarge)
          }
        }
      },
      confirmButton = {},
    )
  }

  if (showGuide) {
    GameGuideDialog(
      onDismiss = { showGuide = false },
      initialPlayerCiv = world.playerState.civilization,
    )
  }
}

@Composable
private fun TacticalPill(
  icon: ImageVector,
  label: String?,
  active: Boolean,
  activeColor: Color = ForestEmerald,
  tag: String,
  compact: Boolean = false,
  onClick: () -> Unit,
) {
  Surface(
    color = if (active) activeColor else Color(0xD821170F),
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(0.8.dp, if (active) BurnishedGold else BronzeBorder),
    modifier =
      Modifier
        .height(if (compact) 24.dp else 28.dp)
        .clip(RoundedCornerShape(14.dp))
        .clickable(onClick = onClick)
        .testTag(tag),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = if (label != null) 7.dp else 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(icon, contentDescription = label ?: tag, tint = ParchmentCream, modifier = Modifier.size(if (compact) 12.dp else 13.dp))
      if (label != null) {
        Spacer(modifier = Modifier.width(3.dp))
        Text(
          label,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compact) 10.sp else 10.5.sp),
          color = ParchmentCream,
        )
      }
    }
  }
}
