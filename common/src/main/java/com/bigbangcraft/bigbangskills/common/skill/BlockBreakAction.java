package com.bigbangcraft.bigbangskills.common.skill;

import java.util.UUID;

public record BlockBreakAction(UUID playerId, String blockId, String worldId, boolean miningBlock, boolean woodcuttingBlock, boolean realPlayer, boolean eventCancelled, boolean placed, boolean provenanceKnown) {}
