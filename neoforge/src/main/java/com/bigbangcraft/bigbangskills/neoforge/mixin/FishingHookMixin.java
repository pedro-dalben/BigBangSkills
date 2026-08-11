package com.bigbangcraft.bigbangskills.neoforge.mixin;

import com.bigbangcraft.bigbangskills.neoforge.NeoForgeBootstrap;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
    @Shadow private Entity hookedIn;
    @Shadow private int timeUntilLured;
    @Shadow private int timeUntilHooked;
    @Unique private int bigbangskills$lastLured;
    @Unique private int bigbangskills$lastHooked;
    @Unique private Entity bigbangskills$lastHookedEntity;

    @Inject(method = "tick", at = @At("HEAD"))
    private void bigbangskills$masterAngler(CallbackInfo callback) {
        var reduction = NeoForgeBootstrap.fishingWaitReduction((FishingHook) (Object) this);
        if (timeUntilLured > 0 && reduction[2] > 0 && (bigbangskills$lastLured == 0 || timeUntilLured > bigbangskills$lastLured)) timeUntilLured = Math.max(reduction[2], timeUntilLured - reduction[0]);
        if (timeUntilHooked > 0 && reduction[3] > 0 && (bigbangskills$lastHooked == 0 || timeUntilHooked > bigbangskills$lastHooked)) timeUntilHooked = Math.max(timeUntilLured, Math.max(reduction[3], timeUntilHooked - reduction[1]));
        bigbangskills$lastLured = timeUntilLured;
        bigbangskills$lastHooked = timeUntilHooked;
        NeoForgeBootstrap.iceFishing((FishingHook) (Object) this);
        if (hookedIn != null && hookedIn != bigbangskills$lastHookedEntity && hookedIn instanceof LivingEntity target
                && ((FishingHook) (Object) this).getPlayerOwner() instanceof net.minecraft.server.level.ServerPlayer player) {
            NeoForgeBootstrap.recordShake(player, target);
        }
        bigbangskills$lastHookedEntity = hookedIn;
    }

    @ModifyArg(method = "retrieve", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;<init>(Lnet/minecraft/world/level/Level;DDDI)V"), index = 4)
    private int bigbangskills$boostFishingXp(int vanillaXp) {
        return NeoForgeBootstrap.modifyFishingXp((FishingHook) (Object) this, vanillaXp);
    }
}
