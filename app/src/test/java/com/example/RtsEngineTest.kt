package com.example

import com.example.game.engine.AiCommander
import com.example.game.engine.MapGenerator
import com.example.game.engine.RtsEngine
import com.example.game.model.Age
import com.example.game.model.BuildingType
import com.example.game.model.Difficulty
import com.example.game.model.Faction
import com.example.game.model.MapType
import com.example.game.model.ResourceNodeType
import com.example.game.model.SkirmishConfig
import com.example.game.model.TechType
import com.example.game.model.UnitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RtsEngineTest {

  @Test
  fun mapGeneratorCreatesBalancedBasesAndResources() {
    for (mapType in MapType.entries) {
      val world = MapGenerator.createWorld(SkirmishConfig(mapType = mapType), seed = 42L)
      val playerTc = world.buildings.firstOrNull { it.faction == Faction.PLAYER && it.type == BuildingType.TOWN_CENTER }
      val enemyTc = world.buildings.firstOrNull { it.faction == Faction.ENEMY && it.type == BuildingType.TOWN_CENTER }
      assertNotNull(playerTc)
      assertNotNull(enemyTc)
      assertEquals(4, world.units.count { it.faction == Faction.PLAYER })
      assertEquals(4, world.units.count { it.faction == Faction.ENEMY })
      assertTrue(world.resources.any { it.type == ResourceNodeType.GOLD_MINE })
      assertTrue(world.resources.any { it.type == ResourceNodeType.BERRY_BUSH })
    }
  }

  @Test
  fun villagerGathersAndDepositsFoodAtTownCenter() {
    val cfg = SkirmishConfig()
    var world = MapGenerator.createWorld(cfg, seed = 100L)
    val startFood = world.playerState.resources.food
    val vil = world.units.first { it.faction == Faction.PLAYER && it.type == UnitType.VILLAGER }
    val berry = world.resources.first { it.type == ResourceNodeType.BERRY_BUSH }

    world = RtsEngine.orderGatherNode(world, listOf(vil.id), berry.id)
    repeat(300) {
      world = RtsEngine.step(world, 0.1f, cfg)
    }

    assertTrue(
      "Expected player food to increase from $startFood, was ${world.playerState.resources.food}",
      world.playerState.resources.food > startFood,
    )
  }

  @Test
  fun villagerConstructsHouseAndIncreasesPopulationCap() {
    val cfg = SkirmishConfig()
    var world = MapGenerator.createWorld(cfg, seed = 101L)
    val tc = world.buildings.first { it.faction == Faction.PLAYER && it.type == BuildingType.TOWN_CENTER }
    val vil = world.units.first { it.faction == Faction.PLAYER && it.type == UnitType.VILLAGER }
    val initialCap = RtsEngine.populationCap(world.buildings.filter { it.faction == Faction.PLAYER })
    assertEquals(5, initialCap)

    val spot =
      (-6..6).flatMap { dy -> (-6..6).map { dx -> (tc.tileX + dx) to (tc.tileY + dy) } }
        .first { (tx, ty) -> RtsEngine.canPlaceBuilding(world, BuildingType.HOUSE, tx, ty) }

    world = RtsEngine.placeBuilding(world, Faction.PLAYER, BuildingType.HOUSE, spot.first, spot.second, listOf(vil.id))
    assertTrue("House should be placed as construction site", world.buildings.any { it.type == BuildingType.HOUSE })

    repeat(250) {
      world = RtsEngine.step(world, 0.1f, cfg)
    }

    val newCap = RtsEngine.populationCap(world.buildings.filter { it.faction == Faction.PLAYER })
    assertEquals(10, newCap)
  }

  @Test
  fun townCenterAdvancesToFeudalAgeAndAiOperates() {
    val cfg = SkirmishConfig(difficulty = Difficulty.MODERATE)
    var world = MapGenerator.createWorld(cfg, seed = 102L)
    world = world.copy(playerState = world.playerState.copy(resources = world.playerState.resources.copy(food = 600)))
    val tc = world.buildings.first { it.faction == Faction.PLAYER && it.type == BuildingType.TOWN_CENTER }

    // Blocked without completed Mill and Barracks
    val blockedWorld = RtsEngine.enqueueTech(world, Faction.PLAYER, tc.id, TechType.ADVANCE_FEUDAL)
    assertEquals(0, blockedWorld.buildings.first { it.id == tc.id }.queue.size)

    val mill =
      com.example.game.model.Building(
        id = world.nextEntityId,
        type = BuildingType.MILL,
        faction = Faction.PLAYER,
        tileX = tc.tileX - 3,
        tileY = tc.tileY,
        footprint = 2,
        hp = 750,
        maxHp = 750,
        buildProgress = 1f,
        isComplete = true,
      )
    val barracks =
      com.example.game.model.Building(
        id = world.nextEntityId + 1,
        type = BuildingType.BARRACKS,
        faction = Faction.PLAYER,
        tileX = tc.tileX + 4,
        tileY = tc.tileY,
        footprint = 2,
        hp = 1200,
        maxHp = 1200,
        buildProgress = 1f,
        isComplete = true,
      )
    world =
      world.copy(
        buildings = world.buildings + mill + barracks,
        nextEntityId = world.nextEntityId + 2,
      )

    world = RtsEngine.enqueueTech(world, Faction.PLAYER, tc.id, TechType.ADVANCE_FEUDAL)
    repeat(450) {
      world = RtsEngine.step(world, 0.1f, cfg)
      if (it % 12 == 0) world = AiCommander.updateAi(world, cfg.difficulty)
    }

    assertEquals(Age.FEUDAL_AGE, world.playerState.age)
    assertTrue(world.enemyState.stats.totalFoodGathered + world.enemyState.stats.totalWoodGathered > 0)
  }

  @Test
  fun idleVillagerAutomaticallyCollectsMostNeededResource() {
    val cfg = SkirmishConfig()
    var world = MapGenerator.createWorld(cfg, seed = 205L)
    // Put all player villagers idle with Wood critically depleted while Food/Gold/Stone are high
    world =
      world.copy(
        playerState =
          world.playerState.copy(
            age = Age.FEUDAL_AGE,
            resources = world.playerState.resources.copy(wood = 0, food = 900, gold = 500, stone = 400),
          )
      )
    assertEquals(com.example.game.model.ResourceType.WOOD, RtsEngine.mostNeededResource(world, Faction.PLAYER))

    // Step 2.0 seconds so idle villagers automatically start gathering the most needed resource
    repeat(20) {
      world = RtsEngine.step(world, 0.1f, cfg)
    }

    val playerVils = world.units.filter { it.faction == Faction.PLAYER && it.type == UnitType.VILLAGER }
    val nodeById = world.resources.associateBy { it.id }
    val gatheringWood =
      playerVils.count { u ->
        u.order == com.example.game.model.UnitOrderType.GATHER &&
          nodeById[u.targetResourceId]?.type?.resourceType == com.example.game.model.ResourceType.WOOD
      }
    assertTrue("Expected idle villagers to auto-gather Wood, got $gatheringWood", gatheringWood >= 1)

    // Verify Food shortage takes priority when Food < 60
    val foodCrisisWorld =
      world.copy(
        playerState = world.playerState.copy(resources = world.playerState.resources.copy(wood = 600, food = 15, gold = 500, stone = 500))
      )
    assertEquals(com.example.game.model.ResourceType.FOOD, RtsEngine.mostNeededResource(foodCrisisWorld, Faction.PLAYER))
  }

  @Test
  fun villagerNavigatesAroundTownCenterAndDepositsSmoothly() {
    val cfg = SkirmishConfig()
    var world = MapGenerator.createWorld(cfg, seed = 310L)
    val tc = world.buildings.first { it.faction == Faction.PLAYER && it.type == BuildingType.TOWN_CENTER }
    val startWood = world.playerState.resources.wood

    val treeTileX = tc.tileX + 1
    val treeTileY = (tc.tileY + tc.footprint + 3).coerceAtMost(world.mapSize - 3)
    val customTree =
      com.example.game.model.ResourceNode(
        id = world.nextEntityId,
        type = ResourceNodeType.FOREST_OAK,
        tileX = treeTileX,
        tileY = treeTileY,
        remaining = 100,
        maxAmount = 100,
      )
    val house =
      com.example.game.model.Building(
        id = world.nextEntityId + 1,
        type = BuildingType.HOUSE,
        faction = Faction.PLAYER,
        tileX = tc.tileX - 2,
        tileY = tc.tileY + 1,
        footprint = 2,
        hp = 550,
        maxHp = 550,
        buildProgress = 1f,
        isComplete = true,
      )
    val vil = world.units.first { it.faction == Faction.PLAYER && it.type == UnitType.VILLAGER }
    val enemyTc = world.buildings.first { it.faction == Faction.ENEMY }
    val repositionedVil =
      vil.copy(
        x = tc.centerX,
        y = (tc.tileY - 0.55f).coerceAtLeast(1.5f),
        order = com.example.game.model.UnitOrderType.IDLE,
        carryingAmount = 0,
        returningToDropOff = false,
      )
    val cleanTiles = world.tiles.toMutableList()
    for (dy in -4..7) {
      for (dx in -4..6) {
        val tx = (tc.tileX + dx).coerceIn(0, world.mapSize - 1)
        val ty = (tc.tileY + dy).coerceIn(0, world.mapSize - 1)
        cleanTiles[ty * world.mapSize + tx] = com.example.game.model.TileTerrain.GRASS
      }
    }
    world =
      world.copy(
        tiles = cleanTiles,
        resources = listOf(customTree),
        buildings = listOf(tc, house, enemyTc),
        units = listOf(repositionedVil),
        nextEntityId = world.nextEntityId + 2,
      )

    world = RtsEngine.orderGatherNode(world, listOf(vil.id), customTree.id)
    var maxYReached = repositionedVil.y
    repeat(320) {
      world = RtsEngine.step(world, 0.1f, cfg)
      val cur = world.units.first { it.id == vil.id }
      maxYReached = kotlin.math.max(maxYReached, cur.y)
    }

    assertTrue(
      "Villager should navigate past the Town Center (tc.bottom=${tc.tileY + tc.footprint}) toward treeY=${customTree.y}, maxYReached=$maxYReached",
      maxYReached > tc.tileY + tc.footprint + 1.0f,
    )
    assertTrue(
      "Expected villager to gather wood and drop it off cleanly at Town Center (start=$startWood, now=${world.playerState.resources.wood})",
      world.playerState.resources.wood > startWood,
    )
  }

  @Test
  fun villagerCarryingResourcesNavigatesAroundBarracksToDropOff() {
    val cfg = SkirmishConfig()
    var world = MapGenerator.createWorld(cfg, seed = 311L)
    val tc = world.buildings.first { it.faction == Faction.PLAYER && it.type == BuildingType.TOWN_CENTER }
    val enemyTc = world.buildings.first { it.faction == Faction.ENEMY }
    val startGold = world.playerState.resources.gold

    // Place a Barracks between the resource and the Town Center on the south side
    val barracks =
      com.example.game.model.Building(
        id = world.nextEntityId,
        type = BuildingType.BARRACKS,
        faction = Faction.PLAYER,
        tileX = tc.tileX,
        tileY = tc.tileY + tc.footprint + 1,
        footprint = 2,
        hp = 1200,
        maxHp = 1200,
        buildProgress = 1f,
        isComplete = true,
      )
    val vil = world.units.first { it.faction == Faction.PLAYER && it.type == UnitType.VILLAGER }
    val carrier =
      vil.copy(
        x = barracks.centerX,
        y = barracks.tileY + barracks.footprint + 0.6f,
        order = com.example.game.model.UnitOrderType.GATHER,
        carryingType = com.example.game.model.ResourceType.GOLD,
        carryingAmount = 10,
        returningToDropOff = true,
      )
    val cleanTiles = world.tiles.toMutableList()
    for (dy in -2..9) {
      for (dx in -4..6) {
        val tx = (tc.tileX + dx).coerceIn(0, world.mapSize - 1)
        val ty = (tc.tileY + dy).coerceIn(0, world.mapSize - 1)
        cleanTiles[ty * world.mapSize + tx] = com.example.game.model.TileTerrain.GRASS
      }
    }
    world =
      world.copy(
        tiles = cleanTiles,
        buildings = listOf(tc, barracks, enemyTc),
        units = listOf(carrier),
        nextEntityId = world.nextEntityId + 1,
      )

    repeat(160) {
      world = RtsEngine.step(world, 0.1f, cfg)
    }

    assertTrue(
      "Villager should round the Barracks and deposit carried Gold at the Town Center (expected >= ${startGold + 10}, was ${world.playerState.resources.gold})",
      world.playerState.resources.gold >= startGold + 10,
    )
  }

  @Test
  fun farmIsPermanentAfterExtendedHarvesting() {
    val cfg = SkirmishConfig()
    var world = MapGenerator.createWorld(cfg, seed = 415L)
    val tc = world.buildings.first { it.faction == Faction.PLAYER && it.type == BuildingType.TOWN_CENTER }
    val enemyTc = world.buildings.first { it.faction == Faction.ENEMY }
    val farm =
      com.example.game.model.Building(
        id = world.nextEntityId,
        type = BuildingType.FARM,
        faction = Faction.PLAYER,
        tileX = tc.tileX,
        tileY = tc.tileY + tc.footprint + 1,
        footprint = 2,
        hp = 350,
        maxHp = 350,
        buildProgress = 1f,
        isComplete = true,
        farmFoodRemaining = 1,
      )
    val vil = world.units.first { it.faction == Faction.PLAYER && it.type == UnitType.VILLAGER }
    val worker =
      vil.copy(
        x = farm.centerX,
        y = farm.centerY,
        order = com.example.game.model.UnitOrderType.GATHER,
        targetEntityId = farm.id,
        carryingAmount = 0,
        returningToDropOff = false,
      )
    world =
      world.copy(
        buildings = listOf(tc, farm, enemyTc),
        units = listOf(worker),
        nextEntityId = world.nextEntityId + 1,
      )
    val startFood = world.playerState.resources.food

    repeat(400) {
      world = RtsEngine.step(world, 0.1f, cfg)
    }

    val updatedFarm = world.buildings.firstOrNull { it.id == farm.id }
    assertNotNull("Farm should remain permanently after extended harvesting", updatedFarm)
    assertTrue(
      "Expected villager to deposit Food continuously from permanent Farm (start=$startFood, now=${world.playerState.resources.food})",
      world.playerState.resources.food >= startFood + 20,
    )
  }

  @Test
  fun difficultyTiersAndBuildingCategoriesAreBalanced() {
    assertEquals(4, Difficulty.entries.size)
    assertEquals(0.425f, Difficulty.STANDARD.aiGatherMultiplier, 0.001f)
    assertEquals(0.50f, Difficulty.MODERATE.aiGatherMultiplier, 0.001f)
    assertEquals(0.60f, Difficulty.HARD.aiGatherMultiplier, 0.001f)
    assertEquals(1.20f, Difficulty.EXTREME.aiGatherMultiplier, 0.001f)
    assertEquals(1.25f, Difficulty.EXTREME.aiTrainRate, 0.001f)

    val defs = com.example.game.model.GameDefinitions.buildingDefs.values
    val eco = defs.filter { it.category == com.example.game.model.BuildCategory.ECONOMY }
    val mil = defs.filter { it.category == com.example.game.model.BuildCategory.MILITARY }
    assertEquals(7, eco.size)
    assertEquals(8, mil.size)
    assertTrue(eco.any { it.type == BuildingType.FARM })
    assertTrue(eco.any { it.type == BuildingType.UNIVERSITY })
    assertTrue(mil.any { it.type == BuildingType.BARRACKS })
    assertTrue(mil.any { it.type == BuildingType.WAR_FACTORY })
    assertTrue(mil.any { it.type == BuildingType.AIRFIELD })
  }

  @Test
  fun buildingProductionQueueAccepts15ItemsAndCancelRefundsResources() {
    val cfg = SkirmishConfig()
    var world = MapGenerator.createWorld(cfg, seed = 500L)
    world = world.copy(playerState = world.playerState.copy(resources = world.playerState.resources.copy(food = 2000)))
    val tc = world.buildings.first { it.faction == Faction.PLAYER && it.type == BuildingType.TOWN_CENTER }

    repeat(20) {
      world = RtsEngine.enqueueUnit(world, Faction.PLAYER, tc.id, UnitType.VILLAGER)
    }

    val queuedTc = world.buildings.first { it.id == tc.id }
    assertEquals(RtsEngine.MAX_PRODUCTION_QUEUE, queuedTc.queue.size)
    assertEquals(15, queuedTc.queue.size)
    // 15 Villagers * 50 Food = 750 Food deducted -> 1250 remaining
    assertEquals(1250, world.playerState.resources.food)

    // Cancel index 1 (second item) and verify refund + size 14
    world = RtsEngine.cancelQueueItem(world, Faction.PLAYER, tc.id, 1)
    val afterCancelTc = world.buildings.first { it.id == tc.id }
    assertEquals(14, afterCancelTc.queue.size)
    assertEquals(1300, world.playerState.resources.food)
  }

  @Test
  fun millAndBlacksmithResearchesApplyEconomyAndCombatBonuses() {
    val cfg = SkirmishConfig()
    var world = MapGenerator.createWorld(cfg, seed = 501L)
    val baseState = world.playerState
    assertEquals(10, RtsEngine.effectiveCarryCapacity(baseState))
    assertEquals(1.0f, RtsEngine.villagerSpeedMultiplier(baseState), 0.001f)
    assertEquals(1.0f, RtsEngine.gatherRateMultiplier(baseState, com.example.game.model.ResourceType.FOOD), 0.001f)

    val researchedState =
      baseState.copy(
        age = Age.MODERN_AGE,
        researchedTechs =
          setOf(
            TechType.HORSE_COLLAR,
            TechType.WHEELBARROW,
            TechType.HEAVY_PLOW,
            TechType.CROP_ROTATION,
            TechType.MECHANIZED_FARMING,
            TechType.SUPPLY_LOGISTICS,
            TechType.FORGING,
            TechType.IRON_CASTING,
            TechType.BLAST_FURNACE,
            TechType.BALLISTICS_ALLOYS,
            TechType.REACTIVE_ARMOR,
          ),
      )

    assertEquals(20, RtsEngine.effectiveCarryCapacity(researchedState))
    assertTrue(RtsEngine.villagerSpeedMultiplier(researchedState) > 1.6f)
    assertTrue(RtsEngine.gatherRateMultiplier(researchedState, com.example.game.model.ResourceType.FOOD) > 3.0f)

    val baseVilHp = RtsEngine.unitMaxHpFor(UnitType.VILLAGER, baseState)
    val upgradedVilHp = RtsEngine.unitMaxHpFor(UnitType.VILLAGER, researchedState)
    assertTrue("Upgraded Villager HP ($upgradedVilHp) should exceed base ($baseVilHp)", upgradedVilHp >= baseVilHp + 20)

    val modernTankBaseHp = RtsEngine.unitMaxHpFor(UnitType.TANK, baseState.copy(age = Age.MODERN_AGE))
    val modernTankArmoredHp = RtsEngine.unitMaxHpFor(UnitType.TANK, researchedState)
    assertEquals(modernTankBaseHp + 30, modernTankArmoredHp)

    val playerTank =
      com.example.game.model.Unit(
        id = 7001,
        type = UnitType.TANK,
        faction = Faction.PLAYER,
        x = 12f,
        y = 12f,
        hp = modernTankArmoredHp,
        maxHp = modernTankArmoredHp,
      )
    val enemyTarget =
      com.example.game.model.Unit(
        id = 7002,
        type = UnitType.MILITIA,
        faction = Faction.ENEMY,
        x = 15f,
        y = 12f,
        hp = 40,
        maxHp = 40,
      )
    val tc = world.buildings.first { it.faction == Faction.PLAYER && it.type == BuildingType.TOWN_CENTER }
    val enemyTc = world.buildings.first { it.faction == Faction.ENEMY && it.type == BuildingType.TOWN_CENTER }

    val upgradedBattleWorld =
      world.copy(
        playerState = researchedState,
        buildings = listOf(tc, enemyTc),
        units = listOf(playerTank, enemyTarget),
      )
    val baseBattleWorld =
      world.copy(
        playerState = baseState.copy(age = Age.MODERN_AGE),
        buildings = listOf(tc, enemyTc),
        units = listOf(playerTank, enemyTarget),
      )

    val upgradedAfterShot = com.example.game.engine.RtsCombat.resolveCombatAndFog(upgradedBattleWorld, 0.1f)
    val baseAfterShot = com.example.game.engine.RtsCombat.resolveCombatAndFog(baseBattleWorld, 0.1f)

    val upgradedProj = upgradedAfterShot.projectiles.first()
    val baseProj = baseAfterShot.projectiles.first()
    assertEquals(com.example.game.model.ProjectileKind.SHELL, upgradedProj.kind)
    assertTrue(
      "Expected upgraded tank shell damage (${upgradedProj.damage}) to exceed base (${baseProj.damage}) by >= 10",
      upgradedProj.damage >= baseProj.damage + 10,
    )
    assertTrue(
      upgradedAfterShot.effects.any { it.type == com.example.game.model.VisualEffectType.MUZZLE_FLASH },
    )
  }

  @Test
  fun industrialAndModernAgesUnlockModernUnitsAndAircraftFlyOverWater() {
    val cfg = SkirmishConfig()
    var world = MapGenerator.createWorld(cfg, seed = 502L)
    val tc = world.buildings.first { it.faction == Faction.PLAYER && it.type == BuildingType.TOWN_CENTER }
    val enemyTc = world.buildings.first { it.faction == Faction.ENEMY && it.type == BuildingType.TOWN_CENTER }

    val university =
      com.example.game.model.Building(
        id = world.nextEntityId,
        type = BuildingType.UNIVERSITY,
        faction = Faction.PLAYER,
        tileX = tc.tileX + 4,
        tileY = tc.tileY,
        footprint = 3,
        hp = 2000,
        maxHp = 2000,
        buildProgress = 1f,
        isComplete = true,
      )
    val blacksmith =
      com.example.game.model.Building(
        id = world.nextEntityId + 1,
        type = BuildingType.BLACKSMITH,
        faction = Faction.PLAYER,
        tileX = tc.tileX - 3,
        tileY = tc.tileY,
        footprint = 2,
        hp = 1400,
        maxHp = 1400,
        buildProgress = 1f,
        isComplete = true,
      )
    val warFactory =
      com.example.game.model.Building(
        id = world.nextEntityId + 2,
        type = BuildingType.WAR_FACTORY,
        faction = Faction.PLAYER,
        tileX = tc.tileX,
        tileY = tc.tileY + 4,
        footprint = 3,
        hp = 2100,
        maxHp = 2100,
        buildProgress = 1f,
        isComplete = true,
      )

    // Advance Industrial -> Modern Age with required buildings & techs
    world =
      world.copy(
        buildings = world.buildings + university + blacksmith + warFactory,
        nextEntityId = world.nextEntityId + 3,
        playerState =
          world.playerState.copy(
            age = Age.IMPERIAL_AGE,
            resources = world.playerState.resources.copy(food = 5000, gold = 5000, wood = 3000),
            researchedTechs =
              setOf(
                TechType.BALLISTICS,
                TechType.BLAST_FURNACE,
                TechType.CHEMISTRY,
                TechType.BALLISTICS_ALLOYS,
              ),
          ),
      )
    world = RtsEngine.enqueueTech(world, Faction.PLAYER, tc.id, TechType.ADVANCE_INDUSTRIAL)
    repeat(420) { world = RtsEngine.step(world, 0.1f, cfg) }
    assertEquals(Age.INDUSTRIAL_AGE, world.playerState.age)

    world = RtsEngine.enqueueTech(world, Faction.PLAYER, tc.id, TechType.ADVANCE_MODERN)
    repeat(460) { world = RtsEngine.step(world, 0.1f, cfg) }
    assertEquals(Age.MODERN_AGE, world.playerState.age)

    // Verify Fighter Jet crosses water unobstructed
    val tilesWithWater = world.tiles.toMutableList()
    for (y in 0 until world.mapSize) {
      tilesWithWater[y * world.mapSize + 15] = com.example.game.model.TileTerrain.WATER
      tilesWithWater[y * world.mapSize + 16] = com.example.game.model.TileTerrain.WATER
    }
    val jet =
      com.example.game.model.Unit(
        id = world.nextEntityId,
        type = UnitType.FIGHTER_PLANE,
        faction = Faction.PLAYER,
        x = 12f,
        y = 12f,
        hp = 240,
        maxHp = 240,
      )
    world =
      world.copy(
        tiles = tilesWithWater,
        buildings = listOf(tc, enemyTc),
        units = listOf(jet),
        nextEntityId = world.nextEntityId + 1,
      )
    world = RtsEngine.orderMove(world, listOf(jet.id), 20f, 12f)
    repeat(30) { world = RtsEngine.step(world, 0.1f, cfg) }

    val movedJet = world.units.first { it.id == jet.id }
    assertTrue("Fighter Jet should fly straight across water barrier (x=${movedJet.x})", movedJet.x > 17f)
  }

  @Test
  fun mapGeneratorSupportsOneToThreeAiPlayersAndAllMapSizes() {
    val cases =
      listOf(
        Triple(1, com.example.game.model.MapSizeOption.STANDARD, 60),
        Triple(2, com.example.game.model.MapSizeOption.LARGE, 76),
        Triple(3, com.example.game.model.MapSizeOption.HUGE, 92),
      )
    for ((aiCount, sizeOpt, expectedTiles) in cases) {
      val cfg = SkirmishConfig(aiCount = aiCount, mapSize = sizeOpt)
      val world = MapGenerator.createWorld(cfg, seed = 700L + aiCount)
      assertEquals(expectedTiles, world.mapSize)
      assertEquals(expectedTiles * expectedTiles, world.tiles.size)
      assertEquals(cfg.activeAiFactions(), world.aiFactions)
      assertTrue(world.buildings.any { it.faction == Faction.PLAYER && it.type == BuildingType.TOWN_CENTER })
      for (ai in cfg.activeAiFactions()) {
        assertTrue(
          "Expected $ai to have a Town Center",
          world.buildings.any { it.faction == ai && it.type == BuildingType.TOWN_CENTER },
        )
        assertEquals(
          "Expected $ai to start with 4 units",
          4,
          world.units.count { it.faction == ai },
        )
      }
      if (aiCount < 3) {
        assertEquals(0, world.buildings.count { it.faction == Faction.ENEMY_3 })
      }
      if (aiCount < 2) {
        assertEquals(0, world.buildings.count { it.faction == Faction.ENEMY_2 })
      }
    }
  }

  @Test
  fun multiAiVictoryRequiresAllRivalsDefeatedAndPopulatesSummaries() {
    val cfg = SkirmishConfig(aiCount = 2, mapSize = com.example.game.model.MapSizeOption.STANDARD)
    val baseWorld = MapGenerator.createWorld(cfg, seed = 811L)
    val playerTc = baseWorld.buildings.first { it.faction == Faction.PLAYER && it.type == BuildingType.TOWN_CENTER }
    val enemyTc = baseWorld.buildings.first { it.faction == Faction.ENEMY && it.type == BuildingType.TOWN_CENTER }
    val enemy2Tc = baseWorld.buildings.first { it.faction == Faction.ENEMY_2 && it.type == BuildingType.TOWN_CENTER }

    // Destroying only ENEMY while ENEMY_2 survives must NOT end the match
    val attackerNextToEnemy2 =
      com.example.game.model.Unit(
        id = 9101,
        type = UnitType.TANK,
        faction = Faction.PLAYER,
        x = enemy2Tc.centerX - 1.5f,
        y = enemy2Tc.centerY,
        hp = 400,
        maxHp = 400,
        order = com.example.game.model.UnitOrderType.ATTACK_BUILDING,
        targetEntityId = enemy2Tc.id,
      )
    val oneAiLeftWorld =
      baseWorld.copy(
        buildings = listOf(playerTc, enemy2Tc.copy(hp = 5)),
        units = listOf(attackerNextToEnemy2),
        projectiles = emptyList(),
      )
    val midStep = RtsEngine.step(oneAiLeftWorld.copy(buildings = listOf(playerTc, enemy2Tc)), 0.1f, cfg)
    assertEquals(null, midStep.matchResult)

    // Destroying last building of last remaining AI triggers PLAYER victory with 2 AI summaries
    val finalBlowWorld =
      oneAiLeftWorld.copy(
        buildings = listOf(playerTc, enemy2Tc.copy(hp = 1)),
        units = listOf(attackerNextToEnemy2.copy(attackTimer = 0f)),
      )
    var endWorld = finalBlowWorld
    repeat(12) {
      if (endWorld.matchResult == null) {
        endWorld = com.example.game.engine.RtsCombat.resolveCombatAndFog(endWorld, 0.25f)
      }
    }
    val result = endWorld.matchResult
    assertNotNull("Expected victory once all active AIs are defeated (buildings left=${endWorld.buildings.map { it.faction }})", result)
    assertEquals(Faction.PLAYER, result!!.winner)
    assertEquals(2, result.aiSummaries.size)
    assertTrue(result.aiSummaries.any { it.faction == Faction.ENEMY })
    assertTrue(result.aiSummaries.any { it.faction == Faction.ENEMY_2 })
  }

  @Test
  fun aiCommanderOperatesAllLivingAiFactionsIndependently() {
    val cfg = SkirmishConfig(aiCount = 3, mapSize = com.example.game.model.MapSizeOption.LARGE)
    var world = MapGenerator.createWorld(cfg, seed = 912L)
    world = AiCommander.updateAi(world, Difficulty.HARD)
    for (ai in listOf(Faction.ENEMY, Faction.ENEMY_2, Faction.ENEMY_3)) {
      val busyVils =
        world.units.count {
          it.faction == ai && it.type == UnitType.VILLAGER && it.order != com.example.game.model.UnitOrderType.IDLE
        }
      val tcQueue =
        world.buildings
          .first { it.faction == ai && it.type == BuildingType.TOWN_CENTER }
          .queue
      assertTrue(
        "Expected $ai to assign villagers or queue production (busyVils=$busyVils, tcQueue=${tcQueue.size})",
        busyVils > 0 || tcQueue.isNotEmpty(),
      )
    }
  }
}

