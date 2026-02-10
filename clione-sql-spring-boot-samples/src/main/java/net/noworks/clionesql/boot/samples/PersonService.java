package net.noworks.clionesql.boot.samples;

import java.util.List;

import tetz42.clione.SQLManager;
import tetz42.clione.util.ResultMap;

import net.noworks.clionesql.boot.ClioneSqlTemplate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for person-related database operations.
 *
 * <p>
 * Demonstrates both {@code useFile} (external SQL templates) and {@code useSQL} (inline 2Way SQL) approaches with
 * {@link ClioneSqlTemplate}.
 */
@Service
public class PersonService {

    private final ClioneSqlTemplate clioneSql;

    /**
     * Creates a new {@code PersonService}.
     *
     * @param clioneSql
     *            the clione-sql template for database access
     */
    public PersonService(ClioneSqlTemplate clioneSql) {
        this.clioneSql = clioneSql;
    }

    /**
     * Finds all persons whose name matches the given value.
     *
     * @param name
     *            the name to search for
     *
     * @return a list of matching person records
     */
    @Transactional(readOnly = true)
    public List<ResultMap> findByName(String name) {
        return clioneSql.useFile("sql/person/SelectByName.sql").findAll(SQLManager.params("name", name));
    }

    /**
     * Retrieves all persons from the database.
     *
     * @return a list of all person records
     */
    @Transactional(readOnly = true)
    public List<ResultMap> findAll() {
        return clioneSql.useFile("sql/person/SelectAll.sql").findAll();
    }

    /**
     * Updates the status of the person with the given ID.
     *
     * @param id
     *            the person ID
     * @param status
     *            the new status value
     *
     * @return the number of rows affected
     */
    @Transactional
    public int updateStatus(int id, String status) {
        return clioneSql.useFile("sql/person/UpdateStatus.sql").update(SQLManager.params("id", id).$("status", status));
    }

    /**
     * Finds a single person by ID using an inline 2Way SQL string.
     *
     * @param id
     *            the person ID
     *
     * @return the matching person record, or {@code null} if not found
     */
    @Transactional(readOnly = true)
    public ResultMap findById(int id) {
        return clioneSql.useSQL("SELECT id, name, status, created_at FROM person WHERE id = /* id */'1'")
                .find(SQLManager.params("id", id));
    }

    /**
     * Inserts a new person using an inline 2Way SQL string.
     *
     * @param id
     *            the person ID
     * @param name
     *            the person's name
     * @param status
     *            the person's status
     *
     * @return the number of rows affected
     */
    @Transactional
    public int insert(int id, String name, String status) {
        return clioneSql.useSQL(
                "INSERT INTO person (id, name, status) VALUES (/* id */'0', /* name */'dummy', /* status */'ACTIVE')")
                .update(SQLManager.params("id", id).$("name", name).$("status", status));
    }
}
