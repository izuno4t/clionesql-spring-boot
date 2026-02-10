package net.noworks.clionesql.boot.autoconfigure;

import javax.sql.DataSource;

import net.noworks.clionesql.boot.ClioneSqlTemplate;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.dao.support.PersistenceExceptionTranslator;

import tetz42.clione.SQLManager;

import static org.assertj.core.api.Assertions.assertThat;

class ClioneSqlAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(
                    AutoConfigurations.of(DataSourceAutoConfiguration.class, ClioneSqlAutoConfiguration.class))
            .withPropertyValues("spring.datasource.url=jdbc:h2:mem:testdb",
                    "spring.datasource.driver-class-name=org.h2.Driver");

    @Test
    void beansAreCreatedWithDataSource() {
        this.contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ClioneSqlTemplate.class);
            assertThat(context).hasSingleBean(PersistenceExceptionTranslator.class);
        });
    }

    @Test
    void beansAreNotCreatedWithoutSQLManagerClass() {
        this.contextRunner.withClassLoader(new FilteredClassLoader(SQLManager.class)).run(context -> {
            assertThat(context).doesNotHaveBean(ClioneSqlTemplate.class);
            assertThat(context).doesNotHaveBean(PersistenceExceptionTranslator.class);
        });
    }

    @Test
    void beansAreNotCreatedWithoutDataSource() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ClioneSqlAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ClioneSqlTemplate.class);
                });
    }
}
