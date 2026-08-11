package com.bigbangcraft.bigbangskills.fabric.mixin;

import com.bigbangcraft.bigbangskills.fabric.FabricBootstrap;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractCookingRecipe.class)
public abstract class AbstractCookingRecipeMixin {
    @Inject(method = "getExperience", at = @At("RETURN"), cancellable = true)
    private void bigbangskills$understandingTheArt(CallbackInfoReturnable<Float> callback) {
        callback.setReturnValue(FabricBootstrap.smeltingRecipeXp(callback.getReturnValue()));
    }
}
