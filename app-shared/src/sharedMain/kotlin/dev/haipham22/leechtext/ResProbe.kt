package dev.haipham22.leechtext

import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.ci_res_probe_p50
import dev.haipham22.leechtext.resources.sources_no_match

// Probe CI bug accessors Res.string (docs/known-issues-compose-resources.md):
// file MỚI reference key MỚI (ci_res_probe_p50) + key CŨ (sources_no_match).
// Stack cũ (Kotlin 2.1/Compose 1.8) fail "Unresolved reference" trên máy CI sạch.
// Giữ nguyên nếu CI xanh — đây là canary regression cho bug đó.
@Suppress("unused")
val ciResProbeNew = Res.string.ci_res_probe_p50

@Suppress("unused")
val ciResProbeOld = Res.string.sources_no_match
