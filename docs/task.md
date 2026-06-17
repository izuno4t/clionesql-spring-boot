# TASKS

Milestone: M1
Goal: clione-sql Spring Boot 統合の P0/P1 機能とテストを完成させる

## Workflow Rules

- Update status to 🚧 when starting a task
- Update status to ✅ when completing a task
- Do not start a task unless all DependsOn tasks are ✅

## Status Notation

| Status | Meaning |
| ---- | ----- |
| ⏳ | TODO |
| 🚧 | IN_PROGRESS |
| 🧪 | REVIEW |
| ✅ | DONE |
| 🚫 | CANCELLED |

## Task List

| ID | Status | Summary | DependsOn |
|----|--------|---------|-----------|
| TASK-001 | ✅ | マルチモジュール Maven プロジェクト構成を作成する | - |
| TASK-002 | ✅ | ClioneSqlProperties（外部設定プロパティクラス）を実装する | TASK-001 |
| TASK-003 | ✅ | ClioneSqlTemplate（DataSource→Connection ブリッジ）を実装する | TASK-002 |
| TASK-004 | ✅ | ClioneSqlPersistenceExceptionTranslator（例外変換）を実装する | TASK-001 |
| TASK-005 | ✅ | ClioneSqlAutoConfiguration（自動構成）を実装する | TASK-003,TASK-004 |
| TASK-006 | ✅ | Auto-Configuration テストを作成する | TASK-005 |
| TASK-007 | ✅ | プロパティバインドテストを作成する | TASK-005 |
| TASK-008 | ✅ | 例外変換ユニットテストを作成する | TASK-004 |
| TASK-009 | ✅ | Bean オーバーライドテストを作成する | TASK-005 |
| TASK-010 | ✅ | トランザクション統合テスト（H2）を作成する | TASK-005 |
| TASK-011 | ✅ | .gitignore を Maven/IDE 対応に更新する | TASK-001 |
| TASK-012 | ✅ | サンプルアプリケーションモジュールを作成する | TASK-005 |

## Task Details

### TASK-001

- Note: 親 pom.xml と 4 モジュール（starter / autoconfigure / core / samples）の pom.xml を作成する
- Note: Spring Boot 3.x + Java 17、clione-sql は provided scope で宣言
- Caution: clione-sql JAR は Maven Central 未登録のため、ローカル install 前提で pom.xml を構成する

### TASK-002

- Note: `@ConfigurationProperties(prefix = "clione-sql")` で定義
- Note: 対象プロパティ: productName, sqlFileEncoding, exceptionTranslationEnabled, developmentMode, sqlFileCacheTime, entityDepthLimit
- Note: core モジュールに配置する

### TASK-003

- Note: core モジュールに配置する
- Note: `TransactionAwareDataSourceProxy` で DataSource をラップし、`DataSourceUtils.getConnection()` で Connection を取得
- Note: `useFile(String)`, `useFile(Class, String)`, `useSQL(String)` の 3 メソッドを提供
- Caution: `SQLManager.closeConnection()` は呼ばない設計（Connection ライフサイクルは Spring に委譲）
- Caution: `applyProperties()` で Config への設定反映を行う（初期は clione.properties 運用ガイド方針）

### TASK-004

- Note: core モジュールに配置する
- Note: `PersistenceExceptionTranslator` を実装し、clione-sql の 6 種例外を Spring DataAccessException へマッピング
- Note: `SQLRuntimeException` → `SQLStateSQLExceptionTranslator` 経由で変換

### TASK-005

- Note: autoconfigure モジュールに配置する
- Note: `@ConditionalOnClass(SQLManager.class)` で clione-sql の存在を条件にする
- Note: `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` に登録する
- Note: `@ConditionalOnMissingBean` で Bean オーバーライドを許可する

### TASK-006

- Note: `ApplicationContextRunner` を使用して Bean 生成・条件付き構成を検証する
- Note: DataSource あり/なし、clione-sql クラスあり/なしの各パターンを網羅する

### TASK-010

- Note: H2 インメモリ DB + `@SpringBootTest` + `@Transactional` ロールバック検証
- Caution: clione-sql JAR がテスト時に必要（ローカル install 前提）

### TASK-012

- Note: samples モジュールに Spring Boot アプリケーションと SQL ファイルを配置する
- Note: 要件定義書 §6 の利用イメージ（PersonService）を実装する

## Backlog List

| ID | Status | Summary | DependsOn |
|----|--------|---------|-----------|
| BACKLOG-001 | ⏳ | SQLファイルキャッシュ制御（開発モード対応）を実装する | - |
| BACKLOG-002 | ⏳ | マルチ DataSource 対応を実装する | - |
| BACKLOG-003 | ⏳ | Actuator 統合（実行SQL監視）を実装する | - |
| BACKLOG-004 | ⏳ | Config ブリッジ Phase 2（Spring properties 自動同期）を設計・実装する | BACKLOG-006 |
| BACKLOG-005 | ⏳ | CI/CD 構築（GitHub Actions）を作成する | - |
| BACKLOG-006 | ⏳ | clione-sql 本体の Config クラスを外部注入可能に改修する | - |
| BACKLOG-007 | ⏳ | clione-sql 本体の ReflectionUtil を JPMS / 将来の JDK 制限に対応させる | - |

## Backlog Details

### BACKLOG-001

- Note: 要件定義書 §8 の P2 項目
- Note: Config.IS_DEVELOPMENT_MODE / SQLFILE_CACHETIME の制御

### BACKLOG-002

- Note: 要件定義書 §8 の P2 項目
- Note: 複数 DataSource に対応する ClioneSqlTemplate の設計

### BACKLOG-004

- Note: clione-sql の Config クラスへ Spring properties を自動反映する仕組み
- Note: BACKLOG-006 の本体改修が前提

### BACKLOG-006

- Note: Config のフィールドが全て `public final` のため、外部から設定値を注入できない
- Note: clione-sql 本体をフォークし、Config に setter / builder / 外部 Properties 注入ポイントを追加する必要がある
- Caution: 本体改修なしでは Spring properties 自動同期（BACKLOG-004）は実現不可

### BACKLOG-007

- Note: `ReflectionUtil` が `Field.setAccessible(true)` / `Constructor.setAccessible(true)` を無条件に使用している
- Note: JDK 21 時点では unnamed module 内のクラスに対しては警告なく動作するが、将来のデフォルト拒否化リスクがある
- Note: ユーザが JPMS（`module-info.java`）を導入した場合、`--add-opens` 指定が必須になる
- Note: `setAccessible` 失敗時のフォールバック（VarHandle, MethodHandle 等）が未実装
- Caution: オブジェクトマッピング機能の根幹に関わるため、代替手段の選定には慎重な設計が必要
