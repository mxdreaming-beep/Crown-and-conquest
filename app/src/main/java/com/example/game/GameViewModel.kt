package com.example.game

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.audio.GameSfx
import com.example.game.audio.SoundEngine
import com.example.game.engine.AiCommander
import com.example.game.engine.MapGenerator
import com.example.game.engine.RtsEngine
import com.example.game.model.Building
import com.example.game.model.BuildingType
import com.example.game.model.CameraState
import com.example.game.model.Civilization
import com.example.game.model.Difficulty
import com.example.game.model.Faction
import com.example.game.model.GameDefinitions
import com.example.game.model.GameWorld
import com.example.game.model.MapType
import com.example.game.model.MatchResult
import com.example.game.model.ProjectileKind
import com.example.game.model.ResourceNode
import com.example.game.model.ResourceNodeType
import com.example.game.model.SkirmishConfig
import com.example.game.model.TechType
import com.example.game.model.Unit
import com.example.game.model.UnitOrderType
import com.example.game.model.UnitType
import com.example.game.model.VisualEffectType
import com.example.game.render.IsometricMath
import com.example.game.render.ProceduralTextures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.hypot

sealed interface AppScreen {
  data object Title : AppScreen
  data object SkirmishSetup : AppScreen
  data object Playing : AppScreen
  data class PostGame(val result: MatchResult) : AppScreen
}

data class GameUiState(
  val screen: AppScreen = AppScreen.Title,
  val config: SkirmishConfig = SkirmishConfig(),
  val world: GameWorld? = null,
  val camera: CameraState = CameraState(),
  val selectedUnitIds: Set<Int> = emptySet(),
  val selectedBuildingId: Int? = null,
  val pendingBuildType: BuildingType? = null,
  val pendingMoveBuildingId: Int? = null,
  val pendingBuildTile: Pair<Int, Int>? = null,
  val boxSelectMode: Boolean = false,
  val attackMoveMode: Boolean = false,
  val isPaused: Boolean = false,
  val gameSpeed: Float = 1.0f,
  val statusBanner: String? = null,
  val animClock: Float = 0f,
  val musicEnabled: Boolean = true,
  val sfxEnabled: Boolean = true,
)

class GameViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(GameUiState())
  val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

  private var simJob: Job? = null
  private var aiAccumulator = 0f
  private var bannerTimer = 0f
  private var workSfxAccumulator = 0f
  private var lastTapTimeMs = 0L
  private var lastTapUnitType: UnitType? = null
  private var idleCycleIndex = 0
  private var soundEngine: SoundEngine? = null

  init {
    viewModelScope.launch(Dispatchers.Default) {
      ProceduralTextures.warmUpForMap(_uiState.value.config.mapType)
    }
  }

  fun initAudio(context: Context) {
    if (soundEngine != null) return
    val engine = SoundEngine(context)
    soundEngine = engine
    engine.setScreen(_uiState.value.screen)
  }

  fun toggleMusic() {
    val engine = soundEngine
    if (engine != null) {
      engine.toggleMusic()
      _uiState.update { it.copy(musicEnabled = engine.musicEnabled.value) }
    } else {
      _uiState.update { it.copy(musicEnabled = !it.musicEnabled) }
    }
  }

  fun toggleSfx() {
    val engine = soundEngine
    if (engine != null) {
      engine.toggleSfx()
      val enabled = engine.sfxEnabled.value
      _uiState.update { it.copy(sfxEnabled = enabled) }
      if (enabled) engine.playSfx(GameSfx.UI_CLICK)
    } else {
      _uiState.update { it.copy(sfxEnabled = !it.sfxEnabled) }
    }
  }

  fun playUiClick() {
    soundEngine?.playSfx(GameSfx.UI_CLICK, volume = 0.65f)
  }

  fun onHostPause() {
    soundEngine?.onHostPause()
  }

  fun onHostResume() {
    soundEngine?.onHostResume()
  }

  override fun onCleared() {
    soundEngine?.release()
    soundEngine = null
    super.onCleared()
  }

  fun navigateToSetup() {
    soundEngine?.playSfx(GameSfx.UI_CLICK, volume = 0.72f)
    soundEngine?.setScreen(AppScreen.SkirmishSetup)
    _uiState.update { it.copy(screen = AppScreen.SkirmishSetup) }
  }

  fun navigateToTitle() {
    simJob?.cancel()
    soundEngine?.playSfx(GameSfx.UI_CLICK, volume = 0.65f)
    soundEngine?.setScreen(AppScreen.Title)
    _uiState.update {
      it.copy(
        screen = AppScreen.Title,
        world = null,
        selectedUnitIds = emptySet(),
        selectedBuildingId = null,
        pendingBuildType = null,
        pendingMoveBuildingId = null,
        pendingBuildTile = null,
        isPaused = false,
      )
    }
  }

  fun updateConfig(
    playerCiv: Civilization = _uiState.value.config.playerCiv,
    enemyCiv: Civilization = _uiState.value.config.enemyCiv,
    mapType: MapType = _uiState.value.config.mapType,
    difficulty: Difficulty = _uiState.value.config.difficulty,
  ) {
    val mapChanged = mapType != _uiState.value.config.mapType
    _uiState.update {
      it.copy(
        config =
          it.config.copy(
            playerCiv = playerCiv,
            enemyCiv = enemyCiv,
            mapType = mapType,
            difficulty = difficulty,
          )
      )
    }
    if (mapChanged) {
      viewModelScope.launch(Dispatchers.Default) {
        ProceduralTextures.warmUpForMap(mapType)
      }
    }
  }

  fun updateFullConfig(nextConfig: SkirmishConfig) {
    val mapChanged = nextConfig.mapType != _uiState.value.config.mapType
    _uiState.update { it.copy(config = nextConfig) }
    if (mapChanged) {
      viewModelScope.launch(Dispatchers.Default) {
        ProceduralTextures.warmUpForMap(nextConfig.mapType)
      }
    }
  }

  fun startSkirmish() {
    val cfg = _uiState.value.config
    val initialWorld = MapGenerator.createWorld(cfg)
    val playerTc = initialWorld.buildings.first { it.faction == Faction.PLAYER }
    aiAccumulator = 0f
    bannerTimer = 3.5f
    workSfxAccumulator = 0f

    soundEngine?.setScreen(AppScreen.Playing)
    soundEngine?.playSfx(GameSfx.ATTACK_HORN, volume = 0.78f)

    val aiLabel = if (cfg.aiCount == 1) "1 AI Rival" else "${cfg.aiCount} AI Rivals"
    _uiState.update {
      it.copy(
        screen = AppScreen.Playing,
        world = initialWorld,
        camera = CameraState(centerWorldX = playerTc.centerX, centerWorldY = playerTc.centerY, zoom = 1.08f),
        selectedUnitIds = emptySet(),
        selectedBuildingId = playerTc.id,
        pendingBuildType = null,
        pendingMoveBuildingId = null,
        pendingBuildTile = null,
        boxSelectMode = false,
        attackMoveMode = false,
        isPaused = false,
        statusBanner = "$aiLabel • ${cfg.mapSize.displayName} — Gather Food & Wood!",
      )
    }
    startSimulationLoop()
  }

  private fun startSimulationLoop() {
    simJob?.cancel()
    simJob =
      viewModelScope.launch {
        val tickMs = 50L
        while (true) {
          delay(tickMs)
          val state = _uiState.value
          if (state.screen != AppScreen.Playing || state.isPaused || state.world == null) continue

          val prevWorld = state.world
          val prevMaxEffectId = prevWorld.effects.maxOfOrNull { it.id } ?: 0
          val prevMaxProjId = prevWorld.projectiles.maxOfOrNull { it.id } ?: 0
          val prevCompletedBuildings = prevWorld.buildings.count { it.faction == Faction.PLAYER && it.isComplete }

          val dt = (tickMs / 1000f) * state.gameSpeed
          var nextWorld = RtsEngine.step(prevWorld, dt, state.config)

          aiAccumulator += dt
          if (aiAccumulator >= 1.25f) {
            aiAccumulator = 0f
            nextWorld = AiCommander.updateAi(nextWorld, state.config.difficulty)
          }

          if (bannerTimer > 0f) {
            bannerTimer -= (tickMs / 1000f)
          }

          emitSimulationSfx(
            prevWorld = prevWorld,
            nextWorld = nextWorld,
            prevMaxEffectId = prevMaxEffectId,
            prevMaxProjId = prevMaxProjId,
            prevCompletedBuildings = prevCompletedBuildings,
            dt = dt,
          )

          val validSelectedUnits =
            state.selectedUnitIds.filter { id -> nextWorld.units.any { it.id == id } }.toSet()
          val validSelectedBuilding =
            state.selectedBuildingId?.takeIf { id -> nextWorld.buildings.any { it.id == id } }

          val result = nextWorld.matchResult
          if (result != null) {
            val endScreen = AppScreen.PostGame(result)
            soundEngine?.setScreen(endScreen)
            soundEngine?.playSfx(
              if (result.winner == Faction.PLAYER) GameSfx.VICTORY_FANFARE else GameSfx.DEFEAT_HORN,
              volume = 0.92f,
            )
            _uiState.update {
              it.copy(
                world = nextWorld,
                screen = endScreen,
              )
            }
            break
          } else {
            _uiState.update {
              it.copy(
                world = nextWorld,
                selectedUnitIds = validSelectedUnits,
                selectedBuildingId = validSelectedBuilding,
                animClock = it.animClock + (tickMs / 1000f),
                statusBanner = if (bannerTimer > 0f) it.statusBanner else null,
              )
            }
          }
        }
      }
  }

  private fun spatialAudioFor(wx: Float, wy: Float, cam: CameraState): Pair<Float, Float> {
    val dx = wx - cam.centerWorldX
    val dy = wy - cam.centerWorldY
    val dist = hypot(dx, dy)
    val attenuation = (1f - (dist - 5f).coerceAtLeast(0f) / 26f).coerceIn(0.28f, 1f)
    // Isometric horizontal offset maps along (dx - dy)
    val pan = ((dx - dy) / 10f).coerceIn(-0.82f, 0.82f)
    return attenuation to pan
  }

  private fun emitSimulationSfx(
    prevWorld: GameWorld,
    nextWorld: GameWorld,
    prevMaxEffectId: Int,
    prevMaxProjId: Int,
    prevCompletedBuildings: Int,
    dt: Float,
  ) {
    val engine = soundEngine ?: return
    val cam = _uiState.value.camera
    for (fx in nextWorld.effects) {
      if (fx.id > prevMaxEffectId) {
        val (atten, pan) = spatialAudioFor(fx.x, fx.y, cam)
        when (fx.type) {
          VisualEffectType.EXPLOSION -> engine.playSfx(GameSfx.EXPLOSION, volume = 0.90f * atten, pan = pan)
          VisualEffectType.ATTACK_SPARK -> engine.playSfx(GameSfx.SWORD_CLASH, volume = 0.56f * atten, pan = pan)
          else -> Unit
        }
      }
    }
    for (p in nextWorld.projectiles) {
      if (p.id > prevMaxProjId) {
        val (atten, pan) = spatialAudioFor(p.startX, p.startY, cam)
        when (p.kind) {
          ProjectileKind.ARROW, ProjectileKind.BOLT ->
            engine.playSfx(GameSfx.ARROW_TWANG, volume = 0.50f * atten, pan = pan)
          ProjectileKind.SNIPER_ROUND ->
            engine.playSfx(GameSfx.SNIPER_SHOT, volume = 0.76f * atten, pan = pan)
          ProjectileKind.SHELL ->
            engine.playSfx(GameSfx.CANNON_FIRE, volume = 0.82f * atten, pan = pan)
          ProjectileKind.MISSILE ->
            engine.playSfx(GameSfx.ROCKET_LAUNCH, volume = 0.74f * atten, pan = pan)
          ProjectileKind.BULLET -> {
            val isAutoBurst =
              nextWorld.units.any { u ->
                (u.type == UnitType.MACHINE_GUNNER || u.type == UnitType.FIGHTER_PLANE) &&
                  hypot(u.x - p.startX, u.y - p.startY) < 0.75f
              }
            engine.playSfx(if (isAutoBurst) GameSfx.MG_BURST else GameSfx.GUNSHOT, volume = 0.68f * atten, pan = pan)
          }
        }
        break
      }
    }
    if (nextWorld.playerState.age.index > prevWorld.playerState.age.index) {
      engine.playSfx(GameSfx.AGE_ADVANCE, volume = 0.95f)
    } else if (nextWorld.buildings.count { it.faction == Faction.PLAYER && it.isComplete } > prevCompletedBuildings) {
      engine.playSfx(GameSfx.BUILD_COMPLETE, volume = 0.82f)
    } else if (nextWorld.playerState.stats.unitsTrained > prevWorld.playerState.stats.unitsTrained) {
      engine.playSfx(GameSfx.UNIT_READY, volume = 0.72f)
    }

    workSfxAccumulator += dt
    if (workSfxAccumulator >= 0.65f) {
      workSfxAccumulator = 0f
      val workingVil =
        nextWorld.units
          .filter {
            it.faction == Faction.PLAYER &&
              it.type == UnitType.VILLAGER &&
              it.isAttackingAnim &&
              (it.order == UnitOrderType.BUILD || it.order == UnitOrderType.GATHER)
          }
          .minByOrNull { hypot(it.x - cam.centerWorldX, it.y - cam.centerWorldY) }
      if (workingVil != null) {
        val (atten, pan) = spatialAudioFor(workingVil.x, workingVil.y, cam)
        if (workingVil.order == UnitOrderType.BUILD) {
          engine.playSfx(GameSfx.BUILD_HAMMER, volume = 0.44f * atten, pan = pan)
        } else {
          val node = nextWorld.resources.firstOrNull { it.id == workingVil.targetResourceId }
          when (node?.type) {
            ResourceNodeType.FOREST_OAK, ResourceNodeType.FOREST_PINE ->
              engine.playSfx(GameSfx.WOOD_CHOP, volume = 0.44f * atten, pan = pan)
            ResourceNodeType.GOLD_MINE, ResourceNodeType.STONE_QUARRY ->
              engine.playSfx(GameSfx.MINE_PICK, volume = 0.42f * atten, pan = pan)
            else -> Unit
          }
        }
      }
    }
  }

  fun togglePause() {
    soundEngine?.playSfx(GameSfx.UI_CLICK, volume = 0.60f)
    _uiState.update { it.copy(isPaused = !it.isPaused) }
  }

  fun cycleSpeed() {
    _uiState.update {
      val next =
        when (it.gameSpeed) {
          1.0f -> 1.5f
          1.5f -> 2.0f
          else -> 1.0f
        }
      it.copy(gameSpeed = next)
    }
    showBanner("Game Speed: ${_uiState.value.gameSpeed}x")
  }

  fun toggleBoxSelectMode() {
    _uiState.update {
      it.copy(
        boxSelectMode = !it.boxSelectMode,
        attackMoveMode = false,
        pendingBuildType = null,
        pendingMoveBuildingId = null,
        pendingBuildTile = null,
      )
    }
  }

  fun toggleAttackMoveMode() {
    _uiState.update {
      it.copy(
        attackMoveMode = !it.attackMoveMode,
        boxSelectMode = false,
        pendingBuildType = null,
        pendingMoveBuildingId = null,
        pendingBuildTile = null,
      )
    }
  }

  fun startBuildPlacement(type: BuildingType) {
    val world = _uiState.value.world ?: return
    val pState = world.playerState
    val def = GameDefinitions.buildingDefs.getValue(type)
    val cost = GameDefinitions.adjustedBuildingCost(type, pState.civilization)
    if (pState.age.index < def.requiredAge.index) {
      soundEngine?.playSfx(GameSfx.ERROR_THUD, volume = 0.72f)
      showBanner("Requires ${def.requiredAge.displayName}")
      return
    }
    if (!pState.resources.canAfford(cost)) {
      soundEngine?.playSfx(GameSfx.ERROR_THUD, volume = 0.72f)
      showBanner("Need ${cost.summary()} to build ${type.displayName}")
      return
    }
    soundEngine?.playSfx(GameSfx.UI_CLICK, volume = 0.65f)
    _uiState.update {
      it.copy(
        pendingBuildType = type,
        pendingMoveBuildingId = null,
        pendingBuildTile = it.camera.centerWorldX.toInt() to it.camera.centerWorldY.toInt(),
        boxSelectMode = false,
        attackMoveMode = false,
      )
    }
    showBanner("Tap map to place ${type.displayName}")
  }

  fun startMoveSelectedBuilding() {
    val state = _uiState.value
    val world = state.world ?: return
    val bId = state.selectedBuildingId ?: return
    val b = world.buildings.firstOrNull { it.id == bId && it.faction == Faction.PLAYER } ?: return
    soundEngine?.playSfx(GameSfx.UI_CLICK, volume = 0.65f)
    _uiState.update {
      it.copy(
        pendingMoveBuildingId = b.id,
        pendingBuildType = null,
        pendingBuildTile = b.tileX to b.tileY,
        boxSelectMode = false,
        attackMoveMode = false,
      )
    }
    showBanner("Tap new location to move ${b.type.displayName}")
  }

  fun cancelBuildPlacement() {
    soundEngine?.playSfx(GameSfx.UI_CLICK, volume = 0.55f)
    _uiState.update { it.copy(pendingBuildType = null, pendingMoveBuildingId = null, pendingBuildTile = null) }
  }

  fun panCamera(deltaScreenX: Float, deltaScreenY: Float) {
    val state = _uiState.value
    val world = state.world ?: return
    val dWorld = IsometricMath.screenDeltaToWorldDelta(-deltaScreenX, -deltaScreenY, state.camera.zoom)
    val maxCoord = (world.mapSize - 2).toFloat()
    _uiState.update {
      it.copy(
        camera =
          it.camera.copy(
            centerWorldX = (it.camera.centerWorldX + dWorld.x).coerceIn(2f, maxCoord),
            centerWorldY = (it.camera.centerWorldY + dWorld.y).coerceIn(2f, maxCoord),
          )
      )
    }
  }

  fun zoomCamera(zoomFactor: Float) {
    _uiState.update {
      it.copy(camera = it.camera.copy(zoom = (it.camera.zoom * zoomFactor).coerceIn(0.45f, 1.95f)))
    }
  }

  fun jumpCameraTo(wx: Float, wy: Float) {
    val maxCoord = ((_uiState.value.world?.mapSize ?: MapGenerator.MAP_SIZE) - 2).toFloat()
    _uiState.update {
      it.copy(camera = it.camera.copy(centerWorldX = wx.coerceIn(2f, maxCoord), centerWorldY = wy.coerceIn(2f, maxCoord)))
    }
  }

  fun jumpToTownCenter() {
    val world = _uiState.value.world ?: return
    val tc = world.buildings.firstOrNull { it.faction == Faction.PLAYER && it.type == BuildingType.TOWN_CENTER }
      ?: world.buildings.firstOrNull { it.faction == Faction.PLAYER }
      ?: return
    _uiState.update {
      it.copy(
        camera = it.camera.copy(centerWorldX = tc.centerX, centerWorldY = tc.centerY),
        selectedBuildingId = tc.id,
        selectedUnitIds = emptySet(),
      )
    }
  }

  fun cycleIdleVillager() {
    val world = _uiState.value.world ?: return
    val idleVils =
      world.units.filter {
        it.faction == Faction.PLAYER && it.type == UnitType.VILLAGER && it.order == UnitOrderType.IDLE
      }
    if (idleVils.isNotEmpty()) {
      val chosen = idleVils[idleCycleIndex % idleVils.size]
      idleCycleIndex++
      _uiState.update {
        it.copy(
          camera = it.camera.copy(centerWorldX = chosen.x, centerWorldY = chosen.y),
          selectedUnitIds = setOf(chosen.id),
          selectedBuildingId = null,
          boxSelectMode = false,
          attackMoveMode = false,
        )
      }
      showBanner("${idleVils.size} Idle Villager${if (idleVils.size > 1) "s" else ""} — Auto-collecting shortly")
      return
    }

    val allVils = world.units.filter { it.faction == Faction.PLAYER && it.type == UnitType.VILLAGER }
    if (allVils.isEmpty()) {
      showBanner("No villagers — train Villagers at your Town Center")
      return
    }
    val chosen = allVils[idleCycleIndex % allVils.size]
    idleCycleIndex++
    _uiState.update {
      it.copy(
        camera = it.camera.copy(centerWorldX = chosen.x, centerWorldY = chosen.y),
        selectedUnitIds = setOf(chosen.id),
        selectedBuildingId = null,
        boxSelectMode = false,
        attackMoveMode = false,
      )
    }
    showBanner("All ${allVils.size} villagers working • Focused Villager")
  }

  fun autoGatherSelectedVillagers() {
    val state = _uiState.value
    var world = state.world ?: return
    val selectedVils =
      world.units.filter {
        it.id in state.selectedUnitIds && it.faction == Faction.PLAYER && it.type == UnitType.VILLAGER
      }
    if (selectedVils.isEmpty()) return
    val needed = RtsEngine.mostNeededResource(world, Faction.PLAYER)
    var assignedAny = false
    for (vil in selectedVils) {
      val next = RtsEngine.assignVillagerToMostNeededResource(world, vil.id)
      if (next != world) {
        world = next
        assignedAny = true
      }
    }
    _uiState.update { it.copy(world = world, pendingBuildType = null) }
    if (assignedAny) {
      soundEngine?.playSfx(GameSfx.ORDER_GATHER, volume = 0.72f)
      showBanner("Villager${if (selectedVils.size > 1) "s" else ""} gathering ${needed.displayName}")
    } else {
      soundEngine?.playSfx(GameSfx.ERROR_THUD, volume = 0.65f)
      showBanner("No resources within 15 tiles of Town Center")
    }
  }

  fun selectAllMilitary() {
    val world = _uiState.value.world ?: return
    val armyIds =
      world.units
        .filter { it.faction == Faction.PLAYER && it.type != UnitType.VILLAGER }
        .map { it.id }
        .toSet()
    if (armyIds.isEmpty()) {
      soundEngine?.playSfx(GameSfx.ERROR_THUD, volume = 0.60f)
      showBanner("Train troops at Barracks, Range, Stable, War Factory, or Airfield")
      return
    }
    soundEngine?.playSfx(GameSfx.SELECT_UNIT, volume = 0.78f)
    _uiState.update {
      it.copy(selectedUnitIds = armyIds, selectedBuildingId = null, pendingBuildType = null)
    }
    showBanner("${armyIds.size} military units selected")
  }

  fun stopSelectedUnits() {
    val state = _uiState.value
    val world = state.world ?: return
    if (state.selectedUnitIds.isEmpty()) return
    soundEngine?.playSfx(GameSfx.UI_CLICK, volume = 0.60f)
    _uiState.update {
      it.copy(world = RtsEngine.stopUnits(world, state.selectedUnitIds.toList()), attackMoveMode = false)
    }
  }

  fun trainUnit(unitType: UnitType) {
    val state = _uiState.value
    val world = state.world ?: return
    val bId = state.selectedBuildingId ?: return
    val building = world.buildings.firstOrNull { it.id == bId } ?: return
    val pState = world.playerState
    val uDef = GameDefinitions.unitDefs.getValue(unitType)
    val cost = GameDefinitions.adjustedUnitCost(unitType, pState.civilization)
    val popCap = RtsEngine.populationCap(world.buildings.filter { it.faction == Faction.PLAYER })
    val popCur = world.units.count { it.faction == Faction.PLAYER }

    when {
      building.queue.size >= RtsEngine.MAX_PRODUCTION_QUEUE -> {
        soundEngine?.playSfx(GameSfx.ERROR_THUD, volume = 0.68f)
        showBanner("Queue full (${RtsEngine.MAX_PRODUCTION_QUEUE}/${RtsEngine.MAX_PRODUCTION_QUEUE})")
      }
      popCur >= popCap -> {
        soundEngine?.playSfx(GameSfx.ERROR_THUD, volume = 0.68f)
        showBanner("Population limit reached — build a House!")
      }
      pState.age.index < uDef.requiredAge.index -> {
        soundEngine?.playSfx(GameSfx.ERROR_THUD, volume = 0.68f)
        showBanner("Requires ${uDef.requiredAge.displayName}")
      }
      !pState.resources.canAfford(cost) -> {
        soundEngine?.playSfx(GameSfx.ERROR_THUD, volume = 0.68f)
        showBanner("Need ${cost.summary()}")
      }
      else -> {
        soundEngine?.playSfx(GameSfx.UI_CLICK, volume = 0.65f)
        _uiState.update { it.copy(world = RtsEngine.enqueueUnit(world, Faction.PLAYER, bId, unitType)) }
      }
    }
  }

  fun researchTech(techType: TechType) {
    val state = _uiState.value
    val world = state.world ?: return
    val bId = state.selectedBuildingId ?: return
    val building = world.buildings.firstOrNull { it.id == bId } ?: return
    val pState = world.playerState
    val tDef = GameDefinitions.techDefs.getValue(techType)
    when {
      building.queue.size >= RtsEngine.MAX_PRODUCTION_QUEUE -> {
        soundEngine?.playSfx(GameSfx.ERROR_THUD, volume = 0.68f)
        showBanner("Queue full (${RtsEngine.MAX_PRODUCTION_QUEUE}/${RtsEngine.MAX_PRODUCTION_QUEUE})")
      }
      techType in pState.researchedTechs || techType in pState.researchingTechs -> return
      pState.age.index < tDef.requiredAge.index -> {
        soundEngine?.playSfx(GameSfx.ERROR_THUD, volume = 0.68f)
        showBanner("Requires ${tDef.requiredAge.displayName}")
      }
      !pState.resources.canAfford(tDef.cost) -> {
        soundEngine?.playSfx(GameSfx.ERROR_THUD, volume = 0.68f)
        showBanner("Need ${tDef.cost.summary()}")
      }
      else -> {
        soundEngine?.playSfx(GameSfx.UI_CLICK, volume = 0.70f)
        _uiState.update { it.copy(world = RtsEngine.enqueueTech(world, Faction.PLAYER, bId, techType)) }
      }
    }
  }

  fun cancelQueueItem(index: Int) {
    val state = _uiState.value
    val world = state.world ?: return
    val bId = state.selectedBuildingId ?: return
    val nextWorld = RtsEngine.cancelQueueItem(world, Faction.PLAYER, bId, index)
    if (nextWorld !== world) {
      _uiState.update { it.copy(world = nextWorld) }
    }
  }

  fun onMapTap(screenX: Float, screenY: Float, viewportW: Float, viewportH: Float) {
    val state = _uiState.value
    val world = state.world ?: return
    val worldPos = IsometricMath.screenToWorld(screenX, screenY, state.camera, viewportW, viewportH)
    val tx = worldPos.x.toInt()
    val ty = worldPos.y.toInt()

    // 1. Building relocation mode
    if (state.pendingMoveBuildingId != null) {
      val b = world.buildings.firstOrNull { it.id == state.pendingMoveBuildingId && it.faction == Faction.PLAYER }
      if (b == null) {
        _uiState.update { it.copy(pendingMoveBuildingId = null, pendingBuildTile = null) }
        return
      }
      if (RtsEngine.canPlaceBuilding(world, b.type, tx, ty, ignoreBuildingId = b.id)) {
        val nextWorld = RtsEngine.moveBuilding(world, Faction.PLAYER, b.id, tx, ty)
        soundEngine?.playSfx(GameSfx.MOVE_BUILDING, volume = 0.78f)
        _uiState.update {
          it.copy(
            world = nextWorld,
            selectedBuildingId = b.id,
            pendingMoveBuildingId = null,
            pendingBuildTile = null,
          )
        }
        showBanner("Relocated ${b.type.displayName}")
      } else {
        _uiState.update { it.copy(pendingBuildTile = tx to ty) }
        soundEngine?.playSfx(GameSfx.ERROR_THUD, volume = 0.68f)
        showBanner("Cannot relocate onto water, cliffs, or occupied tiles")
      }
      return
    }

    // 1b. Building placement mode
    if (state.pendingBuildType != null) {
      val bType = state.pendingBuildType
      if (state.pendingBuildTile == tx to ty && RtsEngine.canPlaceBuilding(world, bType, tx, ty)) {
        confirmBuildAt(tx, ty)
      } else {
        _uiState.update { it.copy(pendingBuildTile = tx to ty) }
        if (RtsEngine.canPlaceBuilding(world, bType, tx, ty)) {
          confirmBuildAt(tx, ty)
        } else {
          soundEngine?.playSfx(GameSfx.ERROR_THUD, volume = 0.68f)
          showBanner("Cannot build on water, cliffs, or occupied tiles")
        }
      }
      return
    }

    // 2. Attack-Move mode
    if (state.attackMoveMode && state.selectedUnitIds.isNotEmpty()) {
      soundEngine?.playSfx(GameSfx.ATTACK_HORN, volume = 0.70f)
      val nextWorld = RtsEngine.orderAttackMove(world, state.selectedUnitIds.toList(), worldPos.x, worldPos.y)
      _uiState.update { it.copy(world = nextWorld, attackMoveMode = false) }
      return
    }

    // 3. Hit-test entities in screen space
    val hitPlayerUnit = findNearestUnit(world.units.filter { it.faction == Faction.PLAYER }, screenX, screenY, state.camera, viewportW, viewportH, 54f)
    val hitEnemyUnit = findNearestUnit(world.units.filter { it.faction.isAi && world.fogAt(it.x.toInt(), it.y.toInt()) == 2 }, screenX, screenY, state.camera, viewportW, viewportH, 58f)
    val hitBuilding = findHitBuilding(world.buildings, world, screenX, screenY, worldPos.x, worldPos.y, state.camera, viewportW, viewportH)
    val hitResource = findNearestResource(world.resources.filter { world.fogAt(it.tileX, it.tileY) > 0 }, screenX, screenY, state.camera, viewportW, viewportH, 58f)

    // Selecting own unit (with double-tap to select all visible of same type)
    if (hitPlayerUnit != null && (state.selectedUnitIds.isEmpty() || hitPlayerUnit.id !in state.selectedUnitIds)) {
      soundEngine?.playSfx(GameSfx.SELECT_UNIT, volume = 0.72f)
      val now = System.currentTimeMillis()
      if (now - lastTapTimeMs < 380L && lastTapUnitType == hitPlayerUnit.type) {
        val sameTypeIds =
          world.units
            .filter { it.faction == Faction.PLAYER && it.type == hitPlayerUnit.type }
            .map { it.id }
            .toSet()
        _uiState.update { it.copy(selectedUnitIds = sameTypeIds, selectedBuildingId = null) }
        showBanner("Selected ${sameTypeIds.size} ${hitPlayerUnit.type.displayName}s")
      } else {
        _uiState.update { it.copy(selectedUnitIds = setOf(hitPlayerUnit.id), selectedBuildingId = null) }
      }
      lastTapTimeMs = now
      lastTapUnitType = hitPlayerUnit.type
      return
    }

    // Issuing commands or selecting buildings
    if (state.selectedUnitIds.isNotEmpty()) {
      val unitIds = state.selectedUnitIds.toList()
      val hasVillagers = world.units.any { it.id in state.selectedUnitIds && it.type == UnitType.VILLAGER }

      when {
        hitEnemyUnit != null -> {
          soundEngine?.playSfx(GameSfx.ATTACK_HORN, volume = 0.74f)
          _uiState.update { it.copy(world = RtsEngine.orderAttackUnit(world, unitIds, hitEnemyUnit.id)) }
        }
        hitBuilding != null && hitBuilding.faction.isAi -> {
          soundEngine?.playSfx(GameSfx.ATTACK_HORN, volume = 0.74f)
          _uiState.update { it.copy(world = RtsEngine.orderAttackBuilding(world, unitIds, hitBuilding.id)) }
        }
        hasVillagers && hitBuilding != null && hitBuilding.faction == Faction.PLAYER &&
          (!hitBuilding.isComplete || hitBuilding.hp < hitBuilding.maxHp) -> {
          soundEngine?.playSfx(GameSfx.BUILD_HAMMER, volume = 0.68f)
          _uiState.update { it.copy(world = RtsEngine.orderBuild(world, unitIds, hitBuilding.id)) }
        }
        hasVillagers && hitBuilding != null && hitBuilding.faction == Faction.PLAYER &&
          hitBuilding.type == BuildingType.FARM && hitBuilding.isComplete -> {
          soundEngine?.playSfx(GameSfx.ORDER_GATHER, volume = 0.68f)
          _uiState.update { it.copy(world = RtsEngine.orderGatherFarm(world, unitIds, hitBuilding.id)) }
        }
        hitBuilding != null && hitBuilding.faction == Faction.PLAYER -> {
          soundEngine?.playSfx(GameSfx.SELECT_BUILDING, volume = 0.72f)
          // Tapping a completed healthy player structure (e.g. Barracks, TC, Range) opens its command menu
          _uiState.update { it.copy(selectedBuildingId = hitBuilding.id, selectedUnitIds = emptySet(), attackMoveMode = false) }
        }
        hasVillagers && hitResource != null -> {
          soundEngine?.playSfx(GameSfx.ORDER_GATHER, volume = 0.70f)
          _uiState.update { it.copy(world = RtsEngine.orderGatherNode(world, unitIds, hitResource.id)) }
        }
        else -> {
          soundEngine?.playSfx(GameSfx.ORDER_MOVE, volume = 0.68f)
          _uiState.update { it.copy(world = RtsEngine.orderMove(world, unitIds, worldPos.x, worldPos.y)) }
        }
      }
      return
    }

    // Selecting own building if no units selected
    if (hitBuilding != null && hitBuilding.faction == Faction.PLAYER) {
      soundEngine?.playSfx(GameSfx.SELECT_BUILDING, volume = 0.72f)
      _uiState.update { it.copy(selectedBuildingId = hitBuilding.id, selectedUnitIds = emptySet()) }
    } else {
      _uiState.update { it.copy(selectedUnitIds = emptySet(), selectedBuildingId = null) }
    }
  }

  fun onBoxSelect(rect: Rect, viewportW: Float, viewportH: Float) {
    val state = _uiState.value
    val world = state.world ?: return
    val inside =
      world.units.filter { u ->
        if (u.faction != Faction.PLAYER) return@filter false
        val s = IsometricMath.worldToScreen(u.x, u.y, state.camera, viewportW, viewportH)
        rect.contains(Offset(s.x, s.y - 20f * state.camera.zoom))
      }
    if (inside.isEmpty()) {
      _uiState.update { it.copy(boxSelectMode = false) }
      return
    }
    soundEngine?.playSfx(GameSfx.SELECT_UNIT, volume = 0.74f)
    val military = inside.filter { it.type != UnitType.VILLAGER }
    val chosen = if (military.isNotEmpty()) military else inside
    _uiState.update {
      it.copy(
        selectedUnitIds = chosen.map { u -> u.id }.toSet(),
        selectedBuildingId = null,
        boxSelectMode = false,
      )
    }
    showBanner("Selected ${chosen.size} units")
  }

  private fun confirmBuildAt(tx: Int, ty: Int) {
    val state = _uiState.value
    val world = state.world ?: return
    val bType = state.pendingBuildType ?: return
    val selectedVils =
      world.units
        .filter { it.id in state.selectedUnitIds && it.type == UnitType.VILLAGER }
        .map { it.id }
    val builders =
      if (selectedVils.isNotEmpty()) selectedVils
      else {
        val fallback =
          world.units
            .filter { it.faction == Faction.PLAYER && it.type == UnitType.VILLAGER }
            .minByOrNull { hypot(it.x - tx, it.y - ty) }
        listOfNotNull(fallback?.id)
      }
    if (builders.isEmpty()) {
      soundEngine?.playSfx(GameSfx.ERROR_THUD, volume = 0.70f)
      showBanner("Train a Villager at your Town Center first!")
      return
    }
    soundEngine?.playSfx(GameSfx.BUILD_HAMMER, volume = 0.80f)
    val nextWorld = RtsEngine.placeBuilding(world, Faction.PLAYER, bType, tx, ty, builders)
    val leadBuilder = nextWorld.units.firstOrNull { it.id in builders }
    val queuedCount = leadBuilder?.buildQueue?.size ?: 0
    _uiState.update {
      it.copy(
        world = nextWorld,
        pendingBuildType = null,
        pendingBuildTile = null,
        selectedUnitIds = builders.toSet(),
      )
    }
    if (queuedCount > 0) {
      showBanner("Queued ${bType.displayName} (${queuedCount + 1} in build queue)")
    }
  }

  private fun findNearestUnit(
    units: List<Unit>,
    sx: Float,
    sy: Float,
    cam: CameraState,
    vw: Float,
    vh: Float,
    maxPx: Float,
  ): Unit? {
    return units
      .map { u ->
        val p = IsometricMath.worldToScreen(u.x, u.y, cam, vw, vh)
        val yOff = if (GameDefinitions.unitDefs.getValue(u.type).isFlying) 48f else 22f
        u to hypot(p.x - sx, (p.y - yOff * cam.zoom) - sy)
      }
      .filter { it.second <= maxPx }
      .minByOrNull { it.second }
      ?.first
  }

  private fun findNearestResource(
    nodes: List<ResourceNode>,
    sx: Float,
    sy: Float,
    cam: CameraState,
    vw: Float,
    vh: Float,
    maxPx: Float,
  ): ResourceNode? {
    return nodes
      .map { r ->
        val p = IsometricMath.worldToScreen(r.x, r.y, cam, vw, vh)
        r to hypot(p.x - sx, (p.y - 26f * cam.zoom) - sy)
      }
      .filter { it.second <= maxPx }
      .minByOrNull { it.second }
      ?.first
  }

  private fun findHitBuilding(
    buildings: List<Building>,
    world: GameWorld,
    sx: Float,
    sy: Float,
    wx: Float,
    wy: Float,
    cam: CameraState,
    vw: Float,
    vh: Float,
  ): Building? {
    return buildings
      .filter { b -> b.faction == Faction.PLAYER || world.buildingMaxFog(b) > 0 }
      .sortedByDescending { it.centerX + it.centerY }
      .firstOrNull { b ->
        val inFootprint =
          wx >= b.tileX - 0.22f &&
            wx <= b.tileX + b.footprint + 0.22f &&
            wy >= b.tileY - 0.22f &&
            wy <= b.tileY + b.footprint + 0.22f
        if (inFootprint) return@firstOrNull true

        val center = IsometricMath.worldToScreen(b.centerX, b.centerY, cam, vw, vh)
        val halfW = b.footprint * (IsometricMath.BASE_TILE_W * 0.44f) * cam.zoom
        val halfH = b.footprint * (IsometricMath.BASE_TILE_H * 0.5f) * cam.zoom
        val roofTop =
          when (b.type) {
            BuildingType.CASTLE, BuildingType.WATCH_TOWER, BuildingType.TOWN_CENTER -> 150f
            BuildingType.UNIVERSITY -> 132f
            BuildingType.MILL -> 110f
            BuildingType.WAR_FACTORY -> 108f
            BuildingType.AIRFIELD -> 115f
            BuildingType.FARM -> 14f
            else -> 92f
          } * cam.zoom
        sx >= center.x - halfW &&
          sx <= center.x + halfW &&
          sy >= center.y - roofTop &&
          sy <= center.y + halfH
      }
  }

  private fun showBanner(msg: String) {
    bannerTimer = 2.6f
    _uiState.update { it.copy(statusBanner = msg) }
  }
}
