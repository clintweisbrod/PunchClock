package punchclock.db;

import java.sql.*;
import java.util.*;
import java.util.concurrent.Executor;

/**
 * Standard Connection object proxy to support JDBC 2.0 DataSource and
 * with ConnectionPooling.
 */
public class ConnectionProxy implements Connection {
		private Connection realConn;
		private DataSourceProxy dsp;
		private boolean isClosed = false;
		static final String CONNECTION_CLOSED = "Connection is closed.";

		public ConnectionProxy(Connection realConn, DataSourceProxy dsp) {
				this.realConn = realConn;
				this.dsp = dsp;
		}

		/**
		 * Return the connection back to the pool.
		 */
		public void close() throws SQLException {
				isClosed = true;
				dsp.returnConnection(realConn);
		}

		/**
		 * Returns true if the ConnectionProxy is closed, false
		 * otherwise.
		 */
		public boolean isClosed() throws SQLException {
				return isClosed;
		}

		/*
		 * Connection interface implementation.
		 */
		public void clearWarnings() throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				realConn.clearWarnings();
		}

		public void commit() throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				realConn.commit();
		}
		
		public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
			return new SimpleArray();
		}
		
		public Blob createBlob() throws SQLException {
			return new SimpleBlob();
		}
		
		public Clob createClob() throws SQLException {
			return new SimpleClob();
		}
		
		public NClob createNClob() throws SQLException {
			return new SimpleNClob();
		}
		
		public SQLXML createSQLXML() throws SQLException {
			return new SimpleSQLXML();
		}

		public Statement createStatement() throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				return realConn.createStatement();
		}

		public Statement createStatement(int resultSetType,
				int resultSetConcurrency) throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				return realConn.createStatement(resultSetType, resultSetConcurrency);
		}
		
		public Statement createStatement(int resultSetType,
				int resultSetConcurrency, int resultSetHoldability) throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				return realConn.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
		}
		
		public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
			return new SimpleStruct();
		}

		 public boolean getAutoCommit() throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				return realConn.getAutoCommit();
		}

		 public String getCatalog() throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				return realConn.getCatalog();
		}
		
		public Properties getClientInfo() throws SQLException {
			return new Properties();
		}
		 
		public String getClientInfo(String name) throws SQLException {
			return name;
		}
		
		public int getHoldability() throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				return realConn.getHoldability();
		}

		public DatabaseMetaData getMetaData() throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				return realConn.getMetaData();
		}

		public int getTransactionIsolation() throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				return realConn.getTransactionIsolation();
		}

		public Map<String,Class<?>> getTypeMap() throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				return realConn.getTypeMap();
		}

		public SQLWarning getWarnings() throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				return realConn.getWarnings();
		}

		public boolean isReadOnly() throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				return realConn.isReadOnly();
		}
		
		public boolean isValid(int timeout) throws SQLException{
			return true;
		}
		
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return true;
		}

		public String nativeSQL(String sql) throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				return realConn.nativeSQL(sql);
		}

		public CallableStatement prepareCall(String sql) throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				return realConn.prepareCall(sql);
		}

		public CallableStatement prepareCall(String sql,int resultSetType,
				int resultSetConcurrency) throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				return realConn.prepareCall(sql, resultSetType, resultSetConcurrency);
		}
		
		public CallableStatement prepareCall(String sql,int resultSetType,
				int resultSetConcurrency, int resultSetHoldability) throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				return realConn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
		}
		
		public PreparedStatement prepareStatement(String sql) throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				return realConn.prepareStatement(sql);
		}
		
		public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
			if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				return realConn.prepareStatement(sql, autoGeneratedKeys);
		}
		
		public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
			if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				return realConn.prepareStatement(sql, columnIndexes);
		}

		public PreparedStatement prepareStatement(String sql, int resultSetType,
				int resultSetConcurrency) throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				return realConn.prepareStatement(sql, resultSetType, resultSetConcurrency);
		}
		
		public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
			if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				return realConn.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
		}
		
		public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
			if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				return realConn.prepareStatement(sql, columnNames);
		}
		
		public void releaseSavepoint(Savepoint savepoint) throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				realConn.releaseSavepoint(savepoint);
		}

		public void rollback() throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				realConn.rollback();
		}
		
		public void rollback(Savepoint savepoint) throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				realConn.rollback(savepoint);
		}

		public void setAutoCommit(boolean autoCommit) throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				realConn.setAutoCommit(autoCommit);
		}

		public void setCatalog(String catalog) throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				realConn.setCatalog(catalog);
		}
		
		public void setClientInfo(Properties properties) throws SQLClientInfoException {
		}
		
		public void setClientInfo(String name, String value) throws SQLClientInfoException {
		}
		
		public void setHoldability(int holdability) throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				realConn.setHoldability(holdability);
		}

		public void setReadOnly(boolean readOnly) throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				realConn.setReadOnly(readOnly);
		}
		
		public Savepoint setSavepoint() throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				return realConn.setSavepoint();
		}
		
		public Savepoint setSavepoint(String name) throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				return realConn.setSavepoint(name);
		}

		public void setTransactionIsolation(int level) throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				realConn.setTransactionIsolation(level);
		}

		public void setTypeMap(Map<String,Class<?>> map) throws SQLException {
				if (isClosed) {
						throw new SQLException(CONNECTION_CLOSED);
				}
				realConn.setTypeMap(map);
		}
		
		public <T> T unwrap(Class<T> iface) throws SQLException {
			throw new SQLException("jdbc.feature_not_supported");
		}
		
		public int getNetworkTimeout() throws SQLException {
			throw new SQLException(CONNECTION_CLOSED); 
		}
		
		public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
			throw new SQLException(CONNECTION_CLOSED);
		}
		
		public void abort(Executor executor) throws SQLException {
			throw new SQLException(CONNECTION_CLOSED); 
		}
		
		public String getSchema() throws SQLException {
			throw new SQLException(CONNECTION_CLOSED); 
		}
		
		public void setSchema(String schema) throws SQLException {
			throw new SQLException(CONNECTION_CLOSED); 
		}
}
