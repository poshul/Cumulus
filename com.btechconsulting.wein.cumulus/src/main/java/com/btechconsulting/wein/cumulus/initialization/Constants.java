package com.btechconsulting.wein.cumulus.initialization;

public class Constants {
	public static final String credentialsFile="AwsCredentials.properties";
	public static final Integer initialInstances=5;
	public static final Integer idealMaxUnitsPerInstance=100; //TODO tune this
	public static final String dispatchQueueName="dispatchQueue";
	public static final String returnQueueName="returnQueue";
	public static final String ec2Region="ec2.us-east-1.amazonaws.com";
	public static final String instanceType="c1.xlarge";//"c1.xlarge"; TODO switch back
	public static final String imageID="ami-14c11a7d";
	public static final String securityGroupID="ssh";
	public static final String keyName="Samkeys";
	//public static final String jdbcUrl="jdbc:mysql://192.168.1.16:3306/cumulus";
	public static final String jdbcUrl="jdbc:mysql://ec2-23-20-154-27.compute-1.amazonaws.com/cumulus";
	public static final String jdbcUser="cumulus";
	//public static final String jdbcPassword="cumulus";
	public static final String jdbcPassword="s3mir4ndom";
}