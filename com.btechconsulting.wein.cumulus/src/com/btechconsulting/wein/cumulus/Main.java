package com.btechconsulting.wein.cumulus;


public class Main {


	//This class is just a scratch pad
	/**
	 * @param args
	 */
	/*	public static void main(String[] args) {
		//check units on server
		//System.out.println(Initializer.INSTANCE.getUnitsOnServer());
		//check sqs dispatch queue
		System.out.println(Initializer.INSTANCE.getDispatchQueue());
		//check sqs return queue
		System.out.println(Initializer.INSTANCE.getReturnQueue());
		try {
			Thread.sleep(10000);
			System.out.println("starting teardown");
			Initializer.INSTANCE.teardownAll();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/
/*	public static void main(String[] args) {
		//manually generate workunit
		WorkUnit testWorkUnit= new WorkUnit();
		String receptor="1";
		String ownerId="0";
		String jobId="2";
		String molecule="ZINC68740768";
		String workUnitId="020";
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
		try{
			WorkUnitGenerator.PutWorkUnitInSQS(testWorkUnit);
			AmazonSQS sqsClient = new AmazonSQSClient(new PropertiesCredentials(
					new FileInputStream(Constants.credentialsFile)));
			ReceiveMessageRequest messageRequest= new ReceiveMessageRequest(Initializer.INSTANCE.getDispatchQueue());
			List<Message> messages= sqsClient.receiveMessage(messageRequest).getMessages();
			for (Message message : messages) {
				System.out.println("  Message");
				System.out.println("    MessageId:     " + message.getMessageId());
				System.out.println("    ReceiptHandle: " + message.getReceiptHandle());
				System.out.println("    MD5OfBody:     " + message.getMD5OfBody());
				System.out.println("    Body:          " + message.getBody());
				//test unmarshalling of the message
				JAXBContext context= JAXBContext.newInstance(WorkUnit.class);
				Unmarshaller um = context.createUnmarshaller();
				WorkUnit umWorkUnit=(WorkUnit) um.unmarshal(new StringReader(message.getBody()));
				System.out.println(umWorkUnit.getPointerToMolecule());
			}
			System.out.println();

		}
		catch (Exception e) {
			System.err.println(e.getStackTrace());
		}


	}
*/
}
