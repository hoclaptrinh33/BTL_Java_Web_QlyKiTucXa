package com.ktx.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.SystemConfig;
import com.ktx.repository.SystemConfigRepository;
import com.ktx.service.impl.SystemConfigServiceImpl;

@ExtendWith(MockitoExtension.class)
class SystemConfigServiceTest {

    @Mock
    private SystemConfigRepository systemConfigRepository;

    private SystemConfigService systemConfigService;

    @BeforeEach
    void setUp() {
        systemConfigService = new SystemConfigServiceImpl(systemConfigRepository);
    }

    private SystemConfig createConfig(String key, String value, String type, String desc) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setValueType(type);
        config.setDescription(desc);
        return config;
    }

    @Test
    @DisplayName("Typed getters return parsed values when config exists")
    void typedGetters_existingKeys() {
        when(systemConfigRepository.findById("alloc.weight.policy"))
                .thenReturn(Optional.of(createConfig("alloc.weight.policy", "1000", "INT", "Policy weight")));
        when(systemConfigRepository.findById("contract.deposit.ratio"))
                .thenReturn(Optional.of(createConfig("contract.deposit.ratio", "0.5", "DECIMAL", "Deposit ratio")));
        when(systemConfigRepository.findById("billing.room.split_monthly"))
                .thenReturn(Optional.of(createConfig("billing.room.split_monthly", "true", "BOOLEAN", "Split monthly")));
        when(systemConfigRepository.findById("alloc.preference.mode"))
                .thenReturn(Optional.of(createConfig("alloc.preference.mode", "SOFT", "STRING", "Pref mode")));

        assertEquals(1000, systemConfigService.getInt("alloc.weight.policy", 500));
        assertEquals(new BigDecimal("0.5"), systemConfigService.getBigDecimal("contract.deposit.ratio", BigDecimal.ZERO));
        assertEquals(0.5, systemConfigService.getDouble("contract.deposit.ratio", 0.0), 0.001);
        assertTrue(systemConfigService.getBoolean("billing.room.split_monthly", false));
        assertEquals("SOFT", systemConfigService.get("alloc.preference.mode", "STRICT"));
    }

    @Test
    @DisplayName("Typed getters return fallback defaults when config missing")
    void typedGetters_missingKeys() {
        when(systemConfigRepository.findById("non.existent.key")).thenReturn(Optional.empty());

        assertEquals(42, systemConfigService.getInt("non.existent.key", 42));
        assertEquals(new BigDecimal("0.75"), systemConfigService.getBigDecimal("non.existent.key", new BigDecimal("0.75")));
        assertEquals(3.14, systemConfigService.getDouble("non.existent.key", 3.14), 0.001);
        assertFalse(systemConfigService.getBoolean("non.existent.key", false));
        assertEquals("default-val", systemConfigService.get("non.existent.key", "default-val"));
    }

    @Test
    @DisplayName("Grouping organizes configs into alloc, contract, billing, conduct, and other")
    void getConfigsGrouped_groupsCorrectly() {
        List<SystemConfig> configs = Arrays.asList(
                createConfig("alloc.weight.policy", "1000", "INT", ""),
                createConfig("alloc.preference.mode", "SOFT", "STRING", ""),
                createConfig("contract.deposit.ratio", "0.5", "DECIMAL", ""),
                createConfig("billing.water.price_per_m3", "15000", "INT", ""),
                createConfig("conduct.initial", "100", "INT", ""),
                createConfig("ticket.autoclose.days", "7", "INT", "")
        );
        when(systemConfigRepository.findAll()).thenReturn(configs);

        Map<String, List<SystemConfig>> grouped = systemConfigService.getConfigsGrouped();

        assertNotNull(grouped);
        assertEquals(2, grouped.get("alloc").size());
        assertEquals(1, grouped.get("contract").size());
        assertEquals(1, grouped.get("billing").size());
        assertEquals(1, grouped.get("conduct").size());
        assertEquals(1, grouped.get("other").size());
        assertEquals("ticket.autoclose.days", grouped.get("other").get(0).getConfigKey());
    }

    @Test
    @DisplayName("Validation fails when contract.deposit.ratio is outside [0.0, 1.0]")
    void updateConfigs_invalidDepositRatio_throwsException() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                systemConfigService.set("contract.deposit.ratio", "1.5")
        );
        assertTrue(ex.getMessage().contains("contract.deposit.ratio"));

        assertThrows(BusinessException.class, () ->
                systemConfigService.set("contract.deposit.ratio", "-0.1")
        );

        assertThrows(BusinessException.class, () ->
                systemConfigService.set("contract.deposit.ratio", "not-a-number")
        );
    }

    @Test
    @DisplayName("Validation fails when alloc.preference.mode is neither SOFT nor STRICT")
    void updateConfigs_invalidPreferenceMode_throwsException() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                systemConfigService.set("alloc.preference.mode", "INVALID_MODE")
        );
        assertTrue(ex.getMessage().contains("SOFT") && ex.getMessage().contains("STRICT"));
    }

    @Test
    @DisplayName("set updates existing config or creates new if not found")
    void set_validConfig_saves() {
        SystemConfig existing = createConfig("contract.deposit.ratio", "0.5", "DECIMAL", "Old desc");
        when(systemConfigRepository.findById("contract.deposit.ratio")).thenReturn(Optional.of(existing));
        when(systemConfigRepository.save(any(SystemConfig.class))).thenAnswer(i -> i.getArgument(0));

        SystemConfig updated = systemConfigService.set("contract.deposit.ratio", "0.4");

        assertEquals("0.4", updated.getConfigValue());
        verify(systemConfigRepository).save(existing);
    }
}
