package com.bigbangcraft.bigbangskills.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Animal.class)
public abstract class AnimalMixin {
    @Inject(method = "spawnChildFromBreeding", at = @At("HEAD"), cancellable = true)
    private void bigbangskills$preventSummonedBreeding(ServerLevel level, Animal partner, CallbackInfo callback) {
        var animal = (Animal) (Object) this;
        if (animal.getTags().contains("bigbangskills_cotw_no_breed") || partner.getTags().contains("bigbangskills_cotw_no_breed")) {
            animal.resetLove();
            partner.resetLove();
            callback.cancel();
        }
    }
}
