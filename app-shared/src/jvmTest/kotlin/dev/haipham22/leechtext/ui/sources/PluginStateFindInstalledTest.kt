package dev.haipham22.leechtext.ui.sources

import dev.haipham22.leechtext.entities.PluginEntity
import dev.haipham22.leechtext.plugin.vbook.model.VBookExtensionEntity
import kotlin.test.Test
import kotlin.test.assertEquals

private const val TRUYEN_FULL_VBOOK_NAME = "Truyện Full (vBook)"
private const val TRUYENFULL_LIVE_SOURCE = "https://truyenfull.live"

/**
 * Bug kho vBook (2026-08-25): 2 extension cùng tên khác source (mirror family
 * truyenfull.today v0.3 / truyenfull.live v1.3) — findInstalled match theo name
 * nên cả 2 row kho map vào 1 plugin → "cài 2 cái khác nhau thành 1".
 * Fix: match source trước, fallback name cho plugin converted cũ không source.
 */
class PluginStateFindInstalledTest {
    private fun installed(
        name: String,
        version: Double,
        source: String?,
    ) = PluginEntity(name = name, version = version, source = source)

    private fun ext(
        name: String,
        source: String?,
    ) = VBookExtensionEntity(name = name, source = source)

    @Test
    fun sameNameDifferentSourceMapsPluginBySource() {
        val state =
            PluginState(
                dev.haipham22.leechtext.testComponentContext(),
                log = dev.haipham22.leechtext.log.platformEngineLogger(),
                pluginManager = dev.haipham22.leechtext.plugin.PluginManager(dev.haipham22.leechtext.log.platformEngineLogger()),
                pluginUpdate =
                dev.haipham22.leechtext.plugin.PluginUpdate(
                    dev.haipham22.leechtext.log.platformEngineLogger(),
                    dev.haipham22.leechtext.plugin.PluginManager(dev.haipham22.leechtext.log.platformEngineLogger()),
                    dev.haipham22.leechtext.plugin.RepositoryManager(dev.haipham22.leechtext.log.platformEngineLogger()),
                ),
                repositoryManager = dev.haipham22.leechtext.plugin.RepositoryManager(dev.haipham22.leechtext.log.platformEngineLogger()),
            )
        state.installed =
            listOf(
                installed(TRUYEN_FULL_VBOOK_NAME, 0.3, "https://truyenfull.today"),
                installed(TRUYEN_FULL_VBOOK_NAME, 1.3, TRUYENFULL_LIVE_SOURCE),
            )

        assertEquals(0.3, state.findInstalled(ext("Truyện Full", "https://truyenfull.today"))?.version)
        assertEquals(1.3, state.findInstalled(ext("Truyện Full", TRUYENFULL_LIVE_SOURCE))?.version)
    }

    @Test
    fun fallbackNameWhenExtensionHasNoSource() {
        val state =
            PluginState(
                dev.haipham22.leechtext.testComponentContext(),
                log = dev.haipham22.leechtext.log.platformEngineLogger(),
                pluginManager = dev.haipham22.leechtext.plugin.PluginManager(dev.haipham22.leechtext.log.platformEngineLogger()),
                pluginUpdate =
                dev.haipham22.leechtext.plugin.PluginUpdate(
                    dev.haipham22.leechtext.log.platformEngineLogger(),
                    dev.haipham22.leechtext.plugin.PluginManager(dev.haipham22.leechtext.log.platformEngineLogger()),
                    dev.haipham22.leechtext.plugin.RepositoryManager(dev.haipham22.leechtext.log.platformEngineLogger()),
                ),
                repositoryManager = dev.haipham22.leechtext.plugin.RepositoryManager(dev.haipham22.leechtext.log.platformEngineLogger()),
            )
        state.installed = listOf(installed(TRUYEN_FULL_VBOOK_NAME, 1.3, TRUYENFULL_LIVE_SOURCE))

        assertEquals(1.3, state.findInstalled(ext(TRUYEN_FULL_VBOOK_NAME, null))?.version)
    }
}
