package com.bigbangcraft.bigbangskills.fabric.mixin;

import com.bigbangcraft.bigbangskills.fabric.FabricBootstrap;

import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TamableAnimal.class)
public abstract class TamableAnimalMixin {
    @Inject(method = "tame", at = @At("HEAD"))
    private void bigbangskills$recordTaming(Player player, CallbackInfo callback) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            FabricBootstrap.recordTaming((TamableAnimal) (Object) this, serverPlayer);
        }
    }
}
