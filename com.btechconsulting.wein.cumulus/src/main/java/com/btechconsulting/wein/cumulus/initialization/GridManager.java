package com.btechconsulting.wein.cumulus.initialization;

import java.util.Date;

import org.apache.log4j.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsResult;
import com.amazonaws.services.ec2.model.DescribeSpotPriceHistoryRequest;
import com.amazonaws.services.ec2.model.DescribeSpotPriceHistoryResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;

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
			} catch (AmazonClientException e){
				logger.warn(e);
				continue; //if the error is due to being unable to connect to sqs it is likely transient, and we should keep going until SQS comes back
			} catch (Exception e) {
				//caught another exception
				System.err.println(e);
				System.err.println("Did not exit cleanly");
				//System.exit(1);
			}
			if (sqsResult==null){
				System.err.println("Couldn't get the number of items in the queue reverting to static functioning");
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
			Integer numSpotRequests=null;
			Initializer.serialize("/tmp/dump.obj", papa.unitsOnServer); //TODO test this.
			//check number of open spot requests
			DescribeSpotInstanceRequestsRequest describeSpotInstancesRequest= new DescribeSpotInstanceRequestsRequest();
			describeSpotInstancesRequest.withFilters(new Filter("state").withValues("open"));
			try{
				//Parse returned instances
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
				//Parse returned open spot requests
				DescribeSpotInstanceRequestsResult describeSpotReservationsResult = ec2Client.describeSpotInstanceRequests(describeSpotInstancesRequest);
				if(describeSpotReservationsResult.getSpotInstanceRequests().size()==0)
				{
					numSpotRequests=0;
				}else {
					Integer totalInstances=0;
					for(SpotInstanceRequest i:describeSpotReservationsResult.getSpotInstanceRequests()){
						totalInstances++;
					}
					numSpotRequests=totalInstances;
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
			if (numInstances==null|| numSpotRequests==null){
				logger.error("Couldn't get the number of machines or requests reverting to static functioning");
				break;
			}
			System.out.println("num instances="+(numInstances+numSpotRequests));

			// if queue is longer than the number of instances * idealMaxUnitsPerInstance
			if(sqsResult>(numInstances+numSpotRequests)*Constants.idealMaxUnitsPerInstance){ // we count pending spot instances too.
				Integer numToCreate= (sqsResult/Constants.idealMaxUnitsPerInstance)-numInstances+1;
				//check current pricing of spot instances
				DescribeSpotPriceHistoryRequest historyRequest= new DescribeSpotPriceHistoryRequest()
				.withAvailabilityZone("us-east-1c")
				.withInstanceTypes(Constants.instanceType)
				.withProductDescriptions("Linux/UNIX (Amazon VPC)")
				.withStartTime(new Date(System.currentTimeMillis()));
				DescribeSpotPriceHistoryResult result= ec2Client.describeSpotPriceHistory(historyRequest);
				Double nowPrice= Double.valueOf(result.getSpotPriceHistory().get(0).getSpotPrice());
				//If spot price is < constants.spotPrice launch numToCreate*Constants.percentSpot spot instances
				Integer spotToCreate=0;
				if (nowPrice<Double.valueOf(Constants.spotPrice)){
					spotToCreate=(int) (numToCreate*Constants.percentSpot);
					numToCreate=(int) (numToCreate*(1-Constants.percentSpot));
				}
				try {
					System.out.println("regular instances: "+numToCreate);
					System.out.println("spot instances: "+ spotToCreate);
					//Create regular and spot instances
					if (numToCreate>0){
					papa.createInstances(ec2Client, numToCreate);
					}
					if (spotToCreate>0){
					papa.createSpotInstances(ec2Client, spotToCreate);
					}
					System.out.println("created instances");
				} catch (Exception e) {
					// Report ec2 exceptions
					logger.error(e);
					e.printStackTrace();
					continue;
				}

			}
			try {
				Thread.sleep(60000); // we wait for any new servers to start up
			} catch (InterruptedException e) {
				logger.error("Sqslistener was interrupted");
				if (papa.getShuttingDown()){
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
