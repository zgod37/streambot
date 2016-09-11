package streambot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class FileLogger {

    private final static Logger logger = Logger.getLogger(Bot.class.getPackage().getName());
    private static FileHandler handler = null;

    public static void initializeLogger() throws IOException {
        handler = new FileHandler(Paths.get("data" + File.separator + "Bot.log").toString(), true);
        logger.addHandler(handler);
        handler.setFormatter(new SimpleFormatter());
    }

    public static void logWarning(String message) {
        logger.log(Level.WARNING, message);
    }

    public static void logInfo(String message) {
        logger.log(Level.INFO, message);
    }
}
