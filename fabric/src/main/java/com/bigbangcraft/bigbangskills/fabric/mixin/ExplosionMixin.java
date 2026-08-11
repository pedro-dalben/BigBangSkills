package com.bigbangcraft.bigbangskills.fabric.mixin;

import com.bigbangcraft.bigbangskills.fabric.FabricBootstrap;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Explosion.class)
public abstract class ExplosionMixin {
    @Inject(method = "finalizeExplosion", at = @At("HEAD"))
    private void bigbangskills$blastMining(boolean particles, CallbackInfo callback) {
        FabricBootstrap.processBlastExplosion((Explosion) (Object) this);
    }
}
