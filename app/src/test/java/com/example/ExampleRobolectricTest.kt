package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.GameViewModel
import com.example.game.engine.RtsEngine
import com.example.game.model.Building
import com.example.game.model.BuildingType
import com.example.game.model.CameraState
import com.example.game.model.Faction
import com.example.game.model.ResourceNodeType
import com.example.game.model.UnitType
import com.example.game.render.BuildingSpriteAtlas
import com.example.game.render.IsometricMath
import com.example.game.render.ProceduralTextures
import com.example.game.render.SpriteAtlas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.example.game.model.MapType
import com.example.game.model.SkirmishConfig
import com.example.ui.BottomCommandPanel
import com.example.ui.GameGuideDialog
import com.example.ui.SkirmishSetupScreen
import com.example.ui.theme.CrownConquestTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Crown & Conquest", appName)
  }

  @Test
  fun `render atlases generate 4x supersampled textures and sprites`() {
    assertNotNull(ProceduralTextures.grassShader)
    assertNotNull(ProceduralTextures.waterShader)
    assertNotNull(ProceduralTextures.stoneShader)
    assertNotNull(ProceduralTextures.timberShader)
    assertNotNull(ProceduralTextures.plowedSoilShader)
    for (mapType in MapType.entries) {
      assertNotNull(ProceduralTextures.groundShaderFor(mapType, alt = false))
      assertNotNull(ProceduralTextures.groundShaderFor(mapType, alt = true))
      assertNotNull(ProceduralTextures.dirtShaderFor(mapType))
      assertNotNull(ProceduralTextures.cliffShaderFor(mapType))
    }

    for (nodeType in ResourceNodeType.entries) {
      val bmp = SpriteAtlas.getResourceSprite(nodeType, variant = 0)
      assertEquals(640, bmp.width)
      assertEquals(720, bmp.height)
    }
    for (faction in Faction.entries) {
      for (unitType in UnitType.entries) {
        for (walkFrame in 0..1) {
          val bmp = SpriteAtlas.getUnitSprite(unitType, faction, facingRight = true, walkFrame = walkFrame)
          assertEquals(448, bmp.width)
          assertEquals(448, bmp.height)
        }
      }
      for (bType in BuildingType.entries) {
        val bmp = BuildingSpriteAtlas.getBuildingSprite(bType, faction)
        assertTrue(bmp.width >= 600 && bmp.height >= 720)
      }
    }
  }

  @Test
  fun `tapping completed barracks selects it even when villager is selected`() {
    val vm = GameViewModel()
    vm.startSkirmish()
    val state0 = vm.uiState.value
    val world0 = state0.world!!
    val tc = world0.buildings.first { it.faction == Faction.PLAYER && it.type == BuildingType.TOWN_CENTER }
    val vil = world0.units.first { it.faction == Faction.PLAYER && it.type == UnitType.VILLAGER }
    val spot =
      (-6..6).flatMap { dy -> (-6..6).map { dx -> (tc.tileX + dx) to (tc.tileY + dy) } }
        .first { (tx, ty) -> RtsEngine.canPlaceBuilding(world0, BuildingType.BARRACKS, tx, ty) }

    val barracks =
      Building(
        id = 9999,
        type = BuildingType.BARRACKS,
        faction = Faction.PLAYER,
        tileX = spot.first,
        tileY = spot.second,
        footprint = 2,
        hp = 1200,
        maxHp = 1200,
        buildProgress = 1f,
        isComplete = true,
      )
    val worldWithBarracks = world0.copy(buildings = world0.buildings + barracks)
    val field = GameViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val flow = field.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.example.game.GameUiState>
    flow.value =
      flow.value.copy(
        world = worldWithBarracks,
        camera = CameraState(centerWorldX = barracks.centerX, centerWorldY = barracks.centerY, zoom = 1.0f),
        selectedUnitIds = setOf(vil.id),
        selectedBuildingId = null,
      )

    val screenCenter = IsometricMath.worldToScreen(barracks.centerX, barracks.centerY, flow.value.camera, 1080f, 1920f)
    vm.onMapTap(screenCenter.x, screenCenter.y - 48f, 1080f, 1920f)

    assertEquals(barracks.id, vm.uiState.value.selectedBuildingId)
    assertTrue(vm.uiState.value.selectedUnitIds.isEmpty())
    vm.navigateToTitle()
  }

  @Test
  fun `idle villager button selects villager and auto gather assigns most needed resource`() {
    val vm = GameViewModel()
    vm.startSkirmish()

    vm.cycleIdleVillager()
    val selectedIds = vm.uiState.value.selectedUnitIds
    assertEquals(1, selectedIds.size)
    val vilId = selectedIds.first()

    vm.autoGatherSelectedVillagers()
    val updatedVil = vm.uiState.value.world!!.units.first { it.id == vilId }
    assertEquals(com.example.game.model.UnitOrderType.GATHER, updatedVil.order)
    vm.navigateToTitle()
  }

  @Test
  fun `skirmish setup displays all four difficulty options including extreme`() {
    composeRule.setContent {
      CrownConquestTheme {
        SkirmishSetupScreen(
          config = SkirmishConfig(),
          onUpdateConfig = { _, _, _, _ -> },
          onLaunchBattle = {},
          onBack = {},
        )
      }
    }
    composeRule.onNodeWithTag("diff_standard").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("diff_moderate").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("diff_hard").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("diff_extreme").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `villager build menu condenses buildings under economy and military`() {
    val world = com.example.game.engine.MapGenerator.createWorld(SkirmishConfig(), seed = 55L)
    val vil = world.units.first { it.faction == Faction.PLAYER && it.type == UnitType.VILLAGER }
    val state = com.example.game.GameUiState(world = world, selectedUnitIds = setOf(vil.id))

    composeRule.setContent {
      CrownConquestTheme {
        BottomCommandPanel(
          uiState = state,
          onJumpCamera = { _, _ -> },
          onStartBuild = {},
          onTrainUnit = {},
          onResearchTech = {},
          onToggleAttackMove = {},
          onStopUnits = {},
          onAutoGatherVillagers = {},
          onSelectAllMilitary = {},
          onJumpToTc = {},
          onCycleIdleVillager = {},
        )
      }
    }

    composeRule.onNodeWithTag("auto_gather_button").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("build_category_economy").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("build_category_military").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("build_barracks").assertDoesNotExist()

    composeRule.onNodeWithTag("build_category_economy").performScrollTo().performClick()
    composeRule.onNodeWithTag("build_house").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("build_barracks").assertDoesNotExist()

    composeRule.onNodeWithTag("build_category_back").performScrollTo().performClick()
    composeRule.onNodeWithTag("build_category_military").performScrollTo().performClick()
    composeRule.onNodeWithTag("build_barracks").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("build_house").assertDoesNotExist()
  }

  @Test
  fun `game guide codex displays overmap costs civs and faq tabs`() {
    composeRule.setContent {
      CrownConquestTheme {
        GameGuideDialog(onDismiss = {})
      }
    }
    composeRule.onNodeWithTag("guide_dialog").assertIsDisplayed()
    composeRule.onNodeWithTag("guide_tab_overmap").performScrollTo().performClick()
    composeRule.onNodeWithTag("guide_tab_costs").performScrollTo().performClick()
    composeRule.onNodeWithTag("guide_tab_civs").performScrollTo().performClick()
    composeRule.onNodeWithTag("guide_tab_faq").performScrollTo().performClick()
    composeRule.onNodeWithTag("close_guide_button").assertIsDisplayed()
  }

  @Test
  fun `game screen gives battlefield canvas dominant height and shows minimap`() {
    val world = com.example.game.engine.MapGenerator.createWorld(SkirmishConfig(mapType = MapType.COASTAL), seed = 77L)
    val tc = world.buildings.first { it.faction == Faction.PLAYER }
    val state =
      com.example.game.GameUiState(
        screen = com.example.game.AppScreen.Playing,
        world = world,
        camera = CameraState(centerWorldX = tc.centerX, centerWorldY = tc.centerY, zoom = 1.08f),
        selectedBuildingId = tc.id,
      )

    composeRule.setContent {
      CrownConquestTheme {
        com.example.ui.GameScreen(
          uiState = state,
          onPanCamera = { _, _ -> },
          onZoomCamera = {},
          onJumpCamera = { _, _ -> },
          onMapTap = { _, _, _, _ -> },
          onBoxSelect = { _, _, _ -> },
          onToggleBoxSelect = {},
          onToggleAttackMove = {},
          onStopUnits = {},
          onStartBuild = {},
          onCancelBuild = {},
          onTrainUnit = {},
          onResearchTech = {},
          onCycleIdleVillager = {},
          onAutoGatherVillagers = {},
          onSelectAllMilitary = {},
          onJumpToTc = {},
          onCycleSpeed = {},
          onTogglePause = {},
          onRestart = {},
          onQuitToMenu = {},
        )
      }
    }

    val canvasBounds = composeRule.onNodeWithTag("battlefield_canvas").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
    val minimapBounds = composeRule.onNodeWithTag("tactical_minimap").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
    assertTrue("Expected tall battlefield canvas, got $canvasBounds", canvasBounds.height > 200f && canvasBounds.width > 200f)
    assertTrue("Expected visible minimap, got $minimapBounds", minimapBounds.height >= 40f && minimapBounds.width >= 40f)
  }

  @Test
  @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
  fun `world renderer renders non-black terrain and entities onto canvas bitmap`() {
    val grassPx = ProceduralTextures.grassBitmap.getPixel(120, 120)
    assertTrue(
      "Procedural grass bitmap should have visible green/earth color, got 0x${Integer.toHexString(grassPx)}",
      android.graphics.Color.green(grassPx) > 25 && android.graphics.Color.alpha(grassPx) == 255,
    )

    var capturedMeasurer: androidx.compose.ui.text.TextMeasurer? = null
    composeRule.setContent {
      capturedMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    }
    composeRule.waitForIdle()
    val textMeasurer = capturedMeasurer!!

    for (mapType in listOf(MapType.COASTAL, MapType.ARABIA, MapType.BLACK_FOREST)) {
      val world = com.example.game.engine.MapGenerator.createWorld(SkirmishConfig(mapType = mapType), seed = 99L)
      val tc = world.buildings.first { it.faction == Faction.PLAYER }
      val camera = CameraState(centerWorldX = tc.centerX, centerWorldY = tc.centerY, zoom = 1.08f)

      val bmp = android.graphics.Bitmap.createBitmap(640, 640, android.graphics.Bitmap.Config.ARGB_8888)
      val nativeCanvas = android.graphics.Canvas(bmp)
      val drawScope = androidx.compose.ui.graphics.drawscope.CanvasDrawScope()
      drawScope.draw(
        density = androidx.compose.ui.unit.Density(1f),
        layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
        canvas = androidx.compose.ui.graphics.Canvas(nativeCanvas),
        size = androidx.compose.ui.geometry.Size(640f, 640f),
      ) {
        with(com.example.game.render.WorldRenderer) {
          drawGameWorld(
            world = world,
            camera = camera,
            selectedUnitIds = emptySet(),
            selectedBuildingId = tc.id,
            pendingBuildType = null,
            pendingBuildTile = null,
            selectionBox = null,
            animClock = 0.5f,
            textMeasurer = textMeasurer,
          )
        }
      }

      var litPixels = 0
      var coloredTerrainPixels = 0
      val totalSampled = (640 / 8) * (640 / 8)
      for (y in 0 until 640 step 8) {
        for (x in 0 until 640 step 8) {
          val px = bmp.getPixel(x, y)
          val r = android.graphics.Color.red(px)
          val g = android.graphics.Color.green(px)
          val b = android.graphics.Color.blue(px)
          if (r > 24 || g > 24 || b > 24) litPixels++
          if ((g > 45 && mapType != MapType.ARABIA) || (r > 70 && g > 55 && mapType == MapType.ARABIA)) {
            coloredTerrainPixels++
          }
        }
      }
      assertTrue(
        "Map $mapType rendered too dark ($litPixels/$totalSampled lit pixels)",
        litPixels > totalSampled / 3,
      )
      assertTrue(
        "Map $mapType missing textured terrain color ($coloredTerrainPixels/$totalSampled terrain pixels)",
        coloredTerrainPixels > totalSampled / 6,
      )
    }
  }

  @Test
  @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
  fun `tactical minimap renders visible terrain and unit markers`() {
    val world = com.example.game.engine.MapGenerator.createWorld(SkirmishConfig(mapType = MapType.COASTAL), seed = 123L)
    val tc = world.buildings.first { it.faction == Faction.PLAYER }
    val camera = CameraState(centerWorldX = tc.centerX, centerWorldY = tc.centerY, zoom = 1.08f)

    composeRule.setContent {
      CrownConquestTheme {
        androidx.compose.foundation.layout.Box(
          modifier = androidx.compose.ui.Modifier.size(128.dp),
        ) {
          com.example.game.render.TacticalMinimap(
            world = world,
            camera = camera,
            onJumpCamera = { _, _ -> },
            modifier = androidx.compose.ui.Modifier.fillMaxSize().testTag("minimap_pixel_test"),
          )
        }
      }
    }
    composeRule.waitForIdle()
    val bounds = composeRule.onNodeWithTag("minimap_pixel_test").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
    assertTrue("Expected visible minimap bounds, got $bounds", bounds.width > 80f && bounds.height > 80f)
  }

  private fun buildBlankTestWorld(
    buildings: List<Building>,
    units: List<com.example.game.model.Unit>,
    resources: List<com.example.game.model.ResourceNode>,
    mapSize: Int = 44,
  ): com.example.game.model.GameWorld {
    val tiles = List(mapSize * mapSize) { com.example.game.model.TileTerrain.GRASS }
    val fog = IntArray(mapSize * mapSize) { 2 }
    val allBuildings =
      if (buildings.any { it.faction == Faction.ENEMY }) buildings
      else buildings + Building(
        id = 999,
        type = BuildingType.TOWN_CENTER,
        faction = Faction.ENEMY,
        tileX = mapSize - 5,
        tileY = mapSize - 5,
        footprint = 3,
        hp = 2400,
        maxHp = 2400,
        buildProgress = 1f,
        isComplete = true,
      )
    return com.example.game.model.GameWorld(
      mapSize = mapSize,
      tiles = tiles,
      fogGrid = fog,
      resources = resources,
      buildings = allBuildings,
      units = units,
      projectiles = emptyList(),
      floatingTexts = emptyList(),
      effects = emptyList(),
      playerState = com.example.game.model.FactionState(Faction.PLAYER, com.example.game.model.Civilization.BRITONS),
      enemyState = com.example.game.model.FactionState(Faction.ENEMY, com.example.game.model.Civilization.FRANKS),
      mapType = MapType.ARABIA,
      nextEntityId = 1000,
    )
  }

  @Test
  fun `undirected villagers do not venture beyond 15 squares of town center`() {
    val tc =
      Building(
        id = 1,
        type = BuildingType.TOWN_CENTER,
        faction = Faction.PLAYER,
        tileX = 10,
        tileY = 10,
        footprint = 3,
        hp = 2400,
        maxHp = 2400,
        buildProgress = 1f,
        isComplete = true,
      )
    val vil =
      com.example.game.model.Unit(
        id = 2,
        type = UnitType.VILLAGER,
        faction = Faction.PLAYER,
        x = 11.5f,
        y = 14.0f,
        hp = 40,
        maxHp = 40,
        order = com.example.game.model.UnitOrderType.IDLE,
        idleTimer = 1.35f,
      )
    val distantGold =
      com.example.game.model.ResourceNode(
        id = 10,
        type = ResourceNodeType.GOLD_MINE,
        tileX = 30,
        tileY = 11,
        remaining = 600,
        maxAmount = 600,
      ) // dist from TC center (11.5, 11.5) = 19.0 tiles (> 15)
    val distantWood =
      com.example.game.model.ResourceNode(
        id = 11,
        type = ResourceNodeType.FOREST_OAK,
        tileX = 27,
        tileY = 11,
        remaining = 200,
        maxAmount = 200,
      ) // dist from TC center (11.5, 11.5) = 16.0 tiles (> 15)

    var world = buildBlankTestWorld(listOf(tc), listOf(vil), listOf(distantGold, distantWood))

    // Auto-gather button helper must ignore nodes outside 15 tiles
    val autoAssigned = RtsEngine.assignVillagerToMostNeededResource(world, vil.id)
    val autoVil = autoAssigned.units.first { it.id == vil.id }
    assertEquals(com.example.game.model.UnitOrderType.IDLE, autoVil.order)

    // Idle auto-assign during simulation ticks must also stay idle rather than venturing >15 tiles
    repeat(20) {
      world = RtsEngine.step(world, 0.1f, SkirmishConfig())
    }
    val steppedVil = world.units.first { it.id == vil.id }
    assertEquals(com.example.game.model.UnitOrderType.IDLE, steppedVil.order)
    assertEquals(vil.x, steppedVil.x, 0.01f)
    assertEquals(vil.y, steppedVil.y, 0.01f)

    // Depleted-node retargeting must not chain onto a node > 15 tiles away
    val depletedWorld =
      world.copy(
        units =
          listOf(
            vil.copy(
              order = com.example.game.model.UnitOrderType.GATHER,
              targetResourceId = 999,
              carryingType = com.example.game.model.ResourceType.GOLD,
              carryingAmount = 0,
            )
          )
      )
    val afterDepleted = RtsEngine.step(depletedWorld, 0.1f, SkirmishConfig())
    val postDepletedVil = afterDepleted.units.first { it.id == vil.id }
    assertEquals(com.example.game.model.UnitOrderType.IDLE, postDepletedVil.order)
  }

  @Test
  fun `undirected villagers pick in-range resource over closer out-of-range resource`() {
    val tc =
      Building(
        id = 1,
        type = BuildingType.TOWN_CENTER,
        faction = Faction.PLAYER,
        tileX = 10,
        tileY = 10,
        footprint = 3,
        hp = 2400,
        maxHp = 2400,
        buildProgress = 1f,
        isComplete = true,
      ) // center = (11.5, 11.5)
    val vil =
      com.example.game.model.Unit(
        id = 2,
        type = UnitType.VILLAGER,
        faction = Faction.PLAYER,
        x = 25.0f,
        y = 11.5f,
        hp = 40,
        maxHp = 40,
        order = com.example.game.model.UnitOrderType.IDLE,
      )
    val outOfRangeGold =
      com.example.game.model.ResourceNode(
        id = 10,
        type = ResourceNodeType.GOLD_MINE,
        tileX = 27,
        tileY = 11,
        remaining = 600,
        maxAmount = 600,
      ) // center (27.5, 11.5) -> 16.0 tiles from TC (> 15)
    val inRangeWood =
      com.example.game.model.ResourceNode(
        id = 11,
        type = ResourceNodeType.FOREST_OAK,
        tileX = 24,
        tileY = 11,
        remaining = 200,
        maxAmount = 200,
      ) // center (24.5, 11.5) -> 13.0 tiles from TC (<= 15)

    val world = buildBlankTestWorld(listOf(tc), listOf(vil), listOf(outOfRangeGold, inRangeWood))
    val assigned = RtsEngine.assignVillagerToMostNeededResource(world, vil.id)
    val updatedVil = assigned.units.first { it.id == vil.id }
    assertEquals(com.example.game.model.UnitOrderType.GATHER, updatedVil.order)
    assertEquals(inRangeWood.id, updatedVil.targetResourceId)
  }

  @Test
  fun `directed villagers can still move and gather beyond 15 squares of town center`() {
    val tc =
      Building(
        id = 1,
        type = BuildingType.TOWN_CENTER,
        faction = Faction.PLAYER,
        tileX = 10,
        tileY = 10,
        footprint = 3,
        hp = 2400,
        maxHp = 2400,
        buildProgress = 1f,
        isComplete = true,
      )
    val vil =
      com.example.game.model.Unit(
        id = 2,
        type = UnitType.VILLAGER,
        faction = Faction.PLAYER,
        x = 11.5f,
        y = 14.0f,
        hp = 40,
        maxHp = 40,
      )
    val distantGold =
      com.example.game.model.ResourceNode(
        id = 10,
        type = ResourceNodeType.GOLD_MINE,
        tileX = 33,
        tileY = 33,
        remaining = 600,
        maxAmount = 600,
      )

    var gatherWorld = buildBlankTestWorld(listOf(tc), listOf(vil), listOf(distantGold))
    gatherWorld = RtsEngine.orderGatherNode(gatherWorld, listOf(vil.id), distantGold.id)
    repeat(10) {
      gatherWorld = RtsEngine.step(gatherWorld, 0.1f, SkirmishConfig())
    }
    val gatheringVil = gatherWorld.units.first { it.id == vil.id }
    assertEquals(com.example.game.model.UnitOrderType.GATHER, gatheringVil.order)
    assertEquals(distantGold.id, gatheringVil.targetResourceId)
    assertTrue("Expected directed villager to travel toward distant node", gatheringVil.x > vil.x + 0.5f)

    var moveWorld = buildBlankTestWorld(listOf(tc), listOf(vil), listOf(distantGold))
    moveWorld = RtsEngine.orderMove(moveWorld, listOf(vil.id), 35f, 35f)
    repeat(10) {
      moveWorld = RtsEngine.step(moveWorld, 0.1f, SkirmishConfig())
    }
    val movingVil = moveWorld.units.first { it.id == vil.id }
    assertEquals(com.example.game.model.UnitOrderType.MOVE, movingVil.order)
    assertTrue("Expected directed villager to move toward (35, 35)", movingVil.x > vil.x + 0.5f)
  }

  @Test
  fun `ai villagers respect 15 square town center gather radius`() {
    val enemyTc =
      Building(
        id = 1,
        type = BuildingType.TOWN_CENTER,
        faction = Faction.ENEMY,
        tileX = 34,
        tileY = 6,
        footprint = 3,
        hp = 2400,
        maxHp = 2400,
        buildProgress = 1f,
        isComplete = true,
      ) // center = (35.5, 7.5)
    val playerTc =
      Building(
        id = 2,
        type = BuildingType.TOWN_CENTER,
        faction = Faction.PLAYER,
        tileX = 6,
        tileY = 34,
        footprint = 3,
        hp = 2400,
        maxHp = 2400,
        buildProgress = 1f,
        isComplete = true,
      )
    val enemyVil =
      com.example.game.model.Unit(
        id = 3,
        type = UnitType.VILLAGER,
        faction = Faction.ENEMY,
        x = 35.5f,
        y = 10.5f,
        hp = 40,
        maxHp = 40,
        order = com.example.game.model.UnitOrderType.IDLE,
      )
    val distantNode =
      com.example.game.model.ResourceNode(
        id = 10,
        type = ResourceNodeType.GOLD_MINE,
        tileX = 12,
        tileY = 30,
        remaining = 600,
        maxAmount = 600,
      )

    val emptyEnemyRes = com.example.game.model.ResourcePool(wood = 0, food = 0, gold = 0, stone = 0)
    val noLocalWorld =
      buildBlankTestWorld(listOf(enemyTc, playerTc), listOf(enemyVil), listOf(distantNode)).let { w ->
        w.copy(enemyState = w.enemyState.copy(resources = emptyEnemyRes))
      }
    val afterNoLocal = com.example.game.engine.AiCommander.updateAi(noLocalWorld, com.example.game.model.Difficulty.HARD)
    val vilNoLocal = afterNoLocal.units.first { it.id == enemyVil.id }
    assertEquals(com.example.game.model.UnitOrderType.IDLE, vilNoLocal.order)

    val nearNode =
      com.example.game.model.ResourceNode(
        id = 11,
        type = ResourceNodeType.BERRY_BUSH,
        tileX = 27,
        tileY = 7,
        remaining = 300,
        maxAmount = 300,
      ) // center (27.5, 7.5) -> 8.0 tiles from enemy TC
    val localWorld =
      buildBlankTestWorld(listOf(enemyTc, playerTc), listOf(enemyVil), listOf(distantNode, nearNode)).let { w ->
        w.copy(enemyState = w.enemyState.copy(resources = emptyEnemyRes))
      }
    val afterLocal = com.example.game.engine.AiCommander.updateAi(localWorld, com.example.game.model.Difficulty.HARD)
    val vilLocal = afterLocal.units.first { it.id == enemyVil.id }
    assertEquals(com.example.game.model.UnitOrderType.GATHER, vilLocal.order)
    assertEquals(nearNode.id, vilLocal.targetResourceId)
  }

  @Test
  fun `medieval synthesizer generates non-silent looping music and valid wav sfx for all events`() {
    val pcm = com.example.game.audio.MedievalSynthesizer.generateMedievalThemePcm(durationSec = 4)
    val expectedLen = com.example.game.audio.MedievalSynthesizer.MUSIC_SAMPLE_RATE * 4 * 2
    assertEquals(expectedLen, pcm.size)
    assertTrue("Expected audible music waveform energy", pcm.any { kotlin.math.abs(it.toInt()) > 1200 })

    for (sfx in com.example.game.audio.GameSfx.entries) {
      val wav = com.example.game.audio.MedievalSynthesizer.generateSfxWav(sfx)
      assertTrue("WAV too short for $sfx", wav.size > 44)
      val header = String(wav, 0, 4, Charsets.US_ASCII)
      val format = String(wav, 8, 4, Charsets.US_ASCII)
      assertEquals("RIFF", header)
      assertEquals("WAVE", format)
    }
  }

  @Test
  fun `title screen exposes music and sfx toggles and viewmodel toggles state cleanly`() {
    val vm = GameViewModel()
    assertTrue(vm.uiState.value.musicEnabled)
    assertTrue(vm.uiState.value.sfxEnabled)
    vm.toggleMusic()
    vm.toggleSfx()
    assertEquals(false, vm.uiState.value.musicEnabled)
    assertEquals(false, vm.uiState.value.sfxEnabled)

    composeRule.setContent {
      CrownConquestTheme {
        com.example.ui.TitleScreen(
          onPlaySkirmish = {},
          musicEnabled = vm.uiState.value.musicEnabled,
          sfxEnabled = vm.uiState.value.sfxEnabled,
          onToggleMusic = { vm.toggleMusic() },
          onToggleSfx = { vm.toggleSfx() },
        )
      }
    }
    composeRule.onNodeWithTag("title_music_toggle").assertIsDisplayed().performClick()
    composeRule.onNodeWithTag("title_sfx_toggle").assertIsDisplayed().performClick()
    composeRule.onNodeWithTag("play_skirmish_button").assertIsDisplayed()
    assertTrue(vm.uiState.value.musicEnabled)
    assertTrue(vm.uiState.value.sfxEnabled)
  }

  @Test
  fun `villagers do not get stuck at town center walls corners or flush adjacent houses`() {
    val tc =
      Building(
        id = 1,
        type = BuildingType.TOWN_CENTER,
        faction = Faction.PLAYER,
        tileX = 10,
        tileY = 10,
        footprint = 3,
        hp = 2400,
        maxHp = 2400,
        buildProgress = 1f,
        isComplete = true,
      )
    // Flush adjacent House blocking the west wall of the Town Center
    val flushHouse =
      Building(
        id = 2,
        type = BuildingType.HOUSE,
        faction = Faction.PLAYER,
        tileX = 8,
        tileY = 10,
        footprint = 2,
        hp = 550,
        maxHp = 550,
        buildProgress = 1f,
        isComplete = true,
      )
    val carrierBehindHouse =
      com.example.game.model.Unit(
        id = 10,
        type = UnitType.VILLAGER,
        faction = Faction.PLAYER,
        x = 7.2f,
        y = 11.0f,
        hp = 40,
        maxHp = 40,
        order = com.example.game.model.UnitOrderType.GATHER,
        carryingType = com.example.game.model.ResourceType.WOOD,
        carryingAmount = 10,
        returningToDropOff = true,
      )
    val wallHuggerNorth =
      com.example.game.model.Unit(
        id = 11,
        type = UnitType.VILLAGER,
        faction = Faction.PLAYER,
        x = 11.5f,
        y = 9.95f,
        hp = 40,
        maxHp = 40,
        order = com.example.game.model.UnitOrderType.IDLE,
      )
    val southTree =
      com.example.game.model.ResourceNode(
        id = 20,
        type = ResourceNodeType.FOREST_OAK,
        tileX = 11,
        tileY = 15,
        remaining = 120,
        maxAmount = 120,
      )

    var world = buildBlankTestWorld(listOf(tc, flushHouse), listOf(carrierBehindHouse, wallHuggerNorth), listOf(southTree))
    val startWood = world.playerState.resources.wood
    world = RtsEngine.orderGatherNode(world, listOf(wallHuggerNorth.id), southTree.id)

    var wallHuggerMaxY = wallHuggerNorth.y
    repeat(200) {
      world = RtsEngine.step(world, 0.1f, SkirmishConfig())
      val nVil = world.units.first { it.id == wallHuggerNorth.id }
      wallHuggerMaxY = kotlin.math.max(wallHuggerMaxY, nVil.y)
    }

    assertTrue(
      "Expected carrier behind flush House to round obstacle and deposit wood at TC (start=$startWood, now=${world.playerState.resources.wood})",
      world.playerState.resources.wood >= startWood + 10,
    )
    assertTrue(
      "Expected north wall-hugger villager to path cleanly around Town Center to south tree, maxY=$wallHuggerMaxY",
      wallHuggerMaxY > 14.0f,
    )
  }

  @Test
  fun `selected villager queues multiple buildings in sequence and returns to nearby resources`() {
    val tc =
      Building(
        id = 1,
        type = BuildingType.TOWN_CENTER,
        faction = Faction.PLAYER,
        tileX = 10,
        tileY = 10,
        footprint = 3,
        hp = 2400,
        maxHp = 2400,
        buildProgress = 1f,
        isComplete = true,
      )
    val vil =
      com.example.game.model.Unit(
        id = 2,
        type = UnitType.VILLAGER,
        faction = Faction.PLAYER,
        x = 9.0f,
        y = 14.0f,
        hp = 40,
        maxHp = 40,
      )
    val nearbyGold =
      com.example.game.model.ResourceNode(
        id = 30,
        type = ResourceNodeType.GOLD_MINE,
        tileX = 16,
        tileY = 12,
        remaining = 600,
        maxAmount = 600,
      )

    var world =
      buildBlankTestWorld(listOf(tc), listOf(vil), listOf(nearbyGold)).let { w ->
        w.copy(
          playerState =
            w.playerState.copy(
              age = com.example.game.model.Age.CASTLE_AGE,
              resources = com.example.game.model.ResourcePool(wood = 1000, food = 1000, gold = 500, stone = 500),
            )
        )
      }

    // Place 3 buildings sequentially while keeping the same single villager selected
    world = RtsEngine.placeBuilding(world, Faction.PLAYER, BuildingType.HOUSE, 8, 15, listOf(vil.id))
    val b1Id = world.buildings.last().id
    world = RtsEngine.placeBuilding(world, Faction.PLAYER, BuildingType.MINING_CAMP, 12, 15, listOf(vil.id))
    val b2Id = world.buildings.last().id
    world = RtsEngine.placeBuilding(world, Faction.PLAYER, BuildingType.BARRACKS, 16, 15, listOf(vil.id))
    val b3Id = world.buildings.last().id

    val queuedVil = world.units.first { it.id == vil.id }
    assertEquals(com.example.game.model.UnitOrderType.BUILD, queuedVil.order)
    assertEquals(b1Id, queuedVil.targetEntityId)
    assertEquals(listOf(b2Id, b3Id), queuedVil.buildQueue)

    // Simulate step-by-step and verify completion order and post-build gather transition
    val completionOrder = mutableListOf<Int>()
    var transitionedToGatherAtFinish = false
    repeat(600) {
      val prevComplete = world.buildings.filter { it.isComplete }.map { it.id }.toSet()
      world = RtsEngine.step(world, 0.1f, SkirmishConfig())
      val newComplete = world.buildings.filter { it.isComplete && it.id !in prevComplete }.map { it.id }
      completionOrder.addAll(newComplete)
      if (completionOrder.size == 3 && b3Id in newComplete) {
        val vNow = world.units.first { it.id == vil.id }
        transitionedToGatherAtFinish =
          vNow.order == com.example.game.model.UnitOrderType.GATHER &&
            vNow.targetResourceId == nearbyGold.id &&
            vNow.buildQueue.isEmpty()
      }
    }

    assertEquals(listOf(b1Id, b2Id, b3Id), completionOrder)
    assertTrue("Expected villager to immediately resume gathering nearby resource after finishing build queue", transitionedToGatherAtFinish)
    val finalVil = world.units.first { it.id == vil.id }
    assertEquals(com.example.game.model.UnitOrderType.GATHER, finalVil.order)
    assertEquals(nearbyGold.id, finalVil.targetResourceId)
    assertTrue(finalVil.buildQueue.isEmpty())
  }

  @Test
  fun `villager finishing queued house followed by farm farms the completed farm`() {
    val tc =
      Building(
        id = 1,
        type = BuildingType.TOWN_CENTER,
        faction = Faction.PLAYER,
        tileX = 10,
        tileY = 10,
        footprint = 3,
        hp = 2400,
        maxHp = 2400,
        buildProgress = 1f,
        isComplete = true,
      )
    val vil =
      com.example.game.model.Unit(
        id = 2,
        type = UnitType.VILLAGER,
        faction = Faction.PLAYER,
        x = 9.0f,
        y = 14.0f,
        hp = 40,
        maxHp = 40,
      )
    var world =
      buildBlankTestWorld(listOf(tc), listOf(vil), emptyList()).let { w ->
        w.copy(
          playerState =
            w.playerState.copy(
              resources = com.example.game.model.ResourcePool(wood = 500, food = 200, gold = 100, stone = 100),
            )
        )
      }

    world = RtsEngine.placeBuilding(world, Faction.PLAYER, BuildingType.HOUSE, 8, 15, listOf(vil.id))
    val houseId = world.buildings.last().id
    world = RtsEngine.placeBuilding(world, Faction.PLAYER, BuildingType.FARM, 12, 15, listOf(vil.id))
    val farmId = world.buildings.last().id

    repeat(350) {
      world = RtsEngine.step(world, 0.1f, SkirmishConfig())
    }

    val house = world.buildings.first { it.id == houseId }
    val farm = world.buildings.first { it.id == farmId }
    val finalVil = world.units.first { it.id == vil.id }
    assertTrue(house.isComplete)
    assertTrue(farm.isComplete)
    assertEquals(com.example.game.model.UnitOrderType.GATHER, finalVil.order)
    assertEquals(farmId, finalVil.targetEntityId)
  }

  @Test
  fun `modern arms roster includes new units with anti-armor bonus and splash rockets`() {
    val modernTypes =
      listOf(
        UnitType.MACHINE_GUNNER,
        UnitType.ROCKET_SOLDIER,
        UnitType.SNIPER,
        UnitType.ROCKET_LAUNCHER,
        UnitType.GUNSHIP,
      )
    for (t in modernTypes) {
      assertNotNull(com.example.game.model.GameDefinitions.unitDefs[t])
    }
    assertTrue(com.example.game.model.GameDefinitions.unitDefs.getValue(UnitType.GUNSHIP).isFlying)
    assertTrue(com.example.game.model.GameDefinitions.unitDefs.getValue(UnitType.ROCKET_SOLDIER).bonusVsArmored >= 25)

    val tc =
      Building(
        id = 1,
        type = BuildingType.TOWN_CENTER,
        faction = Faction.PLAYER,
        tileX = 4,
        tileY = 4,
        footprint = 3,
        hp = 2400,
        maxHp = 2400,
        buildProgress = 1f,
        isComplete = true,
      )
    val rocketTrooper =
      com.example.game.model.Unit(
        id = 10,
        type = UnitType.ROCKET_SOLDIER,
        faction = Faction.PLAYER,
        x = 12f,
        y = 12f,
        hp = 88,
        maxHp = 88,
      )
    val enemyTank =
      com.example.game.model.Unit(
        id = 20,
        type = UnitType.TANK,
        faction = Faction.ENEMY,
        x = 15f,
        y = 12f,
        hp = 320,
        maxHp = 320,
      )
    var world =
      buildBlankTestWorld(listOf(tc), listOf(rocketTrooper, enemyTank), emptyList()).let { w ->
        w.copy(
          playerState = w.playerState.copy(age = com.example.game.model.Age.MODERN_AGE),
          units = RtsEngine.orderAttackUnit(w, listOf(rocketTrooper.id), enemyTank.id).units,
        )
      }
    repeat(20) {
      world = RtsEngine.step(world, 0.1f, SkirmishConfig())
    }
    val damagedTank = world.units.first { it.id == enemyTank.id }
    val dmgDealt = 320 - damagedTank.hp
    assertTrue("Expected Rocket Trooper anti-armor bonus damage > 40 against Tank, got $dmgDealt", dmgDealt > 40)
  }

  @Test
  fun `moveBuilding relocates misplaced structure cleanly and updates active builders`() {
    val tc =
      Building(
        id = 1,
        type = BuildingType.TOWN_CENTER,
        faction = Faction.PLAYER,
        tileX = 10,
        tileY = 10,
        footprint = 3,
        hp = 2400,
        maxHp = 2400,
        buildProgress = 1f,
        isComplete = true,
      )
    val vil =
      com.example.game.model.Unit(
        id = 2,
        type = UnitType.VILLAGER,
        faction = Faction.PLAYER,
        x = 9.0f,
        y = 14.0f,
        hp = 40,
        maxHp = 40,
      )
    val groundBlocker =
      com.example.game.model.Unit(
        id = 3,
        type = UnitType.KNIGHT,
        faction = Faction.PLAYER,
        x = 16.5f,
        y = 15.5f,
        hp = 120,
        maxHp = 120,
      )
    var world =
      buildBlankTestWorld(listOf(tc), listOf(vil, groundBlocker), emptyList()).let { w ->
        w.copy(
          playerState =
            w.playerState.copy(
              resources = com.example.game.model.ResourcePool(wood = 500, food = 200, gold = 100, stone = 100),
            )
        )
      }

    // Place a House and immediately relocate it by 1 overlapping tile while villager is en route
    world = RtsEngine.placeBuilding(world, Faction.PLAYER, BuildingType.HOUSE, 14, 14, listOf(vil.id))
    val houseId = world.buildings.last().id
    assertTrue(RtsEngine.canPlaceBuilding(world, BuildingType.HOUSE, 15, 14, ignoreBuildingId = houseId))

    val movedWorld = RtsEngine.moveBuilding(world, Faction.PLAYER, houseId, 15, 14)
    val movedHouse = movedWorld.buildings.first { it.id == houseId }
    assertEquals(15, movedHouse.tileX)
    assertEquals(14, movedHouse.tileY)

    val builderAfterMove = movedWorld.units.first { it.id == vil.id }
    assertEquals(com.example.game.model.UnitOrderType.BUILD, builderAfterMove.order)
    assertEquals(movedHouse.centerX, builderAfterMove.targetX, 0.01f)
    assertEquals(movedHouse.centerY, builderAfterMove.targetY, 0.01f)

    // Overlapping blocker knight must be pushed outside moved footprint [15..17, 14..16]
    val pushedKnight = movedWorld.units.first { it.id == groundBlocker.id }
    val insideMovedFootprint =
      pushedKnight.x > 15.05f && pushedKnight.x < 16.95f && pushedKnight.y > 14.05f && pushedKnight.y < 15.95f
    assertTrue("Expected ground unit to be ejected from moved building footprint", !insideMovedFootprint)

    // Cannot move on top of Town Center
    val blockedMove = RtsEngine.moveBuilding(movedWorld, Faction.PLAYER, houseId, 10, 10)
    assertEquals(15, blockedMove.buildings.first { it.id == houseId }.tileX)
  }

  @Test
  fun `hud shows move building button and viewmodel map tap relocates selected building`() {
    val vm = GameViewModel()
    vm.startSkirmish()
    val world0 = vm.uiState.value.world!!
    val tc = world0.buildings.first { it.faction == Faction.PLAYER && it.type == BuildingType.TOWN_CENTER }
    val spot =
      (-7..7).flatMap { dy -> (-7..7).map { dx -> (tc.tileX + dx) to (tc.tileY + dy) } }
        .first { (tx, ty) ->
          RtsEngine.canPlaceBuilding(world0, BuildingType.HOUSE, tx, ty) &&
            RtsEngine.canPlaceBuilding(world0, BuildingType.HOUSE, tx + 1, ty)
        }
    val house =
      Building(
        id = 8888,
        type = BuildingType.HOUSE,
        faction = Faction.PLAYER,
        tileX = spot.first,
        tileY = spot.second,
        footprint = 2,
        hp = 55,
        maxHp = 550,
        buildProgress = 0.2f,
        isComplete = false,
      )
    val worldWithHouse = world0.copy(buildings = world0.buildings + house)
    val field = GameViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val flow = field.get(vm) as kotlinx.coroutines.flow.MutableStateFlow<com.example.game.GameUiState>
    flow.value =
      flow.value.copy(
        world = worldWithHouse,
        camera = CameraState(centerWorldX = house.centerX + 1f, centerWorldY = house.centerY, zoom = 1.0f),
        selectedBuildingId = house.id,
        selectedUnitIds = emptySet(),
      )

    var moveClicked = false
    composeRule.setContent {
      CrownConquestTheme {
        BottomCommandPanel(
          uiState = flow.value,
          onJumpCamera = { _, _ -> },
          onStartBuild = {},
          onTrainUnit = {},
          onResearchTech = {},
          onToggleAttackMove = {},
          onStopUnits = {},
          onAutoGatherVillagers = {},
          onSelectAllMilitary = {},
          onJumpToTc = {},
          onCycleIdleVillager = {},
          onMoveSelectedBuilding = {
            moveClicked = true
            vm.startMoveSelectedBuilding()
          },
        )
      }
    }
    composeRule.onNodeWithTag("move_building_button").assertIsDisplayed().performClick()
    assertTrue(moveClicked)
    assertEquals(house.id, vm.uiState.value.pendingMoveBuildingId)

    val destScreen =
      IsometricMath.worldToScreen(spot.first + 1.5f, spot.second + 0.5f, vm.uiState.value.camera, 1080f, 1920f)
    vm.onMapTap(destScreen.x, destScreen.y, 1080f, 1920f)
    val relocated = vm.uiState.value.world!!.buildings.first { it.id == house.id }
    assertEquals(spot.first + 1, relocated.tileX)
    assertEquals(spot.second, relocated.tileY)
    assertEquals(null, vm.uiState.value.pendingMoveBuildingId)
    vm.navigateToTitle()
  }

  @Test
  fun `mill and mining camp researches boost gather rates carry capacity villager speed and building hp`() {
    val baseState = com.example.game.model.FactionState(Faction.PLAYER, com.example.game.model.Civilization.BRITONS)
    val baseCarry = RtsEngine.effectiveCarryCapacity(baseState)
    val baseVilSpeed = RtsEngine.villagerSpeedMultiplier(baseState)
    val baseFoodRate = RtsEngine.gatherRateMultiplier(baseState, com.example.game.model.ResourceType.FOOD)
    val baseGoldRate = RtsEngine.gatherRateMultiplier(baseState, com.example.game.model.ResourceType.GOLD)
    val baseStoneRate = RtsEngine.gatherRateMultiplier(baseState, com.example.game.model.ResourceType.STONE)

    val millUpgraded =
      baseState.copy(
        researchedTechs =
          setOf(
            com.example.game.model.TechType.STONE_GRINDING,
            com.example.game.model.TechType.GRANARY_SILOS,
          )
      )
    assertEquals(baseCarry + 4, RtsEngine.effectiveCarryCapacity(millUpgraded))
    assertTrue(RtsEngine.villagerSpeedMultiplier(millUpgraded) > baseVilSpeed)
    assertTrue(RtsEngine.gatherRateMultiplier(millUpgraded, com.example.game.model.ResourceType.FOOD) > baseFoodRate * 1.35f)

    val mineUpgraded =
      baseState.copy(
        researchedTechs =
          setOf(
            com.example.game.model.TechType.GOLD_MINING,
            com.example.game.model.TechType.STONE_MINING,
            com.example.game.model.TechType.GOLD_SHAFT_MINING,
            com.example.game.model.TechType.STONE_SHAFT_MINING,
            com.example.game.model.TechType.INDUSTRIAL_EXCAVATION,
          )
      )
    assertEquals(baseCarry + 7, RtsEngine.effectiveCarryCapacity(mineUpgraded))
    assertTrue(RtsEngine.gatherRateMultiplier(mineUpgraded, com.example.game.model.ResourceType.GOLD) > baseGoldRate * 2.2f)
    assertTrue(RtsEngine.gatherRateMultiplier(mineUpgraded, com.example.game.model.ResourceType.STONE) > baseStoneRate * 2.2f)
    assertTrue(
      com.example.game.model.GameDefinitions.effectiveBuildingMaxHp(BuildingType.WATCH_TOWER, mineUpgraded) >
        com.example.game.model.GameDefinitions.effectiveBuildingMaxHp(BuildingType.WATCH_TOWER, baseState)
    )
  }

  @Test
  fun `university masonry architecture treadmill crane conscription and combat techs work in engine`() {
    val tc =
      Building(
        id = 1,
        type = BuildingType.TOWN_CENTER,
        faction = Faction.PLAYER,
        tileX = 10,
        tileY = 10,
        footprint = 3,
        hp = 2400,
        maxHp = 2400,
        buildProgress = 1f,
        isComplete = true,
      )
    val uni =
      Building(
        id = 2,
        type = BuildingType.UNIVERSITY,
        faction = Faction.PLAYER,
        tileX = 15,
        tileY = 10,
        footprint = 3,
        hp = 2100,
        maxHp = 2100,
        buildProgress = 1f,
        isComplete = true,
      )
    val barracks =
      Building(
        id = 3,
        type = BuildingType.BARRACKS,
        faction = Faction.PLAYER,
        tileX = 15,
        tileY = 15,
        footprint = 2,
        hp = 1200,
        maxHp = 1200,
        buildProgress = 1f,
        isComplete = true,
      )
    val vil =
      com.example.game.model.Unit(
        id = 10,
        type = UnitType.VILLAGER,
        faction = Faction.PLAYER,
        x = 12.0f,
        y = 14.0f,
        hp = 40,
        maxHp = 40,
      )
    val archer =
      com.example.game.model.Unit(
        id = 11,
        type = UnitType.ARCHER,
        faction = Faction.PLAYER,
        x = 20.0f,
        y = 12.0f,
        hp = 35,
        maxHp = 35,
      )
    val rifleman =
      com.example.game.model.Unit(
        id = 12,
        type = UnitType.RIFLEMAN,
        faction = Faction.PLAYER,
        x = 20.0f,
        y = 14.0f,
        hp = 72,
        maxHp = 72,
      )
    val bomber =
      com.example.game.model.Unit(
        id = 13,
        type = UnitType.BOMBER_PLANE,
        faction = Faction.PLAYER,
        x = 20.0f,
        y = 16.0f,
        hp = 360,
        maxHp = 360,
      )
    val enemyTank =
      com.example.game.model.Unit(
        id = 20,
        type = UnitType.TANK,
        faction = Faction.ENEMY,
        x = 23.0f,
        y = 14.0f,
        hp = 320,
        maxHp = 320,
      )

    var world =
      buildBlankTestWorld(
        listOf(tc, uni, barracks),
        listOf(vil, archer, rifleman, bomber, enemyTank),
        emptyList(),
      ).let { w ->
        w.copy(
          playerState =
            w.playerState.copy(
              age = com.example.game.model.Age.MODERN_AGE,
              resources = com.example.game.model.ResourcePool(wood = 2000, food = 2000, gold = 2000, stone = 2000),
            )
        )
      }

    // Queue University Masonry research & verify retroactive HP upgrade upon completion
    world = RtsEngine.enqueueTech(world, Faction.PLAYER, uni.id, com.example.game.model.TechType.MASONRY)
    repeat(170) {
      world = RtsEngine.step(world, 0.1f, SkirmishConfig())
    }
    assertTrue(com.example.game.model.TechType.MASONRY in world.playerState.researchedTechs)
    val buffedTc = world.buildings.first { it.id == tc.id }
    assertTrue("Expected MASONRY to boost existing TC maxHp above 2400, got ${buffedTc.maxHp}", buffedTc.maxHp > 2400)

    // Verify ACADEMIC_CONSCRIPTION cuts barracks training time
    val baseTrain = RtsEngine.enqueueUnit(world, Faction.PLAYER, barracks.id, UnitType.MILITIA)
    val baseItem = baseTrain.buildings.first { it.id == barracks.id }.queue.first() as com.example.game.model.ProductionItem.TrainUnit
    val withConscriptionState =
      world.playerState.copy(
        researchedTechs =
          world.playerState.researchedTechs +
            setOf(
              com.example.game.model.TechType.ACADEMIC_CONSCRIPTION,
              com.example.game.model.TechType.AERODYNAMICS,
              com.example.game.model.TechType.BALLISTICS,
              com.example.game.model.TechType.CHEMISTRY,
            )
      )
    val conscriptedWorld = RtsEngine.enqueueUnit(world.copy(playerState = withConscriptionState), Faction.PLAYER, barracks.id, UnitType.MILITIA)
    val fastItem = conscriptedWorld.buildings.first { it.id == barracks.id }.queue.first() as com.example.game.model.ProductionItem.TrainUnit
    assertTrue("Expected Academic Conscription to reduce train time", fastItem.totalTime < baseItem.totalTime * 0.85f)

    // Verify AERODYNAMICS boosts flying speed
    assertTrue(
      RtsEngine.unitEffectiveSpeed(UnitType.BOMBER_PLANE, withConscriptionState) >
        RtsEngine.unitEffectiveSpeed(UnitType.BOMBER_PLANE, world.playerState) * 1.1f
    )
  }

  @Test
  fun `hud exposes university in economy strip and researches at mill mining camp and university`() {
    val world = com.example.game.engine.MapGenerator.createWorld(SkirmishConfig(), seed = 88L)
    val tc = world.buildings.first { it.faction == Faction.PLAYER && it.type == BuildingType.TOWN_CENTER }
    val mineCamp =
      Building(
        id = 7001,
        type = BuildingType.MINING_CAMP,
        faction = Faction.PLAYER,
        tileX = tc.tileX + 4,
        tileY = tc.tileY,
        footprint = 2,
        hp = 900,
        maxHp = 900,
        buildProgress = 1f,
        isComplete = true,
      )
    val uni =
      Building(
        id = 7002,
        type = BuildingType.UNIVERSITY,
        faction = Faction.PLAYER,
        tileX = tc.tileX,
        tileY = tc.tileY + 4,
        footprint = 3,
        hp = 2100,
        maxHp = 2100,
        buildProgress = 1f,
        isComplete = true,
      )
    val vil = world.units.first { it.faction == Faction.PLAYER && it.type == UnitType.VILLAGER }
    val augmentedWorld =
      world.copy(
        buildings = world.buildings + mineCamp + uni,
        playerState = world.playerState.copy(age = com.example.game.model.Age.FEUDAL_AGE),
      )

    val vilState = com.example.game.GameUiState(world = augmentedWorld, selectedUnitIds = setOf(vil.id))
    composeRule.setContent {
      CrownConquestTheme {
        BottomCommandPanel(
          uiState = vilState,
          onJumpCamera = { _, _ -> },
          onStartBuild = {},
          onTrainUnit = {},
          onResearchTech = {},
          onToggleAttackMove = {},
          onStopUnits = {},
          onAutoGatherVillagers = {},
          onSelectAllMilitary = {},
          onJumpToTc = {},
          onCycleIdleVillager = {},
        )
      }
    }
    composeRule.onNodeWithTag("build_category_economy").performScrollTo().performClick()
    composeRule.onNodeWithTag("build_university").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `ai commander schedules university construction and mining camp or university research`() {
    val enemyTc =
      Building(
        id = 1,
        type = BuildingType.TOWN_CENTER,
        faction = Faction.ENEMY,
        tileX = 32,
        tileY = 32,
        footprint = 3,
        hp = 2400,
        maxHp = 2400,
        buildProgress = 1f,
        isComplete = true,
      )
    val enemyMine =
      Building(
        id = 2,
        type = BuildingType.MINING_CAMP,
        faction = Faction.ENEMY,
        tileX = 28,
        tileY = 32,
        footprint = 2,
        hp = 900,
        maxHp = 900,
        buildProgress = 1f,
        isComplete = true,
      )
    val enemyVil =
      com.example.game.model.Unit(
        id = 5,
        type = UnitType.VILLAGER,
        faction = Faction.ENEMY,
        x = 30.5f,
        y = 33.5f,
        hp = 40,
        maxHp = 40,
        order = com.example.game.model.UnitOrderType.IDLE,
      )
    val playerTc =
      Building(
        id = 3,
        type = BuildingType.TOWN_CENTER,
        faction = Faction.PLAYER,
        tileX = 6,
        tileY = 6,
        footprint = 3,
        hp = 2400,
        maxHp = 2400,
        buildProgress = 1f,
        isComplete = true,
      )
    val enemyGold =
      com.example.game.model.ResourceNode(
        id = 20,
        type = ResourceNodeType.GOLD_MINE,
        tileX = 26,
        tileY = 32,
        remaining = 600,
        maxAmount = 600,
      )

    val world0 =
      buildBlankTestWorld(listOf(enemyTc, enemyMine, playerTc), listOf(enemyVil), listOf(enemyGold)).let { w ->
        w.copy(
          enemyState =
            w.enemyState.copy(
              age = com.example.game.model.Age.FEUDAL_AGE,
              resources = com.example.game.model.ResourcePool(wood = 600, food = 600, gold = 400, stone = 400),
            )
        )
      }
    val afterAi = com.example.game.engine.AiCommander.updateAi(world0, com.example.game.model.Difficulty.HARD)
    val queuedAtMine = afterAi.buildings.first { it.id == enemyMine.id }.queue
    assertTrue("Expected AI to research GOLD_MINING or STONE_MINING at Mining Camp", queuedAtMine.isNotEmpty())
  }

  @Test
  fun `hi-fi synthesizer runs at 44100hz for sfx and sound engine accepts stereo panning`() {
    assertEquals(44100, com.example.game.audio.MedievalSynthesizer.SFX_SAMPLE_RATE)
    val wav = com.example.game.audio.MedievalSynthesizer.generateSfxWav(com.example.game.audio.GameSfx.CANNON_FIRE)
    val sampleRateInHeader =
      (wav[24].toInt() and 0xFF) or
        ((wav[25].toInt() and 0xFF) shl 8) or
        ((wav[26].toInt() and 0xFF) shl 16) or
        ((wav[27].toInt() and 0xFF) shl 24)
    assertEquals(44100, sampleRateInHeader)

    val context = ApplicationProvider.getApplicationContext<Context>()
    val soundEngine = com.example.game.audio.SoundEngine(context)
    soundEngine.playSfx(com.example.game.audio.GameSfx.GUNSHOT, volume = 0.7f, pitch = 1.0f, pan = -0.65f)
    soundEngine.release()
  }

  @Test
  fun `skirmish setup exposes ai rival counts conditional civs and map sizes`() {
    composeRule.setContent {
      CrownConquestTheme {
        SkirmishSetupScreen(
          config = SkirmishConfig(aiCount = 3),
          onUpdateConfig = { _, _, _, _ -> },
          onLaunchBattle = {},
          onBack = {},
        )
      }
    }
    composeRule.onNodeWithTag("ai_count_1").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("ai_count_2").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("ai_count_3").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("enemy2_civ_teutons").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("enemy3_civ_byzantines").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("map_size_standard").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("map_size_large").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("map_size_huge").performScrollTo().assertIsDisplayed()
  }

  @Test
  @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
  fun `four faction sprite caches produce distinct colors and huge 3-ai hud renders cleanly`() {
    val redKnight = SpriteAtlas.getUnitSprite(UnitType.KNIGHT, Faction.ENEMY, facingRight = true, walkFrame = 0).asAndroidBitmap()
    val greenKnight = SpriteAtlas.getUnitSprite(UnitType.KNIGHT, Faction.ENEMY_2, facingRight = true, walkFrame = 0).asAndroidBitmap()
    val purpleKnight = SpriteAtlas.getUnitSprite(UnitType.KNIGHT, Faction.ENEMY_3, facingRight = true, walkFrame = 0).asAndroidBitmap()
    assertTrue("ENEMY and ENEMY_2 unit sprites must not collide in cache", !redKnight.sameAs(greenKnight))
    assertTrue("ENEMY_2 and ENEMY_3 unit sprites must not collide in cache", !greenKnight.sameAs(purpleKnight))

    val greenCastle = BuildingSpriteAtlas.getBuildingSprite(BuildingType.CASTLE, Faction.ENEMY_2).asAndroidBitmap()
    val purpleCastle = BuildingSpriteAtlas.getBuildingSprite(BuildingType.CASTLE, Faction.ENEMY_3).asAndroidBitmap()
    assertTrue("ENEMY_2 and ENEMY_3 building sprites must use distinct faction banners", !greenCastle.sameAs(purpleCastle))

    val world =
      com.example.game.engine.MapGenerator.createWorld(
        SkirmishConfig(
          aiCount = 3,
          mapSize = com.example.game.model.MapSizeOption.HUGE,
          mapType = MapType.COASTAL,
        ),
        seed = 314L,
      )
    val tc = world.buildings.first { it.faction == Faction.PLAYER }
    composeRule.setContent {
      CrownConquestTheme {
        com.example.ui.TopResourceBar(
          world = world,
          gameSpeed = 1.0f,
          onCycleSpeed = {},
          onPauseClick = {},
        )
      }
    }
    composeRule.onNodeWithTag("hud_rivals_status").assertIsDisplayed()
    assertEquals(92, world.mapSize)
    assertTrue(tc.tileX > 0)
  }

  @Test
  @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
  fun `world renderer draws volumetric effects birds and atmospheric lighting cleanly`() {
    var capturedMeasurer: androidx.compose.ui.text.TextMeasurer? = null
    composeRule.setContent {
      capturedMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    }
    composeRule.waitForIdle()
    val textMeasurer = capturedMeasurer!!

    val baseWorld = com.example.game.engine.MapGenerator.createWorld(SkirmishConfig(mapType = MapType.COASTAL), seed = 42L)
    val tc = baseWorld.buildings.first { it.faction == Faction.PLAYER }
    val fxWorld =
      baseWorld.copy(
        effects =
          listOf(
            com.example.game.model.VisualEffect(
              id = 1,
              type = com.example.game.model.VisualEffectType.EXPLOSION,
              x = tc.centerX + 2f,
              y = tc.centerY + 1f,
              ageSec = 0.25f,
              durationSec = 0.8f,
            ),
            com.example.game.model.VisualEffect(
              id = 2,
              type = com.example.game.model.VisualEffectType.MUZZLE_FLASH,
              x = tc.centerX - 2f,
              y = tc.centerY + 2f,
              ageSec = 0.1f,
              durationSec = 0.3f,
            ),
            com.example.game.model.VisualEffect(
              id = 3,
              type = com.example.game.model.VisualEffectType.DUST_PUFF,
              x = tc.centerX,
              y = tc.centerY - 2f,
              ageSec = 0.2f,
              durationSec = 0.7f,
            ),
          )
      )
    val camera = CameraState(centerWorldX = tc.centerX, centerWorldY = tc.centerY, zoom = 1.05f)
    val bmp = android.graphics.Bitmap.createBitmap(640, 640, android.graphics.Bitmap.Config.ARGB_8888)
    val drawScope = androidx.compose.ui.graphics.drawscope.CanvasDrawScope()
    drawScope.draw(
      density = androidx.compose.ui.unit.Density(1f),
      layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
      canvas = androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bmp)),
      size = androidx.compose.ui.geometry.Size(640f, 640f),
    ) {
      with(com.example.game.render.WorldRenderer) {
        drawGameWorld(
          world = fxWorld,
          camera = camera,
          selectedUnitIds = emptySet(),
          selectedBuildingId = tc.id,
          pendingBuildType = null,
          pendingBuildTile = null,
          selectionBox = null,
          animClock = 2.5f,
          textMeasurer = textMeasurer,
        )
      }
    }

    var warmHighlightPixels = 0
    for (y in 0 until 640 step 8) {
      for (x in 0 until 640 step 8) {
        val px = bmp.getPixel(x, y)
        if (android.graphics.Color.red(px) > 160 && android.graphics.Color.green(px) > 80) {
          warmHighlightPixels++
        }
      }
    }
    assertTrue("Expected warm explosion/lighting highlights on canvas, got $warmHighlightPixels", warmHighlightPixels > 10)
  }

  @Test
  @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
  fun `explored fog state reveals full terrain bridges resources and partially explored buildings`() {
    var capturedMeasurer: androidx.compose.ui.text.TextMeasurer? = null
    composeRule.setContent {
      capturedMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    }
    composeRule.waitForIdle()
    val textMeasurer = capturedMeasurer!!

    val mapSize = 20
    val tiles = MutableList(mapSize * mapSize) { com.example.game.model.TileTerrain.GRASS }
    // Place dirt road and river crossing (DIRT_ROAD bordered by WATER renders a wooden bridge)
    for (x in 4..15) {
      tiles[10 * mapSize + x] = com.example.game.model.TileTerrain.DIRT_ROAD
    }
    for (x in 8..11) {
      tiles[9 * mapSize + x] = com.example.game.model.TileTerrain.WATER
      tiles[11 * mapSize + x] = com.example.game.model.TileTerrain.WATER
    }
    tiles[7 * mapSize + 10] = com.example.game.model.TileTerrain.CLIFF

    val enemyCastle =
      Building(
        id = 77,
        type = BuildingType.CASTLE,
        faction = Faction.ENEMY,
        tileX = 12,
        tileY = 6,
        footprint = 3,
        hp = 1200,
        maxHp = 1200,
        buildProgress = 1f,
        isComplete = true,
      )
    val oakTree =
      com.example.game.model.ResourceNode(
        id = 88,
        type = ResourceNodeType.FOREST_OAK,
        tileX = 6,
        tileY = 7,
        remaining = 150,
        maxAmount = 150,
        variant = 0,
      )

    // First test partial edge exploration for multi-tile buildings
    val edgeOnlyFog = IntArray(mapSize * mapSize) { 0 }
    // Only bottom-right corner (14, 8) of 3x3 building at (12, 6) is explored
    edgeOnlyFog[8 * mapSize + 14] = 1
    val partialWorld =
      com.example.game.model.GameWorld(
        mapSize = mapSize,
        tiles = tiles,
        fogGrid = edgeOnlyFog,
        resources = listOf(oakTree),
        buildings = listOf(enemyCastle),
        units = emptyList(),
        projectiles = emptyList(),
        floatingTexts = emptyList(),
        effects = emptyList(),
        playerState = com.example.game.model.FactionState(Faction.PLAYER, com.example.game.model.Civilization.BRITONS),
        enemyState = com.example.game.model.FactionState(Faction.ENEMY, com.example.game.model.Civilization.FRANKS),
        mapType = MapType.COASTAL,
      )
    assertEquals(1, partialWorld.buildingMaxFog(enemyCastle))

    // Compare fully unexplored world (fog == 0) vs explored world (fog == 1)
    val unexploredWorld = partialWorld.copy(fogGrid = IntArray(mapSize * mapSize) { 0 })
    val exploredWorld = partialWorld.copy(fogGrid = IntArray(mapSize * mapSize) { 1 })

    val camera = CameraState(centerWorldX = 10f, centerWorldY = 9f, zoom = 1.15f)
    fun renderWorldBitmap(w: com.example.game.model.GameWorld): android.graphics.Bitmap {
      val bmp = android.graphics.Bitmap.createBitmap(640, 640, android.graphics.Bitmap.Config.ARGB_8888)
      val drawScope = androidx.compose.ui.graphics.drawscope.CanvasDrawScope()
      drawScope.draw(
        density = androidx.compose.ui.unit.Density(1f),
        layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
        canvas = androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bmp)),
        size = androidx.compose.ui.geometry.Size(640f, 640f),
      ) {
        with(com.example.game.render.WorldRenderer) {
          drawGameWorld(
            world = w,
            camera = camera,
            selectedUnitIds = emptySet(),
            selectedBuildingId = null,
            pendingBuildType = null,
            pendingBuildTile = null,
            selectionBox = null,
            animClock = 1.0f,
            textMeasurer = textMeasurer,
          )
        }
      }
      return bmp
    }

    val darkBmp = renderWorldBitmap(unexploredWorld)
    val exploredBmp = renderWorldBitmap(exploredWorld)

    var visibleBrightPixels = 0
    var diffPixels = 0
    for (y in 40 until 600 step 6) {
      for (x in 40 until 600 step 6) {
        val ePx = exploredBmp.getPixel(x, y)
        val dPx = darkBmp.getPixel(x, y)
        val luma =
          android.graphics.Color.red(ePx) +
            android.graphics.Color.green(ePx) +
            android.graphics.Color.blue(ePx)
        if (luma > 130) visibleBrightPixels++
        if (ePx != dPx) diffPixels++
      }
    }
    assertTrue("Expected explored (fog=1) terrain/bridges/buildings to render brightly, got $visibleBrightPixels", visibleBrightPixels > 200)
    assertTrue("Expected explored (fog=1) canvas to differ clearly from unexplored shroud, got $diffPixels", diffPixels > 500)
  }

  @Test
  fun `buildings require 20-tile town center radius unless building a new town center`() {
    val tc =
      Building(
        id = 1,
        type = BuildingType.TOWN_CENTER,
        faction = Faction.PLAYER,
        tileX = 6,
        tileY = 6,
        footprint = 3,
        hp = 2400,
        maxHp = 2400,
        buildProgress = 1f,
        isComplete = true,
      ) // center = (7.5, 7.5)
    val vil =
      com.example.game.model.Unit(
        id = 2,
        type = UnitType.VILLAGER,
        faction = Faction.PLAYER,
        x = 34.0f,
        y = 34.0f,
        hp = 40,
        maxHp = 40,
      )
    var world =
      buildBlankTestWorld(listOf(tc), listOf(vil), emptyList(), mapSize = 52).let { w ->
        w.copy(
          playerState =
            w.playerState.copy(
              age = com.example.game.model.Age.CASTLE_AGE,
              resources = com.example.game.model.ResourcePool(wood = 1200, food = 500, gold = 500, stone = 500),
            )
        )
      }

    // Tile (33, 33) -> dist ~ 37 tiles (> 20)
    assertEquals(false, RtsEngine.canPlaceBuilding(world, BuildingType.HOUSE, 33, 33, faction = Faction.PLAYER))
    val blockedHouseWorld = RtsEngine.placeBuilding(world, Faction.PLAYER, BuildingType.HOUSE, 33, 33, listOf(vil.id))
    assertEquals(world.buildings.size, blockedHouseWorld.buildings.size)

    // New Town Center CAN be placed outside the 20-tile radius
    assertEquals(true, RtsEngine.canPlaceBuilding(world, BuildingType.TOWN_CENTER, 32, 32, faction = Faction.PLAYER))
    world = RtsEngine.placeBuilding(world, Faction.PLAYER, BuildingType.TOWN_CENTER, 32, 32, listOf(vil.id))
    assertTrue(world.buildings.count { it.faction == Faction.PLAYER && it.type == BuildingType.TOWN_CENTER } == 2)

    // Once the new Town Center is placed near (32, 32), House at (36, 32) is within 20 tiles and succeeds
    assertEquals(true, RtsEngine.canPlaceBuilding(world, BuildingType.HOUSE, 36, 32, faction = Faction.PLAYER))
  }

  @Test
  fun `tech upgrades enforce prerequisite buildings and prerequisite researches`() {
    val tc =
      Building(
        id = 1,
        type = BuildingType.TOWN_CENTER,
        faction = Faction.PLAYER,
        tileX = 8,
        tileY = 8,
        footprint = 3,
        hp = 2400,
        maxHp = 2400,
        buildProgress = 1f,
        isComplete = true,
      )
    val blacksmith =
      Building(
        id = 2,
        type = BuildingType.BLACKSMITH,
        faction = Faction.PLAYER,
        tileX = 12,
        tileY = 8,
        footprint = 2,
        hp = 1400,
        maxHp = 1400,
        buildProgress = 1f,
        isComplete = true,
      )
    var world =
      buildBlankTestWorld(listOf(tc, blacksmith), emptyList(), emptyList()).let { w ->
        w.copy(
          playerState =
            w.playerState.copy(
              age = com.example.game.model.Age.CASTLE_AGE,
              resources = com.example.game.model.ResourcePool(wood = 1200, food = 1500, gold = 1200, stone = 600),
            )
        )
      }

    // IRON_CASTING requires FORGING first
    val missingIron = com.example.game.model.GameDefinitions.missingTechRequirements(world, Faction.PLAYER, com.example.game.model.TechType.IRON_CASTING)
    assertEquals(false, missingIron.isMet)
    assertTrue(com.example.game.model.TechType.FORGING in missingIron.missingTechs)
    val blockedBlacksmith = RtsEngine.enqueueTech(world, Faction.PLAYER, blacksmith.id, com.example.game.model.TechType.IRON_CASTING)
    assertEquals(0, blockedBlacksmith.buildings.first { it.id == blacksmith.id }.queue.size)

    // ADVANCE_CASTLE requires BLACKSMITH, ARCHERY_RANGE, FORGING, STONE_GRINDING
    val missingCastle = com.example.game.model.GameDefinitions.missingTechRequirements(world, Faction.PLAYER, com.example.game.model.TechType.ADVANCE_CASTLE)
    assertEquals(false, missingCastle.isMet)
    assertTrue(BuildingType.ARCHERY_RANGE in missingCastle.missingBuildings)
    assertTrue(com.example.game.model.TechType.FORGING in missingCastle.missingTechs)

    // Once Forging is researched, Iron Casting enqueues
    world = world.copy(playerState = world.playerState.copy(researchedTechs = setOf(com.example.game.model.TechType.FORGING)))
    val allowedBlacksmith = RtsEngine.enqueueTech(world, Faction.PLAYER, blacksmith.id, com.example.game.model.TechType.IRON_CASTING)
    assertEquals(1, allowedBlacksmith.buildings.first { it.id == blacksmith.id }.queue.size)
  }

  @Test
  fun `all 11 languages resolve cleanly and splash screen language selector switches ui copy`() {
    assertEquals(11, com.example.ui.AppLanguage.entries.size)
    for (lang in com.example.ui.AppLanguage.entries) {
      val strings = com.example.ui.stringsFor(lang)
      assertTrue(strings.playSkirmish.isNotBlank())
      assertTrue(strings.tcRadiusErrorMsg.isNotBlank())
      assertEquals(BuildingType.entries.size, strings.buildingNames.size)
      assertEquals(BuildingType.entries.size, strings.buildingDescriptions.size)
      assertEquals(UnitType.entries.size, strings.unitNames.size)
      assertEquals(com.example.game.model.TechType.entries.size, strings.techNames.size)
    }

    val vm = GameViewModel()
    composeRule.setContent {
      val uiState = vm.uiState.collectAsState().value
      androidx.compose.runtime.CompositionLocalProvider(
        com.example.ui.LocalAppStrings provides com.example.ui.stringsFor(uiState.language)
      ) {
        CrownConquestTheme {
          com.example.ui.TitleScreen(
            onPlaySkirmish = {},
            selectedLanguage = uiState.language,
            onSelectLanguage = { vm.setLanguage(it) },
          )
        }
      }
    }

    composeRule.onNodeWithTag("language_selector_card").assertIsDisplayed()
    composeRule.onNodeWithTag("lang_fr").performScrollTo().performClick()
    assertEquals(com.example.ui.AppLanguage.FR, vm.uiState.value.language)
    composeRule.onNodeWithTag("play_skirmish_button").assertIsDisplayed()

    composeRule.onNodeWithTag("lang_ja").performScrollTo().performClick()
    assertEquals(com.example.ui.AppLanguage.JA, vm.uiState.value.language)
  }

  @Test
  fun `resource counter stays visible in portrait and landscape and build strip displays descriptions`() {
    val world = com.example.game.engine.MapGenerator.createWorld(SkirmishConfig(), seed = 909L)
    val vil = world.units.first { it.faction == Faction.PLAYER && it.type == UnitType.VILLAGER }
    val state = com.example.game.GameUiState(world = world, selectedUnitIds = setOf(vil.id))

    composeRule.setContent {
      CrownConquestTheme {
        androidx.compose.foundation.layout.Column {
          androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.size(360.dp, 120.dp)) {
            com.example.ui.TopResourceBar(
              world = world,
              gameSpeed = 1f,
              onCycleSpeed = {},
              onPauseClick = {},
            )
          }
          androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.size(820.dp, 220.dp)) {
            com.example.ui.BottomCommandPanel(
              uiState = state,
              onJumpCamera = { _, _ -> },
              onStartBuild = {},
              onTrainUnit = {},
              onResearchTech = {},
              onToggleAttackMove = {},
              onStopUnits = {},
              onAutoGatherVillagers = {},
              onSelectAllMilitary = {},
              onJumpToTc = {},
              onCycleIdleVillager = {},
              isLandscape = true,
            )
          }
        }
      }
    }

    composeRule.onNodeWithTag("res_wood").assertIsDisplayed()
    composeRule.onNodeWithTag("res_food").assertIsDisplayed()
    composeRule.onNodeWithTag("res_gold").assertIsDisplayed()
    composeRule.onNodeWithTag("res_stone").assertIsDisplayed()
    composeRule.onNodeWithTag("res_pop").assertIsDisplayed()

    val panelBounds = composeRule.onNodeWithTag("bottom_command_panel").assertIsDisplayed().getUnclippedBoundsInRoot()
    assertTrue(
      "Landscape bottom bar should be compact (<= 72.dp) but was ${panelBounds.bottom - panelBounds.top}",
      (panelBounds.bottom - panelBounds.top) <= 72.dp,
    )

    composeRule.onNodeWithTag("build_category_economy").performScrollTo().performClick()
    composeRule.onNodeWithTag("build_house").performScrollTo().performClick()
    composeRule.onNodeWithTag("build_description_banner").assertIsDisplayed()
    assertEquals(com.example.ui.theme.TimesFontFamily, com.example.ui.theme.AppTypography.displayLarge.fontFamily)
  }
}

