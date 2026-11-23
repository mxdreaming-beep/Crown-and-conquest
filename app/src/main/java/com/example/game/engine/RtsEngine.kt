package com.example.game.engine

import com.example.game.model.Age
import com.example.game.model.Building
import com.example.game.model.BuildingType
import com.example.game.model.Faction
import com.example.game.model.FactionState
import com.example.game.model.FloatingText
import com.example.game.model.GameDefinitions
import com.example.game.model.GameWorld
import com.example.game.model.ProductionItem
import com.example.game.model.ResourceNode
import com.example.game.model.ResourceType
import com.example.game.model.SkirmishConfig
import com.example.game.model.TechType
import com.example.game.model.TileTerrain
import com.example.game.model.Unit
import com.example.game.model.UnitOrderType
import com.example.game.model.UnitType
import com.example.game.model.VisualEffect
import com.example.game.model.VisualEffectType
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

object RtsEngine {

  const val MAX_POPULATION_CAP = 100
  const val MAX_PRODUCTION_QUEUE = 15
  const val CARRY_CAPACITY = 10
  const val VILLAGER_AUTO_RADIUS = 15f
  const val BUILD_TC_RADIUS = 20f

  fun isWithinBuildRadius(
    world: GameWorld,
    faction: Faction,
    type: BuildingType,
    tileX: Int,
    tileY: Int,
    radius: Float = BUILD_TC_RADIUS,
  ): Boolean {
    if (type == BuildingType.TOWN_CENTER) return true
    val fp = GameDefinitions.buildingDefs.getValue(type).footprintTiles
    val cx = tileX + fp * 0.5f
    val cy = tileY + fp * 0.5f
    val tcs = world.buildings.filter { it.faction == faction && it.type == BuildingType.TOWN_CENTER }
    if (tcs.isEmpty()) return false
    return tcs.any { tc -> hypot(cx - tc.centerX, cy - tc.centerY) <= radius }
  }

  fun isWithinTownCenterRadius(
    wx: Float,
    wy: Float,
    faction: Faction,
    buildings: Collection<Building>,
    radius: Float = VILLAGER_AUTO_RADIUS,
  ): Boolean {
    val tcs = buildings.filter { it.faction == faction && it.type == BuildingType.TOWN_CENTER }
    val anchors = if (tcs.isNotEmpty()) tcs else buildings.filter { it.faction == faction }
    if (anchors.isEmpty()) return false
    return anchors.any { b -> hypot(wx - b.centerX, wy - b.centerY) <= radius }
  }

  fun effectiveCarryCapacity(state: FactionState): Int {
    var cap = CARRY_CAPACITY
    if (TechType.HEAVY_PLOW in state.researchedTechs) cap += 5
    if (TechType.GRANARY_SILOS in state.researchedTechs) cap += 4
    if (TechType.GOLD_SHAFT_MINING in state.researchedTechs) cap += 3
    if (TechType.INDUSTRIAL_EXCAVATION in state.researchedTechs) cap += 4
    if (TechType.MECHANIZED_FARMING in state.researchedTechs) cap += 5
    return cap
  }

  fun villagerSpeedMultiplier(state: FactionState): Float {
    var mul = 1f
    if (TechType.STONE_GRINDING in state.researchedTechs) mul *= 1.10f
    if (TechType.WHEELBARROW in state.researchedTechs) mul *= 1.25f
    if (TechType.MECHANIZED_FARMING in state.researchedTechs) mul *= 1.30f
    return mul
  }

  fun unitEffectiveSpeed(type: UnitType, state: FactionState): Float {
    val uDef = GameDefinitions.unitDefs.getValue(type)
    return when {
      type == UnitType.VILLAGER -> uDef.speed * villagerSpeedMultiplier(state)
      uDef.isFlying && TechType.AERODYNAMICS in state.researchedTechs -> uDef.speed * 1.15f
      else -> uDef.speed
    }
  }

  fun gatherRateMultiplier(state: FactionState, resourceType: ResourceType): Float {
    var mul = 1f
    if (resourceType == ResourceType.FOOD && TechType.STONE_GRINDING in state.researchedTechs) mul *= 1.15f
    if (TechType.HORSE_COLLAR in state.researchedTechs) mul *= 1.25f
    if (resourceType == ResourceType.FOOD && TechType.HEAVY_PLOW in state.researchedTechs) mul *= 1.20f
    if (resourceType == ResourceType.FOOD && TechType.GRANARY_SILOS in state.researchedTechs) mul *= 1.20f
    if ((resourceType == ResourceType.FOOD || resourceType == ResourceType.WOOD) &&
      TechType.CROP_ROTATION in state.researchedTechs
    ) {
      mul *= 1.35f
    }
    if (resourceType == ResourceType.GOLD && TechType.GOLD_MINING in state.researchedTechs) mul *= 1.25f
    if (resourceType == ResourceType.GOLD && TechType.GOLD_SHAFT_MINING in state.researchedTechs) mul *= 1.35f
    if (resourceType == ResourceType.STONE && TechType.STONE_MINING in state.researchedTechs) mul *= 1.25f
    if (resourceType == ResourceType.STONE && TechType.STONE_SHAFT_MINING in state.researchedTechs) mul *= 1.35f
    if ((resourceType == ResourceType.GOLD || resourceType == ResourceType.STONE) &&
      TechType.INDUSTRIAL_EXCAVATION in state.researchedTechs
    ) {
      mul *= 1.40f
    }
    if (TechType.MECHANIZED_FARMING in state.researchedTechs) mul *= 1.30f
    if (TechType.SUPPLY_LOGISTICS in state.researchedTechs) mul *= 1.40f
    return mul
  }

  fun unitMaxHpFor(type: UnitType, state: FactionState): Int {
    val uDef = GameDefinitions.unitDefs.getValue(type)
    var hp =
      when {
        state.civilization == com.example.game.model.Civilization.FRANKS && uDef.isCavalry ->
          (uDef.maxHp * 1.2f).toInt()
        state.age.index >= Age.IMPERIAL_AGE.index -> (uDef.maxHp * 1.2f).toInt()
        else -> uDef.maxHp
      }
    if (type == UnitType.VILLAGER && TechType.WHEELBARROW in state.researchedTechs) {
      hp += 20
    }
    if (type != UnitType.VILLAGER && TechType.REACTIVE_ARMOR in state.researchedTechs) {
      hp += 30
    }
    return hp
  }

  fun populationCap(buildings: List<Building>): Int {
    val total =
      buildings
        .filter { it.isComplete }
        .sumOf { GameDefinitions.buildingDefs.getValue(it.type).popBonus }
    return total.coerceIn(5, MAX_POPULATION_CAP)
  }

  fun canPlaceBuilding(
    world: GameWorld,
    type: BuildingType,
    tileX: Int,
    tileY: Int,
    ignoreBuildingId: Int = -1,
    faction: Faction? = null,
  ): Boolean {
    val def = GameDefinitions.buildingDefs.getValue(type)
    val fp = def.footprintTiles
    if (tileX < 1 || tileY < 1 || tileX + fp > world.mapSize - 1 || tileY + fp > world.mapSize - 1) {
      return false
    }
    if (faction != null && !isWithinBuildRadius(world, faction, type, tileX, tileY)) {
      return false
    }
    for (dy in 0 until fp) {
      for (dx in 0 until fp) {
        val t = world.tileAt(tileX + dx, tileY + dy)
        if (t == TileTerrain.WATER || t == TileTerrain.CLIFF) return false
      }
    }
    for (b in world.buildings) {
      if (b.id == ignoreBuildingId) continue
      val overlapX = tileX < b.tileX + b.footprint && tileX + fp > b.tileX
      val overlapY = tileY < b.tileY + b.footprint && tileY + fp > b.tileY
      if (overlapX && overlapY) return false
    }
    for (r in world.resources) {
      if (r.tileX in tileX until (tileX + fp) && r.tileY in tileY until (tileY + fp)) {
        return false
      }
    }
    return true
  }

  fun placeBuilding(
    world: GameWorld,
    faction: Faction,
    type: BuildingType,
    tileX: Int,
    tileY: Int,
    builderIds: List<Int>,
  ): GameWorld {
    if (!canPlaceBuilding(world, type, tileX, tileY, faction = faction)) return world
    val fState = world.stateFor(faction)
    val def = GameDefinitions.buildingDefs.getValue(type)
    if (fState.age.index < def.requiredAge.index) return world
    val cost = GameDefinitions.adjustedBuildingCost(type, fState.civilization)
    if (!fState.resources.canAfford(cost)) return world

    val maxHp = GameDefinitions.effectiveBuildingMaxHp(type, fState)
    val newId = world.nextEntityId
    val building =
      Building(
        id = newId,
        type = type,
        faction = faction,
        tileX = tileX,
        tileY = tileY,
        footprint = def.footprintTiles,
        hp = max(1, maxHp / 10),
        maxHp = maxHp,
        buildProgress = 0.05f,
        isComplete = false,
        rallyX = (tileX + def.footprintTiles * 0.5f).coerceIn(1f, world.mapSize - 2f),
        rallyY = (tileY + def.footprintTiles + 0.8f).coerceIn(1f, world.mapSize - 2f),
      )

    val updatedFaction = fState.copy(resources = fState.resources - cost)
    val updatedWorld =
      world
        .copy(
          buildings = world.buildings + building,
          nextEntityId = newId + 1,
        )
        .withFactionState(faction, updatedFaction)
    return enqueueBuild(updatedWorld, builderIds, newId)
  }

  fun moveBuilding(
    world: GameWorld,
    faction: Faction,
    buildingId: Int,
    targetTileX: Int,
    targetTileY: Int,
  ): GameWorld {
    val existing = world.buildings.firstOrNull { it.id == buildingId && it.faction == faction } ?: return world
    if (existing.tileX == targetTileX && existing.tileY == targetTileY) return world
    if (!canPlaceBuilding(world, existing.type, targetTileX, targetTileY, ignoreBuildingId = buildingId, faction = faction)) {
      return world
    }

    val dxTile = (targetTileX - existing.tileX).toFloat()
    val dyTile = (targetTileY - existing.tileY).toFloat()
    val movedBuilding =
      existing.copy(
        tileX = targetTileX,
        tileY = targetTileY,
        rallyX = (existing.rallyX + dxTile).coerceIn(1f, world.mapSize - 2f),
        rallyY = (existing.rallyY + dyTile).coerceIn(1f, world.mapSize - 2f),
      )

    val bDef = GameDefinitions.buildingDefs.getValue(movedBuilding.type)
    val updatedBuildings = world.buildings.map { if (it.id == buildingId) movedBuilding else it }

    val updatedUnits =
      world.units.map { u ->
        var nextU = u
        if (nextU.targetEntityId == buildingId &&
          (nextU.order == UnitOrderType.BUILD || nextU.order == UnitOrderType.GATHER || nextU.order == UnitOrderType.ATTACK_BUILDING)
        ) {
          nextU = nextU.copy(targetX = movedBuilding.centerX, targetY = movedBuilding.centerY)
        }
        val uDef = GameDefinitions.unitDefs.getValue(nextU.type)
        if (!uDef.isFlying && !bDef.isWalkable &&
          nextU.x >= movedBuilding.tileX - 0.04f &&
          nextU.x <= movedBuilding.tileX + movedBuilding.footprint + 0.04f &&
          nextU.y >= movedBuilding.tileY - 0.04f &&
          nextU.y <= movedBuilding.tileY + movedBuilding.footprint + 0.04f
        ) {
          val (ex, ey) = nearestApproachPoint(nextU.x, nextU.y, movedBuilding, world, updatedBuildings)
          nextU = nextU.copy(x = ex, y = ey)
        }
        nextU
      }

    var nextId = world.nextEntityId
    val dust =
      VisualEffect(
        id = nextId++,
        type = VisualEffectType.DUST_PUFF,
        x = movedBuilding.centerX,
        y = movedBuilding.centerY,
        durationSec = 0.65f,
      )

    val updatedWorld =
      world.copy(
        buildings = updatedBuildings,
        units = updatedUnits,
        effects = world.effects + dust,
        nextEntityId = nextId,
      )
    return recomputeFog(updatedWorld)
  }

  fun enqueueUnit(world: GameWorld, faction: Faction, buildingId: Int, unitType: UnitType): GameWorld {
    val building = world.buildings.firstOrNull { it.id == buildingId && it.faction == faction && it.isComplete }
      ?: return world
    if (building.queue.size >= MAX_PRODUCTION_QUEUE) return world

    val fState = world.stateFor(faction)
    val unitDef = GameDefinitions.unitDefs.getValue(unitType)
    if (fState.age.index < unitDef.requiredAge.index) return world
    if (unitDef.requiredTech != null && unitDef.requiredTech !in fState.researchedTechs) return world
    if (unitDef.trainedAt != building.type) return world

    val cost = GameDefinitions.adjustedUnitCost(unitType, fState.civilization)
    if (!fState.resources.canAfford(cost)) return world

    var trainTime = unitDef.trainTimeSec
    if (fState.civilization == com.example.game.model.Civilization.BRITONS &&
      building.type == BuildingType.ARCHERY_RANGE
    ) {
      trainTime *= 0.85f
    }
    if (TechType.SUPPLY_LOGISTICS in fState.researchedTechs) {
      trainTime *= 0.85f
    }
    if (TechType.ACADEMIC_CONSCRIPTION in fState.researchedTechs) {
      trainTime *= 0.80f
    }

    val item = ProductionItem.TrainUnit(unitType = unitType, totalTime = trainTime)
    val updatedBuilding = building.copy(queue = building.queue + item)
    val updatedFaction = fState.copy(resources = fState.resources - cost)

    return world
      .copy(buildings = world.buildings.map { if (it.id == buildingId) updatedBuilding else it })
      .withFactionState(faction, updatedFaction)
  }

  fun enqueueTech(world: GameWorld, faction: Faction, buildingId: Int, techType: TechType): GameWorld {
    val building = world.buildings.firstOrNull { it.id == buildingId && it.faction == faction && it.isComplete }
      ?: return world
    if (building.queue.size >= MAX_PRODUCTION_QUEUE) return world

    val fState = world.stateFor(faction)
    if (techType in fState.researchedTechs || techType in fState.researchingTechs) return world
    val techDef = GameDefinitions.techDefs.getValue(techType)
    if (!GameDefinitions.areTechPrerequisitesMet(world, faction, techType)) return world
    if (techDef.researchedAt != building.type) return world
    if (!fState.resources.canAfford(techDef.cost)) return world

    val item = ProductionItem.ResearchTech(techType = techType, totalTime = techDef.researchTimeSec)
    val updatedBuilding = building.copy(queue = building.queue + item)
    val updatedFaction =
      fState.copy(
        resources = fState.resources - techDef.cost,
        researchingTechs = fState.researchingTechs + techType,
      )

    return world
      .copy(buildings = world.buildings.map { if (it.id == buildingId) updatedBuilding else it })
      .withFactionState(faction, updatedFaction)
  }

  fun cancelQueueItem(world: GameWorld, faction: Faction, buildingId: Int, index: Int): GameWorld {
    val building = world.buildings.firstOrNull { it.id == buildingId && it.faction == faction && it.isComplete }
      ?: return world
    if (index !in building.queue.indices) return world

    val removed = building.queue[index]
    val fState = world.stateFor(faction)
    val updatedQueue = building.queue.toMutableList().apply { removeAt(index) }
    val updatedBuilding = building.copy(queue = updatedQueue)

    val updatedFaction =
      when (removed) {
        is ProductionItem.TrainUnit -> {
          val refund = GameDefinitions.adjustedUnitCost(removed.unitType, fState.civilization)
          fState.copy(
            resources =
              fState.resources.copy(
                wood = fState.resources.wood + refund.wood,
                food = fState.resources.food + refund.food,
                gold = fState.resources.gold + refund.gold,
                stone = fState.resources.stone + refund.stone,
              )
          )
        }
        is ProductionItem.ResearchTech -> {
          val refund = GameDefinitions.techDefs.getValue(removed.techType).cost
          fState.copy(
            resources =
              fState.resources.copy(
                wood = fState.resources.wood + refund.wood,
                food = fState.resources.food + refund.food,
                gold = fState.resources.gold + refund.gold,
                stone = fState.resources.stone + refund.stone,
              ),
            researchingTechs = fState.researchingTechs - removed.techType,
          )
        }
      }

    return world
      .copy(buildings = world.buildings.map { if (it.id == buildingId) updatedBuilding else it })
      .withFactionState(faction, updatedFaction)
  }

  fun orderMove(world: GameWorld, unitIds: List<Int>, targetX: Float, targetY: Float): GameWorld {
    val idSet = unitIds.toSet()
    val clampedX = targetX.coerceIn(1f, world.mapSize - 2f)
    val clampedY = targetY.coerceIn(1f, world.mapSize - 2f)
    val count = idSet.size
    var idx = 0
    val updatedUnits =
      world.units.map { u ->
        if (u.id in idSet) {
          val col = idx % 4
          val row = idx / 4
          val spread = if (count > 1) 0.55f else 0f
          val ox = (col - 1.5f) * spread
          val oy = (row - 0.5f) * spread
          idx++
          u.copy(
            order = UnitOrderType.MOVE,
            targetX = (clampedX + ox).coerceIn(1f, world.mapSize - 2f),
            targetY = (clampedY + oy).coerceIn(1f, world.mapSize - 2f),
            targetEntityId = -1,
            targetResourceId = -1,
            returningToDropOff = false,
            buildQueue = emptyList(),
          )
        } else u
      }
    val ping =
      VisualEffect(
        id = world.nextEntityId,
        type = VisualEffectType.MOVE_PING,
        x = clampedX,
        y = clampedY,
        durationSec = 0.6f,
      )
    return world.copy(
      units = updatedUnits,
      effects = world.effects + ping,
      nextEntityId = world.nextEntityId + 1,
    )
  }

  fun orderAttackMove(world: GameWorld, unitIds: List<Int>, targetX: Float, targetY: Float): GameWorld {
    val idSet = unitIds.toSet()
    val clampedX = targetX.coerceIn(1f, world.mapSize - 2f)
    val clampedY = targetY.coerceIn(1f, world.mapSize - 2f)
    val updatedUnits =
      world.units.map { u ->
        if (u.id in idSet) {
          u.copy(
            order = UnitOrderType.ATTACK_MOVE,
            targetX = clampedX,
            targetY = clampedY,
            targetEntityId = -1,
            targetResourceId = -1,
            returningToDropOff = false,
            buildQueue = emptyList(),
          )
        } else u
      }
    return world.copy(units = updatedUnits)
  }

  fun orderGatherNode(world: GameWorld, unitIds: List<Int>, resourceId: Int): GameWorld {
    val node = world.resources.firstOrNull { it.id == resourceId } ?: return world
    val idSet = unitIds.toSet()
    val updatedUnits =
      world.units.map { u ->
        if (u.id in idSet && u.type == UnitType.VILLAGER) {
          val carryKeep = u.carryingType == node.type.resourceType && u.carryingAmount > 0
          u.copy(
            order = UnitOrderType.GATHER,
            targetResourceId = resourceId,
            targetEntityId = -1,
            targetX = node.x,
            targetY = node.y,
            carryingType = if (carryKeep) u.carryingType else null,
            carryingAmount = if (carryKeep) u.carryingAmount else 0,
            returningToDropOff = false,
            buildQueue = emptyList(),
          )
        } else u
      }
    return world.copy(units = updatedUnits)
  }

  fun orderGatherFarm(world: GameWorld, unitIds: List<Int>, farmId: Int): GameWorld {
    val farm = world.buildings.firstOrNull { it.id == farmId && it.type == BuildingType.FARM } ?: return world
    val idSet = unitIds.toSet()
    val updatedUnits =
      world.units.map { u ->
        if (u.id in idSet && u.type == UnitType.VILLAGER) {
          u.copy(
            order = UnitOrderType.GATHER,
            targetEntityId = farmId,
            targetResourceId = -1,
            targetX = farm.centerX,
            targetY = farm.centerY,
            returningToDropOff = false,
            buildQueue = emptyList(),
          )
        } else u
      }
    return world.copy(units = updatedUnits)
  }

  fun orderBuild(world: GameWorld, unitIds: List<Int>, buildingId: Int): GameWorld {
    val b = world.buildings.firstOrNull { it.id == buildingId } ?: return world
    val idSet = unitIds.toSet()
    val updatedUnits =
      world.units.map { u ->
        if (u.id in idSet && u.type == UnitType.VILLAGER) {
          u.copy(
            order = UnitOrderType.BUILD,
            targetEntityId = buildingId,
            targetResourceId = -1,
            targetX = b.centerX,
            targetY = b.centerY,
            returningToDropOff = false,
            buildQueue = emptyList(),
          )
        } else u
      }
    return world.copy(units = updatedUnits)
  }

  fun enqueueBuild(world: GameWorld, unitIds: List<Int>, buildingId: Int): GameWorld {
    val b = world.buildings.firstOrNull { it.id == buildingId } ?: return world
    val idSet = unitIds.toSet()
    val buildingById = world.buildings.associateBy { it.id }
    val updatedUnits =
      world.units.map { u ->
        if (u.id in idSet && u.type == UnitType.VILLAGER) {
          val activeTarget = buildingById[u.targetEntityId]
          val hasActiveBuild =
            u.order == UnitOrderType.BUILD &&
              activeTarget != null &&
              activeTarget.faction == u.faction &&
              (!activeTarget.isComplete || activeTarget.hp < activeTarget.maxHp) &&
              activeTarget.id != buildingId
          if (hasActiveBuild) {
            if (buildingId in u.buildQueue) u
            else u.copy(buildQueue = u.buildQueue + buildingId)
          } else {
            u.copy(
              order = UnitOrderType.BUILD,
              targetEntityId = buildingId,
              targetResourceId = -1,
              targetX = b.centerX,
              targetY = b.centerY,
              returningToDropOff = false,
              buildQueue = emptyList(),
            )
          }
        } else u
      }
    return world.copy(units = updatedUnits)
  }

  fun orderAttackUnit(world: GameWorld, unitIds: List<Int>, targetUnitId: Int): GameWorld {
    val idSet = unitIds.toSet()
    val updatedUnits =
      world.units.map { u ->
        if (u.id in idSet) {
          u.copy(
            order = UnitOrderType.ATTACK_UNIT,
            targetEntityId = targetUnitId,
            targetResourceId = -1,
            returningToDropOff = false,
            buildQueue = emptyList(),
          )
        } else u
      }
    return world.copy(units = updatedUnits)
  }

  fun orderAttackBuilding(world: GameWorld, unitIds: List<Int>, targetBuildingId: Int): GameWorld {
    val idSet = unitIds.toSet()
    val updatedUnits =
      world.units.map { u ->
        if (u.id in idSet) {
          u.copy(
            order = UnitOrderType.ATTACK_BUILDING,
            targetEntityId = targetBuildingId,
            targetResourceId = -1,
            returningToDropOff = false,
            buildQueue = emptyList(),
          )
        } else u
      }
    return world.copy(units = updatedUnits)
  }

  fun stopUnits(world: GameWorld, unitIds: List<Int>): GameWorld {
    val idSet = unitIds.toSet()
    return world.copy(
      units =
        world.units.map { u ->
          if (u.id in idSet) {
            u.copy(
              order = UnitOrderType.IDLE,
              targetEntityId = -1,
              targetResourceId = -1,
              idleTimer = -4.5f,
              isMoving = false,
              buildQueue = emptyList(),
            )
          } else u
        }
    )
  }

  fun mostNeededResource(
    world: GameWorld,
    faction: Faction,
    restrictToTcRadius: Boolean = true,
  ): ResourceType {
    val fState = world.stateFor(faction)
    val res = fState.resources
    val popCur = world.units.count { it.faction == faction }
    val popCap = populationCap(world.buildings.filter { it.faction == faction })

    val hasFarmFood =
      world.buildings.any {
        it.faction == faction &&
          it.type == BuildingType.FARM &&
          it.isComplete &&
          (!restrictToTcRadius || isWithinTownCenterRadius(it.centerX, it.centerY, faction, world.buildings))
      }

    fun hasSource(type: ResourceType): Boolean {
      if (type == ResourceType.FOOD && hasFarmFood) return true
      return world.resources.any {
        it.type.resourceType == type &&
          it.remaining > 0 &&
          (!restrictToTcRadius || isWithinTownCenterRadius(it.x, it.y, faction, world.buildings))
      }
    }

    val candidates = ResourceType.entries.filter(::hasSource)
    if (candidates.isEmpty()) return ResourceType.FOOD

    // Count villagers of this faction currently gathering each resource type
    val nodeById = world.resources.associateBy { it.id }
    val workersByType = mutableMapOf(
      ResourceType.WOOD to 0,
      ResourceType.FOOD to 0,
      ResourceType.GOLD to 0,
      ResourceType.STONE to 0,
    )
    for (u in world.units) {
      if (u.faction == faction && u.type == UnitType.VILLAGER && u.order == UnitOrderType.GATHER) {
        val t =
          when {
            u.targetEntityId != -1 -> ResourceType.FOOD
            u.targetResourceId != -1 -> nodeById[u.targetResourceId]?.type?.resourceType
            else -> u.carryingType
          }
        if (t != null) workersByType[t] = (workersByType[t] ?: 0) + 1
      }
    }

    val targets =
      when (fState.age) {
        Age.DARK_AGE -> mapOf(
          ResourceType.FOOD to 500f,
          ResourceType.WOOD to 240f,
          ResourceType.GOLD to 90f,
          ResourceType.STONE to 40f,
        )
        Age.FEUDAL_AGE -> mapOf(
          ResourceType.FOOD to 750f,
          ResourceType.WOOD to 340f,
          ResourceType.GOLD to 260f,
          ResourceType.STONE to 140f,
        )
        Age.CASTLE_AGE, Age.IMPERIAL_AGE -> mapOf(
          ResourceType.FOOD to 900f,
          ResourceType.WOOD to 420f,
          ResourceType.GOLD to 680f,
          ResourceType.STONE to 450f,
        )
        Age.INDUSTRIAL_AGE -> mapOf(
          ResourceType.FOOD to 1050f,
          ResourceType.WOOD to 480f,
          ResourceType.GOLD to 900f,
          ResourceType.STONE to 450f,
        )
        Age.MODERN_AGE -> mapOf(
          ResourceType.FOOD to 1200f,
          ResourceType.WOOD to 550f,
          ResourceType.GOLD to 1150f,
          ResourceType.STONE to 500f,
        )
      }

    fun currentAmount(type: ResourceType): Int =
      when (type) {
        ResourceType.WOOD -> res.wood
        ResourceType.FOOD -> res.food
        ResourceType.GOLD -> res.gold
        ResourceType.STONE -> res.stone
      }

    return candidates.maxBy { type ->
      val target = targets.getValue(type)
      val cur = currentAmount(type).toFloat()
      val deficitScore = ((target - cur).coerceAtLeast(0f) / target) * 1.5f
      val urgencyBoost =
        when {
          type == ResourceType.FOOD && res.food < 60 -> 1.65f
          type == ResourceType.WOOD && popCur + 1 >= popCap && res.wood < 40 -> 1.55f
          type == ResourceType.GOLD && fState.age.index >= Age.FEUDAL_AGE.index && res.gold < 200 -> 0.5f
          else -> 0f
        }
      val workerPenalty = (workersByType[type] ?: 0) * 0.28f
      val basePriority =
        when (type) {
          ResourceType.FOOD -> 0.14f
          ResourceType.WOOD -> 0.12f
          ResourceType.GOLD -> 0.08f
          ResourceType.STONE -> 0.04f
        }
      deficitScore + urgencyBoost + basePriority - workerPenalty
    }
  }

  fun assignVillagerToMostNeededResource(world: GameWorld, villagerId: Int): GameWorld {
    val vil = world.units.firstOrNull { it.id == villagerId && it.type == UnitType.VILLAGER } ?: return world
    if (vil.carryingAmount > 0 && vil.carryingType != null) {
      val updated = world.units.map { u ->
        if (u.id == villagerId) u.copy(order = UnitOrderType.GATHER, returningToDropOff = true, idleTimer = 0f) else u
      }
      return world.copy(units = updated)
    }

    val desired = mostNeededResource(world, vil.faction, restrictToTcRadius = true)
    if (desired == ResourceType.FOOD) {
      val openFarm =
        world.buildings
          .filter { b ->
            b.faction == vil.faction &&
              b.type == BuildingType.FARM &&
              b.isComplete &&
              isWithinTownCenterRadius(b.centerX, b.centerY, vil.faction, world.buildings) &&
              world.units.none { other ->
                other.id != vil.id && other.order == UnitOrderType.GATHER && other.targetEntityId == b.id
              }
          }
          .minByOrNull { hypot(it.centerX - vil.x, it.centerY - vil.y) }
      if (openFarm != null) {
        return orderGatherFarm(world, listOf(vil.id), openFarm.id)
      }
    }

    val node =
      world.resources
        .filter {
          it.type.resourceType == desired &&
            it.remaining > 0 &&
            isWithinTownCenterRadius(it.x, it.y, vil.faction, world.buildings)
        }
        .minByOrNull { hypot(it.x - vil.x, it.y - vil.y) }
        ?: world.resources
          .filter {
            it.remaining > 0 &&
              isWithinTownCenterRadius(it.x, it.y, vil.faction, world.buildings)
          }
          .minByOrNull { hypot(it.x - vil.x, it.y - vil.y) }
        ?: return world
    return orderGatherNode(world, listOf(vil.id), node.id)
  }

  fun recomputeFog(world: GameWorld): GameWorld = RtsCombat.recomputeFog(world)

  fun step(world: GameWorld, dtSec: Float, config: SkirmishConfig): GameWorld {
    if (world.matchResult != null) return world
    val dt = dtSec.coerceIn(0f, 0.1f)
    var nextId = world.nextEntityId
    val floatingTexts = world.floatingTexts.mapNotNull {
      val nextAge = it.ageSec + dt
      if (nextAge < 1.3f) it.copy(ageSec = nextAge) else null
    }.toMutableList()

    val effects = world.effects.mapNotNull {
      val nextAge = it.ageSec + dt
      if (nextAge < it.durationSec) it.copy(ageSec = nextAge) else null
    }.toMutableList()

    val resourceMap = world.resources.associateBy { it.id }.toMutableMap()
    val buildingsMap = world.buildings.associateBy { it.id }.toMutableMap()
    val spawnedUnits = mutableListOf<Unit>()
    val stateMap = mutableMapOf<Faction, FactionState>().apply {
      for (f in Faction.entries) {
        this[f] = world.stateFor(f)
      }
    }

    // 1. Update Building Production, Research & Windmill anims
    for ((bId, b) in buildingsMap.toList()) {
      var updated = b.copy(animPhase = b.animPhase + dt * 1.4f, attackTimer = max(0f, b.attackTimer - dt))
      if (updated.isComplete && updated.queue.isNotEmpty()) {
        val head = updated.queue.first()
        val fState = stateMap.getValue(updated.faction)
        val rateMult =
          if (updated.faction.isAi) config.difficulty.aiTrainRate else 1f
        val newProgress = head.progress + dt * rateMult
        if (newProgress >= head.totalTime) {
          val remainingQueue = updated.queue.drop(1)
          updated = updated.copy(queue = remainingQueue)
          when (head) {
            is ProductionItem.TrainUnit -> {
              val currentPop = world.units.count { it.faction == updated.faction } + spawnedUnits.count { it.faction == updated.faction }
              val cap = populationCap(buildingsMap.values.filter { it.faction == updated.faction })
              if (currentPop < cap) {
                val uDef = GameDefinitions.unitDefs.getValue(head.unitType)
                val bonusHp = unitMaxHpFor(head.unitType, fState)
                val spawnPos = findSpawnSpot(world, buildingsMap.values, updated, isFlying = uDef.isFlying)
                spawnedUnits +=
                  Unit(
                    id = nextId++,
                    type = head.unitType,
                    faction = updated.faction,
                    x = spawnPos.first,
                    y = spawnPos.second,
                    hp = bonusHp,
                    maxHp = bonusHp,
                    facingRight = updated.faction == Faction.PLAYER,
                  )
                val prevSt = stateMap.getValue(updated.faction)
                stateMap[updated.faction] =
                  prevSt.copy(stats = prevSt.stats.copy(unitsTrained = prevSt.stats.unitsTrained + 1))
              }
            }
            is ProductionItem.ResearchTech -> {
              val applied = applyTechCompletion(fState, head.techType)
              stateMap[updated.faction] = applied
              if (updated.faction == Faction.PLAYER) {
                floatingTexts +=
                  FloatingText(
                    id = nextId++,
                    x = updated.centerX,
                    y = updated.centerY - 0.6f,
                    text = "${head.techType.displayName} Ready!",
                    colorHex = 0xFFF2C964,
                  )
              }
            }
          }
        } else {
          val updatedHead =
            when (head) {
              is ProductionItem.TrainUnit -> head.copy(progress = newProgress)
              is ProductionItem.ResearchTech -> head.copy(progress = newProgress)
            }
          updated = updated.copy(queue = listOf(updatedHead) + updated.queue.drop(1))
        }
      }
      buildingsMap[bId] = updated
    }

    // Sync existing building max HP bonuses from Masonry, Architecture & Stone Shaft Mining
    for ((bId, b) in buildingsMap.toList()) {
      val bState = stateMap.getValue(b.faction)
      val synced = syncBuildingMaxHpWithTechs(b, bState)
      if (synced !== b) buildingsMap[bId] = synced
    }

    // Apply upgraded unit types and newly completed Age/Tech HP bonuses cleanly
    val unitListInput =
      (world.units + spawnedUnits).map { u ->
        val fState = stateMap.getValue(u.faction)
        val upgraded = upgradeUnitIfResearched(u, fState)
        syncUnitMaxHpWithTechs(upgraded, fState)
      }

    // 2. Update Units Economy, Building & Movement
    val updatedUnits = mutableListOf<Unit>()
    for (unit in unitListInput) {
      var u = unit.copy(attackTimer = max(0f, unit.attackTimer - dt), isAttackingAnim = false, isMoving = false)
      val fState = stateMap.getValue(u.faction)
      val baseGatherMult = if (u.faction.isAi) config.difficulty.aiGatherMultiplier else 1f
      val carryCap = effectiveCarryCapacity(fState)
      val effectiveMoveSpeed = unitEffectiveSpeed(u.type, fState)

      if (u.order != UnitOrderType.IDLE && u.idleTimer != 0f) {
        u = u.copy(idleTimer = 0f)
      }

      when (u.order) {
        UnitOrderType.IDLE -> {
          if (u.type == UnitType.VILLAGER && u.faction == Faction.PLAYER) {
            val nextIdle = u.idleTimer + dt
            if (nextIdle >= 1.4f) {
              if (u.carryingAmount > 0 && u.carryingType != null) {
                u = u.copy(order = UnitOrderType.GATHER, returningToDropOff = true, idleTimer = 0f)
              } else {
                val snapshot =
                  world.copy(
                    resources = resourceMap.values.toList(),
                    buildings = buildingsMap.values.toList(),
                    units = updatedUnits + unitListInput.drop(updatedUnits.size),
                    playerState = stateMap.getValue(Faction.PLAYER),
                    enemyState = stateMap.getValue(Faction.ENEMY),
                  )
                val needed = mostNeededResource(snapshot, Faction.PLAYER, restrictToTcRadius = true)
                var assigned = false
                if (needed == ResourceType.FOOD) {
                  val openFarm =
                    buildingsMap.values
                      .filter { b ->
                        b.faction == Faction.PLAYER &&
                          b.type == BuildingType.FARM &&
                          b.isComplete &&
                          isWithinTownCenterRadius(b.centerX, b.centerY, Faction.PLAYER, buildingsMap.values) &&
                          (updatedUnits.none { other -> other.id != u.id && other.order == UnitOrderType.GATHER && other.targetEntityId == b.id })
                      }
                      .minByOrNull { hypot(it.centerX - u.x, it.centerY - u.y) }
                  if (openFarm != null) {
                    u =
                      u.copy(
                        order = UnitOrderType.GATHER,
                        targetEntityId = openFarm.id,
                        targetResourceId = -1,
                        targetX = openFarm.centerX,
                        targetY = openFarm.centerY,
                        idleTimer = 0f,
                      )
                    assigned = true
                  }
                }
                if (!assigned) {
                  val node =
                    resourceMap.values
                      .filter {
                        it.type.resourceType == needed &&
                          it.remaining > 0 &&
                          isWithinTownCenterRadius(it.x, it.y, Faction.PLAYER, buildingsMap.values)
                      }
                      .minByOrNull { hypot(it.x - u.x, it.y - u.y) }
                      ?: resourceMap.values
                        .filter {
                          it.remaining > 0 &&
                            isWithinTownCenterRadius(it.x, it.y, Faction.PLAYER, buildingsMap.values)
                        }
                        .minByOrNull { hypot(it.x - u.x, it.y - u.y) }
                  if (node != null) {
                    val keepCarry = u.carryingType == node.type.resourceType && u.carryingAmount > 0
                    u =
                      u.copy(
                        order = UnitOrderType.GATHER,
                        targetResourceId = node.id,
                        targetEntityId = -1,
                        targetX = node.x,
                        targetY = node.y,
                        carryingType = if (keepCarry) u.carryingType else null,
                        carryingAmount = if (keepCarry) u.carryingAmount else 0,
                        returningToDropOff = false,
                        idleTimer = 0f,
                      )
                    if (world.elapsedSec > 2.5f) {
                      floatingTexts +=
                        FloatingText(
                          id = nextId++,
                          x = u.x,
                          y = u.y - 0.3f,
                          text = "→ ${node.type.resourceType.displayName}",
                          colorHex = 0xFFF2C964,
                        )
                    }
                    assigned = true
                  }
                }
                if (!assigned) {
                  u = u.copy(idleTimer = 0.7f)
                }
              }
            } else {
              u = u.copy(idleTimer = nextIdle)
            }
          }
        }
        UnitOrderType.MOVE -> {
          val dist = hypot(u.targetX - u.x, u.targetY - u.y)
          if (dist < 0.25f) {
            u = u.copy(order = UnitOrderType.IDLE, isMoving = false)
          } else {
            u = moveUnitTowards(u, u.targetX, u.targetY, effectiveMoveSpeed, dt, world, buildingsMap.values)
          }
        }
        UnitOrderType.BUILD -> {
          val targetB = buildingsMap[u.targetEntityId]
          if (targetB == null) {
            u = advanceBuildQueueOrGather(u, null, buildingsMap, resourceMap, updatedUnits + unitListInput.drop(updatedUnits.size))
          } else {
            val dist = distanceToBuilding(u.x, u.y, targetB)
            if (dist > 1.15f) {
              val (ax, ay) = nearestApproachPoint(u.x, u.y, targetB, world, buildingsMap.values)
              u = moveUnitTowards(u, ax, ay, effectiveMoveSpeed, dt, world, buildingsMap.values, targetBuilding = targetB)
            } else {
              u = u.copy(isAttackingAnim = true, animTime = u.animTime + dt * 7f)
              val bDef = GameDefinitions.buildingDefs.getValue(targetB.type)
              if (!targetB.isComplete) {
                val buildSpeedMult = if (TechType.TREADMILL_CRANE in fState.researchedTechs) 1.35f else 1f
                val effBuildTime = (bDef.buildTimeSec / buildSpeedMult).coerceAtLeast(1f)
                val nextProg = min(1f, targetB.buildProgress + dt / effBuildTime)
                val healAmt = ((dt / effBuildTime) * targetB.maxHp).toInt().coerceAtLeast(1)
                val nowComplete = nextProg >= 1f
                val finishedBuilding =
                  targetB.copy(
                    buildProgress = nextProg,
                    isComplete = nowComplete,
                    hp = min(targetB.maxHp, targetB.hp + healAmt),
                  )
                buildingsMap[targetB.id] = finishedBuilding
                if (nowComplete) {
                  val prevSt = stateMap.getValue(targetB.faction)
                  stateMap[targetB.faction] =
                    prevSt.copy(stats = prevSt.stats.copy(buildingsConstructed = prevSt.stats.buildingsConstructed + 1))
                  u =
                    advanceBuildQueueOrGather(
                      u = u,
                      finishedBuilding = finishedBuilding,
                      buildingsMap = buildingsMap,
                      resourceMap = resourceMap,
                      allUnits = updatedUnits + unitListInput.drop(updatedUnits.size),
                    )
                }
              } else if (targetB.hp < targetB.maxHp) {
                val repairRate = if (TechType.MASONRY in fState.researchedTechs) 28f * 1.35f else 28f
                val repaired = targetB.copy(hp = min(targetB.maxHp, targetB.hp + (dt * repairRate).toInt().coerceAtLeast(1)))
                buildingsMap[targetB.id] = repaired
                if (repaired.hp >= repaired.maxHp) {
                  u =
                    advanceBuildQueueOrGather(
                      u = u,
                      finishedBuilding = repaired,
                      buildingsMap = buildingsMap,
                      resourceMap = resourceMap,
                      allUnits = updatedUnits + unitListInput.drop(updatedUnits.size),
                    )
                }
              } else {
                u =
                  advanceBuildQueueOrGather(
                    u = u,
                    finishedBuilding = targetB,
                    buildingsMap = buildingsMap,
                    resourceMap = resourceMap,
                    allUnits = updatedUnits + unitListInput.drop(updatedUnits.size),
                  )
              }
            }
          }
        }
        UnitOrderType.GATHER -> {
          if (u.returningToDropOff && u.carryingAmount > 0 && u.carryingType != null) {
            val dropOff = findNearestDropOff(u, u.carryingType!!, buildingsMap.values)
            if (dropOff == null) {
              u = u.copy(order = UnitOrderType.IDLE, returningToDropOff = false)
            } else {
              val dist = distanceToBuilding(u.x, u.y, dropOff)
              if (dist > 1.15f) {
                val (ax, ay) = nearestApproachPoint(u.x, u.y, dropOff, world, buildingsMap.values)
                u = moveUnitTowards(u, ax, ay, effectiveMoveSpeed, dt, world, buildingsMap.values, targetBuilding = dropOff)
              } else {
                val resType = u.carryingType!!
                val amt = u.carryingAmount
                stateMap[u.faction] = addGatheredResource(stateMap.getValue(u.faction), resType, amt)
                if (u.faction == Faction.PLAYER) {
                  val colorHex =
                    when (resType) {
                      ResourceType.WOOD -> 0xFFC58D54
                      ResourceType.FOOD -> 0xFFE86F62
                      ResourceType.GOLD -> 0xFFF2C964
                      ResourceType.STONE -> 0xFFC8D0D5
                    }
                  floatingTexts +=
                    FloatingText(
                      id = nextId++,
                      x = dropOff.centerX,
                      y = dropOff.centerY - 0.5f,
                      text = "+$amt ${resType.displayName}",
                      colorHex = colorHex,
                    )
                }
                u = u.copy(carryingAmount = 0, returningToDropOff = false)
              }
            }
          } else {
            // Gathering from Farm OR ResourceNode
            if (u.targetEntityId != -1) {
              val farm = buildingsMap[u.targetEntityId]
              if (farm == null || farm.type != BuildingType.FARM || !farm.isComplete) {
                u = u.copy(order = UnitOrderType.IDLE, targetEntityId = -1)
              } else {
                val dist = hypot(farm.centerX - u.x, farm.centerY - u.y)
                if (dist > 1.0f) {
                  u = moveUnitTowards(u, farm.centerX, farm.centerY, effectiveMoveSpeed, dt, world, buildingsMap.values)
                } else {
                  val farmGatherMult = baseGatherMult * gatherRateMultiplier(fState, ResourceType.FOOD)
                  u = u.copy(isAttackingAnim = true, animTime = u.animTime + dt * 5f, gatherTimer = u.gatherTimer + dt * farmGatherMult)
                  if (u.gatherTimer >= 0.72f) {
                    val harvested = 1
                    val newCarry = u.carryingAmount + harvested
                    u =
                      u.copy(
                        gatherTimer = 0f,
                        carryingType = ResourceType.FOOD,
                        carryingAmount = newCarry,
                        returningToDropOff = newCarry >= carryCap,
                      )
                  }
                }
              }
            } else {
              var node = resourceMap[u.targetResourceId]
              if (node == null || node.remaining <= 0) {
                val desired = u.carryingType
                node =
                  if (desired != null) {
                    resourceMap.values
                      .filter {
                        it.type.resourceType == desired &&
                          it.remaining > 0 &&
                          isWithinTownCenterRadius(it.x, it.y, u.faction, buildingsMap.values)
                      }
                      .minByOrNull { hypot(it.x - u.x, it.y - u.y) }
                  } else null
                if (node == null) {
                  u =
                    if (u.carryingAmount > 0) u.copy(returningToDropOff = true)
                    else u.copy(order = UnitOrderType.IDLE, targetResourceId = -1, carryingType = null)
                  updatedUnits += u
                  continue
                } else {
                  u = u.copy(targetResourceId = node.id)
                }
              }
              val dist = hypot(node.x - u.x, node.y - u.y)
              if (dist > 1.05f) {
                u = moveUnitTowards(u, node.x, node.y, effectiveMoveSpeed, dt, world, buildingsMap.values)
              } else {
                val nodeGatherMult = baseGatherMult * gatherRateMultiplier(fState, node.type.resourceType)
                u =
                  u.copy(
                    facingRight = node.x >= u.x,
                    isAttackingAnim = true,
                    animTime = u.animTime + dt * 6f,
                    gatherTimer = u.gatherTimer + dt * nodeGatherMult,
                  )
                if (u.gatherTimer >= 0.78f) {
                  val harvested = min(1, node.remaining)
                  val newCarry = u.carryingAmount + harvested
                  val rem = node.remaining - harvested
                  if (rem <= 0) resourceMap.remove(node.id)
                  else resourceMap[node.id] = node.copy(remaining = rem)
                  u =
                    u.copy(
                      gatherTimer = 0f,
                      carryingType = node.type.resourceType,
                      carryingAmount = newCarry,
                      returningToDropOff = newCarry >= carryCap || rem <= 0,
                    )
                }
              }
            }
          }
        }
        else -> Unit // Combat & Attack-Move handled in RtsCombat
      }
      updatedUnits += u
    }

    val nextStates =
      world.factionStates.keys.associateWith { stateMap.getValue(it) } +
        mapOf(
          Faction.PLAYER to stateMap.getValue(Faction.PLAYER),
          Faction.ENEMY to stateMap.getValue(Faction.ENEMY),
        )
    val midWorld =
      world.copy(
        resources = resourceMap.values.toList(),
        buildings = buildingsMap.values.toList(),
        units = updatedUnits,
        floatingTexts = floatingTexts,
        effects = effects,
        playerState = stateMap.getValue(Faction.PLAYER),
        enemyState = stateMap.getValue(Faction.ENEMY),
        factionStates = nextStates,
        elapsedSec = world.elapsedSec + dt,
        nextEntityId = nextId,
      )

    return RtsCombat.resolveCombatAndFog(midWorld, dt)
  }

  private fun addGatheredResource(state: FactionState, type: ResourceType, amount: Int): FactionState {
    val nextRes = state.resources.add(type, amount)
    val s = state.stats
    val nextStats =
      when (type) {
        ResourceType.WOOD -> s.copy(totalWoodGathered = s.totalWoodGathered + amount)
        ResourceType.FOOD -> s.copy(totalFoodGathered = s.totalFoodGathered + amount)
        ResourceType.GOLD -> s.copy(totalGoldGathered = s.totalGoldGathered + amount)
        ResourceType.STONE -> s.copy(totalStoneGathered = s.totalStoneGathered + amount)
      }
    return state.copy(resources = nextRes, stats = nextStats)
  }

  private fun applyTechCompletion(state: FactionState, techType: TechType): FactionState {
    val nextResearched = state.researchedTechs + techType
    val nextResearching = state.researchingTechs - techType
    val nextAge =
      when (techType) {
        TechType.ADVANCE_FEUDAL -> Age.FEUDAL_AGE
        TechType.ADVANCE_CASTLE -> Age.CASTLE_AGE
        TechType.ADVANCE_IMPERIAL -> Age.IMPERIAL_AGE
        TechType.ADVANCE_INDUSTRIAL -> Age.INDUSTRIAL_AGE
        TechType.ADVANCE_MODERN -> Age.MODERN_AGE
        else -> state.age
      }
    return state.copy(
      age = nextAge,
      researchedTechs = nextResearched,
      researchingTechs = nextResearching,
    )
  }

  private fun syncUnitMaxHpWithTechs(u: Unit, fState: FactionState): Unit {
    val targetMax = unitMaxHpFor(u.type, fState)
    if (targetMax <= u.maxHp) return u
    val delta = targetMax - u.maxHp
    return u.copy(maxHp = targetMax, hp = min(targetMax, u.hp + delta))
  }

  private fun syncBuildingMaxHpWithTechs(b: Building, fState: FactionState): Building {
    val targetMax = GameDefinitions.effectiveBuildingMaxHp(b.type, fState)
    if (targetMax <= b.maxHp) return b
    val delta = targetMax - b.maxHp
    return b.copy(maxHp = targetMax, hp = min(targetMax, b.hp + delta))
  }

  private fun upgradeUnitIfResearched(u: Unit, fState: FactionState): Unit {
    val newType =
      when {
        u.type == UnitType.MILITIA && TechType.UPGRADE_MAN_AT_ARMS in fState.researchedTechs ->
          UnitType.MAN_AT_ARMS
        u.type == UnitType.SPEARMAN && TechType.UPGRADE_PIKEMAN in fState.researchedTechs ->
          UnitType.PIKEMAN
        u.type == UnitType.ARCHER && TechType.UPGRADE_CROSSBOW in fState.researchedTechs ->
          UnitType.CROSSBOWMAN
        else -> return u
      }
    val oldMax = unitMaxHpFor(u.type, fState)
    val newMax = unitMaxHpFor(newType, fState)
    return u.copy(type = newType, maxHp = newMax, hp = min(newMax, u.hp + (newMax - oldMax).coerceAtLeast(0)))
  }

  private fun findNearestDropOff(u: Unit, type: ResourceType, buildings: Collection<Building>): Building? {
    return buildings
      .filter { b ->
        b.faction == u.faction &&
          b.isComplete &&
          when (type) {
            ResourceType.WOOD -> GameDefinitions.buildingDefs.getValue(b.type).isDropOffWood
            ResourceType.FOOD -> GameDefinitions.buildingDefs.getValue(b.type).isDropOffFood
            ResourceType.GOLD -> GameDefinitions.buildingDefs.getValue(b.type).isDropOffGold
            ResourceType.STONE -> GameDefinitions.buildingDefs.getValue(b.type).isDropOffStone
          }
      }
      .minByOrNull { distanceToBuilding(u.x, u.y, it) }
  }

  fun distanceToBuilding(ux: Float, uy: Float, b: Building): Float {
    val nx = ux.coerceIn(b.tileX.toFloat(), (b.tileX + b.footprint).toFloat())
    val ny = uy.coerceIn(b.tileY.toFloat(), (b.tileY + b.footprint).toFloat())
    return hypot(ux - nx, uy - ny)
  }

  private fun advanceBuildQueueOrGather(
    u: Unit,
    finishedBuilding: Building?,
    buildingsMap: Map<Int, Building>,
    resourceMap: Map<Int, ResourceNode>,
    allUnits: Collection<Unit>,
  ): Unit {
    val nextIdx =
      u.buildQueue.indexOfFirst { bid ->
        val nb = buildingsMap[bid]
        nb != null && nb.faction == u.faction && (!nb.isComplete || nb.hp < nb.maxHp)
      }
    if (nextIdx != -1) {
      val nextId = u.buildQueue[nextIdx]
      val nb = buildingsMap.getValue(nextId)
      return u.copy(
        order = UnitOrderType.BUILD,
        targetEntityId = nextId,
        targetResourceId = -1,
        targetX = nb.centerX,
        targetY = nb.centerY,
        returningToDropOff = false,
        buildQueue = u.buildQueue.drop(nextIdx + 1),
        isAttackingAnim = false,
      )
    }
    if (finishedBuilding != null && finishedBuilding.type == BuildingType.FARM) {
      return u.copy(
        order = UnitOrderType.GATHER,
        targetEntityId = finishedBuilding.id,
        targetResourceId = -1,
        targetX = finishedBuilding.centerX,
        targetY = finishedBuilding.centerY,
        returningToDropOff = false,
        buildQueue = emptyList(),
        isAttackingAnim = false,
      )
    }
    return resumeNearbyGatheringAfterBuild(
      u = u,
      finishedBuilding = finishedBuilding,
      buildingsMap = buildingsMap,
      resourceMap = resourceMap,
      allUnits = allUnits,
    )
  }

  private fun resumeNearbyGatheringAfterBuild(
    u: Unit,
    finishedBuilding: Building?,
    buildingsMap: Map<Int, Building>,
    resourceMap: Map<Int, ResourceNode>,
    allUnits: Collection<Unit>,
  ): Unit {
    val inRangeNodes =
      resourceMap.values.filter {
        it.remaining > 0 && isWithinTownCenterRadius(it.x, it.y, u.faction, buildingsMap.values)
      }
    val openFarms =
      buildingsMap.values.filter { b ->
        b.faction == u.faction &&
          b.type == BuildingType.FARM &&
          b.isComplete &&
          isWithinTownCenterRadius(b.centerX, b.centerY, u.faction, buildingsMap.values) &&
          allUnits.none { other -> other.id != u.id && other.order == UnitOrderType.GATHER && other.targetEntityId == b.id }
      }

    // Prefer resources matching newly completed drop-off structures if nearby
    val specialtyNode =
      when (finishedBuilding?.type) {
        BuildingType.LUMBER_CAMP ->
          inRangeNodes
            .filter { it.type.resourceType == ResourceType.WOOD && hypot(it.x - finishedBuilding.centerX, it.y - finishedBuilding.centerY) <= 10f }
            .minByOrNull { hypot(it.x - u.x, it.y - u.y) }
        BuildingType.MINING_CAMP ->
          inRangeNodes
            .filter { (it.type.resourceType == ResourceType.GOLD || it.type.resourceType == ResourceType.STONE) && hypot(it.x - finishedBuilding.centerX, it.y - finishedBuilding.centerY) <= 10f }
            .minByOrNull { hypot(it.x - u.x, it.y - u.y) }
        BuildingType.MILL ->
          inRangeNodes
            .filter { it.type.resourceType == ResourceType.FOOD && hypot(it.x - finishedBuilding.centerX, it.y - finishedBuilding.centerY) <= 10f }
            .minByOrNull { hypot(it.x - u.x, it.y - u.y) }
        else -> null
      }

    if (specialtyNode != null) {
      val keepCarry = u.carryingType == specialtyNode.type.resourceType && u.carryingAmount > 0
      return u.copy(
        order = UnitOrderType.GATHER,
        targetResourceId = specialtyNode.id,
        targetEntityId = -1,
        targetX = specialtyNode.x,
        targetY = specialtyNode.y,
        carryingType = if (keepCarry) u.carryingType else null,
        carryingAmount = if (keepCarry) u.carryingAmount else 0,
        returningToDropOff = false,
        buildQueue = emptyList(),
        isAttackingAnim = false,
      )
    }

    val nearestNode = inRangeNodes.minByOrNull { hypot(it.x - u.x, it.y - u.y) }
    val nearestFarm = openFarms.minByOrNull { hypot(it.centerX - u.x, it.centerY - u.y) }

    val farmDist = nearestFarm?.let { hypot(it.centerX - u.x, it.centerY - u.y) } ?: Float.POSITIVE_INFINITY
    val nodeDist = nearestNode?.let { hypot(it.x - u.x, it.y - u.y) } ?: Float.POSITIVE_INFINITY

    if (nearestFarm != null && farmDist < nodeDist) {
      return u.copy(
        order = UnitOrderType.GATHER,
        targetEntityId = nearestFarm.id,
        targetResourceId = -1,
        targetX = nearestFarm.centerX,
        targetY = nearestFarm.centerY,
        returningToDropOff = false,
        buildQueue = emptyList(),
        isAttackingAnim = false,
      )
    }
    if (nearestNode != null) {
      val keepCarry = u.carryingType == nearestNode.type.resourceType && u.carryingAmount > 0
      return u.copy(
        order = UnitOrderType.GATHER,
        targetResourceId = nearestNode.id,
        targetEntityId = -1,
        targetX = nearestNode.x,
        targetY = nearestNode.y,
        carryingType = if (keepCarry) u.carryingType else null,
        carryingAmount = if (keepCarry) u.carryingAmount else 0,
        returningToDropOff = false,
        buildQueue = emptyList(),
        isAttackingAnim = false,
      )
    }
    return u.copy(
      order = UnitOrderType.IDLE,
      targetEntityId = -1,
      targetResourceId = -1,
      returningToDropOff = false,
      buildQueue = emptyList(),
      isAttackingAnim = false,
    )
  }

  fun nearestApproachPoint(ux: Float, uy: Float, b: Building): Pair<Float, Float> {
    val margin = 0.30f
    val minX = b.tileX - margin
    val maxX = b.tileX + b.footprint + margin
    val minY = b.tileY - margin
    val maxY = b.tileY + b.footprint + margin
    if (ux in minX..maxX && uy in minY..maxY) {
      // Snap out to nearest perimeter edge if inside footprint
      val dLeft = ux - minX
      val dRight = maxX - ux
      val dTop = uy - minY
      val dBot = maxY - uy
      val m = min(min(dLeft, dRight), min(dTop, dBot))
      return when (m) {
        dLeft -> minX to uy
        dRight -> maxX to uy
        dTop -> ux to minY
        else -> ux to maxY
      }
    }
    return ux.coerceIn(minX, maxX) to uy.coerceIn(minY, maxY)
  }

  fun nearestApproachPoint(
    ux: Float,
    uy: Float,
    b: Building,
    world: GameWorld,
    buildings: Collection<Building>,
  ): Pair<Float, Float> {
    val raw = nearestApproachPoint(ux, uy, b)
    if (isWalkable(raw.first, raw.second, world, buildings)) return raw

    val margin = 0.34f
    val minX = (b.tileX - margin).coerceIn(0.6f, world.mapSize - 1.6f)
    val maxX = (b.tileX + b.footprint + margin).coerceIn(0.6f, world.mapSize - 1.6f)
    val minY = (b.tileY - margin).coerceIn(0.6f, world.mapSize - 1.6f)
    val maxY = (b.tileY + b.footprint + margin).coerceIn(0.6f, world.mapSize - 1.6f)

    var best: Pair<Float, Float>? = null
    var bestDist = Float.POSITIVE_INFINITY
    val steps = (b.footprint * 2) + 2
    for (i in 0..steps) {
      val t = i.toFloat() / steps
      val sx = minX + (maxX - minX) * t
      val sy = minY + (maxY - minY) * t
      val edgePoints = listOf(sx to minY, sx to maxY, minX to sy, maxX to sy)
      for ((px, py) in edgePoints) {
        if (isWalkable(px, py, world, buildings)) {
          val d = hypot(px - ux, py - uy)
          if (d < bestDist) {
            bestDist = d
            best = px to py
          }
        }
      }
    }
    return best ?: raw
  }

  fun moveUnitTowards(
    u: Unit,
    tx: Float,
    ty: Float,
    speed: Float,
    dt: Float,
    world: GameWorld,
    buildings: Collection<Building>,
    targetBuilding: Building? = null,
  ): Unit {
    val dx = tx - u.x
    val dy = ty - u.y
    val dist = hypot(dx, dy)
    if (dist < 0.001f) return u

    // Flying aircraft glide unobstructed in a straight trajectory above water, cliffs, and structures
    if (GameDefinitions.unitDefs.getValue(u.type).isFlying) {
      val step = min(dist, speed * dt)
      val nx = (u.x + (dx / dist) * step).coerceIn(0.6f, world.mapSize - 1.6f)
      val ny = (u.y + (dy / dist) * step).coerceIn(0.6f, world.mapSize - 1.6f)
      val isoFaceRight = if (hypot(nx - u.x, ny - u.y) > 0.001f) ((nx - u.x) - (ny - u.y)) >= 0f else u.facingRight
      return u.copy(
        x = nx,
        y = ny,
        facingRight = isoFaceRight,
        isMoving = true,
        animTime = u.animTime + dt * 8f,
      )
    }

    // If a building overlaps or grazes this unit, smoothly push outward onto a walkable tile first.
    if (!isWalkable(u.x, u.y, world, buildings)) {
      val blockingB =
        buildings.firstOrNull { b ->
          val def = GameDefinitions.buildingDefs.getValue(b.type)
          !def.isWalkable &&
            u.x >= b.tileX - 0.04f &&
            u.x <= b.tileX + b.footprint + 0.04f &&
            u.y >= b.tileY - 0.04f &&
            u.y <= b.tileY + b.footprint + 0.04f
        }
      if (blockingB != null) {
        val (ex, ey) = nearestApproachPoint(u.x, u.y, blockingB, world, buildings)
        val edx = ex - u.x
        val edy = ey - u.y
        val eDist = hypot(edx, edy).coerceAtLeast(0.001f)
        val step = speed * dt
        val nx = (u.x + (edx / eDist) * step).coerceIn(0.6f, world.mapSize - 1.6f)
        val ny = (u.y + (edy / eDist) * step).coerceIn(0.6f, world.mapSize - 1.6f)
        return u.copy(
          x = nx,
          y = ny,
          facingRight = (nx - u.x - (ny - u.y)) >= 0f,
          isMoving = true,
          animTime = u.animTime + dt * 8f,
        )
      }
    }

    val hasDirectSight = Pathfinder.hasClearLine(u.x, u.y, tx, ty, world, buildings, ignoreBuildingId = -1)
    var steerTarget =
      if (hasDirectSight) {
        tx to ty
      } else {
        Pathfinder.nextWaypoint(u.x, u.y, tx, ty, world, buildings, targetBuilding = targetBuilding) ?: (tx to ty)
      }

    var sdx = steerTarget.first - u.x
    var sdy = steerTarget.second - u.y
    var sDist = hypot(sdx, sdy)
    if (sDist < 0.001f) return u

    var step = min(if (hasDirectSight) dist else sDist, speed * dt)
    var baseUx = sdx / sDist
    var baseUy = sdy / sDist

    var (finalX, finalY) = findBestStep(u.x, u.y, baseUx, baseUy, step, world, buildings)
    var moved = hypot(finalX - u.x, finalY - u.y) > 0.001f

    // Stuck recovery: force full A* without targetBuilding short-circuit if direct steering stalled against a wall/corner
    if (!moved) {
      val fallbackWp = Pathfinder.nextWaypoint(u.x, u.y, tx, ty, world, buildings, targetBuilding = null)
      if (fallbackWp != null) {
        steerTarget = fallbackWp
        sdx = steerTarget.first - u.x
        sdy = steerTarget.second - u.y
        sDist = hypot(sdx, sdy)
        if (sDist >= 0.001f) {
          step = min(sDist, speed * dt)
          baseUx = sdx / sDist
          baseUy = sdy / sDist
          val retry = findBestStep(u.x, u.y, baseUx, baseUy, step, world, buildings)
          finalX = retry.first
          finalY = retry.second
          moved = hypot(finalX - u.x, finalY - u.y) > 0.001f
        }
      }
    }

    val isoFaceRight = if (moved) ((finalX - u.x) - (finalY - u.y)) >= 0f else u.facingRight

    return u.copy(
      x = finalX,
      y = finalY,
      facingRight = isoFaceRight,
      isMoving = moved,
      animTime = u.animTime + dt * 8f,
    )
  }

  private fun findBestStep(
    ux: Float,
    uy: Float,
    dirX: Float,
    dirY: Float,
    step: Float,
    world: GameWorld,
    buildings: Collection<Building>,
  ): Pair<Float, Float> {
    // Progressive angular deflections covering full wall-sliding and corner-rounding
    val angles = floatArrayOf(0f, 0.42f, -0.42f, 0.85f, -0.85f, 1.25f, -1.25f, 1.60f, -1.60f, 2.05f, -2.05f, 2.55f, -2.55f)
    for (rad in angles) {
      val c = kotlin.math.cos(rad)
      val s = kotlin.math.sin(rad)
      val rx = dirX * c - dirY * s
      val ry = dirX * s + dirY * c
      val cx = (ux + rx * step).coerceIn(0.6f, world.mapSize - 1.6f)
      val cy = (uy + ry * step).coerceIn(0.6f, world.mapSize - 1.6f)
      if (hypot(cx - ux, cy - uy) > 0.001f && isWalkable(cx, cy, world, buildings)) {
        return cx to cy
      }
    }
    // Axis-slide fallback (only accept genuine non-zero displacement)
    val ax = (ux + dirX * step).coerceIn(0.6f, world.mapSize - 1.6f)
    if (kotlin.math.abs(ax - ux) > 0.001f && isWalkable(ax, uy, world, buildings)) return ax to uy
    val ay = (uy + dirY * step).coerceIn(0.6f, world.mapSize - 1.6f)
    if (kotlin.math.abs(ay - uy) > 0.001f && isWalkable(ux, ay, world, buildings)) return ux to ay
    return ux to uy
  }

  private fun isWalkable(
    x: Float,
    y: Float,
    world: GameWorld,
    buildings: Collection<Building>,
  ): Boolean {
    val tx = x.toInt()
    val ty = y.toInt()
    val tile = world.tileAt(tx, ty)
    if (tile == TileTerrain.WATER || tile == TileTerrain.CLIFF) return false
    for (b in buildings) {
      val def = GameDefinitions.buildingDefs.getValue(b.type)
      if (!def.isWalkable &&
        x >= b.tileX - 0.02f &&
        x <= b.tileX + b.footprint + 0.02f &&
        y >= b.tileY - 0.02f &&
        y <= b.tileY + b.footprint + 0.02f
      ) {
        return false
      }
    }
    return true
  }

  private fun findSpawnSpot(
    world: GameWorld,
    buildings: Collection<Building>,
    b: Building,
    isFlying: Boolean = false,
  ): Pair<Float, Float> {
    if (isFlying) {
      val rx = b.rallyX.coerceIn(1f, world.mapSize - 2f)
      val ry = b.rallyY.coerceIn(1f, world.mapSize - 2f)
      return rx to ry
    }
    val jitter = ((world.units.size % 5) - 2) * 0.25f
    val candidates =
      listOf(
        (b.rallyX + jitter) to b.rallyY,
        b.rallyX to b.rallyY,
        (b.centerX + jitter) to (b.tileY + b.footprint + 0.65f),
        (b.tileX - 0.65f) to (b.centerY + jitter),
        (b.tileX + b.footprint + 0.65f) to (b.centerY + jitter),
        (b.centerX + jitter) to (b.tileY - 0.65f),
        (b.tileX - 0.65f) to (b.tileY + b.footprint + 0.5f),
        (b.tileX + b.footprint + 0.65f) to (b.tileY + b.footprint + 0.5f),
      )
    for ((cx, cy) in candidates) {
      val wx = cx.coerceIn(1f, world.mapSize - 2f)
      val wy = cy.coerceIn(1f, world.mapSize - 2f)
      if (isWalkable(wx, wy, world, buildings)) return wx to wy
    }
    return b.centerX.coerceIn(1f, world.mapSize - 2f) to (b.tileY + b.footprint + 0.65f).coerceIn(1f, world.mapSize - 2f)
  }
}
