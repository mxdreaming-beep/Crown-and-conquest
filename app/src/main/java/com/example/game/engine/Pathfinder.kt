package com.example.game.engine

import com.example.game.model.Building
import com.example.game.model.GameDefinitions
import com.example.game.model.GameWorld
import com.example.game.model.TileTerrain
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

object Pathfinder {

  private data class Node(val tx: Int, val ty: Int, val g: Float, val f: Float) : Comparable<Node> {
    override fun compareTo(other: Node): Int = f.compareTo(other.f)
  }

  /**
   * Returns true if a unit can walk in a straight line from (x0, y0) to (x1, y1)
   * without entering water, cliffs, or clipping a solid building's padded footprint.
   */
  fun hasClearLine(
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    world: GameWorld,
    buildings: Collection<Building>,
    ignoreBuildingId: Int = -1,
  ): Boolean {
    val dist = hypot(x1 - x0, y1 - y0)
    if (dist < 0.05f) return true
    val steps = max(2, ceil(dist / 0.20f).toInt())
    val margin = 0.08f
    for (i in 1..steps) {
      val t = i.toFloat() / steps
      val px = x0 + (x1 - x0) * t
      val py = y0 + (y1 - y0) * t
      if (px < 0.4f || py < 0.4f || px >= world.mapSize - 1.4f || py >= world.mapSize - 1.4f) return false
      val tile = world.tileAt(px.toInt(), py.toInt())
      if (tile == TileTerrain.WATER || tile == TileTerrain.CLIFF) return false
      for (b in buildings) {
        if (b.id == ignoreBuildingId) continue
        val def = GameDefinitions.buildingDefs.getValue(b.type)
        if (!def.isWalkable &&
          px >= b.tileX - margin &&
          px <= b.tileX + b.footprint + margin &&
          py >= b.tileY - margin &&
          py <= b.tileY + b.footprint + margin
        ) {
          return false
        }
      }
    }
    return true
  }

  /**
   * Finds a smooth, line-of-sight pruned world waypoint from (startX, startY) toward (goalX, goalY).
   * If [targetBuilding] is provided, any walkable tile immediately bordering its footprint counts as
   * reaching the goal so villagers smoothly navigate around 2x2 and 3x3 structures (like Town Centers).
   */
  fun nextWaypoint(
    startX: Float,
    startY: Float,
    goalX: Float,
    goalY: Float,
    world: GameWorld,
    buildings: Collection<Building>,
    targetBuilding: Building? = null,
  ): Pair<Float, Float>? {
    val size = world.mapSize
    val sx = startX.toInt().coerceIn(0, size - 1)
    val sy = startY.toInt().coerceIn(0, size - 1)
    val gx = goalX.toInt().coerceIn(0, size - 1)
    val gy = goalY.toInt().coerceIn(0, size - 1)

    val blocked = BooleanArray(size * size)
    for (y in 0 until size) {
      for (x in 0 until size) {
        val t = world.tileAt(x, y)
        if (t == TileTerrain.WATER || t == TileTerrain.CLIFF) {
          blocked[y * size + x] = true
        }
      }
    }
    for (b in buildings) {
      val def = GameDefinitions.buildingDefs.getValue(b.type)
      if (!def.isWalkable) {
        for (dy in 0 until b.footprint) {
          for (dx in 0 until b.footprint) {
            val bx = b.tileX + dx
            val by = b.tileY + dy
            if (bx in 0 until size && by in 0 until size) {
              blocked[by * size + bx] = true
            }
          }
        }
      }
    }
    // Always allow starting tile so a unit grazing an edge can walk out cleanly
    blocked[sy * size + sx] = false

    fun isTargetPerimeter(tx: Int, ty: Int): Boolean {
      if (targetBuilding == null) return false
      return tx >= targetBuilding.tileX - 1 &&
        tx <= targetBuilding.tileX + targetBuilding.footprint &&
        ty >= targetBuilding.tileY - 1 &&
        ty <= targetBuilding.tileY + targetBuilding.footprint &&
        !blocked[ty * size + tx]
    }

    if (targetBuilding == null && sx == gx && sy == gy) return null
    if (targetBuilding != null &&
      isTargetPerimeter(sx, sy) &&
      hasClearLine(startX, startY, goalX, goalY, world, buildings, ignoreBuildingId = -1)
    ) {
      return null
    }

    val goalIsBlocked = blocked[gy * size + gx]

    val gScore = FloatArray(size * size) { Float.POSITIVE_INFINITY }
    val parent = IntArray(size * size) { -1 }
    val closed = BooleanArray(size * size)

    val startIdx = sy * size + sx
    gScore[startIdx] = 0f
    val open = PriorityQueue<Node>()
    open.add(Node(sx, sy, 0f, heuristic(sx, sy, gx, gy)))

    var bestGoalIdx = -1
    var bestFallbackIdx = -1
    var bestFallbackDist = Float.POSITIVE_INFINITY
    var expansions = 0
    val maxExpansions = (size * size).coerceIn(1400, 3600)
    val dirs =
      arrayOf(
        1 to 0, -1 to 0, 0 to 1, 0 to -1,
        1 to 1, 1 to -1, -1 to 1, -1 to -1,
      )

    while (open.isNotEmpty() && expansions < maxExpansions) {
      val cur = open.poll() ?: break
      val cIdx = cur.ty * size + cur.tx
      if (closed[cIdx]) continue
      closed[cIdx] = true
      expansions++

      val distToGoal = heuristic(cur.tx, cur.ty, gx, gy)
      if (cIdx != startIdx && distToGoal < bestFallbackDist) {
        bestFallbackDist = distToGoal
        bestFallbackIdx = cIdx
      }

      if (targetBuilding != null && isTargetPerimeter(cur.tx, cur.ty)) {
        bestGoalIdx = cIdx
        break
      }
      if (cur.tx == gx && cur.ty == gy) {
        bestGoalIdx = cIdx
        break
      }
      if (targetBuilding == null && goalIsBlocked && max(abs(cur.tx - gx), abs(cur.ty - gy)) <= 1) {
        bestGoalIdx = cIdx
        break
      }

      for ((dx, dy) in dirs) {
        val nx = cur.tx + dx
        val ny = cur.ty + dy
        if (nx !in 0 until size || ny !in 0 until size) continue
        val nIdx = ny * size + nx
        if (closed[nIdx] || blocked[nIdx]) continue

        // Prevent diagonal corner cutting across solid obstacles
        if (dx != 0 && dy != 0) {
          if (blocked[cur.ty * size + nx] || blocked[ny * size + cur.tx]) continue
        }

        val stepCost = if (dx != 0 && dy != 0) 1.414f else 1.0f
        val tentativeG = gScore[cIdx] + stepCost
        if (tentativeG < gScore[nIdx]) {
          gScore[nIdx] = tentativeG
          parent[nIdx] = cIdx
          open.add(Node(nx, ny, tentativeG, tentativeG + heuristic(nx, ny, gx, gy)))
        }
      }
    }

    val chosenEndIdx = if (bestGoalIdx != -1) bestGoalIdx else bestFallbackIdx
    if (chosenEndIdx == -1 || chosenEndIdx == startIdx) return null

    // Reconstruct full tile chain from start -> chosenEndIdx
    val chain = ArrayList<Int>()
    var curr = chosenEndIdx
    while (curr != -1 && curr != startIdx) {
      chain.add(curr)
      curr = parent[curr]
    }
    if (chain.isEmpty()) return null
    chain.reverse()

    // String-pulling: pick the furthest tile center along the path with unobstructed line-of-sight
    for (i in chain.indices.reversed()) {
      val idx = chain[i]
      val wx = (idx % size) + 0.5f
      val wy = (idx / size) + 0.5f
      if (hasClearLine(startX, startY, wx, wy, world, buildings, ignoreBuildingId = -1)) {
        return wx to wy
      }
    }

    // Fallback to immediate next tile center
    val firstIdx = chain.first()
    return ((firstIdx % size) + 0.5f) to ((firstIdx / size) + 0.5f)
  }

  private fun heuristic(x1: Int, y1: Int, x2: Int, y2: Int): Float {
    val dx = abs(x1 - x2)
    val dy = abs(y1 - y2)
    return (dx + dy) + (1.414f - 2f) * min(dx, dy)
  }
}
