package com.btechconsulting.wein.nimbus;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.apache.tomcat.dbcp.dbcp.ConnectionFactory;
import org.apache.tomcat.dbcp.dbcp.DriverManagerConnectionFactory;
import org.apache.tomcat.dbcp.dbcp.PoolableConnectionFactory;
import org.apache.tomcat.dbcp.dbcp.PoolingDataSource;
import org.apache.tomcat.dbcp.pool.ObjectPool;
import org.apache.tomcat.dbcp.pool.impl.GenericObjectPool;

//imported from cumulus.  TODO change this to non pooled connections

public enum PooledConnectionFactory {
	INSTANCE;
	
	/**
	 * Create a singleton connection pool to the cumulus database
	 */
	
	private final Logger logger = Logger.getLogger(PooledConnectionFactory.class);
	private DataSource cumulusDatasource= null;
	
	private PooledConnectionFactory() {
		try{
            Class.forName ("com.mysql.jdbc.Driver").newInstance();
		}
		catch (Exception cnfe) {
			logger.error(cnfe.getMessage(), cnfe);
			throw new IllegalStateException("Could not load database driver class: "+ cnfe.getMessage());
		}
		
		//check to make sure that JDBCurl is defined
		if (Constants.JDBCURL==null||Constants.JDBBPASSWORD==null||Constants.JDBCUSER==null){
			throw new IllegalStateException("Couldn't find configuration parameters for JDBC");
		}
		ObjectPool cumulusConnectionPool= new GenericObjectPool(null);
		ConnectionFactory cumulusConnectionFactory= new DriverManagerConnectionFactory(Constants.JDBCURL, Constants.JDBCUSER, Constants.JDBBPASSWORD);
		@SuppressWarnings("unused")
		PoolableConnectionFactory cumulusPoolableConnectionFactory = new PoolableConnectionFactory(cumulusConnectionFactory, cumulusConnectionPool, null, null, false, true);
		cumulusDatasource= new PoolingDataSource(cumulusConnectionPool);
		((PoolingDataSource) cumulusDatasource).setAccessToUnderlyingConnectionAllowed(true);
		// do selftest
		try{
			Connection cumulusTestConnection = cumulusDatasource.getConnection();
			DatabaseMetaData meta = cumulusTestConnection.getMetaData();
			logger.info("Cumulus JDBC driver name is "+meta.getDriverName());
			logger.info("Cumulus JDBC driver version is "+meta.getDriverVersion());
			cumulusTestConnection.close();
		} catch (SQLException sqe){
			throw new IllegalStateException("Could not get connection to test PooledConnectionFactory initialization", sqe);
		}
	}
	/**
	 * 
	 * @return returns a connection from the connection pool
	 * @throws SQLException
	 */
	public Connection getCumulusConnection() throws SQLException{
		try{
			return cumulusDatasource.getConnection();
		}
		catch(SQLException sqe){
			logger.error("Could not obtain connection: "+sqe.getMessage(),sqe);
			throw new SQLException("Could not obtain connection: "+sqe.getMessage(),sqe);
		}
	}

}
