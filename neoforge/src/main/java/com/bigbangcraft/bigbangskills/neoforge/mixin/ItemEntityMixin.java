package com.bigbangcraft.bigbangskills.neoforge.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void bigbangskills$protectDisarmedItem(Player player, CallbackInfo callback) {
        if (player instanceof ServerPlayer serverPlayer
                && ((ItemEntity) (Object) this).getTags().contains("bigbangskills_disarm_owner_" + serverPlayer.getUUID())) {
            return;
        }
        if (player instanceof ServerPlayer
                && ((ItemEntity) (Object) this).getTags().stream().anyMatch(tag -> tag.startsWith("bigbangskills_disarm_owner_"))) {
            callback.cancel();
        }
    }
}
