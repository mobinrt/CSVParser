package io.github.mobinrt.csvparser.app.config;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class PropertiesConfigLoader {

    public Properties loadProperties(Path propertiesPath) {
        Properties props = new Properties();

        if (propertiesPath == null) {
            return props;
        }

        if (!Files.exists(propertiesPath)) {
            return props; 
        }

        try (InputStream in = Files.newInputStream(propertiesPath)) {
            props.load(in);
            return props;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read config file: " + propertiesPath.toAbsolutePath(), e);
        }
    }
}
