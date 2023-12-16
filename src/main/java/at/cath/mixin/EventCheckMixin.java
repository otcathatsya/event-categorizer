package at.cath.mixin;

import at.cath.EventCategorizer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

import static at.cath.EventCategorizer.HIGHLIGHT;


@Mixin(HandledScreen.class)
public abstract class EventCheckMixin {

    @Shadow
    @Final
    protected ScreenHandler handler;

    @Shadow
    protected Slot focusedSlot;

    @Shadow
    protected int x;
    @Shadow
    protected int y;

    @Unique
    private static final EventCategorizer INSTANCE = EventCategorizer.INSTANCE;

    @Inject(method = "render", at = @At("TAIL"))
    public void checkEventItems(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        RenderSystem.disableDepthTest();
        context.getMatrices().push();
        context.getMatrices().translate((float) this.x, (float) this.y, 0.0F);

        for (int i = 0; i < handler.slots.size(); ++i) {
            final Slot slot = handler.slots.get(i);
            final ItemStack targetStack = slot.getStack();

            if (targetStack.isEmpty() || targetStack.getNbt() == null)
                continue;

            final NbtCompound nbt = targetStack.getNbt();

            if (INSTANCE.getEventItems().contains(nbt)) {
                context.fillGradient(RenderLayer.getGuiOverlay(), slot.x, slot.y, slot.x + 16, slot.y + 16, HIGHLIGHT, HIGHLIGHT, 0);
            }
        }

        context.getMatrices().pop();
        RenderSystem.enableDepthTest();
    }

    @Inject(method = "keyPressed", at = @At("TAIL"))
    public void onEventKey(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (focusedSlot == null || !focusedSlot.hasStack())
            return;

        if (EventCategorizer.INSTANCE.getStoreKeyBind().matchesKey(keyCode, scanCode)) {
            final ItemStack stack = focusedSlot.getStack();
            if (stack.isEmpty() || stack.getNbt() == null)
                return;

            final Set<NbtCompound> eventItems = INSTANCE.getEventItems();

            final NbtCompound nbt = stack.getNbt();
            if (eventItems.contains(nbt)) {
                eventItems.remove(nbt);
            } else {
                eventItems.add(nbt);
            }
        }
    }
}