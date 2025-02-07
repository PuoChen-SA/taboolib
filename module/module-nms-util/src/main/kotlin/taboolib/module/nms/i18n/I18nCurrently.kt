@file:Suppress("KDocUnresolvedReference", "DEPRECATION")
package taboolib.module.nms.i18n

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import taboolib.common.TabooLibCommon
import taboolib.module.nms.getInternalName
import java.io.File
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.Executors

/**
 * https://launchermeta.mojang.com/mc/game/version_manifest.json -> [version] -> assetIndex ->
 *
 * @author sky
 * @since 2020-04-04 19:51
 */
object I18nCurrently : I18nBase() {

    val locales = arrayOf(
        arrayOf("zh_cn", "1d1291c554a9bdfce7abd4b40207c82f8f9f1a6f"),
        arrayOf("zh_tw", "0587a4c3de3f56cba952fb92453fda612c94d1df"),
        arrayOf("en_gb", "d72da85adc86e9c31ea84a45ef89ecd9f421f23b")
    )

    private val cache: MutableMap<String, JsonObject> = HashMap()
    private val folder = File("assets")
    private val executor = Executors.newSingleThreadExecutor()

    fun load() {
        locales.forEach {
            val file = File(folder, "${it[1].substring(0, 2)}/${it[1]}")
            if (file.exists()) {
                val jsonObject = JsonParser().parse(file.readText(StandardCharsets.UTF_8)).asJsonObject
                if (jsonObject.size() > 0) {
                    cache[it[0]] = jsonObject
                }
            }
        }
    }

    override fun init() {
        executor.submit {
            load()
            if (cache.isEmpty()) {
                TabooLibCommon.print("Loading language files, please wait...")
                try {
                    locales.forEach {
                        val file = File(folder, "${it[1].substring(0, 2)}/${it[1]}")
                        if (!file.parentFile.exists()) {
                            file.parentFile.mkdirs()
                        }
                        file.createNewFile()
                        file.writeBytes(URL("https://resources.download.minecraft.net/" + it[1].substring(0, 2) + "/" + it[1]).openStream().readBytes())
                    }
                    load()
                } catch (ex: Throwable) {
                    ex.printStackTrace()
                }
            }
        }
    }

    override fun getName(player: Player?, entity: Entity): String {
        if (cache.isEmpty()) {
            return "[LOADING]"
        }
        val locale = getLocale(player) ?: return "[NO LOCALE:${player?.locale ?: "zh_cn (default)"}]"
        val element = locale[entity.getInternalName()]
        return if (element == null) entity.name else element.asString
    }

    override fun getName(player: Player?, itemStack: ItemStack): String {
        if (cache.isEmpty()) {
            return "[LOADING]"
        }
        val locale = getLocale(player) ?: return "[NO LOCALE:${player?.locale ?: "zh_cn (default)"}]"
        val element = locale[itemStack.getInternalName()]
        return if (element == null) itemStack.type.name.lowercase(Locale.getDefault()).replace("_", "") else element.asString
    }

    override fun getName(player: Player?, enchantment: Enchantment): String {
        if (cache.isEmpty()) {
            return "[LOADING]"
        }
        val locale = getLocale(player) ?: return "[NO LOCALE:${player?.locale ?: "zh_cn (default)"}]"
        val element = locale[enchantment.getInternalName()]
        return if (element == null) enchantment.name else element.asString
    }

    override fun getName(player: Player?, potionEffectType: PotionEffectType): String {
        if (cache.isEmpty()) {
            return "[LOADING]"
        }
        val locale = getLocale(player) ?: return "[NO LOCALE:${player?.locale ?: "zh_cn (default)"}]"
        val element = locale[potionEffectType.getInternalName()]
        return if (element == null) potionEffectType.name else element.asString
    }

    private fun getLocale(player: Player?): JsonObject? {
        // 获取玩家语言
        // 若玩家语言在支持范围外，则使用中文
        return cache[player?.locale ?: "zh_cn"] ?: cache["zh_cn"]
    }
}