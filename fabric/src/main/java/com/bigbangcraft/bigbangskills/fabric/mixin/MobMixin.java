package com.bigbangcraft.bigbangskills.fabric.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobMixin {
    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void bigbangskills$recordSpawnOrigin(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type,
                                                  SpawnGroupData data, CallbackInfoReturnable<SpawnGroupData> callback) {
        var mob = (Mob) (Object) this;
        if (type == MobSpawnType.SPAWNER || type == MobSpawnType.TRIAL_SPAWNER) mob.addTag("bigbangskills_spawner_mob");
        if (type == MobSpawnType.SPAWN_EGG || type == MobSpawnType.DISPENSER) mob.addTag("bigbangskills_egg_mob");
        if (type == MobSpawnType.BREEDING) mob.addTag("bigbangskills_bred_mob");
    }
}
