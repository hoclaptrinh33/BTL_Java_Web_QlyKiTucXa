package com.ktx.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.SystemConfig;
import com.ktx.repository.SystemConfigRepository;
import com.ktx.service.SystemConfigService;

@Service
@Transactional
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final Logger log = LoggerFactory.getLogger(SystemConfigServiceImpl.class);

    private final SystemConfigRepository systemConfigRepository;

    public SystemConfigServiceImpl(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public String get(String key, String defaultValue) {
        if (key == null) {
            return defaultValue;
        }
        return systemConfigRepository.findById(key)
                .map(SystemConfig::getConfigValue)
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .orElse(defaultValue);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> find(String key) {
        if (key == null) {
            return Optional.empty();
        }
        return systemConfigRepository.findById(key)
                .map(SystemConfig::getConfigValue)
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim);
    }

    @Override
    @Transactional(readOnly = true)
    public int getInt(String key, int defaultValue) {
        String val = get(key, null);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            log.warn("Lỗi parse int cho config {}: '{}', dùng mặc định {}", key, val, defaultValue);
            return defaultValue;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public double getDouble(String key, double defaultValue) {
        String val = get(key, null);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            log.warn("Lỗi parse double cho config {}: '{}', dùng mặc định {}", key, val, defaultValue);
            return defaultValue;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        String val = get(key, null);
        if (val == null) {
            return defaultValue;
        }
        try {
            return new BigDecimal(val);
        } catch (Exception e) {
            log.warn("Lỗi parse BigDecimal cho config {}: '{}', dùng mặc định {}", key, val, defaultValue);
            return defaultValue;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean getBoolean(String key, boolean defaultValue) {
        String val = get(key, null);
        if (val == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(val) || "1".equals(val) || "yes".equalsIgnoreCase(val);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemConfig> getAllConfigs() {
        return systemConfigRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<SystemConfig>> getConfigsGrouped() {
        List<SystemConfig> all = systemConfigRepository.findAll();
        Map<String, List<SystemConfig>> groups = new LinkedHashMap<>();
        groups.put("alloc", new ArrayList<>());
        groups.put("contract", new ArrayList<>());
        groups.put("billing", new ArrayList<>());
        groups.put("conduct", new ArrayList<>());
        groups.put("other", new ArrayList<>());

        for (SystemConfig config : all) {
            String key = config.getConfigKey();
            if (key.startsWith("alloc.")) {
                groups.get("alloc").add(config);
            } else if (key.startsWith("contract.")) {
                groups.get("contract").add(config);
            } else if (key.startsWith("billing.")) {
                groups.get("billing").add(config);
            } else if (key.startsWith("conduct.")) {
                groups.get("conduct").add(config);
            } else {
                groups.get("other").add(config);
            }
        }

        return groups;
    }

    @Override
    public SystemConfig set(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new BusinessException("Khóa cấu hình không được để trống");
        }
        if (value == null) {
            throw new BusinessException("Giá trị cấu hình không được null");
        }

        SystemConfig config = systemConfigRepository.findById(key)
                .orElseGet(() -> {
                    SystemConfig newConf = new SystemConfig();
                    newConf.setConfigKey(key);
                    newConf.setValueType("STRING");
                    return newConf;
                });

        validateValue(key, config.getValueType(), value.trim());

        config.setConfigValue(value.trim());
        return systemConfigRepository.save(config);
    }

    @Override
    public void updateConfigs(Map<String, String> keyValues) {
        if (keyValues == null || keyValues.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : keyValues.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null && systemConfigRepository.existsById(key)) {
                set(key, value);
            }
        }
    }

    private void validateValue(String key, String valueType, String value) {
        if ("contract.deposit.ratio".equals(key)) {
            try {
                BigDecimal ratio = new BigDecimal(value);
                if (ratio.compareTo(BigDecimal.ZERO) < 0 || ratio.compareTo(BigDecimal.ONE) > 0) {
                    throw new BusinessException("Tỷ lệ đặt cọc contract.deposit.ratio phải nằm trong khoảng từ 0.0 đến 1.0 (ví dụ 0.5)");
                }
            } catch (NumberFormatException e) {
                throw new BusinessException("Tỷ lệ đặt cọc contract.deposit.ratio không hợp lệ: " + value);
            }
        }

        if ("alloc.preference.mode".equals(key)) {
            if (!"SOFT".equalsIgnoreCase(value) && !"STRICT".equalsIgnoreCase(value)) {
                throw new BusinessException("alloc.preference.mode chỉ chấp nhận SOFT hoặc STRICT: " + value);
            }
        }

        if ("INT".equalsIgnoreCase(valueType)) {
            try {
                Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new BusinessException("Cấu hình " + key + " yêu cầu giá trị số nguyên (INT): " + value);
            }
        } else if ("DECIMAL".equalsIgnoreCase(valueType)) {
            try {
                new BigDecimal(value);
            } catch (Exception e) {
                throw new BusinessException("Cấu hình " + key + " yêu cầu giá trị số thập phân (DECIMAL): " + value);
            }
        } else if ("BOOLEAN".equalsIgnoreCase(valueType)) {
            if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                throw new BusinessException("Cấu hình " + key + " yêu cầu giá trị boolean (true hoặc false): " + value);
            }
        }
    }
}
