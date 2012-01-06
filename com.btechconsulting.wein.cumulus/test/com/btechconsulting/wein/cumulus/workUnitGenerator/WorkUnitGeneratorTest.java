/**
 * 
 */
package com.btechconsulting.wein.cumulus.workUnitGenerator;

import java.io.FileInputStream;
import java.io.StringReader;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.btechconsulting.wein.cumulus.initialization.Constants;
import com.btechconsulting.wein.cumulus.initialization.Initializer;
import com.btechconsulting.wein.cumulus.model.VinaParams;
import com.btechconsulting.wein.cumulus.model.WorkUnit;

/**
 * @author samuel
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
		testWorkUnit.setJobID(jobId);
		testWorkUnit.setOwnerID(ownerId);
		testWorkUnit.setPointerToMolecule(molecule);
		testWorkUnit.setPointerToReceptor(receptor);
		testWorkUnit.setVinaParams(params);
		testWorkUnit.setWorkUnitID(workUnitId);
		myWorkUnit = WorkUnitGenerator.BuildWorkUnit(receptor,molecule,ownerId,jobId,workUnitId, params);
		assert(testWorkUnit.equals(myWorkUnit)); //TODO check that equality works.

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
		testWorkUnit.setJobID(jobId);
		testWorkUnit.setOwnerID(ownerId);
		testWorkUnit.setPointerToMolecule(molecule);
		testWorkUnit.setPointerToReceptor(receptor);
		testWorkUnit.setVinaParams(params);
		testWorkUnit.setWorkUnitID(workUnitId);
		WorkUnitGenerator.putWorkUnitInSQSBatch(testWorkUnit);
		AmazonSQS sqsClient = new AmazonSQSClient(new PropertiesCredentials(
				new FileInputStream(Constants.credentialsFile)));
		ReceiveMessageRequest messageRequest= new ReceiveMessageRequest(Initializer.INSTANCE.getDispatchQueue());
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

/*	@After
	public void tearDown() throws Exception {
		Initializer.INSTANCE.teardownAll();
	}*/
}
