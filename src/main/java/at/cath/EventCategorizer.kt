package at.cath

import com.google.gson.JsonElement
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.data.DataProvider
import net.minecraft.data.DataWriter
import net.minecraft.datafixer.fix.BlockEntitySignTextStrictJsonFix.GSON
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.Util
import net.minecraft.util.math.ColorHelper
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.lwjgl.glfw.GLFW
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.exists


object EventCategorizer : ModInitializer {

    private const val MOD_ID = "event-categorizer"
    private val logger: Logger = LogManager.getLogger(MOD_ID)

    private val CONFIG_PATH: Path = Path.of("event_items.json")
    private val NBT_LIST_CODEC = Codec.list(NbtCompound.CODEC)

    @JvmField
    val HIGHLIGHT = ColorHelper.Argb.getArgb(200, 82, 176, 97)
    val eventItems: MutableSet<NbtCompound> = HashSet()

    val storeKeyBind: KeyBinding = KeyBindingHelper.registerKeyBinding(
        KeyBinding(
            "action.eventcat.store",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F1,
            "menu.eventcat.category"
        )
    )

    override fun onInitialize() {
        logger.info("EventCategorizer loaded")
        ClientLifecycleEvents.CLIENT_STOPPING.register(ClientLifecycleEvents.ClientStopping {
            logger.info("Saving scanned event items to file")
            DataProvider.writeCodecToPath(
                DataWriter.UNCACHED,
                NBT_LIST_CODEC,
                eventItems.toMutableList(),
                CONFIG_PATH.toAbsolutePath()
            )
        })

        ClientLifecycleEvents.CLIENT_STARTED.register(ClientLifecycleEvents.ClientStarted {
            logger.info("Loading scanned event items from file")

            if (!CONFIG_PATH.exists())
                CONFIG_PATH.createFile()

            if (CONFIG_PATH.isEmpty())
                return@ClientStarted

            eventItems.addAll(Util.getResult(
                NBT_LIST_CODEC.parse(
                    JsonOps.INSTANCE, GSON.fromJson(
                        Files.newBufferedReader(CONFIG_PATH),
                        JsonElement::class.java
                    )
                )
            ) { err -> IOException(err) })
        })
    }

    private fun Path.isEmpty(): Boolean = Files.size(this) == 0L
}