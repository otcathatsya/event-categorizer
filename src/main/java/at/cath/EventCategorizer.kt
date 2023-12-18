package at.cath

import com.mojang.serialization.Codec
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtOps
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

    @JvmField
    val CONFIG_PATH: Path = Path.of("event_items.json")
    private val NBT_LIST_CODEC = Codec.list(EventItemStack.CODEC)

    @JvmField
    val HIGHLIGHT = ColorHelper.Argb.getArgb(200, 82, 176, 97)
    val eventItems: MutableSet<EventItemStack> = HashSet()

    val storeKeyBind: KeyBinding = KeyBindingHelper.registerKeyBinding(
        KeyBinding(
            "action.eventcat.store",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F1,
            "menu.eventcat.category"
        )
    )

    override fun onInitialize() {
        val file = CONFIG_PATH.toFile()

        ClientLifecycleEvents.CLIENT_STOPPING.register(ClientLifecycleEvents.ClientStopping {
            logger.info("Saving scanned event items to file")

            Util.getResult(NBT_LIST_CODEC.encodeStart(NbtOps.INSTANCE, eventItems.toMutableList())) { err ->
                IOException(err).also { logger.error("Failed to encode event items", it) }
            }.apply {
                val itemsCompound = NbtCompound()
                itemsCompound.put("event_items", this)
                NbtIo.writeCompressed(itemsCompound, file)
            }
        })

        ClientLifecycleEvents.CLIENT_STARTED.register(ClientLifecycleEvents.ClientStarted {
            logger.info("Loading scanned event items from file")

            if (!CONFIG_PATH.exists())
                CONFIG_PATH.createFile()

            if (CONFIG_PATH.isEmpty())
                return@ClientStarted

            eventItems.addAll(
                Util.getResult(
                    NBT_LIST_CODEC.parse(
                        NbtOps.INSTANCE, NbtIo.readCompressed(file).getList(
                            "event_items",
                            NbtElement.COMPOUND_TYPE.toInt()
                        )
                    )
                ) { err -> IOException(err).also { logger.error("Failed to parse event items", it) } })
        })
    }

    private fun Path.isEmpty(): Boolean = Files.size(this) == 0L
}