# clione-sql Spring Boot Integration 要件定義書

## 1. 背景・目的

### 1.1 概要

clione-sql は 2Way SQL テンプレートエンジンであり、SQLファイル内のコメント構文（`/* param */`）を使ってパラメータバインドを行う軽量ライブラリである。本ドキュメントでは、clione-sql を Spring Boot と統合するための要件を定義する。

### 1.2 参考実装

doma-spring-boot（https://github.com/domaframework/doma-spring-boot）の設計パターンを参考にする。doma-spring-boot は以下の統合機構を提供している：

- Auto-Configuration（`@ConditionalOnClass` による自動構成）
- 外部設定（`@ConfigurationProperties`）
- DataSource 統合（`TransactionAwareDataSourceProxy`）
- トランザクション管理（Spring `@Transactional` との連携）
- 例外変換（Doma例外 → Spring `DataAccessException`）
- SQLファイルリソースローディング（Spring `ResourceLoader`）
- Bean オーバーライド（`@ConditionalOnMissingBean`）

---

## 2. clione-sql ソースコード調査結果

### 2.1 基本情報

| 項目 | 内容 |
|------|------|
| パッケージ | `tetz42.clione` |
| バージョン | 0.5.1 |
| ビルドシステム | Apache Ant（Maven/Gradle なし） |
| Java バージョン | Java 6+ 相当（ジェネリクス使用、ラムダなし） |
| ライセンス | Apache License 2.0 |
| 外部依存 | なし（単一JAR） |

### 2.2 エントリポイント：`SQLManager`

`SQLManager` がライブラリの主要ファサードクラスである。

**生成方法（staticファクトリメソッド）：**

```java
// Connection なし（ThreadLocal から取得）
SQLManager.sqlManager()

// Connection 指定
SQLManager.sqlManager(Connection con)

// Product（DB種別）指定
SQLManager.sqlManager(Product product)
SQLManager.sqlManager(Connection con, Product product)

// productName（文字列）指定
SQLManager.sqlManager(String productName)
SQLManager.sqlManager(Connection con, String productName)
```

**Product 列挙型：**
`ORACLE`, `SQLSERVER`, `DB2`, `MYSQL`, `FIREBIRD`, `POSTGRES`, `SQLITE`

**SQL 実行フロー：**

```
SQLManager → useFile(sqlPath) / useSQL(sql) → SQLExecutor → find() / findAll() / update() / each()
```

### 2.3 コネクション管理

**重要な発見：clione-sql は `DataSource` を一切扱わない。`java.sql.Connection` を直接受け取る設計。**

コネクション取得の優先順位：
1. コンストラクタに渡された `Connection`
2. `ThreadLocal<Connection>` に設定された `Connection`（`SQLManager.setThreadConnection(con)`）

```java
// ThreadLocal ベースのコネクション管理
private static ThreadLocal<Connection> tcon = new ThreadLocal<Connection>();

public static void setThreadConnection(Connection con) {
    tcon.set(con);
}

private Connection getCon(Connection con) {
    return con != null ? con : getThreadConnection();
}
```

→ **Spring Boot 統合のキーポイント**: `DataSource` → `Connection` のブリッジが必要。`TransactionAwareDataSourceProxy` + `DataSourceUtils.getConnection()` で Spring トランザクションに参加させる。

### 2.4 例外階層

全例外が `RuntimeException` を直接継承（チェック例外なし）。

```
RuntimeException
├── SQLRuntimeException              ← SQLException をラップ（getCause() で取得可）
│   └── パッケージ: tetz42.clione.common.exception
├── ConnectionNotFoundException      ← Connection が null の場合
├── ClioneFormatException            ← SQL テンプレート構文エラー
├── DuplicateKeyException            ← 重複キー
├── ParameterNotFoundException       ← パラメータ未指定
├── SQLFileNotFoundException         ← SQL ファイル未発見
├── SecurityValidationException      ← セキュリティ検証失敗
└── ImpossibleToCompareException     ← 比較不能
    └── パッケージ: tetz42.clione.exception
```

**例外変換における重要ポイント：**
- `SQLRuntimeException` が `getSQLException()` メソッドを持ち、元の `SQLException` を取得可能
- `SQLException` の `SQLState` を使って Spring の `DataAccessException` サブクラスへマッピング可能

### 2.5 SQL ファイルローディング

`LoaderUtil` がSQLファイルの読み込みとキャッシュを管理。

**読み込み機構：**
```java
Thread.currentThread().getContextClassLoader().getResourceAsStream(sqlPath)
```

**キャッシュ：**
- `ConcurrentHashMap<String, NodeHolder>` による静的キャッシュ（パス別・SQL文別）
- `IS_DEVELOPMENT_MODE = true` かつ `SQLFILE_CACHETIME` 経過でキャッシュ無効化
- 本番モードではキャッシュは永続

**SQLパス解決ルール：**
```
[パッケージ名]/sql/[クラス名]/[SQLファイル名]
例: tetz42/dao/sql/PersonDao/Select.sql
```

**Dialect対応：** `sqlPath + "-" + productName` で DB 固有 SQL を優先探索し、見つからなければ汎用SQLにフォールバック。

### 2.6 設定（`Config` クラス）

`clione.properties` をクラスパスから読み込むシングルトン。

| プロパティ | デフォルト | 説明 |
|-----------|----------|------|
| `DBMS_PRODUCT_NAME` | null | DB製品名（oracle, mysql 等） |
| `SQLFILE_ENCODING` | utf-8 | SQLファイルのエンコーディング |
| `IS_DEVELOPMENT_MODE` | false | 開発モード（キャッシュ無効化） |
| `SQLFILE_CACHETIME` | 0 (ms) | キャッシュ有効期間（開発モード時のみ） |
| `TAB_SIZE` | 4 | タブサイズ |
| `ENTITY_DEPTH_LIMIT` | 8 | エンティティのネスト深度上限 |
| `CONVERTERS.N` | (なし) | カスタムコンバータ定義 |

### 2.7 スレッドセーフティ

| クラス | スレッドセーフ | 備考 |
|--------|:----------:|------|
| `Config` | ✅ | volatile + synchronized でシングルトン保証 |
| `LoaderUtil`（SQLキャッシュ） | ✅ | `ConcurrentHashMap` 使用 |
| `SQLManager` | ❌ | 実行済みSQL等の状態を保持。操作ごとに生成が前提 |
| `SQLExecutor` | ❌ | `PreparedStatement`/`ResultSet` を保持 |

→ **Spring Boot 統合のキーポイント**: `SQLManager` をシングルトン Bean にはできない。リクエストスコープまたは毎回新規生成が必要。ただし Spring 統合層で薄いラッパーを提供し、内部で都度 `SQLManager` を生成する設計がベスト。

---

## 3. モジュール構成

```
clione-sql-spring-boot/
├── clione-sql-spring-boot-starter/         # 依存集約 (starter)
│   └── pom.xml
├── clione-sql-spring-boot-autoconfigure/   # 自動構成クラス
│   ├── src/main/java/
│   └── src/main/resources/
│       └── META-INF/spring/
│           └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
├── clione-sql-spring-boot-core/            # コア統合コンポーネント
│   └── src/main/java/
└── clione-sql-spring-boot-samples/         # サンプルアプリケーション
    └── src/
```

---

## 4. 実装要件

### 4.1 Auto-Configuration

```java
@Configuration
@ConditionalOnClass(SQLManager.class)
@EnableConfigurationProperties(ClioneSqlProperties.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class ClioneSqlAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ClioneSqlTemplate clioneSqlTemplate(
            DataSource dataSource,
            ClioneSqlProperties properties) {
        return new ClioneSqlTemplate(dataSource, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        prefix = "clione-sql",
        name = "exception-translation-enabled",
        matchIfMissing = true)
    public PersistenceExceptionTranslator clioneSqlExceptionTranslator() {
        return new ClioneSqlPersistenceExceptionTranslator();
    }
}
```

**登録ファイル：**
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

### 4.2 外部設定プロパティ

```java
@ConfigurationProperties(prefix = "clione-sql")
public class ClioneSqlProperties {

    /** DB製品名 (oracle, mysql, postgres, db2, sqlserver, sqlite, firebird) */
    private String productName;

    /** SQLファイルのエンコーディング */
    private String sqlFileEncoding = "UTF-8";

    /** 例外変換の有効/無効 */
    private boolean exceptionTranslationEnabled = true;

    /** 開発モード（SQLファイルキャッシュ無効化） */
    private boolean developmentMode = false;

    /** SQLファイルキャッシュ有効期間（ms、開発モード時のみ有効） */
    private int sqlFileCacheTime = 0;

    /** エンティティネスト深度上限 */
    private int entityDepthLimit = 8;

    // getters / setters
}
```

**application.yml での設定例：**

```yaml
clione-sql:
  product-name: postgres
  sql-file-encoding: UTF-8
  development-mode: false
  exception-translation-enabled: true
```

### 4.3 コア統合コンポーネント

#### 4.3.1 `ClioneSqlTemplate` — Spring 統合のファサード

clione-sql の `SQLManager` はスレッドセーフでないため、Spring Bean として公開する薄いラッパーを提供する。内部で都度 `SQLManager` を生成し、Spring 管理の `Connection` を渡す。

```java
public class ClioneSqlTemplate {

    private final DataSource dataSource;
    private final ClioneSqlProperties properties;

    public ClioneSqlTemplate(DataSource dataSource, ClioneSqlProperties properties) {
        // DataSource を TransactionAwareDataSourceProxy でラップ
        this.dataSource = new TransactionAwareDataSourceProxy(dataSource);
        this.properties = properties;
        // Config に properties を反映
        applyProperties(properties);
    }

    /**
     * SQLファイルを指定して SQLExecutor を取得。
     * Connection は Spring の DataSourceUtils 経由で取得し、
     * @Transactional に自動参加する。
     */
    public SQLExecutor useFile(String sqlPath) {
        Connection con = DataSourceUtils.getConnection(this.dataSource);
        SQLManager manager = SQLManager.sqlManager(con, properties.getProductName());
        return manager.useFile(sqlPath);
    }

    public SQLExecutor useFile(Class<?> clazz, String sqlFile) {
        Connection con = DataSourceUtils.getConnection(this.dataSource);
        SQLManager manager = SQLManager.sqlManager(con, properties.getProductName());
        return manager.useFile(clazz, sqlFile);
    }

    public SQLExecutor useSQL(String sql) {
        Connection con = DataSourceUtils.getConnection(this.dataSource);
        SQLManager manager = SQLManager.sqlManager(con, properties.getProductName());
        return manager.useSQL(sql);
    }

    private void applyProperties(ClioneSqlProperties props) {
        // clione.properties の代わりに Spring properties を Config に反映
        // → Config.clear() + System properties or 直接リフレクション等
        // 注: Config は clione.properties 固定読み込みのため、
        //     Spring properties との橋渡しが必要（§4.3.4 参照）
    }
}
```

#### 4.3.2 例外変換

```java
public class ClioneSqlPersistenceExceptionTranslator
        implements PersistenceExceptionTranslator {

    @Override
    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
        // 1. SQLRuntimeException → SQLException を取得して SQLState ベースで変換
        if (ex instanceof SQLRuntimeException) {
            SQLException sqlEx = ((SQLRuntimeException) ex).getSQLException();
            return new SQLStateSQLExceptionTranslator()
                .translate("clione-sql", null, sqlEx);
        }

        // 2. ConnectionNotFoundException → DataAccessResourceFailureException
        if (ex instanceof ConnectionNotFoundException) {
            return new DataAccessResourceFailureException(ex.getMessage(), ex);
        }

        // 3. SQLFileNotFoundException → NonTransientDataAccessResourceException
        if (ex instanceof SQLFileNotFoundException) {
            return new NonTransientDataAccessResourceException(ex.getMessage(), ex);
        }

        // 4. ParameterNotFoundException → InvalidDataAccessApiUsageException
        if (ex instanceof ParameterNotFoundException) {
            return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
        }

        // 5. ClioneFormatException → InvalidDataAccessApiUsageException
        if (ex instanceof ClioneFormatException) {
            return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
        }

        // 6. DuplicateKeyException → DuplicateKeyException (Spring)
        if (ex instanceof tetz42.clione.exception.DuplicateKeyException) {
            return new org.springframework.dao.DuplicateKeyException(
                ex.getMessage(), ex);
        }

        return null; // 変換不可
    }
}
```

#### 4.3.3 SQL ファイルリソースローダー（拡張対応）

clione-sql の `LoaderUtil` は `Thread.currentThread().getContextClassLoader().getResourceAsStream()` 固定であるため、現状の実装では Spring の `ResourceLoader` に差し替えることができない。

**対応方針：**

| 方針 | 内容 | メリット | デメリット |
|------|------|---------|----------|
| **A. 現状維持** | クラスパスに SQL ファイルを配置 | 改修不要 | Spring Resource 抽象の恩恵なし |
| **B. カスタム ClassLoader** | Spring ResourceLoader をラップした ClassLoader を設定 | clione-sql 本体無改修 | 実装が複雑 |
| **C. clione-sql をフォーク** | `LoaderUtil` に拡張ポイントを追加 | 完全な制御 | フォーク保守コスト |

**推奨: 方針 A**（初期リリース）。大半の Spring Boot アプリではクラスパスリソースで十分であり、`src/main/resources/` 以下に SQL ファイルを配置する運用で対応可能。

#### 4.3.4 Config ブリッジ

clione-sql の `Config` クラスは `clione.properties` をクラスパスから固定読み込みする設計。Spring Boot の `application.yml` / `application.properties` から設定を注入するには以下のアプローチが必要：

**対応方針：**

```java
/**
 * Spring Boot 起動時に clione.properties 相当の設定を
 * ClioneSqlProperties から Config に橋渡しする。
 *
 * Config は内部で Properties を読み込むため、
 * System Properties 経由 or クラスパス上に clione.properties を動的生成 で対応。
 */
@Component
public class ClioneSqlConfigBridge implements InitializingBean {

    private final ClioneSqlProperties properties;

    @Override
    public void afterPropertiesSet() {
        // 方法1: Config.clear() + clione.properties をクラスパスに配置する運用ガイドを提供
        // 方法2: clione-sql フォークで Config を外部注入可能にする
        Config.clear(); // キャッシュクリア
    }
}
```

**現実的な初期対応：** `clione.properties` ファイルを `src/main/resources/` に配置する運用ガイドを提供し、Spring properties との自動同期は Phase 2 以降で対応。

---

## 5. コンポーネントマッピング

| doma-spring-boot | clione-sql-spring-boot | 備考 |
|---|---|---|
| `DomaAutoConfiguration` | `ClioneSqlAutoConfiguration` | Auto-Configuration エントリポイント |
| `DomaProperties` | `ClioneSqlProperties` | 外部設定 |
| `DomaConfig` / `DomaConfigBuilder` | `ClioneSqlTemplate` | ファサード（都度 `SQLManager` 生成） |
| `DomaPersistenceExceptionTranslator` | `ClioneSqlPersistenceExceptionTranslator` | 例外変換 |
| `SpringResourceLoader` | （現状不要） | クラスパスリソースで対応 |
| `TransactionAwareDataSourceProxy` | `ClioneSqlTemplate` 内部で使用 | `@Transactional` 連携 |

---

## 6. 利用イメージ

### 6.1 基本的な使い方

```java
@Service
public class PersonService {

    private final ClioneSqlTemplate clioneSql;

    public PersonService(ClioneSqlTemplate clioneSql) {
        this.clioneSql = clioneSql;
    }

    @Transactional(readOnly = true)
    public List<ResultMap> findByName(String name) {
        return clioneSql.useFile("sql/person/SelectByName.sql")
            .findAll(SQLManager.params("name", name));
    }

    @Transactional
    public int updateStatus(int id, String status) {
        return clioneSql.useFile("sql/person/UpdateStatus.sql")
            .update(SQLManager.params("id", id).$("status", status));
    }
}
```

### 6.2 SQL ファイル例（`sql/person/SelectByName.sql`）

```sql
SELECT
  id, name, status, created_at
FROM
  person
WHERE
  name = /* name */'dummy'
  /* IF status */
  AND status = /* status */'ACTIVE'
  /* END */
ORDER BY
  id
```

---

## 7. テスト戦略

| テスト種別 | 内容 | ツール |
|-----------|------|-------|
| Auto-Configuration テスト | Bean 生成・条件付き構成の検証 | `AnnotationConfigApplicationContext` |
| プロパティバインド テスト | `application.yml` → `ClioneSqlProperties` | `@SpringBootTest` |
| トランザクション統合テスト | `@Transactional` ロールバック検証 | `@SpringBootTest` + H2 |
| 例外変換テスト | clione-sql 例外 → Spring `DataAccessException` | JUnit 5 |
| Bean オーバーライド テスト | `@ConditionalOnMissingBean` の動作確認 | `AnnotationConfigApplicationContext` |
| SQL 実行 E2E テスト | 実際の CRUD 操作 | TestContainers + PostgreSQL |

---

## 8. 実装優先度

| 優先度 | 項目 | 理由 |
|:------:|------|------|
| **P0** | Auto-Configuration + Properties | 基盤。これがないと始まらない |
| **P0** | `ClioneSqlTemplate`（DataSource→Connection ブリッジ） | `@Transactional` 連携の核心 |
| **P0** | `TransactionAwareDataSourceProxy` 統合 | Spring トランザクション参加に必須 |
| **P1** | 例外変換 (`PersistenceExceptionTranslator`) | Spring 統一例外階層への準拠 |
| **P1** | Config ブリッジ（`clione.properties` 運用ガイド） | 設定の一貫性 |
| **P2** | SQL ファイルキャッシュ制御（開発モード対応） | 開発体験向上 |
| **P2** | マルチ DataSource 対応 | 複数DB接続要件 |
| **P2** | Actuator 統合（実行SQL監視） | 運用監視 |

---

## 9. 技術的課題と制約

### 9.1 clione-sql の制約

| 課題 | 影響 | 対応方針 |
|------|------|---------|
| **DataSource 非対応** | Connection を直接渡す必要あり | `DataSourceUtils.getConnection()` でブリッジ |
| **Config が固定読み込み** | Spring properties からの動的注入不可 | Phase 1: `clione.properties` 運用ガイド / Phase 2: フォーク検討 |
| **SQLManager がスレッドセーフでない** | シングルトン Bean 化不可 | `ClioneSqlTemplate` で都度生成 |
| **Ant ビルド / Maven Central 未登録** | 依存解決が標準的でない | starter では `provided` 宣言、利用者が各自で JAR を調達。README に記載 |
| **Java 6 相当のコード** | Spring Boot 3.x (Java 17+) との互換性 | コンパイルは通るはず。要動作検証 |
| **LoaderUtil が ClassLoader 固定** | Spring Resource 抽象が使えない | Phase 1: クラスパスで十分。Phase 2 で拡張検討 |

### 9.2 Connection ライフサイクル管理の注意点

clione-sql の `SQLManager.closeConnection()` は Connection を直接クローズするが、Spring 管理の Connection を勝手にクローズすると問題が生じる。

**対策：** `ClioneSqlTemplate` 利用時は `closeConnection()` を呼ばず、`closeStatement()` のみ使用するよう設計する。Connection のライフサイクルは Spring（`DataSourceUtils`）に委譲。

---

## 10. 前提条件・次のアクション

### 前提条件

- clione-sql JAR（0.5.1）が利用プロジェクト側で参照可能であること
  - JAR の調達方法（ローカル install、lib 配置、プライベートリポジトリ等）は利用者に委ねる
  - starter 側の pom.xml では `provided` scope で宣言
- Spring Boot 3.x + Java 17+

### 次のアクション

| # | アクション | 備考 |
|---|----------|------|
| 1 | プロジェクト雛形 + README 作成 | clione-sql JAR の調達方法を記載 |
| 2 | `clione-sql-spring-boot-autoconfigure` プロトタイプ実装 | P0 項目 |
| 3 | H2 + `@Transactional` での統合テスト | トランザクション動作検証 |
| 4 | サンプルアプリケーション作成 | 利用イメージの実証 |
| 5 | CI/CD 構築（GitHub Actions） | Java 17/21 × Spring Boot 3.x マトリクス |
| 6 | Config ブリッジの Phase 2 設計検討 | Spring properties 自動同期 |

---

## 付録 A: clione-sql パッケージ構造

```
tetz42.clione
├── SQLManager.java              ← エントリポイント（ファサード）
├── SQLExecutor.java             ← SQL実行エンジン
├── SQLIterator.java             ← ResultSet イテレータ
├── common/
│   ├── Util.java, Using.java    ← ユーティリティ
│   └── exception/
│       └── SQLRuntimeException.java  ← SQLException ラッパー
├── exception/
│   ├── ConnectionNotFoundException.java
│   ├── ClioneFormatException.java
│   ├── DuplicateKeyException.java
│   ├── ParameterNotFoundException.java
│   ├── SQLFileNotFoundException.java
│   └── SecurityValidationException.java
├── gen/
│   └── SQLGenerator.java        ← SQL生成エンジン
├── loader/
│   └── LoaderUtil.java          ← SQLファイル読み込み・キャッシュ
├── node/                        ← SQL AST ノード
├── parsar/                      ← SQLパーサー
├── lang/
│   ├── dialect/                 ← DB方言（Oracle, MySQL, Postgres 等）
│   └── func/                    ← SQL関数・パラメータ処理
└── util/
    ├── Config.java              ← 設定（clione.properties）
    ├── ParamMap.java            ← パラメータマップ
    ├── ResultMap.java           ← 結果マップ
    └── converter/               ← 型コンバータ群
```
