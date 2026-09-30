package com.example

import com.example.util.PerformanceProfiler
import org.junit.Assert.*
import org.junit.Test

class PerformanceProfilerTest {

  @Test
  fun testPerformanceReportGeneration() {
    PerformanceProfiler.recordAppLaunch()
    PerformanceProfiler.recordFirstFrame()

    val reportDtx = PerformanceProfiler.generateReport(
      pingMs = 21,
      isDtx = true,
      isDucking = true
    )

    assertNotNull(reportDtx)
    assertEquals(21, reportDtx.audioLatencyMs)
    assertEquals(24, reportDtx.bandwidthKbps)
    assertTrue(reportDtx.isDtxEnabled)
    assertTrue(reportDtx.isGameAudioDuckingActive)
    assertTrue(reportDtx.memoryUsageMb >= 0)

    val reportNonDtx = PerformanceProfiler.generateReport(
      pingMs = 35,
      isDtx = false,
      isDucking = false
    )
    assertEquals(32, reportNonDtx.bandwidthKbps)
    assertFalse(reportNonDtx.isDtxEnabled)
  }
}
