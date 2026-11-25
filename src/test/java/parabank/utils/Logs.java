package parabank.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Single entry point for logging (Log4j2, configured in log4j2.xml). */
public final class Logs {

    private static final Logger LOGGER = LogManager.getLogger("ParaBank");

    private Logs() {
    }

    public static void info(String message, Object... args) {
        LOGGER.info(message, args);
    }

    public static void debug(String message, Object... args) {
        LOGGER.debug(message, args);
    }

    public static void warn(String message, Object... args) {
        LOGGER.warn(message, args);
    }

    public static void error(String message, Object... args) {
        LOGGER.error(message, args);
    }
}
