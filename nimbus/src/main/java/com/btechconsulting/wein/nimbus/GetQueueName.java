/**
 * 
 */
package com.btechconsulting.wein.nimbus;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.TagDescription;

/**
 * @author samuel
 * This class gets the SQS queue names from the AMI metadata
 */
public class GetQueueName {
	/**
	 * 
	 * @param credentialsFile
	 * @param instanceID, the unique instance ID of the EC2 Instance
	 * @return A map of the queue name to queue url
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	
	private static final Logger logger= Logger.getLogger(GetQueueName.class);
	public static Map<String,String> GetQueues(String instanceID, PropertiesCredentials credentialsFile) throws IOException{
		Map<String, String> returnMap= new HashMap<String, String>();
		//Create the list of filters
		List<Filter> filters= new ArrayList<>();
		List<String> value =new ArrayList<String>();
		value.add(instanceID);
		Filter filter= new Filter("resource-id",value);
		filters.add(filter);
		
		//create the client
		AmazonEC2 ec2= new AmazonEC2Client(credentialsFile);
		//create the request
		DescribeTagsRequest request= new DescribeTagsRequest(filters);
		DescribeTagsResult result = ec2.describeTags(request);
		List<TagDescription> tags= result.getTags();
		//iterate over the returned tags, we only care about the values of dispatch and return
		for(TagDescription i:tags){
			if (i.getKey().equals("dispatch")){
				returnMap.put("dispatch", i.getValue());
			}else{
				if (i.getKey().equals("return")){
					returnMap.put("return", i.getValue());
				}
			}
		}
		return returnMap;
	}
	
	/**
	 * 
	 * @param instanceIdLoc the address of the instance metadata
	 * @return the unique instance ID of this instance if run on an EC2 instance or null otherwise
	 */
	public static String GetInstanceID(String instanceIdLoc) throws IllegalStateException{
		String responseBody=null;
		HttpClient httpClient = new DefaultHttpClient();
		try{
			HttpGet httpGet= new HttpGet(instanceIdLoc);
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
            responseBody = httpClient.execute(httpGet, responseHandler);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			throw new IllegalStateException("HTTP client exception "+e);
		} catch (IOException e) {
			logger.warn("IOexception getting InstanceID, are we running on an EC2 Instance?");
			System.err.println("IOexception getting InstanceID, are we running on an EC2 Instance?");
			e.printStackTrace();
		}finally{
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpClient.getConnectionManager().shutdown();	
		}
		return responseBody;
	}

}
