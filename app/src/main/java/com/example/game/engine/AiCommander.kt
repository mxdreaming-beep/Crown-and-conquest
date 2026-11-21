package com.example.game.engine

import com.example.game.model.Age
import com.example.game.model.Building
import com.example.game.model.BuildingType
import com.example.game.model.Difficulty
import com.example.game.model.Faction
import com.example.game.model.GameDefinitions
import com.example.game.model.GameWorld
import com.example.game.model.ResourceType
import com.example.game.model.TechType
import com.example.game.model.UnitOrderType
import com.example.game.model.UnitType
import kotlin.math.hypot

object AiCommander {

  fun updateAi(world: GameWorld, difficulty: Difficulty): GameWorld {
    var nextWorld = world
    val activeAis = nextWorld.aiFactions.ifEmpty { listOf(Faction.ENEMY) }
    for (aiFaction in activeAis) {
      nextWorld = updateSingleAi(nextWorld, aiFaction, difficulty)
    }
    return nextWorld
  }

  private fun updateSingleAi(world: GameWorld, aiFaction: Faction, difficulty: Difficulty): GameWorld {
    var nextWorld = world
    val aiBuildingsInitial = nextWorld.buildings.filter { it.faction == aiFaction }
    val aiUnitsInitial = nextWorld.units.filter { it.faction == aiFaction }
    if (aiBuildingsInitial.isEmpty() && aiUnitsInitial.isEmpty()) return nextWorld

    val enemyTc = aiBuildingsInitial.firstOrNull { it.type == BuildingType.TOWN_CENTER && it.isComplete }
    val aiState = nextWorld.stateFor(aiFaction)
    val aiVillagers = aiUnitsInitial.filter { it.type == UnitType.VILLAGER }
    val aiMilitary = aiUnitsInitial.filter { it.type != UnitType.VILLAGER }

    val popCap = RtsEngine.populationCap(aiBuildingsInitial)
    val targetVillagers =
      when (difficulty) {
        Difficulty.STANDARD -> 5
        Difficulty.MODERATE -> 7
        Difficulty.HARD -> 9
        Difficulty.EXTREME -> 18
      }

    // 1. Train Villagers or advance Age at Town Center
    if (enemyTc != null && enemyTc.queue.isEmpty()) {
      val nextAgeTech =
        when (aiState.age) {
          Age.DARK_AGE -> TechType.ADVANCE_FEUDAL
          Age.FEUDAL_AGE -> TechType.ADVANCE_CASTLE
          Age.CASTLE_AGE -> TechType.ADVANCE_IMPERIAL
          Age.IMPERIAL_AGE -> TechType.ADVANCE_INDUSTRIAL
          Age.INDUSTRIAL_AGE -> TechType.ADVANCE_MODERN
          Age.MODERN_AGE -> null
        }

      val ageThresholdVils =
        minOf(
          targetVillagers,
          when (aiState.age) {
            Age.DARK_AGE -> 7
            Age.FEUDAL_AGE -> 10
            Age.CASTLE_AGE -> 12
            else -> 14
          },
        )

      if (nextAgeTech != null &&
        aiVillagers.size >= ageThresholdVils &&
        nextAgeTech !in aiState.researchingTechs &&
        aiState.resources.canAfford(GameDefinitions.techDefs.getValue(nextAgeTech).cost)
      ) {
        nextWorld = RtsEngine.enqueueTech(nextWorld, aiFaction, enemyTc.id, nextAgeTech)
      } else if (aiVillagers.size < targetVillagers && aiUnitsInitial.size < popCap) {
        nextWorld = RtsEngine.enqueueUnit(nextWorld, aiFaction, enemyTc.id, UnitType.VILLAGER)
      }
    }

    // 2. Construct Houses if approaching population cap, and economy/military buildings
    val idleBuilder =
      nextWorld.units.firstOrNull {
        it.faction == aiFaction &&
          it.type == UnitType.VILLAGER &&
          it.order != UnitOrderType.BUILD
      }

    if (idleBuilder != null && enemyTc != null) {
      val updatedAiBuildings = nextWorld.buildings.filter { it.faction == aiFaction }
      val incompleteBuilding = updatedAiBuildings.firstOrNull { !it.isComplete }
      if (incompleteBuilding != null) {
        nextWorld = RtsEngine.orderBuild(nextWorld, listOf(idleBuilder.id), incompleteBuilding.id)
      } else {
        val currentPop = nextWorld.units.count { it.faction == aiFaction }
        val currentCap = RtsEngine.populationCap(updatedAiBuildings)
        val desiredBuilding =
          chooseNextAiBuilding(
            world = nextWorld,
            aiFaction = aiFaction,
            currentPop = currentPop,
            currentCap = currentCap,
            aiBuildings = updatedAiBuildings,
          )
        if (desiredBuilding != null) {
          val spot = findBuildSpotNear(nextWorld, enemyTc.tileX, enemyTc.tileY, desiredBuilding)
          if (spot != null) {
            nextWorld =
              RtsEngine.placeBuilding(
                world = nextWorld,
                faction = aiFaction,
                type = desiredBuilding,
                tileX = spot.first,
                tileY = spot.second,
                builderIds = listOf(idleBuilder.id),
              )
          }
        }
      }
    }

    // 3. Assign idle Villagers to gather needed resources
    val refreshedVillagers =
      nextWorld.units.filter {
        it.faction == aiFaction &&
          it.type == UnitType.VILLAGER &&
          it.order == UnitOrderType.IDLE
      }
    for ((idx, vil) in refreshedVillagers.withIndex()) {
      val curSt = nextWorld.stateFor(aiFaction)
      val desiredType =
        when {
          curSt.resources.food < 220 -> ResourceType.FOOD
          idx % 3 == 0 -> ResourceType.FOOD
          idx % 3 == 1 -> ResourceType.WOOD
          curSt.age.index >= Age.FEUDAL_AGE.index && idx % 5 == 4 -> ResourceType.STONE
          else -> ResourceType.GOLD
        }
      nextWorld = assignAiVillagerToResource(nextWorld, aiFaction, vil.id, desiredType)
    }

    // 4. Train military units & research Mill / Mining Camp / University / Blacksmith upgrades
    for (b in nextWorld.buildings.filter { it.faction == aiFaction && it.isComplete && it.queue.isEmpty() }) {
      val curState = nextWorld.stateFor(aiFaction)
      val curUnits = nextWorld.units.count { it.faction == aiFaction }
      val cap = RtsEngine.populationCap(nextWorld.buildings.filter { it.faction == aiFaction })
      val isResearchBuilding =
        b.type == BuildingType.BLACKSMITH ||
          b.type == BuildingType.MILL ||
          b.type == BuildingType.MINING_CAMP ||
          b.type == BuildingType.UNIVERSITY
      if (curUnits >= cap && !isResearchBuilding) continue

      when (b.type) {
        BuildingType.BARRACKS -> {
          val unitToTrain =
            when {
              curState.age.index >= Age.MODERN_AGE.index && aiMilitary.count { it.type == UnitType.ROCKET_SOLDIER } < 2 ->
                UnitType.ROCKET_SOLDIER
              curState.age.index >= Age.MODERN_AGE.index && aiMilitary.count { it.type == UnitType.MACHINE_GUNNER } < 2 ->
                UnitType.MACHINE_GUNNER
              curState.age.index >= Age.MODERN_AGE.index -> UnitType.MARINE
              curState.age.index >= Age.INDUSTRIAL_AGE.index && aiMilitary.count { it.type == UnitType.MACHINE_GUNNER } < 3 ->
                UnitType.MACHINE_GUNNER
              TechType.UPGRADE_MAN_AT_ARMS in curState.researchedTechs -> UnitType.MAN_AT_ARMS
              curState.age.index >= Age.FEUDAL_AGE.index && aiMilitary.count { it.type == UnitType.SPEARMAN || it.type == UnitType.PIKEMAN } < 2 ->
                if (TechType.UPGRADE_PIKEMAN in curState.researchedTechs) UnitType.PIKEMAN else UnitType.SPEARMAN
              else -> UnitType.MILITIA
            }
          nextWorld = RtsEngine.enqueueUnit(nextWorld, aiFaction, b.id, unitToTrain)
        }
        BuildingType.ARCHERY_RANGE -> {
          val unitToTrain =
            when {
              curState.age.index >= Age.MODERN_AGE.index && aiMilitary.count { it.type == UnitType.SNIPER } < 2 ->
                UnitType.SNIPER
              curState.age.index >= Age.INDUSTRIAL_AGE.index -> UnitType.RIFLEMAN
              TechType.UPGRADE_CROSSBOW in curState.researchedTechs -> UnitType.CROSSBOWMAN
              else -> UnitType.ARCHER
            }
          nextWorld = RtsEngine.enqueueUnit(nextWorld, aiFaction, b.id, unitToTrain)
        }
        BuildingType.STABLE -> {
          val unitToTrain =
            if (curState.age.index >= Age.CASTLE_AGE.index) UnitType.KNIGHT else UnitType.SCOUT_CAVALRY
          nextWorld = RtsEngine.enqueueUnit(nextWorld, aiFaction, b.id, unitToTrain)
        }
        BuildingType.CASTLE -> {
          nextWorld = RtsEngine.enqueueUnit(nextWorld, aiFaction, b.id, UnitType.BATTERING_RAM)
        }
        BuildingType.WAR_FACTORY -> {
          val unitToTrain =
            when {
              curState.age.index >= Age.MODERN_AGE.index && aiMilitary.count { it.type == UnitType.ROCKET_LAUNCHER } < 2 ->
                UnitType.ROCKET_LAUNCHER
              curState.age.index >= Age.MODERN_AGE.index && aiMilitary.count { it.type == UnitType.HEAVY_TANK } < 3 ->
                UnitType.HEAVY_TANK
              aiMilitary.count { it.type == UnitType.ARTILLERY } < 2 ->
                UnitType.ARTILLERY
              else -> UnitType.TANK
            }
          nextWorld = RtsEngine.enqueueUnit(nextWorld, aiFaction, b.id, unitToTrain)
        }
        BuildingType.AIRFIELD -> {
          val unitToTrain =
            when {
              aiMilitary.count { it.type == UnitType.GUNSHIP } < 2 -> UnitType.GUNSHIP
              aiMilitary.count { it.type == UnitType.BOMBER_PLANE } < 2 -> UnitType.BOMBER_PLANE
              else -> UnitType.FIGHTER_PLANE
            }
          nextWorld = RtsEngine.enqueueUnit(nextWorld, aiFaction, b.id, unitToTrain)
        }
        BuildingType.MILL -> {
          val tech =
            listOf(
              TechType.STONE_GRINDING,
              TechType.HORSE_COLLAR,
              TechType.WHEELBARROW,
              TechType.HEAVY_PLOW,
              TechType.GRANARY_SILOS,
              TechType.CROP_ROTATION,
              TechType.MECHANIZED_FARMING,
              TechType.SUPPLY_LOGISTICS,
            ).firstOrNull {
              val def = GameDefinitions.techDefs.getValue(it)
              curState.age.index >= def.requiredAge.index &&
                it !in curState.researchedTechs &&
                it !in curState.researchingTechs &&
                curState.resources.canAfford(def.cost)
            }
          if (tech != null) {
            nextWorld = RtsEngine.enqueueTech(nextWorld, aiFaction, b.id, tech)
          }
        }
        BuildingType.MINING_CAMP -> {
          val tech =
            listOf(
              TechType.GOLD_MINING,
              TechType.STONE_MINING,
              TechType.GOLD_SHAFT_MINING,
              TechType.STONE_SHAFT_MINING,
              TechType.INDUSTRIAL_EXCAVATION,
            ).firstOrNull {
              val def = GameDefinitions.techDefs.getValue(it)
              curState.age.index >= def.requiredAge.index &&
                it !in curState.researchedTechs &&
                it !in curState.researchingTechs &&
                curState.resources.canAfford(def.cost)
            }
          if (tech != null) {
            nextWorld = RtsEngine.enqueueTech(nextWorld, aiFaction, b.id, tech)
          }
        }
        BuildingType.UNIVERSITY -> {
          val tech =
            listOf(
              TechType.MASONRY,
              TechType.TREADMILL_CRANE,
              TechType.BALLISTICS,
              TechType.ARCHITECTURE,
              TechType.SIEGE_ENGINEERS,
              TechType.CHEMISTRY,
              TechType.ACADEMIC_CONSCRIPTION,
              TechType.AERODYNAMICS,
            ).firstOrNull {
              val def = GameDefinitions.techDefs.getValue(it)
              curState.age.index >= def.requiredAge.index &&
                it !in curState.researchedTechs &&
                it !in curState.researchingTechs &&
                curState.resources.canAfford(def.cost)
            }
          if (tech != null) {
            nextWorld = RtsEngine.enqueueTech(nextWorld, aiFaction, b.id, tech)
          }
        }
        BuildingType.BLACKSMITH -> {
          val tech =
            listOf(
              TechType.FORGING,
              TechType.FLETCHING,
              TechType.SCALE_ARMOR,
              TechType.IRON_CASTING,
              TechType.CHAIN_MAIL,
              TechType.BODKIN_ARROW,
              TechType.BLAST_FURNACE,
              TechType.PLATE_ARMOR,
              TechType.BALLISTICS_ALLOYS,
              TechType.REACTIVE_ARMOR,
            ).firstOrNull {
              val def = GameDefinitions.techDefs.getValue(it)
              curState.age.index >= def.requiredAge.index &&
                it !in curState.researchedTechs &&
                it !in curState.researchingTechs &&
                curState.resources.canAfford(def.cost)
            }
          if (tech != null) {
            nextWorld = RtsEngine.enqueueTech(nextWorld, aiFaction, b.id, tech)
          }
        }
        else -> Unit
      }
    }

    // 5. Base Defense & Coordinated Multi-Faction Attack Waves
    if (enemyTc != null) {
      val threatsNearBase =
        nextWorld.units.filter {
          it.faction != aiFaction && hypot(it.x - enemyTc.centerX, it.y - enemyTc.centerY) < 12f
        }
      if (threatsNearBase.isNotEmpty()) {
        val threat = threatsNearBase.first()
        val defenders =
          nextWorld.units
            .filter { it.faction == aiFaction && it.type != UnitType.VILLAGER }
            .map { it.id }
        if (defenders.isNotEmpty()) {
          nextWorld = RtsEngine.orderAttackMove(nextWorld, defenders, threat.x, threat.y)
        }
      } else {
        val rivalTarget =
          nextWorld.buildings
            .filter { it.faction != aiFaction }
            .minByOrNull { b ->
              val tcBonus = if (b.type == BuildingType.TOWN_CENTER) 0f else 12f
              hypot(b.centerX - enemyTc.centerX, b.centerY - enemyTc.centerY) + tcBonus
            }
        if (rivalTarget != null) {
          val waveSize =
            when (difficulty) {
              Difficulty.STANDARD -> 10
              Difficulty.MODERATE -> 8
              Difficulty.HARD -> 6
              Difficulty.EXTREME -> 4
            }
          val idleArmy =
            nextWorld.units.filter {
              it.faction == aiFaction &&
                it.type != UnitType.VILLAGER &&
                it.type != UnitType.SCOUT_CAVALRY &&
                it.order == UnitOrderType.IDLE
            }
          if (idleArmy.size >= waveSize) {
            nextWorld =
              RtsEngine.orderAttackMove(
                nextWorld,
                idleArmy.map { it.id },
                rivalTarget.centerX,
                rivalTarget.centerY,
              )
          }
        }
      }
    }

    return nextWorld
  }

  private fun chooseNextAiBuilding(
    world: GameWorld,
    aiFaction: Faction,
    currentPop: Int,
    currentCap: Int,
    aiBuildings: List<Building>,
  ): BuildingType? {
    val st = world.stateFor(aiFaction)
    val age = st.age
    val res = st.resources
    fun countOf(t: BuildingType) = aiBuildings.count { it.type == t }

    if (currentPop + 2 >= currentCap && currentCap < RtsEngine.MAX_POPULATION_CAP && res.canAfford(GameDefinitions.buildingDefs.getValue(BuildingType.HOUSE).cost)) {
      return BuildingType.HOUSE
    }
    if (countOf(BuildingType.BARRACKS) == 0 && res.canAfford(GameDefinitions.buildingDefs.getValue(BuildingType.BARRACKS).cost)) {
      return BuildingType.BARRACKS
    }
    if (countOf(BuildingType.MILL) == 0 && res.canAfford(GameDefinitions.buildingDefs.getValue(BuildingType.MILL).cost)) {
      return BuildingType.MILL
    }
    if (countOf(BuildingType.MINING_CAMP) == 0 && res.canAfford(GameDefinitions.buildingDefs.getValue(BuildingType.MINING_CAMP).cost)) {
      return BuildingType.MINING_CAMP
    }
    if (countOf(BuildingType.FARM) < 4 && countOf(BuildingType.MILL) > 0 && res.canAfford(GameDefinitions.buildingDefs.getValue(BuildingType.FARM).cost)) {
      return BuildingType.FARM
    }
    if (age.index >= Age.FEUDAL_AGE.index) {
      if (countOf(BuildingType.ARCHERY_RANGE) == 0 && res.canAfford(GameDefinitions.buildingDefs.getValue(BuildingType.ARCHERY_RANGE).cost)) {
        return BuildingType.ARCHERY_RANGE
      }
      if (countOf(BuildingType.STABLE) == 0 && res.canAfford(GameDefinitions.buildingDefs.getValue(BuildingType.STABLE).cost)) {
        return BuildingType.STABLE
      }
      if (countOf(BuildingType.BLACKSMITH) == 0 && res.canAfford(GameDefinitions.buildingDefs.getValue(BuildingType.BLACKSMITH).cost)) {
        return BuildingType.BLACKSMITH
      }
      if (countOf(BuildingType.UNIVERSITY) == 0 && res.canAfford(GameDefinitions.buildingDefs.getValue(BuildingType.UNIVERSITY).cost)) {
        return BuildingType.UNIVERSITY
      }
      if (countOf(BuildingType.WATCH_TOWER) < 2 && res.canAfford(GameDefinitions.buildingDefs.getValue(BuildingType.WATCH_TOWER).cost)) {
        return BuildingType.WATCH_TOWER
      }
    }
    if (age.index >= Age.CASTLE_AGE.index) {
      if (countOf(BuildingType.CASTLE) == 0 && res.canAfford(GameDefinitions.buildingDefs.getValue(BuildingType.CASTLE).cost)) {
        return BuildingType.CASTLE
      }
    }
    if (age.index >= Age.INDUSTRIAL_AGE.index) {
      if (countOf(BuildingType.WAR_FACTORY) == 0 && res.canAfford(GameDefinitions.buildingDefs.getValue(BuildingType.WAR_FACTORY).cost)) {
        return BuildingType.WAR_FACTORY
      }
    }
    if (age.index >= Age.MODERN_AGE.index) {
      if (countOf(BuildingType.AIRFIELD) == 0 && res.canAfford(GameDefinitions.buildingDefs.getValue(BuildingType.AIRFIELD).cost)) {
        return BuildingType.AIRFIELD
      }
    }
    return null
  }

  private fun findBuildSpotNear(
    world: GameWorld,
    originX: Int,
    originY: Int,
    type: BuildingType,
  ): Pair<Int, Int>? {
    for (radius in 3..10) {
      for (dy in -radius..radius) {
        for (dx in -radius..radius) {
          val tx = originX + dx
          val ty = originY + dy
          if (RtsEngine.canPlaceBuilding(world, type, tx, ty)) {
            return tx to ty
          }
        }
      }
    }
    return null
  }

  private fun assignAiVillagerToResource(
    world: GameWorld,
    aiFaction: Faction,
    villagerId: Int,
    desiredType: ResourceType,
  ): GameWorld {
    val vil = world.units.firstOrNull { it.id == villagerId } ?: return world
    if (desiredType == ResourceType.FOOD) {
      val openFarm =
        world.buildings.firstOrNull {
          it.faction == aiFaction &&
            it.type == BuildingType.FARM &&
            it.isComplete &&
            RtsEngine.isWithinTownCenterRadius(it.centerX, it.centerY, aiFaction, world.buildings) &&
            world.units.none { u -> u.order == UnitOrderType.GATHER && u.targetEntityId == it.id }
        }
      if (openFarm != null) {
        return RtsEngine.orderGatherFarm(world, listOf(villagerId), openFarm.id)
      }
    }
    val node =
      world.resources
        .filter {
          it.type.resourceType == desiredType &&
            it.remaining > 0 &&
            RtsEngine.isWithinTownCenterRadius(it.x, it.y, aiFaction, world.buildings)
        }
        .minByOrNull { hypot(it.x - vil.x, it.y - vil.y) }
        ?: world.resources
          .filter {
            it.remaining > 0 &&
              RtsEngine.isWithinTownCenterRadius(it.x, it.y, aiFaction, world.buildings)
          }
          .minByOrNull { hypot(it.x - vil.x, it.y - vil.y) }
        ?: return world
    return RtsEngine.orderGatherNode(world, listOf(villagerId), node.id)
  }
}
