package dev.haipham22.leechtext.plugin.vbook

import dev.haipham22.leechtext.log.platformEngineLogger
import dev.haipham22.leechtext.plugin.PluginManager
import kotlin.test.Test

/**
 * One-off: tải 1 plugin vBook thật, dump mọi field của VBookPluginEntity
 * + PluginEntity sau convert — xem field nào bị rơi/unused.
 * Chạy: ./gradlew :engine:jvmTest --tests "*FieldDump*"
 */
class PluginFieldDumpTest {
    @Test
    fun dumpRealPluginFields() {
        val service = VBookPluginService(platformEngineLogger(), pluginManager = PluginManager(platformEngineLogger()))
        val extensions = service.getAvailablePlugins()
        println("=== REGISTRY: ${extensions.size} extensions — lấy 1 ===")
        val ext = extensions.first { !it.path.isNullOrEmpty() && !it.name.isNullOrEmpty() }
        println("Extension: name=${ext.name} author=${ext.author} path=${ext.path}")
        println("  source=${ext.source} version=${ext.version} type=${ext.type}")
        println("  locale=${ext.locale} tag=${ext.tag} icon=${ext.icon?.take(60)}")
        println("  description=${ext.description?.take(80)}")

        // Download + extract raw entity (chưa convert)
        val extractor = PluginZipExtractor(platformEngineLogger())
        val vbook = extractor.extractFromZip(ext.path!!)
        println("\n=== VBOOK PLUGIN ENTITY (sau extract zip) ===")
        println("name=${vbook.name}")
        println("author=${vbook.author}")
        println("version=${vbook.version}")
        println("source=${vbook.source}")
        println("regexp=${vbook.regexp?.take(60)}")
        println("description=${vbook.description?.take(80)}")
        println("locale=${vbook.locale}")
        println("type=${vbook.type}")
        println("language=${vbook.language}") // ← field riêng của vbook
        println("priority=${vbook.priority}") // ← bị bỏ khi convert
        println("tag=${vbook.tag}") // ← bị bỏ khi convert
        println("scripts=${vbook.scripts.keys}")
        println("iconBase64=${vbook.iconBase64?.take(50)}")
        println("config keys=${vbook.config.keys}")
        vbook.config.forEach { (k, spec) ->
            println("  config[$k]: title=${spec.title} mode=${spec.mode} default=${spec.default} values=${spec.values}")
        }
        println("rawMetadata length=${vbook.rawMetadata?.length}")

        println("\n=== SAU CONVERT → PluginEntity ===")
        val converted = VBookToLeechTextConverter().convert(vbook)
        println("name=${converted.name} version=${converted.version} language=${converted.language}")
        println("group=${converted.group} icon(base64)=${converted.icon?.take(40)}")
        println("describe=${converted.describe?.take(60)}")
        println("config keys=${converted.config?.keys}")
        println("configSpec keys=${converted.configSpec?.keys}")

        println("\n=== PHÂN TÍCH RAW plugin.json — keys không map ===")
        vbook.rawMetadata?.let { raw ->
            // Parse keys thô bằng regex (không cần Gson cho demo)
            val keys = Regex("\"([a-zA-Z_]+)\"\\s*:").findAll(raw).map { it.groupValues[1] }.toSet()
            println("raw keys: $keys")
        }
    }
}
