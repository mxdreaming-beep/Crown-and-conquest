package com.example.game.model

data class SkirmishConfig(
  val playerCiv: Civilization = Civilization.BRITONS,
  val enemyCiv: Civilization = Civilization.FRANKS,
  val mapType: MapType = MapType.ARABIA,
  val difficulty: Difficulty = Difficulty.MODERATE,
  val aiCount: Int = 2,
  val enemy2Civ: Civilization = Civilization.TEUTONS,
  val enemy3Civ: Civilization = Civilization.BYZANTINES,
  val mapSize: MapSizeOption = MapSizeOption.LARGE,
) {
  fun activeAiFactions(): List<Faction> = Faction.aiFactions.take(aiCount.coerceIn(1, 3))

  fun enemyCivFor(faction: Faction): Civilization =
    when (faction) {
      Faction.ENEMY -> enemyCiv
      Faction.ENEMY_2 -> enemy2Civ
      Faction.ENEMY_3 -> enemy3Civ
      Faction.PLAYER -> playerCiv
    }
}

data class ResourcePool(
  val wood: Int = 200,
  val food: Int = 200,
  val gold: Int = 100,
  val stone: Int = 200,
) {
  fun canAfford(cost: ResourceCost): Boolean =
    wood >= cost.wood && food >= cost.food && gold >= cost.gold && stone >= cost.stone

  operator fun minus(cost: ResourceCost): ResourcePool =
    copy(
      wood = (wood - cost.wood).coerceAtLeast(0),
      food = (food - cost.food).coerceAtLeast(0),
      gold = (gold - cost.gold).coerceAtLeast(0),
      stone = (stone - cost.stone).coerceAtLeast(0),
    )

  fun add(type: ResourceType, amount: Int): ResourcePool =
    when (type) {
      ResourceType.WOOD -> copy(wood = wood + amount)
      ResourceType.FOOD -> copy(food = food + amount)
      ResourceType.GOLD -> copy(gold = gold + amount)
      ResourceType.STONE -> copy(stone = stone + amount)
    }
}

data class ResourceNode(
  val id: Int,
  val type: ResourceNodeType,
  val tileX: Int,
  val tileY: Int,
  val remaining: Int,
  val maxAmount: Int,
  val variant: Int = 0,
) {
  val x: Float get() = tileX + 0.5f
  val y: Float get() = tileY + 0.5f
}

sealed class ProductionItem {
  abstract val progress: Float
  abstract val totalTime: Float

  data class TrainUnit(
    val unitType: UnitType,
    override val progress: Float = 0f,
    override val totalTime: Float,
  ) : ProductionItem()

  data class ResearchTech(
    val techType: TechType,
    override val progress: Float = 0f,
    override val totalTime: Float,
  ) : ProductionItem()
}

data class Building(
  val id: Int,
  val type: BuildingType,
  val faction: Faction,
  val tileX: Int,
  val tileY: Int,
  val footprint: Int,
  val hp: Int,
  val maxHp: Int,
  val buildProgress: Float, // 0f..1f
  val isComplete: Boolean,
  val queue: List<ProductionItem> = emptyList(),
  val rallyX: Float = tileX + footprint + 0.8f,
  val rallyY: Float = tileY + footprint + 0.8f,
  val attackTimer: Float = 0f,
  val farmFoodRemaining: Int = if (type == BuildingType.FARM) 250 else 0,
  val animPhase: Float = 0f,
) {
  val centerX: Float get() = tileX + footprint * 0.5f
  val centerY: Float get() = tileY + footprint * 0.5f
}

data class Unit(
  val id: Int,
  val type: UnitType,
  val faction: Faction,
  val x: Float,
  val y: Float,
  val hp: Int,
  val maxHp: Int,
  val facingRight: Boolean = true,
  val order: UnitOrderType = UnitOrderType.IDLE,
  val stance: UnitStance = UnitStance.AGGRESSIVE,
  val targetX: Float = x,
  val targetY: Float = y,
  val targetEntityId: Int = -1,
  val targetResourceId: Int = -1,
  val carryingType: ResourceType? = null,
  val carryingAmount: Int = 0,
  val gatherTimer: Float = 0f,
  val returningToDropOff: Boolean = false,
  val attackTimer: Float = 0f,
  val idleTimer: Float = 0f,
  val animTime: Float = 0f,
  val isMoving: Boolean = false,
  val isAttackingAnim: Boolean = false,
  val buildQueue: List<Int> = emptyList(),
)

enum class ProjectileKind {
  ARROW,
  BOLT,
  BULLET,
  SNIPER_ROUND,
  SHELL,
  MISSILE,
}

data class Projectile(
  val id: Int,
  val faction: Faction,
  val startX: Float,
  val startY: Float,
  val targetX: Float,
  val targetY: Float,
  val targetUnitId: Int,
  val targetBuildingId: Int,
  val progress: Float,
  val duration: Float,
  val damage: Int,
  val bonusVsArchers: Int = 0,
  val isBolt: Boolean = false,
  val kind: ProjectileKind = if (isBolt) ProjectileKind.BOLT else ProjectileKind.ARROW,
  val splashRadius: Float = 0f,
)

data class FloatingText(
  val id: Int,
  val x: Float,
  val y: Float,
  val text: String,
  val colorHex: Long,
  val ageSec: Float = 0f,
)

enum class VisualEffectType {
  MOVE_PING,
  ATTACK_SPARK,
  DUST_PUFF,
  EXPLOSION,
  MUZZLE_FLASH,
}

data class VisualEffect(
  val id: Int,
  val type: VisualEffectType,
  val x: Float,
  val y: Float,
  val ageSec: Float = 0f,
  val durationSec: Float = 0.55f,
)

data class FactionStats(
  val totalWoodGathered: Int = 0,
  val totalFoodGathered: Int = 0,
  val totalGoldGathered: Int = 0,
  val totalStoneGathered: Int = 0,
  val unitsTrained: Int = 0,
  val unitsLost: Int = 0,
  val enemiesKilled: Int = 0,
  val buildingsConstructed: Int = 0,
  val buildingsRazed: Int = 0,
)

data class FactionState(
  val faction: Faction,
  val civilization: Civilization,
  val resources: ResourcePool = ResourcePool(),
  val age: Age = Age.DARK_AGE,
  val researchedTechs: Set<TechType> = emptySet(),
  val researchingTechs: Set<TechType> = emptySet(),
  val stats: FactionStats = FactionStats(),
)

data class FactionMatchSummary(
  val faction: Faction,
  val civilization: Civilization,
  val age: Age,
  val stats: FactionStats,
  val survivingBuildings: Int,
)

data class MatchResult(
  val winner: Faction,
  val durationSec: Float,
  val playerAge: Age,
  val enemyAge: Age,
  val playerStats: FactionStats,
  val enemyStats: FactionStats,
  val playerCiv: Civilization,
  val enemyCiv: Civilization,
  val mapType: MapType,
  val aiSummaries: List<FactionMatchSummary> = emptyList(),
)

data class GameWorld(
  val mapSize: Int,
  val tiles: List<TileTerrain>,
  val fogGrid: IntArray, // 0 = unexplored, 1 = explored, 2 = visible
  val resources: List<ResourceNode>,
  val buildings: List<Building>,
  val units: List<Unit>,
  val projectiles: List<Projectile>,
  val floatingTexts: List<FloatingText>,
  val effects: List<VisualEffect>,
  val playerState: FactionState,
  val enemyState: FactionState,
  val elapsedSec: Float = 0f,
  val nextEntityId: Int = 1000,
  val mapType: MapType = MapType.ARABIA,
  val matchResult: MatchResult? = null,
  val factionStates: Map<Faction, FactionState> =
    mapOf(Faction.PLAYER to playerState, Faction.ENEMY to enemyState),
) {
  val aiFactions: List<Faction>
    get() = factionStates.keys.filter { it.isAi }.sortedBy { it.ordinal }

  fun stateFor(faction: Faction): FactionState =
    when (faction) {
      Faction.PLAYER -> playerState
      Faction.ENEMY -> enemyState
      else -> factionStates[faction] ?: enemyState
    }

  fun withFactionState(faction: Faction, state: FactionState): GameWorld {
    val nextMap = factionStates + (faction to state)
    return when (faction) {
      Faction.PLAYER -> copy(playerState = state, factionStates = nextMap)
      Faction.ENEMY -> copy(enemyState = state, factionStates = nextMap)
      else -> copy(factionStates = nextMap)
    }
  }

  fun withFactionStates(updated: Map<Faction, FactionState>): GameWorld {
    var result = this
    for ((f, s) in updated) {
      result = result.withFactionState(f, s)
    }
    return result
  }

  fun tileAt(tx: Int, ty: Int): TileTerrain {
    if (tx < 0 || ty < 0 || tx >= mapSize || ty >= mapSize) return TileTerrain.CLIFF
    return tiles[ty * mapSize + tx]
  }

  fun fogAt(tx: Int, ty: Int): Int {
    if (tx < 0 || ty < 0 || tx >= mapSize || ty >= mapSize) return 0
    return fogGrid[ty * mapSize + tx]
  }

  fun buildingMaxFog(b: Building): Int {
    var best = 0
    for (dy in 0 until b.footprint) {
      for (dx in 0 until b.footprint) {
        val f = fogAt(b.tileX + dx, b.tileY + dy)
        if (f > best) best = f
      }
    }
    return best
  }
}

data class CameraState(
  val centerWorldX: Float = 8f,
  val centerWorldY: Float = 8f,
  val zoom: Float = 1.05f,
)
