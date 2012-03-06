package com.btechconsulting.wein.cumulus.initialization;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;

import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.btechconsulting.wein.cumulus.model.ReturnUnit;

class SqsListener implements Runnable {

	/**
	 * This class creates the thread that picks up results from the SQS.
	 * It is important that the Listener not die
	 */
	static final Logger logger=Logger.getLogger(SqsListener.class);

	public void run() {
		logger.info("entered thread");
		AmazonSQSClient sqsClient=new AmazonSQSClient(Initializer.getInstance().getCredentials());
		javax.xml.bind.Unmarshaller unMarshaller = null;
		try{
			JAXBContext context= JAXBContext.newInstance(ReturnUnit.class);
			unMarshaller= context.createUnmarshaller();
		}
		catch (JAXBException jbe){
			//This exception is bad, and should result in the system terminating, as cleanly as possible
			logger.error("Cannot create unmarshaller for return queue");
			Initializer.getInstance().teardownAll();
			System.exit(1);
		}
		while (true){
			ReceiveMessageRequest request= new ReceiveMessageRequest(Initializer.getInstance().getReturnQueue()).withMaxNumberOfMessages(10);
			List<Message> results =sqsClient.receiveMessage(request).getMessages();
			List<DeleteMessageBatchRequestEntry> deleteList=new ArrayList<DeleteMessageBatchRequestEntry>();
			if (results.size()>1){
				logger.info("got "+results.size()+" results");
			}
			for (Message i:results){
				String marshalledResult=i.getBody();
				ReturnUnit unMarshalledResult;
				try {
					unMarshalledResult = (ReturnUnit) unMarshaller.unmarshal(new StringReader(marshalledResult));
					deleteList.add(new DeleteMessageBatchRequestEntry(i.getMessageId(),i.getReceiptHandle()));
				} catch (JAXBException e) {
					logger.error("Malformed return unit");
					logger.error("Malformed unit:"+marshalledResult);
					continue;
				}
				logger.info(unMarshalledResult.getStatus());
				Initializer.getInstance().putWorkUnit(unMarshalledResult.getOwnerID(), unMarshalledResult.getJobID(), unMarshalledResult.getWorkUnitID(),Initializer.wUStatus.valueOf(unMarshalledResult.getStatus()));//we get a null pointer exception here
				logger.info("got unit from queue");
			}
			DeleteMessageBatchRequest deleteRequests=new DeleteMessageBatchRequest(Initializer.getInstance().getReturnQueue(), deleteList);
			//after we have read the messages we delete them
			if (deleteList.size()>0){ // we can only delete if we have request of things to delete
				sqsClient.deleteMessageBatch(deleteRequests);
			}
			try {
				Thread.sleep(1000); // this prevents us from polling constantly, running up a huge bill NB, we may end up adjusting this down for a heavily loaded server
			} catch (InterruptedException e) {
				logger.error("Sqslistener was interrupted");
				if (Initializer.getInstance(null).getShuttingDown()){
					break;
				}
				else{
					continue;
				}
			}
		}
	}

}
