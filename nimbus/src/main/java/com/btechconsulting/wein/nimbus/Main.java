/**
 * 
 */
package com.btechconsulting.wein.nimbus;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.btechconsulting.wein.nimbus.model.ReturnUnit;
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
			try {
				Thread.sleep(1000); // This guarantees that we will not query the SQS more than once a second
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				System.err.println("Waiting between requests to sqs was interrupted");
				System.exit(2);
			} 
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
			//generate the skeleton of the returnUnit
			ReturnUnit returnU= new ReturnUnit();
			returnU.setJobID(unMarshalledUnit.getJobID());
			returnU.setOwnerID(unMarshalledUnit.getOwnerID());
			returnU.setWorkUnitID(unMarshalledUnit.getWorkUnitID());
			returnU.setStatus("ERROR");//I'd much rather have a return unit incorrectly marked as an error than have a null status

			//get receptor and molecule from sql

			//store receptor and molecule on disk
			String receptorFileName=""; //TODO fill in this
			String moleculeFileName=""; //TODO fill in this
			//call vina
			Callable<String> callable = new VinaCaller(moleculeFileName, receptorFileName, unMarshalledUnit.getVinaParams());
			ExecutorService executor = new ScheduledThreadPoolExecutor(1);
			Future<String> returnString = executor.submit(callable);
			Long now =System.currentTimeMillis();
			Boolean retried= false;
			//we loop while waiting, up to a limit of 15 minutes
			while (!returnString.isDone()&& System.currentTimeMillis()<=900000) {
				try {
					Thread.sleep(1000); //check whether we are finished each second.
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.err.println("We were interrupted");
					System.exit(2);
				}
			}
			//check to see whether we finished.
			if (!returnString.isDone()){ //TODO refactor this into a loop, it is currently sloppy
				//if we haven't finished.
				if(retried==false){ //If this is our first try, retry
					now =System.currentTimeMillis();
					//we loop while waiting, up to a limit of 15 minutes
					while (!returnString.isDone()&& System.currentTimeMillis()<=900000) {
						try {
							Thread.sleep(1000); //check whether we are finished each second.
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							System.err.println("We were interrupted");
							System.exit(2);
						}
					}
				}
				if (!returnString.isDone()){
					executor.shutdownNow(); // kill ALL the things!
				}
			}
			
			System.out.println(returnString.get());

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
	
	private static String MarshallReturnUnit(ReturnUnit returnUnit) throws JAXBException{
		JAXBContext context = JAXBContext.newInstance(ReturnUnit.class);
		Marshaller m = context.createMarshaller();
		StringWriter writer = new StringWriter();
		m.marshal(returnUnit, writer);
		return writer.toString();
	}
	
	private static void SendStatusToReturnQueue(AmazonSQSClient client,String returnQueue, ReturnUnit returnUnit) throws JAXBException{
		String marshalledReturnUnit = MarshallReturnUnit(returnUnit);
		SendMessageRequest request = new SendMessageRequest(returnQueue, marshalledReturnUnit);
	}
	
	private static void DeleteMessageFromDispatchQueue(AmazonSQSClient client, String dispatchQueue, String receiptHandle){
		DeleteMessageRequest request = new DeleteMessageRequest(dispatchQueue, receiptHandle);
		client.deleteMessage(request);
	}

}
