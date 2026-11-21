package com.example.game.engine

import com.example.game.model.Age
import com.example.game.model.Building
import com.example.game.model.BuildingType
import com.example.game.model.Faction
import com.example.game.model.FactionState
import com.example.game.model.GameDefinitions
import com.example.game.model.GameWorld
import com.example.game.model.MapType
import com.example.game.model.ResourceNode
import com.example.game.model.ResourceNodeType
import com.example.game.model.SkirmishConfig
import com.example.game.model.TileTerrain
import com.example.game.model.Unit
import com.example.game.model.UnitType
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

object MapGenerator {

  const val MAP_SIZE = 76

  fun createWorld(config: SkirmishConfig, seed: Long = System.currentTimeMillis()): GameWorld {
    val mapSize = config.mapSize.tiles.coerceAtLeast(44)
    val mid = (mapSize - 1) * 0.5f
    val rng = Random(seed)
    val tiles = MutableList(mapSize * mapSize) {
      if (rng.nextFloat() < 0.28f) TileTerrain.GRASS_ALT else TileTerrain.GRASS
    }
    val occupied = BooleanArray(mapSize * mapSize)
    val resources = mutableListOf<ResourceNode>()
    var nextId = 100

    val activeAis = config.activeAiFactions()
    // 4 quadrant bases cleanly spaced from edges
    val playerTcX = 7
    val playerTcY = mapSize - 11
    val aiBaseCoords: Map<Faction, Pair<Int, Int>> =
      mapOf(
        Faction.ENEMY to ((mapSize - 11) to 7),
        Faction.ENEMY_2 to (7 to 7),
        Faction.ENEMY_3 to ((mapSize - 11) to (mapSize - 11)),
      ).filterKeys { it in activeAis }

    val allBases: List<Pair<Faction, Pair<Int, Int>>> =
      listOf(Faction.PLAYER to (playerTcX to playerTcY)) +
        activeAis.map { it to aiBaseCoords.getValue(it) }

    // Terrain features scaled proportionally to mapSize
    when (config.mapType) {
      MapType.COASTAL -> {
        // Winding diagonal river from NW toward SE with 4 wide stone bridges/fords so all 4 quadrants connect
        val bridge1 = (mapSize * 0.25f) to (mapSize * 0.74f)
        val bridge2 = (mapSize * 0.74f) to (mapSize * 0.25f)
        val centerFord = mid to mid
        for (y in 0 until mapSize) {
          for (x in 0 until mapSize) {
            val diag = (x + y).toFloat()
            val wobble = kotlin.math.sin(x * 0.20f) * 2.2f
            val distRiver = abs(diag - (mapSize - 1) + wobble)
            if (distRiver < 2.6f) {
              val nearB1 = hypot(x - bridge1.first, y - bridge1.second) < 3.4f
              val nearB2 = hypot(x - bridge2.first, y - bridge2.second) < 3.4f
              val nearCenter = hypot(x - centerFord.first, y - centerFord.second) < 3.2f
              if (nearB1 || nearB2 || nearCenter) {
                tiles[y * mapSize + x] = TileTerrain.DIRT_ROAD
              } else {
                tiles[y * mapSize + x] = TileTerrain.WATER
                occupied[y * mapSize + x] = true
              }
            } else if (distRiver < 3.8f && rng.nextFloat() < 0.45f) {
              tiles[y * mapSize + x] = TileTerrain.DIRT_ROAD
            }
          }
        }
        // Keep base plazas dry in case NW/SE AI corners brush near diagonal
        for ((_, pos) in allBases) {
          val (bx, by) = pos
          for (dy in -4..6) {
            for (dx in -4..6) {
              val tx = bx + dx
              val ty = by + dy
              if (tx in 0 until mapSize && ty in 0 until mapSize && tiles[ty * mapSize + tx] == TileTerrain.WATER) {
                tiles[ty * mapSize + tx] = TileTerrain.GRASS
                occupied[ty * mapSize + tx] = false
              }
            }
          }
        }
      }
      MapType.BLACK_FOREST -> {
        // Woodland ponds nestled in outer flanks (away from base TCs)
        val pondCenters =
          listOf(
            (mapSize * 0.27f).toInt() to (mapSize * 0.48f).toInt(),
            (mapSize * 0.72f).toInt() to (mapSize * 0.52f).toInt(),
            (mapSize * 0.48f).toInt() to (mapSize * 0.24f).toInt(),
            (mapSize * 0.52f).toInt() to (mapSize * 0.76f).toInt(),
          )
        for ((px, py) in pondCenters) {
          for (dy in -2..2) {
            for (dx in -2..2) {
              val tx = px + dx
              val ty = py + dy
              if (tx in 2 until mapSize - 2 && ty in 2 until mapSize - 2 && abs(dx) + abs(dy) <= 2) {
                tiles[ty * mapSize + tx] = TileTerrain.WATER
                occupied[ty * mapSize + tx] = true
              }
            }
          }
        }
        // Cross-shaped strategic ancient forest avenues with 4 clear woodland passes + center clearing
        val passPoints =
          listOf(
            (mapSize * 0.24f) to (mapSize * 0.76f),
            (mapSize * 0.76f) to (mapSize * 0.24f),
            (mapSize * 0.24f) to (mapSize * 0.24f),
            (mapSize * 0.76f) to (mapSize * 0.76f),
            mid to mid,
          )
        for (y in 2 until mapSize - 2) {
          for (x in 2 until mapSize - 2) {
            val diagMain = abs((x + y) - (mapSize - 1).toFloat())
            val diagCross = abs(x - y.toFloat())
            val nearAnyBase = allBases.any { (_, b) -> hypot((x - b.first - 1).toFloat(), (y - b.second - 1).toFloat()) < 10.5f }
            val inForestBelt = (diagMain < 4.6f || (activeAis.size > 1 && diagCross < 3.6f)) && !nearAnyBase
            if (inForestBelt) {
              val inPass = passPoints.any { (px, py) -> hypot(x - px, y - py) < 3.6f }
              if (inPass) {
                if (rng.nextFloat() < 0.75f) tiles[y * mapSize + x] = TileTerrain.DIRT_ROAD
              } else if (rng.nextFloat() < 0.72f) {
                val type =
                  if (rng.nextBoolean()) ResourceNodeType.FOREST_PINE else ResourceNodeType.FOREST_OAK
                resources +=
                  ResourceNode(
                    id = nextId++,
                    type = type,
                    tileX = x,
                    tileY = y,
                    remaining = 110,
                    maxAmount = 110,
                    variant = rng.nextInt(3),
                  )
                occupied[y * mapSize + x] = true
              }
            }
          }
        }
      }
      MapType.ARABIA -> {
        // Scaled rocky cliff ridges + oasis waterhole & golden caravan crossroads
        val ridgeCount = (mapSize / 4.5f).toInt()
        for (i in 0 until ridgeCount) {
          val cx = rng.nextInt(11, mapSize - 11)
          val cy = rng.nextInt(11, mapSize - 11)
          val farFromBases = allBases.all { (_, b) -> hypot((cx - b.first).toFloat(), (cy - b.second).toFloat()) > 11f }
          if (farFromBases && hypot(cx - mid, cy - mid) > 7f) {
            val horiz = i % 2 == 0
            val len = rng.nextInt(2, 5)
            for (step in 0 until len) {
              val tx = cx + if (horiz) step else 0
              val ty = cy + if (horiz) 0 else step
              if (tx in 6 until mapSize - 6 && ty in 6 until mapSize - 6) {
                tiles[ty * mapSize + tx] = TileTerrain.CLIFF
                occupied[ty * mapSize + tx] = true
              }
            }
          }
        }
        // Steppe caravan trails converging at central crossroads
        for ((_, base) in allBases) {
          val steps = (mapSize * 0.55f).toInt()
          for (t in 0..steps) {
            val f = t / steps.toFloat()
            val tx = (base.first + 1 + (mid - base.first - 1) * f + kotlin.math.sin(f * 6f) * 1.6f)
              .toInt().coerceIn(2, mapSize - 3)
            val ty = (base.second + 1 + (mid - base.second - 1) * f + kotlin.math.cos(f * 5f) * 1.6f)
              .toInt().coerceIn(2, mapSize - 3)
            if (tiles[ty * mapSize + tx] != TileTerrain.CLIFF) {
              tiles[ty * mapSize + tx] = TileTerrain.DIRT_ROAD
            }
          }
        }
      }
    }

    // Paint dirt road plazas around all Town Centers
    fun paintRoadAround(tcX: Int, tcY: Int) {
      for (dy in -1..3) {
        for (dx in -1..3) {
          val tx = tcX + dx
          val ty = tcY + dy
          if (tx in 0 until mapSize && ty in 0 until mapSize &&
            tiles[ty * mapSize + tx] != TileTerrain.WATER &&
            tiles[ty * mapSize + tx] != TileTerrain.CLIFF
          ) {
            tiles[ty * mapSize + tx] = TileTerrain.DIRT_ROAD
          }
        }
      }
    }
    for ((_, pos) in allBases) {
      paintRoadAround(pos.first, pos.second)
    }

    fun reserveRect(rx: Int, ry: Int, size: Int) {
      for (dy in 0 until size) {
        for (dx in 0 until size) {
          val tx = rx + dx
          val ty = ry + dy
          if (tx in 0 until mapSize && ty in 0 until mapSize) {
            occupied[ty * mapSize + tx] = true
          }
        }
      }
    }

    fun placeResource(type: ResourceNodeType, tx: Int, ty: Int, amount: Int) {
      if (tx !in 1 until mapSize - 1 || ty !in 1 until mapSize - 1) return
      val idx = ty * mapSize + tx
      if (occupied[idx] || tiles[idx] == TileTerrain.WATER || tiles[idx] == TileTerrain.CLIFF) return
      occupied[idx] = true
      resources +=
        ResourceNode(
          id = nextId++,
          type = type,
          tileX = tx,
          tileY = ty,
          remaining = amount,
          maxAmount = amount,
          variant = rng.nextInt(3),
        )
    }

    fun placeCluster(type: ResourceNodeType, cx: Int, cy: Int, count: Int, amount: Int, spread: Int) {
      var placed = 0
      var attempts = 0
      while (placed < count && attempts < 48) {
        attempts++
        val tx = (cx + rng.nextInt(-spread, spread + 1)).coerceIn(1, mapSize - 2)
        val ty = (cy + rng.nextInt(-spread, spread + 1)).coerceIn(1, mapSize - 2)
        val idx = ty * mapSize + tx
        if (!occupied[idx] && tiles[idx] != TileTerrain.WATER && tiles[idx] != TileTerrain.CLIFF) {
          placeResource(type, tx, ty, amount)
          placed++
        }
      }
    }

    // Reserve TC + immediate yard for all active factions
    for ((_, pos) in allBases) {
      reserveRect(pos.first - 1, pos.second - 1, 5)
    }

    // Starting resources around EVERY active base (Player SW placed FIRST to preserve berry proximity)
    for ((faction, pos) in allBases) {
      val (bx, by) = pos
      val xSign = if (bx < mid) 1 else -1
      val ySign = if (by < mid) 1 else -1
      placeCluster(ResourceNodeType.BERRY_BUSH, bx - 3 * xSign, by + 1, 5, 140, 2)
      placeCluster(ResourceNodeType.GOLD_MINE, bx + 5 * xSign, by - 2 * ySign, 3, 800, 1)
      placeCluster(ResourceNodeType.STONE_QUARRY, bx - 2 * xSign, by + 5 * ySign, 2, 500, 1)
      placeCluster(ResourceNodeType.DEER, bx + 5 * xSign, by + 4 * ySign, 3, 150, 2)
      placeCluster(ResourceNodeType.FOREST_OAK, bx - 4 * xSign, by - 4 * ySign, 12, 110, 3)
      placeCluster(ResourceNodeType.FOREST_PINE, bx + 1 * xSign, by + 6 * ySign, 11, 110, 3)
      val secondaryGoldX = (bx + 8 * xSign).coerceIn(3, mapSize - 4)
      val secondaryGoldY = (by - 5 * ySign).coerceIn(3, mapSize - 4)
      placeCluster(ResourceNodeType.GOLD_MINE, secondaryGoldX, secondaryGoldY, 2, 650, 1)
    }

    // Contested mid-map Gold & Stone veins scaled across the broader arena
    val m = mid.toInt()
    val q1 = (mapSize * 0.34f).toInt()
    val q3 = (mapSize * 0.66f).toInt()
    placeCluster(ResourceNodeType.GOLD_MINE, m, m, 6, 900, 2)
    placeCluster(ResourceNodeType.STONE_QUARRY, q1, m, 3, 550, 1)
    placeCluster(ResourceNodeType.STONE_QUARRY, q3, m, 3, 550, 1)
    placeCluster(ResourceNodeType.GOLD_MINE, m, q1, 3, 750, 1)
    placeCluster(ResourceNodeType.GOLD_MINE, m, q3, 3, 750, 1)
    placeCluster(ResourceNodeType.GOLD_MINE, q1, q1, 3, 700, 2)
    placeCluster(ResourceNodeType.GOLD_MINE, q3, q3, 3, 700, 2)
    placeCluster(ResourceNodeType.STONE_QUARRY, q1, q3, 2, 500, 1)
    placeCluster(ResourceNodeType.STONE_QUARRY, q3, q1, 2, 500, 1)

    // Natural timber groves distributed safely across the larger map
    val groveCoords =
      listOf(
        (mapSize * 0.28f).toInt() to (mapSize * 0.14f).toInt(),
        (mapSize * 0.14f).toInt() to (mapSize * 0.32f).toInt(),
        (mapSize * 0.72f).toInt() to (mapSize * 0.86f).toInt(),
        (mapSize * 0.86f).toInt() to (mapSize * 0.68f).toInt(),
        (mapSize * 0.50f).toInt() to (mapSize * 0.12f).toInt(),
        (mapSize * 0.50f).toInt() to (mapSize * 0.88f).toInt(),
        (mapSize * 0.12f).toInt() to (mapSize * 0.50f).toInt(),
        (mapSize * 0.88f).toInt() to (mapSize * 0.50f).toInt(),
        (mapSize * 0.35f).toInt() to (mapSize * 0.65f).toInt(),
        (mapSize * 0.65f).toInt() to (mapSize * 0.35f).toInt(),
      )
    for ((gx, gy) in groveCoords) {
      val tooClose = allBases.any { (_, b) -> hypot((gx - b.first).toFloat(), (gy - b.second).toFloat()) < 10f }
      if (!tooClose) {
        val treeType =
          if ((gx + gy) % 2 == 0) ResourceNodeType.FOREST_OAK else ResourceNodeType.FOREST_PINE
        placeCluster(treeType, gx, gy, 10, 110, 3)
      }
    }

    // Starting Town Centers + starting Villagers & Scout per active faction
    val buildings = mutableListOf<Building>()
    val units = mutableListOf<Unit>()
    val factionStates = linkedMapOf<Faction, FactionState>()

    fun createUnit(type: UnitType, faction: Faction, x: Float, y: Float, facingRight: Boolean): Unit {
      val def = GameDefinitions.unitDefs.getValue(type)
      return Unit(
        id = nextId++,
        type = type,
        faction = faction,
        x = x,
        y = y,
        hp = def.maxHp,
        maxHp = def.maxHp,
        facingRight = facingRight,
      )
    }

    for ((faction, pos) in allBases) {
      val (tcX, tcY) = pos
      val civ = config.enemyCivFor(faction)
      val tcHp = GameDefinitions.effectiveBuildingMaxHp(BuildingType.TOWN_CENTER, civ)
      val onLeft = tcX < mid
      val onTop = tcY < mid
      val rallyDx = if (onLeft) -1.2f else 3.8f
      val vilDirX = if (onLeft) 1f else -1f
      val vilDirY = if (onTop) 1f else -1f

      buildings +=
        Building(
          id = nextId++,
          type = BuildingType.TOWN_CENTER,
          faction = faction,
          tileX = tcX,
          tileY = tcY,
          footprint = 3,
          hp = tcHp,
          maxHp = tcHp,
          buildProgress = 1f,
          isComplete = true,
          rallyX = tcX + rallyDx,
          rallyY = tcY + 1.5f,
        )

      units += createUnit(UnitType.VILLAGER, faction, tcX + (if (onLeft) -0.8f else 3.6f), tcY + 1.2f, onLeft)
      units += createUnit(UnitType.VILLAGER, faction, tcX + 0.8f, tcY + (if (onTop) 3.7f else -0.8f), onLeft)
      units += createUnit(UnitType.VILLAGER, faction, tcX + 2.4f, tcY + (if (onTop) 3.7f else 3.7f), onLeft)
      units += createUnit(
        UnitType.SCOUT_CAVALRY,
        faction,
        tcX + 1.5f + 2.4f * vilDirX,
        tcY + 1.5f + 2.2f * vilDirY,
        onLeft,
      )

      factionStates[faction] = FactionState(faction = faction, civilization = civ, age = Age.DARK_AGE)
    }

    val playerState = factionStates.getValue(Faction.PLAYER)
    val enemyState = factionStates[Faction.ENEMY]
      ?: FactionState(faction = Faction.ENEMY, civilization = config.enemyCiv, age = Age.DARK_AGE)

    val initialWorld =
      GameWorld(
        mapSize = mapSize,
        tiles = tiles,
        fogGrid = IntArray(mapSize * mapSize),
        resources = resources,
        buildings = buildings,
        units = units,
        projectiles = emptyList(),
        floatingTexts = emptyList(),
        effects = emptyList(),
        playerState = playerState,
        enemyState = enemyState,
        nextEntityId = nextId,
        mapType = config.mapType,
        factionStates = factionStates,
      )

    return RtsEngine.recomputeFog(initialWorld)
  }
}
