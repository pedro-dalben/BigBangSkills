package com.bigbangcraft.bigbangskills.fabric.mixin;

import com.bigbangcraft.bigbangskills.fabric.FabricBootstrap;

import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Fabric has no fishing-result event; retrieve is the vanilla authoritative boundary. */
@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
    @Shadow private Entity hookedIn;
    @Shadow private int timeUntilLured;
    @Shadow private int timeUntilHooked;
    @Unique private int bigbangskills$lastLured;
    @Unique private int bigbangskills$lastHooked;
    @Unique private ItemStack bigbangskills$firstCatch;
    @Unique private Entity bigbangskills$lastHookedEntity;

    @Inject(method = "tick", at = @At("HEAD"))
    private void bigbangskills$masterAngler(CallbackInfo callback) {
        var reduction = FabricBootstrap.fishingWaitReduction((FishingHook) (Object) this);
        if (timeUntilLured > 0 && reduction[2] > 0 && (bigbangskills$lastLured == 0 || timeUntilLured > bigbangskills$lastLured)) timeUntilLured = Math.max(reduction[2], timeUntilLured - reduction[0]);
        if (timeUntilHooked > 0 && reduction[3] > 0 && (bigbangskills$lastHooked == 0 || timeUntilHooked > bigbangskills$lastHooked)) timeUntilHooked = Math.max(timeUntilLured, Math.max(reduction[3], timeUntilHooked - reduction[1]));
        bigbangskills$lastLured = timeUntilLured;
        bigbangskills$lastHooked = timeUntilHooked;
        FabricBootstrap.iceFishing((FishingHook) (Object) this);
        if (hookedIn != null && hookedIn != bigbangskills$lastHookedEntity && hookedIn instanceof LivingEntity target
                && ((FishingHook) (Object) this).getPlayerOwner() instanceof net.minecraft.server.level.ServerPlayer player) {
            FabricBootstrap.recordShake(player, target);
        }
        bigbangskills$lastHookedEntity = hookedIn;
    }

    @ModifyArg(method = "retrieve", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;<init>(Lnet/minecraft/world/level/Level;DDDI)V"), index = 4)
    private int bigbangskills$boostFishingXp(int vanillaXp) {
        return FabricBootstrap.modifyFishingXp((FishingHook) (Object) this, vanillaXp);
    }

    @ModifyArg(method = "retrieve", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V"), index = 4)
    private ItemStack bigbangskills$captureCatch(ItemStack stack) {
        if (bigbangskills$firstCatch == null) bigbangskills$firstCatch = stack.copy();
        return FabricBootstrap.prepareFishingCatch((FishingHook) (Object) this, stack);
    }

    @Inject(method = "retrieve", at = @At("RETURN"))
    private void bigbangskills$recordFishing(ItemStack rod, CallbackInfoReturnable<Integer> callback) {
        var hook = (FishingHook) (Object) this;
        var player = hook.getPlayerOwner();
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer && callback.getReturnValue() > 0 && bigbangskills$firstCatch != null)
            FabricBootstrap.recordFishing(serverPlayer, hook, bigbangskills$firstCatch);
        bigbangskills$firstCatch = null;
    }
}
