package dev.haipham22.leechtext

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume

/** ComponentContext cho test — lifecycle resumed ngay, không Compose. */
fun testComponentContext(): DefaultComponentContext = DefaultComponentContext(LifecycleRegistry().apply { resume() })
