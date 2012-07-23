/**
 * 
 */
package com.btechconsulting.wein.cumulus.workUnitGenerator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.btechconsulting.wein.cumulus.initialization.Constants;
import com.btechconsulting.wein.cumulus.initialization.Initializer;
import com.btechconsulting.wein.cumulus.model.FilterParams;
import com.btechconsulting.wein.cumulus.model.VinaParams;
import com.btechconsulting.wein.cumulus.model.WorkUnit;

/**
 * @author samuel
 * 
 *
 */
public class WorkUnitGeneratorTest {

	/**
	 * Test method for {@link com.btechconsulting.wein.cumulus.workUnitGenerator.WorkUnitGenerator#BuildWorkUnit(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, com.btechconsulting.wein.cumulus.model.VinaParams)}.
	 */
	@Test
	public void testBuildWorkUnit() {
		WorkUnit myWorkUnit= new WorkUnit();
		WorkUnit testWorkUnit= new WorkUnit();
		String receptor="1";
		String ownerId="0";
		Integer jobId=2;
		String molecule="ZINC68740768";
		Integer workUnitId=020;
		VinaParams params = new VinaParams();
		params.setCenterX(0);
		params.setCenterY(3);
		params.setCenterZ(-1);
		params.setExhaustiveness(9);
		params.setNumModes(7);
		params.setSizeX(3);
		params.setSizeY(2);
		params.setSizeZ(2);
		testWorkUnit.setJobID(jobId);
		testWorkUnit.setOwnerID(ownerId);
		testWorkUnit.setPointerToMolecule(molecule);
		testWorkUnit.setPointerToReceptor(receptor);
		testWorkUnit.setVinaParams(params);
		testWorkUnit.setWorkUnitID(workUnitId);
		myWorkUnit = WorkUnitGenerator.BuildWorkUnit(receptor,molecule,ownerId,jobId,workUnitId, params);
		assert(testWorkUnit.equals(myWorkUnit));
	}

	/**
	 * Test method for {@link com.btechconsulting.wein.cumulus.workUnitGenerator.WorkUnitGenerator#putWorkUnitInSQSBatch(com.btechconsulting.wein.cumulus.model.WorkUnit)}.
	 */
	@Test
	public void testPutWorkUnitInSQS() throws Exception{
		//manually generate workunit
		WorkUnit testWorkUnit= new WorkUnit();
		String receptor="1";
		String ownerId="0";
		Integer jobId=2;
		String molecule="ZINC68740768";
		Integer workUnitId=020;
		VinaParams params = new VinaParams();
		params.setCenterX(0);
		params.setCenterY(3);
		params.setCenterZ(-1);
		params.setExhaustiveness(9);
		params.setNumModes(7);
		params.setSizeX(3);
		params.setSizeY(2);
		params.setSizeZ(2);
		testWorkUnit.setJobID(jobId);
		testWorkUnit.setOwnerID(ownerId);
		testWorkUnit.setPointerToMolecule(molecule);
		testWorkUnit.setPointerToReceptor(receptor);
		testWorkUnit.setVinaParams(params);
		testWorkUnit.setWorkUnitID(workUnitId);
		//WorkUnitGenerator.putWorkUnitInSQSBatch(testWorkUnit);
		AmazonSQS sqsClient = new AmazonSQSClient(Initializer.getInstance(null).getCredentials());
		String queue;
		try {
			queue = Initializer.getInstance(null).createQueue(Initializer.getInstance(null).getSqsClient(), Constants.testDispatchQueueName);
			WorkUnitGenerator.PutWorkUnitOnServer(testWorkUnit, queue);
			ReceiveMessageRequest messageRequest= new ReceiveMessageRequest(queue);
			List<Message> messages= sqsClient.receiveMessage(messageRequest).getMessages();
			List<DeleteMessageBatchRequestEntry> deleteList=new ArrayList<DeleteMessageBatchRequestEntry>();
			for (Message message : messages) {
				//test unmarshalling of the message
				deleteList.add(new DeleteMessageBatchRequestEntry(message.getMessageId(),message.getReceiptHandle()));
				JAXBContext context= JAXBContext.newInstance(WorkUnit.class);
				Unmarshaller um = context.createUnmarshaller();
				WorkUnit umWorkUnit=(WorkUnit) um.unmarshal(new StringReader(message.getBody()));
				System.out.println(umWorkUnit.getPointerToMolecule());
				assert (umWorkUnit.equals(testWorkUnit)); //test that we have marshalled and unmarshalled properly
			}
			DeleteMessageBatchRequest deleteRequests=new DeleteMessageBatchRequest(queue, deleteList);
			//after we have read the messages we delete them
			if (deleteList.size()>0){ // we can only delete if we have request of things to delete
				sqsClient.deleteMessageBatch(deleteRequests);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testBuildJob() throws AmazonServiceException, AmazonClientException, FileNotFoundException, SQLException, JAXBException, IOException{
		FilterParams filter= new FilterParams();
		filter.setMaxNrb(0);
		filter.setMaxMwt((float) 150.1);
		VinaParams params= new VinaParams();
		params.setCenterX(0);
		params.setCenterY(0);
		params.setCenterZ(0);
		params.setSizeX(2);
		params.setSizeY(5);
		params.setSizeZ(5);
		String queue;
		try {
			queue = Initializer.getInstance(null).createQueue(Initializer.getInstance(null).getSqsClient(), Constants.testDispatchQueueName);
			WorkUnitGenerator.BuildJob("blah", "0", params, filter, queue);
			ReceiveMessageRequest messageRequest= new ReceiveMessageRequest(queue).withMaxNumberOfMessages(10); //TODO fix queue loc
			
			List<Message> messages= Initializer.getInstance(null).getSqsClient().receiveMessage(messageRequest).getMessages();
			List<DeleteMessageBatchRequestEntry> deleteList=new ArrayList<DeleteMessageBatchRequestEntry>();
			Thread.sleep(1000);
			for (Message message : messages) {
				//test unmarshalling of the message
				deleteList.add(new DeleteMessageBatchRequestEntry(message.getMessageId(),message.getReceiptHandle()));
			}
			DeleteMessageBatchRequest deleteRequests=new DeleteMessageBatchRequest(queue, deleteList);
			//after we have read the messages we delete them
			if (deleteList.size()>0){ // we can only delete if we have request of things to delete
				Initializer.getInstance(null).getSqsClient().deleteMessageBatch(deleteRequests);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}


	}

	//@Test //TODO fix test
	/*	public void TestPutWorkOnServer() throws AmazonServiceException, InternalError, AmazonClientException, FileNotFoundException, JAXBException, IOException{
		//manually generate workunit
		WorkUnit testWorkUnit= new WorkUnit();
		String receptor="1";
		String ownerId="0";
		Integer jobId=2;
		String molecule="ZINC68740768";
		Integer workUnitId=020;
		VinaParams params = new VinaParams();
		params.setCenterX(0);
		params.setCenterY(3);
		params.setCenterZ(-1);
		params.setExhaustiveness(9);
		params.setNumModes(7);
		params.setSizeX(3);
		params.setSizeY(2);
		params.setSizeZ(2);
		testWorkUnit.setJobID(jobId);
		testWorkUnit.setOwnerID(ownerId);
		testWorkUnit.setPointerToMolecule(molecule);
		testWorkUnit.setPointerToReceptor(receptor);
		testWorkUnit.setVinaParams(params);
		testWorkUnit.setWorkUnitID(workUnitId);
		WorkUnitGenerator.PutWorkUnitOnServer(testWorkUnit);//TODO overload PutWorkUnitOnServer to accept different queue locations
		AmazonSQS sqsClient = new AmazonSQSClient(new PropertiesCredentials(
				new FileInputStream(Constants.credentialsFile)));
		ReceiveMessageRequest messageRequest= new ReceiveMessageRequest(Initializer.getInstance(null).getDispatchQueue());//TODO fix sqs loc
		List<Message> messages= sqsClient.receiveMessage(messageRequest).getMessages();
		for (Message message : messages) {
			//test unmarshalling of the message
			JAXBContext context= JAXBContext.newInstance(WorkUnit.class);
			Unmarshaller um = context.createUnmarshaller();
			WorkUnit umWorkUnit=(WorkUnit) um.unmarshal(new StringReader(message.getBody()));
			System.out.println(umWorkUnit.getPointerToMolecule());
			assert (umWorkUnit.equals(testWorkUnit)); //test that we have marshalled and unmarshalled properly
		}
	}
	 */
	/*	@After
	public void tearDown() throws Exception {
		Initializer.getInstance().teardownAll();
	}*/
}
