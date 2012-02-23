package com.btechconsulting.wein.nimbus;

import org.apache.tomcat.dbcp.pool.impl.GenericKeyedObjectPool.Config;

public final class Constants {
	public static final String VINALOC="vina";  //the name of the executable for vina
	public static final String CREDENTIALSFILE="/resources/AwsCredentials.properties"; //TODO change to read only keys before deploy
	public static final String NIMBUSPROPSFILE="/resources/nimbus.properties";
	public static final String INSTANCEIDLOC="http://169.254.169.254/latest/meta-data/instance-id";
	public static final String SHUTDOWNCOMMAND="/sbin/shutdown";
	public static final String SHUTDOWNMODIFIER="-k";
	public static final Integer WAITTIME=600000;
	public static final String JDBCURL="jdbc:mysql://192.168.1.16:3306/cumulus";
	public static final String JDBCUSER="cumulus";
	public static final String JDBBPASSWORD="cumulus";
	public static final Boolean ONCLOUD=false;
}
