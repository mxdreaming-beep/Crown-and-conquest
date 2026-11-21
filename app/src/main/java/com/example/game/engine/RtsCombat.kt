package com.example.game.engine

import com.example.game.model.Age
import com.example.game.model.Building
import com.example.game.model.BuildingType
import com.example.game.model.Civilization
import com.example.game.model.Faction
import com.example.game.model.FactionMatchSummary
import com.example.game.model.FactionState
import com.example.game.model.FactionStats
import com.example.game.model.GameDefinitions
import com.example.game.model.GameWorld
import com.example.game.model.MatchResult
import com.example.game.model.Projectile
import com.example.game.model.ProjectileKind
import com.example.game.model.TechType
import com.example.game.model.Unit
import com.example.game.model.UnitDef
import com.example.game.model.UnitOrderType
import com.example.game.model.UnitStance
import com.example.game.model.UnitType
import com.example.game.model.VisualEffect
import com.example.game.model.VisualEffectType
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

object RtsCombat {

  fun resolveCombatAndFog(world: GameWorld, dt: Float): GameWorld {
    var nextId = world.nextEntityId
    val unitsMap = world.units.associateBy { it.id }.toMutableMap()
    val buildingsMap = world.buildings.associateBy { it.id }.toMutableMap()
    val newProjectiles = mutableListOf<Projectile>()
    val newEffects = world.effects.toMutableList()
    val statsMap = mutableMapOf<Faction, FactionStats>().apply {
      for (f in Faction.entries) {
        this[f] = world.stateFor(f).stats
      }
    }

    fun stateFor(faction: Faction): FactionState = world.stateFor(faction)

    fun applyDamageToUnit(targetId: Int, rawDmg: Int, isPierce: Boolean, bonusVsArchers: Int = 0, attackerFaction: Faction) {
      val target = unitsMap[targetId] ?: return
      val tDef = GameDefinitions.unitDefs.getValue(target.type)
      val tState = stateFor(target.faction)
      var armor = if (isPierce) tDef.pierceArmor else tDef.meleeArmor
      if (TechType.SCALE_ARMOR in tState.researchedTechs && (tDef.isInfantry || tDef.isCavalry)) {
        armor += 2
      }
      if (TechType.CHAIN_MAIL in tState.researchedTechs && (tDef.isInfantry || tDef.isCavalry)) {
        armor += 2
      }
      if (TechType.PLATE_ARMOR in tState.researchedTechs && target.type != UnitType.VILLAGER) {
        armor += 3
      }
      if (TechType.REACTIVE_ARMOR in tState.researchedTechs && target.type != UnitType.VILLAGER) {
        armor += 3
      }
      if (!isPierce && tState.civilization == Civilization.TEUTONS && tDef.isInfantry) {
        armor += 1
      }
      val extra = if (tDef.isRanged) bonusVsArchers else 0
      val dmg = max(1, rawDmg + extra - armor)
      val newHp = target.hp - dmg
      if (newHp <= 0) {
        unitsMap.remove(targetId)
        val deathFx =
          if (tDef.isArmored || tDef.isFlying) VisualEffectType.EXPLOSION else VisualEffectType.DUST_PUFF
        val fxDur = if (deathFx == VisualEffectType.EXPLOSION) 0.65f else 0.5f
        newEffects += VisualEffect(id = nextId++, type = deathFx, x = target.x, y = target.y, durationSec = fxDur)
        val victimSt = statsMap.getValue(target.faction)
        statsMap[target.faction] = victimSt.copy(unitsLost = victimSt.unitsLost + 1)
        val killerSt = statsMap.getValue(attackerFaction)
        statsMap[attackerFaction] = killerSt.copy(enemiesKilled = killerSt.enemiesKilled + 1)
      } else {
        unitsMap[targetId] = target.copy(hp = newHp)
      }
    }

    fun applyDamageToBuilding(targetId: Int, rawDmg: Int, attackerFaction: Faction) {
      val b = buildingsMap[targetId] ?: return
      val dmg = max(1, rawDmg)
      val newHp = b.hp - dmg
      if (newHp <= 0) {
        buildingsMap.remove(targetId)
        newEffects += VisualEffect(id = nextId++, type = VisualEffectType.EXPLOSION, x = b.centerX, y = b.centerY, durationSec = 0.8f)
        val attackerSt = statsMap.getValue(attackerFaction)
        statsMap[attackerFaction] = attackerSt.copy(buildingsRazed = attackerSt.buildingsRazed + 1)
      } else {
        buildingsMap[targetId] = b.copy(hp = newHp)
      }
    }

    // 1. Unit Combat & Attack-Move
    for (uId in unitsMap.keys.toList()) {
      var u = unitsMap[uId] ?: continue
      val uDef = GameDefinitions.unitDefs.getValue(u.type)
      val fState = stateFor(u.faction)
      val effectiveSpeed = RtsEngine.unitEffectiveSpeed(u.type, fState)
      val effectiveRange =
        uDef.range +
          (if (uDef.isRanged && fState.civilization == Civilization.BRITONS) 1f else 0f) +
          (if (uDef.isRanged && TechType.FLETCHING in fState.researchedTechs) 1f else 0f) +
          (if (uDef.isRanged && TechType.BODKIN_ARROW in fState.researchedTechs) 1f else 0f) +
          (if (uDef.isRanged && TechType.BALLISTICS_ALLOYS in fState.researchedTechs) 1f else 0f) +
          (if (uDef.isSiege && TechType.SIEGE_ENGINEERS in fState.researchedTechs) 1f else 0f)

      val meleeOrArmorBoost =
        (if ((!uDef.isRanged || uDef.isArmored) && TechType.FORGING in fState.researchedTechs) 2 else 0) +
          (if ((!uDef.isRanged || uDef.isArmored) && TechType.IRON_CASTING in fState.researchedTechs) 2 else 0) +
          (if ((!uDef.isRanged && u.type != UnitType.VILLAGER || (uDef.isArmored && !uDef.isFlying)) && TechType.BLAST_FURNACE in fState.researchedTechs) 3 else 0)

      val rangedBoost =
        (if (uDef.isRanged && TechType.FLETCHING in fState.researchedTechs) 1 else 0) +
          (if (uDef.isRanged && TechType.BODKIN_ARROW in fState.researchedTechs) 2 else 0) +
          (if (uDef.isRanged && TechType.BALLISTICS_ALLOYS in fState.researchedTechs) 3 else 0) +
          (if (uDef.isRanged && TechType.BALLISTICS in fState.researchedTechs) 1 else 0)

      val universityCombatBoost =
        (if (TechType.CHEMISTRY in fState.researchedTechs && isGunpowderOrModern(u.type)) 3 else 0) +
          (if (TechType.AERODYNAMICS in fState.researchedTechs && uDef.isFlying) 4 else 0)

      val siegeBonusVsB =
        if (uDef.isSiege && TechType.SIEGE_ENGINEERS in fState.researchedTechs) 35 else 0
      val projSpeedMult =
        if (TechType.BALLISTICS in fState.researchedTechs) 1.30f else 1f

      val ageBoost =
        (if (fState.age.index >= Age.IMPERIAL_AGE.index) 2 else 0) +
          (if (fState.age.index >= Age.INDUSTRIAL_AGE.index) 2 else 0) +
          (if (fState.age.index >= Age.MODERN_AGE.index) 3 else 0)

      val effectiveAttack = uDef.attack + meleeOrArmorBoost + rangedBoost + universityCombatBoost + ageBoost

      fun canEngageTarget(target: Unit): Boolean {
        val targetDef = GameDefinitions.unitDefs.getValue(target.type)
        return uDef.isRanged || uDef.isFlying || !targetDef.isFlying
      }

      // Auto-acquire nearby enemies for IDLE or ATTACK_MOVE military units
      if ((u.order == UnitOrderType.IDLE && u.type != UnitType.VILLAGER && u.stance != UnitStance.STAND_GROUND) ||
        u.order == UnitOrderType.ATTACK_MOVE
      ) {
        val scanRadius =
          if (uDef.isFlying) 9.5f
          else if (u.order == UnitOrderType.ATTACK_MOVE) 6.5f
          else 5.0f
        val nearEnemyUnit =
          unitsMap.values
            .filter { it.faction != u.faction && canEngageTarget(it) && hypot(it.x - u.x, it.y - u.y) <= scanRadius }
            .minByOrNull { hypot(it.x - u.x, it.y - u.y) }
        if (nearEnemyUnit != null) {
          val dist = hypot(nearEnemyUnit.x - u.x, nearEnemyUnit.y - u.y)
          if (dist <= effectiveRange) {
            u =
              performUnitAttack(
                u = u,
                targetUnit = nearEnemyUnit,
                effectiveAttack = effectiveAttack,
                uDef = uDef,
                projSpeedMult = projSpeedMult,
                newProjectiles = newProjectiles,
                newEffects = newEffects,
                nextIdAlloc = { nextId++ },
                applyMelee = { tid, dmg -> applyDamageToUnit(tid, dmg, false, 0, u.faction) },
              )
            unitsMap[uId] = u
            continue
          } else {
            u = RtsEngine.moveUnitTowards(u, nearEnemyUnit.x, nearEnemyUnit.y, effectiveSpeed, dt, world, buildingsMap.values)
            unitsMap[uId] = u
            continue
          }
        } else if (u.order == UnitOrderType.ATTACK_MOVE) {
          val nearEnemyB =
            buildingsMap.values
              .filter { it.faction != u.faction && RtsEngine.distanceToBuilding(u.x, u.y, it) <= 5.5f }
              .minByOrNull { RtsEngine.distanceToBuilding(u.x, u.y, it) }
          if (nearEnemyB != null) {
            val dB = RtsEngine.distanceToBuilding(u.x, u.y, nearEnemyB)
            if (dB <= effectiveRange + 0.3f) {
              u =
                performBuildingAttack(
                  u = u,
                  targetB = nearEnemyB,
                  effectiveAttack = effectiveAttack,
                  siegeBonusVsB = siegeBonusVsB,
                  projSpeedMult = projSpeedMult,
                  uDef = uDef,
                  newProjectiles = newProjectiles,
                  newEffects = newEffects,
                  nextIdAlloc = { nextId++ },
                  applyMelee = { bid, dmg -> applyDamageToBuilding(bid, dmg, u.faction) },
                )
            } else {
              val (ax, ay) = RtsEngine.nearestApproachPoint(u.x, u.y, nearEnemyB)
              u = RtsEngine.moveUnitTowards(u, ax, ay, effectiveSpeed, dt, world, buildingsMap.values, targetBuilding = nearEnemyB)
            }
            unitsMap[uId] = u
            continue
          } else {
            val destDist = hypot(u.targetX - u.x, u.targetY - u.y)
            if (destDist < 0.4f) {
              u = u.copy(order = UnitOrderType.IDLE)
            } else {
              u = RtsEngine.moveUnitTowards(u, u.targetX, u.targetY, effectiveSpeed, dt, world, buildingsMap.values)
            }
            unitsMap[uId] = u
            continue
          }
        }
      }

      if (u.order == UnitOrderType.ATTACK_UNIT) {
        val target = unitsMap[u.targetEntityId]
        if (target == null || !canEngageTarget(target)) {
          u = u.copy(order = UnitOrderType.IDLE, targetEntityId = -1)
        } else {
          val dist = hypot(target.x - u.x, target.y - u.y)
          if (dist <= effectiveRange) {
            u =
              performUnitAttack(
                u = u,
                targetUnit = target,
                effectiveAttack = effectiveAttack,
                uDef = uDef,
                projSpeedMult = projSpeedMult,
                newProjectiles = newProjectiles,
                newEffects = newEffects,
                nextIdAlloc = { nextId++ },
                applyMelee = { tid, dmg -> applyDamageToUnit(tid, dmg, false, 0, u.faction) },
              )
          } else {
            u = RtsEngine.moveUnitTowards(u, target.x, target.y, effectiveSpeed, dt, world, buildingsMap.values)
          }
        }
        unitsMap[uId] = u
      } else if (u.order == UnitOrderType.ATTACK_BUILDING) {
        val targetB = buildingsMap[u.targetEntityId]
        if (targetB == null) {
          u = u.copy(order = UnitOrderType.IDLE, targetEntityId = -1)
        } else {
          val dist = RtsEngine.distanceToBuilding(u.x, u.y, targetB)
          if (dist <= effectiveRange + 0.3f) {
            u =
              performBuildingAttack(
                u = u,
                targetB = targetB,
                effectiveAttack = effectiveAttack,
                siegeBonusVsB = siegeBonusVsB,
                projSpeedMult = projSpeedMult,
                uDef = uDef,
                newProjectiles = newProjectiles,
                newEffects = newEffects,
                nextIdAlloc = { nextId++ },
                applyMelee = { bid, dmg -> applyDamageToBuilding(bid, dmg, u.faction) },
              )
          } else {
            val (ax, ay) = RtsEngine.nearestApproachPoint(u.x, u.y, targetB)
            u = RtsEngine.moveUnitTowards(u, ax, ay, effectiveSpeed, dt, world, buildingsMap.values, targetBuilding = targetB)
          }
        }
        unitsMap[uId] = u
      }
    }

    // 2. Defensive Buildings Auto-Fire (Town Center, Watch Tower, Castle, Airfield SAM)
    for (bId in buildingsMap.keys.toList()) {
      val b = buildingsMap[bId] ?: continue
      if (!b.isComplete) continue
      val bDef = GameDefinitions.buildingDefs.getValue(b.type)
      if (bDef.attackDamage <= 0 || b.attackTimer > 0f) continue
      val fState = stateFor(b.faction)
      val bonusRange =
        (if (TechType.FLETCHING in fState.researchedTechs) 1f else 0f) +
          (if (TechType.BODKIN_ARROW in fState.researchedTechs) 1f else 0f) +
          (if (TechType.BALLISTICS_ALLOYS in fState.researchedTechs) 1f else 0f)
      val bonusDmg =
        (if (TechType.FLETCHING in fState.researchedTechs) 1 else 0) +
          (if (TechType.BODKIN_ARROW in fState.researchedTechs) 2 else 0) +
          (if (TechType.BALLISTICS in fState.researchedTechs) 2 else 0) +
          (if (TechType.BALLISTICS_ALLOYS in fState.researchedTechs) 3 else 0)
      val bProjSpeedMult = if (TechType.BALLISTICS in fState.researchedTechs) 1.30f else 1f
      val target =
        unitsMap.values
          .filter { it.faction != b.faction && RtsEngine.distanceToBuilding(it.x, it.y, b) <= bDef.attackRange + bonusRange }
          .minByOrNull { RtsEngine.distanceToBuilding(it.x, it.y, b) }
      if (target != null) {
        val kind = if (b.type == BuildingType.AIRFIELD) ProjectileKind.MISSILE else ProjectileKind.BOLT
        val speedDiv = (if (kind == ProjectileKind.MISSILE) 18f else 14f) * bProjSpeedMult
        newProjectiles +=
          Projectile(
            id = nextId++,
            faction = b.faction,
            startX = b.centerX,
            startY = b.centerY - 0.5f,
            targetX = target.x,
            targetY = target.y,
            targetUnitId = target.id,
            targetBuildingId = -1,
            progress = 0f,
            duration = max(0.11f, hypot(target.x - b.centerX, target.y - b.centerY) / speedDiv),
            damage = bDef.attackDamage + bonusDmg,
            isBolt = kind == ProjectileKind.BOLT,
            kind = kind,
            splashRadius = if (kind == ProjectileKind.MISSILE) 0.9f else 0f,
          )
        buildingsMap[bId] = b.copy(attackTimer = bDef.attackCooldown)
      }
    }

    // 3. Advance & Resolve Projectiles (including Splash Explosions)
    val activeProjectiles = mutableListOf<Projectile>()
    for (p in world.projectiles + newProjectiles) {
      val nextProg = p.progress + dt / p.duration
      if (nextProg >= 1f) {
        if (p.targetUnitId != -1) {
          applyDamageToUnit(p.targetUnitId, p.damage, true, p.bonusVsArchers, p.faction)
        } else if (p.targetBuildingId != -1) {
          applyDamageToBuilding(p.targetBuildingId, p.damage, p.faction)
        }
        if (p.splashRadius > 0f) {
          val splashDmg = max(4, (p.damage * 0.55f).toInt())
          for (other in unitsMap.values.toList()) {
            if (other.faction != p.faction && other.id != p.targetUnitId) {
              if (hypot(other.x - p.targetX, other.y - p.targetY) <= p.splashRadius) {
                applyDamageToUnit(other.id, splashDmg, true, 0, p.faction)
              }
            }
          }
          for (otherB in buildingsMap.values.toList()) {
            if (otherB.faction != p.faction && otherB.id != p.targetBuildingId) {
              if (RtsEngine.distanceToBuilding(p.targetX, p.targetY, otherB) <= p.splashRadius) {
                applyDamageToBuilding(otherB.id, splashDmg, p.faction)
              }
            }
          }
          newEffects +=
            VisualEffect(
              id = nextId++,
              type = VisualEffectType.EXPLOSION,
              x = p.targetX,
              y = p.targetY,
              durationSec = 0.62f,
            )
        } else {
          newEffects +=
            VisualEffect(
              id = nextId++,
              type = VisualEffectType.ATTACK_SPARK,
              x = p.targetX,
              y = p.targetY,
              durationSec = 0.25f,
            )
        }
      } else {
        activeProjectiles += p.copy(progress = nextProg)
      }
    }

    // 4. Victory / Defeat Check (Conquest: defeat all active AI rivals)
    val activeAis = world.aiFactions.ifEmpty { listOf(Faction.ENEMY) }
    val playerAlive = buildingsMap.values.any { it.faction == Faction.PLAYER }
    val aliveAis = activeAis.filter { ai -> buildingsMap.values.any { it.faction == ai } }
    fun buildSummaries(): List<FactionMatchSummary> =
      activeAis.map { ai ->
        val st = world.stateFor(ai)
        FactionMatchSummary(
          faction = ai,
          civilization = st.civilization,
          age = st.age,
          stats = statsMap.getValue(ai),
          survivingBuildings = buildingsMap.values.count { it.faction == ai },
        )
      }

    val matchResult =
      when {
        aliveAis.isEmpty() ->
          MatchResult(
            winner = Faction.PLAYER,
            durationSec = world.elapsedSec,
            playerAge = world.playerState.age,
            enemyAge = world.enemyState.age,
            playerStats = statsMap.getValue(Faction.PLAYER),
            enemyStats = statsMap.getValue(Faction.ENEMY),
            playerCiv = world.playerState.civilization,
            enemyCiv = world.enemyState.civilization,
            mapType = world.mapType,
            aiSummaries = buildSummaries(),
          )
        !playerAlive -> {
          val topAi =
            aliveAis.maxByOrNull { ai ->
              buildingsMap.values.count { it.faction == ai } * 10 + unitsMap.values.count { it.faction == ai }
            } ?: Faction.ENEMY
          MatchResult(
            winner = topAi,
            durationSec = world.elapsedSec,
            playerAge = world.playerState.age,
            enemyAge = world.stateFor(topAi).age,
            playerStats = statsMap.getValue(Faction.PLAYER),
            enemyStats = statsMap.getValue(topAi),
            playerCiv = world.playerState.civilization,
            enemyCiv = world.stateFor(topAi).civilization,
            mapType = world.mapType,
            aiSummaries = buildSummaries(),
          )
        }
        else -> null
      }

    val updatedPlayerState = world.playerState.copy(stats = statsMap.getValue(Faction.PLAYER))
    val updatedEnemyState = world.enemyState.copy(stats = statsMap.getValue(Faction.ENEMY))
    val nextFactionStates =
      world.factionStates.keys.associateWith { f ->
        world.stateFor(f).copy(stats = statsMap.getValue(f))
      } + mapOf(Faction.PLAYER to updatedPlayerState, Faction.ENEMY to updatedEnemyState)

    val nextWorld =
      world.copy(
        buildings = buildingsMap.values.toList(),
        units = unitsMap.values.toList(),
        projectiles = activeProjectiles,
        effects = newEffects,
        playerState = updatedPlayerState,
        enemyState = updatedEnemyState,
        factionStates = nextFactionStates,
        nextEntityId = nextId,
        matchResult = matchResult,
      )

    return recomputeFog(nextWorld)
  }

  private fun projectileSpecFor(type: UnitType): Triple<ProjectileKind, Float, Float> =
    when (type) {
      UnitType.RIFLEMAN, UnitType.MACHINE_GUNNER, UnitType.MARINE, UnitType.FIGHTER_PLANE ->
        Triple(ProjectileKind.BULLET, 0f, 23f)
      UnitType.SNIPER -> Triple(ProjectileKind.SNIPER_ROUND, 0f, 34f)
      UnitType.ARTILLERY -> Triple(ProjectileKind.SHELL, 1.55f, 11f)
      UnitType.TANK -> Triple(ProjectileKind.SHELL, 1.15f, 15f)
      UnitType.HEAVY_TANK -> Triple(ProjectileKind.SHELL, 1.35f, 16f)
      UnitType.ROCKET_SOLDIER -> Triple(ProjectileKind.MISSILE, 0.95f, 17f)
      UnitType.ROCKET_LAUNCHER -> Triple(ProjectileKind.MISSILE, 1.70f, 14f)
      UnitType.GUNSHIP -> Triple(ProjectileKind.MISSILE, 1.05f, 19f)
      UnitType.BOMBER_PLANE -> Triple(ProjectileKind.MISSILE, 1.65f, 13f)
      UnitType.CROSSBOWMAN -> Triple(ProjectileKind.BOLT, 0f, 13f)
      else -> Triple(ProjectileKind.ARROW, 0f, 12f)
    }

  private fun isGunpowderOrModern(type: UnitType): Boolean =
    when (type) {
      UnitType.RIFLEMAN,
      UnitType.MACHINE_GUNNER,
      UnitType.MARINE,
      UnitType.SNIPER,
      UnitType.ROCKET_SOLDIER,
      UnitType.ARTILLERY,
      UnitType.TANK,
      UnitType.HEAVY_TANK,
      UnitType.ROCKET_LAUNCHER,
      UnitType.FIGHTER_PLANE,
      UnitType.BOMBER_PLANE,
      UnitType.GUNSHIP,
      -> true
      else -> false
    }

  private fun performUnitAttack(
    u: Unit,
    targetUnit: Unit,
    effectiveAttack: Int,
    uDef: UnitDef,
    projSpeedMult: Float,
    newProjectiles: MutableList<Projectile>,
    newEffects: MutableList<VisualEffect>,
    nextIdAlloc: () -> Int,
    applyMelee: (Int, Int) -> kotlin.Unit,
  ): Unit {
    val facing = (targetUnit.x - targetUnit.y) >= (u.x - u.y)
    if (u.attackTimer > 0f) return u.copy(facingRight = facing, isAttackingAnim = true)
    val targetDef = GameDefinitions.unitDefs.getValue(targetUnit.type)
    val bonus =
      (if (targetDef.isCavalry) uDef.bonusVsCavalry else 0) +
        (if (targetDef.isArmored) uDef.bonusVsArmored else 0)
    if (uDef.isRanged) {
      val dist = hypot(targetUnit.x - u.x, targetUnit.y - u.y)
      val (kind, splash, projSpeed) = projectileSpecFor(u.type)
      newProjectiles +=
        Projectile(
          id = nextIdAlloc(),
          faction = u.faction,
          startX = u.x,
          startY = u.y,
          targetX = targetUnit.x,
          targetY = targetUnit.y,
          targetUnitId = targetUnit.id,
          targetBuildingId = -1,
          progress = 0f,
          duration = max(0.07f, dist / (projSpeed * projSpeedMult)),
          damage = effectiveAttack + bonus,
          bonusVsArchers = uDef.bonusVsArchers,
          isBolt = kind == ProjectileKind.BOLT,
          kind = kind,
          splashRadius = splash,
        )
      if (kind == ProjectileKind.BULLET ||
        kind == ProjectileKind.SNIPER_ROUND ||
        kind == ProjectileKind.SHELL ||
        kind == ProjectileKind.MISSILE
      ) {
        newEffects += VisualEffect(id = nextIdAlloc(), type = VisualEffectType.MUZZLE_FLASH, x = u.x, y = u.y, durationSec = 0.18f)
      }
    } else {
      applyMelee(targetUnit.id, effectiveAttack + bonus)
      newEffects +=
        VisualEffect(
          id = nextIdAlloc(),
          type = VisualEffectType.ATTACK_SPARK,
          x = (u.x + targetUnit.x) * 0.5f,
          y = (u.y + targetUnit.y) * 0.5f,
          durationSec = 0.25f,
        )
    }
    return u.copy(facingRight = facing, attackTimer = uDef.attackCooldown, isAttackingAnim = true)
  }

  private fun performBuildingAttack(
    u: Unit,
    targetB: Building,
    effectiveAttack: Int,
    siegeBonusVsB: Int,
    projSpeedMult: Float,
    uDef: UnitDef,
    newProjectiles: MutableList<Projectile>,
    newEffects: MutableList<VisualEffect>,
    nextIdAlloc: () -> Int,
    applyMelee: (Int, Int) -> kotlin.Unit,
  ): Unit {
    val facing = (targetB.centerX - targetB.centerY) >= (u.x - u.y)
    if (u.attackTimer > 0f) return u.copy(facingRight = facing, isAttackingAnim = true)
    val dmg = effectiveAttack + uDef.bonusVsBuildings + siegeBonusVsB
    if (uDef.isRanged) {
      val dist = hypot(targetB.centerX - u.x, targetB.centerY - u.y)
      val (kind, splash, projSpeed) = projectileSpecFor(u.type)
      newProjectiles +=
        Projectile(
          id = nextIdAlloc(),
          faction = u.faction,
          startX = u.x,
          startY = u.y,
          targetX = targetB.centerX,
          targetY = targetB.centerY,
          targetUnitId = -1,
          targetBuildingId = targetB.id,
          progress = 0f,
          duration = max(0.07f, dist / (projSpeed * projSpeedMult)),
          damage = dmg,
          isBolt = kind == ProjectileKind.BOLT,
          kind = kind,
          splashRadius = splash,
        )
      if (kind == ProjectileKind.BULLET ||
        kind == ProjectileKind.SNIPER_ROUND ||
        kind == ProjectileKind.SHELL ||
        kind == ProjectileKind.MISSILE
      ) {
        newEffects += VisualEffect(id = nextIdAlloc(), type = VisualEffectType.MUZZLE_FLASH, x = u.x, y = u.y, durationSec = 0.18f)
      }
    } else {
      applyMelee(targetB.id, max(1, dmg / 2))
      newEffects +=
        VisualEffect(
          id = nextIdAlloc(),
          type = VisualEffectType.ATTACK_SPARK,
          x = targetB.centerX,
          y = targetB.centerY,
          durationSec = 0.28f,
        )
    }
    return u.copy(facingRight = facing, attackTimer = uDef.attackCooldown, isAttackingAnim = true)
  }

  fun recomputeFog(world: GameWorld): GameWorld {
    val size = world.mapSize
    val fog = world.fogGrid.copyOf()
    // Demote currently visible (2) to explored (1)
    for (i in fog.indices) {
      if (fog[i] == 2) fog[i] = 1
    }

    fun reveal(cx: Float, cy: Float, radius: Float) {
      val rInt = radius.roundToInt() + 1
      val rSq = radius * radius
      val minX = max(0, cx.toInt() - rInt)
      val maxX = kotlin.math.min(size - 1, cx.toInt() + rInt)
      val minY = max(0, cy.toInt() - rInt)
      val maxY = kotlin.math.min(size - 1, cy.toInt() + rInt)
      for (ty in minY..maxY) {
        for (tx in minX..maxX) {
          val dx = (tx + 0.5f) - cx
          val dy = (ty + 0.5f) - cy
          if (dx * dx + dy * dy <= rSq) {
            fog[ty * size + tx] = 2
          }
        }
      }
    }

    for (u in world.units) {
      if (u.faction == Faction.PLAYER) {
        reveal(u.x, u.y, GameDefinitions.unitDefs.getValue(u.type).lineOfSight)
      }
    }
    for (b in world.buildings) {
      if (b.faction == Faction.PLAYER) {
        reveal(b.centerX, b.centerY, GameDefinitions.buildingDefs.getValue(b.type).lineOfSight)
      }
    }
    return world.copy(fogGrid = fog)
  }
}
