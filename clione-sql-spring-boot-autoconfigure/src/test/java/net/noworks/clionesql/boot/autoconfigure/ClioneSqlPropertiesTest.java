package net.noworks.clionesql.boot.autoconfigure;

import net.noworks.clionesql.boot.ClioneSqlProperties;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ClioneSqlPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(
                    AutoConfigurations.of(DataSourceAutoConfiguration.class, ClioneSqlAutoConfiguration.class))
            .withPropertyValues("spring.datasource.url=jdbc:h2:mem:testdb",
                    "spring.datasource.driver-class-name=org.h2.Driver");

    @Test
    void defaultValues() {
        this.contextRunner.run(context -> {
            ClioneSqlProperties properties = context.getBean(ClioneSqlProperties.class);
            assertThat(properties.getProductName()).isNull();
            assertThat(properties.getSqlFileEncoding()).isEqualTo("UTF-8");
            assertThat(properties.isExceptionTranslationEnabled()).isTrue();
            assertThat(properties.isDevelopmentMode()).isFalse();
            assertThat(properties.getSqlFileCacheTime()).isZero();
            assertThat(properties.getEntityDepthLimit()).isEqualTo(8);
        });
    }

    @Test
    void customValues() {
        this.contextRunner
                .withPropertyValues("clione-sql.product-name=postgres", "clione-sql.sql-file-encoding=Shift_JIS",
                        "clione-sql.exception-translation-enabled=false", "clione-sql.development-mode=true",
                        "clione-sql.sql-file-cache-time=5000", "clione-sql.entity-depth-limit=4")
                .run(context -> {
                    ClioneSqlProperties properties = context.getBean(ClioneSqlProperties.class);
                    assertThat(properties.getProductName()).isEqualTo("postgres");
                    assertThat(properties.getSqlFileEncoding()).isEqualTo("Shift_JIS");
                    assertThat(properties.isExceptionTranslationEnabled()).isFalse();
                    assertThat(properties.isDevelopmentMode()).isTrue();
                    assertThat(properties.getSqlFileCacheTime()).isEqualTo(5000);
                    assertThat(properties.getEntityDepthLimit()).isEqualTo(4);
                });
    }
}
