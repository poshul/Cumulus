package com.btechconsulting.wein.nimbus;

import java.io.IOException;
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

//imported from cumulus.

public enum PooledConnectionFactory {
	INSTANCE;
	
	/**
	 * Create a singleton connection pool to the cumulus database
	 */
	
	private final Logger logger = Logger.getLogger(PooledConnectionFactory.class);
	private DataSource cumulusDatasource= null;
	
	private PooledConnectionFactory() {
		//get Properties file
		Boolean mvn= false; //this tracks if we are being run from the maven build, and whether to take properties from constants for nimbus.properties
		java.util.Properties props= new java.util.Properties();
		try {
			props.load( Main.class.getResourceAsStream(Constants.NIMBUSPROPSFILE));
			mvn=true;
		} catch (IOException e4) {
			//this means we couldn't load the properties file, so we default to the settings in constants.
			e4.printStackTrace();
			mvn=false;
		}
		//see comment about Boolean mvn
		String JDBCURL;
		String JDBCPASSWORD;
		String JDBCUSER;
		if (mvn==true){
			JDBCURL=  props.getProperty("JDBCURL");
			JDBCUSER= props.getProperty("JDBCUser");
			JDBCPASSWORD= props.getProperty("JDBCPassword");
		}else{
			JDBCURL=  Constants.JDBCURL;
			JDBCUSER= Constants.JDBCUSER;
			JDBCPASSWORD= Constants.JDBCPASSWORD;
		}
		
		try{
            Class.forName ("com.mysql.jdbc.Driver").newInstance();
		}
		catch (Exception cnfe) {
			logger.error(cnfe.getMessage(), cnfe);
			throw new IllegalStateException("Could not load database driver class: "+ cnfe.getMessage());
		}
		
		//check to make sure that JDBCurl is defined
		//this checks the Constants class if we are being run without maven, and the nimbus.properties file otherwise
		if (JDBCURL==null||JDBCPASSWORD==null||JDBCUSER==null){
			throw new IllegalStateException("Couldn't find configuration parameters for JDBC");
		}
		ObjectPool cumulusConnectionPool= new GenericObjectPool(null);
		ConnectionFactory cumulusConnectionFactory= new DriverManagerConnectionFactory(JDBCURL, JDBCUSER, JDBCPASSWORD);
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
