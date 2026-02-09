package io.github.mobinrt.csvparser.app.config;

import java.nio.file.Path;
import java.util.Properties;

public final class DbConfigResolver {

    public static final String KEY_DB_URL  = "CSV_DB_URL";
    public static final String KEY_DB_USER = "CSV_DB_USER";
    public static final String KEY_DB_PASS = "CSV_DB_PASS";

    public ResolvedDbConfig resolveDbConfig(
            String cliDbUrl,
            String cliDbUser,
            String cliDbPass,
            Path propertiesPath
    ) {
        Properties props = new PropertiesConfigLoader().loadProperties(propertiesPath);

        String dbUrl  = pickFirstNonBlank(cliDbUrl,  System.getenv(KEY_DB_URL),  props.getProperty(KEY_DB_URL));
        String dbUser = pickFirstNonBlank(cliDbUser, System.getenv(KEY_DB_USER), props.getProperty(KEY_DB_USER));
        String dbPass = pickFirstNonBlank(cliDbPass, System.getenv(KEY_DB_PASS), props.getProperty(KEY_DB_PASS));

        if (isBlank(dbUrl) || isBlank(dbUser) || dbPass == null) {
            throw new IllegalArgumentException(
                    "DB config missing. Provide --db-url/--db-user/--db-pass OR set env vars " +
                            KEY_DB_URL + "/" + KEY_DB_USER + "/" + KEY_DB_PASS +
                            " OR put them in config.properties (or pass --config <path>)."
            );
        }

        return new ResolvedDbConfig(dbUrl, dbUser, dbPass);
    }

    private String pickFirstNonBlank(String... candidates) {
        for (String c : candidates) {
            if (!isBlank(c)) return c;
        }
        return null;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static final class ResolvedDbConfig {
        private final String dbUrl;
        private final String dbUser;
        private final String dbPass;

        public ResolvedDbConfig(String dbUrl, String dbUser, String dbPass) {
            this.dbUrl = dbUrl;
            this.dbUser = dbUser;
            this.dbPass = dbPass;
        }

        public String getDbUrl() {
            return dbUrl;
        }

        public String getDbUser() {
            return dbUser;
        }

        public String getDbPass() {
            return dbPass;
        }
    }
}
