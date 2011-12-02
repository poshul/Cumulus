package com.btechconsulting.wein.cumulus.initialization;

public class Constants {
	public static final String credentialsFile="resources/AwsCredentials.properties";
	public static final Integer initialInstances=2;
	public static final String dispatchQueueName="dispatchQueue";
	public static final String returnQueueName="returnQueue";
	public static final String ec2Region="ec2.us-east-1.amazonaws.com";
	public static final String instanceType="t1.micro";//"c1.xlarge"; TODO switch back
	public static final String imageID="ami-1e39ca77";
	public static final String securityGroupID="ssh";
	public static final String keyName="Samkeys";
}
