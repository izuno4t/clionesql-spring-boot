package net.noworks.clionesql.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for clione-sql integration with Spring Boot.
 *
 * <p>
 * Properties are bound from the {@code clione-sql} prefix in application configuration (e.g. {@code application.yml} or
 * {@code application.properties}).
 *
 * @see ClioneSqlTemplate
 */
@ConfigurationProperties(prefix = "clione-sql")
public class ClioneSqlProperties {

    /** Creates a new {@code ClioneSqlProperties} with default values. */
    public ClioneSqlProperties() {
    }

    /**
     * Database product name used by clione-sql for SQL dialect selection. Supported values: oracle, mysql, postgres,
     * db2, sqlserver, sqlite, firebird. When {@code null}, clione-sql auto-detects the product from the connection
     * metadata.
     */
    private String productName;

    /**
     * Character encoding for SQL template files. Defaults to {@code "UTF-8"}.
     */
    private String sqlFileEncoding = "UTF-8";

    /**
     * Whether to enable translation of clione-sql exceptions into Spring's
     * {@link org.springframework.dao.DataAccessException} hierarchy. Defaults to {@code true}.
     */
    private boolean exceptionTranslationEnabled = true;

    /**
     * Whether to enable development mode, which disables SQL file caching so that changes to SQL files are picked up
     * without restarting the application. Defaults to {@code false}.
     */
    private boolean developmentMode = false;

    /**
     * SQL file cache duration in milliseconds. Only effective when {@link #isDevelopmentMode() developmentMode} is
     * {@code true}. Defaults to {@code 0}.
     */
    private int sqlFileCacheTime = 0;

    /**
     * Maximum nesting depth for entity mapping. Defaults to {@code 8}.
     */
    private int entityDepthLimit = 8;

    /**
     * Returns the database product name.
     *
     * @return the database product name, or {@code null} if not set
     */
    public String getProductName() {
        return productName;
    }

    /**
     * Sets the database product name.
     *
     * @param productName
     *            the database product name
     */
    public void setProductName(String productName) {
        this.productName = productName;
    }

    /**
     * Returns the SQL file encoding.
     *
     * @return the character encoding name
     */
    public String getSqlFileEncoding() {
        return sqlFileEncoding;
    }

    /**
     * Sets the SQL file encoding.
     *
     * @param sqlFileEncoding
     *            the character encoding name
     */
    public void setSqlFileEncoding(String sqlFileEncoding) {
        this.sqlFileEncoding = sqlFileEncoding;
    }

    /**
     * Returns whether exception translation is enabled.
     *
     * @return {@code true} if exception translation is enabled
     */
    public boolean isExceptionTranslationEnabled() {
        return exceptionTranslationEnabled;
    }

    /**
     * Sets whether exception translation is enabled.
     *
     * @param exceptionTranslationEnabled
     *            {@code true} to enable, {@code false} to disable
     */
    public void setExceptionTranslationEnabled(boolean exceptionTranslationEnabled) {
        this.exceptionTranslationEnabled = exceptionTranslationEnabled;
    }

    /**
     * Returns whether development mode is enabled.
     *
     * @return {@code true} if development mode is enabled
     */
    public boolean isDevelopmentMode() {
        return developmentMode;
    }

    /**
     * Sets whether development mode is enabled.
     *
     * @param developmentMode
     *            {@code true} to enable, {@code false} to disable
     */
    public void setDevelopmentMode(boolean developmentMode) {
        this.developmentMode = developmentMode;
    }

    /**
     * Returns the SQL file cache duration in milliseconds.
     *
     * @return the cache duration in milliseconds
     */
    public int getSqlFileCacheTime() {
        return sqlFileCacheTime;
    }

    /**
     * Sets the SQL file cache duration in milliseconds.
     *
     * @param sqlFileCacheTime
     *            the cache duration in milliseconds
     */
    public void setSqlFileCacheTime(int sqlFileCacheTime) {
        this.sqlFileCacheTime = sqlFileCacheTime;
    }

    /**
     * Returns the entity nesting depth limit.
     *
     * @return the maximum nesting depth
     */
    public int getEntityDepthLimit() {
        return entityDepthLimit;
    }

    /**
     * Sets the entity nesting depth limit.
     *
     * @param entityDepthLimit
     *            the maximum nesting depth
     */
    public void setEntityDepthLimit(int entityDepthLimit) {
        this.entityDepthLimit = entityDepthLimit;
    }
}
