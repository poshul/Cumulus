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
	// units on server is a map of OwnerID to a map of JobID to a map of WorkUnitID to work Unit status
	// TODO figure out how to get unit testing to work when this is private
	Map<String, Map<String, Map<String,wUStatus>>> unitsOnServer;
	
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

	private Map<String, Map<String, Map<String, wUStatus>>> createUnitsOnServer() {
		return Collections.synchronizedMap(new HashMap<String, Map<String, Map<String,wUStatus>>>());
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
	 * putJobOnServer: add a complete job (jobID, Map<String, wUstatus>) to the server
	 * @param userID the user ID of the end user adding the job
	 * @param jobID the ID of the job to add to the server
	 * @param workUnits A map of workunits to add to the server
	 */
	public synchronized void putJobOnServer(
			String userID, String jobID, Map<String, wUStatus> workUnits ){
		this.unitsOnServer.get(userID).put(jobID, workUnits);
	}
	/**
	 * removeJobFromServer: remove a complete job from the server
	 * @param userID the user ID of the end user removing the job
	 * @param jobID the ID of the job to be removed
	 */
	public synchronized void removeJobFromServer(
			String userID, String jobID){
		this.unitsOnServer.get(userID).remove(jobID);
	}
	
	/**
	 * putWorkUnit: puts a workunit into an existing job.  NB: This either creates a new work unit, or overwrites an existing one
	 * @param userID the ID of the end user putting in the work unit
	 * @param jobID the ID of the job for which the work unit is being modified
	 * @param workUnitID the ID of the work unit to be modified
	 * @param newStatus the new status of the work unit
	 */

	public synchronized void putWorkUnit(
			String userID, String jobID, String workUnitID, wUStatus newStatus){
		this.unitsOnServer.get(userID).get(jobID).put(workUnitID, newStatus);
	}

	/**
	 * getStatusOfWorkUnit: returns the status of the selected work unit
	 * @param userID the ID of the end user querying the work unit
	 * @param jobID the ID of the job which is being queried
	 * @param workUnitID the ID of the work unit being queried 
	 * @return wUStatus the status of the work unit
	 */
	public synchronized wUStatus getStatusOfWorkUnit(
			String userID, String jobID, String workUnitID){
		return this.unitsOnServer.get(userID).get(jobID).get(workUnitID);
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

	

}
