package dev.haipham22.leechtext.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.plugin.PluginManager
import dev.haipham22.leechtext.plugin.PluginUpdate
import dev.haipham22.leechtext.plugin.RepositoryManager
import dev.haipham22.leechtext.ui.library.LibraryState
import dev.haipham22.leechtext.ui.queue.DownloadQueueState
import dev.haipham22.leechtext.ui.sources.PluginState

/**
 * Component gốc — sở hữu tab hiện tại + 3 state app-scope. Mỗi state 1 child
 * context riêng (StateKeeper key DefaultChildStack không đụng nhau). States là
 * owner-created (pattern Decompose chính thống); logger + engine deps inject từ
 * platform entry (Koin global context) xuống toàn bộ state tree qua constructor.
 */
@Suppress("LongParameterList") // Decompose root — deps từ Koin entry
class RootComponent(
    componentContext: ComponentContext,
    initialTab: Int = 0,
    val log: EngineLogger,
    val pluginManager: PluginManager,
    val pluginUpdate: PluginUpdate,
    val repositoryManager: RepositoryManager,
) : ComponentContext by componentContext {
    private val libraryCtx = componentContext.childContext("library")
    private val pluginCtx = componentContext.childContext("plugin")
    private val queueCtx = componentContext.childContext("queue")

    val library = LibraryState(libraryCtx, log, pluginManager)
    val plugin = PluginState(pluginCtx, log, pluginManager, pluginUpdate, repositoryManager)
    val queue = DownloadQueueState(queueCtx, log, pluginManager)

    var tab by mutableIntStateOf(initialTab)
}
