package io.github.mobinrt.csvparser.infrastructure.db;

import java.sql.Connection;
import java.sql.Statement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mobinrt.csvparser.domain.ports.ErrorWriter;

public final class MySqlErrorWriter implements ErrorWriter {

    private static final Logger log = LoggerFactory.getLogger(MySqlErrorWriter.class);

    private final DataSource dataSource;

    public MySqlErrorWriter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void ensureErrorTableExist() {
        String ddl = """
                CREATE TABLE IF NOT EXISTS `error_rows` (
                  `id` BIGINT NOT NULL AUTO_INCREMENT,
                  `source_file` VARCHAR(1024) NULL,
                  `row_number` BIGINT NULL,
                  `raw_row` TEXT NULL,
                  `error_message` TEXT NULL,
                  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`)
                ) ENGINE=InnoDB
                """;

        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            st.execute(ddl);
            log.info("Ensured error table exists: error_rows");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create/verify error_rows table", e);
        }
    }
}
