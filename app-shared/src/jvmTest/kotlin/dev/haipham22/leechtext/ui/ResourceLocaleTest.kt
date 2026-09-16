package dev.haipham22.leechtext.ui

import androidx.compose.ui.ImageComposeScene
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.history_title
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.stringResource
import kotlin.test.Test
import kotlin.test.assertEquals

/** Shim ProvideAppLanguage (reflection) phải đổi được ngôn ngữ stringResource theo state. */
class ResourceLocaleTest {
    @Test
    fun provideAppLanguageResolvesByLanguage() = runBlocking {
        assertEquals("History", renderTitle("en"))
        assertEquals("Lịch sử", renderTitle("vi"))
    }

    private suspend fun renderTitle(language: String): String {
        var captured = ""
        val scene =
            ImageComposeScene(width = 200, height = 50) {
                ProvideAppLanguage(language) {
                    androidx.compose.foundation.layout.Box {
                        captured = stringResource(Res.string.history_title)
                    }
                }
            }
        // vài frame cho composition + resource load xong
        repeat(5) {
            scene.render(it * 16_000_000L)
            delay(50)
        }
        scene.close()
        return captured
    }
}
