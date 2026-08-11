package com.bigbangcraft.bigbangskills.common.skill;

/** Pure Alchemy formulas; brewing inventory mutation remains loader-specific. */
public final class AlchemyEngine {
    private static final double DEFAULT_LUCKY_MODIFIER = 4.0 / 3.0;

    public double brewSpeed(int level, int unlockLevel, boolean lucky, double minSpeed, double maxSpeed, int maxBonusLevel) {
        if (maxBonusLevel <= unlockLevel || level < unlockLevel) return minSpeed;
        var progress = Math.min(1.0, (double) (level - unlockLevel) / (maxBonusLevel - unlockLevel));
        var speed = Math.min(maxSpeed, minSpeed + (maxSpeed - minSpeed) * progress);
        return speed * (lucky ? DEFAULT_LUCKY_MODIFIER : 1.0);
    }

    public int concoctionsRank(int level) {
        var unlocks = new int[]{0, 10, 20, 35, 50, 75, 90, 100};
        var rank = 0;
        for (var unlock : unlocks) if (level >= unlock) rank++;
        return Math.min(8, rank);
    }

    public int concoctionTier(String ingredientId) {
        return switch (ingredientId) {
            case "minecraft:breeze_rod", "minecraft:blaze_powder", "minecraft:fermented_spider_eye", "minecraft:ghast_tear",
                    "minecraft:glowstone_dust", "minecraft:golden_carrot", "minecraft:magma_cream", "minecraft:nether_wart",
                    "minecraft:redstone", "minecraft:glistering_melon_slice", "minecraft:spider_eye", "minecraft:sugar",
                    "minecraft:gunpowder", "minecraft:water_lily", "minecraft:pufferfish", "minecraft:dragon_breath",
                    "minecraft:stone", "minecraft:slime_block", "minecraft:cobweb", "minecraft:turtle_helmet" -> 1;
            case "minecraft:carrot", "minecraft:slime_ball", "minecraft:phantom_membrane" -> 2;
            case "minecraft:quartz", "minecraft:rabbit_foot" -> 3;
            case "minecraft:apple", "minecraft:rotten_flesh" -> 4;
            case "minecraft:brown_mushroom", "minecraft:ink_sac" -> 5;
            case "minecraft:fern" -> 6;
            case "minecraft:poisonous_potato" -> 7;
            case "minecraft:golden_apple" -> 8;
            default -> 0;
        };
    }
}
