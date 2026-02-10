package io.github.mobinrt.csvparser.infrastructure.db;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mobinrt.csvparser.domain.model.ErrorRow;
import io.github.mobinrt.csvparser.domain.model.RowData;
import io.github.mobinrt.csvparser.domain.ports.ErrorWriter;

/**
 * Performs JDBC batch inserts with resilient error handling for streaming
 * imports.
 *
 * <p>
 * This class supports two insertion strategies:</p>
 * <ul>
 * <li><b>Savepoint isolation</b>: Always deterministic. On batch failure, rolls
 * back to a savepoint and isolates bad rows.</li>
 * <li><b>UpdateCounts optimization</b>: Attempts to keep a successful prefix
 * using {@link BatchUpdateException#getUpdateCounts()}, then isolates only the
 * remaining tail when update counts are clearly reliable; otherwise falls back
 * to savepoint isolation.</li>
 * </ul>
 *
 * <p>
 * Prerequisite: the caller must set
 * {@code connection.setAutoCommit(false)}.</p>
 */
public final class MySqlBatchInserter {

    private static final Logger log = LoggerFactory.getLogger(MySqlBatchInserter.class);

    /**
     * Inserts a batch under a savepoint. If the batch fails, rolls back to the
     * savepoint and isolates failing rows.
     *
     * @param connection the JDBC connection (must be in a transaction;
     * autoCommit=false)
     * @param insertSql the parameterized INSERT SQL
     * @param items the rows and their JDBC values to insert
     * @param errorWriter writer used to persist isolated failing rows
     * @return number of successfully inserted rows
     */
    public long insertWithSavepointIsolation(
            Connection connection,
            String insertSql,
            List<InsertItem> items,
            ErrorWriter errorWriter
    ) {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(insertSql, "insertSql");
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(errorWriter, "errorWriter");

        if (items.isEmpty()) {
            return 0;
        }

        if (log.isDebugEnabled()) {
            log.debug("Inserting batch with savepoint isolation (size={})", items.size());
        }

        Savepoint sp = createSavepoint(connection);
        try {
            executeBatch(connection, insertSql, items);
            if (log.isDebugEnabled()) {
                log.debug("Batch insert succeeded (size={})", items.size());
            }
            return items.size();
        } catch (Exception ex) {
            log.warn("Batch insert failed; rolling back to savepoint and isolating (size={}, error={})",
                    items.size(), safeMessage(ex));
            rollbackToSavepointQuietly(connection, sp);
            return isolateByDivideAndConquer(connection, insertSql, items, errorWriter, ex);
        }
    }

    /**
     * Inserts a batch and, on {@link BatchUpdateException}, attempts to keep a
     * successful prefix using update counts. If update counts are not clearly
     * reliable, it falls back to deterministic savepoint isolation.
     *
     * @param connection the JDBC connection (must be in a transaction;
     * autoCommit=false)
     * @param insertSql the parameterized INSERT SQL
     * @param items the rows and their JDBC values to insert
     * @param errorWriter writer used to persist isolated failing rows
     * @return number of successfully inserted rows
     */
    public long insertWithUpdateCountsOptimization(
            Connection connection,
            String insertSql,
            List<InsertItem> items,
            ErrorWriter errorWriter
    ) {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(insertSql, "insertSql");
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(errorWriter, "errorWriter");

        if (items.isEmpty()) {
            return 0;
        }

        if (log.isDebugEnabled()) {
            log.debug("Inserting batch with updateCounts optimization (size={})", items.size());
        }

        Savepoint sp = createSavepoint(connection);

        try {
            executeBatch(connection, insertSql, items);
            if (log.isDebugEnabled()) {
                log.debug("Batch insert succeeded (size={})", items.size());
            }
            return items.size();

        } catch (BatchUpdateException bue) {
            PrefixDecision decision = decideReliableSuccessPrefix(bue.getUpdateCounts(), items.size());

            if (!decision.isReliable()) {
                log.warn("BatchUpdateException updateCounts unreliable; falling back to savepoint isolation (size={}, error={})",
                        items.size(), safeMessage(bue));
                rollbackToSavepointQuietly(connection, sp);
                return isolateByDivideAndConquer(connection, insertSql, items, errorWriter, bue);
            }

            int prefixSucceeded = decision.getSucceededPrefix();
            if (log.isDebugEnabled()) {
                log.debug("BatchUpdateException updateCounts reliable; succeededPrefix={} of {}", prefixSucceeded, items.size());
            }

            if (prefixSucceeded <= 0) {
                log.warn("updateCounts indicates no reliable successes; falling back to savepoint isolation (size={}, error={})",
                        items.size(), safeMessage(bue));
                rollbackToSavepointQuietly(connection, sp);
                return isolateByDivideAndConquer(connection, insertSql, items, errorWriter, bue);
            }

            if (prefixSucceeded >= items.size()) {
                if (log.isDebugEnabled()) {
                    log.debug("updateCounts indicates full batch succeeded despite exception (size={})", items.size());
                }
                return items.size();
            }

            List<InsertItem> tail = new ArrayList<>(items.subList(prefixSucceeded, items.size()));
            long tailInserted = insertWithSavepointIsolation(connection, insertSql, tail, errorWriter);
            long total = prefixSucceeded + tailInserted;

            if (log.isInfoEnabled()) {
                log.info("Kept successful prefix and isolated tail (prefixSucceeded={}, tailSize={}, tailInserted={}, totalInserted={})",
                        prefixSucceeded, tail.size(), tailInserted, total);
            }

            return total;

        } catch (Exception ex) {
            log.warn("Batch insert failed (non-batch exception); falling back to savepoint isolation (size={}, error={})",
                    items.size(), safeMessage(ex));
            rollbackToSavepointQuietly(connection, sp);
            return isolateByDivideAndConquer(connection, insertSql, items, errorWriter, ex);
        }
    }

    private void executeBatch(Connection connection, String insertSql, List<InsertItem> items) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
            for (InsertItem item : items) {
                bind(ps, item.getJdbcValues());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void bind(PreparedStatement ps, Object[] values) throws SQLException {
        for (int i = 0; i < values.length; i++) {
            ps.setObject(i + 1, values[i]);
        }
    }

    private long isolateByDivideAndConquer(
            Connection connection,
            String insertSql,
            List<InsertItem> items,
            ErrorWriter errorWriter,
            Exception cause
    ) {
        if (items.isEmpty()) {
            return 0;
        }

        if (items.size() == 1) {
            return tryInsertSingleOrReport(connection, insertSql, items.get(0), errorWriter, cause);
        }

        int mid = items.size() / 2;
        List<InsertItem> left = new ArrayList<>(items.subList(0, mid));
        List<InsertItem> right = new ArrayList<>(items.subList(mid, items.size()));

        if (log.isDebugEnabled()) {
            log.debug("Isolating batch by split (size={}, left={}, right={})", items.size(), left.size(), right.size());
        }

        long inserted = 0;
        inserted += insertWithSavepointIsolation(connection, insertSql, left, errorWriter);
        inserted += insertWithSavepointIsolation(connection, insertSql, right, errorWriter);
        return inserted;
    }

    private long tryInsertSingleOrReport(
            Connection connection,
            String insertSql,
            InsertItem item,
            ErrorWriter errorWriter,
            Exception originalCause
    ) {
        Savepoint sp = createSavepoint(connection);
        try {
            executeBatch(connection, insertSql, List.of(item));
            if (log.isDebugEnabled()) {
                RowData row = item.getRow();
                log.debug("Single-row insert succeeded (sourceFile={}, rowNumber={})", row.getSourceFile(), row.getRowNumber());
            }
            return 1;
        } catch (Exception ex) {
            rollbackToSavepointQuietly(connection, sp);

            RowData row = item.getRow();
            log.warn("Single-row insert failed; writing to error table (sourceFile={}, rowNumber={}, error={})",
                    row.getSourceFile(), row.getRowNumber(), safeMessage(ex));

            errorWriter.writeErrorRow(new ErrorRow(
                    row.getSourceFile(),
                    row.getRowNumber(),
                    row.getRawRow(),
                    "DB insert failed for row (isolated). original=" + safeMessage(originalCause)
                    + " current=" + safeMessage(ex)
            ));
            return 0;
        }
    }

    private Savepoint createSavepoint(Connection connection) {
        try {
            return connection.setSavepoint();
        } catch (SQLException e) {
            log.error("Failed to create savepoint (autoCommit must be false). error={}", safeMessage(e));
            throw new IllegalStateException("Failed to create savepoint (ensure autoCommit=false)", e);
        }
    }

    private void rollbackToSavepointQuietly(Connection connection, Savepoint sp) {
        try {
            connection.rollback(sp);
        } catch (SQLException e) {
            log.warn("Rollback to savepoint failed. error={}", safeMessage(e));
        }
    }

    private PrefixDecision decideReliableSuccessPrefix(int[] updateCounts, int batchSize) {
        if (updateCounts == null || updateCounts.length == 0) {
            return PrefixDecision.unreliable();
        }

        if (batchSize > 1 && updateCounts.length == 1) {
            return PrefixDecision.unreliable();
        }

        if (updateCounts.length > batchSize) {
            return PrefixDecision.unreliable();
        }

        if (updateCounts.length == batchSize) {
            int firstFailed = firstIndexOf(updateCounts, Statement.EXECUTE_FAILED);
            if (firstFailed < 0) {
                return PrefixDecision.unreliable();
            }
            for (int i = 0; i < firstFailed; i++) {
                if (!isSuccessCount(updateCounts[i])) {
                    return PrefixDecision.unreliable();
                }
            }
            return PrefixDecision.reliable(firstFailed);
        }

        if (updateCounts.length > 1) {
            for (int c : updateCounts) {
                if (!isSuccessCount(c)) {
                    return PrefixDecision.unreliable();
                }
            }
            return PrefixDecision.reliable(updateCounts.length);
        }

        return PrefixDecision.unreliable();
    }

    private boolean isSuccessCount(int c) {
        return c >= 0 || c == Statement.SUCCESS_NO_INFO;
    }

    private int firstIndexOf(int[] arr, int value) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == value) {
                return i;
            }
        }
        return -1;
    }

    private String safeMessage(Exception ex) {
        if (ex == null) {
            return "null";
        }
        String m = ex.getMessage();
        return (m == null || m.isBlank()) ? ex.getClass().getSimpleName() : m;
    }

    private static final class PrefixDecision {

        private final boolean reliable;
        private final int succeededPrefix;

        private PrefixDecision(boolean reliable, int succeededPrefix) {
            this.reliable = reliable;
            this.succeededPrefix = succeededPrefix;
        }

        static PrefixDecision reliable(int succeededPrefix) {
            return new PrefixDecision(true, succeededPrefix);
        }

        static PrefixDecision unreliable() {
            return new PrefixDecision(false, 0);
        }

        boolean isReliable() {
            return reliable;
        }

        int getSucceededPrefix() {
            return succeededPrefix;
        }
    }

    /**
     * A single insert unit: the source row (for reporting) and the converted
     * JDBC parameter values.
     */
    public static final class InsertItem {

        private final RowData row;
        private final Object[] jdbcValues;

        public InsertItem(RowData row, Object[] jdbcValues) {
            this.row = Objects.requireNonNull(row, "row");
            this.jdbcValues = Objects.requireNonNull(jdbcValues, "jdbcValues").clone();
        }

        public RowData getRow() {
            return row;
        }

        public Object[] getJdbcValues() {
            return jdbcValues.clone();
        }
    }
}
