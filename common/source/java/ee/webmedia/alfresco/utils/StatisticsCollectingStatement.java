package ee.webmedia.alfresco.utils;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

import org.alfresco.util.Pair;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.listener.StatisticsPhaseListener;
import ee.webmedia.alfresco.common.listener.StatisticsPhaseListenerLogColumn;
import ee.webmedia.alfresco.monitoring.MonitoredService;
import ee.webmedia.alfresco.monitoring.MonitoringUtil;

public class StatisticsCollectingStatement implements PreparedStatement {

    private final Statement statement;
    private final PreparedStatement preparedStatement;

    public StatisticsCollectingStatement(Statement statement) {
        Assert.notNull(statement);
        this.statement = statement;
        if (statement instanceof PreparedStatement) {
            preparedStatement = (PreparedStatement) statement;
        } else {
            preparedStatement = null;
        }
    }

    public Statement getDelegate() {
        return statement;
    }

    private static ThreadLocal<Boolean> isTimingInProgress = new ThreadLocal<Boolean>();

    private Pair<Boolean, Long> beginTiming() {
        Boolean performTiming = Boolean.FALSE;
        long startTime = 0;
        if (!Boolean.TRUE.equals(isTimingInProgress.get())) {
            isTimingInProgress.set(Boolean.TRUE);
            performTiming = Boolean.TRUE;
            startTime = System.nanoTime();
        }
        return Pair.newInstance(performTiming, startTime);
    }

    private static void endTiming(Pair<Boolean, Long> timing) {
        if (Boolean.TRUE.equals(timing.getFirst())) {
            StatisticsPhaseListener.addTimingNano(StatisticsPhaseListenerLogColumn.DB, timing.getSecond());
            isTimingInProgress.set(null);
        }
    }

    private void logSuccess() {
        MonitoringUtil.logSuccess(MonitoredService.OUT_DATABASE);
    }

    private void logError(Exception e) {
        MonitoringUtil.logError(MonitoredService.OUT_DATABASE, e);
    }

    // ADD STATISTICS

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        Pair<Boolean, Long> timing = beginTiming();
        try {
            ResultSet resultSet = statement.executeQuery(sql);
            logSuccess();
            return resultSet;
        } catch (SQLException e) {
            logError(e);
            throw e;
        } catch (RuntimeException e) {
            logError(e);
            throw e;
        } finally {
            endTiming(timing);
        }
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        Pair<Boolean, Long> timing = beginTiming();
        try {
            ResultSet resultSet = preparedStatement.executeQuery();
            logSuccess();
            return resultSet;
        } catch (SQLException e) {
            logError(e);
            throw e;
        } catch (RuntimeException e) {
            logError(e);
            throw e;
        } finally {
            endTiming(timing);
        }
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        Pair<Boolean, Long> timing = beginTiming();
        try {
            int rowCount = statement.executeUpdate(sql);
            logSuccess();
            return rowCount;
        } catch (SQLException e) {
            logError(e);
            throw e;
        } catch (RuntimeException e) {
            logError(e);
            throw e;
        } finally {
            endTiming(timing);
        }
    }

    @Override
    public int executeUpdate() throws SQLException {
        Pair<Boolean, Long> timing = beginTiming();
        try {
            int rowCount = preparedStatement.executeUpdate();
            logSuccess();
            return rowCount;
        } catch (SQLException e) {
            logError(e);
            throw e;
        } catch (RuntimeException e) {
            logError(e);
            throw e;
        } finally {
            endTiming(timing);
        }
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        Pair<Boolean, Long> timing = beginTiming();
        try {
            boolean result = statement.execute(sql);
            logSuccess();
            return result;
        } catch (SQLException e) {
            logError(e);
            throw e;
        } catch (RuntimeException e) {
            logError(e);
            throw e;
        } finally {
            endTiming(timing);
        }
    }

    @Override
    public boolean execute() throws SQLException {
        Pair<Boolean, Long> timing = beginTiming();
        try {
            boolean result = preparedStatement.execute();
            logSuccess();
            return result;
        } catch (SQLException e) {
            logError(e);
            throw e;
        } catch (RuntimeException e) {
            logError(e);
            throw e;
        } finally {
            endTiming(timing);
        }
    }

    @Override
    public int[] executeBatch() throws SQLException {
        Pair<Boolean, Long> timing = beginTiming();
        try {
            return statement.executeBatch();
        } finally {
            endTiming(timing);
        }
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        Pair<Boolean, Long> timing = beginTiming();
        try {
            int rowCount = statement.executeUpdate(sql, autoGeneratedKeys);
            logSuccess();
            return rowCount;
        } catch (SQLException e) {
            logError(e);
            throw e;
        } catch (RuntimeException e) {
            logError(e);
            throw e;
        } finally {
            endTiming(timing);
        }
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        Pair<Boolean, Long> timing = beginTiming();
        try {
            int rowCount = statement.executeUpdate(sql, columnIndexes);
            logSuccess();
            return rowCount;
        } catch (SQLException e) {
            logError(e);
            throw e;
        } catch (RuntimeException e) {
            logError(e);
            throw e;
        } finally {
            endTiming(timing);
        }
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        Pair<Boolean, Long> timing = beginTiming();
        try {
            int rowCount = statement.executeUpdate(sql, columnNames);
            logSuccess();
            return rowCount;
        } catch (SQLException e) {
            logError(e);
            throw e;
        } catch (RuntimeException e) {
            logError(e);
            throw e;
        } finally {
            endTiming(timing);
        }
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        Pair<Boolean, Long> timing = beginTiming();
        try {
            boolean result = statement.execute(sql, autoGeneratedKeys);
            logSuccess();
            return result;
        } catch (SQLException e) {
            logError(e);
            throw e;
        } catch (RuntimeException e) {
            logError(e);
            throw e;
        } finally {
            endTiming(timing);
        }
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        Pair<Boolean, Long> timing = beginTiming();
        try {
            boolean result = statement.execute(sql, columnIndexes);
            logSuccess();
            return result;
        } catch (SQLException e) {
            logError(e);
            throw e;
        } catch (RuntimeException e) {
            logError(e);
            throw e;
        } finally {
            endTiming(timing);
        }
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        Pair<Boolean, Long> timing = beginTiming();
        try {
            boolean result = statement.execute(sql, columnNames);
            logSuccess();
            return result;
        } catch (SQLException e) {
            logError(e);
            throw e;
        } catch (RuntimeException e) {
            logError(e);
            throw e;
        } finally {
            endTiming(timing);
        }
    }

    // PASS-THROUGH

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return statement.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return statement.isWrapperFor(iface);
    }

    @Override
    public void close() throws SQLException {
        statement.close();
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        preparedStatement.setNull(parameterIndex, sqlType);
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return statement.getMaxFieldSize();
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        preparedStatement.setBoolean(parameterIndex, x);
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        statement.setMaxFieldSize(max);
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        preparedStatement.setByte(parameterIndex, x);
    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        preparedStatement.setShort(parameterIndex, x);
    }

    @Override
    public int getMaxRows() throws SQLException {
        return statement.getMaxRows();
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        preparedStatement.setInt(parameterIndex, x);
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        statement.setMaxRows(max);
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        preparedStatement.setLong(parameterIndex, x);
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        statement.setEscapeProcessing(enable);
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        preparedStatement.setFloat(parameterIndex, x);
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return statement.getQueryTimeout();
    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        preparedStatement.setDouble(parameterIndex, x);
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        statement.setQueryTimeout(seconds);
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        preparedStatement.setBigDecimal(parameterIndex, x);
    }

    @Override
    public void cancel() throws SQLException {
        statement.cancel();
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        preparedStatement.setString(parameterIndex, x);
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return statement.getWarnings();
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        preparedStatement.setBytes(parameterIndex, x);
    }

    @Override
    public void clearWarnings() throws SQLException {
        statement.clearWarnings();
    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        preparedStatement.setDate(parameterIndex, x);
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        statement.setCursorName(name);
    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        preparedStatement.setTime(parameterIndex, x);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        preparedStatement.setTimestamp(parameterIndex, x);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        preparedStatement.setAsciiStream(parameterIndex, x, length);
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return statement.getResultSet();
    }

    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        preparedStatement.setUnicodeStream(parameterIndex, x, length);
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return statement.getUpdateCount();
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return statement.getMoreResults();
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        preparedStatement.setBinaryStream(parameterIndex, x, length);
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        statement.setFetchDirection(direction);
    }

    @Override
    public void clearParameters() throws SQLException {
        preparedStatement.clearParameters();
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return statement.getFetchDirection();
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        preparedStatement.setObject(parameterIndex, x, targetSqlType);
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        statement.setFetchSize(rows);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return statement.getFetchSize();
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        preparedStatement.setObject(parameterIndex, x);
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return statement.getResultSetConcurrency();
    }

    @Override
    public int getResultSetType() throws SQLException {
        return statement.getResultSetType();
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        statement.addBatch(sql);
    }

    @Override
    public void clearBatch() throws SQLException {
        statement.clearBatch();
    }

    @Override
    public void addBatch() throws SQLException {
        preparedStatement.addBatch();
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        preparedStatement.setCharacterStream(parameterIndex, reader, length);
    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        preparedStatement.setRef(parameterIndex, x);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return statement.getConnection();
    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        preparedStatement.setBlob(parameterIndex, x);
    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        preparedStatement.setClob(parameterIndex, x);
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return statement.getMoreResults(current);
    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        preparedStatement.setArray(parameterIndex, x);
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return preparedStatement.getMetaData();
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return statement.getGeneratedKeys();
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        preparedStatement.setDate(parameterIndex, x, cal);
    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        preparedStatement.setTime(parameterIndex, x, cal);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        preparedStatement.setTimestamp(parameterIndex, x, cal);
    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        preparedStatement.setNull(parameterIndex, sqlType, typeName);
    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        preparedStatement.setURL(parameterIndex, x);
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return preparedStatement.getParameterMetaData();
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        preparedStatement.setRowId(parameterIndex, x);
    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
        preparedStatement.setNString(parameterIndex, value);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        preparedStatement.setNCharacterStream(parameterIndex, value, length);
    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        preparedStatement.setNClob(parameterIndex, value);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        preparedStatement.setClob(parameterIndex, reader, length);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return statement.getResultSetHoldability();
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        preparedStatement.setBlob(parameterIndex, inputStream, length);
    }

    @Override
    public boolean isClosed() throws SQLException {
        return statement.isClosed();
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        statement.setPoolable(poolable);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        preparedStatement.setNClob(parameterIndex, reader, length);
    }

    @Override
    public boolean isPoolable() throws SQLException {
        return statement.isPoolable();
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        preparedStatement.setSQLXML(parameterIndex, xmlObject);
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        preparedStatement.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        preparedStatement.setAsciiStream(parameterIndex, x, length);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        preparedStatement.setBinaryStream(parameterIndex, x, length);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        preparedStatement.setCharacterStream(parameterIndex, reader, length);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        preparedStatement.setAsciiStream(parameterIndex, x);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        preparedStatement.setBinaryStream(parameterIndex, x);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        preparedStatement.setCharacterStream(parameterIndex, reader);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        preparedStatement.setNCharacterStream(parameterIndex, value);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        preparedStatement.setClob(parameterIndex, reader);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        preparedStatement.setBlob(parameterIndex, inputStream);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        preparedStatement.setNClob(parameterIndex, reader);
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return false;
    }

}
