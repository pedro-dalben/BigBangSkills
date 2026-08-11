package com.bigbangcraft.bigbangskills.neoforge.mixin;

import com.bigbangcraft.bigbangskills.neoforge.NeoForgeBootstrap;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void bigbangskills$recordOrigin(CallbackInfo callback) {
        NeoForgeBootstrap.recordArrowOrigin((AbstractArrow) (Object) this);
    }

    @Inject(method = "onHitBlock", at = @At("HEAD"), cancellable = true)
    private void bigbangskills$trickShot(BlockHitResult hit, CallbackInfo callback) {
        if (NeoForgeBootstrap.tryTrickShot((AbstractArrow) (Object) this, hit)) callback.cancel();
    }
}
