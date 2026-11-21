package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.AppScreen
import com.example.game.GameViewModel
import com.example.ui.GameScreen
import com.example.ui.PostGameScreen
import com.example.ui.SkirmishSetupScreen
import com.example.ui.TitleScreen
import com.example.ui.theme.CrownConquestTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      CrownConquestTheme {
        val vm: GameViewModel = viewModel()
        val uiState by vm.uiState.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        LaunchedEffect(vm) {
          vm.initAudio(context.applicationContext)
        }

        DisposableEffect(lifecycleOwner, vm) {
          val observer = LifecycleEventObserver { _, event ->
            when (event) {
              Lifecycle.Event.ON_PAUSE -> vm.onHostPause()
              Lifecycle.Event.ON_RESUME -> vm.onHostResume()
              else -> Unit
            }
          }
          lifecycleOwner.lifecycle.addObserver(observer)
          onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        when (val screen = uiState.screen) {
          AppScreen.Title -> {
            TitleScreen(
              onPlaySkirmish = vm::navigateToSetup,
              musicEnabled = uiState.musicEnabled,
              sfxEnabled = uiState.sfxEnabled,
              onToggleMusic = vm::toggleMusic,
              onToggleSfx = vm::toggleSfx,
            )
          }
          AppScreen.SkirmishSetup -> {
            SkirmishSetupScreen(
              config = uiState.config,
              onUpdateConfig = { pCiv, eCiv, map, diff ->
                vm.playUiClick()
                vm.updateConfig(pCiv, eCiv, map, diff)
              },
              onUpdateFullConfig = { nextCfg ->
                vm.playUiClick()
                vm.updateFullConfig(nextCfg)
              },
              onLaunchBattle = vm::startSkirmish,
              onBack = vm::navigateToTitle,
              musicEnabled = uiState.musicEnabled,
              onToggleMusic = vm::toggleMusic,
            )
          }
          AppScreen.Playing -> {
            GameScreen(
              uiState = uiState,
              onPanCamera = vm::panCamera,
              onZoomCamera = vm::zoomCamera,
              onJumpCamera = vm::jumpCameraTo,
              onMapTap = vm::onMapTap,
              onBoxSelect = vm::onBoxSelect,
              onToggleBoxSelect = vm::toggleBoxSelectMode,
              onToggleAttackMove = vm::toggleAttackMoveMode,
              onStopUnits = vm::stopSelectedUnits,
              onStartBuild = vm::startBuildPlacement,
              onCancelBuild = vm::cancelBuildPlacement,
              onTrainUnit = vm::trainUnit,
              onResearchTech = vm::researchTech,
              onCycleIdleVillager = vm::cycleIdleVillager,
              onAutoGatherVillagers = vm::autoGatherSelectedVillagers,
              onSelectAllMilitary = vm::selectAllMilitary,
              onJumpToTc = vm::jumpToTownCenter,
              onCycleSpeed = vm::cycleSpeed,
              onTogglePause = vm::togglePause,
              onRestart = vm::startSkirmish,
              onQuitToMenu = vm::navigateToTitle,
              onCancelQueueItem = vm::cancelQueueItem,
              onToggleMusic = vm::toggleMusic,
              onToggleSfx = vm::toggleSfx,
              onMoveSelectedBuilding = vm::startMoveSelectedBuilding,
            )
          }
          is AppScreen.PostGame -> {
            PostGameScreen(
              result = screen.result,
              onPlayAgain = vm::startSkirmish,
              onReturnToMenu = vm::navigateToTitle,
            )
          }
        }
      }
    }
  }
}
