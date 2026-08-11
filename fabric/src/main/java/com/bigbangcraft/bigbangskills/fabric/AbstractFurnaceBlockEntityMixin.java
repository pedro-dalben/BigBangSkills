package com.bigbangcraft.bigbangskills.fabric.mixin;

import com.bigbangcraft.bigbangskills.fabric.FabricBootstrap;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {
    @Inject(method = "serverTick", at = @At("TAIL"))
    private static void bigbangskills$secondSmelt(net.minecraft.world.level.Level level,
                                                   net.minecraft.core.BlockPos pos,
                                                   net.minecraft.world.level.block.state.BlockState state,
                                                   AbstractFurnaceBlockEntity furnace,
                                                   CallbackInfo callback) {
        FabricBootstrap.processSecondSmelt(furnace);
    }

    @Inject(method = "awardUsedRecipesAndPopExperience", at = @At("HEAD"))
    private void bigbangskills$recordSmelting(ServerPlayer player, CallbackInfo callback) {
        FabricBootstrap.beginSmeltingXp((AbstractFurnaceBlockEntity) (Object) this);
        var output = ((Container) (Object) this).getItem(2);
        if (!output.isEmpty()) FabricBootstrap.recordSmelting(player, output);
    }

    @Inject(method = "awardUsedRecipesAndPopExperience", at = @At("RETURN"))
    private void bigbangskills$clearSmeltingContext(ServerPlayer player, CallbackInfo callback) {
        FabricBootstrap.endSmeltingXp();
    }

    @Inject(method = "getBurnDuration", at = @At("RETURN"), cancellable = true)
    private void bigbangskills$fuelEfficiency(ItemStack fuel, CallbackInfoReturnable<Integer> callback) {
        callback.setReturnValue(FabricBootstrap.smeltingFuelDuration((AbstractFurnaceBlockEntity) (Object) this, callback.getReturnValue()));
    }

}
