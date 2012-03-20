package com.btechconsulting.wein.cumulus.initialization;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;

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
	Initializer papa;
	
	public SqsListener(Initializer parent) {
		this.papa=parent;
	}

	public void run() {
		logger.info("Started SqsListener");
		AmazonSQSClient sqsClient=new AmazonSQSClient(papa.getCredentials());
		javax.xml.bind.Unmarshaller unMarshaller = null;
		try{
			JAXBContext context= JAXBContext.newInstance(ReturnUnit.class);
			unMarshaller= context.createUnmarshaller();
		}
		catch (JAXBException jbe){
			//This exception is bad, and should result in the system terminating, as cleanly as possible
			logger.error("Cannot create unmarshaller for return queue");
			papa.teardownAll();
			System.exit(1);
		}
		int nummessages=0;//TODO remove this

		while (true){
			ReceiveMessageRequest request= new ReceiveMessageRequest(papa.getReturnQueue()).withMaxNumberOfMessages(10);
			List<Message> results =sqsClient.receiveMessage(request).getMessages();
			List<DeleteMessageBatchRequestEntry> deleteList=new ArrayList<DeleteMessageBatchRequestEntry>();
			if (results.size()>1){
				logger.info("got "+results.size()+" results");
				System.out.println("got "+results.size()+" results");//TODO remove this
			}
			for (Message i:results){
				String marshalledResult=i.getBody();
				ReturnUnit unMarshalledResult;
				try {
					unMarshalledResult = (ReturnUnit) unMarshaller.unmarshal(new StringReader(marshalledResult));
					deleteList.add(new DeleteMessageBatchRequestEntry(i.getMessageId(),i.getReceiptHandle()));
					nummessages++; //TODO remove this
				} catch (JAXBException e) {
					logger.error("Malformed return unit");
					logger.error("Malformed unit:"+marshalledResult);
					continue;
				}
				logger.info(unMarshalledResult.getStatus());
				papa.putWorkUnit(unMarshalledResult.getOwnerID(), unMarshalledResult.getJobID(), unMarshalledResult.getWorkUnitID(),Initializer.wUStatus.valueOf(unMarshalledResult.getStatus()));//we get a null pointer exception here
				logger.info("got unit from queue");
			}
			DeleteMessageBatchRequest deleteRequests=new DeleteMessageBatchRequest(papa.getReturnQueue(), deleteList);
			//after we have read the messages we delete them
			if (deleteList.size()>0){ // we can only delete if we have request of things to delete
				sqsClient.deleteMessageBatch(deleteRequests);
			}
			try {
				if (results.size()!=10){
					Thread.sleep(1000); // this prevents us from polling constantly, running up a huge bill NB, we may end up adjusting this down for a heavily loaded server
				}
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

}
