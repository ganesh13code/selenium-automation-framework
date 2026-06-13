package com.framework.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * ConfigReader — Reads all values from config.properties.
 *
 * ═══════════════════════════════════════════════════════════
 * CONCEPT: Why externalize configuration?
 * ═══════════════════════════════════════════════════════════
 * Hardcoded values in test code are fragile:
 *   driver.get("https://qa.example.com");   // breaks when URL changes
 *   driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10)); // magic number
 *
 * config.properties centralizes ALL such values:
 *   - One change in the file = every class picks it up automatically
 *   - CI/CD pipelines can override with -Dkey=value JVM args
 *   - Multi-env: swap config-qa.properties vs config-prod.properties
 * ═══════════════════════════════════════════════════════════
 *
 * ═══════════════════════════════════════════════════════════
 * CONCEPT: Static Initializer Block
 * ═══════════════════════════════════════════════════════════
 * The block `static { ... }` runs ONCE when the class is first loaded
 * by the JVM — before any method is called. This is perfect for
 * loading the properties file exactly one time per test run.
 *
 * Alternatives:
 *   - Singleton getInstance() pattern (lazy loading on first call)
 *   - Spring @PropertySource (if using Spring)
 * Static init is simplest for a pure-Java framework.
 * ═══════════════════════════════════════════════════════════
 */
public class ConfigReader {

    private static final Logger log = LogManager.getLogger(ConfigReader.class);

    private static final Properties properties;
    private static final String CONFIG_PATH = "src/test/resources/config.properties";

    // Static block: runs ONCE when ConfigReader is first referenced
    static {
        properties = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_PATH)) {
            properties.load(fis);
            log.info("config.properties loaded from: {}", CONFIG_PATH);
        } catch (IOException e) {
            log.error("FATAL: Cannot load config.properties at '{}'", CONFIG_PATH);
            throw new RuntimeException("config.properties not found. Path: " + CONFIG_PATH, e);
        }
    }

    private ConfigReader() {}

    // ─────────────────────────────────────────────────────
    // Generic getters
    // ─────────────────────────────────────────────────────

    /** Returns property value, or null if key not found. */
    public static String get(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            log.warn("Property '{}' not found in config.properties", key);
        }
        return value;
    }

    /** Returns property value, or defaultValue if key not found. */
    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    // ─────────────────────────────────────────────────────
    // Typed convenience methods (saves casting everywhere)
    // ─────────────────────────────────────────────────────

    public static String getBrowser() {
        return get("browser", "chrome");
    }

    public static boolean isHeadless() {
        return Boolean.parseBoolean(get("headless", "false"));
    }

    public static String getBaseUrl() {
        return get("base.url");
    }

    public static int getImplicitWait() {
        return Integer.parseInt(get("implicit.wait", "10"));
    }

    public static int getExplicitWait() {
        return Integer.parseInt(get("explicit.wait", "20"));
    }

    public static int getPageLoadTimeout() {
        return Integer.parseInt(get("page.load.timeout", "30"));
    }

    public static String getScreenshotDir() {
        return get("screenshot.dir", "test-output/screenshots");
    }

    public static String getReportDir() {
        return get("report.dir", "test-output/reports");
    }

    public static String getReportName() {
        return get("report.name", "HybridFrameworkReport");
    }
}
