package com.example.game.render

import androidx.compose.ui.geometry.Offset
import com.example.game.model.CameraState

object IsometricMath {

  const val BASE_TILE_W = 148f
  const val BASE_TILE_H = 74f

  fun worldToScreen(
    wx: Float,
    wy: Float,
    camera: CameraState,
    viewportWidth: Float,
    viewportHeight: Float,
  ): Offset {
    val halfW = BASE_TILE_W * 0.5f * camera.zoom
    val halfH = BASE_TILE_H * 0.5f * camera.zoom
    val dx = wx - camera.centerWorldX
    val dy = wy - camera.centerWorldY
    val sx = viewportWidth * 0.5f + (dx - dy) * halfW
    val sy = viewportHeight * 0.5f + (dx + dy) * halfH
    return Offset(sx, sy)
  }

  fun screenToWorld(
    sx: Float,
    sy: Float,
    camera: CameraState,
    viewportWidth: Float,
    viewportHeight: Float,
  ): Offset {
    val halfW = BASE_TILE_W * 0.5f * camera.zoom
    val halfH = BASE_TILE_H * 0.5f * camera.zoom
    val rx = (sx - viewportWidth * 0.5f) / halfW
    val ry = (sy - viewportHeight * 0.5f) / halfH
    val dx = (rx + ry) * 0.5f
    val dy = (ry - rx) * 0.5f
    return Offset(camera.centerWorldX + dx, camera.centerWorldY + dy)
  }

  fun screenDeltaToWorldDelta(
    dsx: Float,
    dsy: Float,
    zoom: Float,
  ): Offset {
    val halfW = BASE_TILE_W * 0.5f * zoom
    val halfH = BASE_TILE_H * 0.5f * zoom
    val rx = dsx / halfW
    val ry = dsy / halfH
    val dwx = (rx + ry) * 0.5f
    val dwy = (ry - rx) * 0.5f
    return Offset(dwx, dwy)
  }
}
