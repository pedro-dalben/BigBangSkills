package com.bigbangcraft.bigbangskills.fabric.mixin;

import com.bigbangcraft.bigbangskills.fabric.FabricBootstrap;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fabric equivalent of NeoForge's mutable incoming-damage event. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Unique private ItemStack bigbangskills$consumedItem = ItemStack.EMPTY;

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float bigbangskills$modifyPetDamage(float amount, DamageSource source) {
        return FabricBootstrap.modifyIncomingDamage((LivingEntity) (Object) this, source, amount);
    }

    @Inject(method = "completeUsingItem()V", at = @At("HEAD"))
    private void bigbangskills$captureFood(CallbackInfo callback) {
        bigbangskills$consumedItem = ((LivingEntity) (Object) this).getUseItem().copy();
    }

    @Inject(method = "completeUsingItem()V", at = @At("RETURN"))
    private void bigbangskills$applyFishermanDiet(CallbackInfo callback) {
        var entity = (LivingEntity) (Object) this;
        if (entity instanceof ServerPlayer player) FabricBootstrap.recordFood(player, bigbangskills$consumedItem);
        bigbangskills$consumedItem = ItemStack.EMPTY;
    }
}
