package com.ktx.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ktx.domain.SystemConfig;

public interface SystemConfigService {

    String get(String key, String defaultValue);

    Optional<String> find(String key);

    int getInt(String key, int defaultValue);

    double getDouble(String key, double defaultValue);

    BigDecimal getBigDecimal(String key, BigDecimal defaultValue);

    boolean getBoolean(String key, boolean defaultValue);

    List<SystemConfig> getAllConfigs();

    Map<String, List<SystemConfig>> getConfigsGrouped();

    SystemConfig set(String key, String value);

    void updateConfigs(Map<String, String> keyValues);
}
