package com.example.util

object PerformanceProfiler {
  private const val TAG = "PerformanceProfiler"

  private var appLaunchTimestamp: Long = System.currentTimeMillis()
  private var firstFrameTimestamp: Long = 0

  data class PerformanceReport(
    val coldStartMs: Long,
    val memoryUsageMb: Long,
    val audioLatencyMs: Int,
    val bandwidthKbps: Int,
    val isDtxEnabled: Boolean,
    val isGameAudioDuckingActive: Boolean
  )

  fun recordAppLaunch() {
    appLaunchTimestamp = System.currentTimeMillis()
  }

  fun recordFirstFrame() {
    if (firstFrameTimestamp == 0L) {
      firstFrameTimestamp = System.currentTimeMillis()
    }
  }

  fun getColdStartDurationMs(): Long {
    return if (firstFrameTimestamp > 0) {
      firstFrameTimestamp - appLaunchTimestamp
    } else {
      System.currentTimeMillis() - appLaunchTimestamp
    }
  }

  fun getRuntimeMemoryMb(): Long {
    val runtime = Runtime.getRuntime()
    val usedBytes = runtime.totalMemory() - runtime.freeMemory()
    return usedBytes / (1024 * 1024)
  }

  fun generateReport(
    pingMs: Int,
    isDtx: Boolean,
    isDucking: Boolean
  ): PerformanceReport {
    return PerformanceReport(
      coldStartMs = getColdStartDurationMs(),
      memoryUsageMb = getRuntimeMemoryMb(),
      audioLatencyMs = pingMs,
      bandwidthKbps = if (isDtx) 24 else 32,
      isDtxEnabled = isDtx,
      isGameAudioDuckingActive = isDucking
    )
  }
}
