package io.github.mobinrt.csvparser.infrastructure.db;

import java.time.Duration;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public final class DataSourceFactory {

    public DataSource createMySqlDataSource(String jdbcUrl, String user, String pass) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalArgumentException("jdbcUrl is required");
        }
        if (user == null || user.isBlank()) {
            throw new IllegalArgumentException("db user is required");
        }
        if (pass == null) {
            throw new IllegalArgumentException("db password is required");
        }

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbcUrl);
        cfg.setUsername(user);
        cfg.setPassword(pass);

        cfg.setMaximumPoolSize(4);
        cfg.setMinimumIdle(0);
        cfg.setConnectionTimeout(Duration.ofSeconds(10).toMillis());
        cfg.setValidationTimeout(Duration.ofSeconds(5).toMillis());

        cfg.addDataSourceProperty("cachePrepStmts", "true");
        cfg.addDataSourceProperty("prepStmtCacheSize", "250");
        cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        cfg.addDataSourceProperty("useServerPrepStmts", "true");

        return new HikariDataSource(cfg);
    }
}
