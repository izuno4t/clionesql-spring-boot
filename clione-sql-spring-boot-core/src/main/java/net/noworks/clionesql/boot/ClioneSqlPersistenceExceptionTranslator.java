package net.noworks.clionesql.boot;

import java.sql.SQLException;

import tetz42.clione.common.exception.SQLRuntimeException;
import tetz42.clione.exception.ClioneFormatException;
import tetz42.clione.exception.ConnectionNotFoundException;
import tetz42.clione.exception.ParameterNotFoundException;
import tetz42.clione.exception.SQLFileNotFoundException;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.NonTransientDataAccessResourceException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;

/**
 * {@link PersistenceExceptionTranslator} implementation that translates clione-sql exceptions into Spring's
 * {@link DataAccessException} hierarchy.
 *
 * <p>
 * The following mappings are applied:
 * <ul>
 * <li>{@link SQLRuntimeException} &rarr; delegated to {@link SQLStateSQLExceptionTranslator} for SQL-state-based
 * translation</li>
 * <li>{@link ConnectionNotFoundException} &rarr; {@link DataAccessResourceFailureException}</li>
 * <li>{@link SQLFileNotFoundException} &rarr; {@link NonTransientDataAccessResourceException}</li>
 * <li>{@link ParameterNotFoundException} &rarr; {@link InvalidDataAccessApiUsageException}</li>
 * <li>{@link ClioneFormatException} &rarr; {@link InvalidDataAccessApiUsageException}</li>
 * <li>{@link tetz42.clione.exception.DuplicateKeyException} &rarr; {@link DuplicateKeyException}</li>
 * </ul>
 *
 * <p>
 * Unrecognized exceptions return {@code null}, indicating that this translator cannot handle them.
 *
 * @see ClioneSqlProperties#isExceptionTranslationEnabled()
 */
public class ClioneSqlPersistenceExceptionTranslator implements PersistenceExceptionTranslator {

    /** Creates a new {@code ClioneSqlPersistenceExceptionTranslator}. */
    public ClioneSqlPersistenceExceptionTranslator() {
    }

    private final SQLStateSQLExceptionTranslator sqlStateTranslator = new SQLStateSQLExceptionTranslator();

    /**
     * Translates the given clione-sql runtime exception into a Spring {@link DataAccessException}, if possible.
     *
     * @param ex
     *            the runtime exception to translate
     *
     * @return the corresponding {@link DataAccessException}, or {@code null} if the exception is not recognized
     */
    @Override
    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
        if (ex instanceof SQLRuntimeException sqlRuntimeEx) {
            SQLException sqlEx = sqlRuntimeEx.getSQLException();
            return sqlStateTranslator.translate("clione-sql", null, sqlEx);
        }

        if (ex instanceof ConnectionNotFoundException) {
            return new DataAccessResourceFailureException(ex.getMessage(), ex);
        }

        if (ex instanceof SQLFileNotFoundException) {
            return new NonTransientDataAccessResourceException(ex.getMessage(), ex);
        }

        if (ex instanceof ParameterNotFoundException) {
            return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
        }

        if (ex instanceof ClioneFormatException) {
            return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
        }

        if (ex instanceof tetz42.clione.exception.DuplicateKeyException) {
            return new DuplicateKeyException(ex.getMessage(), ex);
        }

        return null;
    }
}
