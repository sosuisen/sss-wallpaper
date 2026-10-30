package com.sosuisha.domain.repository;

import module java.base;

import com.sosuisha.domain.exception.RepositoryException;
import com.sosuisha.domain.model.MonitorSettings;

/**
 * Stores the settings of each monitor.
 */
public interface MonitorSettingsRepository {
    /**
     * Loads the settings.
     *
     * @return settings by monitor id, or an empty map if nothing is saved yet
     * @throws RepositoryException if the saved data cannot be read
     */
    Map<String, MonitorSettings> load() throws RepositoryException;

    /**
     * Saves the settings. The saved data is replaced.
     *
     * @param settings settings by monitor id
     * @throws NullPointerException if settings is null
     * @throws RepositoryException if the data cannot be saved
     */
    void save(Map<String, MonitorSettings> settings) throws RepositoryException;
}
