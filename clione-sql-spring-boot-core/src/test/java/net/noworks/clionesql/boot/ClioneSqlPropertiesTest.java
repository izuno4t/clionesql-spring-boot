package net.noworks.clionesql.boot;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClioneSqlPropertiesTest {

    @Test
    void defaultValues() {
        ClioneSqlProperties properties = new ClioneSqlProperties();
        assertThat(properties.getProductName()).isNull();
        assertThat(properties.getSqlFileEncoding()).isEqualTo("UTF-8");
        assertThat(properties.isExceptionTranslationEnabled()).isTrue();
        assertThat(properties.isDevelopmentMode()).isFalse();
        assertThat(properties.getSqlFileCacheTime()).isZero();
        assertThat(properties.getEntityDepthLimit()).isEqualTo(8);
    }

    @Test
    void setAndGetValues() {
        ClioneSqlProperties properties = new ClioneSqlProperties();

        properties.setProductName("postgres");
        assertThat(properties.getProductName()).isEqualTo("postgres");

        properties.setSqlFileEncoding("Shift_JIS");
        assertThat(properties.getSqlFileEncoding()).isEqualTo("Shift_JIS");

        properties.setExceptionTranslationEnabled(false);
        assertThat(properties.isExceptionTranslationEnabled()).isFalse();

        properties.setDevelopmentMode(true);
        assertThat(properties.isDevelopmentMode()).isTrue();

        properties.setSqlFileCacheTime(5000);
        assertThat(properties.getSqlFileCacheTime()).isEqualTo(5000);

        properties.setEntityDepthLimit(4);
        assertThat(properties.getEntityDepthLimit()).isEqualTo(4);
    }
}
