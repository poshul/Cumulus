package com.btechconsulting.wein.cumulus.initialization;


import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;

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
	private String dispatchQueue;
	private String returnQueue;
	private AmazonSQS sqsClient;
	private Map<String, Map<String,wUStatus>> unitsOnServer;
	
	private Initializer(){
		try{
			sqsClient = new AmazonSQSClient(new PropertiesCredentials(
					new FileInputStream(Constants.credentialsFile)));
			dispatchQueue = createQueue(sqsClient, Constants.dispatchQueueName);
			returnQueue = createQueue(sqsClient, Constants.returnQueueName);
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
		catch (NullPointerException npee){
			System.err.println("Couldn't find credentials file\n");
		}
		catch (Exception e){
			System.err.println(e);
		}

	}

	private void createInitialInstances(String credentialsFile) throws Exception {
		AmazonEC2 ec2= new AmazonEC2Client(new PropertiesCredentials(
				new FileInputStream(credentialsFile)));
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
	private String createQueue(AmazonSQS client, String queueName) throws Exception {
		// Create a queue
		System.out.println("Creating a new SQS queue "+queueName+".\n");
		CreateQueueRequest createQueueRequest = new CreateQueueRequest(queueName);
		String myQueueUrl = client.createQueue(createQueueRequest).getQueueUrl();
		System.out.println("Created queue at "+myQueueUrl+"\n");
		return myQueueUrl;
	}
	
	/**
	 * 
	 */
	public void teardownAll() throws Exception{
		sqsClient.deleteQueue(new DeleteQueueRequest(dispatchQueue));
		sqsClient.deleteQueue(new DeleteQueueRequest(returnQueue));
		System.out.println("Deleted SQS queues");
		//We create a request to describe current instances with the imageID that Cumulus uses.
		DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
		describeInstancesRequest.withFilters(new Filter("image-id").withValues(Constants.imageID));
		System.out.println("Getting list of active cumulus drones");
		// Initialize variables.
		List<String> instanceIds = new ArrayList<String>();
		try{
			AmazonEC2 ec2= new AmazonEC2Client(new PropertiesCredentials(
					new FileInputStream(Constants.credentialsFile)));
			// get all of the instanceID's.  add them to the list
			DescribeInstancesResult describeInstancesResult = ec2.describeInstances(describeInstancesRequest);
			List<Reservation> reservations = describeInstancesResult.getReservations();
			if (reservations!=null){
				for (Reservation reservation: reservations){
					List<Instance> instances = reservation.getInstances();
					if (instances!=null){
						for (Instance instance: instances){
							instanceIds.add(instance.getInstanceId());
						}
					}
				}
			}
			// Terminate instances.
        	System.out.println("Terminate instances");
        	TerminateInstancesRequest terminateRequest = new TerminateInstancesRequest(instanceIds);
        	ec2.terminateInstances(terminateRequest);
		} catch (AmazonServiceException e) {
    		// Write out any exceptions that may have occurred.
            System.err.println("Error terminating instances");
    		System.err.println("Caught Exception: " + e.getMessage());
            System.err.println("Reponse Status Code: " + e.getStatusCode());
            System.err.println("Error Code: " + e.getErrorCode());
            System.err.println("Request ID: " + e.getRequestId());
        } catch (Exception e) {
        	//caught another exception
			System.err.println(e);		}
		System.out.println("Killed all cumulus drones");
		}

	/**
	 * @return the unitsOnServer
	 */
	public synchronized Map<String, Map<String, wUStatus>> getUnitsOnServer() {
		return unitsOnServer;
	}

	/**
	 * @param unitsOnServer the unitsOnServer to set
	 */
	public synchronized void setUnitsOnServer(
			Map<String, Map<String, wUStatus>> unitsOnServer) {
		this.unitsOnServer = unitsOnServer;
	}

	/**
	 * @return the dispatchQueue
	 */
	public String getDispatchQueue() {
		return dispatchQueue;
	}

	/**
	 * @return the returnQueue
	 */
	public String getReturnQueue() {
		return returnQueue;
	}

	/**
	 * @return the sqsClient
	 */
	public AmazonSQS getSqsClient() {
		return sqsClient;
	}

	/**
	 * @param sqsClient the sqsClient to set
	 */
	public void setSqsClient(AmazonSQS sqsClient) {
		this.sqsClient = sqsClient;
	}
	
	

}
