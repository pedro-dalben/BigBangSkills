package com.bigbangcraft.bigbangskills.fabric.mixin;

import com.bigbangcraft.bigbangskills.fabric.FabricBootstrap;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
    @Inject(method = "onTake", at = @At("HEAD"))
    private void bigbangskills$recordRepair(Player player, ItemStack output, CallbackInfo callback) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer && !output.isEmpty() && output.isDamageableItem()) {
            var input = ((AnvilMenu) (Object) this).getSlot(0).getItem().copy();
            FabricBootstrap.recordRepair(serverPlayer, input, output);
        }
    }
}
