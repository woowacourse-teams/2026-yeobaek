package yeobaek.backend.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import javax.sql.DataSource;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

class DatabaseCleanerFailureTest {

    @Test
    @DisplayName("테이블 정리와 외래 키 복구가 모두 실패하면 원래 실패를 보존하고 연결을 폐기한다")
    void preserveCleaningFailureAndAbortConnectionWhenRestoreFails() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        SQLException cleaningFailure = new SQLException("truncate failed");
        SQLException restorationFailure = new SQLException("restore failed");

        try (Connection connection = mock(Connection.class);
                Statement cleaningStatement = mock(Statement.class);
                Statement restorationStatement = mock(Statement.class);
                CachedRowSet tables = tableNames("members")) {
            given(dataSource.getConnection()).willReturn(connection);
            given(connection.getMetaData()).willReturn(metadata);
            given(metadata.getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"})).willReturn(tables);
            given(connection.createStatement()).willReturn(cleaningStatement, restorationStatement);
            doThrow(cleaningFailure).when(cleaningStatement).execute("TRUNCATE TABLE `members`");
            doThrow(restorationFailure).when(restorationStatement).execute("SET FOREIGN_KEY_CHECKS = 1");

            assertThatThrownBy(() -> new DatabaseCleaner(dataSource).clean())
                    .isInstanceOf(DataAccessException.class)
                    .satisfies(exception -> {
                        assertThat(exception.getCause()).isSameAs(cleaningFailure);
                        assertThat(cleaningFailure.getSuppressed()).contains(restorationFailure);
                    });
            verify(connection).abort(any());
        }
    }

    private CachedRowSet tableNames(String tableName) throws SQLException {
        RowSetMetaDataImpl metadata = new RowSetMetaDataImpl();
        metadata.setColumnCount(1);
        metadata.setColumnName(1, "TABLE_NAME");
        metadata.setColumnLabel(1, "TABLE_NAME");
        metadata.setColumnType(1, Types.VARCHAR);

        CachedRowSet tables = RowSetProvider.newFactory().createCachedRowSet();
        tables.setMetaData(metadata);
        tables.moveToInsertRow();
        tables.updateString("TABLE_NAME", tableName);
        tables.insertRow();
        tables.moveToCurrentRow();
        tables.beforeFirst();
        return tables;
    }
}
