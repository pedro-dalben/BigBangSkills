package com.bigbangcraft.bigbangskills.common.skill;

import java.util.UUID;

public record BlockBreakAction(UUID playerId, String blockId, String worldId, boolean miningBlock, boolean woodcuttingBlock, boolean realPlayer, boolean eventCancelled, boolean placed, boolean provenanceKnown, boolean silkTouch, boolean abilityActive, boolean excavationBlock, boolean herbalismBlock) {
    public BlockBreakAction(UUID playerId, String blockId, String worldId, boolean miningBlock, boolean woodcuttingBlock, boolean realPlayer, boolean eventCancelled, boolean placed, boolean provenanceKnown, boolean silkTouch, boolean abilityActive) {
        this(playerId, blockId, worldId, miningBlock, woodcuttingBlock, realPlayer, eventCancelled, placed, provenanceKnown, silkTouch, abilityActive, false, false);
    }
    public BlockBreakAction(UUID playerId, String blockId, String worldId, boolean miningBlock, boolean woodcuttingBlock, boolean realPlayer, boolean eventCancelled, boolean placed, boolean provenanceKnown) {
        this(playerId, blockId, worldId, miningBlock, woodcuttingBlock, realPlayer, eventCancelled, placed, provenanceKnown, false, false);
    }
}
