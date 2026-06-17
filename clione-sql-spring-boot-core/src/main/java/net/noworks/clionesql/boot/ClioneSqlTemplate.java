package net.noworks.clionesql.boot;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.sql.DataSource;

import tetz42.clione.SQLExecutor;
import tetz42.clione.SQLManager;
import tetz42.clione.util.ResultMap;

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
 * List&lt;ResultMap&gt; results = clioneSqlTemplate.useFile("person/SelectAll.sql").findAll();
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
     *            the classpath-relative path to the SQL file (e.g. {@code "person/SelectAll.sql"} when
     *            {@code clione-sql.sql-file-prefix=sql})
     *
     * @return a {@link SQLExecutor} ready to bind parameters and execute the query
     */
    public SQLExecutor useFile(String sqlPath) {
        return createSQLManager().useFile(resolveSqlPath(sqlPath));
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
     * Executes an action against a {@link SQLExecutor} loaded from the given SQL file, guaranteeing that the underlying
     * connection is returned after execution.
     *
     * <p>
     * Unlike {@link #useFile(String)}, this method obtains the connection via {@link DataSourceUtils} and releases it
     * with {@link DataSourceUtils#releaseConnection(Connection, DataSource)} in a {@code finally} block. Outside a
     * Spring-managed transaction this returns the connection to the pool (preventing connection leaks); within a
     * transaction the release is a no-op and the connection is closed at transaction completion. Prefer this method
     * (and the {@code query}/{@code queryForList}/{@code update} helpers) over {@link #useFile(String)} for
     * non-transactional access.
     *
     * @param sqlPath
     *            the classpath-relative path to the SQL file (e.g. {@code "person/SelectAll.sql"})
     * @param action
     *            the action to apply to the prepared {@link SQLExecutor}; typically calls {@code find}, {@code findAll}
     *            or {@code update}
     * @param <R>
     *            the result type produced by {@code action}
     *
     * @return the result of applying {@code action}
     */
    public <R> R execute(String sqlPath, Function<SQLExecutor, R> action) {
        return executeInternal(manager -> manager.useFile(resolveSqlPath(sqlPath)), action);
    }

    /**
     * Executes the SQL file and returns a single result, releasing the connection afterward.
     *
     * @param sqlPath
     *            the classpath-relative path to the SQL file
     *
     * @return the single matching row, or {@code null} if none
     */
    public ResultMap query(String sqlPath) {
        return execute(sqlPath, ex -> ex.find());
    }

    /**
     * Executes the SQL file with the given parameters and returns a single result, releasing the connection afterward.
     *
     * @param sqlPath
     *            the classpath-relative path to the SQL file
     * @param params
     *            the named parameters (e.g. created via {@link SQLManager#params(String, Object)})
     *
     * @return the single matching row, or {@code null} if none
     */
    public ResultMap query(String sqlPath, Map<String, Object> params) {
        return execute(sqlPath, ex -> ex.find(params));
    }

    /**
     * Executes the SQL file and returns all results, releasing the connection afterward.
     *
     * @param sqlPath
     *            the classpath-relative path to the SQL file
     *
     * @return the list of matching rows
     */
    public List<ResultMap> queryForList(String sqlPath) {
        return execute(sqlPath, ex -> ex.findAll());
    }

    /**
     * Executes the SQL file with the given parameters and returns all results, releasing the connection afterward.
     *
     * @param sqlPath
     *            the classpath-relative path to the SQL file
     * @param params
     *            the named parameters (e.g. created via {@link SQLManager#params(String, Object)})
     *
     * @return the list of matching rows
     */
    public List<ResultMap> queryForList(String sqlPath, Map<String, Object> params) {
        return execute(sqlPath, ex -> ex.findAll(params));
    }

    /**
     * Executes the SQL file as an update (INSERT/UPDATE/DELETE), releasing the connection afterward.
     *
     * @param sqlPath
     *            the classpath-relative path to the SQL file
     *
     * @return the number of affected rows
     */
    public int update(String sqlPath) {
        return execute(sqlPath, ex -> ex.update());
    }

    /**
     * Executes the SQL file as an update (INSERT/UPDATE/DELETE) with the given parameters, releasing the connection
     * afterward.
     *
     * @param sqlPath
     *            the classpath-relative path to the SQL file
     * @param params
     *            the named parameters (e.g. created via {@link SQLManager#params(String, Object)})
     *
     * @return the number of affected rows
     */
    public int update(String sqlPath, Map<String, Object> params) {
        return execute(sqlPath, ex -> ex.update(params));
    }

    /**
     * Runs the given action with a freshly created {@link SQLManager}, ensuring the statement is closed and the
     * connection is released in a {@code finally} block.
     *
     * @param open
     *            opens a {@link SQLExecutor} from the manager (e.g. {@code useFile})
     * @param action
     *            the action to apply to the opened {@link SQLExecutor}
     * @param <R>
     *            the result type produced by {@code action}
     *
     * @return the result of applying {@code action}
     */
    private <R> R executeInternal(Function<SQLManager, SQLExecutor> open, Function<SQLExecutor, R> action) {
        Connection con = DataSourceUtils.getConnection(this.dataSource);
        SQLManager manager = createSQLManager(con);
        try {
            return action.apply(open.apply(manager));
        } finally {
            manager.closeStatement();
            DataSourceUtils.releaseConnection(con, this.dataSource);
        }
    }

    /**
     * Creates a new {@link SQLManager} instance with a connection obtained from the Spring-managed data source.
     *
     * @return a new {@link SQLManager} bound to the current transaction's connection
     */
    private SQLManager createSQLManager() {
        return createSQLManager(DataSourceUtils.getConnection(this.dataSource));
    }

    /**
     * Creates a new {@link SQLManager} instance bound to the given connection.
     *
     * @param con
     *            the connection to bind
     *
     * @return a new {@link SQLManager}
     */
    private SQLManager createSQLManager(Connection con) {
        String productName = properties.getProductName();
        if (productName != null) {
            return SQLManager.sqlManager(con, productName);
        }
        return SQLManager.sqlManager(con);
    }

    private String resolveSqlPath(String sqlPath) {
        String prefix = normalizePath(properties.getSqlFilePrefix());
        String path = normalizePath(sqlPath);
        if (prefix == null || path == null || path.startsWith(prefix + "/") || path.equals(prefix)) {
            return path;
        }
        return prefix + "/" + path;
    }

    private String normalizePath(String path) {
        if (path == null) {
            return null;
        }
        String normalized = path.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        int start = 0;
        int end = normalized.length();
        while (start < end && normalized.charAt(start) == '/') {
            start++;
        }
        while (end > start && normalized.charAt(end - 1) == '/') {
            end--;
        }
        return normalized.substring(start, end);
    }
}
