package net.noworks.clionesql.boot.autoconfigure;

import javax.sql.DataSource;

import net.noworks.clionesql.boot.ClioneSqlProperties;
import net.noworks.clionesql.boot.ClioneSqlTemplate;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.support.PersistenceExceptionTranslator;

import static org.assertj.core.api.Assertions.assertThat;

class BeanOverrideTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(
                    AutoConfigurations.of(DataSourceAutoConfiguration.class, ClioneSqlAutoConfiguration.class))
            .withPropertyValues("spring.datasource.url=jdbc:h2:mem:testdb",
                    "spring.datasource.driver-class-name=org.h2.Driver");

    @Test
    void customClioneSqlTemplateOverridesAutoConfigured() {
        this.contextRunner.withUserConfiguration(CustomTemplateConfig.class).run(context -> {
            assertThat(context).hasSingleBean(ClioneSqlTemplate.class);
            assertThat(context.getBean(ClioneSqlTemplate.class)).isSameAs(context.getBean("customTemplate"));
        });
    }

    @Test
    void customExceptionTranslatorOverridesAutoConfigured() {
        this.contextRunner.withUserConfiguration(CustomExceptionTranslatorConfig.class).run(context -> {
            assertThat(context).hasSingleBean(PersistenceExceptionTranslator.class);
            assertThat(context.getBean(PersistenceExceptionTranslator.class))
                    .isSameAs(context.getBean("customTranslator"));
        });
    }

    @Test
    void exceptionTranslatorDisabledByProperty() {
        this.contextRunner.withPropertyValues("clione-sql.exception-translation-enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(PersistenceExceptionTranslator.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomTemplateConfig {
        @Bean
        ClioneSqlTemplate customTemplate(DataSource dataSource) {
            return new ClioneSqlTemplate(dataSource, new ClioneSqlProperties());
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomExceptionTranslatorConfig {
        @Bean
        PersistenceExceptionTranslator customTranslator() {
            return ex -> null;
        }
    }
}
