package io.github.mobinrt.csvparser.infrastructure.db;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mobinrt.csvparser.domain.model.ColumnDef;
import io.github.mobinrt.csvparser.domain.model.Schema;
import io.github.mobinrt.csvparser.domain.ports.TableWriter;

public final class MySqlTableWriter implements TableWriter {

    private static final Logger log = LoggerFactory.getLogger(MySqlTableWriter.class);

    private final DataSource dataSource;
    private final SqlTypeMapper typeMapper = new SqlTypeMapper();

    public MySqlTableWriter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void ensureDataTableExists(Schema schema, String tableOverride, List<String> includeColumns) {
        Objects.requireNonNull(schema, "schema");

        String tableName = chooseTableName(schema, tableOverride);
        SqlIdentifier.requireSafe("table name", tableName);

        List<ColumnDef> selectedColumns = selectColumns(schema, includeColumns);
        String ddl = buildCreateTableDdl(tableName, selectedColumns);

        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            st.execute(ddl);
            log.info("Ensured data table exists: {} (columns={})", tableName, selectedColumns.size());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create/verify data table: " + tableName, e);
        }
    }

    private String chooseTableName(Schema schema, String tableOverride) {
        if (tableOverride != null && !tableOverride.isBlank()) {
            return tableOverride.trim();
        }
        return schema.getTableName();
    }

    private List<ColumnDef> selectColumns(Schema schema, List<String> includeColumns) {
        List<ColumnDef> all = schema.getColumns();
        if (includeColumns == null || includeColumns.isEmpty()) {
            return all;
        }

        Map<String, ColumnDef> byLower = new HashMap<>();
        for (ColumnDef c : all) {
            byLower.put(c.getName().toLowerCase(Locale.ROOT), c);
        }

        List<ColumnDef> selected = new ArrayList<>();
        for (String name : includeColumns) {
            if (name == null || name.isBlank()) {
                continue;
            }
            String key = name.trim().toLowerCase(Locale.ROOT);
            ColumnDef col = byLower.get(key);
            if (col == null) {
                throw new IllegalArgumentException("include-columns contains unknown column: " + name);
            }
            selected.add(col);
        }

        if (selected.isEmpty()) {
            throw new IllegalArgumentException("include-columns resolved to empty selection");
        }
        return List.copyOf(selected);
    }

    private String buildCreateTableDdl(String tableName, List<ColumnDef> columns) {
        String cols = columns.stream()
                .map(c -> {
                    SqlIdentifier.requireSafe("column name", c.getName());
                    String type = typeMapper.toMySqlType(c.getType());
                    return SqlIdentifier.quote(c.getName()) + " " + type + " NULL";
                })
                .collect(Collectors.joining(",\n  "));

        return """
                CREATE TABLE IF NOT EXISTS %s (
                  %s
                ) ENGINE=InnoDB
                """.formatted(SqlIdentifier.quote(tableName), cols);
    }
}
