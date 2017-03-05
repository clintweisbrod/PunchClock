package punchclock.db;

import java.io.*;
import java.sql.*;
import java.util.logging.Logger;

import javax.sql.*;

/**
 * Class which implements the JDBC 2.0 SE DataSource
 * interface to allow use of connection pooling without using a JDBC 2.0
 * DataSource.
 */
public class DataSourceProxy implements DataSource {
	private DBConnectionManager pool;
	private String poolName;
	static final String NOT_SUPPORTED = "Not supported.";

	public DataSourceProxy(String poolName) throws ClassNotFoundException, InstantiationException,
		SQLException, IllegalAccessException {
		this.poolName = poolName;
		pool = DBConnectionManager.getInstance();
	}

	/**
	 * Gets a connection from the connection pool.
	 */
	public Connection getConnection() throws SQLException {
			return new ConnectionProxy(pool.getConnection(poolName), this);
	}

	/**
	 * Returns a Connection to the connection pool.
	 */
	public void returnConnection(Connection conn) {
			pool.freeConnection(poolName, conn);
	}

	/**
	 * Username and password are set when the pool initializes connections.
	 */
	public Connection getConnection(String username, String password)
					throws SQLException {
			throw new SQLException(NOT_SUPPORTED);
	}

	/**
	 * Not supported.
	 */
	public int getLoginTimeout() throws SQLException {
			throw new SQLException(NOT_SUPPORTED);
	}

	/**
	 * Not supported.
	 */
	public PrintWriter getLogWriter() throws SQLException {
			throw new SQLException(NOT_SUPPORTED);
	}
	
	/**
	 * Not supported.
	 */
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException(NOT_SUPPORTED);
	}
	
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return true;
	}

	/**
	 * Release all resources owned by the connection pool.
	 */
	public void release() {
			pool.release();
	}

	/**
	 * Not supported.
	 */
	public void setLoginTimeout(int seconds) throws SQLException {
			throw new SQLException(NOT_SUPPORTED);
	}

	/**
	 * Not supported.
	 */
	public synchronized void setLogWriter(PrintWriter out) throws SQLException {
			throw new SQLException("Not supported");
	}
	
	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new SQLException("jdbc.feature_not_supported");
	}
}
