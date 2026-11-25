package parabank.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Reads the framework settings.
 * Priority: JVM system property (-Dkey=value) > environment variable (KEY) > config.properties.
 */
public final class Config {

    private static final Properties PROPERTIES = load();

    private Config() {
    }

    public static String baseUrl() {
        String url = get("baseUrl");
        return url.endsWith("/") ? url : url + "/";
    }

    public static String browser() {
        return get("browser").toLowerCase();
    }

    public static boolean headless() {
        return Boolean.parseBoolean(get("headless"));
    }

    public static double slowMo() {
        return Double.parseDouble(get("slowMo"));
    }

    public static double timeout() {
        return Double.parseDouble(get("timeout"));
    }

    private static String get(String key) {
        String value = System.getProperty(key);
        if (isBlank(value)) {
            value = System.getenv(toEnvName(key));
        }
        if (isBlank(value)) {
            value = PROPERTIES.getProperty(key);
        }
        if (isBlank(value)) {
            throw new IllegalStateException("Missing configuration value: " + key);
        }
        return value.trim();
    }

    /** baseUrl -> BASE_URL */
    private static String toEnvName(String key) {
        return key.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new IllegalStateException("config.properties not found on the classpath");
            }
            properties.load(input);
            return properties;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read config.properties", e);
        }
    }
}
