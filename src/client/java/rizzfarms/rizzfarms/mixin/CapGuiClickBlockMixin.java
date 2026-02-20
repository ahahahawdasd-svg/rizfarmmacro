package rizzfarms.rizzfarms.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rizzfarms.rizzfarms.gui.CapGuiAssist;

@Mixin(AbstractContainerScreen.class)
public abstract class CapGuiClickBlockMixin {

    @Shadow protected Slot hoveredSlot;

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void rizz$blockWrong(net.minecraft.world.inventory.Slot slot, int slotId, int button, net.minecraft.world.inventory.ClickType type, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        // Block slot click if CapGuiAssist deems it incorrect
        if (!CapGuiAssist.isClickAllowed((Screen)(Object)this, slot)) {
            ci.cancel();
        }
    }
}
