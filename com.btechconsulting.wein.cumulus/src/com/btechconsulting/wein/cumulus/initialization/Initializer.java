package com.btechconsulting.wein.cumulus.initialization;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;

/**
 * @author Samuel Wein
 * @date 12/1/2011
 *
 */
public enum Initializer {
	INSTANCE;
	

	public enum wUStatus{
		INFLIGHT,DONE,ERROR
	}
	private AmazonSQS dispatchQueue;
	private AmazonSQS returnQueue;
	private Map<String, Map<String,wUStatus>> unitsOnServer;
	
	private Initializer(){
		try{
			dispatchQueue = createQueue(Constants.credentialsFile, Constants.dispatchQueueName);
			returnQueue = createQueue(Constants.credentialsFile, Constants.returnQueueName);
			unitsOnServer= createUnitsOnServer();
			createInitialInstances(Constants.credentialsFile);
		}
		catch (AmazonServiceException ase) {
			System.err.println("Caught an AmazonServiceException, which means your request made it " +
					"to Amazon AWS, but was rejected with an error response for some reason.");
			System.err.println("Error Message:    " + ase.getMessage());
			System.err.println("HTTP Status Code: " + ase.getStatusCode());
			System.err.println("AWS Error Code:   " + ase.getErrorCode());
			System.err.println("Error Type:       " + ase.getErrorType());
			System.err.println("Request ID:       " + ase.getRequestId());
		}
		catch (AmazonClientException ace) {
            System.err.println("Caught an AmazonClientException, which means the client encountered " +
                    "a serious internal problem while trying to communicate with AWS, such as not " +
                    "being able to access the network.");
            System.err.println("Error Message: " + ace.getMessage());
		}
		catch (Exception e){
			System.err.println();
		}

	}

	private void createInitialInstances(String credentialsFile) throws Exception {
		AmazonEC2 ec2= new AmazonEC2Client(new PropertiesCredentials(
				Initializer.class.getResourceAsStream(credentialsFile)));
		//set the zone
		ec2.setEndpoint(Constants.ec2Region);
		System.out.println("Intializing "+Constants.initialInstances+" instances\n");
		//create EC2 instances
		RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
		.withInstanceType(Constants.instanceType)
		.withImageId(Constants.imageID)
		.withMinCount(Constants.initialInstances)
		.withMaxCount(Constants.initialInstances)
		.withSecurityGroupIds(Constants.securityGroupID)
		.withKeyName(Constants.keyName);

		RunInstancesResult runInstances = ec2.runInstances(runInstancesRequest);
		System.out.println(runInstances.toString());
	}

	/* creatUnitsOnServer: creates the datastructure for storing workunits
	 */

	private Map<String, Map<String, wUStatus>> createUnitsOnServer() {
		return Collections.synchronizedMap(new HashMap<String, Map<String,wUStatus>>());
	}

	/* createQueue: creates the AWS SQS queue for cumulus.  Throws Exception if we can't create the queue
	 * @param credentialsFile the file containing credentials to access the AWS
	 * @return AmazonSQS the queue
	 */
	private AmazonSQS createQueue(String credentialsFile, String queueName) throws Exception {
		AmazonSQS sqs = new AmazonSQSClient(new PropertiesCredentials(
				Initializer.class.getResourceAsStream(credentialsFile)));
		// Create a queue
		System.out.println("Creating a new SQS queue "+queueName+".\n");
		CreateQueueRequest createQueueRequest = new CreateQueueRequest(queueName);
		return sqs;
	}

}
