package com.bigbangcraft.bigbangskills.fabric.mixin;

import com.bigbangcraft.bigbangskills.fabric.FabricBootstrap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin {
    @Shadow private int brewTime;

    @Inject(method = "serverTick", at = @At("TAIL"))
    private static void bigbangskills$accelerate(Level level, BlockPos pos, BlockState state, BrewingStandBlockEntity stand, CallbackInfo callback) {
        var extra = FabricBootstrap.brewingExtraTicks(stand, pos, level);
        if (extra > 0) ((BrewingStandBlockEntityMixin) (Object) stand).bigbangskills$subtract(extra);
    }

    @Inject(method = "doBrew", at = @At("HEAD"), cancellable = true)
    private static void bigbangskills$beforeBrew(Level level, BlockPos pos, net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> items, CallbackInfo callback) {
        FabricBootstrap.recordBrewingBefore(pos, level, items);
        if (FabricBootstrap.tryConcoction(level, pos, items)) callback.cancel();
    }

    @Inject(method = "doBrew", at = @At("TAIL"))
    private static void bigbangskills$afterBrew(Level level, BlockPos pos, net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> items, CallbackInfo callback) {
        FabricBootstrap.recordBrewingAfter(pos, level, items);
    }

    private void bigbangskills$subtract(int extra) { brewTime = Math.max(0, brewTime - extra); }
}
