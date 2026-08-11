package com.bigbangcraft.bigbangskills.neoforge.mixin;

import com.bigbangcraft.bigbangskills.neoforge.NeoForgeBootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.item.ItemStack;
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
        NeoForgeBootstrap.processSecondSmelt(furnace);
    }

    @Inject(method = "awardUsedRecipesAndPopExperience", at = @At("HEAD"))
    private void bigbangskills$recordSmelting(ServerPlayer player, CallbackInfo callback) {
        NeoForgeBootstrap.beginSmeltingXp((AbstractFurnaceBlockEntity) (Object) this);
        var output = ((Container) (Object) this).getItem(2);
        if (!output.isEmpty()) NeoForgeBootstrap.recordSmelting(player, output);
    }

    @Inject(method = "awardUsedRecipesAndPopExperience", at = @At("RETURN"))
    private void bigbangskills$clearSmeltingContext(ServerPlayer player, CallbackInfo callback) {
        NeoForgeBootstrap.endSmeltingXp();
    }

    @Inject(method = "getBurnDuration", at = @At("RETURN"), cancellable = true)
    private void bigbangskills$fuelEfficiency(ItemStack fuel, CallbackInfoReturnable<Integer> callback) {
        callback.setReturnValue(NeoForgeBootstrap.smeltingFuelDuration((AbstractFurnaceBlockEntity) (Object) this, callback.getReturnValue()));
    }

}
