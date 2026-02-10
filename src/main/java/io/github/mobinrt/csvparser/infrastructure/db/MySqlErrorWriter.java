package io.github.mobinrt.csvparser.infrastructure.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Objects;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mobinrt.csvparser.domain.model.ErrorRow;
import io.github.mobinrt.csvparser.domain.ports.ErrorWriter;

/**
 * Persists parsing/validation/import errors into MySQL table
 * {@code error_rows}.
 *
 * <p>
 * Important: this writer guarantees durability even if the DataSource returns
 * connections with {@code autoCommit=false} (common with pools). In that case
 * it commits after each insert.</p>
 */
public final class MySqlErrorWriter implements ErrorWriter {

    private static final Logger log = LoggerFactory.getLogger(MySqlErrorWriter.class);

    private final DataSource dataSource;

    public MySqlErrorWriter(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public void ensureErrorTableExist() {
        String ddl = """
                CREATE TABLE IF NOT EXISTS `error_rows` (
                  `id` BIGINT NOT NULL AUTO_INCREMENT,
                  `source_file` VARCHAR(1024) NULL,
                  `row_num` BIGINT NULL,
                  `raw_row` LONGTEXT NULL,
                  `error_message` LONGTEXT NULL,
                  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`)
                ) ENGINE=InnoDB
                """;

        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            st.execute(ddl);
            log.info("Ensured error table exists: error_rows");
            log.info("MySqlErrorWriter BUILD_MARKER=ERRWRITER_2026_02_10_2005");
            log.info("Ensured error table exists: error_rows");

        } catch (Exception e) {
            throw new IllegalStateException("Failed to create/verify error_rows table", e);
        }
    }

    @Override
    public void writeErrorRow(ErrorRow errorRow) {
        Objects.requireNonNull(errorRow, "errorRow");

        String sql = """
                INSERT INTO `error_rows` (`source_file`, `row_num`, `raw_row`, `error_message`)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql); Statement st = c.createStatement()) {

            boolean auto = c.getAutoCommit();

            ps.setString(1, errorRow.getSourceFile());
            if (errorRow.getRowNumber() == null) {
                ps.setObject(2, null);
            } else {
                ps.setLong(2, errorRow.getRowNumber());
            }
            ps.setString(3, errorRow.getRawRow());
            ps.setString(4, errorRow.getErrorMessage());

            int affected = ps.executeUpdate();

            if (!auto) {
                c.commit();
            }

            String serverUuid = "unknown";
            int port = -1;
            String db = "unknown";
            try (var rs = st.executeQuery("SELECT @@server_uuid, @@port, DATABASE()")) {
                if (rs.next()) {
                    serverUuid = rs.getString(1);
                    port = rs.getInt(2);
                    db = rs.getString(3);
                }
            }

            log.info("error_rows insert done (affected={}, autoCommit={}, db={}, server_uuid={}, server_port={}, url={})",
                    affected, auto, db, serverUuid, port, c.getMetaData().getURL());

        } catch (Exception e) {
            throw new IllegalStateException("Failed to insert into error_rows", e);
        }
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "?" : s;
    }
}
