package net.noworks.clionesql.boot.autoconfigure;

import net.noworks.clionesql.boot.ClioneSqlTemplate;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Connection リーク（issue #1）の再現および修正検証テスト。
 *
 * <p>
 * HikariCP を maximumPoolSize=1 で明示構成する。プールサイズを超える回数を非トランザクションで呼び出したとき、接続が返却されないと枯渇する。
 *
 * <ul>
 * <li>{@link #nonTransactionalRepeatedQueriesDoNotExhaustPool()} — callback
 * API（{@code query}）は接続を返却するため枯渇しない（修正検証）。</li>
 * <li>{@link #transactionalRepeatedQueriesDoNotExhaustPool()} — トランザクション境界内では終了時に返却されるため枯渇しない（対照）。</li>
 * <li>{@link #legacyDirectApiLeaksConnectionNonTransactionally()} — 旧来の {@code useSQL/useFile}
 * 直叩きは非トランザクションで接続を返却せず枯渇する（リーク再現）。</li>
 * </ul>
 */
@SpringBootTest(properties = { "spring.datasource.url=jdbc:h2:mem:leakrepro;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.type=com.zaxxer.hikari.HikariDataSource", "spring.datasource.hikari.maximum-pool-size=1",
        "spring.datasource.hikari.connection-timeout=1000", "spring.datasource.hikari.pool-name=LeakReproPool" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ConnectionLeakReproductionTest {

    private static final int CALLS = 5;

    private static final String SQL_PATH = "sql/SelectOne.sql";

    @Autowired
    private ClioneSqlTemplate clioneSqlTemplate;

    @Autowired
    private TxQueryRunner txQueryRunner;

    /**
     * callback API（{@link ClioneSqlTemplate#query(String)}）は実行ごとに接続を返却するため、非トランザクションでプールサイズを超えて呼び出しても枯渇しない。
     */
    @Test
    void nonTransactionalRepeatedQueriesDoNotExhaustPool() {
        assertThatCode(() -> {
            for (int i = 0; i < CALLS; i++) {
                clioneSqlTemplate.query(SQL_PATH);
            }
        }).doesNotThrowAnyException();
    }

    /**
     * 対照: トランザクション境界内なら、接続はトランザクション終了時に返却される。サイズ1のプールでも繰り返し成功する。
     */
    @Test
    void transactionalRepeatedQueriesDoNotExhaustPool() {
        assertThatCode(() -> {
            for (int i = 0; i < CALLS; i++) {
                txQueryRunner.runQuery(clioneSqlTemplate);
            }
        }).doesNotThrowAnyException();
    }

    /**
     * リーク再現: 旧来の {@code useSQL().find()} 直叩きは接続を返却しないため、非トランザクションで2回目以降にプール（サイズ1）が枯渇し、 {@code connection-timeout}
     * 経過後に例外が送出される。
     */
    @Test
    void legacyDirectApiLeaksConnectionNonTransactionally() {
        assertThatThrownBy(() -> {
            for (int i = 0; i < CALLS; i++) {
                clioneSqlTemplate.useSQL("SELECT 1 AS v").find();
            }
        }).isInstanceOf(Throwable.class);
    }

    @SpringBootApplication
    static class TestApplication {

        @Bean
        TxQueryRunner txQueryRunner() {
            return new TxQueryRunner();
        }
    }

    /** {@link Transactional} 境界を提供するヘルパー。プロキシ経由でトランザクションを開始するため public メソッドにする。 */
    static class TxQueryRunner {

        @Transactional
        public void runQuery(ClioneSqlTemplate template) {
            template.query(SQL_PATH);
        }
    }
}
