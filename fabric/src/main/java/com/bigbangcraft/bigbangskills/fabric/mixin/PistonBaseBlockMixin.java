package com.bigbangcraft.bigbangskills.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlockMixin {
    @Inject(method = "moveBlocks", at = @At("HEAD"))
    private void bigbangskills$transferProvenance(Level level, BlockPos piston, Direction direction, boolean extending, CallbackInfo callback) {
        com.bigbangcraft.bigbangskills.fabric.FabricBootstrap.transferPistonProvenance(level, piston, direction, extending);
    }
}
