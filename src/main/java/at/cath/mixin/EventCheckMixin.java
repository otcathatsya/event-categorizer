package at.cath.mixin;

import at.cath.EventCategorizer;
import at.cath.EventItemStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
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
            final ItemStack stack = slot.getStack();

            if (stack.isEmpty() || stack.getNbt() == null)
                continue;

            if (INSTANCE.getEventItems().contains(new EventItemStack(stack.getItem(), stack.getNbt()))) {
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

            final Set<EventItemStack> eventItems = INSTANCE.getEventItems();

            final EventItemStack eventStack = new EventItemStack(stack.getItem(), stack.getNbt());
            if (eventItems.contains(eventStack)) {
                eventItems.remove(eventStack);
            } else {
                eventItems.add(eventStack);
            }
        }
    }
}