# clione-sql Spring Boot Starter

[![Java](https://img.shields.io/badge/Java-17%2B-blue)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Spring Boot integration for
[clione-sql](https://github.com/tauty/clione-sql),
a 2Way SQL template engine for Java.

## Features

- Auto-configuration of `ClioneSqlTemplate` with
  Spring-managed `DataSource`
- Full `@Transactional` support via
  `TransactionAwareDataSourceProxy`
- Exception translation from clione-sql exceptions to
  Spring's `DataAccessException` hierarchy
- External configuration via
  `application.properties`

## Requirements

- Java 17+
- Spring Boot 3.3+
- clione-sql 0.5.1

## Getting Started

### 1. Install clione-sql to Local Maven Repository

clione-sql is not available on Maven Central.
Download the JAR and install it manually:

```bash
mvn install:install-file \
  -Dfile=clione-sql-0.5.1.jar \
  -DgroupId=tetz42 \
  -DartifactId=clione-sql \
  -Dversion=0.5.1 \
  -Dpackaging=jar
```

### 2. Add Dependencies

```xml
<dependency>
    <groupId>net.noworks</groupId>
    <artifactId>clione-sql-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>tetz42</groupId>
    <artifactId>clione-sql</artifactId>
    <version>0.5.1</version>
</dependency>
```

### 3. Configure clione-sql (`clione.properties`)

clione-sql reads its configuration from
`clione.properties` on the classpath.
Place this file in `src/main/resources/`:

```properties
# Database product name (auto-detected if omitted)
#DBMS_PRODUCT_NAME=postgres

# SQL file encoding (default: utf-8)
SQLFILE_ENCODING=utf-8

# Development mode: disable SQL file cache
IS_DEVELOPMENT_MODE=false

# SQL file cache duration in ms (dev mode only)
SQLFILE_CACHETIME=0

# Indentation tab size for 2Way SQL parsing
TAB_SIZE=4

# Max nesting depth for entity mapping
ENTITY_DEPTH_LIMIT=8

# Custom type converters (comma-separated FQCNs)
#CONVERTERS=com.example.MyConverter
```

> **Note:** Due to clione-sql's `Config` class using
> `public final` fields, these values cannot be
> overridden via Spring Boot's `application.properties`.
> See [Constraints](#config-class-has-immutable-public-final-fields).

### 4. Configure Spring Integration (Optional)

```properties
clione-sql.exception-translation-enabled=true
```

This controls Spring-side behavior only
(exception translation).

## Usage

`ClioneSqlTemplate` is auto-configured and can be
injected directly.

### Using External SQL Files

Place SQL files on the classpath
(e.g. `src/main/resources/sql/`):

```sql
-- sql/person/SelectByName.sql
SELECT
  id, name, status, created_at
FROM
  person
WHERE
  name = /* $name */'dummy'
ORDER BY
  id
```

```java
@Service
public class PersonService {

    private final ClioneSqlTemplate clioneSql;

    public PersonService(ClioneSqlTemplate clioneSql) {
        this.clioneSql = clioneSql;
    }

    @Transactional(readOnly = true)
    public List<ResultMap> findByName(String name) {
        return clioneSql
                .useFile("sql/person/SelectByName.sql")
                .findAll(SQLManager.params("name", name));
    }
}
```

### Using Inline SQL (2Way SQL)

```java
@Transactional(readOnly = true)
public ResultMap findById(int id) {
    return clioneSql.useSQL(
            "SELECT id, name, status FROM person"
            + " WHERE id = /* id */'1'")
            .find(SQLManager.params("id", id));
}

@Transactional
public int insert(int id, String name, String status) {
    return clioneSql.useSQL(
            "INSERT INTO person (id, name, status)"
            + " VALUES (/* id */'0',"
            + " /* name */'dummy',"
            + " /* status */'ACTIVE')")
            .update(SQLManager.params("id", id)
                    .$("name", name).$("status", status));
}
```

### Using Class-relative SQL Files

```java
// Loads SQL from the same package as PersonService
return clioneSql
        .useFile(PersonService.class, "Select.sql")
        .findAll(params);
```

## Spring Configuration Properties

Properties under the `clione-sql` prefix in
`application.properties`:

| Property | Default | Description |
| -------- | ------- | ----------- |
| `exception-translation-enabled` | `true` | Enable exception translation |
| `product-name` | *(auto)* | Passed to `SQLManager` |

> **Note:** Other properties (`development-mode`,
> `sql-file-encoding`, etc.) are defined in
> `ClioneSqlProperties` for forward compatibility
> but currently have no effect due to clione-sql's
> `Config` class limitation.
> Configure them via `clione.properties` instead.

## Exception Translation

When enabled (default), clione-sql exceptions are
translated to Spring's `DataAccessException` hierarchy:

| clione-sql Exception | Spring Exception |
| -------------------- | ---------------- |
| `SQLRuntimeException` | via `SQLStateSQLExceptionTranslator` |
| `ConnectionNotFoundException` | `DataAccessResourceFailureException` |
| `SQLFileNotFoundException` | `NonTransientDataAccessResourceException` |
| `ParameterNotFoundException` | `InvalidDataAccessApiUsageException` |
| `ClioneFormatException` | `InvalidDataAccessApiUsageException` |
| `DuplicateKeyException` | `DuplicateKeyException` |

## Module Structure

```text
clione-sql-spring-boot-build (parent)
+-- clione-sql-spring-boot-core
+-- clione-sql-spring-boot-autoconfigure
+-- clione-sql-spring-boot-starter
+-- clione-sql-spring-boot-samples
```

- **core** -- Template, Properties, ExceptionTranslator
- **autoconfigure** -- Auto-configuration
- **starter** -- Dependency aggregation
- **samples** -- Sample application

## Constraints and Known Limitations

The following limitations originate from clione-sql:

### clione-sql is not on Maven Central

clione-sql is built with Ant and not published to any
Maven repository. Users must manually download the JAR
and install it to their local Maven repository.
See [Getting Started](#getting-started).

### Config class has immutable public final fields

clione-sql's `Config` class exposes configuration via
`public final` fields. This means Spring Boot properties
**cannot be dynamically injected** into the clione-sql
runtime configuration.

Properties such as `development-mode` and
`sql-file-cache-time` are defined in
`ClioneSqlProperties` for future use but have no effect
until clione-sql's `Config` class is modified to support
external injection.

To configure clione-sql's internal behavior
(e.g. development mode, encoding), place a
`clione.properties` file on the classpath as described
in the clione-sql documentation.

### SQLManager is not thread-safe

`SQLManager` instances must not be shared across threads.
`ClioneSqlTemplate` handles this by creating a new
`SQLManager` for each operation.

### Object mapping uses deep reflection

clione-sql's entity mapping
(`find(Entity.class, ...)`) uses
`Field.setAccessible(true)` to access private fields.
This works without warnings on JDK 17-21 for classes
in the unnamed module (standard Spring Boot apps).
However:

- If your application adopts **JPMS**
  (`module-info.java`), you must add `--add-opens`
  flags for entity packages.
- Future JDK versions may restrict reflective access
  to unnamed modules by default, which would break
  entity mapping.
- There is no fallback mechanism
  (e.g. `VarHandle`, `MethodHandle`)
  when `setAccessible` fails.

When entity mapping is not needed, use `ResultMap`
(the default return type) to avoid reflection entirely.

## License

[Apache License 2.0](LICENSE)
