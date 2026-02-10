package net.noworks.clionesql.boot;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import tetz42.clione.SQLExecutor;

import org.junit.jupiter.api.Test;

import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClioneSqlTemplateTest {

    @Test
    void wrapsDataSourceWithTransactionAwareProxy() {
        DataSource dataSource = mock(DataSource.class);
        ClioneSqlProperties properties = new ClioneSqlProperties();

        ClioneSqlTemplate template = new ClioneSqlTemplate(dataSource, properties);
        assertThat(template).isNotNull();
    }

    @Test
    void doesNotDoubleWrapTransactionAwareDataSourceProxy() {
        DataSource rawDataSource = mock(DataSource.class);
        TransactionAwareDataSourceProxy proxy = new TransactionAwareDataSourceProxy(rawDataSource);
        ClioneSqlProperties properties = new ClioneSqlProperties();

        ClioneSqlTemplate template = new ClioneSqlTemplate(proxy, properties);
        assertThat(template).isNotNull();
    }

    @Test
    void useFileCreatesExecutor() throws SQLException {
        ClioneSqlTemplate template = createTemplate(new ClioneSqlProperties());

        SQLExecutor executor = template.useFile("sql/test/SelectOne.sql");
        assertThat(executor).isNotNull();
    }

    @Test
    void useFileWithProductName() throws SQLException {
        ClioneSqlProperties properties = new ClioneSqlProperties();
        properties.setProductName("postgres");
        ClioneSqlTemplate template = createTemplate(properties);

        SQLExecutor executor = template.useFile("sql/test/SelectOne.sql");
        assertThat(executor).isNotNull();
    }

    @Test
    void useSQLCreatesExecutor() throws SQLException {
        ClioneSqlTemplate template = createTemplate(new ClioneSqlProperties());

        SQLExecutor executor = template.useSQL("SELECT 1");
        assertThat(executor).isNotNull();
    }

    @Test
    void useFileWithClassCreatesExecutor() throws SQLException {
        ClioneSqlTemplate template = createTemplate(new ClioneSqlProperties());

        SQLExecutor executor = template.useFile(ClioneSqlTemplateTest.class, "SelectByClass.sql");
        assertThat(executor).isNotNull();
    }

    private ClioneSqlTemplate createTemplate(ClioneSqlProperties properties) throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        return new ClioneSqlTemplate(dataSource, properties);
    }
}
