package at.cath

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.Registries

class EventItemStack(private val item: Item, private val tag: NbtCompound) {
    companion object {
        @JvmStatic
        val CODEC: Codec<EventItemStack> =
            RecordCodecBuilder.create { instance: RecordCodecBuilder.Instance<EventItemStack> ->
                instance.group(
                    Registries.ITEM.codec.fieldOf("id").forGetter { obj: EventItemStack -> obj.item },
                    NbtCompound.CODEC.fieldOf("tag").forGetter { obj: EventItemStack -> obj.tag })
                    .apply(
                        instance
                    ) { item: Item, tag: NbtCompound ->
                        EventItemStack(item, tag)
                    }
            }
    }

    override fun hashCode(): Int {
        var result = item.hashCode()
        result = 31 * result + tag.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EventItemStack
        ItemStack(item).nbt
        if (item != other.item) return false
        if (tag != other.tag) return false

        return true
    }
}