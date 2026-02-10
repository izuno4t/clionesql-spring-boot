package net.noworks.clionesql.boot.autoconfigure;

import net.noworks.clionesql.boot.ClioneSqlTemplate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Rollback
class TransactionIntegrationTest {

    @Autowired
    private ClioneSqlTemplate clioneSqlTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS test_person (" + "id INT PRIMARY KEY, name VARCHAR(100))");
        jdbcTemplate.execute("DELETE FROM test_person");
    }

    @Test
    void clioneSqlTemplateBeanIsCreated() {
        assertThat(clioneSqlTemplate).isNotNull();
    }

    @Test
    void insertWithUseSQLAndRollback() {
        clioneSqlTemplate.useSQL("INSERT INTO test_person (id, name) VALUES (/* id */1, /* name */'test')")
                .update(tetz42.clione.SQLManager.params("id", 1).$("name", "Alice"));

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_person WHERE id = 1", Integer.class);
        assertThat(count).isEqualTo(1);
        // @Rollback により、テスト後にロールバックされる
    }

    @SpringBootApplication
    static class TestApplication {
    }
}
