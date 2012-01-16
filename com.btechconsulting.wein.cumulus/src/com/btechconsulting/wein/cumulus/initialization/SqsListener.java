package com.btechconsulting.wein.cumulus.initialization;

import java.io.StringReader;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.btechconsulting.wein.cumulus.model.ReturnUnit;

class SqsListener implements Runnable {

	/**
	 * This class creates the thread that picks up results from the SQS.
	 * It is important that the Listener not die
	 */

	public void run() {
		System.out.println("entered thread");
		AmazonSQSClient sqsClient=new AmazonSQSClient(Initializer.INSTANCE.getCredentials());
		javax.xml.bind.Unmarshaller unMarshaller = null;
		try{
		JAXBContext context= JAXBContext.newInstance(ReturnUnit.class);
		unMarshaller= context.createUnmarshaller();
		}
		catch (JAXBException jbe){
			//This exception is bad, and should result in the system terminating, as cleanly as possible
			System.err.println("Cannot create unmarshaller for return queue");
			Initializer.INSTANCE.teardownAll();
			System.exit(1);
		}
		while (true){
			ReceiveMessageRequest request= new ReceiveMessageRequest(Initializer.INSTANCE.getReturnQueue());
			List<Message> results =sqsClient.receiveMessage(request).getMessages();
			for (Message i:results){
				String marshalledResult=i.getBody();
				ReturnUnit unMarshalledResult;
				try {
					unMarshalledResult = (ReturnUnit) unMarshaller.unmarshal(new StringReader(marshalledResult));
				} catch (JAXBException e) {
					System.err.println("Malformed return unit");
					System.err.println("Malformed unit:"+marshalledResult);
					continue;
				}
				Initializer.INSTANCE.putWorkUnit(unMarshalledResult.getOwnerID(), unMarshalledResult.getJobID(), unMarshalledResult.getWorkUnitID(),Initializer.wUStatus.valueOf(unMarshalledResult.getStatus()));
				System.out.println("got unit from queue");
			}
			try {
				Thread.sleep(1000); // this prevents us from polling constantly, running up a huge bill
			} catch (InterruptedException e) {
				System.err.println("Sqslistener was interrupted");
				continue;
			}
		}
	}
}