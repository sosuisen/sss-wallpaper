package com.sosuisha.domain.repository;

import java.util.Map;

import com.sosuisha.domain.model.MonitorSettings;

/**
 * A settings repository that stores nothing. Tests override the methods they need.
 */
public class NullMonitorSettingsRepository implements MonitorSettingsRepository {
    @Override
    public Map<String, MonitorSettings> load() {
        return Map.of();
    }

    @Override
    public void save(Map<String, MonitorSettings> settings) {}
}
