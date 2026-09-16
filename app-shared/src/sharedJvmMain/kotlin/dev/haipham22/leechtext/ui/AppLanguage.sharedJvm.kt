package dev.haipham22.leechtext.ui

import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ProvidedValue
import java.lang.reflect.Proxy

/**
 * Actual JVM/Android — shim reflection (nguyên văn từ ResourceLocale.kt cũ).
 *
 * ponytail: CMP 1.8–1.12 chưa có API public override locale string resources —
 * LocalComposeEnvironment / ResourceEnvironment / ctor đều `internal` trong source
 * (javap xác nhận public ở bytecode, ổn định 1.8→1.12). Khi CMP thêm API public
 * (issue compose-multiplatform #4571) thay bằng CompositionLocalProvider thẳng.
 */
internal actual fun appLanguageProviders(language: String): Array<ProvidedValue<*>> {
    val environment =
        Proxy.newProxyInstance(
            cls("ComposeEnvironment").classLoader,
            arrayOf(cls("ComposeEnvironment")),
        ) { proxy, method, args ->
            when (method.name) {
                // Proxy bắt MỌI method kể cả equals/hashCode của Object — Compose gọi
                // areEqual(old, new) khi đổi locale, handler trả ResourceEnvironment
                // cho equals → IllegalArgumentException → app văng (dogfood 260902)
                "equals" -> args?.get(0) === proxy

                "hashCode" -> System.identityHashCode(proxy)

                "toString" -> "ComposeEnvironment(shim, lang=$language)"

                else -> newResourceEnvironment(language)
            }
        }
    return arrayOf(provideRaw(composeEnvironmentLocal, environment))
}

private const val PKG = "org.jetbrains.compose.resources"

private fun cls(name: String): Class<*> = Class.forName("$PKG.$name")

/** LocalComposeEnvironment internal — lấy instance qua reflection (getter public ở bytecode). */
private val composeEnvironmentLocal: CompositionLocal<*> by lazy {
    cls("ResourceEnvironmentKt").getDeclaredMethod("getLocalComposeEnvironment").invoke(null) as CompositionLocal<*>
}

/** ResourceEnvironment(language, script, region, theme, density) — ctor internal, bytecode public.
 *  Compose 1.12 thêm ScriptQualifier (tham số 2) — 1.8 không có. */
private fun newResourceEnvironment(language: String): Any {
    val lq = cls("LanguageQualifier")
    val sq = cls("ScriptQualifier")
    val rq = cls("RegionQualifier")
    val tq = cls("ThemeQualifier")
    val dq = cls("DensityQualifier")
    return cls("ResourceEnvironment").getDeclaredConstructor(lq, sq, rq, tq, dq).newInstance(
        lq.getConstructor(String::class.java).newInstance(if (language == "en") "en" else "vi"),
        sq.getConstructor(String::class.java).newInstance(""),
        rq.getConstructor(String::class.java).newInstance(""),
        tq.getMethod("valueOf", String::class.java).invoke(null, "LIGHT"),
        dq.getMethod("valueOf", String::class.java).invoke(null, "MDPI"),
    )
}

/** provides cho CompositionLocal<*> — ComposeEnvironment internal nên không gọi provides trực tiếp. */
@Suppress("UNCHECKED_CAST")
private fun provideRaw(
    local: CompositionLocal<*>,
    value: Any?,
): ProvidedValue<*> = (local as ProvidableCompositionLocal<Any?>).provides(value)
