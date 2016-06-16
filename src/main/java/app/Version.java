package app;

import java.io.IOException;
import java.util.Properties;

public final class Version {

    private static final String VERSION = loadVersion();

    public static String version() {
        return VERSION;
    }

    private static String loadVersion() {
        Properties prop = new Properties();
        try {
            prop.load(Version.class.getResourceAsStream("/application.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return prop.getProperty("version");
    }

}
