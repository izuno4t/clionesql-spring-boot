package net.noworks.clionesql.boot;

import java.sql.Connection;

import javax.sql.DataSource;

import tetz42.clione.SQLExecutor;
import tetz42.clione.SQLManager;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

/**
 * Spring-integrated facade for clione-sql's {@link SQLManager}.
 *
 * <p>
 * This template participates in Spring-managed transactions by wrapping the {@link DataSource} with a
 * {@link TransactionAwareDataSourceProxy} and obtaining connections via
 * {@link DataSourceUtils#getConnection(DataSource)}.
 *
 * <p>
 * A new {@link SQLManager} instance is created for each operation because {@code SQLManager} is not thread-safe.
 *
 * <p>
 * Usage examples:
 *
 * <pre>
 * // Using an external SQL file
 * List&lt;ResultMap&gt; results = clioneSqlTemplate.useFile("sql/person/SelectAll.sql").findAll();
 *
 * // Using an inline SQL string (2Way SQL)
 * ResultMap result = clioneSqlTemplate.useSQL("SELECT * FROM person WHERE id = &#47;* id *&#47;'1'")
 *         .find(SQLManager.params("id", 1));
 * </pre>
 *
 * @see ClioneSqlProperties
 * @see SQLManager
 */
public class ClioneSqlTemplate {

    private final DataSource dataSource;
    private final ClioneSqlProperties properties;

    /**
     * Creates a new {@code ClioneSqlTemplate}.
     *
     * <p>
     * If the given {@code dataSource} is not already a {@link TransactionAwareDataSourceProxy}, it will be wrapped in
     * one to ensure Spring transaction participation.
     *
     * @param dataSource
     *            the data source to use for obtaining connections
     * @param properties
     *            the clione-sql configuration properties
     */
    public ClioneSqlTemplate(DataSource dataSource, ClioneSqlProperties properties) {
        if (dataSource instanceof TransactionAwareDataSourceProxy) {
            this.dataSource = dataSource;
        } else {
            this.dataSource = new TransactionAwareDataSourceProxy(dataSource);
        }
        this.properties = properties;
    }

    /**
     * Loads a 2Way SQL template from the specified file path on the classpath.
     *
     * @param sqlPath
     *            the classpath-relative path to the SQL file (e.g. {@code "sql/person/SelectAll.sql"})
     *
     * @return a {@link SQLExecutor} ready to bind parameters and execute the query
     */
    public SQLExecutor useFile(String sqlPath) {
        return createSQLManager().useFile(sqlPath);
    }

    /**
     * Loads a 2Way SQL template from a file located relative to the given class.
     *
     * @param clazz
     *            the class whose package is used to resolve the SQL file location
     * @param sqlFile
     *            the SQL file name relative to the class's package
     *
     * @return a {@link SQLExecutor} ready to bind parameters and execute the query
     */
    public SQLExecutor useFile(Class<?> clazz, String sqlFile) {
        return createSQLManager().useFile(clazz, sqlFile);
    }

    /**
     * Uses an inline 2Way SQL string directly.
     *
     * @param sql
     *            the 2Way SQL string with clione-sql parameter placeholders
     *
     * @return a {@link SQLExecutor} ready to bind parameters and execute the query
     */
    public SQLExecutor useSQL(String sql) {
        return createSQLManager().useSQL(sql);
    }

    /**
     * Creates a new {@link SQLManager} instance with a connection obtained from the Spring-managed data source.
     *
     * @return a new {@link SQLManager} bound to the current transaction's connection
     */
    private SQLManager createSQLManager() {
        Connection con = DataSourceUtils.getConnection(this.dataSource);
        String productName = properties.getProductName();
        if (productName != null) {
            return SQLManager.sqlManager(con, productName);
        }
        return SQLManager.sqlManager(con);
    }
}
