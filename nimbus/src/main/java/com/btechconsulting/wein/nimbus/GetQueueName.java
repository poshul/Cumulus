/**
 * 
 */
package com.btechconsulting.wein.nimbus;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.TagDescription;

/**
 * @author samuel
 * This class gets the SQS queue names from the AMI metadata
 */
public class GetQueueName {
	/**
	 * 
	 * @return A map of the queue name to queue url
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Map<String,String> GetQueues() throws FileNotFoundException, IOException{
		Map<String, String> returnMap= new HashMap<String, String>();
		PropertiesCredentials credentials=new PropertiesCredentials(
				new FileInputStream(Constants.CREDENTIALSFILE));
		AmazonEC2 ec2= new AmazonEC2Client(credentials);
		DescribeTagsRequest request= new DescribeTagsRequest();
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

}
