package com.bigbangcraft.bigbangskills.neoforge.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.class)
public abstract class BlockMixin {
    @Inject(method = "onRemove", at = @At("TAIL"))
    private void bigbangskills$clearFluidProvenance(BlockState state, Level level, BlockPos pos, BlockState newState,
                                                   boolean movedByPiston, CallbackInfo callback) {
        if (state.getBlock() instanceof LiquidBlock && !(newState.getBlock() instanceof LiquidBlock)) {
            com.bigbangcraft.bigbangskills.neoforge.NeoForgeBootstrap.clearFluidProvenance(level, pos);
        }
    }
}
