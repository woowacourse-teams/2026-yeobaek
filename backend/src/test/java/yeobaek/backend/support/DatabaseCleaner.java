package yeobaek.backend.support;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

public class DatabaseCleaner {

    private static final String TABLE_TYPE = "TABLE";
    private static final String DISABLE_FOREIGN_KEY_CHECKS = "SET FOREIGN_KEY_CHECKS = 0";
    private static final String ENABLE_FOREIGN_KEY_CHECKS = "SET FOREIGN_KEY_CHECKS = 1";

    private final JdbcTemplate jdbcTemplate;

    public DatabaseCleaner(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void clean() {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            clean(connection);
            return null;
        });
    }

    private void clean(Connection connection) throws SQLException {
        List<String> tableNames = findTableNames(connection);

        try (Statement statement = connection.createStatement()) {
            statement.execute(DISABLE_FOREIGN_KEY_CHECKS);
            SQLException cleaningFailure = null;
            try {
                for (String tableName : tableNames) {
                    statement.execute("TRUNCATE TABLE `" + escapeIdentifier(tableName) + "`");
                }
            } catch (SQLException exception) {
                cleaningFailure = exception;
                throw exception;
            } finally {
                restoreForeignKeyChecks(connection, cleaningFailure);
            }
        }
    }

    private void restoreForeignKeyChecks(Connection connection, SQLException cleaningFailure) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(ENABLE_FOREIGN_KEY_CHECKS);
        } catch (SQLException restorationFailure) {
            abortConnection(connection, restorationFailure);
            if (cleaningFailure == null) {
                throw restorationFailure;
            }
            cleaningFailure.addSuppressed(restorationFailure);
        }
    }

    private void abortConnection(Connection connection, SQLException failure) {
        try {
            connection.abort(Runnable::run);
        } catch (SQLException abortFailure) {
            failure.addSuppressed(abortFailure);
        }
    }

    private List<String> findTableNames(Connection connection) throws SQLException {
        List<String> tableNames = new ArrayList<>();
        DatabaseMetaData metadata = connection.getMetaData();

        try (ResultSet tables = metadata.getTables(connection.getCatalog(), null, "%", new String[]{TABLE_TYPE})) {
            while (tables.next()) {
                tableNames.add(tables.getString("TABLE_NAME"));
            }
        }

        return tableNames;
    }

    private String escapeIdentifier(String identifier) {
        return identifier.replace("`", "``");
    }
}
