package com.btechconsulting.wein.cumulus.initialization;

import java.util.Date;

import org.junit.Test;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsResult;
import com.amazonaws.services.ec2.model.DescribeSpotPriceHistoryRequest;
import com.amazonaws.services.ec2.model.DescribeSpotPriceHistoryResult;
import com.amazonaws.services.ec2.model.Filter;

public class SpotPriceTest {

	@Test
	public void testCreateSpotInstances() throws Exception {
		AmazonEC2Client ec2Client= new AmazonEC2Client(Initializer.getInstance(null).getCredentials());//TODO fix creds location
		DescribeSpotPriceHistoryRequest historyRequest= new DescribeSpotPriceHistoryRequest()
		.withAvailabilityZone("us-east-1c")
		.withInstanceTypes(Constants.instanceType)
		.withProductDescriptions("Linux/UNIX (Amazon VPC)")
		.withStartTime(new Date(System.currentTimeMillis()));
		DescribeSpotPriceHistoryResult result= ec2Client.describeSpotPriceHistory(historyRequest);
		System.out.println(result.getSpotPriceHistory().get(0).getSpotPrice());
	}
	
	@Test
	public void testCheckSpotInstances() throws Exception {
		AmazonEC2Client ec2Client= new AmazonEC2Client(Initializer.getInstance(null).getCredentials()); //TODO fix creds location
		DescribeSpotInstanceRequestsRequest describeSpotInstancesRequest= new DescribeSpotInstanceRequestsRequest();
		describeSpotInstancesRequest.withFilters(new Filter("state").withValues("open"));
		Integer numSpotRequests=0;
		try{
			DescribeSpotInstanceRequestsResult describeSpotReservationsResult = ec2Client.describeSpotInstanceRequests(describeSpotInstancesRequest);
			if(describeSpotReservationsResult.getSpotInstanceRequests().size()==0)
			{
				numSpotRequests=0;
			}else {
/*				Integer totalInstances=0;
				for(SpotInstanceRequest i:describeSpotReservationsResult.getSpotInstanceRequests()){
					totalInstances++;
				}*/
				numSpotRequests=describeSpotReservationsResult.getSpotInstanceRequests().size();
				//numSpotRequests=totalInstances;
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
		System.out.println(numSpotRequests);
	}

}
