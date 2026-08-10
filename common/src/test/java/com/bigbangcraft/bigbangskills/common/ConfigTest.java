package com.bigbangcraft.bigbangskills.common;

import com.bigbangcraft.bigbangskills.common.config.ProgressionConfig;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {
    @Test void defaultsAreValidAndImmutable() {
        var config = ProgressionConfig.defaults();
        assertEquals(BigDecimal.ONE, config.globalMultiplier());
        assertThrows(UnsupportedOperationException.class, () -> config.blockXp().put("bad", BigDecimal.ONE));
    }
    @Test void negativeValuesFailValidation() {
        assertThrows(IllegalArgumentException.class, () -> new ProgressionConfig(BigDecimal.ONE.negate(), BigDecimal.ONE, true, Map.of()));
    }
}
