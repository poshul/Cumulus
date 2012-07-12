package com.btechconsulting.wein.cumulus.initialization;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.btechconsulting.wein.cumulus.model.ReturnUnit;

public class SqsListenerTest {

/*		@Before
	public void setUp() throws Exception {

	}*/

	//@Test
	public void threadTest() {
		//sleep until we can recreate the queue
		try {
			Thread.sleep(60000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		String testUID= "tester";
		Integer testjobID= 9;
		Integer testWorkunitID=1;
		Marshaller marshaller=null;
		try{
			JAXBContext context= JAXBContext.newInstance(ReturnUnit.class);
			marshaller= context.createMarshaller();
		}
		catch (Exception e){
			System.err.println("Couldn't create return unit marshaller");
		}
		AmazonSQSClient sqsClient=new AmazonSQSClient(Initializer.getInstance().getCredentials());
		Map<Integer, Initializer.wUStatus> work= new HashMap<Integer, Initializer.wUStatus>();
		ReturnUnit testUnit= new ReturnUnit();
		testUnit.setJobID(testjobID);
		testUnit.setOwnerID(testUID);
		testUnit.setWorkUnitID(testWorkunitID);
		testUnit.setStatus(Initializer.wUStatus.DONE.toString());
		java.io.StringWriter writer= new StringWriter();
		//marshal the return unit and put it on the server
		//put the work unit in the sqs and the unitsOnServer map
		work.put(testWorkunitID, Initializer.wUStatus.INFLIGHT);
		Initializer.getInstance().putJobOnServer(testUID, testjobID, work);
		try {
			marshaller.marshal(testUnit,writer);
			String marshalledReturn= writer.toString();
			SendMessageRequest request = new SendMessageRequest(Initializer.getInstance().getReturnQueue(),marshalledReturn);
			sqsClient.sendMessage(request);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		try {
			//It takes a while for the message to become available in SQS.
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertEquals(Initializer.wUStatus.DONE,Initializer.getInstance().getStatusOfWorkUnit(testUID, testjobID, testWorkunitID));
	}
	//delete the unit from the queue

	//@After
	public void tearDown() throws Exception{
		Initializer.getInstance().teardownAll();
	}

}
