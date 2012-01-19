/**
 * 
 */
package com.btechconsulting.wein.nimbus;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.btechconsulting.wein.nimbus.model.WorkUnit;

/**
 * @author Samuel Wein
 *
 */
public class Main {

	/**
	 * This is the class that runs on startup of the Nimbus
	 * @param args
	 */
	public static void main(String[] args) {
		//TODO write shutdown hook here.

		//open sql connection

		//Get the queue names
		PropertiesCredentials credentials = null;
		try {
			credentials=new PropertiesCredentials(
					new FileInputStream(Constants.CREDENTIALSFILE));
		} catch (FileNotFoundException e1) {
			System.err.println("Error reading credentials file");
			e1.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Error reading credentials file");
			e.printStackTrace();
			System.exit(1);
		} if (credentials==null){
			System.err.println("Error reading credentials file, not caught.");
			//we should never reach here if we do it indicates a flow problem
			System.exit(2);
		}

		Map<String,String> queues= new HashMap<String, String>(); 
		String dispatchQueue=null;
		String returnQueue=null;
		dispatchQueue= queues.get("dispatch");
		returnQueue= queues.get("return");
		if (dispatchQueue==null|| returnQueue==null){
			System.err.println("Couldn't get queue names");
			System.exit(1);
		}
		try {
			queues = GetQueueName.GetQueues(GetQueueName.GetInstanceID(), null);
		} catch (IOException e) {
			System.err.println("Error reading credentials file");
			e.printStackTrace();
			System.exit(1);
		} catch (IllegalStateException e) {
			System.err.println("Internal Error");
			e.printStackTrace();
			System.exit(1);
		}
		//Get SQS client.
		AmazonSQSClient client = new AmazonSQSClient(credentials);
		//Start loop here
		while (true){
			//Check dispatch queue for workunit
			List<Message> messageList = GetMessageBundle(dispatchQueue, client);
			if (messageList.size()==0){//if the queue didn't have any messages
				try {
					Thread.sleep(Constants.WAITTIME);
				} catch (InterruptedException e) {
					System.err.println("Waiting interrupted");
					e.printStackTrace();
					System.exit(2);
				}
				messageList = GetMessageBundle(dispatchQueue, client);
				if (messageList.size()==0){//if the queue still didn't have any messages we die of boredom
					System.err.println("We ran out of work");
					System.exit(0);
				}
			}if (messageList.size()>1){// if we somehow get too many messages
				System.err.println("we got too many messages");
				System.exit(2);
			}
			//Retrieve workunit
			String receiptHandle= messageList.get(0).getReceiptHandle();
			String marshalledWorkUnit= messageList.get(0).getBody();
			//unmarshall workunit
			WorkUnit unMarshalledUnit= null;
			try{
				unMarshalledUnit= UnmarshallWorkUnit(marshalledWorkUnit);
			}
			catch (JAXBException e) {
				// TODO: handle exception send error to queue
			}
			if (unMarshalledUnit==null){
				//TODO: handle malformed work unit
			}
			
			//get receptor and molecule from sql

			//store receptor and molecule on disk

			//call vina

			//wait for time to expire or vina to return

			//load results from disk and put into SQL

			//put results in return queue
			//delete workunit from dispatch queue
		}
		//end loop

	}
	
	private static List<Message> GetMessageBundle(String dispatchQueue, AmazonSQSClient client){
		ReceiveMessageRequest request = new ReceiveMessageRequest(dispatchQueue).withMaxNumberOfMessages(1).withVisibilityTimeout(930);
		ReceiveMessageResult result= client.receiveMessage(request);
		List<Message> messageList =result.getMessages();
		return messageList;
	}
	
	private static WorkUnit UnmarshallWorkUnit(String workUnit) throws JAXBException{
		JAXBContext context = JAXBContext.newInstance(WorkUnit.class);
		Unmarshaller um = context.createUnmarshaller();
		WorkUnit returnUnit =(WorkUnit) um.unmarshal(new StringReader(workUnit));
		return returnUnit;
	}

}
