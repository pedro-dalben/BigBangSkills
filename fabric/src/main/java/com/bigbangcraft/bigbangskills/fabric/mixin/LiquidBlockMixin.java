package com.bigbangcraft.bigbangskills.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LiquidBlock.class)
public abstract class LiquidBlockMixin {
    @Inject(method = "onPlace", at = @At("TAIL"))
    private void bigbangskills$propagateProvenance(BlockState state, Level level, BlockPos pos, BlockState oldState,
                                                   boolean movedByPiston, CallbackInfo callback) {
        com.bigbangcraft.bigbangskills.fabric.FabricBootstrap.transferFluidProvenance(level, pos);
    }

}
