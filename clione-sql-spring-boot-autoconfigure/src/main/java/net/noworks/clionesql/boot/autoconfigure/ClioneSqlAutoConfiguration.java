package net.noworks.clionesql.boot.autoconfigure;

import javax.sql.DataSource;

import tetz42.clione.SQLManager;

import net.noworks.clionesql.boot.ClioneSqlPersistenceExceptionTranslator;
import net.noworks.clionesql.boot.ClioneSqlProperties;
import net.noworks.clionesql.boot.ClioneSqlTemplate;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.support.PersistenceExceptionTranslator;

/**
 * Spring Boot {@link AutoConfiguration} for clione-sql.
 *
 * <p>
 * This configuration is activated when {@link SQLManager} is on the classpath and runs after
 * {@link DataSourceAutoConfiguration} to ensure a {@link DataSource} is available.
 *
 * <p>
 * It registers the following beans:
 * <ul>
 * <li>{@link ClioneSqlTemplate} &mdash; the main facade for executing 2Way SQL. Only created when a {@link DataSource}
 * bean exists and no user-defined {@code ClioneSqlTemplate} is present.</li>
 * <li>{@link PersistenceExceptionTranslator} &mdash; translates clione-sql exceptions to Spring's
 * {@code DataAccessException} hierarchy. Can be disabled via
 * {@code clione-sql.exception-translation-enabled=false}.</li>
 * </ul>
 *
 * @see ClioneSqlProperties
 * @see ClioneSqlTemplate
 * @see ClioneSqlPersistenceExceptionTranslator
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnClass(SQLManager.class)
@EnableConfigurationProperties(ClioneSqlProperties.class)
public class ClioneSqlAutoConfiguration {

    /** Creates a new {@code ClioneSqlAutoConfiguration}. */
    public ClioneSqlAutoConfiguration() {
    }

    /**
     * Creates a {@link ClioneSqlTemplate} bean backed by the application's {@link DataSource}.
     *
     * @param dataSource
     *            the data source to use
     * @param properties
     *            the clione-sql configuration properties
     *
     * @return a new {@link ClioneSqlTemplate} instance
     */
    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean
    public ClioneSqlTemplate clioneSqlTemplate(DataSource dataSource, ClioneSqlProperties properties) {
        return new ClioneSqlTemplate(dataSource, properties);
    }

    /**
     * Creates a {@link PersistenceExceptionTranslator} that maps clione-sql exceptions to Spring's
     * {@code DataAccessException} hierarchy.
     *
     * <p>
     * This bean is not registered when the property {@code clione-sql.exception-translation-enabled} is set to
     * {@code false}.
     *
     * @return a new {@link ClioneSqlPersistenceExceptionTranslator} instance
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "clione-sql", name = "exception-translation-enabled", matchIfMissing = true)
    public PersistenceExceptionTranslator clioneSqlExceptionTranslator() {
        return new ClioneSqlPersistenceExceptionTranslator();
    }
}
