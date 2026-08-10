package com.bigbangcraft.bigbangskills.api;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillIdTest {
    @Test void parsesCommandAliases() {
        assertEquals(SkillId.parse("bigbangskills:mining"), SkillId.parseUserInput(" Mining "));
        assertEquals(SkillId.parse("bigbangskills:woodcutting"), SkillId.parseUserInput("woodcuting"));
    }
}
