package com.btechconsulting.wein.cumulus.initialization;

public class Constants {
	public static final String credentialsFile="AwsCredentials.properties";//FIXME change back
	public static final Integer initialInstances=2;
	public static final String dispatchQueueName="dispatchQueue";
	public static final String returnQueueName="returnQueue";
	public static final String ec2Region="ec2.us-east-1.amazonaws.com";
	public static final String instanceType="t1.micro";//"c1.xlarge"; TODO switch back
	public static final String imageID="ami-1e39ca77";
	public static final String securityGroupID="ssh";
	public static final String keyName="Samkeys";
	public static final String jdbcUrl="jdbc:mysql://192.168.1.16:3306/cumulus";
/*	} else{
		public static final String jdbcUrl=System.getProperty("database.jdbcUrl");//"jdbc:mysql://192.168.1.16:3306/cumulus";
	}*/

	public static final String jdbcUser="cumulus";
	public static final String jdbcPassword="cumulus";
}