package net.noworks.clionesql.boot;

import java.sql.SQLException;

import tetz42.clione.common.exception.SQLRuntimeException;
import tetz42.clione.exception.ClioneFormatException;
import tetz42.clione.exception.ConnectionNotFoundException;
import tetz42.clione.exception.ParameterNotFoundException;
import tetz42.clione.exception.SQLFileNotFoundException;

import org.junit.jupiter.api.Test;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.NonTransientDataAccessResourceException;

import static org.assertj.core.api.Assertions.assertThat;

class ClioneSqlPersistenceExceptionTranslatorTest {

    private final ClioneSqlPersistenceExceptionTranslator translator = new ClioneSqlPersistenceExceptionTranslator();

    @Test
    void translateSQLRuntimeException() {
        SQLException sqlEx = new SQLException("test error", "23000");
        SQLRuntimeException ex = new SQLRuntimeException(sqlEx);
        DataAccessException result = translator.translateExceptionIfPossible(ex);
        assertThat(result).isNotNull();
        assertThat(result.getCause()).isEqualTo(sqlEx);
    }

    @Test
    void translateConnectionNotFoundException() {
        ConnectionNotFoundException ex = new ConnectionNotFoundException("no connection");
        DataAccessException result = translator.translateExceptionIfPossible(ex);
        assertThat(result).isInstanceOf(DataAccessResourceFailureException.class);
        assertThat(result.getCause()).isEqualTo(ex);
    }

    @Test
    void translateSQLFileNotFoundException() {
        SQLFileNotFoundException ex = new SQLFileNotFoundException("file not found");
        DataAccessException result = translator.translateExceptionIfPossible(ex);
        assertThat(result).isInstanceOf(NonTransientDataAccessResourceException.class);
        assertThat(result.getCause()).isEqualTo(ex);
    }

    @Test
    void translateParameterNotFoundException() {
        ParameterNotFoundException ex = new ParameterNotFoundException("param missing");
        DataAccessException result = translator.translateExceptionIfPossible(ex);
        assertThat(result).isInstanceOf(InvalidDataAccessApiUsageException.class);
        assertThat(result.getCause()).isEqualTo(ex);
    }

    @Test
    void translateClioneFormatException() {
        ClioneFormatException ex = new ClioneFormatException("bad format");
        DataAccessException result = translator.translateExceptionIfPossible(ex);
        assertThat(result).isInstanceOf(InvalidDataAccessApiUsageException.class);
        assertThat(result.getCause()).isEqualTo(ex);
    }

    @Test
    void translateDuplicateKeyException() {
        tetz42.clione.exception.DuplicateKeyException ex = new tetz42.clione.exception.DuplicateKeyException(
                "duplicate");
        DataAccessException result = translator.translateExceptionIfPossible(ex);
        assertThat(result).isInstanceOf(DuplicateKeyException.class);
        assertThat(result.getCause()).isEqualTo(ex);
    }

    @Test
    void returnNullForUnknownException() {
        RuntimeException ex = new IllegalStateException("unknown");
        DataAccessException result = translator.translateExceptionIfPossible(ex);
        assertThat(result).isNull();
    }
}
