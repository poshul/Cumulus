package com.btechconsulting.wein.cumulus.initialization;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;

/**
 * @author Samuel Wein
 * @date 12/1/2011
 *
 */
public enum Initializer {
	instance;

	public enum wUStatus{
		INFLIGHT,DONE,ERROR
	}

	private Initializer(){
		try{
			AmazonSQS dispatchQueue = createQueue(Constants.credentialsFile, Constants.dispatchQueueName);
			AmazonSQS returnQueue = createQueue(Constants.credentialsFile, Constants.returnQueueName);
			Map<String, Map<String,wUStatus>> unitsOnServer= createUnitsOnServer();
			createInitialInstances(Constants.initialInstances, Constants.credentialsFile);
		}
		catch (Exception e) {
			// TODO: handle exception
		}

	}

	private void createInitialInstances(Integer initialinstances, String credentialsFile) throws Exception {
		AmazonEC2 ec2= new AmazonEC2Client(new PropertiesCredentials(
				Initializer.class.getResourceAsStream(credentialsFile)));
		//TODO restart here
		
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
