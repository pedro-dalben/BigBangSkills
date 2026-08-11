package com.bigbangcraft.bigbangskills.fabric.mixin;

import com.bigbangcraft.bigbangskills.fabric.FabricBootstrap;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
    @Inject(method = "addItem", at = @At("HEAD"), cancellable = true)
    private static void bigbangskills$filterAlchemyTransfer(Container source, Container destination, ItemStack stack,
                                                             Direction direction, CallbackInfoReturnable<ItemStack> callback) {
        if (destination instanceof BrewingStandBlockEntity && !FabricBootstrap.allowAlchemyHopperTransfer(stack)) callback.setReturnValue(stack);
    }
}
