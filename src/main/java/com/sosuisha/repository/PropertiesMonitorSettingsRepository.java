package com.sosuisha.repository;

import module java.base;

import com.sosuisha.domain.exception.RepositoryException;
import com.sosuisha.domain.model.Alignment;
import com.sosuisha.domain.model.FillColor;
import com.sosuisha.domain.model.MonitorSettings;
import com.sosuisha.domain.repository.MonitorSettingsRepository;

/**
 * Settings repository that saves the settings in a properties file. Each key
 * is the monitor id followed by the name of the setting, such as
 * {@code <id>.folder}.
 */
public class PropertiesMonitorSettingsRepository implements MonitorSettingsRepository {
    private static final String FOLDER = ".folder";
    private static final String IMAGE = ".image";
    private static final String ALIGNMENT = ".alignment";
    private static final String FILL_COLOR = ".fill";

    private final Path file;

    /**
     * Creates the repository.
     *
     * @param file properties file to save the settings in. Its parent folder is
     *             created when the settings are saved.
     * @throws NullPointerException if file is null
     */
    public PropertiesMonitorSettingsRepository(Path file) {
        this.file = Objects.requireNonNull(file, "file must not be null");
    }

    @Override
    public Map<String, MonitorSettings> load() throws RepositoryException {
        if (!Files.exists(file)) { return Map.of(); }
        var properties = new Properties();
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new RepositoryException("Cannot read the settings file: " + file, e);
        }
        var settings = new HashMap<String, MonitorSettings>();
        for (var key : properties.stringPropertyNames()) {
            // Every monitor has an alignment, so the alignment keys list the monitors.
            if (key.endsWith(ALIGNMENT)) {
                var monitorId = key.substring(0, key.length() - ALIGNMENT.length());
                settings.put(monitorId, toSettings(properties, monitorId));
            }
        }
        return Map.copyOf(settings);
    }

    @Override
    public void save(Map<String, MonitorSettings> settings) throws RepositoryException {
        Objects.requireNonNull(settings, "settings must not be null");
        var properties = new Properties();
        settings.forEach((monitorId, monitor) -> {
            monitor.folder()
                .ifPresent(f -> properties.setProperty(monitorId + FOLDER, f.toString()));
            monitor.image().ifPresent(i -> properties.setProperty(monitorId + IMAGE, i.toString()));
            properties.setProperty(monitorId + ALIGNMENT, monitor.alignment().name());
            properties.setProperty(monitorId + FILL_COLOR, monitor.fillColor().name());
        });
        try {
            // An absolute path of a file always has a parent.
            Files.createDirectories(Objects.requireNonNull(file.toAbsolutePath().getParent()));
            try (var writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                properties.store(writer, "Settings of sss-wallpaper by monitor id");
            }
        } catch (IOException e) {
            throw new RepositoryException("Cannot write the settings file: " + file, e);
        }
    }

    private MonitorSettings toSettings(Properties properties, String monitorId) {
        var alignmentName = properties.getProperty(monitorId + ALIGNMENT);
        try {
            return new MonitorSettings(
                Optional.ofNullable(properties.getProperty(monitorId + FOLDER)).map(Path::of),
                Optional.ofNullable(properties.getProperty(monitorId + IMAGE)).map(Path::of),
                Alignment.valueOf(alignmentName),
                // Files saved before the fill color was added have no fill color.
                Optional.ofNullable(properties.getProperty(monitorId + FILL_COLOR))
                    .map(FillColor::valueOf)
                    .orElse(FillColor.WHITE)
            );
        } catch (IllegalArgumentException e) {
            // An unknown enum name or a path that is not valid on this PC.
            throw new RepositoryException(
                "Broken setting of monitor " + monitorId + " in " + file, e
            );
        }
    }
}
