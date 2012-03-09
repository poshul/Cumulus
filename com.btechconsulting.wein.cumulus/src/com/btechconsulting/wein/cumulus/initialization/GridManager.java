package com.btechconsulting.wein.cumulus.initialization;

import java.util.Map;

import org.apache.log4j.Logger;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;

public class GridManager implements Runnable {

	/**
	 * @author samuel Wein
	 * @date Mar/6/2012
	 * Manages the number of running EC2 instances
	 */
	static final Logger logger=Logger.getLogger(GridManager.class);
	Initializer papa;
	
	public GridManager(Initializer parent) {
		this.papa=parent;
	}

	public void run() {
		logger.debug("started GridManager");
		System.out.println("started GridManager");
		AmazonSQSClient sqsClient =new AmazonSQSClient(papa.getCredentials());
		AmazonEC2Client ec2Client =new AmazonEC2Client(papa.getCredentials());
		while (true){ //loop from startup to shutdown
			//check the number of messages in the queue.
			GetQueueAttributesRequest sqsRequest= new GetQueueAttributesRequest(papa.getDispatchQueue()).withAttributeNames("ApproximateNumberOfMessages");
			Integer sqsResult= null;
			try{
				sqsResult= Integer.valueOf(sqsClient.getQueueAttributes(sqsRequest).getAttributes().get("ApproximateNumberOfMessages"));
			} catch (AmazonServiceException e) {
				// Write out any exceptions that may have occurred.
				System.err.println("Error getting list of instances");
				System.err.println("Caught Exception: " + e.getMessage());
				System.err.println("Reponse Status Code: " + e.getStatusCode());
				System.err.println("Error Code: " + e.getErrorCode());
				System.err.println("Request ID: " + e.getRequestId());
			} catch (Exception e) {
				//caught another exception
				System.err.println(e);
				System.err.println("Did not exit cleanly");
				//System.exit(1);
			}
			if (sqsResult==null){
				logger.error("Couldn't get the number of items in the queue reverting to static functioning");
				break;
			}
			System.out.println("Queue size="+sqsResult);
			//check the number of running instances.
			DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
			//get the number of running instances with our image ID
			describeInstancesRequest.withFilters(new Filter("image-id").withValues(Constants.imageID),new Filter("instance-state-name").withValues("running"));
			logger.debug("Getting list of active cumulus drones");
			Integer numInstances=null;
			try{
				DescribeInstancesResult describeInstancesResult = ec2Client.describeInstances(describeInstancesRequest);
				if(describeInstancesResult.getReservations().size()==0)
				{
					numInstances=0;
				}else {
					Integer totalInstances=0;
					for(Reservation i:describeInstancesResult.getReservations()){
						totalInstances+=i.getInstances().size();
					}
					numInstances=totalInstances;
				}
				//System.out.println("num instances="+numInstances);
			} catch (AmazonServiceException e) {
				// Write out any exceptions that may have occurred.
				System.err.println("Error getting list of instances");
				System.err.println("Caught Exception: " + e.getMessage());
				System.err.println("Reponse Status Code: " + e.getStatusCode());
				System.err.println("Error Code: " + e.getErrorCode());
				System.err.println("Request ID: " + e.getRequestId());
				//System.exit(1);
			} catch (Exception e) {
				//caught another exception
				System.err.println(e);
				System.err.println("Did not exit cleanly");
				//System.exit(1);
			}
			if (numInstances==null){
				logger.error("Couldn't get the number of items in the queue reverting to static functioning");
				break;
			}
			System.out.println("num instances="+numInstances);

			// if queue is longer than the number of instances * idealMaxUnitsPerInstance
			if(sqsResult>numInstances*Constants.idealMaxUnitsPerInstance){
				Integer numToCreate= (sqsResult/Constants.idealMaxUnitsPerInstance)-numInstances+1;
				try {
					System.out.println(numToCreate);
					papa.createInstances(ec2Client, numToCreate);
					System.out.println("created instances");
				} catch (Exception e) {
					// TODO Deal with ec2 exceptions
					e.printStackTrace();
				}

			}
			try {
				Thread.sleep(60000); // we wait for any new servers to start up
			} catch (InterruptedException e) {
				logger.error("Sqslistener was interrupted");
				if (papa.getShuttingDown()){//FIXME
					break;
				}
				else{
					continue;
				}
			}
		}
	}
	
/*	public static void main(String[] args) {
		//start the gridManager
		Thread gridManager = new Thread(new GridManager());
		gridManager.setName("gridManager");
		gridManager.start();
	}*/

}
