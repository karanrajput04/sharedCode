package com.messagequeue.config;

import com.messagequeue.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Manages configuration loading and access for the MQ System application.
 * Supports both property files and environment variable overrides.
 * 
 * @author MQ System Development Team
 * @version 1.0.0
 */
public class MQConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(MQConfiguration.class);
    private static final String CONFIG_FILE = "mq-config.properties";
    private Properties properties;

    /**
     * Initializes the configuration by loading properties from file.
     * 
     * @throws ConfigurationException if configuration file cannot be loaded
     */
    public MQConfiguration() throws ConfigurationException {
        properties = new Properties();
        loadConfiguration();
    }

    /**
     * Loads configuration from properties file and environment variables.
     * Environment variables take precedence over file properties.
     * 
     * @throws ConfigurationException if file cannot be read
     */
    private void loadConfiguration() throws ConfigurationException {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {
            
            if (input != null) {
                properties.load(input);
                logger.info("Loaded configuration from {}", CONFIG_FILE);
            } else {
                logger.warn("Configuration file {} not found, using defaults", CONFIG_FILE);
            }
        } catch (IOException e) {
            throw new ConfigurationException("Failed to load configuration file: " + CONFIG_FILE, e);
        }

        // Override with environment variables
        overrideWithEnvironmentVariables();
    }

    /**
     * Overrides configuration properties with environment variables if present.
     */
    private void overrideWithEnvironmentVariables() {
        String[] envVars = {
            "MQ_HOST", "MQ_PORT", "MQ_QUEUE_MANAGER", "MQ_QUEUE_NAME",
            "MQ_CHANNEL", "MQ_USERNAME", "MQ_PASSWORD",
            "MQ_SSL_ENABLED", "MQ_SSL_TRUSTSTORE_PATH", "MQ_SSL_TRUSTSTORE_PASSWORD",
            "MQ_SSL_KEYSTORE_PATH", "MQ_SSL_KEYSTORE_PASSWORD", "MQ_SSL_CIPHER_SUITE",
            "MQ_CONNECTION_TIMEOUT", "MQ_MAX_RETRIES", "MQ_RETRY_DELAY"
        };

        for (String envVar : envVars) {
            String value = System.getenv(envVar);
            if (value != null) {
                String propertyKey = envVar.toLowerCase().replace("_", ".");
                properties.setProperty(propertyKey, value);
                logger.debug("Overriding property {} with environment variable", propertyKey);
            }
        }
    }

    /**
     * Retrieves a string configuration property with a default value.
     * 
     * @param key the property key
     * @param defaultValue the default value if not found
     * @return the property value or default value
     */
    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Retrieves a string configuration property.
     * 
     * @param key the property key
     * @return the property value
     * @throws ConfigurationException if property not found
     */
    public String getString(String key) throws ConfigurationException {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new ConfigurationException("Required configuration property not found: " + key);
        }
        return value;
    }

    /**
     * Retrieves an integer configuration property.
     * 
     * @param key the property key
     * @param defaultValue the default value if not found
     * @return the property value as integer or default value
     */
    public int getInteger(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid integer value for property {}: {}", key, value);
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Retrieves a boolean configuration property.
     * 
     * @param key the property key
     * @param defaultValue the default value if not found
     * @return the property value as boolean or default value
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }

    /**
     * Checks if a property exists.
     * 
     * @param key the property key
     * @return true if property exists, false otherwise
     */
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    /**
     * Reloads configuration from file.
     * 
     * @throws ConfigurationException if configuration cannot be reloaded
     */
    public void reload() throws ConfigurationException {
        properties.clear();
        loadConfiguration();
        logger.info("Configuration reloaded successfully");
    }

    /**
     * Gets all configuration properties.
     * 
     * @return the properties object
     */
    public Properties getProperties() {
        return (Properties) properties.clone();
    }
}
